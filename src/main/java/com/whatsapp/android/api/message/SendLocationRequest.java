package com.whatsapp.android.api.message;

import Message.WhatsMessage;
import ProtocolTree.ProtocolTreeNode;
import Util.StringUtil;
import cn.hutool.core.convert.Convert;
import com.whatsapp.android.GorgeousEngine;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.pack.message.SendLocationPack;
import com.whatsapp.android.entity.response.message.SendMessageResult;
import com.whatsapp.android.request.AbstractRequest;
import com.whatsapp.android.util.WhatsAppUtils;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 发送位置信息
 *
 * @author sunnoc
 * @date 2021-03-11 10:12
 */
public class SendLocationRequest extends AbstractRequest<SendMessageResult> {
    private SendLocationPack messagePack;
    private String finalMsgId;

    public SendLocationRequest(SendLocationPack messagePack) {
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
        return TypeConstant.TaskType.SEND_LOCATION_MESSAGE;
    }

    public String getTaskId() {
        return finalMsgId;
    }


    @Override
    public boolean request() {
        String userId = messagePack.getUserId();
        List<String> userIds = messagePack.getUserIds();
        if (StringUtils.isEmpty(userId) && (userIds == null || userIds.size() == 0)) {
            return false;
        }
        WhatsMessage.WhatsAppContextInfo.Builder contextInfoBuilder = null;
        String quotedMsgId = messagePack.getQuotedMsgId();
        String quotedMsgContent = messagePack.getQuotedMsgContent();
        String participant = messagePack.getParticipant();
        if (StringUtils.hasLength(quotedMsgId) && StringUtils.hasLength(quotedMsgContent)) {
            userId = WhatsAppUtils.JidNormalize(userId);
            if (StringUtil.IsGroupJid(userId)) {
                if (StringUtils.isEmpty(participant)) {
                    return false;
                }
                participant = WhatsAppUtils.JidNormalize(participant);
            } else {
                participant = userId;
            }
            contextInfoBuilder = WhatsAppUtils.generateQuotedMsg(participant, messagePack.getQuotedMsgId(),
                    messagePack.getQuotedMsgContent(), false);
        }
        contextInfoBuilder = user.getGorgeousEngine().applyEphemeralMessage(messagePack.getUserId(), contextInfoBuilder);
        WhatsMessage.WhatsAppMessage.Builder builder = WhatsMessage.WhatsAppMessage.newBuilder();
        WhatsMessage.WhatsAppLocationMessage.Builder contactMessage = builder.getLocationMessageBuilder();
        contactMessage.setAddress(messagePack.getAddr());
        contactMessage.setDegreesLatitude(Convert.toDouble(messagePack.getLatitude()));
        contactMessage.setDegreesLongitude(Convert.toDouble(messagePack.getLongitude()));
        contactMessage.setName(messagePack.getLocationName());
        contactMessage.setComment(messagePack.getMessage());
        if (contextInfoBuilder != null) {
            contactMessage.setContextInfo(contextInfoBuilder);
        }
        GorgeousEngine gorgeousEngine = user.getGorgeousEngine();
        if (gorgeousEngine != null) {
            if (userIds != null && userIds.size() > 0) {
                gorgeousEngine.addTaskToQueue(() -> gorgeousEngine.firstSendMsgToFans(null, userIds, builder.build().toByteArray(), "media", "location", getTaskId()));
            } else {
                gorgeousEngine.addTaskToQueue(() -> gorgeousEngine.firstSendMsgToFans(messagePack.getUserId(), builder.build().toByteArray(), "media", "location", getTaskId(), messagePack.getTcToken()));
            }

        }
        return true;
    }

    @Override
    public long timeOut() {
        return 60L;
    }

    @Override
    public SendMessageResult parseResult(ProtocolTreeNode node) {
        return parseSendMessageResult(node);
    }
}
