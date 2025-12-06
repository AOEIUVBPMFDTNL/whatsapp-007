package com.whatsapp.android.api.message;

import Message.WhatsMessage;
import ProtocolTree.ProtocolTreeNode;
import Util.StringUtil;
import cn.hutool.core.io.FileUtil;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.pack.message.MessagePack;
import com.whatsapp.android.entity.response.message.SendMessageResult;
import com.whatsapp.android.request.AbstractRequest;
import com.whatsapp.android.util.WhatsAppUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

/**
 * 发送文件消息
 *
 * @author sunnoc
 * @date 2021-03-12 14:38
 */
public class SendFileMessageRequest extends AbstractRequest<SendMessageResult> {
    private byte[] file;
    private MessagePack messagePack;

    /**
     * 带后缀的文件名
     */
    private String path;
    private String finalMsgId;

    public SendFileMessageRequest(byte[] file, MessagePack messagePack) {
        this.file = file;
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
    public String getTaskId() {
        return finalMsgId;
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.SEND_FILE_MESSAGE;
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
                    messagePack.getQuotedMsgContent(), messagePack.isUnicode());
        }
        contextInfoBuilder = user.getGorgeousEngine().applyEphemeralMessage(messagePack.getUserId(), contextInfoBuilder);
        contextInfoBuilder = WhatsAppUtils.generateMentionedMsg(messagePack.getMentionedUserIds(), contextInfoBuilder);
        path = System.getProperty("user.dir") + "/out/media/" + UUID.randomUUID().toString();
        FileUtil.writeBytes(file, path);
        if (userIds != null && userIds.size() > 0) {
            user.getGorgeousEngine().SendMedia(userIds, path, "document", getTaskId(), messagePack.getFileName(), null, contextInfoBuilder, timeOut() * 1000);
        } else {
            user.getGorgeousEngine().SendMedia(userId, path, "document", getTaskId(), messagePack.getFileName(), null, contextInfoBuilder, timeOut() * 1000, messagePack.getTcToken());
        }
        return true;
    }

    @Override
    public boolean needDeleteFile() {
        return true;
    }

    @Override
    public String deleteFile() {
        return path;
    }

    @Override
    public long timeOut() {
        return 120L;
    }

    @Override
    public SendMessageResult parseResult(ProtocolTreeNode node) {
        return parseSendMessageResult(node);
    }
}
