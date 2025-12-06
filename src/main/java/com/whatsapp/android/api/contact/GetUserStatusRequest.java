package com.whatsapp.android.api.contact;

import ProtocolTree.ProtocolTreeNode;
import ProtocolTree.StanzaAttribute;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.response.contact.GetUserStatusResult;
import com.whatsapp.android.request.AbstractRequest;


/**
 * 获取用户状态
 *
 * @author sunnoc
 * @date 2022-02-14 15:17
 */
public class GetUserStatusRequest extends AbstractRequest<GetUserStatusResult> {
    private String userId;

    public GetUserStatusRequest(String userId) {
        this.userId = userId;
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.GET_USER_STATUS;
    }

    @Override
    public boolean request() {
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("id", getTaskId()));
        iq.AddAttribute(new StanzaAttribute("xmlns", "status"));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        ProtocolTreeNode statusNode = new ProtocolTreeNode("status");
        ProtocolTreeNode userNode = new ProtocolTreeNode("user");
        userNode.AddAttribute(new StanzaAttribute("jid", user.getGorgeousEngine().JidNormalize(userId)));
        statusNode.AddChild(userNode);
        iq.AddChild(statusNode);
        user.getGorgeousEngine().AddTask("SetStatue", iq);
        return true;
    }

    @Override
    public GetUserStatusResult parseResult(ProtocolTreeNode node) {
        GetUserStatusResult checkResult = checkResult(node, GetUserStatusResult.class);
        if (checkResult != null) {
            return checkResult;
        }
        ProtocolTreeNode statusNode = node.getOneChildren("status");
        if (statusNode == null) {
            return new GetUserStatusResult(StatusResult.fail("获取签名描述失败"));
        }
        ProtocolTreeNode userNode = statusNode.getOneChildren("user");
        if (userNode == null) {
            return new GetUserStatusResult(StatusResult.fail("获取签名描述失败"));
        }
        String jid = userNode.GetAttributeValue("jid");
        String t = userNode.GetAttributeValue("t");
        long modifyTime = Convert.toLong(t, 0L);
        byte[] bytes = userNode.GetData();
        GetUserStatusResult userStatusResult = new GetUserStatusResult();
        if (modifyTime != 0 && bytes != null) {
            String describe = new String(bytes);
            userStatusResult.setExistDescribe(true);
            userStatusResult.setDescribe(describe);
            userStatusResult.setModifyDescribeTime(DateUtil.date(modifyTime * 1000).toString());
        } else {
            userStatusResult.setExistDescribe(false);
        }
        return userStatusResult;
    }
}
