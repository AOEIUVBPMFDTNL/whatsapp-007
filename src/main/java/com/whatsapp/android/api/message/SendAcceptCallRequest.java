package com.whatsapp.android.api.message;

import ProtocolTree.ProtocolTreeNode;
import ProtocolTree.StanzaAttribute;
import Util.StringUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import com.whatsapp.android.GorgeousEngine;
import com.whatsapp.android.constant.CacheConstants;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.pack.message.VoipCallPack;
import com.whatsapp.android.entity.response.message.VoipAcceptResult;
import com.whatsapp.android.request.AbstractRequest;
import com.whatsapp.android.service.AsyncMessageService;
import com.whatsapp.android.util.RedisService;
import com.whatsapp.android.util.WhatsAppUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * 接听通话
 *
 * @author Rocky
 */
@Slf4j
public class SendAcceptCallRequest extends AbstractRequest<StatusResult> {
    private final VoipCallPack voipCallPack;
    private final String callId = IdUtil.simpleUUID().toUpperCase();

    public SendAcceptCallRequest(VoipCallPack voipCallPack) {
        this.voipCallPack = voipCallPack;
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.ACCEPT_VOIP_CALL;
    }

    @Override
    public String getTaskId() {
        return callId;
    }

    @Override
    public boolean request() {
        GorgeousEngine gorgeousEngine = user.getGorgeousEngine();
        if (gorgeousEngine != null) {
            ProtocolTreeNode acceptCallNode = new ProtocolTreeNode("call");
            acceptCallNode.AddAttribute(new StanzaAttribute("to", WhatsAppUtils.JidNormalize(this.voipCallPack.getUserId())));
            acceptCallNode.AddAttribute(new StanzaAttribute("id", callId));
            ProtocolTreeNode accept = new ProtocolTreeNode("accept");
            accept.AddAttribute(new StanzaAttribute("call-creator", StringUtil.ParseJid(this.voipCallPack.getUserId()).toString()));
            accept.AddAttribute(new StanzaAttribute("call-id", this.voipCallPack.getCallId()));
            accept.AddAttribute(new StanzaAttribute("device_class", "2015"));
            ProtocolTreeNode audioNode = new ProtocolTreeNode("audio");
            audioNode.AddAttribute(new StanzaAttribute("rate", "16000"));
            audioNode.AddAttribute(new StanzaAttribute("enc", "opus"));
            accept.AddChild(audioNode);
            if (this.voipCallPack.isVideo()) {
                ProtocolTreeNode videoNode = new ProtocolTreeNode("video");
                videoNode.AddAttribute(new StanzaAttribute("dec", "H264,AV1"));
                videoNode.AddAttribute(new StanzaAttribute("device_orientation", "1"));
                accept.AddChild(videoNode);
            }
            ProtocolTreeNode net = new ProtocolTreeNode("net");
            net.AddAttribute(new StanzaAttribute("medium", "2"));
            accept.AddChild(net);
            ProtocolTreeNode acceptEncopt = new ProtocolTreeNode("encopt");
            acceptEncopt.AddAttribute(new StanzaAttribute("keygen", "2"));
            accept.AddChild(acceptEncopt);
            acceptCallNode.AddChild(accept);
            gorgeousEngine.AddTask(acceptCallNode);
        }
        return true;
    }

    @Override
    public StatusResult parseResult(ProtocolTreeNode node) {
        String username = user.getLoginPack().getUsername();
        String redisKey = CacheConstants.VOIP_ACCEPT_RESULT_KEY + user.getLoginPack().getUsername() + ":" + voipCallPack.getCallId();
        VoipAcceptResult voipAcceptResult = (VoipAcceptResult) RedisService.getInstance().get(redisKey);
        if (ObjectUtil.isNotNull(voipAcceptResult)) {
            AsyncMessageService.asyncMessagePushService.voipCallConnected(StringUtil.ParseJid(this.voipCallPack.getUserId()).recipientId, username, this.voipCallPack.getCallId(), username,
                    DateUtil.currentSeconds(), false);
        }
        return parseBaseResult(node);
    }
}
