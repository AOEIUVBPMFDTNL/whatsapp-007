package com.whatsapp.android.api.message;

import ProtocolTree.ProtocolTreeNode;
import ProtocolTree.StanzaAttribute;
import Util.StringUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.IdUtil;
import com.whatsapp.android.GorgeousEngine;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.pack.message.VoipTerminatePack;
import com.whatsapp.android.request.AbstractRequest;
import com.whatsapp.android.util.WhatsAppUtils;

public class SendVoipTerminateRequest extends AbstractRequest<StatusResult> {
    private final VoipTerminatePack voipTerminatePack;
    private final String id = IdUtil.simpleUUID().toUpperCase();

    public SendVoipTerminateRequest(VoipTerminatePack voipTerminatePack) {
        this.voipTerminatePack = voipTerminatePack;
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.SEND_VOIP_TERMINATE;
    }

    @Override
    public String getTaskId() {
        return id;
    }

    @Override
    public boolean request() {
        /**
         * <call to='27611399711@s.whatsapp.net' id='F71FBE5B5CAB7CB0E901C19E8D246533'>
         *     <terminate duration='19028' audio_duration='19028' call-creator='27611399711.0:0@s.whatsapp.net'
         *         call-id='3800EC0B3EBCC7A50E8CA5D1AD41FD98' />
         * </call>
         */
        GorgeousEngine gorgeousEngine = user.getGorgeousEngine();
        if (gorgeousEngine != null) {
            ProtocolTreeNode terminateCallNode = new ProtocolTreeNode("call");
            terminateCallNode.AddAttribute(new StanzaAttribute("to", WhatsAppUtils.JidNormalize(this.voipTerminatePack.getUserId())));
            terminateCallNode.AddAttribute(new StanzaAttribute("id", id));
            ProtocolTreeNode terminate = new ProtocolTreeNode("terminate");
            String duration = Convert.toStr(voipTerminatePack.getCallTime(), "20401");
            terminate.AddAttribute(new StanzaAttribute("duration", duration));
            if (voipTerminatePack.isVideo()) {
                terminate.AddAttribute(new StanzaAttribute("video_duration", duration));
            } else {
                terminate.AddAttribute(new StanzaAttribute("audio_duration", duration));
            }
            terminate.AddAttribute(new StanzaAttribute("call-creator", StringUtil.ParseJid(this.voipTerminatePack.getUserId()).toString()));
            terminate.AddAttribute(new StanzaAttribute("call-id", this.voipTerminatePack.getCallId()));
            terminateCallNode.AddChild(terminate);
            gorgeousEngine.AddTask(terminateCallNode);
        }
        return true;
    }

    @Override
    public StatusResult parseResult(ProtocolTreeNode node) {
        return parseBaseResult(node);
    }
}