package com.whatsapp.android;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

/**
 * @author sunnoc
 * @date 2022-02-14 17:35
 */
public class HttpTest {
    public static void main(String[] args) {
        String content = "{\"data\":{\"timeInMillis\":1602558858397,\"key\":\"debug\",\"type\":\"filterFansInfoRequest\",\"taskId\":\"5856474a-5f50-4889-964f-4ec5a5d00702\",\"username\":\"959401677350\",\"data\": {\"userIds\":[\"19084154278@s.whatsapp.net\"]}},\"type\":\"task\"}";
        JSONObject jsonObject = JSONObject.parseObject(content);
        JSONArray jsonArray = jsonObject.getJSONObject("data").getJSONObject("data").getJSONArray("userIds");
        jsonArray.clear();
        String s = FileUtil.readString("/Users/sunnoc/Library/Containers/com.tencent.xinWeChat/Data/Library/Application Support/com.tencent.xinWeChat/2.0b4.0.9/48930f59a2550b4449c1be40c9e6458e/Message/MessageTemp/444966e19292c819293b7b65596f21e5/File/128条美国封号.txt", StandardCharsets.UTF_8);
        String[] split = StrUtil.split(s, "\r\n");
        for (int i = 0; i < split.length; i++) {
            if (StringUtils.hasLength(split[i])) {
                jsonArray.add(i, split[i]);
            }
        }
        String data = jsonObject.toJSONString();
        // System.out.println(data);
        String body = HttpUtil.createPost("http://127.0.0.1:85/api/task")
                .header("Content-Type", "application/json")
                .body(data)
                .execute().body();
        System.out.println(body);

    }
}
