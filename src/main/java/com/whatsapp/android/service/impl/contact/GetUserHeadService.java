package com.whatsapp.android.service.impl.contact;

import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.annotation.ApiType;
import com.whatsapp.android.api.contact.GetUserHeadRequest;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.entity.response.contact.GetUserHeadResult;
import com.whatsapp.android.service.ApiStrategy;
import org.springframework.stereotype.Service;

/**
 * 获取用户头像
 *
 * @author sunnoc
 * @date 2021-03-17 09:42
 */
@Service
@ApiType(TypeConstant.TaskType.GET_USER_HEAD_IMAGE)
public class GetUserHeadService implements ApiStrategy {
    @Override
    public Result execute(String type, JSONObject dataObject, String taskId, User user) {
        JSONObject data = dataObject.getJSONObject("data");
        String userId = data.getString("userId");
        boolean preview = data.getBooleanValue("preview");
        String username = user.getLoginPack().getUsername();
        GetUserHeadResult getUserHeadResult = user.sendRequest(new GetUserHeadRequest(userId, preview));
        if (Constant.OK.equals(getUserHeadResult.getStatus())) {
            return Result.taskSuccess(type, taskId, username, getUserHeadResult);
        }
        return Result.taskFail(type, taskId, username, getUserHeadResult);
    }
}
