package Handshake;

import Env.DeviceEnv;
import ProtocolTree.ProtocolTreeNode;
import ProtocolTree.XmppDecode;
import ProtocolTree.XmppEncode;
import Util.StringUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.google.protobuf.ByteString;
import com.southernstorm.noise.protocol.CipherStatePair;
import com.southernstorm.noise.protocol.HandshakeState;
import com.whatsapp.android.GorgeousEngine;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.MccMnc;
import com.whatsapp.android.enums.NodeTaskType;
import com.whatsapp.android.util.DeviceUtil;
import com.whatsapp.android.util.TaskNotify;
import com.whatsapp.android.util.WhatsAppUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.netty.resolver.DefaultAddressResolverGroup;
import io.netty.resolver.NoopAddressResolverGroup;
import io.netty.util.ReferenceCountUtil;
import jni.ProtocolNodeJni;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.squirrelframework.foundation.fsm.StateMachineBuilderFactory;
import org.squirrelframework.foundation.fsm.UntypedStateMachine;
import org.squirrelframework.foundation.fsm.UntypedStateMachineBuilder;
import org.squirrelframework.foundation.fsm.annotation.StateMachineParameters;
import org.squirrelframework.foundation.fsm.impl.AbstractUntypedStateMachine;
import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.ecc.Curve;
import org.whispersystems.libsignal.util.KeyHelper;

import javax.crypto.BadPaddingException;
import javax.crypto.ShortBufferException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
public class NoiseHandshake {
    public interface HandshakeNotify {
        void OnLoginFail(ProtocolTreeNode node);

        void OnConnected(byte[] serverPublicKey);

        void OnDisconnected(String desc);

        void OnPush(ProtocolTreeNode node);

        void taskNotify(String username, Runnable runnable);

        void OnClearServerStaticPublic();
    }

    public static class Proxy {
        public int type;
        public String server;
        public int port;
        public String userName;
        public String password;
    }

    @Getter
    private CipherStatePair cipherKey_;

    //接收tcp 数据
    class ClientHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            OnChannelRead(ctx, msg);
        }

        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
                throws Exception {
            ctx.pipeline().remove(this);
            disconnect(ctx, cause);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            ctx.pipeline().remove(this);
            disconnect(ctx);
        }
    }

    public void disconnect(ChannelHandlerContext ctx, Throwable cause) {
        log.info("用户：{},连接断开：{}", username, ctx, cause);
        ctx.close();
        //tcp连接断开通知事件
        NotifyDisconnect(ctx.channel().toString());
    }

    public void disconnect(ChannelHandlerContext ctx) {
        log.info("用户：{},连接断开：{}", username, ctx);
        ctx.close();
        //tcp连接断开通知事件
        NotifyDisconnect(ctx.channel().toString());
    }

    public void connectFail(Channel channel, String content) {
        log.info("用户：{}, netty连接失败：{}", username, channel);
        //tcp连接断开通知事件
        if (channel != null) {
            channel.close();
        }
        notifyLoginFail(content);
    }

    void OnChannelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf newMsg = (ByteBuf) msg;
        receiveBuf_.writeBytes(newMsg);
        try {
            while (true) {
                int readableBytes = receiveBuf_.readableBytes();
                if (readableBytes < 3) {
                    //是否接收完头部数据
                    return;
                }
                byte[] lenBuffer = new byte[3];
                receiveBuf_.getBytes(0, lenBuffer);
                int bodyLen = HandshakeUtil.BodyBytesToLen(lenBuffer);
                if (readableBytes < 3 + bodyLen) {
                    //判断是否接收完body
                    return;
                }
                byte[] body = new byte[bodyLen];
                receiveBuf_.skipBytes(3);
                receiveBuf_.readBytes(body);
                receiveBuf_.discardReadBytes();
                addTaskToQueue(() -> {
                    HandleSegment(body);
                });
            }

        } finally {
            newMsg.release();
        }
    }


    void HandleSegment(byte[] body) {
        if (handshakeStateMachine_.getCurrentState() == HandshakeXXState.WaitFinish) {
            HandleXXServerHello(body);
        } else if (handshakeStateMachine_.getCurrentState() == HandshakeIKState.Finish) {
            HandleIKServerHello(body);
        } else {
            try {
                HandleReceivePacket(body);
            } catch (ShortBufferException e) {
                log.error("ShortBufferException", e);
            } catch (BadPaddingException e) {
                log.error("BadPaddingException", e);
            }
        }
    }

    //HandshakeXX StateMachine
    enum HandshakeXXEvent {
        SendClientHello,
        HandleServerHello,
        SendClientFinish,
        Notify
    }

    enum HandshakeXXState {
        Init,
        WaitFinish,
        Finish,
        ChannelReady
    }

    @StateMachineParameters(stateType = HandshakeXXState.class, eventType = HandshakeXXEvent.class, contextType = NoiseHandshake.class)
    static class HandshakeXXStateMachine extends AbstractUntypedStateMachine {
        protected void FromInitToWaitServerResponse(HandshakeXXState from, HandshakeXXState to, HandshakeXXEvent event, NoiseHandshake context) {
            log.debug("Transition from '" + from + "' to '" + to + "' on event '" + event +
                    "' with context '" + context + "'.");
            try {
                context.SendClientHello();
            } catch (ShortBufferException e) {
                log.error("ShortBufferException", e);
            }
        }

        protected void Finish(HandshakeXXState from, HandshakeXXState to, HandshakeXXEvent event, NoiseHandshake context) {
            log.debug("Transition from '" + from + "' to '" + to + "' on event '" + event +
                    "' with context '" + context + "'.");
            try {
                context.HandshakeXXFinish();
            } catch (ShortBufferException e) {
                log.error("ShortBufferException", e);
            }
        }

        protected void Notify(HandshakeXXState from, HandshakeXXState to, HandshakeXXEvent event, NoiseHandshake context) {
            log.debug("Transition from '" + from + "' to '" + to + "' on event '" + event +
                    "' with context '" + context + "'.");
            context.NotifyConnect();
        }
    }

    void SendClientHello() throws ShortBufferException {
        if (noiseHandshakeState_ == null) {
            return;
        }
        //GorgeousLooper.Instance().CheckThread();
        //1) 获取一个32 字节的公钥
        byte[] ephemeral_public_buf = new byte[32];
        noiseHandshakeState_.writeMessage(ephemeral_public_buf, 0, new byte[0], 0, 0);
        //2) 构造一个 client hello
        DeviceEnv.HandshakeMessage.Builder builder = DeviceEnv.HandshakeMessage.newBuilder();
        DeviceEnv.ClientHello.Builder clientHello = DeviceEnv.ClientHello.newBuilder();
        clientHello.setEphemeral(ByteString.copyFrom(ephemeral_public_buf));
        builder.setClientHello(clientHello);
        //3) 发送数据
        WriteSegment(builder.build().toByteArray()).addListener(future -> {
            if (future.isSuccess()) {
                //等待服务器回包，这里不需要修改状态
            } else {
                //改成login失败的
                notifyLoginFail(future.toString());
            }
        });
    }

    void HandshakeXXFinish() throws ShortBufferException {
        if (noiseHandshakeState_ == null) {
            return;
        }
        //GorgeousLooper.Instance().CheckThread();
        //1) 构造发送的payload
        byte[] payload = CreateFullPayload();
        //2) 加密数据
        // log.info("登陆设备信息十六进制：{}", HexUtil.encodeHexStr(payload));
        byte[] message = new byte[1024];
        int length = noiseHandshakeState_.writeMessage(message, 0, payload, 0, payload.length);
        // log.info("登陆设备信息Message十六进制：{}", HexUtil.encodeHexStr(message));
        //43 构造client finish
        DeviceEnv.HandshakeMessage.Builder clientFinish = DeviceEnv.HandshakeMessage.newBuilder();
        clientFinish.getClientFinishBuilder().setStatic(ByteString.copyFrom(message, 0, 48));
        clientFinish.getClientFinishBuilder().setPayload(ByteString.copyFrom(message, 48, length - 48));
        cipherKey_ = noiseHandshakeState_.split();
        //5 发送数据
        ChannelFuture channelFuture = WriteSegment(clientFinish.build().toByteArray());
        try {
            channelFuture.get(10, TimeUnit.SECONDS);
            if (channelFuture.isSuccess()) {
                handshakeStateMachine_.fire(HandshakeXXEvent.Notify, this);
                return;
            }
        } catch (Exception ignored) {
        }
        notifyLoginFail("连接失败");
    }


    void HandleXXServerHello(byte[] body) {
        if (noiseHandshakeState_ == null) {
            return;
        }
        //GorgeousLooper.Instance().CheckThread();
        try {
            DeviceEnv.HandshakeMessage serverHello = DeviceEnv.HandshakeMessage.parseFrom(body);
            if (!serverHello.hasServerHello()) {
                notifyLoginFail("hasServerHello no");
                return;
            }
            byte[] ephemerial = serverHello.getServerHello().getEphemeral().toByteArray();
            byte[] staticBuffer = serverHello.getServerHello().getStatic().toByteArray();
            byte[] serverPayload = serverHello.getServerHello().getPayload().toByteArray();
            byte[] message = new byte[ephemerial.length + staticBuffer.length + serverPayload.length];
            System.arraycopy(ephemerial, 0, message, 0, ephemerial.length);
            System.arraycopy(staticBuffer, 0, message, ephemerial.length, staticBuffer.length);
            System.arraycopy(serverPayload, 0, message, ephemerial.length + staticBuffer.length, serverPayload.length);

            byte[] payload = new byte[message.length + 1024];
            int payload_len = noiseHandshakeState_.readMessage(message, 0, message.length, payload, 0);
            //校验证书，这步骤可以不做
            CheckCertificate(payload, 0, payload_len);
            publicServerKey_ = new byte[noiseHandshakeState_.getRemotePublicKey().getPublicKeyLength()];
            noiseHandshakeState_.getRemotePublicKey().getPublicKey(publicServerKey_, 0);
            //流转下一次状态
            handshakeStateMachine_.fire(HandshakeXXEvent.SendClientFinish, this);
        } catch (Exception e) {
            notifyLoginFail(e.getLocalizedMessage());
        }
    }

    //HandshakeIK StateMachine
    enum HandshakeIKEvent {
        SendPayload,
        HandleServerHello,
        Notify
    }

    enum HandshakeIKState {
        Init,
        Finish,
        ChannelReady
    }

    @StateMachineParameters(stateType = HandshakeIKState.class, eventType = HandshakeIKEvent.class, contextType = NoiseHandshake.class)
    static class HandshakeIKStateMachine extends AbstractUntypedStateMachine {
        protected void FromInitToWaitServerResponse(HandshakeIKState from, HandshakeIKState to, HandshakeIKEvent event, NoiseHandshake context) {
            log.debug("Transition from '" + from + "' to '" + to + "' on event '" + event +
                    "' with context '" + context + "'.");
            try {
                context.SendPayload();
            } catch (ShortBufferException e) {
                log.error("ShortBufferException", e);
            }
        }

        protected void Notify(HandshakeIKState from, HandshakeIKState to, HandshakeIKEvent event, NoiseHandshake context) {
            log.debug("Transition from '" + from + "' to '" + to + "' on event '" + event +
                    "' with context '" + context + "'.");
            context.NotifyConnect();
        }
    }

    void HandleIKServerHello(byte[] body) {
        if (noiseHandshakeState_ == null) {
            return;
        }
        //GorgeousLooper.Instance().CheckThread();
        try {
            DeviceEnv.HandshakeMessage serverHello = DeviceEnv.HandshakeMessage.parseFrom(body);
            if (!serverHello.hasServerHello()) {
                notifyLoginFail("hasServerHello no");
                return;
            }
            byte[] ephemerial = serverHello.getServerHello().getEphemeral().toByteArray();
            byte[] staticBuffer = serverHello.getServerHello().getStatic().toByteArray();
            byte[] serverPayload = serverHello.getServerHello().getPayload().toByteArray();
            byte[] message = new byte[ephemerial.length + staticBuffer.length + serverPayload.length];
            System.arraycopy(ephemerial, 0, message, 0, ephemerial.length);
            System.arraycopy(staticBuffer, 0, message, ephemerial.length, staticBuffer.length);
            System.arraycopy(serverPayload, 0, message, ephemerial.length + staticBuffer.length, serverPayload.length);
            byte[] payload = new byte[message.length + 1024];
            int length = noiseHandshakeState_.readMessage(message, 0, message.length, payload, 0);
            if (length == 0) {
                //需要清理serverkey
                publicServerKey_ = null;
            }

            cipherKey_ = noiseHandshakeState_.split();
            //流转下一次状态
            handshakeStateMachine_.fire(HandshakeIKEvent.Notify, this);
        } catch (Exception e) {
            publicServerKey_ = null;
            notifyLoginFail(e.getLocalizedMessage(), true);
        }
    }

    void SendPayload() throws ShortBufferException {
        if (noiseHandshakeState_ == null) {
            return;
        }
        //GorgeousLooper.Instance().CheckThread();
        //1) 构造payload
        byte[] payload = CreateFullPayload();
        //2) 加密 payload
        byte[] message = new byte[1024];
        int length = noiseHandshakeState_.writeMessage(message, 0, payload, 0, payload.length);
        //3) 构造 client hello
        DeviceEnv.HandshakeMessage.Builder builder = DeviceEnv.HandshakeMessage.newBuilder();
        DeviceEnv.ClientHello.Builder clientHello = DeviceEnv.ClientHello.newBuilder();
        clientHello.setEphemeral(ByteString.copyFrom(message, 0, 32));
        clientHello.setStatic(ByteString.copyFrom(message, 32, 48));
        clientHello.setPayload(ByteString.copyFrom(message, 32 + 48, length - 32 - 48));
        builder.setClientHello(clientHello);
        //4) 发送数据
        WriteSegment(builder.build().toByteArray()).addListener(future -> {
            if (future.isSuccess()) {
                //等待服务器回包
            } else {
                notifyLoginFail(future.toString());
            }
        });
    }

    void HandleReceivePacket(byte[] body) throws ShortBufferException, BadPaddingException {
        if (null == noiseHandshakeState_) {
            log.info("noiseHandshakeState_ null");
            return;
        }

        byte[] receiveBuffer = new byte[body.length + 1024];
        int length = cipherKey_.getReceiver().decryptWithAd(new byte[0], body, 0, receiveBuffer, 0, body.length);
        /*//写出数据到日志
        String encode = Base64.encode(recvBuffer);
        FileUtil.appendUtf8String(encode + System.lineSeparator(), StartedUpRunner.encodeLogs);*/
        byte[] decrypt_buffer = Arrays.copyOfRange(receiveBuffer, 0, length);

        if (length > 0) {
            ProtocolTreeNode node;
            try {
                node = XmppDecode.decode(decrypt_buffer);
            } catch (Exception e) {
                log.error("用户: {}, xmpp: {}, 解析异常", username, HexUtil.encodeHexStr(decrypt_buffer), e);
                return;
            }
            if (log.isDebugEnabled()) {
                log.debug("用户: {}, 接收hex: {}", username, HexUtil.encodeHexStr(decrypt_buffer));
            }
            showLogs(node);
            if (null != notify_) {
                notify_.OnPush(node);
            }
        } else {
            log.error("接收，解密失败：{}", body.length);
        }
    }

    private void showLogs(ProtocolTreeNode node) {
        if (log.isDebugEnabled()) {
            log.debug("用户：{}，接收：{}", username, node);
        }
    }

    static EventLoopGroup eventGroup_ = new NioEventLoopGroup();
    ByteBuf receiveBuf_ = Unpooled.buffer();
    Channel socketChannel_;
    HandshakeNotify notify_;
    Proxy proxy_;
    HandshakeState noiseHandshakeState_;
    UntypedStateMachine handshakeStateMachine_;
    Env.DeviceEnv.AndroidEnv env_;
    byte[] publicServerKey_ = null;
    private String username;
    private boolean businessVersion;
    private boolean iosLogin;
    private String waVersion;
    private String versionCode;
    private TaskNotify taskNotify;
    private MccMnc mccMnc;
    private static final byte[] HEADER = {69, 68, 0, 1};


    public NoiseHandshake(HandshakeNotify notify, Proxy proxy, TaskNotify taskNotify, MccMnc mccMnc) {
        notify_ = notify;
        proxy_ = proxy;
        this.taskNotify = taskNotify;
        this.mccMnc = mccMnc;
    }

    //开始进行noise 握手， 为了方便同步收发数据，这里简单在一个线程进行
    public void StartNoiseHandShake(Env.DeviceEnv.AndroidEnv env, String username, boolean iosLogin, boolean businessVersion, String waVersion) {
        this.username = username;
        this.businessVersion = businessVersion;
        this.iosLogin = iosLogin;
        this.waVersion = waVersion;
        this.versionCode = getVersionCode();
        //String server = "34.192.181.12";
        String server = "g.whatsapp.net";
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(eventGroup_)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        if (proxy_ != null) {
                            if (proxy_.type == 0) {
                                if (StringUtil.isEmpty(proxy_.userName)) {
                                    ch.pipeline().addFirst(new HttpProxyHandler(new InetSocketAddress(proxy_.server, proxy_.port)));
                                } else {
                                    ch.pipeline().addFirst(new HttpProxyHandler(new InetSocketAddress(proxy_.server, proxy_.port), proxy_.userName, proxy_.password));
                                }
                            } else {
                                if (StringUtil.isEmpty(proxy_.userName)) {
                                    ch.pipeline().addFirst(new Socks5ProxyHandler(new InetSocketAddress(proxy_.server, proxy_.port)));
                                } else {
                                    ch.pipeline().addFirst(new Socks5ProxyHandler(new InetSocketAddress(proxy_.server, proxy_.port), proxy_.userName, proxy_.password));
                                }
                            }
                        }
                        ch.pipeline().addLast(new ReadTimeoutHandler(195, TimeUnit.SECONDS));
                        ch.pipeline().addLast(new WriteTimeoutHandler(195, TimeUnit.SECONDS));
                        ch.pipeline().addLast(new ClientHandler());
                    }
                });
        if (proxy_ != null) {
            bootstrap.resolver(NoopAddressResolverGroup.INSTANCE);
        } else {
            bootstrap.resolver(DefaultAddressResolverGroup.INSTANCE);
        }
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);
        ChannelFuture future = bootstrap.connect(server, 443);
        socketChannel_ = future.channel();
        future.addListener(connectFuture -> {
            if (connectFuture.isSuccess()) {
                env_ = env;
                //连接成功开始走handshake流程
                HandleNoiseHandshake();
            } else {
                connectFail(socketChannel_, connectFuture.toString());
            }
        });
    }

    public void StopNoiseHandShake() {
        try {
            if (socketChannel_ != null) {
                socketChannel_.close();
                socketChannel_ = null;
            }
            //receiveBuf_ = Unpooled.buffer();
            ReferenceCountUtil.release(receiveBuf_);
            noiseHandshakeState_ = null;
        } catch (Throwable throwable) {
            log.error("停止握手异常", throwable);
        }

    }

    public String SendNode(ProtocolTreeNode node, ConcurrentHashMap<String, GorgeousEngine.NodeHandleInfo> registerHandleMap_) {
        return SendNode(node, registerHandleMap_, NodeTaskType.DEFAULT);
    }

    public String SendNode(ProtocolTreeNode node, ConcurrentHashMap<String, GorgeousEngine.NodeHandleInfo> registerHandleMap_, NodeTaskType taskType, Runnable runnable) {
        addTaskToQueue(() -> {
            if (null == noiseHandshakeState_) {
                log.info("用户: {}, noiseHandshakeState_ null", username);
                String taskId = node.IqId();
                if (StringUtils.hasLength(taskId)) {
                    taskNotify.setEventContent(taskId, ProtocolTreeNode.fail(Constant.FAIL));
                }
                return;
            }
            try {
                String taskId = node.IqId();
                removeTag(node);
                //log.debug( "发送：" + node.toString());
                byte[] data;
                try {
                    data = XmppEncode.encode(node);
                } catch (Exception e) {
                    log.error("用户: {}, xmpp编码异常: {}", username, node, e);
                    if (StringUtils.hasLength(taskId)) {
                        registerHandleMap_.remove(taskId);
                        taskNotify.setEventContent(taskId, ProtocolTreeNode.fail(Constant.FAIL));
                    }
                    return;
                }
                if (data.length == 0) {
                    if (StringUtils.hasLength(taskId)) {
                        taskNotify.setEventContent(taskId, ProtocolTreeNode.fail(Constant.FAIL));
                    }
                    return;
                }

                byte[] cipherText = new byte[data.length + 1024];
                int length = cipherKey_.getSender().encryptWithAd(new byte[0], data, 0, cipherText, 0, data.length);
                runnable.run();
                ChannelFuture channelFuture = WriteSegment(cipherText, length);
                channelFuture.addListener((ChannelFutureListener) future -> {
                    boolean success = channelFuture.isSuccess();
                    if (!success) {
                        if (StringUtils.hasLength(taskId)) {
                            taskNotify.setEventContent(taskId, ProtocolTreeNode.fail(Constant.FAIL));
                        }
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("用户: {}, 发送hex: {}", username, HexUtil.encodeHexStr(data));
                            log.debug("用户: {}，发送: {}", username, node);
                        }
                        //处理发送包没有回复内容的接口
                        if (taskType == NodeTaskType.MODIFY_NICKNAME) {
                            taskNotify.setEventContent(taskId, ProtocolTreeNode.success(Constant.OK));
                        }
                    }
                });
            } catch (Exception e) {
                String taskId = node.IqId();
                if (StringUtils.hasLength(taskId)) {
                    registerHandleMap_.remove(taskId);
                    taskNotify.setEventContent(taskId, ProtocolTreeNode.fail(Constant.FAIL));
                }
            }
        });
        return node.IqId();
    }

    public String SendNode(ProtocolTreeNode node, ConcurrentHashMap<String, GorgeousEngine.NodeHandleInfo> registerHandleMap_, NodeTaskType taskType) {
        addTaskToQueue(() -> {
            if (null == noiseHandshakeState_) {
                log.info("用户: {}, noiseHandshakeState_ null", username);
                String taskId = node.IqId();
                if (StringUtils.hasLength(taskId)) {
                    registerHandleMap_.remove(taskId);
                    taskNotify.setEventContent(taskId, ProtocolTreeNode.fail(Constant.FAIL));
                }
                return;
            }
            try {
                String taskId = node.IqId();
                removeTag(node);
                //log.debug( "发送：" + node.toString());
                byte[] data;
                try {
                    data = XmppEncode.encode(node);
                } catch (Exception e) {
                    log.error("用户: {}, xmpp编码异常: {}", username, node, e);
                    if (StringUtils.hasLength(taskId)) {
                        registerHandleMap_.remove(taskId);
                        taskNotify.setEventContent(taskId, ProtocolTreeNode.fail(Constant.FAIL));
                    }
                    return;
                }
                if (data.length == 0) {
                    if (StringUtils.hasLength(taskId)) {
                        registerHandleMap_.remove(taskId);
                        taskNotify.setEventContent(taskId, ProtocolTreeNode.fail(Constant.FAIL));
                    }
                    return;
                }

                byte[] cipherText = new byte[data.length + 1024];
                int length = cipherKey_.getSender().encryptWithAd(new byte[0], data, 0, cipherText, 0, data.length);
                ChannelFuture channelFuture = WriteSegment(cipherText, length);
                channelFuture.addListener((ChannelFutureListener) future -> {
                    boolean success = channelFuture.isSuccess();
                    if (!success) {
                        if (StringUtils.hasLength(taskId)) {
                            taskNotify.setEventContent(taskId, ProtocolTreeNode.fail(Constant.FAIL));
                        }
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("用户: {}, 发送hex: {}", username, HexUtil.encodeHexStr(data));
                            log.debug("用户: {}，发送: {}", username, node);
                        }
                        //处理发送包没有回复内容的接口
                        if (taskType == NodeTaskType.MODIFY_NICKNAME) {
                            taskNotify.setEventContent(taskId, ProtocolTreeNode.success(Constant.OK));
                        }
                    }
                });
            } catch (Exception e) {
                String taskId = node.IqId();
                if (StringUtils.hasLength(taskId)) {
                    registerHandleMap_.remove(taskId);
                    taskNotify.setEventContent(taskId, ProtocolTreeNode.fail(Constant.FAIL));
                }
            }
        });
        return node.IqId();
    }

    public void sendXmpp(ProtocolTreeNode node, byte[] data, Consumer<Boolean> consumer) {
        addTaskToQueue(() -> {
            try {
                byte[] cipherText = new byte[data.length + 1024];
                int length = cipherKey_.getSender().encryptWithAd(new byte[0], data, 0, cipherText, 0, data.length);
                ChannelFuture channelFuture = WriteSegment(cipherText, length);
                channelFuture.addListener((ChannelFutureListener) future -> {
                    boolean success = channelFuture.isSuccess();
                    if (!success) {
                        consumer.accept(false);
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("用户: {}, 发送hex: {}", username, HexUtil.encodeHexStr(data));
                            log.debug("用户: {}，发送: {}", username, node);
                        }
                        consumer.accept(true);
                    }
                });
            } catch (Exception e) {
                consumer.accept(false);
            }
        });

    }

    void HandleNoiseHandshake() throws IOException, NoSuchAlgorithmException, ShortBufferException, BadPaddingException {
        // 连接成功,发送初始化信息
        byte[] prologue = getWaInfo();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        // 获取并处理routing_info，只有在非空且有效时才添加
        byte[] routingInfo = null;
        if (env_.hasEdgeRoutingInfo()) {
            routingInfo = env_.getEdgeRoutingInfo().toByteArray();
        }

        if (routingInfo != null && routingInfo.length > 0) {
            byteArrayOutputStream.write(HEADER);
            byteArrayOutputStream.write(new byte[]{
                    (byte) (routingInfo.length >> 16),
                    (byte) (routingInfo.length >> 8),
                    (byte) (routingInfo.length)
            });
            byteArrayOutputStream.write(routingInfo);
        }

        // 添加序言信息
        byteArrayOutputStream.write(prologue);
        byte[] initData = byteArrayOutputStream.toByteArray();

        ChannelFuture future = socketChannel_.writeAndFlush(Unpooled.copiedBuffer(initData));
        future.addListener(sendFuture -> {
            if (sendFuture.isSuccess()) {
                boolean hasServerStaticPublic = env_.hasServerStaticPublic();
                if (!hasServerStaticPublic) {
                    addTaskToQueue(() -> {
                        try {
                            HandshakeXX();
                        } catch (NoSuchAlgorithmException e) {
                            log.error("NoSuchAlgorithmException", e);
                            notifyLoginFail("失败");
                        }
                    });
                } else {
                    addTaskToQueue(() -> {
                        try {
                            HandshakeIK();
                        } catch (NoSuchAlgorithmException e) {
                            log.error("NoSuchAlgorithmException", e);
                            notifyLoginFail("失败");
                        }
                    });
                }
            } else {
                notifyLoginFail(sendFuture.toString());
            }
        });
    }


    void HandshakeXX() throws NoSuchAlgorithmException {
        //GorgeousLooper.Instance().CheckThread();
        noiseHandshakeState_ = new HandshakeState("Noise_XX_25519_AESGCM_SHA256", 1);
        //开始握手
        byte[] WA = getWaInfo();
        noiseHandshakeState_.setPrologue(WA, 0, WA.length);
        if (noiseHandshakeState_.needsLocalKeyPair()) {
            noiseHandshakeState_.getLocalKeyPair().setPublicKey(env_.getClientStaticKeyPair().getStrPubKey().toByteArray(), 0);
            noiseHandshakeState_.getLocalKeyPair().setPrivateKey(env_.getClientStaticKeyPair().getStrPrivateKey().toByteArray(), 0);
        }
        noiseHandshakeState_.start();
        //组装状态机
        UntypedStateMachineBuilder builder = StateMachineBuilderFactory.create(HandshakeXXStateMachine.class);
        builder.externalTransition().from(HandshakeXXState.Init).to(HandshakeXXState.WaitFinish).on(HandshakeXXEvent.SendClientHello).callMethod("FromInitToWaitServerResponse");
        builder.externalTransition().from(HandshakeXXState.WaitFinish).to(HandshakeXXState.Finish).on(HandshakeXXEvent.SendClientFinish).callMethod("Finish");
        builder.externalTransition().from(HandshakeXXState.Finish).to(HandshakeXXState.ChannelReady).on(HandshakeXXEvent.Notify).callMethod("Notify");
        handshakeStateMachine_ = builder.newStateMachine(HandshakeXXState.Init);
        handshakeStateMachine_.start();
        handshakeStateMachine_.fire(HandshakeXXEvent.SendClientHello, this);
    }

    void HandshakeIK() throws NoSuchAlgorithmException {
        //GorgeousLooper.Instance().CheckThread();
        noiseHandshakeState_ = new HandshakeState("Noise_IK_25519_AESGCM_SHA256", 1);
        byte[] WA = getWaInfo();
        noiseHandshakeState_.setPrologue(WA, 0, WA.length);
        if (noiseHandshakeState_.needsLocalKeyPair()) {
            noiseHandshakeState_.getLocalKeyPair().setPublicKey(env_.getClientStaticKeyPair().getStrPubKey().toByteArray(), 0);
            noiseHandshakeState_.getLocalKeyPair().setPrivateKey(env_.getClientStaticKeyPair().getStrPrivateKey().toByteArray(), 0);
        }
        if (noiseHandshakeState_.needsRemotePublicKey()) {
            noiseHandshakeState_.getRemotePublicKey().setPublicKey(env_.getServerStaticPublic().toByteArray(), 0);
        }
        noiseHandshakeState_.start();
        //组装状态机
        UntypedStateMachineBuilder builder = StateMachineBuilderFactory.create(HandshakeIKStateMachine.class);
        builder.externalTransition().from(HandshakeIKState.Init).to(HandshakeIKState.Finish).on(HandshakeIKEvent.SendPayload).callMethod("FromInitToWaitServerResponse");
        builder.externalTransition().from(HandshakeIKState.Finish).to(HandshakeIKState.ChannelReady).on(HandshakeIKEvent.Notify).callMethod("Notify");
        handshakeStateMachine_ = builder.newStateMachine(HandshakeIKState.Init);
        handshakeStateMachine_.start();
        handshakeStateMachine_.fire(HandshakeIKEvent.SendPayload, this);
    }

    ChannelFuture WriteSegment(byte[] data) {
        return WriteSegment(data, data.length);
    }

    ChannelFuture WriteSegment(byte[] data, int length) {
        ByteBuf buffer = Unpooled.buffer(3 + length);
        buffer.writeBytes(HandshakeUtil.GenerateDataHead(length));
        buffer.writeBytes(data, 0, length);
        return socketChannel_.writeAndFlush(buffer);
    }

    void notifyLoginFail(String desc) {
        addTaskToQueue(() -> {
            if (null != notify_) {
                notify_.OnLoginFail(ProtocolTreeNode.fail("loginFail", desc));
            }
        });
    }

    void notifyLoginFail(String desc, boolean clearServerKey) {
        addTaskToQueue(() -> {
            if (null != notify_) {
                if (clearServerKey) {
                    notify_.OnClearServerStaticPublic();
                }
                notify_.OnLoginFail(ProtocolTreeNode.fail("loginFail", desc));
            }
        });
    }

    void NotifyDisconnect(String detail) {
        addTaskToQueue(() -> {
            if (null != notify_) {
                notify_.OnDisconnected(detail);
            }
        });
    }

    void NotifyConnect() {
        if (noiseHandshakeState_ == null) {
            return;
        }
        if (null != notify_) {
            notify_.OnConnected(publicServerKey_);
        }
    }

    boolean CheckCertificate(byte[] payload, int offset, int length) {
        try {
            DeviceEnv.NoiseCertificate certificate = DeviceEnv.NoiseCertificate.parseFrom(ByteBuffer.wrap(payload, 0, length));
            DeviceEnv.CertificateDetails details = DeviceEnv.CertificateDetails.parseFrom(certificate.getDetails());
            assert details.getIssuer() == "WhatsAppLongTerm1";
            return Curve.verifySignature(new IdentityKey(HandshakeConfig.PUBLIC_KEY, 0).getPublicKey(), certificate.getDetails().toByteArray(), certificate.getSignature().toByteArray());
        } catch (Exception e) {
            log.error("CheckCertificate", e);
        }
        return false;
    }

    byte[] CreateFullPayload() {
        DeviceEnv.ClientPayload.Builder clientPayload = DeviceEnv.ClientPayload.newBuilder();
        clientPayload.setUsername(Long.parseLong(env_.getFullphone()));
        clientPayload.setPassive(env_.getPassive());
        DeviceEnv.UserAgent userAgent = env_.getUserAgent();
        clientPayload.setUserAgent(userAgent);
        if (iosLogin) {
            DeviceEnv.UserAgent.Builder builder = userAgent.toBuilder();
            String iosDeviceModelType = DeviceUtil.getIOSDeviceModelType(builder.getDevice());
            if (StrUtil.isNotEmpty(iosDeviceModelType)) {
                builder.setDeviceModelType(iosDeviceModelType);
            }
            DeviceEnv.AppVersion.Builder appVersionBuilder = builder.getAppVersion().toBuilder();
            appVersionBuilder.setQuinary(0);
            builder.setAppVersion(appVersionBuilder);
            clientPayload.setUserAgent(builder);
            clientPayload.setPull(false);
        }
        // android.os.Build.BOARD
        String cpuChip = env_.getUserAgent().getCpuChip();

        if (StringUtils.isEmpty(cpuChip) && !iosLogin) {
            cpuChip = "SM-A805N";
        }
        if (StringUtils.hasLength(cpuChip)) {
            clientPayload.getUserAgentBuilder().setCpuChip(cpuChip);
        }
        if (ObjectUtil.isNotNull(mccMnc)) {
            clientPayload.getUserAgentBuilder().setMcc(mccMnc.getMcc());
            clientPayload.getUserAgentBuilder().setMnc(mccMnc.getMnc());
        }
        String pushName = env_.getPushname();
        clientPayload.setPushName(pushName);
        clientPayload.setSessionId(KeyHelper.getRandomSequence(Integer.MAX_VALUE));
        clientPayload.setShortConnect(true);
        clientPayload.setConnectType(DeviceEnv.ConnectType.WIFI_UNKNOWN);
        clientPayload.setConnectReason(DeviceEnv.ConnectReason.USER_ACTIVATED);
        clientPayload.setOc(true);
        clientPayload.getUserAgentBuilder().setDeviceType(DeviceEnv.DeviceType.PHONE);
        clientPayload.setProduct(DeviceEnv.Product.WHATSAPP);
        DeviceEnv.DNSSource.Builder dnsBuilder = DeviceEnv.DNSSource.newBuilder();
        dnsBuilder.setDnsMethod(DeviceEnv.DNSSource.DNSResolutionMethod.SYSTEM);
        if (iosLogin) {
            clientPayload.setConnectAttemptCount(0);
            clientPayload.setDevice(0);
            clientPayload.getUserAgentBuilder().clearDeviceExpId();
            clientPayload.getUserAgentBuilder().clearCpuChip();
            clientPayload.setOc(false);
            clientPayload.setPull(false);

        } else {
            clientPayload.setMemoryTotal(2015);
            clientPayload.setMemoryClass(256);
            dnsBuilder.setAppCached(false);
        }
        clientPayload.setDns(dnsBuilder);
        if (!iosLogin) {
            clientPayload.setConnectionLc(env_.getConnectionLc());
        }
        if (businessVersion) {
            //商业版需要修改地方
            clientPayload.getDnsBuilder().setDnsMethod(DeviceEnv.DNSSource.DNSResolutionMethod.SYSTEM);
        }
        /*if (env_.hasRegdata()) {
            clientPayload.setRegData(env_.getRegdata());
        }*/
        byte[] bytes = clientPayload.build().toByteArray();
        if (log.isDebugEnabled()) {
            log.debug("用户：{}，登陆的payload：{}", username, HexUtil.encodeHexStr(bytes));
        }
        return clientPayload.build().toByteArray();
    }

    public void addTaskToQueue(Runnable runnable) {
        if (null != notify_) {
            notify_.taskNotify(this.username, runnable);
        }
    }

    private void removeTag(ProtocolTreeNode node) {
        String taskTag = node.taskTag();
        if (StringUtils.hasLength(taskTag)) {
            if (TypeConstant.TaskType.SET_NAME.equals(taskTag)) {
                node.removeAttribute(Arrays.asList("id", Constant.TASK_TAG));
            }
        }
    }

    private byte[] getWaInfo() {
        if ("53".equals(versionCode)) {
            return new byte[]{(byte) 'W', (byte) 'A', (byte) 5, (byte) 3};
        } else {
            return new byte[]{(byte) 'W', (byte) 'A', (byte) 6, (byte) 3};
        }
    }

    private String getVersionCode() {
        if (iosLogin) {
            if (WhatsAppUtils.isVersionLessThan(waVersion, "2.23.25.81")) {
                return "53";
            }
            return "63";
        }
        if (businessVersion) {
            if (WhatsAppUtils.isVersionLessThan(waVersion, "2.23.23.78")) {
                return "53";
            }
            return "63";
        }
        if (WhatsAppUtils.isVersionLessThan(waVersion, "2.23.24.75")) {
            return "53";
        }
        return "63";
    }
}
