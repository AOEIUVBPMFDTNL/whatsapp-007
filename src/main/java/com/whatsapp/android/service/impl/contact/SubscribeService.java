package com.whatsapp.android.service.impl.contact;

import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.annotation.ApiType;
import com.whatsapp.android.api.contact.SubscribeRequest;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.entity.response.contact.SubscribeResult;
import com.whatsapp.android.service.ApiStrategy;
import com.whatsapp.android.util.WhatsAppUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 查询用户在线时间
 *
 * @author sunnoc
 * @date 2021-03-17 09:48
 */
@Service
@ApiType(TypeConstant.TaskType.SUBSCRIBE)
public class SubscribeService implements ApiStrategy {
    @Override
    public Result execute(String type, JSONObject dataObject, String taskId, User user) {
        String userId = dataObject.getJSONObject("data").getString("userId");
        String username = user.getLoginPack().getUsername();
        if (StringUtils.isEmpty(userId)) {
            return Result.taskFail(type, taskId, username, StatusResult.fail("用户id不能为空"));
        }
        SubscribeResult subscribeResult = user.sendRequest(new SubscribeRequest(userId), WhatsAppUtils.JidNormalize(userId));
        if (Constant.OK.equals(subscribeResult.getStatus())) {
            return Result.taskSuccess(type, taskId, username, subscribeResult);
        }
        return Result.taskFail(type, taskId, username, subscribeResult);
    }
}
