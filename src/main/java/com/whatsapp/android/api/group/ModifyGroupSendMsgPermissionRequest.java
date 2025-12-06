package com.whatsapp.android.api.group;

import ProtocolTree.ProtocolTreeNode;
import ProtocolTree.StanzaAttribute;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.pack.group.CreateGroupPack;
import com.whatsapp.android.entity.pack.group.ModifyGroupSendMsgPermissionPack;
import com.whatsapp.android.entity.response.goup.CreateGroupResult;
import com.whatsapp.android.request.AbstractRequest;
import com.whatsapp.android.util.TaskEvent;


/**
 * 修改群发言权限
 *
 * @author sunnoc
 * @date 2021-10-16 16:02
 */
public class ModifyGroupSendMsgPermissionRequest extends AbstractRequest<StatusResult> {
    private ModifyGroupSendMsgPermissionPack modifyGroupSendMsgPermissionPack;

    public ModifyGroupSendMsgPermissionRequest(ModifyGroupSendMsgPermissionPack modifyGroupSendMsgPermissionPack) {
        this.modifyGroupSendMsgPermissionPack = modifyGroupSendMsgPermissionPack;
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.MODIFY_GROUP_SEND_MSG_PERMISSION;
    }

    @Override
    public boolean request() {
        String groupId = modifyGroupSendMsgPermissionPack.getGroupId();
        CreateGroupPack createGroupPack = new CreateGroupPack();
        createGroupPack.setGroupId(groupId);
        //先获取群描述
        CreateGroupResult createGroupResult = user.sendRequest(new GetGroupInfoRequest(createGroupPack));
        if (Constant.FAIL.equals(createGroupResult.getStatus())) {
            user.getTaskNotify().setEventContent(getTaskId(), ProtocolTreeNode.fail(Constant.FAIL, "获取群信息失败"));
            return true;
        }
        boolean forbid = createGroupResult.getForbid();
        if (forbid == modifyGroupSendMsgPermissionPack.isForbid()) {
            user.getTaskNotify().setEventContent(getTaskId(), ProtocolTreeNode.success(Constant.OK));
            return true;
        }
        //<iq id='6' xmlns='w:g2' type='set' to='8615228092350-1616936849@g.us'><announcement/></iq>
        ProtocolTreeNode node = new ProtocolTreeNode("iq");
        node.AddAttribute(new StanzaAttribute("id", getTaskId()));
        node.AddAttribute(new StanzaAttribute("xmlns", "w:g2"));
        node.AddAttribute(new StanzaAttribute("type", "set"));
        node.AddAttribute(new StanzaAttribute("to", user.getGorgeousEngine().JidNormalize(groupId)));
        if (modifyGroupSendMsgPermissionPack.isForbid()) {
            node.AddChild(new ProtocolTreeNode("announcement"));
        } else {
            node.AddChild(new ProtocolTreeNode("not_announcement"));
        }
        user.getGorgeousEngine().AddTask("modifyGroupSendMsgPermission", node);
        return true;
    }

    @Override
    public StatusResult parseResult(ProtocolTreeNode node) {
        return parseBaseResult(node);
    }
}
