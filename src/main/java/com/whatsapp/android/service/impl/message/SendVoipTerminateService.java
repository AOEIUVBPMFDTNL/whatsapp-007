package com.whatsapp.android.service.impl.message;

import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.annotation.ApiType;
import com.whatsapp.android.api.message.SendVoipTerminateRequest;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.entity.pack.message.VoipTerminatePack;
import com.whatsapp.android.service.ApiStrategy;
import org.springframework.stereotype.Service;

@Service
@ApiType(TypeConstant.TaskType.SEND_VOIP_TERMINATE)
public class SendVoipTerminateService implements ApiStrategy {
    @Override
    public Result execute(String type, JSONObject dataObject, String taskId, User user) {
        VoipTerminatePack data = dataObject.getObject("data", VoipTerminatePack.class);
        String username = user.getLoginPack().getUsername();
        StatusResult statusResult = user.sendRequest(new SendVoipTerminateRequest(data));
        if (Constant.OK.equals(statusResult.getStatus())) {
            return Result.taskSuccess(type, taskId, username, statusResult);
        }
        return Result.taskFail(type, taskId, username, statusResult);
    }
}