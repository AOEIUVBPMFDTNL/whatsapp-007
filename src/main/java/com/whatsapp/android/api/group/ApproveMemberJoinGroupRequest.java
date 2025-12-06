package com.whatsapp.android.api.group;

import ProtocolTree.ProtocolTreeNode;
import ProtocolTree.StanzaAttribute;
import cn.hutool.core.util.StrUtil;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.pack.group.CreateGroupPack;
import com.whatsapp.android.entity.response.goup.CreateGroupResult;
import com.whatsapp.android.request.AbstractRequest;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 处理请求进群请求
 */
public class ApproveMemberJoinGroupRequest extends AbstractRequest<CreateGroupResult> {
    private CreateGroupPack createGroupPack;

    public ApproveMemberJoinGroupRequest(CreateGroupPack createGroupPack) {
        this.createGroupPack = createGroupPack;
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.APPROVE_MEMBER_JOIN_GROUP;
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
        String approval;
        if (createGroupPack.isApproval()) {
            approval = "approve";
        } else {
            approval = "reject";
        }
        ProtocolTreeNode membershipRequestsAction = new ProtocolTreeNode("membership_requests_action");
        ProtocolTreeNode approvalNode = new ProtocolTreeNode(approval);
        for (String member : createGroupPack.getList()) {
            ProtocolTreeNode participant = new ProtocolTreeNode("participant");
            participant.AddAttribute(new StanzaAttribute("jid", user.getGorgeousEngine().JidNormalize(member)));
            approvalNode.AddChild(participant);
        }
        membershipRequestsAction.AddChild(approvalNode);
        node.AddChild(membershipRequestsAction);
        if (createGroupPack.isApproval()) {
            user.getGorgeousEngine().AddTask("ApproveMemberJoin", node);
        } else {
            user.getGorgeousEngine().AddTask("RejectMemberJoin", node);
        }
        return true;
    }

    @Override
    public CreateGroupResult parseResult(ProtocolTreeNode node) {
        // 批准入群
        /**
         *<iq from='120363318071985335@g.us' type='result' id='02'>
         *     <membership_requests_action>
         *         <approve>
         *             <participant jid='27824409458@s.whatsapp.net' />
         *         </approve>
         *     </membership_requests_action>
         * </iq>
         */
        // 拒绝入群
        /**
         *<iq from='120363318071985335@g.us' type='result' id='02'>
         *     <membership_requests_action>
         *         <reject>
         *             <participant jid='27824409458@s.whatsapp.net' />
         *         </reject>
         *     </membership_requests_action>
         * </iq>
         */
        /**
         * <iq from='120363417843328277@g.us' type='result' id='02372' addressing_mode='lid'>
         *     <membership_requests_action>
         *         <approve>
         *             <participant jid='7232959869091.1:0@lid' phone_number='27676809720@s.whatsapp.net' />
         *         </approve>
         *     </membership_requests_action>
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
        ProtocolTreeNode membershipRequestsAction = node.getOneChildren("membership_requests_action");
        if (membershipRequestsAction == null) {
            return new CreateGroupResult(StatusResult.fail("审批成员进群失败"));
        }
        if (createGroupPack.isApproval()) {
            groupNode = membershipRequestsAction.getOneChildren("approve");
        } else {
            groupNode = membershipRequestsAction.getOneChildren("reject");
        }
        if (groupNode == null) {
            return new CreateGroupResult(StatusResult.fail("审批成员进群失败"));
        }
        CreateGroupResult createGroupResult = new CreateGroupResult();
        List<String> list = new ArrayList<>();
        LinkedList<ProtocolTreeNode> participant = groupNode.GetChildren("participant");
        if (participant != null) {
            for (ProtocolTreeNode protocolTreeNode : participant) {
                String userId = protocolTreeNode.GetAttributeValue("jid");
                StanzaAttribute error = protocolTreeNode.GetAttribute("error");
                if (error != null) {
                    if (error.value_.equals("404")) {
                        return new CreateGroupResult(StatusResult.fail("审批成员进群失败, 请求已被处理"));
                    }
                    return new CreateGroupResult(StatusResult.fail("审批成员进群响应失败"));
                }
                if (StringUtils.hasLength(userId)) {
                    String phoneNumber = protocolTreeNode.GetAttributeValue("phone_number");
                    if (StrUtil.isNotEmpty(phoneNumber)) {
                        list.add(phoneNumber);
                    } else {
                        list.add(userId);
                    }
                }
            }
        }
        createGroupResult.setGroupId(groupId);
        createGroupResult.setApproveMemberJoin(list);
        return createGroupResult;
    }
}
