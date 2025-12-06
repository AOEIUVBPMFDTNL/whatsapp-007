package com.whatsapp.android.entity;

import lombok.Data;


@Data
public class ThreadStatus {
    /**
     * 队列大小
     */
    private int queueSize;
    /**
     * 当前活动线程数
     */
    private int activeCount;
    /**
     * 核心线程池大小
     */
    private int corePoolSize;
    /**
     * 最大线程数限制
     */
    private int maxThreads;

    public ThreadStatus() {
    }

    public ThreadStatus(int queueSize, int activeCount, int corePoolSize, int maxThreads) {
        this.queueSize = queueSize;
        this.activeCount = activeCount;
        this.corePoolSize = corePoolSize;
        this.maxThreads = maxThreads;
    }
}
