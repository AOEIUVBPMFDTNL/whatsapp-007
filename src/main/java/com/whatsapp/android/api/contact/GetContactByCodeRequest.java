package com.whatsapp.android.api.contact;

import ProtocolTree.ProtocolTreeNode;
import ProtocolTree.StanzaAttribute;
import Util.StringUtil;
import cn.hutool.core.util.StrUtil;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.response.contact.GetContactByCodeResult;
import com.whatsapp.android.request.AbstractRequest;
import org.springframework.util.StringUtils;

/**
 * 通过二维码获取信息
 *
 * @author sunnoc
 * @date 2021-04-07 17:15
 */
public class GetContactByCodeRequest extends AbstractRequest<GetContactByCodeResult> {
    private String qrUrl;

    public GetContactByCodeRequest(String qrUrl) {
        this.qrUrl = qrUrl;
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.GET_CONTACT_BY_CODE;
    }

    @Override
    public boolean request() {
        if (StringUtil.isEmpty(qrUrl)) {
            return false;
        }
        String code = StrUtil.subAfter(qrUrl, "wa.me/qr/", false);
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("id", getTaskId()));
        iq.AddAttribute(new StanzaAttribute("xmlns", "w:qr"));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        ProtocolTreeNode qr = new ProtocolTreeNode("qr");
        qr.AddAttribute(new StanzaAttribute("code", code));
        iq.AddChild(qr);
        return !StringUtils.isEmpty(user.getGorgeousEngine().AddTask("GetContactByCode", iq));
    }

    @Override
    public GetContactByCodeResult parseResult(ProtocolTreeNode node) {
        GetContactByCodeResult checkResult = checkResult(node, GetContactByCodeResult.class);
        if (checkResult != null) {
            return checkResult;
        }
        ProtocolTreeNode qrNode = node.getOneChildren("qr");
        if (qrNode == null) {
            return new GetContactByCodeResult(StatusResult.fail("获取信息失败"));
        }
        String userId = qrNode.GetAttributeValue("jid");
        String nickname = qrNode.GetAttributeValue("notify");
        return new GetContactByCodeResult(StatusResult.ok(), userId, nickname);

    }
}
