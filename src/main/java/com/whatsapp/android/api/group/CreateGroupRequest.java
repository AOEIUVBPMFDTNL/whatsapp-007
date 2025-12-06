package com.whatsapp.android.api.group;

import ProtocolTree.ProtocolTreeNode;
import ProtocolTree.StanzaAttribute;
import Util.StringUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.whatsapp.android.GorgeousEngine;
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
 * 创建群聊
 *
 * @author sunnoc
 * @date 2021-03-12 15:06
 */
public class CreateGroupRequest extends AbstractRequest<CreateGroupResult> {
    private CreateGroupPack createGroupPack;

    public CreateGroupRequest(CreateGroupPack createGroupPack) {
        this.createGroupPack = createGroupPack;
    }

    /**
     * 群聊主题名称
     */
    @Override
    public String funcName() {
        return TypeConstant.TaskType.CREATE_GROUP;
    }

    @Override
    public boolean request() {
        if (StringUtil.isEmpty(createGroupPack.getSubjectName())) {
            return false;
        }
        GorgeousEngine gorgeousEngine = user.getGorgeousEngine();
        ProtocolTreeNode node = new ProtocolTreeNode("iq");
        node.AddAttribute(new StanzaAttribute("id", getTaskId()));
        node.AddAttribute(new StanzaAttribute("xmlns", "w:g2"));
        node.AddAttribute(new StanzaAttribute("type", "set"));
        node.AddAttribute(new StanzaAttribute("to", "g.us"));
        ProtocolTreeNode create = new ProtocolTreeNode("create");
        create.AddAttribute(new StanzaAttribute("subject", createGroupPack.getSubjectName()));
        String key;
        if (!gorgeousEngine.isIosLogin()) {
            key = String.format("%s-%s@temp", user.getGorgeousEngine().getEnvBuilder_().getFullphone(), IdUtil.simpleUUID());
        } else {
            key = String.valueOf(System.currentTimeMillis() / 1000);
        }
        create.AddAttribute(new StanzaAttribute("key", key));
        for (String member : createGroupPack.getList()) {
            ProtocolTreeNode participant = new ProtocolTreeNode("participant");
            participant.AddAttribute(new StanzaAttribute("jid", user.getGorgeousEngine().JidNormalize(member)));
            create.AddChild(participant);
        }
        ProtocolTreeNode memberAddMode = new ProtocolTreeNode("member_add_mode");
        if (createGroupPack.isAllMemberAdd()) {
            memberAddMode.SetData("all_member_add".getBytes(StandardCharsets.UTF_8));
        } else {
            memberAddMode.SetData("admin_add".getBytes(StandardCharsets.UTF_8));
        }
        create.AddChild(memberAddMode);
        ProtocolTreeNode membershipApprovalMode = new ProtocolTreeNode("membership_approval_mode");
        ProtocolTreeNode groupJoin = new ProtocolTreeNode("group_join");
        if (createGroupPack.isMembershipApprovalModeOn()) {
            groupJoin.AddAttribute(new StanzaAttribute("state", "on"));
        } else {
            groupJoin.AddAttribute(new StanzaAttribute("state", "off"));
        }
        membershipApprovalMode.AddChild(groupJoin);
        create.AddChild(membershipApprovalMode);
        node.AddChild(create);
        gorgeousEngine.AddTask("CreateGroup", node);
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
            return new CreateGroupResult(StatusResult.fail("创建群聊失败"));
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
                if (StringUtils.hasLength(authority)) {
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
        return createGroupResult;
        /**
         * <iq from='g.us' type='result' id='3619FDD5CCE8438D831C29F8F065B053'>
         *     <group id='8617748754950-1615533672' creator='8617748754950@s.whatsapp.net' creation='1615533672' subject='testGroup' s_t='1615533672' s_o='8617748754950@s.whatsapp.net'>
         *         <participant jid='8617748754950@s.whatsapp.net' type='superadmin'/>
         *         <participant jid='8618688676082@s.whatsapp.net'/>
         *         <participant jid='8615228092350@s.whatsapp.net'/>
         *     </group>
         * </iq>
         */
        /**
         * {
         *     "creator": "8617748754950@s.whatsapp.net",
         *     "groupId": "8617748754950-1615536008@g.us",
         *     "members": [
         *         {
         *             "type": 1,
         *             "userId": "8617748754950@s.whatsapp.net"
         *         },
         *         {
         *             "type": 0,
         *             "userId": "8615228092350@s.whatsapp.net"
         *         },
         *         {
         *             "type": 0,
         *             "userId": "84862988210@s.whatsapp.net"
         *         }
         *     ],
         *     "status": "ok",
         *     "subjectName": "testGroup3"
         * }
         */
    }
}
