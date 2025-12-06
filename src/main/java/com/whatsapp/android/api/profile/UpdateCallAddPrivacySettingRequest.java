package com.whatsapp.android.api.profile;

import cn.hutool.core.util.ObjectUtil;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.pack.profile.UpdatePrivacySettingPack;
import com.whatsapp.android.enums.PrivacySettingType;

import static com.whatsapp.android.enums.PrivacySettingType.isEffectiveSetting;

/**
 * 更新是否接受语音通话隐私设置
 *
 * @author Rocky
 */
public class UpdateCallAddPrivacySettingRequest extends UpdateReadReceiptsPrivacySettingRequest {


    public UpdateCallAddPrivacySettingRequest(UpdatePrivacySettingPack updatePrivacySettingPack) {
        super(updatePrivacySettingPack);
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.UPDATE_CALL_ADD_PRIVACY_SETTING;
    }

    @Override
    public boolean request() {
        String update = updatePrivacySettingPack.getCallAdd();
        // 对应字段没有设置更新值
        if (ObjectUtil.isNull(update)) {
            return false;
        }
        // 设定的更新值不是有效值
        if (!isEffectiveSetting(update)) {
            return false;
        }
        if (!(update.equals(PrivacySettingType.ALL.getType()) || update.equals(PrivacySettingType.KNOWN.getType()))) {
            return false;
        }
        user.getGorgeousEngine().UpdatePrivacySetting("calladd", update, taskId);
        return true;
    }

}
