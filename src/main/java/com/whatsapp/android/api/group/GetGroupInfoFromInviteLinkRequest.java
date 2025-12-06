package com.whatsapp.android.api.group;

import ProtocolTree.ProtocolTreeNode;
import ProtocolTree.StanzaAttribute;
import cn.hutool.core.util.StrUtil;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.response.goup.CreateGroupResult;
import com.whatsapp.android.entity.response.goup.GroupMember;
import com.whatsapp.android.request.AbstractRequest;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 从邀请链接获取群信息
 *
 * @author sunnoc
 * @date 2022-02-11 09:37
 */
public class GetGroupInfoFromInviteLinkRequest extends AbstractRequest<CreateGroupResult> {
    /**
     * 邀请链接
     */
    private String inviteLink;

    public GetGroupInfoFromInviteLinkRequest(String inviteLink) {
        this.inviteLink = inviteLink;
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.GET_GROUP_INFO_FROM_INVITE_LINK;
    }

    @Override
    public boolean request() {
        if (StringUtils.isEmpty(inviteLink)) {
            return false;
        }
        String tempInviteLink = StrUtil.replace(inviteLink, "https://chat.whatsapp.com/", "");
        ProtocolTreeNode node = new ProtocolTreeNode("iq");
        node.AddAttribute(new StanzaAttribute("id", getTaskId()));
        node.AddAttribute(new StanzaAttribute("xmlns", "w:g2"));
        node.AddAttribute(new StanzaAttribute("type", "get"));
        node.AddAttribute(new StanzaAttribute("to", "g.us"));
        ProtocolTreeNode invite = new ProtocolTreeNode("invite");
        invite.AddAttribute(new StanzaAttribute("code", tempInviteLink));
        node.AddChild(invite);
        user.getGorgeousEngine().AddTask("GetInviteLink", node);
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
        CreateGroupResult createGroupResult = new CreateGroupResult();
        String groupId = groupNode.GetAttributeValue("id");
        String creator = groupNode.GetAttributeValue("s_o");
        if (!StringUtils.hasLength(creator)) {
            creator = groupNode.GetAttributeValue("creator");
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
                String userId = protocolTreeNode.GetAttributeValue("jid");
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
    }
}
