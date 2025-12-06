package com.whatsapp.android.service.impl.profile;

import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.annotation.ApiType;
import com.whatsapp.android.api.profile.RemoveCompanionDeviceRequest;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.service.ApiStrategy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 移除多设备
 *
 * @author sunnoc
 * @date 2022-12-29 10:30
 */
@Service
@ApiType(TypeConstant.TaskType.REMOVE_COMPANION_DEVICE)
public class RemoveCompanionDeviceService implements ApiStrategy {

    @Override
    public Result execute(String type, JSONObject dataObject, String taskId, User user) {
        String userId = dataObject.getJSONObject("data").getString("userId");
        String username = user.getLoginPack().getUsername();
        if (StringUtils.isEmpty(userId)) {
            return Result.taskFail(type, taskId, username, StatusResult.fail("用户id不能为空"));
        }
        StatusResult statusResult = user.sendRequest(new RemoveCompanionDeviceRequest(userId));
        if (Constant.OK.equals(statusResult.getStatus())) {
            return Result.taskSuccess(type, taskId, username, statusResult);
        }
        return Result.taskFail(type, taskId, username, statusResult);
    }
}
