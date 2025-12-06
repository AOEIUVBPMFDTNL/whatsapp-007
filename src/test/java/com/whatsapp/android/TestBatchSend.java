package com.whatsapp.android;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

/**
 * @author sunnoc
 * @date 2022-02-15 10:23
 */
@Slf4j
public class TestBatchSend {
    private static final String requestIp = "139.180.155.209";

    public static void main(String[] args) {
        String username = "50763916733";
        String path = "/Users/sunnoc/IdeaProjects/whatsapp-android/src/test/java/com/whatsapp/android/testBatchSendData.txt";
        String s = FileUtil.readString(path, StandardCharsets.UTF_8);
        String[] split = StrUtil.split(s, System.lineSeparator());
        for (int i = 0; i < split.length; i++) {
            if (StringUtils.hasLength(split[i])) {
                boolean success = setTimeLimitMsg(username, split[i]);
                if (success) {
                    log.info("用户：{}，粉丝：{}，发送成功", username, split[i]);
                } else {
                    log.info("用户：{}，粉丝：{}，发送失败", username, split[i]);
                }
                // ThreadUtil.sleep(3000);
            }
        }
        log.info("执行完毕");
    }

    private static boolean setTimeLimitMsg(String username, String userId) {
        String data = "{\"data\":{\"taskId\":\"D09E79713E390314FD7ED3C3E8D28353\",\"username\":\"" + username + "\",\"data\":{\"userId\":\"" + userId + "\",\"expireTime\":0},\"type\":\"setTimeLimitedMessage\",\"timeInMillis\":1638418301472,\"key\":\"debug\"},\"type\":\"task\"}";
        System.out.println(data);
        try {
            String body = HttpUtil.createPost("http://" + requestIp + ":85/api/task")
                    .header("Content-Type", "application/json")
                    .body(data)
                    .execute().body();
            log.info("限时消息：{}", body);
            JSONObject jsonObject = JSONObject.parseObject(body);
            int code = jsonObject.getJSONObject("data").getJSONObject("body").getIntValue("code");
            return code == 1;
        } catch (Exception e) {
            log.error("设置限时消息异常", e);
            return false;
        }
    }

    private static boolean filterFansInfoRequest(String username, String userId) {
        String data = "{\"data\":{\"timeInMillis\":1602558858397,\"key\":\"debug\",\"type\":\"filterFansInfoRequest\",\"taskId\":\"5856474a-5f50-4889-964f-4ec5a5d00702\",\"username\":\"" + username + "\",\"data\": {\"userIds\":[\"" + userId + "@s.whatsapp.net\"]}},\"type\":\"task\"}";
        System.out.println(data);
        try {
            String body = HttpUtil.createPost("http://" + requestIp + ":85/api/task")
                    .header("Content-Type", "application/json")
                    .body(data)
                    .execute().body();
            log.info("限时消息：{}", body);
            JSONObject jsonObject = JSONObject.parseObject(body);
            int code = jsonObject.getJSONObject("data").getJSONObject("body").getIntValue("code");
            return code == 1;
        } catch (Exception e) {
            log.error("设置限时消息异常", e);
            return false;
        }
    }

    private static boolean sendMsg(String username, String userId) {
        String data = "{\"data\":{\"taskId\":\"D09E79713E390314FD7ED3C3E8D28353\",\"username\":\"" + username + "\",\"data\":{\"userId\":\"" + userId + "\",\"content\":\"hi\"},\"type\":\"sendTextMessage\",\"timeInMillis\":1638418301472,\"key\":\"debug\"},\"type\":\"task\"}";
        System.out.println(data);
        try {
            String body = HttpUtil.createPost("http://" + requestIp + ":85/api/task")
                    .header("Content-Type", "application/json")
                    .body(data)
                    .execute().body();
            log.info("发送消息：{}", body);
            JSONObject jsonObject = JSONObject.parseObject(body);
            int code = jsonObject.getJSONObject("data").getJSONObject("body").getIntValue("code");
            return code == 1;
        } catch (Exception e) {
            log.error("发送消息异常", e);
            return false;
        }
    }
}
