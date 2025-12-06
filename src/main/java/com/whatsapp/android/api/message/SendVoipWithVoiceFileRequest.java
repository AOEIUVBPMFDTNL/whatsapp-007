package com.whatsapp.android.api.message;

import ProtocolTree.ProtocolTreeNode;
import cn.hutool.core.util.IdUtil;
import com.whatsapp.android.GorgeousEngine;
import com.whatsapp.android.constant.CacheConstants;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.pack.message.VoipCallPack;
import com.whatsapp.android.entity.response.message.VoipCallResult;
import com.whatsapp.android.request.AbstractRequest;
import com.whatsapp.android.util.RedisService;
import com.whatsapp.android.util.WhatsAppUtils;
import org.springframework.util.StringUtils;


/**
 * @author sunnoc
 * @date 2023-02-27 23:18
 */
public class SendVoipWithVoiceFileRequest extends AbstractRequest<VoipCallResult> {
    private final VoipCallPack voipCallPack;
    private String finalMsgId;
    private String callId;
    private final boolean systemMsgId;

    public SendVoipWithVoiceFileRequest(VoipCallPack voipCallPack, boolean systemMsgId) {
        this.voipCallPack = voipCallPack;
        this.systemMsgId = systemMsgId;
    }

    @Override
    public void init() {
        this.callId = systemMsgId ? WhatsAppUtils.generateFeedMsgIdToLTS(user.getLoginPack().getUsername(), false) : IdUtil.simpleUUID().toUpperCase();
        String msgId = this.voipCallPack.getCallId();
        if (StringUtils.hasLength(msgId)) {
            this.finalMsgId = msgId;
        } else {
            if (user.getLoginPack().isIosLogin()) {
                this.finalMsgId = user.getGorgeousEngine().GenerateIqId();
            } else {
                this.finalMsgId = WhatsAppUtils.generateAndroidMsgId();
            }
        }
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.SEND_VOIP_WITH_VOICE_FILE_MESSAGE;
    }

    @Override
    public String getTaskId() {
        return finalMsgId;
    }

    @Override
    public boolean request() {
        String userId = voipCallPack.getUserId();
        if (!StringUtils.hasLength(userId)) {
            return false;
        }
        userId = WhatsAppUtils.JidNormalize(userId);
        GorgeousEngine gorgeousEngine = user.getGorgeousEngine();
        if (gorgeousEngine != null) {
            String redisKey = CacheConstants.VOIP_CALL_PACK_KEY + gorgeousEngine.getUsername() + ":" + callId;
            RedisService.getInstance().set(redisKey, voipCallPack, 60L);
            String finalUserId = userId;
            gorgeousEngine.addTaskToQueue(() -> gorgeousEngine.sendVoipVoiceMsg(finalUserId, taskId, callId));
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
