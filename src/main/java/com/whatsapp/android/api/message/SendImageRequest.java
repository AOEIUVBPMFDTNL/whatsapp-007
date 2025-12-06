package com.whatsapp.android.api.message;

import Message.WhatsMessage;
import ProtocolTree.ProtocolTreeNode;
import Util.StringUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.pack.message.MessagePack;
import com.whatsapp.android.entity.response.message.SendMessageResult;
import com.whatsapp.android.request.AbstractRequest;
import com.whatsapp.android.util.WhatsAppUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.List;

/**
 * 发送图片
 *
 * @author sunnoc
 * @date 2021-03-11 12:15
 */
public class SendImageRequest extends AbstractRequest<SendMessageResult> {
    private byte[] file;
    private String path;
    private MessagePack messagePack;
    private String finalMsgId;

    public SendImageRequest(byte[] file, MessagePack messagePack) {
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
        return TypeConstant.TaskType.SEND_IMAGE_MESSAGE;
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
        String fileName = IdUtil.randomUUID();
        String tempPath = System.getProperty("user.dir") + "/out/media/" + fileName;
        FileUtil.writeBytes(file, tempPath);
        String outPath = WhatsAppUtils.imageToJpg(tempPath);
        FileUtil.del(tempPath);
        FileUtil.rename(new File(outPath), fileName, true);
        path = tempPath;
        if (userIds != null && userIds.size() > 0) {
            user.getGorgeousEngine().SendMedia(userIds, path, "image", getTaskId(), null, messagePack.getCaption(), contextInfoBuilder, timeOut() * 1000);
        } else {
            user.getGorgeousEngine().SendMedia(userId, path, "image", getTaskId(), null, messagePack.getCaption(), contextInfoBuilder, timeOut() * 1000, messagePack.getTcToken());
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
