package com.whatsapp.android.service.impl.profile;

import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.annotation.ApiType;
import com.whatsapp.android.api.profile.GetPrivacySettingsRequest;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.entity.response.profile.GetPrivacySettingsResult;
import com.whatsapp.android.service.ApiStrategy;
import org.springframework.stereotype.Service;

/**
 * 获取隐私设置
 *
 * @author sunnoc
 * @date 2021-04-07 16:53
 */
@Service
@ApiType(TypeConstant.TaskType.GET_PRIVACY_SETTINGS)
public class GetPrivacySettingsService implements ApiStrategy {
    @Override
    public Result execute(String type, JSONObject dataObject, String taskId, User user) {
        String username = user.getLoginPack().getUsername();
        GetPrivacySettingsResult getPrivacySettingsResult = user.sendRequest(new GetPrivacySettingsRequest());
        if (Constant.OK.equals(getPrivacySettingsResult.getStatus())) {
            return Result.taskSuccess(type, taskId, username, getPrivacySettingsResult);
        }
        return Result.taskFail(type, taskId, username, getPrivacySettingsResult);
    }
}
