package com.whatsapp.android.service.impl.contact;

import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.annotation.ApiType;
import com.whatsapp.android.api.contact.UnSubscribeRequest;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.service.ApiStrategy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * @author sunnoc
 * @date 2021-07-01 11:29
 */
@Service
@ApiType(TypeConstant.TaskType.UN_SUBSCRIBE)
public class UnSubscribeService implements ApiStrategy {
    @Override
    public Result execute(String type, JSONObject dataObject, String taskId, User user) {
        String userId = dataObject.getJSONObject("data").getString("userId");
        String username = user.getLoginPack().getUsername();
        if (StringUtils.isEmpty(userId)) {
            return Result.taskFail(type, taskId, username, StatusResult.fail("用户id不能为空"));
        }
        StatusResult statusResult = user.sendRequest(new UnSubscribeRequest(userId));
        if (Constant.OK.equals(statusResult.getStatus())) {
            return Result.taskSuccess(type, taskId, username, statusResult);
        }
        return Result.taskFail(type, taskId, username, statusResult);
    }
}
