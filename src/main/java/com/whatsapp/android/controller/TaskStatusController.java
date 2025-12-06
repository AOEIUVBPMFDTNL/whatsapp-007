package com.whatsapp.android.controller;

import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.config.ThreadPoolConfig;
import com.whatsapp.android.entity.ThreadStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class TaskStatusController {

    @GetMapping(value = "getExecuteTaskStatus")
    public JSONObject getThreadStatus() {
        Map<String, Object> map = new HashMap<>(4);
        map.put("threadPoolExecutor", new ThreadStatus(ThreadPoolConfig.threadPoolExecutor.getQueueSize(),
                ThreadPoolConfig.threadPoolExecutor.getActiveCount(),
                ThreadPoolConfig.threadPoolExecutor.getCorePoolSize(),
                ThreadPoolConfig.threadPoolExecutor.getMaxThreads()));
        map.put("loginThreadPool", new ThreadStatus(ThreadPoolConfig.loginThreadPool.getQueueSize(),
                ThreadPoolConfig.loginThreadPool.getActiveCount(),
                ThreadPoolConfig.loginThreadPool.getCorePoolSize(),
                ThreadPoolConfig.loginThreadPool.getMaxThreads()));
        map.put("logoutThreadPool", new ThreadStatus(ThreadPoolConfig.logoutThreadPool.getQueueSize(),
                ThreadPoolConfig.logoutThreadPool.getActiveCount(),
                ThreadPoolConfig.logoutThreadPool.getCorePoolSize(),
                ThreadPoolConfig.logoutThreadPool.getMaxThreads()));
        map.put("xmppPoolExecutor", new ThreadStatus(ThreadPoolConfig.xmppPoolExecutor.getQueueSize(),
                ThreadPoolConfig.xmppPoolExecutor.getActiveCount(),
                ThreadPoolConfig.xmppPoolExecutor.getCorePoolSize(),
                ThreadPoolConfig.xmppPoolExecutor.getMaxThreads()));
        map.put("gcmAndGooglePoolExecutor", new ThreadStatus(ThreadPoolConfig.gcmAndGooglePoolExecutor.getQueueSize(),
                ThreadPoolConfig.gcmAndGooglePoolExecutor.getActiveCount(),
                ThreadPoolConfig.gcmAndGooglePoolExecutor.getCorePoolSize(),
                ThreadPoolConfig.gcmAndGooglePoolExecutor.getMaxThreads()));
        map.put("chatHistoryPoolExecutor", new ThreadStatus(ThreadPoolConfig.chatHistoryPoolExecutor.getQueueSize(),
                ThreadPoolConfig.chatHistoryPoolExecutor.getActiveCount(),
                ThreadPoolConfig.chatHistoryPoolExecutor.getCorePoolSize(),
                ThreadPoolConfig.chatHistoryPoolExecutor.getMaxThreads()));
        map.put("sendMsgPoolExecutor", new ThreadStatus(ThreadPoolConfig.sendMsgPoolExecutor.getQueueSize(),
                ThreadPoolConfig.sendMsgPoolExecutor.getActiveCount(),
                ThreadPoolConfig.sendMsgPoolExecutor.getCorePoolSize(),
                ThreadPoolConfig.sendMsgPoolExecutor.getMaxThreads()));
        map.put("cronTaskThreadPoolExecutor", new ThreadStatus(ThreadPoolConfig.cronTaskThreadPoolExecutor.getQueueSize(),
                ThreadPoolConfig.cronTaskThreadPoolExecutor.getActiveCount(),
                ThreadPoolConfig.cronTaskThreadPoolExecutor.getCorePoolSize(),
                ThreadPoolConfig.cronTaskThreadPoolExecutor.getMaxThreads()));
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("data", map);
        jsonObject.put("code", 200);
        return jsonObject;
    }
}
