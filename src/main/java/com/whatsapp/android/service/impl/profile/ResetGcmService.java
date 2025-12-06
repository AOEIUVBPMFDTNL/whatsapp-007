package com.whatsapp.android.service.impl.profile;

import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.annotation.ApiType;
import com.whatsapp.android.api.profile.ResetGcmRequest;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.service.ApiStrategy;
import org.springframework.stereotype.Service;

/**
 * 重置gcm
 *
 * @author sunnoc
 * @date 2023-05-26 17:06
 */
@Service
@ApiType(TypeConstant.TaskType.RESET_GCM)
public class ResetGcmService implements ApiStrategy {

    @Override
    public Result execute(String type, JSONObject dataObject, String taskId, User user) {
        String username = user.getLoginPack().getUsername();
        boolean force = false;
        JSONObject data = dataObject.getJSONObject("data");
        if (data != null) {
            force = data.getBooleanValue("force");
        }
        StatusResult statusResult = user.sendRequest(new ResetGcmRequest(force));
        if (Constant.OK.equals(statusResult.getStatus())) {
            return Result.taskSuccess(type, taskId, username, statusResult);
        }
        return Result.taskFail(type, taskId, username, statusResult);
    }
}
