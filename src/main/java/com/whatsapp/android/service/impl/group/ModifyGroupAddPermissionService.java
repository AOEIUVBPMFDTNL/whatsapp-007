package com.whatsapp.android.service.impl.group;

import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.annotation.ApiType;
import com.whatsapp.android.api.group.ModifyGroupAddPermissionRequest;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.entity.pack.group.ModifyGroupAddPermissionPack;
import com.whatsapp.android.service.ApiStrategy;
import org.springframework.stereotype.Service;

/**
 * 修改群成员邀请权限
 *
 * @author sunnoc
 * @date 2021-10-17 11:38
 */
@Service
@ApiType(TypeConstant.TaskType.MODIFY_GROUP_ADD_PERMISSION)
public class ModifyGroupAddPermissionService implements ApiStrategy {
    @Override
    public Result execute(String type, JSONObject dataObject, String taskId, User user) {
        ModifyGroupAddPermissionPack data = dataObject.getObject("data", ModifyGroupAddPermissionPack.class);
        String username = user.getLoginPack().getUsername();
        StatusResult statusResult = user.sendRequest(new ModifyGroupAddPermissionRequest(data));
        if (Constant.OK.equals(statusResult.getStatus())) {
            return Result.taskSuccess(type, taskId, username, statusResult);
        }
        return Result.taskFail(type, taskId, username, statusResult);
    }
}
