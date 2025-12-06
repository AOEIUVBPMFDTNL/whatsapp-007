package com.whatsapp.android.api.profile;

import ProtocolTree.ProtocolTreeNode;
import ProtocolTree.StanzaAttribute;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.request.AbstractRequest;

import java.nio.charset.StandardCharsets;

/**
 * 设置状态
 *
 * @author sunnoc
 * @date 2021-03-11 11:12
 */
public class SetStatusRequest extends AbstractRequest<StatusResult> {
    /**
     * 状态，可以随便设置
     */
    private String status;

    public SetStatusRequest(String status) {
        this.status = status;
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.SET_STATUS;
    }

    @Override
    public boolean request() {
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("id", getTaskId()));
        iq.AddAttribute(new StanzaAttribute("xmlns", "status"));
        iq.AddAttribute(new StanzaAttribute("type", "set"));
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        ProtocolTreeNode statusNode = new ProtocolTreeNode("status");
        statusNode.SetData(status.getBytes(StandardCharsets.UTF_8));
        iq.AddChild(statusNode);
        user.getGorgeousEngine().AddTask("SetStatue", iq);
        return true;
    }

    @Override
    public StatusResult parseResult(ProtocolTreeNode node) {
        return parseBaseResult(node);
    }
}
