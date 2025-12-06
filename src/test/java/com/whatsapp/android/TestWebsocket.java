package com.whatsapp.android;

import com.whatsapp.android.config.ThreadPoolConfig;
import com.whatsapp.android.util.cron.CronUtil;
import com.whatsapp.android.util.cron.task.Task;
import com.whatsapp.android.ws.WebSocketChatClient;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author sunnoc
 * @date 2021-12-07 21:24
 */
@Slf4j
public class TestWebsocket {
    private static WebSocketChatClient webSocketChatClient;

    public static void main(String[] args) throws URISyntaxException {
        String url = "ws://8.214.66.72:3010/connection-test?a=A32PKK%2FaGT4BC7nZYXdOxaxk0VgCZbFq30X%2F7kAx04A%3D&b=z%2B6puqdyOrFtpIKtpmzHuw%3D%3D";
        // String url = "ws://api.007.tools/connection-test?a=A32PKK%2FaGT4BC7nZYXdOxaxk0VgCZbFq30X%2F7kAx04A%3D&b=z%2B6puqdyOrFtpIKtpmzHuw%3D%3D";
        URI uri = new URI(url);
        webSocketChatClient = new WebSocketChatClient(uri);
        Boolean start = webSocketChatClient.start();
        System.out.println("连接状态：" + start);
        // 支持秒级别定时任务
        CronUtil.setMatchSecond(true);
        CronUtil.start(ThreadPoolConfig.cronTaskThreadPoolExecutor);
        //每30秒执行一次任务
        CronUtil.schedule("0/30 * * * * ?", (Task) TestWebsocket::check);
    }

    private static void check() {
        if (WebSocketChatClient.online) {
            webSocketChatClient.send(".");
            log.info("发送心跳");
        }
    }
}
