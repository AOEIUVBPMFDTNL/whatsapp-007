package com.whatsapp.android.api.group;

import ProtocolTree.ProtocolTreeNode;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.pack.group.CreateGroupPack;
import com.whatsapp.android.entity.pack.group.ModifyGroupDesc;
import com.whatsapp.android.entity.response.goup.CreateGroupResult;
import com.whatsapp.android.request.AbstractRequest;
import com.whatsapp.android.util.TaskEvent;
import org.springframework.util.StringUtils;

/**
 * 修改群描述
 *
 * @author sunnoc
 * @date 2021-06-07 13:05
 */
public class ModifyGroupDescRequest extends AbstractRequest<StatusResult> {
    private ModifyGroupDesc modifyGroupDesc;

    public ModifyGroupDescRequest(ModifyGroupDesc modifyGroupDesc) {
        this.modifyGroupDesc = modifyGroupDesc;
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.MODIFY_GROUP_DESC;
    }

    @Override
    public boolean request() {
        String groupId = modifyGroupDesc.getGroupId();
        String desc = modifyGroupDesc.getDesc();
        if (StringUtils.isEmpty(groupId) || StringUtils.isEmpty(desc)) {
            return false;
        }
        CreateGroupPack createGroupPack = new CreateGroupPack();
        createGroupPack.setGroupId(groupId);
        //先获取群描述
        CreateGroupResult createGroupResult = user.sendRequest(new GetGroupInfoRequest(createGroupPack));
        if (Constant.FAIL.equals(createGroupResult.getStatus())) {
            user.getTaskNotify().setEventContent(getTaskId(), ProtocolTreeNode.fail(Constant.FAIL, "获取群描述失败"));
            return true;
        }
        user.getGorgeousEngine().ModifyGroupDesc(getTaskId(), groupId, createGroupResult.getDescId(), desc);
        return true;
    }

    @Override
    public StatusResult parseResult(ProtocolTreeNode node) {
        return parseBaseResult(node);
    }
}
