package com.whatsapp.android.service.impl.contact;

import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.annotation.ApiType;
import com.whatsapp.android.api.contact.GetBusinessUserNicknameRequest;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.entity.response.contact.GetBusinessUserNicknameResult;
import com.whatsapp.android.service.ApiStrategy;
import org.springframework.stereotype.Service;

/**
 * 获取商业用户粉丝昵称
 *
 * @author sunnoc
 * @date 2022-02-14 15:22
 */
@Service
@ApiType(TypeConstant.TaskType.GET_BUSINESS_USER_NICKNAME)
public class GetBusinessUserNicknameService implements ApiStrategy {
    @Override
    public Result execute(String type, JSONObject dataObject, String taskId, User user) {
        String userId = dataObject.getJSONObject("data").getString("userId");
        String username = user.getLoginPack().getUsername();
        GetBusinessUserNicknameResult userNicknameResult = user.sendRequest(new GetBusinessUserNicknameRequest(userId));
        if (Constant.OK.equals(userNicknameResult.getStatus())) {
            return Result.taskSuccess(type, taskId, username, userNicknameResult);
        }
        return Result.taskFail(type, taskId, username, userNicknameResult);
    }
}
