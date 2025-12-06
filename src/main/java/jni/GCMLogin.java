package jni;

import Handshake.NoiseHandshake;
import Util.StringUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.RandomUtil;
import com.google.protobuf.ByteString;
import com.whatsapp.android.constant.Constant;
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
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.netty.resolver.NoopAddressResolverGroup;
import io.netty.util.concurrent.ScheduledFuture;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.microg.gms.gcm.mcs.Mcs;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

@Slf4j
public class GCMLogin {
    private static final String HOST = "mtalk.google.com";
    private static final int PORT = 5228;
    private volatile boolean started = false;
    public int streamId = 0;
    private ScheduledFuture<?> scheduledFuture;
    @Getter
    public Channel socketChannel_;
    static EventLoopGroup eventGroup_ = new NioEventLoopGroup();
    String login_param_;
    NoiseHandshake.Proxy proxy_;
    private boolean initialized;
    private int version = -1;
    private final ByteBuf messageBuffer = Unpooled.buffer();
    private final GcmEvent gcmEvent;
    private String gcmPersistentIdValue;

    public interface GcmEvent {
        void OnGcmConnectSuccess();

        /**
         * 收到消息
         */
        void OnGcmMessage(int mcsTag, byte[] message, int streamId);

        /**
         * gcm关闭
         */
        void OnGcmClose(String reason, boolean force);
    }

    public GCMLogin(NoiseHandshake.Proxy proxy_, GcmEvent gcmEvent) {
        this.proxy_ = proxy_;
        this.gcmEvent = gcmEvent;
    }

    public void StartListen(String param, String androidId, String securityToken, GcmEvent gcmEvent, String gcmPersistentIdValue) {
        login_param_ = param;
        this.gcmPersistentIdValue = gcmPersistentIdValue;
        Bootstrap bootstrap = new Bootstrap();
        log.info("mtalk ip:{}", proxy_.server);
        bootstrap.group(eventGroup_)
                .channel(NioSocketChannel.class)
                .resolver(NoopAddressResolverGroup.INSTANCE)
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
                        try {
                            SSLEngine sslEngine = createSSLEngine();
                            ch.pipeline().addLast("ssl", new SslHandler(sslEngine));
                        } catch (Exception e) {
                            log.error("ssl error:", e);
                            throw new RuntimeException();
                        }
                        ch.pipeline().addLast(new ReadTimeoutHandler(60, TimeUnit.SECONDS));
                        ch.pipeline().addLast(new WriteTimeoutHandler(60, TimeUnit.SECONDS));
                        ch.pipeline().addLast(new GCMLogin.ClientHandler());
                    }
                });
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        ChannelFuture future = bootstrap.connect(HOST, PORT);
        socketChannel_ = future.channel();
        future.addListener(connectFuture -> {
            if (connectFuture.isSuccess()) {
                //连接成功开始走handshake流程
                DoStartListen(androidId, securityToken);
                gcmEvent.OnGcmConnectSuccess();
            } else {
                disconnect(socketChannel_, "gcm连接失败");
            }
        });

    }

    private static SSLEngine createSSLEngine() throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[]{new TrustAllManager()}, null);
        SSLEngine sslEngine = sslContext.createSSLEngine();
        sslEngine.setUseClientMode(true);
        return sslEngine;

    }

    void DoStartListen(String androidId, String securityToken) {
        byte[] decode = loginRequestPack(androidId, securityToken);
        log.debug("gcm发送的hex：{}", HexUtil.encodeHexStr(decode));
        ByteBuf buffer = Unpooled.buffer(decode.length);
        buffer.writeBytes(decode, 0, decode.length);
        socketChannel_.writeAndFlush(buffer);
    }

    void OnChannelRead(ChannelHandlerContext ctx, Object msg) {
        // 处理接收到的消息
        ByteBuf data = (ByteBuf) msg;
        try {
            byte[] byteArray = new byte[data.readableBytes()];
            data.readBytes(byteArray);
            handler(byteArray);
        } finally {
            data.release(); // 释放资源，防止内存泄漏
        }

    }

    public void handler(byte[] data) {
        messageBuffer.writeBytes(data);
        ensureVersionRead();
        try {
            while (messageBuffer.isReadable()) {
                messageBuffer.markReaderIndex();
                int mcsTag = messageBuffer.readUnsignedByte();
                int mcsSize = readVarInt();
                if (mcsTag < 0 || mcsSize < 0) {
                    messageBuffer.resetReaderIndex();
                    return;
                }
                if (messageBuffer.readableBytes() < mcsSize) {
                    // 不足以读取完整的消息体，重置读取位置并返回等待更多的数据
                    messageBuffer.resetReaderIndex();
                    return;
                }
                byte[] messageBytes = new byte[mcsSize];
                messageBuffer.readBytes(messageBytes);
                streamId++;
                gcmEvent.OnGcmMessage(mcsTag, messageBytes, streamId);
            }
        } catch (Exception e) {
            messageBuffer.resetReaderIndex();
        }

    }


    //接收tcp 数据
    class ClientHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            OnChannelRead(ctx, msg);
        }

        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
                throws Exception {
            ctx.pipeline().remove(this);
            disconnect(ctx);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            ctx.pipeline().remove(this);
            disconnect(ctx);
        }
    }

    public void disconnect(Channel channel, String content) {
        // log.info("断开链接2");
        //tcp连接断开通知事件
        if (channel != null) {
            channel.close();
        }
        if (scheduledFuture != null) {
            cancelScheduled();
        }
        gcmEvent.OnGcmClose(content, false);
    }

    public void disconnect(ChannelHandlerContext ctx) {
        // log.info("断开链接1");
        ctx.close();
        if (scheduledFuture != null) {
            cancelScheduled();
        }
        gcmEvent.OnGcmClose("断开连接", false);
    }

    public void close() {
        try {
            if (scheduledFuture != null) {
                cancelScheduled();
            }
            socketChannel_.close();
        } catch (Exception ignored) {
        }
    }

    private static class TrustAllManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            // Accept all client certificates
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            // Accept all server certificates
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

    public void scheduleHeartbeat() {
        if (!started) {
            // 使用定时任务定期发送心跳消息
            scheduledFuture = socketChannel_.eventLoop().scheduleAtFixedRate(this::sendHeartbeat, 0, 30, TimeUnit.SECONDS);
            started = true;
        }
    }

    public void cancelScheduled() {
        scheduledFuture.cancel(true);
    }

    private void sendHeartbeat() {
        // 发送心跳消息
        Mcs.HeartbeatPing.Builder builder = Mcs.HeartbeatPing.newBuilder();
        builder.setStreamId(streamId);
        byte[] bytes = builder.build().toByteArray();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            byteArrayOutputStream.write(Constant.MCS_HEARTBEAT_PING_TAG);
            byteArrayOutputStream.write(WhatsAppUtils.intToVarInt(bytes.length));
            byteArrayOutputStream.write(bytes);
        } catch (Exception ignored) {
        }
        byte[] msg = byteArrayOutputStream.toByteArray();
        ByteBuf buffer = Unpooled.buffer(msg.length);
        buffer.writeBytes(msg, 0, msg.length);
        log.debug("gcm发送心跳：{}", HexUtil.encodeHexStr(msg));
        socketChannel_.writeAndFlush(buffer);
    }

    private byte[] loginRequestPack(String androidId, String securityToken) {
        Mcs.LoginRequest.Builder builder = Mcs.LoginRequest.newBuilder();
        int version;
        if (Constant.GCM_INFO == null) {
            version = 230775003;
        } else {
            version = Constant.GCM_INFO.getVersion();
        }
        byte[] bytes = builder.setId("android-" + version)
                .setDomain("mcs.android.com")
                .setUser(androidId)
                .setResource(androidId)
                .setAuthToken(securityToken)
                .setDeviceId("android-" + RandomUtil.randomString("0123456789abcdef", 16))
                .addSetting(Mcs.Setting.newBuilder().setName("new_vc").setValue("1").build())
                .setAdaptiveHeartbeat(false)
                .setUseRmq2(true)
                .setAuthService(Mcs.LoginRequest.AuthService.ANDROID_ID)
                .addReceivedPersistentId(this.gcmPersistentIdValue)
                .setNetworkType(1).build()
                .toByteArray();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            byteArrayOutputStream.write(41);
            byteArrayOutputStream.write(Constant.GCM_LOGIN_AUTH_TAG);
            byteArrayOutputStream.write(WhatsAppUtils.intToVarInt(bytes.length));
            byteArrayOutputStream.write(bytes);
        } catch (Exception ignored) {
        }
        return byteArrayOutputStream.toByteArray();
    }

    public void sendMsgAck(int streamId) {
        /**
         * type: SET
         * id: ""
         * extension {
         *   id: 13
         *   data: ""
         * }
         * last_stream_id_received: 3
         * status: 0
         */
        Mcs.IqStanza.Builder builder = Mcs.IqStanza.newBuilder();
        builder.setType(Mcs.IqStanza.IqType.SET);
        builder.setId("");
        Mcs.Extension.Builder extensionBuilder = Mcs.Extension.newBuilder();
        extensionBuilder.setId(13);
        extensionBuilder.setData(ByteString.EMPTY);
        builder.setExtension(extensionBuilder);
        builder.setLastStreamIdReceived(streamId);
        builder.setStatus(0);
        byte[] bytes = builder.build().toByteArray();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            byteArrayOutputStream.write(Constant.MCS_MSG_ACK);
            byteArrayOutputStream.write(WhatsAppUtils.intToVarInt(bytes.length));
            byteArrayOutputStream.write(bytes);
        } catch (Exception ignored) {
        }
        byte[] msg = byteArrayOutputStream.toByteArray();
        ByteBuf buffer = Unpooled.buffer(msg.length);
        buffer.writeBytes(msg, 0, msg.length);
        log.debug("gcm发送消息确认ack：{}", HexUtil.encodeHexStr(msg));
        socketChannel_.writeAndFlush(buffer);
    }

    private byte[] gcmMsgWrap(byte[]... chunks) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        for (byte[] bytes : chunks) {
            try {
                byteArrayOutputStream.write(bytes);
            } catch (Exception ignored) {
            }
        }
        return byteArrayOutputStream.toByteArray();
    }

    private synchronized void ensureVersionRead() {
        if (!initialized) {
            version = messageBuffer.readUnsignedByte();
            initialized = true;
            //发送心跳
            scheduleHeartbeat();
        }
    }

    private int readVarInt() {
        int res = 0, s = -7, read;
        do {
            res |= ((read = messageBuffer.readUnsignedByte()) & 0x7F) << (s += 7);
        } while (read >= 0 && (read & 0x80) == 0x80 && s < 32);
        if (read < 0) return -1;
        return res;
    }
}

