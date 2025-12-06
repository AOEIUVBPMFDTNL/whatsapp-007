package com.whatsapp.android.api.message;

import Message.WhatsMessage;
import ProtocolTree.ProtocolTreeNode;
import Util.StringUtil;
import cn.hutool.core.codec.Base64;
import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.pack.message.SendResourcesMessagePack;
import com.whatsapp.android.entity.response.message.SendMessageResult;
import com.whatsapp.android.request.AbstractRequest;
import com.whatsapp.android.util.WhatsAppUtils;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 发送资源消息
 *
 * @author sunnoc
 * @date 2021-04-27 10:39
 */
public class SendResourcesMessageRequest extends AbstractRequest<SendMessageResult> {

    private SendResourcesMessagePack sendResourcesMessagePack;
    private String finalMsgId;

    public SendResourcesMessageRequest(SendResourcesMessagePack sendResourcesMessagePack) {
        this.sendResourcesMessagePack = sendResourcesMessagePack;
    }

    @Override
    public void init() {
        String msgId = this.sendResourcesMessagePack.getMsgId();
        if (StringUtils.hasLength(msgId)) {
            this.finalMsgId = msgId;
        } else {
            String userId = this.sendResourcesMessagePack.getUserId();
            this.finalMsgId = WhatsAppUtils.generateMsgId(userId, user.getLoginPack().isIosLogin());
        }
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.SEND_RESOURCES_MESSAGE + ":" + sendResourcesMessagePack.getMsgType();
    }

    public String getTaskId() {
        return finalMsgId;
    }

    @Override
    public long timeOut() {
        return 45;
    }

    @Override
    public boolean request() {
        String userId = sendResourcesMessagePack.getUserId();
        List<String> userIds = sendResourcesMessagePack.getUserIds();
        if (StringUtils.isEmpty(userId) && (userIds == null || userIds.size() == 0)) {
            user.getTaskNotify().setEventContent(getTaskId(), ProtocolTreeNode.fail(Constant.FAIL, "发送人不能为空"));
            return true;
        }
        WhatsMessage.WhatsAppContextInfo.Builder contextInfoBuilder = null;
        String quotedMsgId = sendResourcesMessagePack.getQuotedMsgId();
        String quotedMsgContent = sendResourcesMessagePack.getQuotedMsgContent();
        String participant = sendResourcesMessagePack.getParticipant();
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
            contextInfoBuilder = WhatsAppUtils.generateQuotedMsg(participant, quotedMsgId,
                    quotedMsgContent, false);
        }
        contextInfoBuilder = user.getGorgeousEngine().applyEphemeralMessage(sendResourcesMessagePack.getUserId(), contextInfoBuilder);
        String content = sendResourcesMessagePack.getContent();
        if (StringUtils.isEmpty(content)) {
            user.getTaskNotify().setEventContent(getTaskId(), ProtocolTreeNode.fail(Constant.FAIL, "内容不能为空"));
            return true;
        }
        String str = Base64.decodeStr(content);
        JSONObject mediaInfo = JSONObject.parseObject(str);
        if (!("ptt".equals(sendResourcesMessagePack.getMsgType())) && !("gif".equals(sendResourcesMessagePack.getMsgType()))) {
            // 只有文件，图片，视频才可以@人
            contextInfoBuilder = WhatsAppUtils.generateMentionedMsg(sendResourcesMessagePack.getMentionedUserIds(), contextInfoBuilder);
        }
        if ("image".equals(sendResourcesMessagePack.getMsgType()) || "ptt".equals(sendResourcesMessagePack.getMsgType()) ||
                "video".equals(sendResourcesMessagePack.getMsgType()) || "document".equals(sendResourcesMessagePack.getMsgType()) || "gif".equals(sendResourcesMessagePack.getMsgType())) {
            if (userIds != null && userIds.size() > 0) {
                user.getGorgeousEngine().submitMediaRequest(null, userIds, mediaInfo, getTaskId(), contextInfoBuilder);

            } else {
                user.getGorgeousEngine().submitMediaRequest(userId, null, mediaInfo, getTaskId(), contextInfoBuilder, sendResourcesMessagePack.getTcToken());
            }
        } else {
            user.getTaskNotify().setEventContent(getTaskId(), ProtocolTreeNode.fail(Constant.FAIL, "发送类型错误"));
        }
        return true;
    }

    @Override
    public SendMessageResult parseResult(ProtocolTreeNode node) {
        return parseSendMessageResult(node);
    }
}
