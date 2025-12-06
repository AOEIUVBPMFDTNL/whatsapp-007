package com.whatsapp.android.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.core.StandardThreadExecutor;


/**
 * @author sunnoc
 * @date 2020-02-18 13:45
 */
@Slf4j
public class ThreadPoolConfig {
    public static StandardThreadExecutor threadPoolExecutor = startThreadPool("threadPool-", 30, 80);

    public static StandardThreadExecutor loginThreadPool = startThreadPool("loginThread-", 30, 400);

    public static StandardThreadExecutor logoutThreadPool = startThreadPool("logoutThread-", 10, 20);

    public static StandardThreadExecutor cronTaskThreadPoolExecutor = startThreadPool("CronTask-", 5, 20);
    public static StandardThreadExecutor xmppPoolExecutor = startThreadPool("xmppPool-", 30, 470);
    public static StandardThreadExecutor gcmAndGooglePoolExecutor = startThreadPool("gcmAndGooglePool-", 1, 2);
    public static StandardThreadExecutor chatHistoryPoolExecutor = startThreadPool("chatHistory-", 2, 5);
    public static StandardThreadExecutor sendMsgPoolExecutor = startThreadPool("sendMsg-", 50, 300);

    public static StandardThreadExecutor platformNotificationPoolExecutor = startThreadPool("GCM_APNS-", 30, 30);

    public static StandardThreadExecutor startThreadPool(String namePrefix, int minSpareThreads, int maxThreads) {
        StandardThreadExecutor standardThreadExecutor = new StandardThreadExecutor();
        standardThreadExecutor.setMinSpareThreads(minSpareThreads);
        standardThreadExecutor.setMaxThreads(maxThreads);
        standardThreadExecutor.setNamePrefix(namePrefix);
        try {
            standardThreadExecutor.start();
        } catch (Exception e) {
            log.error("启动线程池异常", e);
        }
        return standardThreadExecutor;
    }

    public static void stopThreadPool(StandardThreadExecutor standardThreadExecutor) {
        try {
            standardThreadExecutor.stop();
        } catch (Exception e) {
            log.error("停止线程池异常", e);
        }
    }
}
