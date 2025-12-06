package com.whatsapp.android.service.impl.message;

import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.annotation.ApiType;
import com.whatsapp.android.api.message.MarkReadRequest;
import com.whatsapp.android.api.message.SendAcceptCallRequest;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.entity.pack.message.AcceptCallPack;
import com.whatsapp.android.entity.pack.message.VoipCallPack;
import com.whatsapp.android.service.ApiStrategy;
import org.springframework.stereotype.Service;

/**
 * 接听语音通话
 *
 * @author Rocky
 */
@Service
@ApiType(TypeConstant.TaskType.ACCEPT_VOIP_CALL)
public class SendAcceptCallService implements ApiStrategy {
    @Override
    public Result execute(String type, JSONObject dataObject, String taskId, User user) {
        VoipCallPack data = dataObject.getObject("data", VoipCallPack.class);
        String username = user.getLoginPack().getUsername();
        return execute(user, username, type, data, taskId);
    }

    public Result execute(User user, String username, String type, VoipCallPack data, String taskId) {
        StatusResult statusResult = user.sendRequest(new SendAcceptCallRequest(data));
        if (Constant.OK.equals(statusResult.getStatus())) {
            return Result.taskSuccess(type, taskId, username, statusResult);
        }
        return Result.taskFail(type, taskId, username, statusResult);

    }

}