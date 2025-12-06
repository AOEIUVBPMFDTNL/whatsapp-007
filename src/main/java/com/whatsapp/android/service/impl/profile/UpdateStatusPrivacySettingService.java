package com.whatsapp.android.service.impl.profile;

import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.annotation.ApiType;
import com.whatsapp.android.api.profile.UpdateStatusPrivacySettingRequest;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.entity.pack.profile.UpdatePrivacySettingPack;
import com.whatsapp.android.entity.response.profile.GetPrivacySettingsResult;
import com.whatsapp.android.service.ApiStrategy;
import org.springframework.stereotype.Service;

/**
 * 更新发送状态隐私设置
 *
 * @author Rocky
 */
@Service
@ApiType(TypeConstant.TaskType.UPDATE_STATUS_PRIVACY_SETTING)
public class UpdateStatusPrivacySettingService implements ApiStrategy {
    @Override
    public Result execute(String type, JSONObject dataObject, String taskId, User user) {
        String username = user.getLoginPack().getUsername();
        UpdatePrivacySettingPack data = dataObject.getObject("data", UpdatePrivacySettingPack.class);
        GetPrivacySettingsResult getPrivacySettingsResult = user.sendRequest(new UpdateStatusPrivacySettingRequest(data));
        if (Constant.OK.equals(getPrivacySettingsResult.getStatus())) {
            return Result.taskSuccess(type, taskId, username, getPrivacySettingsResult);
        }
        return Result.taskFail(type, taskId, username, getPrivacySettingsResult);
    }
}
