package com.whatsapp.android.service.impl.group;

import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.annotation.ApiType;
import com.whatsapp.android.api.group.ModifyGroupMembershipApprovalModeRequest;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.entity.pack.group.ModifyGroupMembershipApprovalModePack;
import com.whatsapp.android.service.ApiStrategy;
import org.springframework.stereotype.Service;

/**
 * 修改是否开启管理员审批群成员权限
 *
 * @author sunnoc
 * @date 2021-10-17 11:38
 */
@Service
@ApiType(TypeConstant.TaskType.MODIFY_GROUP_MEMBERSHIP_APPROVAL_MODE)
public class ModifyGroupMembershipApprovalModeService implements ApiStrategy {
    @Override
    public Result execute(String type, JSONObject dataObject, String taskId, User user) {
        ModifyGroupMembershipApprovalModePack data = dataObject.getObject("data", ModifyGroupMembershipApprovalModePack.class);
        String username = user.getLoginPack().getUsername();
        StatusResult statusResult = user.sendRequest(new ModifyGroupMembershipApprovalModeRequest(data));
        if (Constant.OK.equals(statusResult.getStatus())) {
            return Result.taskSuccess(type, taskId, username, statusResult);
        }
        return Result.taskFail(type, taskId, username, statusResult);
    }
}
