package com.whatsapp.android.api.group;

import ProtocolTree.ProtocolTreeNode;
import ProtocolTree.StanzaAttribute;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.response.goup.CreateGroupResult;
import com.whatsapp.android.entity.response.goup.GetAllGroupRelationshipResult;
import com.whatsapp.android.request.AbstractRequest;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 获取社群下的子群
 *
 * @author Rocky
 */
public class GetSubGroupsRequest extends AbstractRequest<GetAllGroupRelationshipResult> {
    private String linkedParentGroupId;

    public GetSubGroupsRequest(String linkedParentGroupId) {
        this.linkedParentGroupId = linkedParentGroupId;
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.GET_SUB_GROUPS;
    }

    @Override
    public boolean request() {
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("id", getTaskId()));
        iq.AddAttribute(new StanzaAttribute("xmlns", "w:g2"));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        iq.AddAttribute(new StanzaAttribute("to", linkedParentGroupId));
        ProtocolTreeNode subGroups = new ProtocolTreeNode("sub_groups");
        iq.AddChild(subGroups);
        user.getGorgeousEngine().AddTask("GetSubGroups", iq);
        return true;
    }

    @Override
    public GetAllGroupRelationshipResult parseResult(ProtocolTreeNode node) {
        GetAllGroupRelationshipResult checkResult = checkResult(node, GetAllGroupRelationshipResult.class);
        if (ObjectUtil.isNotNull(checkResult)) {
            return checkResult;
        }
        ProtocolTreeNode subGroups = node.getOneChildren("sub_groups");
        if (ObjectUtil.isNull(subGroups)) {
            return new GetAllGroupRelationshipResult(StatusResult.fail("获取子群失败"));
        }
        LinkedList<ProtocolTreeNode> groupList = subGroups.GetChildren("group");
        if (groupList.isEmpty()) {
            return new GetAllGroupRelationshipResult(new ArrayList<>());
        }
        List<CreateGroupResult> createGroupResults = new ArrayList<>();
        for (ProtocolTreeNode groupNode : groupList) {
            CreateGroupResult createGroupResult = new CreateGroupResult();
            String subGroupId = groupNode.GetAttributeValue("id");
            String subject = groupNode.GetAttributeValue("subject");
            if (!StrUtil.isNotEmpty(subGroupId)) {
                continue;
            }
            createGroupResult.setGroupId(subGroupId + "@g.us");
            createGroupResult.setSubjectName(subject);
            createGroupResult.setSubGroup(true);
            if (ObjectUtil.isNotNull(groupNode.getOneChildren("default_sub_group"))) {
                createGroupResult.setDefaultSubGroup(true);
            }
            createGroupResults.add(createGroupResult);
        }
        return new GetAllGroupRelationshipResult(createGroupResults);
    }
}
