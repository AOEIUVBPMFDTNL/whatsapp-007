package com.whatsapp.android.api.group;

import ProtocolTree.ProtocolTreeNode;
import ProtocolTree.StanzaAttribute;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.pack.group.CreateGroupPack;
import com.whatsapp.android.entity.pack.group.ModifyGroupAddPermissionPack;
import com.whatsapp.android.entity.response.goup.CreateGroupResult;
import com.whatsapp.android.request.AbstractRequest;

import java.nio.charset.StandardCharsets;

/**
 * 修改群成员邀请权限
 *
 * @author sunnoc
 * @date 2021-10-17 12:38
 */
public class ModifyGroupAddPermissionRequest extends AbstractRequest<StatusResult> {
    private final ModifyGroupAddPermissionPack modifyGroupAddPermissionPack;

    public ModifyGroupAddPermissionRequest(ModifyGroupAddPermissionPack modifyGroupAddPermissionPack) {
        this.modifyGroupAddPermissionPack = modifyGroupAddPermissionPack;
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.MODIFY_GROUP_ADD_PERMISSION;
    }

    @Override
    public boolean request() {
        String groupId = modifyGroupAddPermissionPack.getGroupId();
        CreateGroupPack createGroupPack = new CreateGroupPack();
        createGroupPack.setGroupId(groupId);
        CreateGroupResult createGroupResult = user.sendRequest(new GetGroupInfoRequest(createGroupPack));
        if (Constant.FAIL.equals(createGroupResult.getStatus())) {
            user.getTaskNotify().setEventContent(getTaskId(), ProtocolTreeNode.fail(Constant.FAIL, "获取群信息失败"));
            return true;
        }
        Boolean allMemberAdd = createGroupResult.getAllMemberAdd();
        if (allMemberAdd == modifyGroupAddPermissionPack.isAllMemberAdd()) {
            user.getTaskNotify().setEventContent(getTaskId(), ProtocolTreeNode.success(Constant.OK));
            return true;
        }
        ProtocolTreeNode node = new ProtocolTreeNode("iq");
        node.AddAttribute(new StanzaAttribute("id", getTaskId()));
        node.AddAttribute(new StanzaAttribute("xmlns", "w:g2"));
        node.AddAttribute(new StanzaAttribute("type", "set"));
        node.AddAttribute(new StanzaAttribute("to", user.getGorgeousEngine().JidNormalize(groupId)));
        ProtocolTreeNode memberAddMode = new ProtocolTreeNode("member_add_mode");
        if (modifyGroupAddPermissionPack.isAllMemberAdd()) {
            memberAddMode.SetData("all_member_add".getBytes(StandardCharsets.UTF_8));
        } else {
            memberAddMode.SetData("admin_add".getBytes(StandardCharsets.UTF_8));
        }
        node.AddChild(memberAddMode);
        user.getGorgeousEngine().AddTask(node);
        return true;
    }

    @Override
    public StatusResult parseResult(ProtocolTreeNode node) {
        return parseBaseResult(node);
    }
}
