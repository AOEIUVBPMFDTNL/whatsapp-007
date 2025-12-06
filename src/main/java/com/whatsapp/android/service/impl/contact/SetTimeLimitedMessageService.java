package com.whatsapp.android.service.impl.contact;

import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.annotation.ApiType;
import com.whatsapp.android.api.contact.SetTimeLimitedMessageRequest;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.entity.pack.contact.TimeLimitedMessagePack;
import com.whatsapp.android.entity.response.message.SendMessageResult;
import com.whatsapp.android.service.ApiStrategy;
import org.springframework.stereotype.Service;

/**
 * 设置限时消息
 *
 * @author sunnoc
 * @date 2022-01-24 09:57
 */
@Service
@ApiType(TypeConstant.TaskType.SET_TIME_LIMITED_MESSAGE)
public class SetTimeLimitedMessageService implements ApiStrategy {

    @Override
    public Result execute(String type, JSONObject dataObject, String taskId, User user) {
        TimeLimitedMessagePack data = dataObject.getObject("data", TimeLimitedMessagePack.class);
        String username = user.getLoginPack().getUsername();
        SendMessageResult sendMessageResult = user.sendRequest(new SetTimeLimitedMessageRequest(data));
        if (Constant.OK.equals(sendMessageResult.getStatus())) {
            return Result.taskSuccess(type, taskId, username, sendMessageResult);
        }
        return Result.taskFail(type, taskId, username, sendMessageResult);
    }
}
