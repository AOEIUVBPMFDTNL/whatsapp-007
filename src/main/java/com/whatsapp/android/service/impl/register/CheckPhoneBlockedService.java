package com.whatsapp.android.service.impl.register;

import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.annotation.ApiType;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.entity.pack.register.CheckPhoneBlockedPack;
import com.whatsapp.android.entity.response.register.CheckPhoneBlockedResult;
import com.whatsapp.android.service.ApiStrategy;
import org.springframework.stereotype.Service;

/**
 * 校验手机号是否封号
 *
 * @author sunnoc
 * @date 2021-11-04 18:40
 */
@Service
@ApiType(TypeConstant.TaskType.CHECK_PHONE_BLOCKED)
public class CheckPhoneBlockedService implements ApiStrategy {
    @Override
    public Result execute(String type, JSONObject dataObject, String taskId, User user1) {
        CheckPhoneBlockedPack checkPhoneBlockedPack = dataObject.getObject("data", CheckPhoneBlockedPack.class);
        String username = checkPhoneBlockedPack.getPhone();
        User user = new User();
        CheckPhoneBlockedResult checkPhoneBlockedResult = user.checkPhoneBlockedRequest(checkPhoneBlockedPack);
        if (Constant.OK.equals(checkPhoneBlockedResult.getStatus())) {
            return Result.taskSuccess(type, taskId, username, checkPhoneBlockedResult);
        } else {
            return Result.taskFail(type, taskId, username, checkPhoneBlockedResult);
        }
    }
}
