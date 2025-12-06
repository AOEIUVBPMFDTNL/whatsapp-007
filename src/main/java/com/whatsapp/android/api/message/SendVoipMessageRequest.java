package com.whatsapp.android.api.message;

import ProtocolTree.ProtocolTreeNode;
import cn.hutool.core.util.IdUtil;
import com.whatsapp.android.GorgeousEngine;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.pack.message.VoipMessagePack;
import com.whatsapp.android.entity.response.message.VoipCallResult;
import com.whatsapp.android.request.AbstractRequest;
import com.whatsapp.android.util.WhatsAppUtils;
import org.springframework.util.StringUtils;


/**
 * @author sunnoc
 * @date 2023-02-27 23:18
 */
public class SendVoipMessageRequest extends AbstractRequest<VoipCallResult> {
    private VoipMessagePack voipMessagePack;
    private String finalMsgId;
    private String callId;
    private final boolean systemMsgId;

    public SendVoipMessageRequest(VoipMessagePack voipMessagePack, boolean systemMsgId) {
        this.voipMessagePack = voipMessagePack;
        this.systemMsgId = systemMsgId;
    }

    @Override
    public void init() {
        this.callId = systemMsgId ? WhatsAppUtils.generateFeedMsgIdToLTS(user.getLoginPack().getUsername(), false) : IdUtil.simpleUUID().toUpperCase();
        String msgId = this.voipMessagePack.getMsgId();
        if (user.getLoginPack().isIosLogin()) {
            this.finalMsgId = user.getGorgeousEngine().GenerateIqId();
        } else {
            this.finalMsgId = WhatsAppUtils.generateAndroidMsgId();
        }
        if (StringUtils.hasLength(msgId)) {
            this.callId = msgId;
        }
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.SEND_VOIP_MESSAGE;
    }

    @Override
    public String getTaskId() {
        return finalMsgId;
    }

    @Override
    public boolean request() {
        int delay = voipMessagePack.getDelay();
        String userId = voipMessagePack.getUserId();
        if (StringUtils.isEmpty(userId)) {
            return false;
        }
        userId = WhatsAppUtils.JidNormalize(userId);

        GorgeousEngine gorgeousEngine = user.getGorgeousEngine();
        if (gorgeousEngine != null) {
            String finalUserId = userId;
            gorgeousEngine.addTaskToQueue(() -> gorgeousEngine.sendVoipMsg(finalUserId, taskId, callId, delay, voipMessagePack.getTcToken(), voipMessagePack.isFake()));
        }
        return true;
    }

    @Override
    public VoipCallResult parseResult(ProtocolTreeNode node) {
        StatusResult statusResult = parseBaseResult(node);
        VoipCallResult voipCallResult = new VoipCallResult();
        if (Constant.OK.equals(statusResult.getStatus())) {
            voipCallResult.setCallId(callId);
            return voipCallResult;
        }
        voipCallResult.setStatus(statusResult.getStatus());
        return voipCallResult;
    }
}
