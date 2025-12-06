package com.whatsapp.android.api.message;

import Message.WhatsMessage;
import ProtocolTree.ProtocolTreeNode;
import Util.StringUtil;
import com.whatsapp.android.GorgeousEngine;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.pack.message.SendVCardPack;
import com.whatsapp.android.entity.response.message.SendMessageResult;
import com.whatsapp.android.request.AbstractRequest;
import com.whatsapp.android.util.WhatsAppUtils;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 发送名片
 *
 * @author sunnoc
 * @date 2021-03-11 09:30
 */
public class SendVCardRequest extends AbstractRequest<SendMessageResult> {
    private SendVCardPack messagePack;
    private String finalMsgId;

    public SendVCardRequest(SendVCardPack messagePack) {
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
        return TypeConstant.TaskType.SEND_VCARD_MESSAGE;
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
        String vcard = "BEGIN:VCARD\n" +
                "VERSION:3.0\n" +
                "N:;;;;\n" +
                "FN:名称\n" +
                "item1.TEL;waid=带区号手机:+带区号手机\n" +
                "item1.X-ABLabel:描述\n" +
                "END:VCARD";
        vcard = vcard.replace("名称", messagePack.getName()).replace("带区号手机", messagePack.getPhone()).replace("描述", messagePack.getDescribe());
        WhatsMessage.WhatsAppMessage.Builder builder = WhatsMessage.WhatsAppMessage.newBuilder();
        WhatsMessage.WhatsAppContactMessage.Builder contactMessage = builder.getContactMessageBuilder();
        contactMessage.setDisplayName(messagePack.getShowName());
        contactMessage.setVcard(vcard);
        contextInfoBuilder = user.getGorgeousEngine().applyEphemeralMessage(messagePack.getUserId(), contextInfoBuilder);
        if (contextInfoBuilder != null) {
            contactMessage.setContextInfo(contextInfoBuilder);
        }
        GorgeousEngine gorgeousEngine = user.getGorgeousEngine();
        if (gorgeousEngine != null) {
            if (userIds != null && userIds.size() > 0) {
                gorgeousEngine.addTaskToQueue(() -> gorgeousEngine.firstSendMsgToFans(null, userIds, builder.build().toByteArray(), "media", "vcard", getTaskId()));
            } else {
                gorgeousEngine.addTaskToQueue(() -> gorgeousEngine.firstSendMsgToFans(messagePack.getUserId(), builder.build().toByteArray(), "media", "vcard", getTaskId(), messagePack.getTcToken()));
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
