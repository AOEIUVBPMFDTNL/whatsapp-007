package com.whatsapp.android.api.group;

import ProtocolTree.ProtocolTreeNode;
import ProtocolTree.StanzaAttribute;
import Util.StringUtil;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.pack.group.CreateGroupPack;
import com.whatsapp.android.entity.response.goup.CreateGroupResult;
import com.whatsapp.android.entity.response.goup.GroupMember;
import com.whatsapp.android.request.AbstractRequest;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


/**
 * 移除群成员
 *
 * @author sunnoc
 * @date 2021-03-12 16:33
 */
public class RemoveGroupMembersRequest extends AbstractRequest<CreateGroupResult> {
    private CreateGroupPack createGroupPack;

    public RemoveGroupMembersRequest(CreateGroupPack createGroupPack) {
        this.createGroupPack = createGroupPack;
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.REMOVE_GROUP_MEMBERS;
    }

    @Override
    public boolean request() {
        if (StringUtil.isEmpty(createGroupPack.getGroupId()) || createGroupPack.getList() == null) {
            return false;
        }
        ProtocolTreeNode node = new ProtocolTreeNode("iq");
        node.AddAttribute(new StanzaAttribute("id", getTaskId()));
        node.AddAttribute(new StanzaAttribute("xmlns", "w:g2"));
        node.AddAttribute(new StanzaAttribute("type", "set"));
        node.AddAttribute(new StanzaAttribute("to", user.getGorgeousEngine().JidNormalize(createGroupPack.getGroupId())));
        ProtocolTreeNode remove = new ProtocolTreeNode("remove");
        for (String member : createGroupPack.getList()) {
            ProtocolTreeNode participant = new ProtocolTreeNode("participant");
            participant.AddAttribute(new StanzaAttribute("jid", user.getGorgeousEngine().JidNormalize(member)));
            remove.AddChild(participant);
        }
        node.AddChild(remove);
        user.getGorgeousEngine().AddTask("RemoveGroupMembers", node);
        return true;
    }

    @Override
    public CreateGroupResult parseResult(ProtocolTreeNode node) {
        /**
         * <iq from='8617748754950-1615536008@g.us' type='result' id='079E2C5AFB1B4B36BEA350FCB5305C70'>
         *     <remove>
         *         <participant jid='8618688676082@s.whatsapp.net'/>
         *     </remove>
         * </iq>
         */
        CreateGroupResult checkResult = checkResult(node, CreateGroupResult.class);
        if (checkResult != null) {
            return checkResult;
        }
        String groupId = node.GetAttributeValue("from");
        if (StringUtils.isEmpty(groupId)) {
            return new CreateGroupResult(StatusResult.fail("群id获取失败"));
        }
        ProtocolTreeNode groupNode = node.getOneChildren("remove");
        if (groupNode == null) {
            return new CreateGroupResult(StatusResult.fail("移除群成员失败"));
        }
        CreateGroupResult createGroupResult = new CreateGroupResult();
        List<GroupMember> list = new ArrayList<>();
        LinkedList<ProtocolTreeNode> participant = groupNode.GetChildren("participant");
        if (participant != null) {
            for (ProtocolTreeNode protocolTreeNode : participant) {
                String userId = protocolTreeNode.GetAttributeValue("jid");
                if (StringUtils.hasLength(userId)) {
                    list.add(new GroupMember(0, userId));
                }
            }
        }
        createGroupResult.setGroupId(groupId);
        createGroupResult.setMembers(list);
        return createGroupResult;
    }
}
