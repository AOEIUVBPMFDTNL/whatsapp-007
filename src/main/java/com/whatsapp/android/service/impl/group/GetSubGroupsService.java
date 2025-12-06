package com.whatsapp.android.service.impl.group;

import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.annotation.ApiType;
import com.whatsapp.android.api.group.GetSubGroupsRequest;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.entity.pack.group.CreateGroupPack;
import com.whatsapp.android.entity.response.goup.GetAllGroupRelationshipResult;
import com.whatsapp.android.service.ApiStrategy;
import org.springframework.stereotype.Service;

/**
 * 获取社群下的子群id
 */
@Service
@ApiType(TypeConstant.TaskType.GET_SUB_GROUPS)
public class GetSubGroupsService implements ApiStrategy {
    @Override
    public Result execute(String type, JSONObject dataObject, String taskId, User user) {
        CreateGroupPack data = dataObject.getObject("data", CreateGroupPack.class);
        String username = user.getLoginPack().getUsername();
        GetAllGroupRelationshipResult getSubGroupsResult = user.sendRequest(new GetSubGroupsRequest(data.getGroupId()));
        if (Constant.OK.equals(getSubGroupsResult.getStatus())) {
            return Result.taskSuccess(type, taskId, username, getSubGroupsResult);
        }
        return Result.taskFail(type, taskId, username, getSubGroupsResult);
    }
}
