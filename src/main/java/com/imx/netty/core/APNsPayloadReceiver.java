package com.imx.netty.core;

import com.imx.apns.common.APNsNotification;
import com.imx.apns.common.APNsState;
import com.imx.apns.common.PayloadType;
import com.imx.common.BiPlusConsumer;
import com.imx.common.Pair;
import com.imx.netty.chain.ChainContext;
import io.netty.channel.socket.SocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Slf4j
public class APNsPayloadReceiver {

    private final BiConsumer<SocketChannel, byte[]> subscribe;
    private final APNsClientContext context;
    private final ChainContext chainContext;


    private static final Consumer<SocketChannel> STATE;
    private static final BiPlusConsumer<SocketChannel, byte[], byte[]> NOTIFICATION_ACK;

    static {
        STATE = sc -> {
            try {
                APNsPayload payload = APNsPayload.make(1);
                sc.writeAndFlush(payload);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        NOTIFICATION_ACK = (sc, id, token) -> {
            try {
                APNsPayload payload = APNsPayload.make(id, token);
                sc.writeAndFlush(payload);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }


    public APNsPayloadReceiver(APNsClientContext context, ChainContext chainContext) {
        this.context = context;
        this.chainContext = chainContext;
        this.subscribe = (sc, token) -> {
            try {
                APNsPayload payload = APNsPayload.make(context.getTopics(), token);
                sc.writeAndFlush(payload);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    public void receive(SocketChannel socketChannel, APNsPayload payload) {
        try {
            PayloadType payloadType = PayloadType.byType(payload.getId());
            if (Objects.isNull(payloadType)) {
                log.warn("Unsupported payload type | payload id: {}", payload.getId());
                return;
            }
            switch (payloadType) {
                case TOKEN:
                    Optional<byte[]> connectSuccess = payload.getField((byte) 1);
                    connectSuccess.ifPresent(bytes -> {
                        ByteBuffer buffer = ByteBuffer.wrap(bytes);
                        if (buffer.get() == 0) {
                            //通知连接成功
                            chainContext.getFutureNotification().OnApnsConnectSuccess();
                            //发送心跳
                            chainContext.setScheduledFuture(socketChannel.eventLoop()
                                    .scheduleAtFixedRate(() ->
                                            socketChannel.writeAndFlush(APNsPayload.make()), 0, 120, TimeUnit.SECONDS)
                            );
                        }
                    });
                    Optional<byte[]> field = payload.getField((byte) 3);
                    APNsState apNsState = context.getApNsState();
                    byte[] token = apNsState.getToken();
                    if (token == null) {
                        token = field.get();
                        apNsState.setToken(token);
                        //第一次才做通知
                        context.getFutureResult().OnApnsState(context.getApNsState());
                    }
                    STATE.accept(socketChannel);
                    subscribe.accept(socketChannel, token);
                    break;
                case NOTIFICATION:
                    APNsNotification notification = new APNsNotification();
                    byte[] messageIdBytes = null;
                    for (Pair<Byte, byte[]> fieldPair : payload.getFields()) {
                        Byte key = fieldPair.getKey();
                        if (key == 1) {
                            notification.setToken(fieldPair.getValue());
                        } else if (key == 2) {
                            notification.setTopicHash(fieldPair.getValue());
                        } else if (key == 3) {
                            notification.setPayload(fieldPair.getValue());
                        } else if (key == 4) {
                            ByteBuffer buffer = ByteBuffer.wrap(fieldPair.getValue());
                            notification.setMessageId(buffer.getInt());
                            messageIdBytes = fieldPair.getValue();
                        } else if (key == 5) {
                            ByteBuffer buffer = ByteBuffer.wrap(fieldPair.getValue());
                            notification.setTimestamp(buffer.getInt());
                        } else {
                            if (log.isDebugEnabled()) {
                                log.debug("Unknown key {}", key);
                            }
                        }
                    }

                    PayloadResult notificationResult = new PayloadResult();
                    notificationResult.setNotification(notification);
                    context.getFutureResult().OnApnsNotify(notificationResult);
                    NOTIFICATION_ACK.accept(socketChannel, messageIdBytes, notification.getToken());
                    break;

                case SUBSCRIPTION:
                    break;

                case KEEPALIVE:
                    if (log.isDebugEnabled()) {
                        log.info("Received the heartbeat response.");
                    }
                    break;

                default:
                    if (log.isDebugEnabled()) {
                        log.warn("Unexpected value: " + payloadType);
                    }
            }

        } catch (RuntimeException e) {
            log.error("Async error on channel receive execution.", e);
        }
    }
}
