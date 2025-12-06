package com.whatsapp.android.api.group;

import ProtocolTree.ProtocolTreeNode;
import ProtocolTree.StanzaAttribute;
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
 * 设置群管理员
 *
 * @author sunnoc
 * @date 2021-03-12 16:54
 */
public class SetGroupAdminRequest extends AbstractRequest<CreateGroupResult> {
    private CreateGroupPack createGroupPack;

    public SetGroupAdminRequest(CreateGroupPack createGroupPack) {
        this.createGroupPack = createGroupPack;
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.SET_GROUP_ADMIN;
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
        node.AddAttribute(new StanzaAttribute("to", user.getGorgeousEngine().JidNormalize(createGroupPack.getGroupId())));
        String tag;
        if (createGroupPack.isAdmin()) {
            tag = "promote";
        } else {
            tag = "demote";
        }
        ProtocolTreeNode remove = new ProtocolTreeNode(tag);
        for (String member : createGroupPack.getList()) {
            ProtocolTreeNode participant = new ProtocolTreeNode("participant");
            participant.AddAttribute(new StanzaAttribute("jid", user.getGorgeousEngine().JidNormalize(member)));
            remove.AddChild(participant);
        }
        node.AddChild(remove);
        if (createGroupPack.isAdmin()) {
            user.getGorgeousEngine().AddTask("PromoteGroupMember", node);
        } else {
            user.getGorgeousEngine().AddTask("DemoteGroupMember", node);
        }
        return true;
    }

    @Override
    public CreateGroupResult parseResult(ProtocolTreeNode node) {
        //提升管理员
        /**
         * <iq from='8617748754950-1615536008@g.us' type='result' id='D56D6DBFEB5A47009318476829BFF533'>
         *     <promote>
         *         <participant jid='8618688676082@s.whatsapp.net' type='admin'/>
         *     </promote>
         * </iq>
         */
        //降级
        /**
         *<iq from='8617748754950-1615536008@g.us' type='result' id='8F9442C6618B497FBA2BE1EB1B1F54CD'>
         *     <demote>
         *         <participant jid='8618688676082@s.whatsapp.net'/>
         *     </demote>
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
        ProtocolTreeNode groupNode;
        if (createGroupPack.isAdmin()) {
            groupNode = node.getOneChildren("promote");
        } else {
            groupNode = node.getOneChildren("demote");
        }
        if (groupNode == null) {
            return new CreateGroupResult(StatusResult.fail("成员权限设置失败"));
        }
        CreateGroupResult createGroupResult = new CreateGroupResult();
        List<GroupMember> list = new ArrayList<>();
        LinkedList<ProtocolTreeNode> participant = groupNode.GetChildren("participant");
        if (participant != null) {
            for (ProtocolTreeNode protocolTreeNode : participant) {
                String userId = protocolTreeNode.GetAttributeValue("jid");
                if (StringUtils.hasLength(userId)) {
                    if (createGroupPack.isAdmin()) {
                        list.add(new GroupMember(1, userId));
                    } else {
                        list.add(new GroupMember(0, userId));
                    }
                }
            }
        }
        createGroupResult.setGroupId(groupId);
        createGroupResult.setMembers(list);
        return createGroupResult;
    }
}
