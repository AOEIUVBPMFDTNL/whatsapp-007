package com.imx.netty.core;

import com.imx.netty.chain.ChainContext;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslCloseCompletionEvent;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Optional;


@Slf4j
@ChannelHandler.Sharable
public class APNsClientHandler extends ChannelDuplexHandler {

    private List<ConnectedInvocation> connectedInvocations = Collections.emptyList();
    private APNsPayloadReceiver apnsPayloadReceiver;
    private APNsClientContext context;
    private final ChainContext chainContext;

    public APNsClientHandler(List<ConnectedInvocation> connectedInvocations, ChainContext chainContext) {
        this.chainContext = chainContext;
        Optional.ofNullable(connectedInvocations).ifPresent(listeners -> {
            this.connectedInvocations = connectedInvocations;
        });
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg == null) {
            return;
        }
        if (msg instanceof APNsPayload) {
            APNsPayload payload = (APNsPayload) msg;
            apnsPayloadReceiver.receive((SocketChannel) ctx.channel(), payload);
        } else {
            log.error("收到未知消息：{}", msg);
            ctx.fireChannelRead(msg);

        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.pipeline().remove(this);
        disconnect(ctx, cause.getMessage());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.pipeline().remove(this);
        disconnect(ctx);
    }

    private void invokeConnectedListeners(SocketChannel socketChannel) {
        for (ConnectedInvocation callBack : connectedInvocations) {
            callBack.connect(socketChannel);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof SslHandshakeCompletionEvent) {
            invokeConnectedListeners((SocketChannel) ctx.channel());
        } else if (evt instanceof SslCloseCompletionEvent) {
            log.debug("Closed the channel.");
        }
        super.userEventTriggered(ctx, evt);
    }

    public void disconnect(ChannelHandlerContext ctx, String reason) {
        cancelScheduled();
        ctx.close();
        context.getFutureResult().OnApnsClose(reason, false);
    }

    public void disconnect(ChannelHandlerContext ctx) {
        cancelScheduled();
        ctx.close();
        context.getFutureResult().OnApnsClose("断开连接", false);
    }

    public void disconnect(Channel channel, String content) {
        cancelScheduled();
        if (channel != null) {
            channel.close();
        }
        context.getFutureResult().OnApnsClose(content, false);
    }


    public void initAPNsPayloadReceiver(APNsClientContext context) {
        this.context = context;
        this.apnsPayloadReceiver = new APNsPayloadReceiver(context, chainContext);
    }

    private void cancelScheduled() {
        if (chainContext.getScheduledFuture() != null) {
            chainContext.getScheduledFuture().cancel(true);
        }
    }
}
