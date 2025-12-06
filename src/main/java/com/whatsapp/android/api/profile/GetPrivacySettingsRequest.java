package com.whatsapp.android.api.profile;

import ProtocolTree.ProtocolTreeNode;
import ProtocolTree.StanzaAttribute;
import cn.hutool.core.util.ObjectUtil;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.response.profile.GetPrivacySettingsResult;
import com.whatsapp.android.request.AbstractRequest;

import java.util.LinkedList;

/**
 * 获取隐私设置
 *
 * @author Rocky
 */
public class GetPrivacySettingsRequest extends AbstractRequest<GetPrivacySettingsResult> {

    public GetPrivacySettingsRequest() {
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.GET_PRIVACY_SETTINGS;
    }

    @Override
    public boolean request() {
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        iq.AddAttribute(new StanzaAttribute("id", getTaskId()));
        iq.AddAttribute(new StanzaAttribute("xmlns", "privacy"));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        iq.AddChild(new ProtocolTreeNode("privacy"));
        user.getGorgeousEngine().AddTask(iq);
        return true;
    }

    @Override
    public GetPrivacySettingsResult parseResult(ProtocolTreeNode node) {
        ProtocolTreeNode privacy = node.getOneChildren("privacy");
        if (ObjectUtil.isNull(privacy)) {
            return new GetPrivacySettingsResult(StatusResult.fail("获取隐私设定失败"));
        }
        LinkedList<ProtocolTreeNode> category = privacy.GetChildren("category");
        GetPrivacySettingsResult getPrivacySettingsResult = new GetPrivacySettingsResult();
        for (ProtocolTreeNode categoryNode : category) {
            String name = categoryNode.GetAttributeValue("name");
            String value = categoryNode.GetAttributeValue("value");
            if (ObjectUtil.isNotNull(name) && ObjectUtil.isNotNull(value)) {
                getPrivacySettingsResult.updateSetting(name, value);
            }
        }
        return getPrivacySettingsResult;
    }
}
