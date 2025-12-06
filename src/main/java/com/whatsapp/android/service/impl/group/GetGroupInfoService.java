package com.whatsapp.android.service.impl.group;

import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.annotation.ApiType;
import com.whatsapp.android.api.group.GetGroupInfoRequest;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.entity.pack.group.CreateGroupPack;
import com.whatsapp.android.entity.response.goup.CreateGroupResult;
import com.whatsapp.android.service.ApiStrategy;
import org.springframework.stereotype.Service;

/**
 * 获取群信息
 *
 * @author sunnoc
 * @date 2021-03-17 09:57
 */
@Service
@ApiType(TypeConstant.TaskType.GET_GROUP_INFO)
public class GetGroupInfoService implements ApiStrategy {
    @Override
    public Result execute(String type, JSONObject dataObject, String taskId, User user) {
        CreateGroupPack data = dataObject.getObject("data", CreateGroupPack.class);
        String username = user.getLoginPack().getUsername();
        CreateGroupResult createGroupResult = user.sendRequest(new GetGroupInfoRequest(data));
        if (Constant.OK.equals(createGroupResult.getStatus())) {
            return Result.taskSuccess(type, taskId, username, createGroupResult);
        }
        return Result.taskFail(type, taskId, username, createGroupResult);
    }
}
