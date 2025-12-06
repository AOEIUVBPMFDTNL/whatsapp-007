package com.whatsapp.android.api.profile;

import ProtocolTree.ProtocolTreeNode;
import ProtocolTree.StanzaAttribute;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.request.AbstractRequest;

/**
 * 删除
 *
 * @author sunnoc
 * @date 2025-01-09 15:40
 */
public class DeleteProfilePictureRequest extends AbstractRequest<StatusResult> {
    public DeleteProfilePictureRequest() {
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.DELETE_HEAD_IMAGE;
    }

    @Override
    public boolean request() {
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("id", getTaskId()));
        iq.AddAttribute(new StanzaAttribute("xmlns", "w:profile:picture"));
        iq.AddAttribute(new StanzaAttribute("type", "set"));
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        ProtocolTreeNode picture = new ProtocolTreeNode("picture");
        picture.AddAttribute(new StanzaAttribute("type", "image"));
        iq.AddChild(picture);
        user.getGorgeousEngine().AddTask("DeleteHead", iq);
        return true;
    }

    @Override
    public long timeOut() {
        return 60L;
    }

    @Override
    public StatusResult parseResult(ProtocolTreeNode node) {
        //成功
        //<iq from='8617748754950@s.whatsapp.net' type='result' id='B8F447CCFBAE4B17BB1641CAB164304D'></iq>
        return parseBaseResult(node);
    }
}
