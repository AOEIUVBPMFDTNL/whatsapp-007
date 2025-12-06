package com.whatsapp.android.api.message;

import Message.WhatsMessage;
import ProtocolTree.ProtocolTreeNode;
import Util.StringUtil;
import cn.hutool.core.text.UnicodeUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.whatsapp.android.GorgeousEngine;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.pack.message.MessagePack;
import com.whatsapp.android.entity.response.message.SendMessageResult;
import com.whatsapp.android.request.AbstractRequest;
import com.whatsapp.android.util.WhatsAppUtils;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 发送文字消息
 *
 * @author sunnoc
 * @date 2021-03-10 18:27
 */
public class SendTextMessageRequest extends AbstractRequest<SendMessageResult> {
    private MessagePack messagePack;
    private String finalMsgId;

    public SendTextMessageRequest(MessagePack messagePack) {
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
        return TypeConstant.TaskType.SEND_TEXT_MESSAGE;
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
        List<String> userIds = messagePack.getUserIds();
        if (StringUtils.isEmpty(userId) && (userIds == null || userIds.size() == 0)) {
            return false;
        }
        WhatsMessage.WhatsAppMessage.Builder protocolMessageBuild = WhatsMessage.WhatsAppMessage.newBuilder();
        String quotedMsgId = messagePack.getQuotedMsgId();
        String quotedMsgContent = messagePack.getQuotedMsgContent();
        String participant = messagePack.getParticipant();
        List<String> mentionedUserIds = messagePack.getMentionedUserIds();
        if (messagePack.isLinkMode()) {
            WhatsMessage.WhatsAppExtendedTextMessage.Builder builder = WhatsMessage.WhatsAppExtendedTextMessage.newBuilder();
            if (messagePack.isUnicode()) {
                builder.setText(UnicodeUtil.toString(StrUtil.replace(messagePack.getContent(), "%u", "\\u")));
            } else {
                builder.setText(messagePack.getContent());
            }
            WhatsMessage.WhatsAppContextInfo.Builder contextBuilder = WhatsMessage.WhatsAppContextInfo.newBuilder();
            contextBuilder.setForwardingScore(3);
            contextBuilder.setIsForwarded(true);
            WhatsMessage.BusinessMessageForwardInfo.Builder businessMessageBuilder = WhatsMessage.BusinessMessageForwardInfo.newBuilder();
            String jid = WhatsAppUtils.JidNormalize(messagePack.getLinkUserId());
            businessMessageBuilder.setBusinessOwnerJid(jid);
            contextBuilder.setBusinessMessageForwardInfo(businessMessageBuilder);
            contextBuilder = WhatsAppUtils.generateMentionedMsg(mentionedUserIds, contextBuilder);
            builder.setContextInfo(contextBuilder);
            protocolMessageBuild.setExtendedTextMessage(builder);
            /*
            WhatsMessage.WhatsAppUnknow25.Builder builder = WhatsMessage.WhatsAppUnknow25.newBuilder();
            WhatsMessage.FacebookInvite.Builder facebookBuilder = WhatsMessage.FacebookInvite.newBuilder();
            if (messagePack.isUnicode()) {
                facebookBuilder.setContent(UnicodeUtil.toString(StrUtil.replace(messagePack.getContent(), "%u", "\\u")));
            } else {
                facebookBuilder.setContent(messagePack.getContent());
            }
            WhatsMessage.FacebookInvite.unknow8.Builder unKnow8Builder = WhatsMessage.FacebookInvite.unknow8.newBuilder();
            WhatsMessage.FacebookInvite.unknow8.unknow2.Builder unKnow2Builder = WhatsMessage.FacebookInvite.unknow8.unknow2.newBuilder();
            unKnow2Builder.setTitle(messagePack.getLinkTitle());
            unKnow2Builder.setUrl(messagePack.getLinkUrl());
            unKnow8Builder.setUnknow2(unKnow2Builder);
            unKnow8Builder.setU4(0);
            facebookBuilder.setUnknow8(unKnow8Builder);
            builder.setFacebook(facebookBuilder);
            protocolMessageBuild.setUnknow25(builder);
            */

        } else {
            int expirationTime = user.getGorgeousEngine().getEphemeralMessageTime(messagePack.getUserId());
            if (StringUtils.isEmpty(quotedMsgId) || StringUtils.isEmpty(quotedMsgContent)) {
                if (expirationTime > 0 || (ObjectUtil.isNotNull(mentionedUserIds) && !mentionedUserIds.isEmpty())) {
                    //新方式
                    WhatsMessage.WhatsAppExtendedTextMessage.Builder builder = WhatsMessage.WhatsAppExtendedTextMessage.newBuilder();
                    if (messagePack.isUnicode()) {
                        builder.setText(UnicodeUtil.toString(StrUtil.replace(messagePack.getContent(), "%u", "\\u")));
                    } else {
                        builder.setText(messagePack.getContent());
                    }
                    builder.setPreviewType(WhatsMessage.WhatsAppPreviewType.PreviewType_NONE);
                    WhatsMessage.WhatsAppContextInfo.Builder contextInfoBuilder = WhatsMessage.WhatsAppContextInfo.newBuilder();
                    if (expirationTime > 0) {
                        WhatsAppUtils.applyEphemeralMessage(contextInfoBuilder, expirationTime);
                    }
                    contextInfoBuilder = WhatsAppUtils.generateMentionedMsg(mentionedUserIds, contextInfoBuilder);
                    builder.setContextInfo(contextInfoBuilder);
                    protocolMessageBuild.setExtendedTextMessage(builder);
                } else {
                    if (messagePack.isUnicode()) {
                        protocolMessageBuild.setConversation(UnicodeUtil.toString(StrUtil.replace(messagePack.getContent(), "%u", "\\u")));
                    } else {
                        protocolMessageBuild.setConversation(messagePack.getContent());
                    }
                }
            } else {
                WhatsMessage.WhatsAppExtendedTextMessage.Builder builder = WhatsMessage.WhatsAppExtendedTextMessage.newBuilder();
                if (messagePack.isUnicode()) {
                    builder.setText(UnicodeUtil.toString(StrUtil.replace(messagePack.getContent(), "%u", "\\u")));
                } else {
                    builder.setText(messagePack.getContent());
                }
                userId = WhatsAppUtils.JidNormalize(userId);
                if (StringUtil.IsGroupJid(userId)) {
                    if (StringUtils.isEmpty(participant)) {
                        return false;
                    }
                    participant = WhatsAppUtils.JidNormalize(participant);
                } else {
                    participant = userId;
                }
                builder.setPreviewType(WhatsMessage.WhatsAppPreviewType.PreviewType_NONE);
                WhatsMessage.WhatsAppContextInfo.Builder contextInfoBuilder;
                if (!messagePack.isQuotedMedia()) {
                    contextInfoBuilder = WhatsAppUtils.generateQuotedMsg(participant, messagePack.getQuotedMsgId(),
                            messagePack.getQuotedMsgContent(), messagePack.isUnicode());
                } else {
                    contextInfoBuilder = WhatsAppUtils.generateMediaQuotedMsg(participant, messagePack.getQuotedMsgId(), messagePack.getQuotedMsgContent());
                }
                if (expirationTime > 0) {
                    WhatsAppUtils.applyEphemeralMessage(contextInfoBuilder, expirationTime);
                }
                WhatsAppUtils.generateMentionedMsg(mentionedUserIds, contextInfoBuilder);
                builder.setContextInfo(contextInfoBuilder);
                protocolMessageBuild.setExtendedTextMessage(builder);
            }
        }
        // 增加时间戳
        WhatsMessage.MessageContextInfo.Builder messageContextInfo = WhatsMessage.MessageContextInfo.newBuilder();
        WhatsMessage.DeviceListMetadata.Builder deviceListMetadata = WhatsMessage.DeviceListMetadata.newBuilder();
        deviceListMetadata.setSenderTimestamp(System.currentTimeMillis() / 1000);
        messageContextInfo.setDeviceListMetadataVersion(2);
        messageContextInfo.setDeviceListMetadata(deviceListMetadata);
        protocolMessageBuild.setMessageContextInfo(messageContextInfo);
        GorgeousEngine gorgeousEngine = user.getGorgeousEngine();
        if (gorgeousEngine != null) {
            if (userIds != null && userIds.size() > 0) {
                gorgeousEngine.addTaskToQueue(() -> gorgeousEngine.firstSendMsgToFans(null, userIds, protocolMessageBuild.build().toByteArray(), "text", "", getTaskId()));
            } else {
                gorgeousEngine.addTaskToQueue(() -> gorgeousEngine.firstSendMsgToFans(messagePack.getUserId(), protocolMessageBuild.build().toByteArray(), "text", "", getTaskId(), messagePack.getTcToken()));
            }
        }
        return true;
    }

    @Override
    public SendMessageResult parseResult(ProtocolTreeNode node) {
        return parseSendMessageResult(node);
    }
}
