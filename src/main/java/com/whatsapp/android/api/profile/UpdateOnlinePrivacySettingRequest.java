package com.whatsapp.android.api.profile;

import cn.hutool.core.util.ObjectUtil;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.pack.profile.UpdatePrivacySettingPack;
import com.whatsapp.android.enums.PrivacySettingType;

import static com.whatsapp.android.enums.PrivacySettingType.isEffectiveSetting;

/**
 * 更新在线状态隐私设置
 *
 * @author Rocky
 */
public class UpdateOnlinePrivacySettingRequest extends UpdateReadReceiptsPrivacySettingRequest {


    public UpdateOnlinePrivacySettingRequest(UpdatePrivacySettingPack updatePrivacySettingPack) {
        super(updatePrivacySettingPack);
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.UPDATE_ONLINE_PRIVACY_SETTING;
    }

    @Override
    public boolean request() {
        String update = updatePrivacySettingPack.getOnline();
        // 对应字段没有设置更新值
        if (ObjectUtil.isNull(update)) {
            return false;
        }
        // 设定的更新值不是有效值
        if (!isEffectiveSetting(update)) {
            return false;
        }
        if (!(update.equals(PrivacySettingType.MATCH_LAST_SEEN.getType()) || update.equals(PrivacySettingType.ALL.getType()))) {
            return false;
        }
        user.getGorgeousEngine().UpdatePrivacySetting("online", update, taskId);
        return true;
    }

}
