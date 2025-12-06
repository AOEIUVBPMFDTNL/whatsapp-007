package com.whatsapp.android.util;


import cn.hutool.core.thread.ExecutorBuilder;
import cn.hutool.core.thread.NamedThreadFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 自定义线程池
 *
 * @author sunnoc
 * @date 2021-06-11 19:06
 */
@Slf4j
public class CustomThreadPool {
    private static final List<ThreadPoolExecutor> threadGroup = new ArrayList<>();
    private static final String threadGroupPrefix = "taskGroup";
    private static int threadNum = 0;

    public static void creatThreadPool(int threadNum) {
        if (threadNum == 0) {
            threadNum = 5;
        }
        CustomThreadPool.threadNum = threadNum;
        for (int i = 0; i < threadNum; i++) {
            ThreadPoolExecutor threadPoolExecutor = ExecutorBuilder.create().
                    setThreadFactory(new NamedThreadFactory(threadGroupPrefix + i + "-", false))
                    .setCorePoolSize(1).setMaxPoolSize(1).setWorkQueue(new LinkedBlockingQueue<>()).build();
            threadGroup.add(threadPoolExecutor);
        }
    }

    public static ThreadPoolExecutor getThread(String username) {
        int index = Math.abs(username.hashCode()) % threadNum;
        return threadGroup.get(index);
    }

}
