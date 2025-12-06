package com.whatsapp.android.service.impl.profile;

import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.annotation.ApiType;
import com.whatsapp.android.api.profile.QueryContactTcTokenRequest;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.entity.response.profile.QueryContactTcTokenResult;
import com.whatsapp.android.service.ApiStrategy;
import org.springframework.stereotype.Service;

@Service
@ApiType(TypeConstant.TaskType.QUERY_CONTACT_TC_TOKEN)
public class QueryContactTcTokenService implements ApiStrategy {
    @Override
    public Result execute(String type, JSONObject dataObject, String taskId, User user) {
        String username = user.getLoginPack().getUsername();
        QueryContactTcTokenResult queryContactTcTokenResult = user.sendRequest(new QueryContactTcTokenRequest());
        if (Constant.OK.equals(queryContactTcTokenResult.getStatus())) {
            return Result.taskSuccess(type, taskId, username, queryContactTcTokenResult);
        }
        return Result.taskFail(type, taskId, username, queryContactTcTokenResult);
    }
}
