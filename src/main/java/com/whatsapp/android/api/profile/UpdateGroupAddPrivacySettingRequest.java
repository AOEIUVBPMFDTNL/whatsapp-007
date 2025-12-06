package com.whatsapp.android.api.profile;

import cn.hutool.core.util.ObjectUtil;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.pack.profile.UpdatePrivacySettingPack;
import com.whatsapp.android.enums.PrivacySettingType;

import static com.whatsapp.android.enums.PrivacySettingType.isEffectiveSetting;

/**
 * 更新是否接受群邀请隐私设置
 *
 * @author Rocky
 */
public class UpdateGroupAddPrivacySettingRequest extends UpdateReadReceiptsPrivacySettingRequest {


    public UpdateGroupAddPrivacySettingRequest(UpdatePrivacySettingPack updatePrivacySettingPack) {
        super(updatePrivacySettingPack);
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.UPDATE_GROUP_ADD_PRIVACY_SETTING;
    }

    @Override
    public boolean request() {
        String update = updatePrivacySettingPack.getGroupAdd();
        // 对应字段没有设置更新值
        if (ObjectUtil.isNull(update)) {
            return false;
        }
        // 设定的更新值不是有效值
        if (!isEffectiveSetting(update)) {
            return false;
        }
        if (!(update.equals(PrivacySettingType.ALL.getType()) || update.equals(PrivacySettingType.CONTACTS.getType()))) {
            return false;
        }
        user.getGorgeousEngine().UpdatePrivacySetting("groupadd", update, taskId);
        return true;
    }

}
