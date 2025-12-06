package com.imx.netty.chain;

import com.imx.apns.activate.ActivationInfo;
import com.imx.apns.common.APNsState;
import com.imx.common.Chain;
import com.imx.common.ChainExecution;
import com.imx.netty.core.*;
import com.whatsapp.android.entity.ProxyInfo;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

import static com.imx.apns.common.Constants.APPLE_COURIER_PORT;


@Slf4j
public class APNsClientChain extends Chain<ChainContext> implements ChainExecution<ChainContext> {

    FutureNotification<PayloadResult> futureNotification;

    public APNsClientChain(FutureNotification<PayloadResult> futureNotification) {
        this.futureNotification = futureNotification;
    }

    @Override
    public void process(ChainContext chainContext) {
        try {
            execute(chainContext);
        } catch (Exception e) {
            log.error("Failed to execute apnsClient chain.", e);
            return;
        }
        if (nextChain == null) {
            return;
        }
        nextChain.process(chainContext);
    }

    @Override
    public void execute(@NonNull ChainContext chainContext) throws Exception {
        String host = chainContext.getCourierHost();
        APNsState apnsState = chainContext.getApNsState();
        ProxyInfo proxyInfo = chainContext.getProxyInfo();
        NettyClient nettyClient = new NettyClient(host, APPLE_COURIER_PORT, proxyInfo, chainContext);
        nettyClient.addConnectedInvoker(socketChannel -> {
            try {
                chainContext.setSocketChannel(socketChannel);
                APNsPayload payload = APNsPayload.make(apnsState);
                socketChannel.writeAndFlush(payload);
            } catch (Exception e) {
                log.error("Failed to execute connected callback.");
                throw new RuntimeException(e);
            }
        });
        chainContext.initFuture(futureNotification);
        nettyClient.initialize(buildAPNsClientContext(chainContext));
        nettyClient.start();
    }

    public APNsClientContext buildAPNsClientContext(ChainContext chainContext) {
        return APNsClientContext.builder()
                .topics(chainContext.getTopics())
                .apNsState(chainContext.getApNsState())
                .futureResult(futureNotification)
                .build();
    }
}
