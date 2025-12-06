package com.whatsapp.android.api.profile;

import ProtocolTree.ProtocolTreeNode;
import cn.hutool.core.util.ObjectUtil;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.pack.profile.UpdatePrivacySettingPack;
import com.whatsapp.android.entity.response.profile.GetPrivacySettingsResult;
import com.whatsapp.android.enums.PrivacySettingType;
import com.whatsapp.android.request.AbstractRequest;

import static com.whatsapp.android.enums.PrivacySettingType.isEffectiveSetting;

/**
 * 更新消息已读隐私设置
 *
 * @author Rocky
 */
public class UpdateReadReceiptsPrivacySettingRequest extends AbstractRequest<GetPrivacySettingsResult> {

    public final UpdatePrivacySettingPack updatePrivacySettingPack;

    public UpdateReadReceiptsPrivacySettingRequest(UpdatePrivacySettingPack updatePrivacySettingPack) {
        this.updatePrivacySettingPack = updatePrivacySettingPack;
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.UPDATE_READ_RECEIPTS_PRIVACY_SETTING;
    }

    @Override
    public boolean request() {
        String update = updatePrivacySettingPack.getReadReceipts();
        // 对应字段没有设置更新值
        if (ObjectUtil.isNull(update)) {
            return false;
        }
        // 设定的更新值不是有效值
        if (!isEffectiveSetting(update)) {
            return false;
        }
        if (!(update.equals(PrivacySettingType.ALL.getType()) || update.equals(PrivacySettingType.NONE.getType()))) {
            return false;
        }
        user.getGorgeousEngine().UpdatePrivacySetting("readreceipts", update, taskId);
        return true;
    }

    @Override
    public GetPrivacySettingsResult parseResult(ProtocolTreeNode node) {
        // <iq from='s.whatsapp.net' type='result' id='01'><privacy><category name='profile' value='none'/></privacy></iq>
        ProtocolTreeNode privacy = node.getOneChildren("privacy");
        if (ObjectUtil.isNull(privacy)) {
            return new GetPrivacySettingsResult(StatusResult.fail("更新隐私设置失败"));
        }
        ProtocolTreeNode category = privacy.getOneChildren("category");
        if (ObjectUtil.isNull(category)) {
            return new GetPrivacySettingsResult(StatusResult.fail("更新隐私设置失败"));
        }
        String name = category.GetAttributeValue("name");
        String value = category.GetAttributeValue("value");
        if (ObjectUtil.isNull(name) || ObjectUtil.isNull(value)) {
            return new GetPrivacySettingsResult(StatusResult.fail("更新隐私设置失败"));
        }
        GetPrivacySettingsResult getPrivacySettingsResult = new GetPrivacySettingsResult();
        getPrivacySettingsResult.updateSetting(name, value);
        return getPrivacySettingsResult;
    }

}
