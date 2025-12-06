package com.whatsapp.android.api.message;

import Message.WhatsMessage;
import ProtocolTree.ProtocolTreeNode;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.whatsapp.android.GorgeousEngine;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.pack.message.MessagePack;
import com.whatsapp.android.entity.response.message.SendMessageResult;
import com.whatsapp.android.request.AbstractRequest;
import com.whatsapp.android.util.WhatsAppUtils;
import org.springframework.util.StringUtils;

import static com.whatsapp.android.util.WhatsAppUtils.JidNormalize;

/**
 * 消息回复表情
 *
 * @author sunnoc
 * @date 2021-03-10 18:27
 */
public class SendReactionMessageRequest extends AbstractRequest<SendMessageResult> {
    private MessagePack messagePack;
    private String finalMsgId;

    public SendReactionMessageRequest(MessagePack messagePack) {
        this.messagePack = messagePack;
    }

    @Override
    public void init() {
        String msgId = this.messagePack.getMsgId();
        if (StringUtils.hasLength(msgId)) {
            this.finalMsgId = msgId;
        } else {
            String userId = this.messagePack.getUserId();
            this.finalMsgId = WhatsAppUtils.generateMsgId(userId, user.getLoginPack().isIosLogin());
        }
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.SEND_REACTION_MESSAGE;
    }

    @Override
    public String getTaskId() {
        return finalMsgId;
    }

    @Override
    public long timeOut() {
        return 60L;
    }

    @Override
    public boolean request() {
        String userId = messagePack.getUserId();
        String participant = messagePack.getParticipant();
        String quotedMsgId = messagePack.getQuotedMsgId();
        boolean fromMe = messagePack.isFromMe();
        String content = messagePack.getContent();
        if (!(StrUtil.isNotEmpty(userId) && StrUtil.isNotEmpty(quotedMsgId) && ObjectUtil.isNotNull(fromMe))) {
            return false;
        }
        WhatsMessage.WhatsAppMessage.Builder protocolMessageBuild = WhatsMessage.WhatsAppMessage.newBuilder();
        // 增加时间戳
        WhatsMessage.MessageContextInfo.Builder messageContextInfo = WhatsMessage.MessageContextInfo.newBuilder();
        WhatsMessage.DeviceListMetadata.Builder deviceListMetadata = WhatsMessage.DeviceListMetadata.newBuilder();
        deviceListMetadata.setSenderTimestamp(System.currentTimeMillis() / 1000);
        messageContextInfo.setDeviceListMetadataVersion(2);
        messageContextInfo.setDeviceListMetadata(deviceListMetadata);
        protocolMessageBuild.setMessageContextInfo(messageContextInfo);
        String jid = JidNormalize(userId);
        // 增加表情回复
        WhatsMessage.ReactionMessage.Builder reactionMessage = WhatsMessage.ReactionMessage.newBuilder();
        WhatsMessage.MessageKey.Builder messageKey = WhatsMessage.MessageKey.newBuilder();
        messageKey.setFromMe(fromMe);
        messageKey.setId(quotedMsgId);
        messageKey.setRemoteJid(jid);
        if (StrUtil.isNotEmpty(participant)) {
            String participant_ = JidNormalize(participant);
            messageKey.setParticipant(participant_);
        }
        reactionMessage.setKey(messageKey);
        reactionMessage.setText(content);
        reactionMessage.setSenderTimestampMs(System.currentTimeMillis());
        protocolMessageBuild.setReactionMessage(reactionMessage);
        GorgeousEngine gorgeousEngine = user.getGorgeousEngine();
        if (gorgeousEngine != null) {
            gorgeousEngine.addTaskToQueue(() -> gorgeousEngine.firstSendMsgToFans(messagePack.getUserId(), protocolMessageBuild.build().toByteArray(), "reaction", "", getTaskId(), messagePack.getTcToken()));
        }
        return true;


    }

    @Override
    public SendMessageResult parseResult(ProtocolTreeNode node) {
        return parseSendMessageResult(node);
    }
}
