package jni;

import Handshake.NoiseHandshake;
import Util.StringUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Slf4j
public class GC {
    public void StartListen(String param, NoiseHandshake.Proxy proxy){
        login_param_ = param;
        proxy_ = proxy;
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(eventGroup_)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    private SSLEngine createSSLEngine() {
                        SSLContext sslContext = null;
                        try {
                            sslContext = SSLContext.getDefault();
                        } catch (NoSuchAlgorithmException e) {
                            e.printStackTrace();
                        }
                        SSLEngine sslEngine = sslContext.createSSLEngine();
                        sslEngine.setUseClientMode(true);
                        return sslEngine;
                    }
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        if (proxy != null) {
                            if (proxy.type == 0) {
                                if (StringUtil.isEmpty(proxy.userName)) {
                                    ch.pipeline().addFirst(new HttpProxyHandler(new InetSocketAddress(proxy.server, proxy.port)));
                                } else {
                                    ch.pipeline().addFirst(new HttpProxyHandler(new InetSocketAddress(proxy.server, proxy.port), proxy.userName, proxy.password));
                                }
                            } else {
                                if (StringUtil.isEmpty(proxy.userName)) {
                                    ch.pipeline().addFirst(new Socks5ProxyHandler(new InetSocketAddress(proxy.server, proxy.port)));
                                } else {
                                    ch.pipeline().addFirst(new Socks5ProxyHandler(new InetSocketAddress(proxy.server, proxy.port), proxy.userName, proxy.password));
                                }
                            }
                        }
                        try {
                            SslProvider provider =
                                    SslProvider.isAlpnSupported(SslProvider.OPENSSL)? SslProvider.OPENSSL : SslProvider.JDK;
                            final SslContext sslContext = SslContextBuilder.forClient()
                                    .sslProvider(provider)
                                    .protocols("TLSv1.3", "TLSv1.2")
                                    .build();

                            ch.pipeline().addLast("ssl", sslContext.newHandler(ch.alloc(),"mtalk.google.com", 5228));
                        }
                        catch (Exception e) {
                            System.out.printf(e.getMessage());
                        }

                        ch.pipeline().addLast(new ReadTimeoutHandler(45, TimeUnit.SECONDS));
                        ch.pipeline().addLast(new WriteTimeoutHandler(45, TimeUnit.SECONDS));
                        ch.pipeline().addLast(new GC.ClientHandler());
                    }
                });
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);

        ChannelFuture future = bootstrap.connect("mtalk.google.com", 5228);
        socketChannel_ = future.channel();
        future.addListener(connectFuture -> {
            if (connectFuture.isSuccess()) {
                //连接成功开始走handshake流程
                DoStartListen();
            } else {
                disconnect(socketChannel_, connectFuture.toString());
            }
        });
    }


    void DoStartListen() {
        instance_ = Register.CreateGCMClient(login_param_);
        String proxyType = "";
        String proxy_server = "";
        int port = 0;
        String username = "";
        String password = "";
        if (proxy_ != null) {
            if (proxy_.type == 0) {
                proxyType = "http";
            } else {
                proxyType = "socks5";
            }
            if(proxy_.server != null) {
                proxy_server = proxy_.server;
            }
            port = proxy_.port;
            if (proxy_.userName != null) {
                username = proxy_.userName;
                password = proxy_.password;
            }
        }

        String initdata = Register.InitData(instance_, proxyType, proxy_server, port, username, password);
        socketChannel_.writeAndFlush(Base64.getDecoder().decode(initdata));
    }

    void OnChannelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf newMsg = (ByteBuf) msg;
        try {
            Register.Feed(instance_, newMsg.array());
        } finally {
            newMsg.release();
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
        //tcp连接断开通知事件
        if (channel != null) {
            channel.close();
        }
        Uninit();
    }

    public void disconnect(ChannelHandlerContext ctx) {
        ctx.close();
        Uninit();
    }

    //mtalk.google.com 5228

    public void Uninit() {
        if (instance_ != 0) {
            Register.DestroyGCMClient(instance_);
            instance_ = 0;
        }
    }
    Channel socketChannel_;
    static EventLoopGroup eventGroup_ = new NioEventLoopGroup();
    long instance_;
    String login_param_;
    NoiseHandshake.Proxy proxy_;
}

