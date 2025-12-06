package com.imx.netty.chain;

import com.imx.apns.common.APNsState;
import com.imx.apns.activate.ActivationInfo;
import com.imx.netty.core.FutureNotification;
import com.imx.netty.core.PayloadResult;
import com.whatsapp.android.entity.ProxyInfo;
import io.netty.channel.Channel;
import io.netty.util.concurrent.ScheduledFuture;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.security.KeyPair;
import java.util.List;


@Getter
@Setter
@Builder
public class ChainContext {

    private KeyPair keyPair;
    private ActivationInfo activationInfo;
    private String courierHost;
    private APNsState apNsState;
    private List<String> topics;
    private FutureNotification<PayloadResult> futureNotification;
    private ProxyInfo proxyInfo;
    private Channel socketChannel;
    private ScheduledFuture<?> scheduledFuture;

    public void initFuture(FutureNotification<PayloadResult> futureNotification) {
        this.futureNotification = futureNotification;
    }

}
