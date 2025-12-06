package com.imx.netty.core;

import Util.StringUtil;
import com.imx.netty.chain.ChainContext;
import com.imx.netty.ssl.SslContextProvider;
import com.whatsapp.android.entity.ProxyInfo;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.netty.resolver.NoopAddressResolverGroup;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLParameters;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class NettyClient {

    private Bootstrap bootstrap;
    @Setter
    private String host;
    private final int port;
    private final SslContext sslContext;
    private final List<ConnectedInvocation> connectedCallBacks;
    private final APNsClientHandler clientHandler;
    private final ProxyInfo proxyInfo;
    static EventLoopGroup eventGroup = new NioEventLoopGroup();

    public NettyClient(String host, int port, ProxyInfo proxyInfo, ChainContext chainContext) throws Exception {
        this.sslContext = SslContextProvider.SSL_CONTEXT;
        this.host = host;
        this.port = port;
        this.proxyInfo = proxyInfo;
        this.connectedCallBacks = new ArrayList<>();
        this.clientHandler = new APNsClientHandler(connectedCallBacks, chainContext);
    }

    public void init() {
        this.bootstrap = new Bootstrap();
        bootstrap.group(eventGroup)
                .channel(NioSocketChannel.class)
                .resolver(NoopAddressResolverGroup.INSTANCE)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        if (proxyInfo != null) {
                            addProxyInfo(proxyInfo, ch);
                        }
                        ch.pipeline()
                                .addLast(getSslHandler(ch))
                                .addLast(new APNsPayloadDecoder())
                                .addLast(new APNsPayloadEncoder())
                                .addLast(new ReadTimeoutHandler(330, TimeUnit.SECONDS))
                                .addLast(new WriteTimeoutHandler(330, TimeUnit.SECONDS))
                                .addLast(clientHandler);
                    }
                });
    }

    private void addProxyInfo(ProxyInfo proxyInfo, SocketChannel ch) {
        if (proxyInfo.getType() == 0) {
            if (StringUtil.isEmpty(proxyInfo.getProxyUser())) {
                ch.pipeline().addFirst(new HttpProxyHandler(new InetSocketAddress(proxyInfo.getProxyHost(), proxyInfo.getProxyPort())));
            } else {
                ch.pipeline().addFirst(new HttpProxyHandler(new InetSocketAddress(proxyInfo.getProxyHost(), proxyInfo.getProxyPort()), proxyInfo.getProxyUser(), proxyInfo.getProxyPwd()));
            }
        } else {
            if (StringUtil.isEmpty(proxyInfo.getProxyUser())) {
                ch.pipeline().addFirst(new Socks5ProxyHandler(new InetSocketAddress(proxyInfo.getProxyHost(), proxyInfo.getProxyPort())));
            } else {
                ch.pipeline().addFirst(new Socks5ProxyHandler(new InetSocketAddress(proxyInfo.getProxyHost(), proxyInfo.getProxyPort()), proxyInfo.getProxyUser(), proxyInfo.getProxyPwd()));
            }
        }
    }

    public void connect(String host) {
        bootstrap.connect(host, port).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                log.debug("Successfully connected to the server [{}:{}]", host, port);
            } else {
                clientHandler.disconnect(future.channel(), "APNS连接失败");
            }
        });
    }

    public SslHandler getSslHandler(Channel ch) {
        SslHandler sslHandler = sslContext.newHandler(ch.alloc());
        SSLParameters sslParameters = new SSLParameters();
        sslParameters.setApplicationProtocols(new String[]{"apns-security-v3"});
        sslHandler.engine().setSSLParameters(sslParameters);
        return sslHandler;
    }

    public void addConnectedInvoker(ConnectedInvocation connectedInvocation) {
        this.connectedCallBacks.add(connectedInvocation);
    }

    public void initialize(APNsClientContext context) {
        clientHandler.initAPNsPayloadReceiver(context);
        init();
    }

    public void start() {
        connect(host);
    }
}
