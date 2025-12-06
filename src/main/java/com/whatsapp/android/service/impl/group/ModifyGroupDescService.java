package com.whatsapp.android.service.impl.group;

import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.annotation.ApiType;
import com.whatsapp.android.api.group.ModifyGroupDescRequest;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.entity.pack.group.ModifyGroupDesc;
import com.whatsapp.android.service.ApiStrategy;
import org.springframework.stereotype.Service;

/**
 * 修改群描述
 *
 * @author sunnoc
 * @date 2021-06-07 13:10
 */
@Service
@ApiType(TypeConstant.TaskType.MODIFY_GROUP_DESC)
public class ModifyGroupDescService implements ApiStrategy {
    @Override
    public Result execute(String type, JSONObject dataObject, String taskId, User user) {
        ModifyGroupDesc data = dataObject.getObject("data", ModifyGroupDesc.class);
        String username = user.getLoginPack().getUsername();
        StatusResult statusResult = user.sendRequest(new ModifyGroupDescRequest(data));
        if (Constant.OK.equals(statusResult.getStatus())) {
            return Result.taskSuccess(type, taskId, username, statusResult);
        }
        return Result.taskFail(type, taskId, username, statusResult);
    }
}
