package com.whatsapp.android.api.profile;

import ProtocolTree.ProtocolTreeNode;
import ProtocolTree.StanzaAttribute;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.request.AbstractRequest;
import com.whatsapp.android.util.WhatsAppUtils;
import org.springframework.util.StringUtils;

/**
 * 修改头像
 *
 * @author sunnoc
 * @date 2021-03-10 15:40
 */
public class ModifyHeadImageRequest extends AbstractRequest<StatusResult> {
    private byte[] file;
    private String groupId;

    public ModifyHeadImageRequest(byte[] file, String groupId) {
        this.file = file;
        this.groupId = groupId;
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.MODIFY_HEAD_IMAGE;
    }

    @Override
    public boolean request() {
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("id", getTaskId()));
        iq.AddAttribute(new StanzaAttribute("xmlns", "w:profile:picture"));
        iq.AddAttribute(new StanzaAttribute("type", "set"));
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        if (StringUtils.hasLength(groupId)) {
            iq.AddAttribute(new StanzaAttribute("target", WhatsAppUtils.JidNormalize(groupId)));
        }
        ProtocolTreeNode picture = new ProtocolTreeNode("picture");
        picture.AddAttribute(new StanzaAttribute("type", "image"));
        picture.SetData(file);
        iq.AddChild(picture);
        user.getGorgeousEngine().AddTask("SetHDHead", iq);
        return true;
    }

    @Override
    public long timeOut() {
        return 60L;
    }

    @Override
    public StatusResult parseResult(ProtocolTreeNode node) {
        //成功
        //<iq from='8617748754950@s.whatsapp.net' type='result' id='B8F447CCFBAE4B17BB1641CAB164304D'><picture id='1615370967'/></iq>
        //失败
        //<iq from='8617748754950@s.whatsapp.net' type='error' id='2E95DBF407AE4610804876F8E6AE54BB'><error code='406' text='not-acceptable'/></iq>
        return parseBaseResult(node);
    }
}
