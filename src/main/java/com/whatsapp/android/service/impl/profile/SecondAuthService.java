package com.whatsapp.android.service.impl.profile;

import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.annotation.ApiType;
import com.whatsapp.android.api.profile.SecondAuthRequest;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.entity.pack.profile.SecondAuthPack;
import com.whatsapp.android.service.ApiStrategy;
import org.springframework.stereotype.Service;

/**
 * 二次验证
 *
 * @author sunnoc
 * @date 2021-05-15 13:32
 */
@Service
@ApiType(TypeConstant.TaskType.SECOND_AUTH)
public class SecondAuthService implements ApiStrategy {
    @Override
    public Result execute(String type, JSONObject dataObject, String taskId, User user) {
        String username = user.getLoginPack().getUsername();
        SecondAuthPack secondAuthPack = dataObject.getObject("data", SecondAuthPack.class);
        StatusResult statusResult = user.sendRequest(new SecondAuthRequest(secondAuthPack));
        if (Constant.OK.equals(statusResult.getStatus())) {
            return Result.taskSuccess(type, taskId, username, statusResult);
        }
        return Result.taskFail(type, taskId, username, statusResult);
    }
}
