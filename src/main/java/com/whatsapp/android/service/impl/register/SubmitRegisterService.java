package com.whatsapp.android.service.impl.register;

import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.annotation.ApiType;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.entity.pack.register.RegisterPack;
import com.whatsapp.android.entity.response.register.SubmitRegisterResult;
import com.whatsapp.android.service.ApiStrategy;
import org.springframework.stereotype.Service;

/**
 * 提交注册
 *
 * @author sunnoc
 * @date 2021-04-22 20:39
 */
@Service
@ApiType(TypeConstant.TaskType.SUBMIT_REGISTER)
public class SubmitRegisterService implements ApiStrategy {
    @Override
    public Result execute(String type, JSONObject dataObject, String taskId, User user1) {
        RegisterPack registerPack = dataObject.getObject("data", RegisterPack.class);
        String username = registerPack.getUsername();
        User user = new User();
        SubmitRegisterResult submitRegisterResult = user.submitRegisterRequest(registerPack);
        if (Constant.OK.equals(submitRegisterResult.getStatus())) {
            return Result.taskSuccess(type, taskId, username, submitRegisterResult);
        } else {
            return Result.taskFail(type, taskId, username, submitRegisterResult);
        }
    }
}
