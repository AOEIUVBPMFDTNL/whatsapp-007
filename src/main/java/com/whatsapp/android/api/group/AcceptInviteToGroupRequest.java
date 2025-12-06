package com.whatsapp.android.api.group;

import ProtocolTree.ProtocolTreeNode;
import ProtocolTree.StanzaAttribute;
import Util.StringUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.response.goup.CreateGroupResult;
import com.whatsapp.android.request.AbstractRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

/**
 * 通过群链接进群
 *
 * @author sunnoc
 * @date 2021-03-17 11:41
 */
@Slf4j
public class AcceptInviteToGroupRequest extends AbstractRequest<CreateGroupResult> {
    /**
     * 群链接
     */
    private final String groupUrl;

    public AcceptInviteToGroupRequest(String groupUrl) {
        this.groupUrl = groupUrl;
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.ACCEPT_INVITE_TO_GROUP;
    }

    @Override
    public boolean request() {
        if (StringUtil.isEmpty(groupUrl)) {
            return false;
        }
        String token = StrUtil.subAfter(groupUrl, "whatsapp.com/", false);
        ProtocolTreeNode node = new ProtocolTreeNode("iq");
        node.AddAttribute(new StanzaAttribute("id", getTaskId()));
        node.AddAttribute(new StanzaAttribute("xmlns", "w:g2"));
        node.AddAttribute(new StanzaAttribute("type", "set"));
        node.AddAttribute(new StanzaAttribute("to", "g.us"));

        ProtocolTreeNode invite = new ProtocolTreeNode("invite");
        invite.AddAttribute(new StanzaAttribute("code", token));
        node.AddChild(invite);
        user.getGorgeousEngine().AddTask("AcceptInviteToGroup", node);
        return true;
    }

    @Override
    public CreateGroupResult parseResult(ProtocolTreeNode node) {
        /**
         * <iq from='g.us' type='result' id='5ABBEECFF0D34C43B2BF049C0CBCF5D5'>
         *     <group jid='8615228092350-1615952250@g.us'/>
         * </iq>
         */
        CreateGroupResult checkResult = checkResult(node, CreateGroupResult.class);
        if (checkResult != null) {
            return checkResult;
        }
        CreateGroupResult createGroupResult;
        ProtocolTreeNode groupNode = node.getOneChildren("group");
        if (ObjectUtil.isNotNull(groupNode)) {
            String groupId = groupNode.GetAttributeValue("jid");
            if (StrUtil.isNotEmpty(groupId) && StringUtil.IsGroupJid(groupId)) {
                createGroupResult = new CreateGroupResult();
                createGroupResult.setGroupId(groupId);
                return createGroupResult;
            }
        }
        // 需要群管理进行审批
        ProtocolTreeNode approvalRequest = node.getOneChildren("membership_approval_request");
        if (ObjectUtil.isNotNull(approvalRequest)) {
            return new CreateGroupResult(StatusResult.fail("已提交进群申请, 请等待管理员审核进群"));
        }
        // 进群失败
        ProtocolTreeNode error = node.getOneChildren("error");
        if (ObjectUtil.isNotNull(error)) {
            String code = error.GetAttributeValue("code");
            switch (code) {
                case "410":
                    // <iq from='g.us' type='error' id='011e'><error code='410' text='gone'/></iq>
                    return new CreateGroupResult(StatusResult.fail("群链接已重置或失效"));
                case "304":
                    // <iq from='g.us' type='error' id='011e4'><error code='304' text='already-exists'/></iq>
                    return new CreateGroupResult(StatusResult.fail("该用户已进群或管理员尚未审批进群请求"));
                case "429":
                    // <iq from='g.us' type='error' id='057'><error code='429' text='rate-overlimit'/></iq>
                    return new CreateGroupResult(StatusResult.fail("进群速率过快, 请稍后重试"));
                case "409":
                    // <iq from='g.us' type='error' id='0b9'><error code='409' text='conflict'/></iq>
                    return new CreateGroupResult(StatusResult.fail("进群冲突, 请稍后重试"));
                case "403":
                case "401":
                    // <iq from='g.us' type='error' id='1718603833-5'><error code='401' text='not-authorized'/></iq>
                    // <iq from='g.us' type='error' id='0b9'><error code='409' text='conflict'/></iq>
                    return new CreateGroupResult(StatusResult.fail("进群被官方禁止, 请稍后重试"));
                case "400":
                    return new CreateGroupResult(StatusResult.fail("邀请链接错误"));
                default:
                    log.error("用户: {}, 进群失败, code: {}, reason: {}", user.getLoginPack().getUsername(), code, error.GetAttributeValue("text"));
                    return new CreateGroupResult(StatusResult.fail("进群失败"));
            }
        }
        log.error("用户: {}, 进群失败, 未知xmpp结构体: {}", user.getLoginPack().getUsername(), node);
        return new CreateGroupResult(StatusResult.fail("进群失败"));
    }
}
