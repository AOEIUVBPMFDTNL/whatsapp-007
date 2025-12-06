package com.whatsapp.android.api.profile;

import ProtocolTree.ProtocolTreeNode;
import cn.hutool.core.util.ObjectUtil;
import com.whatsapp.android.GorgeousEngine;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.response.profile.ScanWebWhatsappResult;
import com.whatsapp.android.request.AbstractRequest;
import org.springframework.util.StringUtils;

/**
 * 扫码web whatsapp
 *
 * @author sunnoc
 * @date 2022-06-17 14:04
 */
public class ScanWebWhatsappRequest extends AbstractRequest<ScanWebWhatsappResult> {
    private final String qrCode;

    public ScanWebWhatsappRequest(String qrCode) {
        this.qrCode = qrCode;
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.SCAN_WEB_WHATSAPP;
    }

    @Override
    public boolean request() {
        GorgeousEngine gorgeousEngine = user.getGorgeousEngine();
        if (gorgeousEngine != null) {
            gorgeousEngine.ScanWebWhatsapp(qrCode, taskId);
        }
        return true;
    }

    @Override
    public ScanWebWhatsappResult parseResult(ProtocolTreeNode node) {
        ScanWebWhatsappResult checkResult = checkResult(node, ScanWebWhatsappResult.class);
        if (checkResult != null) {
            return checkResult;
        }
        ProtocolTreeNode deviceNode = node.getOneChildren("device");
        if (deviceNode == null) {
            deviceNode = node.getOneChildren("media.fagr1-1.fna.whatsapp.net");
        }
        if (ObjectUtil.isNotNull(deviceNode)) {
            String jid = deviceNode.GetAttributeValue("jid");
            if (StringUtils.hasLength(jid)) {
                return new ScanWebWhatsappResult(jid);
            }
            return new ScanWebWhatsappResult(StatusResult.fail("多设备id获取失败"));
        } else {
            // 返回失败原因
            if (ObjectUtil.isNotNull(node.getOneChildren("retry-ts"))) {
                // <iq from='s.whatsapp.net' type='result' id='058'><retry-ts ts='1718158750'/></iq>
                return new ScanWebWhatsappResult(StatusResult.fail("网页未完全加载, 请重试"));
            }
            ProtocolTreeNode error = node.getOneChildren("error");
            if (ObjectUtil.isNotNull(error)) {
                String code = error.GetAttributeValue("code");
                if ("419".equals(code)) {
                    // 超过多设备数
                    // <iq from='s.whatsapp.net' type='error' id='063'><error code='419' text='resource-limit'/></iq>
                    return new ScanWebWhatsappResult(StatusResult.fail("多设备数已满, 请退出多余的设备"));
                }
                if ("400".equals(code)) {
                    return new ScanWebWhatsappResult(StatusResult.fail("二维码已过期"));
                }
                if ("500".equals(code)) {
                    return new ScanWebWhatsappResult(StatusResult.fail("ws扫码服务器错误"));
                }
                if ("454".equals(code)) {
                    return new ScanWebWhatsappResult(StatusResult.fail("网页/平板设备未成功连接"));
                }
                return new ScanWebWhatsappResult(StatusResult.fail("错误状态码: " + code + " 失败原因: " + error.GetAttributeValue("text")));
            }
            return new ScanWebWhatsappResult(StatusResult.fail());
        }
    }

    @Override
    public long timeOut() {
        return 60L;
    }
}