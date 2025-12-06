package com.whatsapp.android.api.group;

import ProtocolTree.ProtocolTreeNode;
import ProtocolTree.StanzaAttribute;
import Util.StringUtil;
import cn.hutool.core.util.StrUtil;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.pack.group.CreateGroupPack;
import com.whatsapp.android.entity.response.goup.CreateGroupResult;
import com.whatsapp.android.request.AbstractRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 获取群所有请求进群成员信息
 *
 * @author Rocky
 */
@Slf4j
public class GetGroupMembershipApprovalRequest extends AbstractRequest<CreateGroupResult> {

    private CreateGroupPack createGroupPack;

    public GetGroupMembershipApprovalRequest(CreateGroupPack createGroupPack) {
        this.createGroupPack = createGroupPack;
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.GET_GROUP_MEMBERSHIP_APPROVAL;
    }

    @Override
    public boolean request() {
        /**
         * <iq xmlns='w:g2' id='02' type='get' to='120363358760589965@g.us'>
         *     <membership_approval_requests />
         * </iq>
         */
        String groupId = createGroupPack.getGroupId();
        if (StrUtil.isEmpty(groupId) || !StringUtil.IsGroupJid(groupId)) {
            return false;
        }
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("id", getTaskId()));
        iq.AddAttribute(new StanzaAttribute("xmlns", "w:g2"));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        iq.AddAttribute(new StanzaAttribute("to", groupId));
        ProtocolTreeNode membershipApprovalRequests = new ProtocolTreeNode("membership_approval_requests");
        iq.AddChild(membershipApprovalRequests);
        user.getGorgeousEngine().AddTask("GetGroupMembershipApproval", iq);
        return true;
    }

    @Override
    public CreateGroupResult parseResult(ProtocolTreeNode node) {
        /**
         * <iq from='120363417361032304@g.us' type='result' id='1748229874-4' addressing_mode='lid'>
         *     <membership_approval_requests>
         *         <membership_approval_request jid='2349022630277@s.whatsapp.net' request_method='invite_link'
         *             request_time='1747901682' />
         *         <membership_approval_request jid='2349028766394@s.whatsapp.net' request_method='invite_link'
         *             request_time='1747901682' />
         *     </membership_approval_requests>
         * </iq>x
         *
         * */
        CreateGroupResult checkResult = checkResult(node, CreateGroupResult.class);
        if (checkResult != null) {
            return checkResult;
        }
        boolean pnAddressingMode = true;
        String addressingMode = node.GetAttributeValue("addressing_mode");
        if (addressingMode != null && addressingMode.equals("lid")) {
            pnAddressingMode = false;
        }
        String groupId = node.GetAttributeValue("from");
        if (StringUtils.isEmpty(groupId)) {
            return new CreateGroupResult(StatusResult.fail("群id获取失败"));
        }
        ProtocolTreeNode membershipApprovalRequests = node.getOneChildren("membership_approval_requests");
        if (membershipApprovalRequests == null) {
            return new CreateGroupResult(StatusResult.fail("获取待审批成员失败"));
        }
        LinkedList<ProtocolTreeNode> membershipApprovalRequest = membershipApprovalRequests.GetChildren("membership_approval_request");
        List<String> requestMemberList = new ArrayList<>();
        for (ProtocolTreeNode protocolTreeNode : membershipApprovalRequest) {
            if (pnAddressingMode) {
                String jid = protocolTreeNode.GetAttributeValue("jid");
                if (StrUtil.isNotEmpty(jid)) {
                    requestMemberList.add(jid);
                }
            } else {
                String phoneNumber = protocolTreeNode.GetAttributeValue("phone_number");
                if (StrUtil.isNotEmpty(phoneNumber)) {
                    requestMemberList.add(phoneNumber);
                } else {
                    // 还是以jid形式传
                    String jid = protocolTreeNode.GetAttributeValue("jid");
                    if (StrUtil.isNotEmpty(jid)) {
                        requestMemberList.add(jid);
                    }
                }
            }
        }
        CreateGroupResult createGroupResult = new CreateGroupResult();
        createGroupResult.setGroupId(groupId);
        createGroupResult.setApproveMemberJoin(requestMemberList);
        return createGroupResult;
    }
}
