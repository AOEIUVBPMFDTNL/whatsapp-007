package com.whatsapp.android;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.whatsapp.android.entity.ProxyInfo;
import com.whatsapp.android.util.WaProxyCheck;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.core.StandardThreadExecutor;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class Application {

    public static void main(String[] args) {
        StandardThreadExecutor standardThreadExecutor = new StandardThreadExecutor();
        standardThreadExecutor.setMinSpareThreads(30);
        standardThreadExecutor.setMaxThreads(30);
        try {
            standardThreadExecutor.start();
        } catch (Exception ignored) {
        }
        String content = FileUtil.readString("/Users/sunnoc/IdeaProjects/whatsapp-android/src/test/java/com/whatsapp/android/proxy.txt", StandardCharsets.UTF_8);
        String[] split = StrUtil.split(content, "\n");
        CountDownLatch countDownLatch = new CountDownLatch(split.length);
        for (String proxyInfo : split) {
            standardThreadExecutor.execute(() -> {
                String[] proxy = StrUtil.split(proxyInfo, "\t");
                String ip = proxy[0];
                String port = proxy[1];
                String username = proxy[2];
                String password = proxy[3];
                ProxyInfo proxyInfo1 = new ProxyInfo(0, ip, Integer.parseInt(port), username, password);
                WaProxyCheck.StatusResult statusResult = WaProxyCheck.check(proxyInfo1);
                if (!statusResult.isSuccess()) {
                    FileUtil.appendString(proxyInfo + "\n", "/Users/sunnoc/IdeaProjects/whatsapp-android/src/test/java/com/whatsapp/android/fail.txt", StandardCharsets.UTF_8);
                }
                countDownLatch.countDown();
            });


        }
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        log.info("执行完毕");

    }
}