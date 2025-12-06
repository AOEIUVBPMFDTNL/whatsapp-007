package com.whatsapp.android.api.contact;

import Message.WhatsMessage;
import ProtocolTree.ProtocolTreeNode;
import com.whatsapp.android.GorgeousEngine;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.pack.contact.TimeLimitedMessagePack;
import com.whatsapp.android.entity.response.message.SendMessageResult;
import com.whatsapp.android.request.AbstractRequest;
import com.whatsapp.android.util.WhatsAppUtils;
import org.springframework.util.StringUtils;

/**
 * 设置限时消息
 *
 * @author sunnoc
 * @date 2022-01-24 09:46
 */
public class SetTimeLimitedMessageRequest extends AbstractRequest<SendMessageResult> {
    private TimeLimitedMessagePack timeLimitedMessagePack;

    public SetTimeLimitedMessageRequest(TimeLimitedMessagePack timeLimitedMessagePack) {
        this.timeLimitedMessagePack = timeLimitedMessagePack;
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.SET_TIME_LIMITED_MESSAGE;
    }

    @Override
    public boolean request() {
        String userId = timeLimitedMessagePack.getUserId();
        if (StringUtils.isEmpty(userId)) {
            return false;
        }
        WhatsMessage.WhatsAppMessage.Builder protocolMessageBuild = WhatsMessage.WhatsAppMessage.newBuilder();
        WhatsMessage.WhatsAppProtocolMessage.Builder builder = WhatsMessage.WhatsAppProtocolMessage.newBuilder();
        builder.setType(WhatsMessage.WhatsAppProtocolMessageType.EPHEMERAL_SETTING);
        builder.setEphemeralExpiration(timeLimitedMessagePack.getExpireTime());
        WhatsMessage.WhatsAppProtocolMessage.MessageKey.Builder messageKeyBuilder = WhatsMessage.WhatsAppProtocolMessage.MessageKey.newBuilder();
        userId = WhatsAppUtils.JidNormalize(userId);
        messageKeyBuilder.setRemoteJid(userId);
        messageKeyBuilder.setFromMe(1);
        builder.setKey(messageKeyBuilder);
        protocolMessageBuild.setProtocolMessage(builder);
        GorgeousEngine gorgeousEngine = user.getGorgeousEngine();
        if (gorgeousEngine != null) {
            gorgeousEngine.executeUserIdSubscribeInternal(userId);
            gorgeousEngine.addTaskToQueue(() -> gorgeousEngine.SendSerialData(timeLimitedMessagePack.getUserId(), protocolMessageBuild.build().toByteArray(), "text", "", getTaskId()));
        }
        return true;
    }

    @Override
    public SendMessageResult parseResult(ProtocolTreeNode node) {
        return parseSendMessageResult(node);
    }
}
