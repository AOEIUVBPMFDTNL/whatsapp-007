package com.whatsapp.android.api.group;

import ProtocolTree.ProtocolTreeNode;
import ProtocolTree.StanzaAttribute;
import ProtocolTree.XmppJid;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.pack.group.CreateGroupPack;
import com.whatsapp.android.entity.response.goup.CreateGroupResult;
import com.whatsapp.android.entity.response.goup.GroupMember;
import com.whatsapp.android.request.AbstractRequest;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 获取群信息
 *
 * @author sunnoc
 * @date 2021-03-12 17:36
 */
public class GetGroupInfoRequest extends AbstractRequest<CreateGroupResult> {
    private CreateGroupPack createGroupPack;

    public GetGroupInfoRequest(CreateGroupPack createGroupPack) {
        this.createGroupPack = createGroupPack;
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.GET_GROUP_INFO;
    }

    @Override
    public boolean request() {
        if (StringUtils.isEmpty(createGroupPack.getGroupId())) {
            return false;
        }
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("id", getTaskId()));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        iq.AddAttribute(new StanzaAttribute("to", user.getGorgeousEngine().JidNormalize(createGroupPack.getGroupId())));
        iq.AddAttribute(new StanzaAttribute("xmlns", "w:g2"));
        ProtocolTreeNode query = new ProtocolTreeNode("query");
        query.AddAttribute(new StanzaAttribute("request", "interactive"));
        iq.AddChild(query);
        user.getGorgeousEngine().AddTask("GetGroupInfo", iq);
        return true;
    }

    @Override
    public CreateGroupResult parseResult(ProtocolTreeNode node) {
        CreateGroupResult checkResult = checkResult(node, CreateGroupResult.class);
        if (checkResult != null) {
            return checkResult;
        }
        ProtocolTreeNode groupNode = node.getOneChildren("group");
        if (groupNode == null) {
            return new CreateGroupResult(StatusResult.fail("获取群信息失败"));
        }
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
        if (StringUtils.isEmpty(groupId) || StringUtils.isEmpty(subject)) {
            return new CreateGroupResult(StatusResult.fail("群相关信息获取失败"));
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
                if (XmppJid.isIncognitoJid(userId)) {
                    continue;
                }
                if ("superadmin".equals(authority)) {
                    if (StringUtils.isEmpty(creator)) {
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
        return createGroupResult;
        /**
         * <iq from='8617748754950-1615536008@g.us' type='result' id='5733A87E369C42FCAB1EE9A0B613EC2E'>
         *     <group creator='8617748754950@s.whatsapp.net' s_o='8617748754950@s.whatsapp.net' id='8617748754950-1615536008' creation='1615536008' subject='哈哈哈' s_t='1615536973' p_v_id='1615542541810665' a_v_id='1615536008421535'>
         *         <participant jid='84862988210@s.whatsapp.net'/>
         *         <participant jid='8615228092350@s.whatsapp.net'/>
         *         <participant jid='8618688676082@s.whatsapp.net' type='admin'/>
         *         <participant jid='8617748754950@s.whatsapp.net' type='superadmin'/>
         *         <description/>
         *     </group>
         * </iq>
         */

        /**
         * {
         *     "creator": "8617748754950@s.whatsapp.net",
         *     "groupId": "8617748754950-1615536008@g.us",
         *     "members": [
         *         {
         *             "type": 0,
         *             "userId": "84862988210@s.whatsapp.net"
         *         },
         *         {
         *             "type": 0,
         *             "userId": "8615228092350@s.whatsapp.net"
         *         },
         *         {
         *             "type": 1,
         *             "userId": "8618688676082@s.whatsapp.net"
         *         },
         *         {
         *             "type": 2,
         *             "userId": "8617748754950@s.whatsapp.net"
         *         }
         *     ],
         *     "status": "ok",
         *     "subjectName": "哈哈哈"
         * }
         */
    }
}
