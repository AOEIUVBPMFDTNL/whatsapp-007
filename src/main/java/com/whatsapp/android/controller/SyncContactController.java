package com.whatsapp.android.controller;

import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.api.contact.SyncContactRequest;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.entity.UserRecord;
import com.whatsapp.android.entity.pack.contact.SyncContactPack;
import com.whatsapp.android.entity.response.contact.SyncContactResult;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ConcurrentMap;

/**
 * @author sunnoc
 * @date 2021-05-26 10:34
 */
@RestController
@RequestMapping("/api")
public class SyncContactController {
    @RequestMapping(method = RequestMethod.POST, value = "syncContact", consumes = "application/json")
    public Result task(@RequestBody JSONObject jsonObject) {
        JSONObject dataObject = jsonObject.getJSONObject("data");
        String type = dataObject.getString("type");
        String taskId = dataObject.getString("taskId");
        String key = dataObject.getString("key");
        if (!key.equals(Constant.SYNC_CONTACT_KEY)) {
            return Result.taskFail(type, taskId, "unknown", StatusResult.fail("通信密匙错误"));
        }
        ConcurrentMap<String, User> record = UserRecord.getRecord();
        User user = null;
        String username = null;
        for (int i = 0; i < 3; i++) {
            int size = record.size();
            if (size == 0) {
                return Result.taskFail(type, taskId, "unknown", StatusResult.fail("没有可使用账号"));
            }
            int randomInt = RandomUtil.randomInt(0, size);
            String[] usernames = record.keySet().toArray(new String[0]);
            username = usernames[randomInt];
            user = record.get(username);
            boolean online = user.isOnline();
            if (online) {
                break;
            }
            username = null;
        }
        if (StringUtils.isEmpty(username)) {
            return Result.taskFail(type, taskId, "unknown", StatusResult.fail("未获取到在线账号"));
        }
        SyncContactPack data = dataObject.getObject("data", SyncContactPack.class);
        SyncContactResult syncContactResult = user.sendRequest(new SyncContactRequest(data));
        if (Constant.OK.equals(syncContactResult.getStatus())) {
            return Result.taskSuccess(type, taskId, username, syncContactResult);
        }
        return Result.taskFail(type, taskId, username, syncContactResult);
    }
}
