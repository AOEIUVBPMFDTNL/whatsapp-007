package com.whatsapp.android.api.group;

import ProtocolTree.ProtocolTreeNode;
import ProtocolTree.StanzaAttribute;
import cn.hutool.core.util.ObjectUtil;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.pack.group.CreateGroupPack;
import com.whatsapp.android.entity.pack.group.ModifyGroupMembershipApprovalModePack;
import com.whatsapp.android.entity.response.goup.CreateGroupResult;
import com.whatsapp.android.request.AbstractRequest;

/**
 * 修改是否开启管理员审批群成员权限
 *
 * @author sunnoc
 * @date 2021-10-17 12:38
 */
public class ModifyGroupMembershipApprovalModeRequest extends AbstractRequest<StatusResult> {
    private final ModifyGroupMembershipApprovalModePack modifyGroupMembershipApprovalModePack;

    public ModifyGroupMembershipApprovalModeRequest(ModifyGroupMembershipApprovalModePack modifyGroupMembershipApprovalModePack) {
        this.modifyGroupMembershipApprovalModePack = modifyGroupMembershipApprovalModePack;
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.MODIFY_GROUP_MEMBERSHIP_APPROVAL_MODE;
    }

    @Override
    public boolean request() {
        String groupId = modifyGroupMembershipApprovalModePack.getGroupId();
        CreateGroupPack createGroupPack = new CreateGroupPack();
        createGroupPack.setGroupId(groupId);
        CreateGroupResult createGroupResult = user.sendRequest(new GetGroupInfoRequest(createGroupPack));
        if (Constant.FAIL.equals(createGroupResult.getStatus())) {
            user.getTaskNotify().setEventContent(getTaskId(), ProtocolTreeNode.fail(Constant.FAIL, "获取群信息失败"));
            return true;
        }
        Boolean memberAddApprovalOn = createGroupResult.getMemberAddApprovalOn();
        if (ObjectUtil.isNull(memberAddApprovalOn) && !modifyGroupMembershipApprovalModePack.isMembershipApprovalModeOn()){
            // 若为空则默认不开启
            user.getTaskNotify().setEventContent(getTaskId(), ProtocolTreeNode.success(Constant.OK));
            return true;
        }
        if (ObjectUtil.isNotNull(memberAddApprovalOn) && memberAddApprovalOn == modifyGroupMembershipApprovalModePack.isMembershipApprovalModeOn()) {
            user.getTaskNotify().setEventContent(getTaskId(), ProtocolTreeNode.success(Constant.OK));
            return true;
        }
        ProtocolTreeNode node = new ProtocolTreeNode("iq");
        node.AddAttribute(new StanzaAttribute("id", getTaskId()));
        node.AddAttribute(new StanzaAttribute("xmlns", "w:g2"));
        node.AddAttribute(new StanzaAttribute("type", "set"));
        node.AddAttribute(new StanzaAttribute("to", user.getGorgeousEngine().JidNormalize(groupId)));
        ProtocolTreeNode membershipApprovalMode = new ProtocolTreeNode("membership_approval_mode");
        ProtocolTreeNode groupJoin = new ProtocolTreeNode("group_join");
        if (modifyGroupMembershipApprovalModePack.isMembershipApprovalModeOn()) {
            groupJoin.AddAttribute(new StanzaAttribute("state", "on"));
        } else {
            groupJoin.AddAttribute(new StanzaAttribute("state", "off"));
        }
        membershipApprovalMode.AddChild(groupJoin);
        node.AddChild(membershipApprovalMode);
        user.getGorgeousEngine().AddTask(node);
        return true;
    }

    @Override
    public StatusResult parseResult(ProtocolTreeNode node) {
        return parseBaseResult(node);
    }
}
