package com.whatsapp.android.service.impl.group;

import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.annotation.ApiType;
import com.whatsapp.android.api.group.SetGroupAdminRequest;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.entity.pack.group.CreateGroupPack;
import com.whatsapp.android.entity.response.goup.CreateGroupResult;
import com.whatsapp.android.service.ApiStrategy;
import org.springframework.stereotype.Service;

/**
 * 设置管理员
 *
 * @author sunnoc
 * @date 2021-03-17 10:12
 */
@Service
@ApiType(TypeConstant.TaskType.SET_GROUP_ADMIN)
public class SetGroupAdminService implements ApiStrategy {
    @Override
    public Result execute(String type, JSONObject dataObject, String taskId, User user) {
        CreateGroupPack data = dataObject.getObject("data", CreateGroupPack.class);
        String username = user.getLoginPack().getUsername();
        CreateGroupResult createGroupResult = user.sendRequest(new SetGroupAdminRequest(data));
        if (Constant.OK.equals(createGroupResult.getStatus())) {
            return Result.taskSuccess(type, taskId, username, createGroupResult);
        }
        return Result.taskFail(type, taskId, username, createGroupResult);
    }
}
