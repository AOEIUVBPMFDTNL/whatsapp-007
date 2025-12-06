package com.whatsapp.android.service.impl.profile;

import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.annotation.ApiType;
import com.whatsapp.android.api.profile.DeleteProfilePictureRequest;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.service.ApiStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 删除头像
 *
 * @author sunnoc
 * @date 2025-01-09 10:51
 */
@Service
@ApiType(TypeConstant.TaskType.DELETE_HEAD_IMAGE)
@RequiredArgsConstructor
public class DeleteHeadImageService implements ApiStrategy {

    @Override
    public Result execute(String type, JSONObject dataObject, String taskId, User user) {
        String username = user.getLoginPack().getUsername();
        StatusResult statusResult = user.sendRequest(new DeleteProfilePictureRequest());
        if (Constant.OK.equals(statusResult.getStatus())) {
            return Result.taskSuccess(type, taskId, username, statusResult);
        }
        return Result.taskFail(type, taskId, username, statusResult);
    }
}
