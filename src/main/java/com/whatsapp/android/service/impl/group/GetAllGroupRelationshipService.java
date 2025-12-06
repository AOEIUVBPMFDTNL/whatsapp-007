package com.whatsapp.android.service.impl.group;

import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.annotation.ApiType;
import com.whatsapp.android.api.group.GetAllGroupRelationshipRequest;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.entity.response.goup.GetAllGroupRelationshipResult;
import com.whatsapp.android.service.ApiStrategy;
import org.springframework.stereotype.Service;

/**
 * 获取小号所有群关系
 */
@Service
@ApiType(TypeConstant.TaskType.GET_ALL_GROUP_RELATIONSHIP)
public class GetAllGroupRelationshipService implements ApiStrategy {

    @Override
    public Result execute(String type, JSONObject dataObject, String taskId, User user) {
        String username = user.getLoginPack().getUsername();
        GetAllGroupRelationshipResult getAllGroupRelationshipResult = user.sendRequest(new GetAllGroupRelationshipRequest());
        if (Constant.OK.equals(getAllGroupRelationshipResult.getStatus())) {
            return Result.taskSuccess(type, taskId, username, getAllGroupRelationshipResult);
        }
        return Result.taskFail(type, taskId, username, getAllGroupRelationshipResult);
    }
}
