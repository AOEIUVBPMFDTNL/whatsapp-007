package com.whatsapp.android.api.group;

import ProtocolTree.ProtocolTreeNode;
import ProtocolTree.StanzaAttribute;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.response.goup.CreateGroupResult;
import com.whatsapp.android.entity.response.goup.GetAllGroupRelationshipResult;
import com.whatsapp.android.entity.response.goup.GroupMember;
import com.whatsapp.android.request.AbstractRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 获取小号下所有的群关系
 *
 * @author Rocky
 */
@Slf4j
public class GetAllGroupRelationshipRequest extends AbstractRequest<GetAllGroupRelationshipResult> {
    @Override
    public String funcName() {
        return TypeConstant.TaskType.GET_ALL_GROUP_RELATIONSHIP;
    }

    @Override
    public boolean request() {
        // <iq id='01' xmlns='w:g2' type='get' to='g.us'><participating><participants/><description/></participating></iq>
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("id", getTaskId()));
        iq.AddAttribute(new StanzaAttribute("xmlns", "w:g2"));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        iq.AddAttribute(new StanzaAttribute("to", "g.us"));
        ProtocolTreeNode participating = new ProtocolTreeNode("participating");
        ProtocolTreeNode description = new ProtocolTreeNode("description");
        participating.AddChild(description);
        iq.AddChild(participating);
        user.getGorgeousEngine().AddTask("GetAllGroupRelationship", iq);
        return true;
    }

    @Override
    public GetAllGroupRelationshipResult parseResult(ProtocolTreeNode node) {
        GetAllGroupRelationshipResult checkResult = checkResult(node, GetAllGroupRelationshipResult.class);
        if (ObjectUtil.isNotNull(checkResult)) {
            return checkResult;
        }
        ProtocolTreeNode groupsNode = node.getOneChildren("groups");
        if (ObjectUtil.isNull(groupsNode)) {
            return new GetAllGroupRelationshipResult(StatusResult.fail("获取所有群关系失败"));
        }
        LinkedList<ProtocolTreeNode> groupList = groupsNode.GetChildren("group");
        if (groupList.isEmpty()) {
            return new GetAllGroupRelationshipResult(new ArrayList<>());
        }
        List<CreateGroupResult> createGroupResults = new ArrayList<>();
        for (ProtocolTreeNode groupNode : groupList) {
            boolean addressingModeJid = true;
            String addressingMode = groupNode.GetAttributeValue("addressing_mode");
            if (StrUtil.isNotBlank(addressingMode)) {
                if (addressingMode.equals("lid")) {
                    addressingModeJid = false;
                }
            }
            CreateGroupResult createGroupResult = new CreateGroupResult();
            String groupId = groupNode.GetAttributeValue("id");
            String creator;
            if (addressingModeJid) {
                creator = groupNode.GetAttributeValue("s_o");
            } else {
                creator = groupNode.GetAttributeValue("s_o_pn");
            }
            if (!StringUtils.hasLength(creator)) {
                if (addressingModeJid) {
                    creator = groupNode.GetAttributeValue("creator");
                } else {
                    creator = groupNode.GetAttributeValue("creator_pn");
                }
            }
            String subject = groupNode.GetAttributeValue("subject");
            if (org.springframework.util.StringUtils.isEmpty(groupId) || org.springframework.util.StringUtils.isEmpty(subject)) {
                return new GetAllGroupRelationshipResult(StatusResult.fail("群相关信息获取失败"));
            }
            ProtocolTreeNode descNode = groupNode.getOneChildren("description");
            if (descNode != null) {
                String descId = descNode.GetAttributeValue("id");
                createGroupResult.setDescId(descId);
                ProtocolTreeNode bodyNode = descNode.getOneChildren("body");
                if (bodyNode != null) {
                    byte[] bytes = bodyNode.GetData();
                    String desc = new String(bytes, StandardCharsets.UTF_8);
                    createGroupResult.setDesc(desc);
                }
            }

            List<GroupMember> list = new ArrayList<>();
            LinkedList<ProtocolTreeNode> participant = groupNode.GetChildren("participant");
            if (participant != null) {
                for (ProtocolTreeNode protocolTreeNode : participant) {
                    int type = 0;
                    String userId;
                    if (addressingModeJid) {
                        userId = protocolTreeNode.GetAttributeValue("jid");
                    } else {
                        String phoneNumber = protocolTreeNode.GetAttributeValue("phone_number");
                        if (StrUtil.isNotBlank(phoneNumber)) {
                            userId = protocolTreeNode.GetAttributeValue("phone_number");
                        } else {
                            continue;
                        }
                    }
                    String authority = protocolTreeNode.GetAttributeValue("type");
                    if ("superadmin".equals(authority)) {
                        if (org.springframework.util.StringUtils.isEmpty(creator)) {
                            creator = userId;
                        }
                        type = 2;
                    } else if ("admin".equals(authority)) {
                        if (userId.equals(creator)) {
                            type = 2;
                        } else {
                            type = 1;
                        }
                    }
                    if (StringUtils.hasLength(userId)) {
                        list.add(new GroupMember(type, userId));
                    }
                }
            }
            if (StrUtil.indexOf(groupId, "@g.us", 0, false) == -1) {
                createGroupResult.setGroupId(groupId + "@g.us");
            } else {
                createGroupResult.setGroupId(groupId);
            }
            createGroupResult.setCreator(creator);
            createGroupResult.setSubjectName(subject);
            createGroupResult.setMembers(list);
            ProtocolTreeNode announcementNode = groupNode.getOneChildren("announcement");
            ProtocolTreeNode lockedNode = groupNode.getOneChildren("locked");
            ProtocolTreeNode suspended = groupNode.getOneChildren("suspended");
            ProtocolTreeNode memberAddMode = groupNode.getOneChildren("member_add_mode");
            ProtocolTreeNode membershipApprovalMode = groupNode.getOneChildren("membership_approval_mode");
            ProtocolTreeNode parent = groupNode.getOneChildren("parent");
            ProtocolTreeNode linkedParent = groupNode.getOneChildren("linked_parent");
            ProtocolTreeNode defaultSubGroup = groupNode.getOneChildren("default_sub_group");
            createGroupResult.setForbid(announcementNode != null);
            createGroupResult.setEditGroupLocked(lockedNode != null);
            createGroupResult.setBanned(suspended != null);
            String add = new String(memberAddMode.GetData(), StandardCharsets.UTF_8);
            if (add.equals("all_member_add")) {
                createGroupResult.setAllMemberAdd(true);
            } else {
                createGroupResult.setAllMemberAdd(false);
            }
            if (ObjectUtil.isNotNull(membershipApprovalMode)) {
                createGroupResult.setMemberAddApprovalOn(true);
            }
            if (ObjectUtil.isNotNull(parent)) {
                createGroupResult.setCommunity(true);
            }
            if (ObjectUtil.isNotNull(linkedParent)) {
                createGroupResult.setSubGroup(true);
                String jid = linkedParent.GetAttributeValue("jid");
                if (StrUtil.isNotEmpty(jid)) {
                    createGroupResult.setLinkedParentGroupId(jid);
                }
            }
            if (ObjectUtil.isNotNull(defaultSubGroup)) {
                createGroupResult.setDefaultSubGroup(true);
            }
            createGroupResults.add(createGroupResult);
        }
        return new GetAllGroupRelationshipResult(createGroupResults);
    }
}
