package com.whatsapp.android.service.impl.contact;

import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.annotation.ApiType;
import com.whatsapp.android.api.contact.GetUserStatusRequest;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.service.ApiStrategy;
import org.springframework.stereotype.Service;

/**
 * 获取用户状态
 *
 * @author sunnoc
 * @date 2022-02-14 15:22
 */
@Service
@ApiType(TypeConstant.TaskType.GET_USER_STATUS)
public class GetUserStatusService implements ApiStrategy {
    @Override
    public Result execute(String type, JSONObject dataObject, String taskId, User user) {
        String userId = dataObject.getJSONObject("data").getString("userId");
        String username = user.getLoginPack().getUsername();
        StatusResult statusResult = user.sendRequest(new GetUserStatusRequest(userId));
        if (Constant.OK.equals(statusResult.getStatus())) {
            return Result.taskSuccess(type, taskId, username, statusResult);
        }
        return Result.taskFail(type, taskId, username, statusResult);
    }
}
