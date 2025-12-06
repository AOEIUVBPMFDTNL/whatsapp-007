package com.whatsapp.android.service.impl.contact;

import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.annotation.ApiType;
import com.whatsapp.android.api.contact.GetUserDeviceInfoRequest;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.service.ApiStrategy;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 获取用户设备信息
 *
 * @author sunnoc
 * @date 2021-12-08 12:25
 */
@Service
@ApiType(TypeConstant.TaskType.GET_USER_DEVICE_INFO)
public class GetUserDeviceInfoService implements ApiStrategy {

    @Override
    public Result execute(String type, JSONObject dataObject, String taskId, User user) {
        List<String> userList = dataObject.getJSONObject("data").getJSONArray("userList").toJavaList(String.class);
        String username = user.getLoginPack().getUsername();
        StatusResult statusResult = user.sendRequest(new GetUserDeviceInfoRequest(userList));
        if (Constant.OK.equals(statusResult.getStatus())) {
            return Result.taskSuccess(type, taskId, username, statusResult);
        }
        return Result.taskFail(type, taskId, username, statusResult);
    }
}
