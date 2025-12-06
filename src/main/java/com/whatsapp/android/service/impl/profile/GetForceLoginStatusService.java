package com.whatsapp.android.service.impl.profile;

import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.annotation.ApiType;
import com.whatsapp.android.api.profile.GetForceLoginStatusRequest;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.entity.response.profile.ForceLoginStatus;
import com.whatsapp.android.service.ApiStrategy;
import org.springframework.stereotype.Service;

/**
 * 获取抢登状态
 */
@Service
@ApiType(TypeConstant.TaskType.GET_FORCE_LOGIN_STATUS)
public class GetForceLoginStatusService implements ApiStrategy {

    @Override
    public Result execute(String type, JSONObject dataObject, String taskId, User user) {
        String username = user.getLoginPack().getUsername();
        ForceLoginStatus forceLoginStatus = user.sendRequest(new GetForceLoginStatusRequest());
        if (Constant.OK.equals(forceLoginStatus.getStatus())) {
            return Result.taskSuccess(type, taskId, username, forceLoginStatus);
        }
        return Result.taskFail(type, taskId, username, forceLoginStatus);
    }
}