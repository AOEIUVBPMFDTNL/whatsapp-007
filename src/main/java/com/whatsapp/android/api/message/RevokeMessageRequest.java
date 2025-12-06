package com.whatsapp.android.api.message;

import Message.WhatsMessage;
import ProtocolTree.ProtocolTreeNode;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.whatsapp.android.GorgeousEngine;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.pack.message.RevokeMessagePack;
import com.whatsapp.android.request.AbstractRequest;
import com.whatsapp.android.util.WhatsAppUtils;
import org.springframework.util.StringUtils;

import static com.whatsapp.android.util.WhatsAppUtils.JidNormalize;

/**
 * 撤回消息
 *
 * @author sunnoc
 * @date 2021-12-29 17:45
 */
public class RevokeMessageRequest extends AbstractRequest<StatusResult> {
    private RevokeMessagePack revokeMessagePack;
    private String finalMsgId;

    public RevokeMessageRequest(RevokeMessagePack revokeMessagePack) {
        this.revokeMessagePack = revokeMessagePack;
    }

    @Override
    public void init() {
        String userId = this.revokeMessagePack.getUserId();
        this.finalMsgId = WhatsAppUtils.generateMsgId(userId, user.getLoginPack().isIosLogin());
    }

    @Override
    public String getTaskId() {
        return finalMsgId;
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.REVOKE_MESSAGE;
    }

    @Override
    public boolean request() {
        WhatsMessage.WhatsAppMessage.Builder protocolMessageBuild = WhatsMessage.WhatsAppMessage.newBuilder();
        WhatsMessage.WhatsAppProtocolMessage.Builder builder = WhatsMessage.WhatsAppProtocolMessage.newBuilder();
        WhatsMessage.WhatsAppProtocolMessage.MessageKey.Builder keyBuilder = WhatsMessage.WhatsAppProtocolMessage.MessageKey.newBuilder();
        GorgeousEngine gorgeousEngine = user.getGorgeousEngine();
        String userId = revokeMessagePack.getUserId();
        String revokeMsgId = revokeMessagePack.getRevokeMsgId();
        String participant = revokeMessagePack.getParticipant();
        boolean adminDelete = revokeMessagePack.isAdminDelete();
        if (StringUtils.isEmpty(userId) || StringUtils.isEmpty(revokeMsgId)) {
            return false;
        }
        if (ObjectUtil.isNotNull(revokeMessagePack.isReaction()) && revokeMessagePack.isReaction()) {
            if (ObjectUtil.isNull(revokeMessagePack.isFromMe())) {
                return false;
            }
            // 撤回心情
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
            messageKey.setFromMe(revokeMessagePack.isFromMe());
            messageKey.setId(revokeMsgId);
            messageKey.setRemoteJid(jid);
            if (StrUtil.isNotEmpty(participant)) {
                String participant_ = JidNormalize(participant);
                messageKey.setParticipant(participant_);
            }
            reactionMessage.setKey(messageKey);
            reactionMessage.setText("");
            reactionMessage.setSenderTimestampMs(System.currentTimeMillis());
            protocolMessageBuild.setReactionMessage(reactionMessage);
            if (gorgeousEngine != null) {
                gorgeousEngine.addTaskToQueue(() -> gorgeousEngine.firstSendMsgToFans(revokeMessagePack.getUserId(), protocolMessageBuild.build().toByteArray(), TypeConstant.TaskType.REVOKE_REACTION_MESSAGE, "", getTaskId(), revokeMessagePack.getTcToken()));
                return true;
            }
            return false;
        }
        userId = WhatsAppUtils.JidNormalize(userId);
        keyBuilder.setRemoteJid(userId);
//        keyBuilder.setFromMe(1);
        keyBuilder.setId(revokeMsgId);
        if (adminDelete && StrUtil.isNotEmpty(participant)) {
            // 群聊管理员删除
            keyBuilder.setFromMe(0);
            String participantJid = WhatsAppUtils.JidNormalize(participant);
            keyBuilder.setParticipant(participantJid);
        } else {
            keyBuilder.setFromMe(1);
        }
        builder.setKey(keyBuilder);
        builder.setType(WhatsMessage.WhatsAppProtocolMessageType.REVOKE);
        protocolMessageBuild.setProtocolMessage(builder);
        if (gorgeousEngine != null) {
            gorgeousEngine.addTaskToQueue(() -> gorgeousEngine.firstSendMsgToFans(revokeMessagePack.getUserId(), protocolMessageBuild.build().toByteArray(), TypeConstant.TaskType.REVOKE_MESSAGE, "", getTaskId(), revokeMessagePack.getTcToken()));
        }
        return true;
    }

    @Override
    public long timeOut() {
        return 60L;
    }

    @Override
    public StatusResult parseResult(ProtocolTreeNode node) {
        return parseBaseResult(node);
    }
}
