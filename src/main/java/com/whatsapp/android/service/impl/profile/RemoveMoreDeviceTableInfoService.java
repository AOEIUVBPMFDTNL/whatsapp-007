package com.whatsapp.android.service.impl.profile;

import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.annotation.ApiType;
import com.whatsapp.android.api.profile.RemoveMoreDeviceTableInfoRequest;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.service.ApiStrategy;
import org.springframework.stereotype.Service;

/**
 * 移除多设备表信息
 *
 * @author sunnoc
 * @date 2023-06-25 10:38
 */
@Service
@ApiType(TypeConstant.TaskType.REMOVE_MORE_DEVICE_TABLE_INFO)
public class RemoveMoreDeviceTableInfoService implements ApiStrategy {

    @Override
    public Result execute(String type, JSONObject dataObject, String taskId, User user) {
        String username = user.getLoginPack().getUsername();
        StatusResult statusResult = user.sendRequest(new RemoveMoreDeviceTableInfoRequest());
        if (Constant.OK.equals(statusResult.getStatus())) {
            return Result.taskSuccess(type, taskId, username, statusResult);
        }
        return Result.taskFail(type, taskId, username, statusResult);
    }
}
