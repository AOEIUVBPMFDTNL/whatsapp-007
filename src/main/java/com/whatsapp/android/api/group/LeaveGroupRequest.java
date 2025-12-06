package com.whatsapp.android.api.group;

import ProtocolTree.ProtocolTreeNode;
import ProtocolTree.StanzaAttribute;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.pack.group.CreateGroupPack;
import com.whatsapp.android.request.AbstractRequest;

/**
 * 离开群
 *
 * @author sunnoc
 * @date 2021-03-12 17:15
 */
public class LeaveGroupRequest extends AbstractRequest<StatusResult> {
    private CreateGroupPack createGroupPack;

    public LeaveGroupRequest(CreateGroupPack createGroupPack) {
        this.createGroupPack = createGroupPack;
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.LEAVE_GROUP;
    }

    @Override
    public boolean request() {
        if (createGroupPack.getList() == null) {
            return false;
        }
        ProtocolTreeNode node = new ProtocolTreeNode("iq");
        node.AddAttribute(new StanzaAttribute("id", getTaskId()));
        node.AddAttribute(new StanzaAttribute("xmlns", "w:g2"));
        node.AddAttribute(new StanzaAttribute("type", "set"));
        node.AddAttribute(new StanzaAttribute("to", "g.us"));
        ProtocolTreeNode leave = new ProtocolTreeNode("leave");
        for (String groupJid : createGroupPack.getList()) {
            ProtocolTreeNode group;
            if (createGroupPack.isCommunity()) {
                group = new ProtocolTreeNode("linked_groups");
                group.AddAttribute(new StanzaAttribute("parent_group_jid", user.getGorgeousEngine().JidNormalize(groupJid)));
            } else {
                group = new ProtocolTreeNode("group");
                group.AddAttribute(new StanzaAttribute("id", user.getGorgeousEngine().JidNormalize(groupJid)));
            }
            leave.AddChild(group);
        }
        node.AddChild(leave);
        user.getGorgeousEngine().AddTask("LeaveGroup", node);
        return true;
    }

    @Override
    public StatusResult parseResult(ProtocolTreeNode node) {
        return parseBaseResult(node);
    }
}
