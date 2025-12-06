package com.whatsapp.android.api.profile;

import ProtocolTree.ProtocolTreeNode;
import ProtocolTree.StanzaAttribute;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.response.profile.GetQrCodeResult;
import com.whatsapp.android.request.AbstractRequest;
import org.springframework.util.StringUtils;

/**
 * 获取二维码
 *
 * @author sunnoc
 * @date 2021-04-07 16:46
 */
public class GetQrCodeRequest extends AbstractRequest<GetQrCodeResult> {
    @Override
    public String funcName() {
        return TypeConstant.TaskType.GET_QR_CODE;
    }

    @Override
    public boolean request() {
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("id", getTaskId()));
        iq.AddAttribute(new StanzaAttribute("xmlns", "w:qr"));
        iq.AddAttribute(new StanzaAttribute("type", "set"));
        ProtocolTreeNode qr = new ProtocolTreeNode("qr");
        qr.AddAttribute(new StanzaAttribute("type", "contact"));
        qr.AddAttribute(new StanzaAttribute("action", "get"));
        iq.AddChild(qr);
        return !StringUtils.isEmpty(user.getGorgeousEngine().AddTask("GetQrcode", iq));


    }

    @Override
    public GetQrCodeResult parseResult(ProtocolTreeNode node) {
        GetQrCodeResult checkResult = checkResult(node, GetQrCodeResult.class);
        if (checkResult != null) {
            return checkResult;
        }
        ProtocolTreeNode qrNode = node.getOneChildren("qr");
        if (qrNode == null) {
            return new GetQrCodeResult(StatusResult.fail("获取二维码失败"));
        }
        String code = qrNode.GetAttributeValue("code");
        if (StringUtils.isEmpty(code)) {
            return new GetQrCodeResult(StatusResult.fail("获取二维码失败"));
        }
        return new GetQrCodeResult(StatusResult.ok(), "https://wa.me/qr/" + code);
    }
}
