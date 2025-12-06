package com.imx.netty.core;

import com.imx.apns.activate.ActivationInfo;
import com.imx.apns.common.APNsState;
import com.imx.common.Chain;
import com.imx.netty.chain.*;
import com.whatsapp.android.entity.ProxyInfo;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class APNsClientProcessor {

    FutureNotification<PayloadResult> futureNotification;
    APNsState apnsState;
    ActivationInfo activationInfo;
    List<String> topics;
    ProxyInfo proxyInfo;
    ChainContext chainContext;

    public APNsClientProcessor(APNsState apnsState,
                               ActivationInfo activationInfo,
                               List<String> topics, ProxyInfo proxyInfo,
                               FutureNotification<PayloadResult> futureNotification) {
        this.apnsState = apnsState;
        this.activationInfo = activationInfo;
        this.proxyInfo = proxyInfo;
        this.topics = topics;
        this.futureNotification = futureNotification;
    }

    public APNsClientProcessor(APNsState apnsState, List<String> topics, ProxyInfo proxyInfo, FutureNotification<PayloadResult> futureNotification) {
        this.apnsState = apnsState;
        this.topics = topics;
        this.proxyInfo = proxyInfo;
        this.futureNotification = futureNotification;
    }

    public void process() throws Throwable {
        chainContext = ChainContext.builder()
                .activationInfo(activationInfo)
                .topics(topics)
                .proxyInfo(proxyInfo)
                .apNsState(apnsState)
                .build();

        Chain<ChainContext> chain = new Chain<>();
        chain.setNextChain(new BasicChain())
                .setNextChain(new ActivateChain())
                .setNextChain(new AppleBagChain())
                .setNextChain(new APNsClientChain(futureNotification));
        chain.process(chainContext);
    }

    public void process2() {
        chainContext = ChainContext.builder()
                .activationInfo(activationInfo)
                .topics(topics)
                .proxyInfo(proxyInfo)
                .apNsState(apnsState)
                .build();
        Chain<ChainContext> chain = new Chain<>();
        chain.setNextChain(new AppleBagChain())
                .setNextChain(new APNsClientChain(futureNotification));
        chain.process(chainContext);
    }

    public void close() {
        try {
            Channel socketChannel = chainContext.getSocketChannel();
            if (socketChannel != null && socketChannel.isActive()) {
                socketChannel.close();
            }
        } catch (Exception ignored) {
        }
    }
}
