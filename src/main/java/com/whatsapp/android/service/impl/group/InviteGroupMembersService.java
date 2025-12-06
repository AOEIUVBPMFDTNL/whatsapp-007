package com.whatsapp.android.service.impl.group;

import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.annotation.ApiType;
import com.whatsapp.android.api.group.InviteGroupMembersRequest;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.entity.pack.group.CreateGroupPack;
import com.whatsapp.android.entity.response.goup.CreateGroupResult;
import com.whatsapp.android.entity.response.goup.InviteGroupMemberResult;
import com.whatsapp.android.service.ApiStrategy;
import org.springframework.stereotype.Service;

/**
 * 邀请群成员
 *
 * @author sunnoc
 * @date 2021-03-17 09:59
 */
@Service
@ApiType(TypeConstant.TaskType.INVITE_GROUP_MEMBERS)
public class InviteGroupMembersService implements ApiStrategy {
    @Override
    public Result execute(String type, JSONObject dataObject, String taskId, User user) {
        CreateGroupPack data = dataObject.getObject("data", CreateGroupPack.class);
        String username = user.getLoginPack().getUsername();
        InviteGroupMemberResult inviteGroupMemberResult = user.sendRequest(new InviteGroupMembersRequest(data));
        if (Constant.OK.equals(inviteGroupMemberResult.getStatus())) {
            return Result.taskSuccess(type, taskId, username, inviteGroupMemberResult);
        }
        return Result.taskFail(type, taskId, username, inviteGroupMemberResult);
    }
}
