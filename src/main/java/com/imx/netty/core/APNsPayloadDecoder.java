package com.imx.netty.core;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Slf4j
public class APNsPayloadDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        try {
            Optional<APNsPayload> payload = APNsPayload.decode(in);
            payload.ifPresent(p -> out.add(payload.get()));
        } catch (IOException e) {
            log.error("Failed to decode.", e);
        }
    }
}