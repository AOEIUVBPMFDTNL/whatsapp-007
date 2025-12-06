package com.whatsapp.android.api.group;

import ProtocolTree.ProtocolTreeNode;
import ProtocolTree.StanzaAttribute;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.request.AbstractRequest;

import java.nio.charset.StandardCharsets;

/**
 * 修改群主题名
 *
 * @author sunnoc
 * @date 2021-03-12 16:03
 */
public class ModifyGroupSubjectRequest extends AbstractRequest<StatusResult> {
    /**
     * 群id
     */
    private String groupId;
    /**
     * 群名称
     */
    private String subjectName;

    public ModifyGroupSubjectRequest(String groupId, String subjectName) {
        this.groupId = groupId;
        this.subjectName = subjectName;
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.MODIFY_GROUP_SUBJECT;
    }

    @Override
    public boolean request() {
        ProtocolTreeNode node = new ProtocolTreeNode("iq");
        node.AddAttribute(new StanzaAttribute("id", getTaskId()));
        node.AddAttribute(new StanzaAttribute("xmlns", "w:g2"));
        node.AddAttribute(new StanzaAttribute("type", "set"));
        node.AddAttribute(new StanzaAttribute("to", user.getGorgeousEngine().JidNormalize(groupId)));
        ProtocolTreeNode subject = new ProtocolTreeNode("subject");
        subject.SetData(subjectName.getBytes(StandardCharsets.UTF_8));
        node.AddChild(subject);
        user.getGorgeousEngine().AddTask("ModifyGroupSubject", node);
        return true;
    }

    @Override
    public StatusResult parseResult(ProtocolTreeNode node) {
        return parseBaseResult(node);
    }
}
