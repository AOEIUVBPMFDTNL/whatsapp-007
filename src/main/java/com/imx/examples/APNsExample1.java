package com.imx.examples;

import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSONObject;
import com.imx.apns.activate.ActivationInfo;
import com.imx.apns.common.APNsNotification;
import com.imx.apns.common.APNsState;
import com.imx.apns.common.PayloadType;
import com.imx.common.util.ByteUtil;
import com.imx.netty.core.APNsClientProcessor;
import com.imx.netty.core.FutureNotification;
import com.imx.netty.core.PayloadResult;
import com.whatsapp.android.entity.ProxyInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
public class APNsExample1 {

    public static void main(String[] args) {
        String serialNumber = RandomUtil.randomStringUpper(12);
        List<String> topics = Collections.singletonList("net.whatsapp.WhatsApp");
        ActivationInfo activationInfo = new ActivationInfo.Builder()
                //.serialNumber("GGLH101MJC6F")
                .serialNumber(serialNumber)
                .activationState("Unactivated")
                .buildVersion("22F82")
                .deviceClass("MacOS")
                .productType("MacBookPro18,3")
                .productVersion("13.4.1")
                .build();

        // 消息通知
        FutureNotification<PayloadResult> futureNotification = new FutureNotification<PayloadResult>() {
            @Override
            public void OnApnsConnectSuccess() {
                log.info("Apns连接成功");
            }

            @Override
            public void OnApnsNotify(PayloadResult payloadResult) {
                log.info("Received notification: {}", JSONObject.toJSONString(payloadResult));
            }

            @Override
            public void OnApnsState(APNsState apnsState) {

            }

            @Override
            public void OnApnsClose(String reason, boolean force) {
                log.info("Apns连接断开: {}", reason);
            }
        };
        ProxyInfo proxyInfo = new ProxyInfo();
        proxyInfo.setType(1);
        proxyInfo.setProxyHost("127.0.0.1");
        proxyInfo.setProxyPort(1080);
        APNsClientProcessor apNsClientManager = new APNsClientProcessor(null, activationInfo, topics, proxyInfo, futureNotification);
        try {
            apNsClientManager.process();
            /*String state = ByteUtil.bytesToHex(ByteUtil.serialize(apnsState.get()));
            System.out.println("Received state: \n" + state);

            String token = HexUtil.encodeHexStr(apnsState.get().getToken());
            System.out.println("Received token: \n" + token);*/
        } catch (Throwable t) {
            System.err.println("Process exception: " + t.getMessage());
        }
    }
}
