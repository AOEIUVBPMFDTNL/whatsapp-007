package com.whatsapp.android.service.impl.register;

import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.annotation.ApiType;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.entity.pack.register.RegisterPack;
import com.whatsapp.android.entity.response.register.SendSmsRegisterResult;
import com.whatsapp.android.service.ApiStrategy;
import org.springframework.stereotype.Service;

@Service
@ApiType(TypeConstant.TaskType.SEND_SMS)
public class SendSmsService implements ApiStrategy {
    @Override
    public Result execute(String type, JSONObject dataObject, String taskId, User user1) {
        RegisterPack registerPack = dataObject.getObject("data", RegisterPack.class);
        String username = registerPack.getUsername();
        User user = new User();
        SendSmsRegisterResult sendSmsRegisterResult = user.sendSms(registerPack);
        if (Constant.OK.equals(sendSmsRegisterResult.getStatus())) {
            return Result.taskSuccess(type, taskId, username, sendSmsRegisterResult);
        } else {
            return Result.taskFail(type, taskId, username, sendSmsRegisterResult);
        }
    }
}
