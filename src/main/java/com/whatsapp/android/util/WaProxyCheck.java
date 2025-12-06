package com.whatsapp.android.util;

import Util.StringUtil;
import com.whatsapp.android.entity.ProxyInfo;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;
import io.netty.resolver.DefaultAddressResolverGroup;
import io.netty.resolver.NoopAddressResolverGroup;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * wa代理检测
 *
 * @author sunnoc
 * @date 2022-11-05 09:57
 */
@Slf4j
public class WaProxyCheck {


    /**
     * 全局初始化一次就行，如果程序关闭调用一下 EVENT_GROUP.shutdownGracefully();
     */
    private static final EventLoopGroup EVENT_GROUP = new NioEventLoopGroup();


    public static void main(String[] args) {

        /*157.230.41.132  12970  fans007  fans888
        128.199.200.131  12956  fans007  fans888*/
        //test:vuirl779141@194.233.71.95:59398
        //可用代理
        /**
         * ProxyInfo(type=1, proxyHost=sg1.ip007.cc, proxyPort=32133, proxyUser=a1c0dd44a7e, proxyPwd=474e70a2be3)
         */
        ProxyInfo proxyInfo = new ProxyInfo();
        proxyInfo.setType(1);
        proxyInfo.setProxyHost("sg1.ip007.cc");
        proxyInfo.setProxyPort(32133);
        proxyInfo.setProxyUser("a1c0dd44a7e");
        proxyInfo.setProxyPwd("474e70a2be3");

        //异常代理
        /*ProxyInfo proxyInfo = new ProxyInfo();
        proxyInfo.setType(0);
        proxyInfo.setProxyHost("128.199.200.131");
        proxyInfo.setProxyPort(12956);
        proxyInfo.setProxyUser("fans007");
        proxyInfo.setProxyPwd("fans888");*/
        StatusResult statusResult = check(proxyInfo);
        log.info(statusResult.toString());
    }

    /**
     * wa 代理检测
     *
     * @param proxyInfo 代理信息
     * @return StatusResult
     */
    public static StatusResult check(ProxyInfo proxyInfo) {
        return check(proxyInfo, 5000, 700);
    }

    /**
     * wa 代理检测
     *
     * @param proxyInfo                 代理信息
     * @param connectTimeout            连接超时，单位毫秒
     * @param connectSuccessWaitTimeout 连接成功等待超时，单位毫秒
     * @return StatusResult
     */
    public static StatusResult check(ProxyInfo proxyInfo, int connectTimeout, int connectSuccessWaitTimeout) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(EVENT_GROUP)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
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
                        ch.pipeline().addLast(new ClientHandler());
                    }
                });
        if (proxyInfo != null) {
            bootstrap.resolver(NoopAddressResolverGroup.INSTANCE);
        } else {
            bootstrap.resolver(DefaultAddressResolverGroup.INSTANCE);
        }
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout);
        CountDownLatch successCountDownLatch = new CountDownLatch(1);
        CountDownLatch exceptionCloseCountDownLatch = new CountDownLatch(1);
        AtomicBoolean success = new AtomicBoolean(false);
        AtomicBoolean exceptionClose = new AtomicBoolean(false);
        ChannelFuture channelFuture = bootstrap.connect("g.whatsapp.net", 443);
        Channel channel = channelFuture.channel();
        channelFuture.addListener(connectFuture -> {
            if (connectFuture.isSuccess()) {
                success.set(true);
            } else {
                success.set(false);
                log.debug("wa代理检测连接失败：{}", connectFuture.toString());
            }
            successCountDownLatch.countDown();
        });
        channel.closeFuture().addListener(closeFuture -> {
            if (exceptionClose.get()) {
                return;
            }
            exceptionClose.set(true);
            log.debug("wa代理检测tcp连接断开：{}", closeFuture.toString());
            exceptionCloseCountDownLatch.countDown();
        });
        try {
            successCountDownLatch.await(connectTimeout + 10, TimeUnit.MILLISECONDS);
            if (success.get()) {
                log.debug("连接成功");
                exceptionCloseCountDownLatch.await(connectSuccessWaitTimeout, TimeUnit.MILLISECONDS);
                if (exceptionClose.get()) {
                    //短时间内代理就断开，有异常
                    return StatusResult.fail("代理存在问题");
                }
                exceptionClose.set(true);
                return StatusResult.ok();
            } else {
                log.debug("代理连接失败");
                return StatusResult.fail("代理连接失败");
            }

        } catch (Exception ignored) {
            return StatusResult.fail("检查异常");
        } finally {
            channel.close();
        }
    }

    public static class StatusResult {
        private boolean success;
        private String message;

        public StatusResult() {
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public StatusResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static StatusResult ok() {
            return new StatusResult(true, "成功");
        }

        public static StatusResult ok(String message) {
            return new StatusResult(true, message);
        }

        public static StatusResult fail() {
            return new StatusResult(false, "失败");
        }

        public static StatusResult fail(String message) {
            return new StatusResult(false, message);
        }
    }

    private static class ClientHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        }

        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
                throws Exception {
        }
    }


}
