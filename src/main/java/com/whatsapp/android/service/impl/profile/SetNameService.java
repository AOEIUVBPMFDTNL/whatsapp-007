package com.whatsapp.android.service.impl.profile;

import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.annotation.ApiType;
import com.whatsapp.android.api.profile.SetNameRequest;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.service.ApiStrategy;
import org.springframework.stereotype.Service;

/**
 * 设置昵称
 *
 * @author sunnoc
 * @date 2021-03-17 11:30
 */
@Service
@ApiType(TypeConstant.TaskType.SET_NAME)
public class SetNameService implements ApiStrategy {
    @Override
    public Result execute(String type, JSONObject dataObject, String taskId, User user) {
        String nickName = dataObject.getJSONObject("data").getString("nickName");
        String username = user.getLoginPack().getUsername();
        StatusResult statusResult = user.sendRequest(new SetNameRequest(nickName));
        if (Constant.OK.equals(statusResult.getStatus())) {
            return Result.taskSuccess(type, taskId, username, statusResult);
        }
        return Result.taskFail(type, taskId, username, statusResult);
    }
}
