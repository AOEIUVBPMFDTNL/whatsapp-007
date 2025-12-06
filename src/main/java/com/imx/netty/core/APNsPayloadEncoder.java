package com.imx.netty.core;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class APNsPayloadEncoder extends MessageToByteEncoder<APNsPayload> {

    @Override
    protected void encode(ChannelHandlerContext ctx, APNsPayload payload, ByteBuf out) {
        payload.encode(out);
    }
}