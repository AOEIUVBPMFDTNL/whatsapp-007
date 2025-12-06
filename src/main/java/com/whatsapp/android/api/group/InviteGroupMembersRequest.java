package com.whatsapp.android.api.group;

import ProtocolTree.ProtocolTreeNode;
import ProtocolTree.StanzaAttribute;
import Util.StringUtil;
import cn.hutool.core.util.StrUtil;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.pack.group.CreateGroupPack;
import com.whatsapp.android.entity.response.goup.GroupMember;
import com.whatsapp.android.entity.response.goup.InviteGroupMemberResult;
import com.whatsapp.android.request.AbstractRequest;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 邀请群成员
 *
 * @author sunnoc
 * @date 2021-03-12 16:17
 */
public class InviteGroupMembersRequest extends AbstractRequest<InviteGroupMemberResult> {
    private final CreateGroupPack createGroupPack;

    public InviteGroupMembersRequest(CreateGroupPack createGroupPack) {
        this.createGroupPack = createGroupPack;
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.INVITE_GROUP_MEMBERS;
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
        ProtocolTreeNode add = new ProtocolTreeNode("add");
        for (String member : createGroupPack.getList()) {
            ProtocolTreeNode participant = new ProtocolTreeNode("participant");
            participant.AddAttribute(new StanzaAttribute("jid", user.getGorgeousEngine().JidNormalize(member)));
            add.AddChild(participant);
        }
        node.AddChild(add);
        user.getGorgeousEngine().AddTask("InviteGroupMembers", node);
        return true;
    }

    @Override
    public InviteGroupMemberResult parseResult(ProtocolTreeNode node) {
        /**
         * <iq from='8617748754950-1615536008@g.us' type='result' id='66FE4D1971E44DEEBF9F265A0063F6DE'>
         *     <add>
         *         <participant jid='8618688676082@s.whatsapp.net'/>
         *     </add>
         * </iq>
         */
        InviteGroupMemberResult checkResult = checkResult(node, InviteGroupMemberResult.class);
        if (checkResult != null) {
            return checkResult;
        }
        String groupId = node.GetAttributeValue("from");
        if (StringUtils.isEmpty(groupId)) {
            return new InviteGroupMemberResult(StatusResult.fail("群id获取失败"));
        }
        ProtocolTreeNode add = node.getOneChildren("add");
        if (add == null) {
            return new InviteGroupMemberResult(StatusResult.fail("邀请群成员失败"));
        }
        LinkedList<ProtocolTreeNode> participant = add.GetChildren("participant");
        if (participant == null || participant.isEmpty()) {
            return new InviteGroupMemberResult(StatusResult.fail("邀请群成员失败"));
        }
        List<GroupMember> successMember = new ArrayList<>();
        List<GroupMember> pendingApproveMember = new ArrayList<>();
        List<GroupMember> alreadyInGroupMember = new ArrayList<>();
        List<GroupMember> failureMember = new ArrayList<>();
        List<GroupMember> unRegisteredMember = new ArrayList<>();
        for (ProtocolTreeNode protocolTreeNode : participant) {
            String error = protocolTreeNode.GetAttributeValue("error");
            String jid = protocolTreeNode.GetAttributeValue("jid");
            String phoneNumber = protocolTreeNode.GetAttributeValue("phone_number");
            if (StrUtil.isNotEmpty(phoneNumber) && phoneNumber.contains("@s.whatsapp.net")) {
                jid = phoneNumber;
            }
            GroupMember groupMember = new GroupMember(0, jid);
            if (StrUtil.isNotEmpty(error)) {
                // 拉群失败
                switch (error) {
                    case "403":
                        // 拉人失败, 被风控
                        failureMember.add(groupMember);
                        break;
                    case "421":
                        // 待审核
                        pendingApproveMember.add(groupMember);
                        break;
                    case "409":
                        // 粉丝已在群
                        alreadyInGroupMember.add(groupMember);
                        break;
                    case "404":
                        // 未注册的用户
                        unRegisteredMember.add(groupMember);
                    default:
                        failureMember.add(groupMember);
                        break;
                }
                continue;
            }
            // 成功
            successMember.add(groupMember);
        }
        InviteGroupMemberResult inviteGroupMemberResult = new InviteGroupMemberResult();
        inviteGroupMemberResult.setGroupId(groupId);
        inviteGroupMemberResult.setMembers(successMember);
        inviteGroupMemberResult.setFailureMember(failureMember);
        inviteGroupMemberResult.setPendingApproveMember(pendingApproveMember);
        inviteGroupMemberResult.setAlreadyInGroupMember(alreadyInGroupMember);
        inviteGroupMemberResult.setUnRegisteredMember(unRegisteredMember);
        return inviteGroupMemberResult;
    }
}
