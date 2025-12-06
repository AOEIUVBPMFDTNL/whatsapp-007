package com.whatsapp.android.api.group;

import ProtocolTree.ProtocolTreeNode;
import ProtocolTree.StanzaAttribute;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.pack.group.CreateGroupPack;
import com.whatsapp.android.entity.pack.group.ModifyGroupEditPermissionPack;
import com.whatsapp.android.entity.response.goup.CreateGroupResult;
import com.whatsapp.android.request.AbstractRequest;
import com.whatsapp.android.util.TaskEvent;

/**
 * 修改编辑群组信息权限
 *
 * @author sunnoc
 * @date 2021-10-17 12:38
 */
public class ModifyGroupEditPermissionRequest extends AbstractRequest<StatusResult> {
    private final ModifyGroupEditPermissionPack modifyGroupEditPermissionPack;

    public ModifyGroupEditPermissionRequest(ModifyGroupEditPermissionPack modifyGroupEditPermissionPack) {
        this.modifyGroupEditPermissionPack = modifyGroupEditPermissionPack;
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.MODIFY_GROUP_EDIT_PERMISSION;
    }

    @Override
    public boolean request() {
        String groupId = modifyGroupEditPermissionPack.getGroupId();
        CreateGroupPack createGroupPack = new CreateGroupPack();
        createGroupPack.setGroupId(groupId);
        CreateGroupResult createGroupResult = user.sendRequest(new GetGroupInfoRequest(createGroupPack));
        if (Constant.FAIL.equals(createGroupResult.getStatus())) {
            user.getTaskNotify().setEventContent(getTaskId(), ProtocolTreeNode.fail(Constant.FAIL, "获取群信息失败"));
            return true;
        }
        boolean editGroupLocked = createGroupResult.getEditGroupLocked();
        if (editGroupLocked == modifyGroupEditPermissionPack.isEditGroupLocked()) {
            user.getTaskNotify().setEventContent(getTaskId(), ProtocolTreeNode.success(Constant.OK));
            return true;
        }
        ProtocolTreeNode node = new ProtocolTreeNode("iq");
        node.AddAttribute(new StanzaAttribute("id", getTaskId()));
        node.AddAttribute(new StanzaAttribute("xmlns", "w:g2"));
        node.AddAttribute(new StanzaAttribute("type", "set"));
        node.AddAttribute(new StanzaAttribute("to", user.getGorgeousEngine().JidNormalize(groupId)));
        if (modifyGroupEditPermissionPack.isEditGroupLocked()) {
            node.AddChild(new ProtocolTreeNode("locked"));
        } else {
            node.AddChild(new ProtocolTreeNode("unlocked"));
        }
        user.getGorgeousEngine().AddTask("modifyGroupEditPermission", node);
        return true;
    }

    @Override
    public StatusResult parseResult(ProtocolTreeNode node) {
        return parseBaseResult(node);
    }
}
