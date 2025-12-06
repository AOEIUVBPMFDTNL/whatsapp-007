package com.whatsapp.android.api.group;

import ProtocolTree.ProtocolTreeNode;
import ProtocolTree.StanzaAttribute;
import Util.StringUtil;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.pack.group.CreateGroupPack;
import com.whatsapp.android.entity.response.goup.CreateGroupResult;
import com.whatsapp.android.request.AbstractRequest;
import org.springframework.util.StringUtils;


/**
 * 获取邀请群链接
 *
 * @author sunnoc
 * @date 2021-03-29 16:15
 */
public class GetGroupInviteLinkRequest extends AbstractRequest<CreateGroupResult> {
    private CreateGroupPack createGroupPack;
    private boolean reset;

    public GetGroupInviteLinkRequest(CreateGroupPack createGroupPack,boolean reset) {
        this.createGroupPack = createGroupPack;
        this.reset = reset;
    }

    @Override
    public String funcName() {
        if (reset) {
            return TypeConstant.TaskType.RESET_GROUP_INVITE_LINK;
        } else {
            return TypeConstant.TaskType.GET_GROUP_INVITE_LINK;
        }
    }

    @Override
    public boolean request() {
        if (StringUtil.isEmpty(createGroupPack.getGroupId())) {
            return false;
        }
        ProtocolTreeNode node = new ProtocolTreeNode("iq");
        node.AddAttribute(new StanzaAttribute("id", getTaskId()));
        node.AddAttribute(new StanzaAttribute("xmlns", "w:g2"));
        if (reset) {
            node.AddAttribute(new StanzaAttribute("type", "set"));
        } else {
            node.AddAttribute(new StanzaAttribute("type", "get"));
        }
        node.AddAttribute(new StanzaAttribute("to", user.getGorgeousEngine().JidNormalize(createGroupPack.getGroupId())));
        ProtocolTreeNode invite = new ProtocolTreeNode("invite");
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
        String groupId = node.GetAttributeValue("from");
        if (StringUtils.isEmpty(groupId)) {
            return new CreateGroupResult(StatusResult.fail("群id获取失败"));
        }
        ProtocolTreeNode inviteNode = node.getOneChildren("invite");
        if (inviteNode == null) {
            return new CreateGroupResult(StatusResult.fail("获取群邀请链接失败"));
        }
        CreateGroupResult createGroupResult = new CreateGroupResult();
        String inviteCode = inviteNode.GetAttributeValue("code");
        if (StringUtils.isEmpty(inviteCode)) {
            return new CreateGroupResult(StatusResult.fail("获取群邀请链接失败"));
        }
        createGroupResult.setGroupId(groupId);
        createGroupResult.setInviteLink("https://chat.whatsapp.com/" + inviteCode);
        return createGroupResult;
    }
}
