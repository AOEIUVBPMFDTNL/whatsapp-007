package com.whatsapp.android.service.impl.group;

import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.annotation.ApiType;
import com.whatsapp.android.api.group.AcceptInviteToGroupRequest;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.entity.response.goup.CreateGroupResult;
import com.whatsapp.android.service.ApiStrategy;
import org.springframework.stereotype.Service;

/**
 * 通过群链接进群
 *
 * @author sunnoc
 * @date 2021-03-17 12:09
 */
@Service
@ApiType(TypeConstant.TaskType.ACCEPT_INVITE_TO_GROUP)
public class AcceptInviteToGroupService implements ApiStrategy {
    @Override
    public Result execute(String type, JSONObject dataObject, String taskId, User user) {
        String groupUrl = dataObject.getJSONObject("data").getString("groupUrl");
        String username = user.getLoginPack().getUsername();
        CreateGroupResult createGroupResult = user.sendRequest(new AcceptInviteToGroupRequest(groupUrl));
        if (Constant.OK.equals(createGroupResult.getStatus())) {
            return Result.taskSuccess(type, taskId, username, createGroupResult);
        }
        return Result.taskFail(type, taskId, username, createGroupResult);
    }
}
