package com.whatsapp.android.crypto;

import Message.WhatsMessage;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import com.google.protobuf.InvalidProtocolBufferException;
import com.whatsapp.android.util.WhatsAppUtils;
import lombok.extern.slf4j.Slf4j;
import org.whispersystems.libsignal.kdf.HKDFv3;

import java.nio.charset.StandardCharsets;

/**
 * ReportToken加密需要拆分proto结构
 *
 * @author Rocky
 */
@Slf4j
public class ReportToken {

    public static byte[] generateEncryptProto(WhatsMessage.WhatsAppMessage whatsAppMessage) {
        // WhatsAppWebMessage, viewOnceMessage, FutureProofMessage 这三个暂且未用到，先不做
        WhatsMessage.WhatsAppMessage.Builder reportTokenBuilder = WhatsMessage.WhatsAppMessage.newBuilder();
        if (whatsAppMessage.hasConversation()) {
            // 文本消息
            reportTokenBuilder.setConversation(whatsAppMessage.getConversation());
        }
        if (whatsAppMessage.hasImageMessage()) {
            WhatsMessage.WhatsAppImageMessage imageMessage = whatsAppMessage.getImageMessage();
            // 图片消息
            WhatsMessage.WhatsAppImageMessage.Builder imageBuilder = WhatsMessage.WhatsAppImageMessage.newBuilder();
            if (imageMessage.hasMimetype()) {
                imageBuilder.setMimetype(imageMessage.getMimetype());
            }
            if (imageMessage.hasCaption()) {
                imageBuilder.setCaption(imageMessage.getCaption());
            }
            if (imageMessage.hasMediaKey()) {
                imageBuilder.setMediaKey(imageMessage.getMediaKey());
            }
            if (imageMessage.hasDirectPath()) {
                imageBuilder.setDirectPath(imageMessage.getDirectPath());
            }
            if (imageMessage.hasContextInfo()) {
                WhatsMessage.WhatsAppContextInfo imageContextInfo = imageMessage.getContextInfo();
                WhatsMessage.WhatsAppContextInfo.Builder imageContextBuilder = WhatsMessage.WhatsAppContextInfo.newBuilder();
                if (imageContextInfo.hasForwardingScore()) {
                    imageContextBuilder.setForwardingScore(imageContextInfo.getForwardingScore());
                }
                if (imageContextInfo.hasIsForwarded()) {
                    imageContextBuilder.setIsForwarded(imageContextInfo.getIsForwarded());
                }
                if (imageContextBuilder.hasIsForwarded() || imageContextBuilder.hasForwardingScore()) {
                    imageBuilder.setContextInfo(imageContextBuilder);
                }
            }
            if (imageMessage.hasViewOnce()) {
                imageBuilder.setViewOnce(imageMessage.getViewOnce());
            }
            reportTokenBuilder.setImageMessage(imageBuilder);
        }
        if (whatsAppMessage.hasExtendedTextMessage()) {
            WhatsMessage.WhatsAppExtendedTextMessage extendedTextMessage = whatsAppMessage.getExtendedTextMessage();
            WhatsMessage.WhatsAppExtendedTextMessage.Builder extendedTextMessageBuilder = WhatsMessage.WhatsAppExtendedTextMessage.newBuilder();
            if (extendedTextMessage.hasText()) {
                extendedTextMessageBuilder.setText(extendedTextMessage.getText());
            }
            if (extendedTextMessage.hasContextInfo()) {
                WhatsMessage.WhatsAppContextInfo extendedTextMessageContextInfo = extendedTextMessage.getContextInfo();
                WhatsMessage.WhatsAppContextInfo.Builder extendedTextMessageContextInfoBuilder = WhatsMessage.WhatsAppContextInfo.newBuilder();
                if (extendedTextMessageContextInfo.hasForwardingScore()) {
                    extendedTextMessageContextInfoBuilder.setForwardingScore(extendedTextMessageContextInfo.getForwardingScore());
                }
                if (extendedTextMessageContextInfo.hasIsForwarded()) {
                    extendedTextMessageContextInfoBuilder.setIsForwarded(extendedTextMessageContextInfo.getIsForwarded());
                }
                if (extendedTextMessageContextInfoBuilder.hasForwardingScore() || extendedTextMessageContextInfoBuilder.hasIsForwarded()) {
                    extendedTextMessageBuilder.setContextInfo(extendedTextMessageContextInfoBuilder);
                }
            }
            if (extendedTextMessage.hasViewOnce()) {
                extendedTextMessageBuilder.setViewOnce(extendedTextMessage.getViewOnce());
            }
            reportTokenBuilder.setExtendedTextMessage(extendedTextMessageBuilder);
        }
        if (whatsAppMessage.hasDocumentMessage()) {
            WhatsMessage.WhatsAppDocumentMessage documentMessage = whatsAppMessage.getDocumentMessage();
            WhatsMessage.WhatsAppDocumentMessage.Builder documentMessageBuilder = WhatsMessage.WhatsAppDocumentMessage.newBuilder();
            if (documentMessage.hasMimetype()) {
                documentMessageBuilder.setMimetype(documentMessage.getMimetype());
            }
            if (documentMessage.hasMediaKey()) {
                documentMessageBuilder.setMediaKey(documentMessage.getMediaKey());
            }
            if (documentMessage.hasDirectPath()) {
                documentMessageBuilder.setDirectPath(documentMessage.getDirectPath());
            }
            if (documentMessage.hasContextInfo()) {
                WhatsMessage.WhatsAppContextInfo documentMessageContextInfo = documentMessage.getContextInfo();
                WhatsMessage.WhatsAppContextInfo.Builder documentMessageContextInfoBuilder = WhatsMessage.WhatsAppContextInfo.newBuilder();
                if (documentMessageContextInfo.hasForwardingScore()) {
                    documentMessageContextInfoBuilder.setForwardingScore(documentMessageContextInfo.getForwardingScore());
                }
                if (documentMessageContextInfo.hasIsForwarded()) {
                    documentMessageContextInfoBuilder.setIsForwarded(documentMessageContextInfo.getIsForwarded());
                }
                if (documentMessageContextInfoBuilder.hasForwardingScore() || documentMessageContextInfoBuilder.hasIsForwarded()) {
                    documentMessageBuilder.setContextInfo(documentMessageContextInfoBuilder);
                }
            }
            if (documentMessage.hasCaption()) {
                documentMessageBuilder.setCaption(documentMessage.getCaption());
            }
            reportTokenBuilder.setDocumentMessage(documentMessageBuilder);

        }
        if (whatsAppMessage.hasAudioMessage()) {
            WhatsMessage.WhatsAppAudioMessage audioMessage = whatsAppMessage.getAudioMessage();
            WhatsMessage.WhatsAppAudioMessage.Builder audioMessageBuilder = WhatsMessage.WhatsAppAudioMessage.newBuilder();
            if (audioMessage.hasMimetype()) {
                audioMessageBuilder.setMimetype(audioMessage.getMimetype());
            }
            if (audioMessage.hasMediaKey()) {
                audioMessageBuilder.setMediaKey(audioMessage.getMediaKey());
            }
            if (audioMessage.hasDirectPath()) {
                audioMessageBuilder.setDirectPath(audioMessage.getDirectPath());
            }
            if (audioMessage.hasContextInfo()) {
                WhatsMessage.WhatsAppContextInfo audioMessageContextInfo = audioMessage.getContextInfo();
                WhatsMessage.WhatsAppContextInfo.Builder audioMessageContextInfoBuilder = WhatsMessage.WhatsAppContextInfo.newBuilder();
                if (audioMessageContextInfo.hasForwardingScore()) {
                    audioMessageContextInfoBuilder.setForwardingScore(audioMessageContextInfo.getForwardingScore());
                }
                if (audioMessageContextInfo.hasIsForwarded()) {
                    audioMessageContextInfoBuilder.setIsForwarded(audioMessageContextInfo.getIsForwarded());
                }
                if (audioMessageContextInfoBuilder.hasForwardingScore() || audioMessageContextInfoBuilder.hasIsForwarded()) {
                    audioMessageBuilder.setContextInfo(audioMessageContextInfoBuilder);
                }
            }
            if (audioMessage.hasViewOnce()) {
                audioMessageBuilder.setViewOnce(audioMessage.getViewOnce());
            }
            reportTokenBuilder.setAudioMessage(audioMessageBuilder);

        }
        if (whatsAppMessage.hasVideoMessage()) {
            WhatsMessage.WhatsAppVideoMessage videoMessage = whatsAppMessage.getVideoMessage();
            WhatsMessage.WhatsAppVideoMessage.Builder videoMessageBuilder = WhatsMessage.WhatsAppVideoMessage.newBuilder();
            if (videoMessage.hasMimetype()) {
                videoMessageBuilder.setMimetype(videoMessage.getMimetype());
            }
            if (videoMessage.hasMediaKey()) {
                videoMessageBuilder.setMediaKey(videoMessage.getMediaKey());
            }
            if (videoMessage.hasDirectPath()) {
                videoMessageBuilder.setDirectPath(videoMessage.getDirectPath());
            }
            if (videoMessage.hasCaption()) {
                videoMessageBuilder.setCaption(videoMessage.getCaption());
            }
            if (videoMessage.hasContextInfo()) {
                WhatsMessage.WhatsAppContextInfo videoMessageContextInfo = videoMessage.getContextInfo();
                WhatsMessage.WhatsAppContextInfo.Builder videoMessageContextInfoBuilder = WhatsMessage.WhatsAppContextInfo.newBuilder();
                if (videoMessageContextInfo.hasForwardingScore()) {
                    videoMessageContextInfoBuilder.setForwardingScore(videoMessageContextInfo.getForwardingScore());
                }
                if (videoMessageContextInfo.hasIsForwarded()) {
                    videoMessageContextInfoBuilder.setIsForwarded(videoMessageContextInfo.getIsForwarded());
                }
                if (videoMessageContextInfoBuilder.hasForwardingScore() || videoMessageContextInfoBuilder.hasIsForwarded()) {
                    videoMessageBuilder.setContextInfo(videoMessageContextInfoBuilder);
                }
            }
            if (videoMessage.hasViewOnce()) {
                videoMessageBuilder.setViewOnce(videoMessage.getViewOnce());
            }
            reportTokenBuilder.setVideoMessage(videoMessageBuilder);
        }
        byte[] byteArray = reportTokenBuilder.build().toByteArray();
        if (byteArray.length == 0) {
            return null;
        }
        return byteArray;
    }

    public static byte[] generateEncryptProto(byte[] messageProto) {
        try {
            WhatsMessage.WhatsAppMessage whatsAppMessage = WhatsMessage.WhatsAppMessage.parseFrom(messageProto);
            return generateEncryptProto(whatsAppMessage);
        } catch (InvalidProtocolBufferException e) {
            log.error("ReportToken 执行generateEncryptProto出现异常");
            // 出现异常则返回原来的
            return messageProto;
        }
    }

    public static byte[] generateEncryptToken(byte[] encryptProto, byte[] messageSecret, String msgId, String senderJid, String receiveJid) {
        String salt = msgId + WhatsAppUtils.JidNormalize(senderJid) + WhatsAppUtils.JidNormalize(receiveJid) + "Report Token";
        byte[] expendKey = new HKDFv3().deriveSecrets(messageSecret, salt.getBytes(StandardCharsets.UTF_8), 0x20);
        HMac hmac = DigestUtil.hmac(HmacAlgorithm.HmacSHA256, expendKey);
        byte[] digest = hmac.digest(encryptProto);
        byte[] output = new byte[16];
        System.arraycopy(digest, 0, output, 0, 16);
        return output;
    }

}
