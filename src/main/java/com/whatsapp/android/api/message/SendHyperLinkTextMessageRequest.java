package com.whatsapp.android.api.message;

import Message.WhatsMessage;
import ProtocolTree.ProtocolTreeNode;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.google.protobuf.ByteString;
import com.whatsapp.android.GorgeousEngine;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.pack.message.HyperLinkMessagePack;
import com.whatsapp.android.entity.response.message.SendMessageResult;
import com.whatsapp.android.request.AbstractRequest;
import com.whatsapp.android.util.WhatsAppUtils;
import org.springframework.util.StringUtils;

/**
 * 发送超链接消息
 */
public class SendHyperLinkTextMessageRequest extends AbstractRequest<SendMessageResult> {
    private HyperLinkMessagePack hyperLinkMessagePack;
    private String finalMsgId;
    private byte[] imageByte;
    private String path;

    /**
     * 所需参数: 图片, 标题, 文字内容, 超链接介绍
     */
    public SendHyperLinkTextMessageRequest(HyperLinkMessagePack hyperLinkMessagePack, byte[] imageByte) {
        this.hyperLinkMessagePack = hyperLinkMessagePack;
        this.imageByte = imageByte;
    }

    @Override
    public void init() {
        String msgId = this.hyperLinkMessagePack.getMsgId();
        if (StringUtils.hasLength(msgId)) {
            this.finalMsgId = msgId;
        } else {
            String userId = this.hyperLinkMessagePack.getUserId();
            this.finalMsgId = WhatsAppUtils.generateMsgId(userId, user.getLoginPack().isIosLogin());
        }
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.SEND_HYPER_LINK_TEXT_MESSAGE;
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
        String userId = hyperLinkMessagePack.getUserId();
        String image = hyperLinkMessagePack.getImage();
        String linkTitle = hyperLinkMessagePack.getLinkTitle();
        String linkCaption = hyperLinkMessagePack.getLinkCaption();
        String linkUrl = hyperLinkMessagePack.getLinkUrl();
        if (StrUtil.isEmpty(userId) || StrUtil.isEmpty(image) || StrUtil.isEmpty(linkTitle) || StrUtil.isEmpty(linkCaption) || StrUtil.isEmpty(linkUrl)) {
            return false;
        }
        String fileName = IdUtil.randomUUID();
        String tempPath = System.getProperty("user.dir") + "/out/media/" + fileName;
        String outPath = System.getProperty("user.dir") + "/out/media/" + fileName + ".jpg";
        FileUtil.writeBytes(imageByte, tempPath);
        WhatsAppUtils.imageCapture(tempPath, outPath);
        path = tempPath;
        byte[] thumbnail = FileUtil.readBytes(outPath);
        WhatsMessage.WhatsAppMessage.Builder protocolMessageBuild = WhatsMessage.WhatsAppMessage.newBuilder();
        WhatsMessage.WhatsAppExtendedTextMessage.Builder extendedTextMessageBuilder = WhatsMessage.WhatsAppExtendedTextMessage.newBuilder();
        WhatsMessage.WhatsAppContextInfo.Builder contextBuilder = WhatsMessage.WhatsAppContextInfo.newBuilder();
        WhatsMessage.ExternalAdReplyInfo.Builder extendAdReplyInfoBuilder = WhatsMessage.ExternalAdReplyInfo.newBuilder();
        // 设置文本内容
        if (StrUtil.isNotEmpty(hyperLinkMessagePack.getContent())) {
            extendedTextMessageBuilder.setText(hyperLinkMessagePack.getContent());
        }
        // 设置超链接内容
        extendAdReplyInfoBuilder.setTitle(linkTitle);
        extendAdReplyInfoBuilder.setBody(linkCaption);
        extendAdReplyInfoBuilder.setMediaType(WhatsMessage.ExternalAdReplyInfo.MediaType.VIDEO);
        extendAdReplyInfoBuilder.setMediaUrl(linkUrl);
        extendAdReplyInfoBuilder.setThumbnail(ByteString.copyFrom(thumbnail));
        extendAdReplyInfoBuilder.setContainsAutoReply(false);
        extendAdReplyInfoBuilder.setRenderLargerThumbnail(false);
        extendAdReplyInfoBuilder.setShowAdAttribution(false);
        extendAdReplyInfoBuilder.setClickToWhatsappCall(false);
        extendAdReplyInfoBuilder.setAdContextPreviewDismissed(false);
        extendAdReplyInfoBuilder.setAutomatedGreetingMessageShown(false);
        extendAdReplyInfoBuilder.setDisableNudg(false);
        contextBuilder.setExternalAdReply(extendAdReplyInfoBuilder);
        contextBuilder.setForwardingScore(2);
        contextBuilder.setIsForwarded(true);
        // 增加时间戳
        WhatsMessage.MessageContextInfo.Builder messageContextInfo = WhatsMessage.MessageContextInfo.newBuilder();
        WhatsMessage.DeviceListMetadata.Builder deviceListMetadata = WhatsMessage.DeviceListMetadata.newBuilder();
        deviceListMetadata.setSenderTimestamp(System.currentTimeMillis() / 1000);
        messageContextInfo.setDeviceListMetadataVersion(2);
        messageContextInfo.setDeviceListMetadata(deviceListMetadata);
        protocolMessageBuild.setMessageContextInfo(messageContextInfo);
        extendedTextMessageBuilder.setContextInfo(contextBuilder);
        protocolMessageBuild.setExtendedTextMessage(extendedTextMessageBuilder);
        GorgeousEngine gorgeousEngine = user.getGorgeousEngine();
        gorgeousEngine.addTaskToQueue(() -> gorgeousEngine.firstSendMsgToFans(hyperLinkMessagePack.getUserId(), protocolMessageBuild.build().toByteArray(), "text", "", getTaskId(), hyperLinkMessagePack.getTcToken()));
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
    public SendMessageResult parseResult(ProtocolTreeNode node) {
        return parseSendMessageResult(node);
    }
}
