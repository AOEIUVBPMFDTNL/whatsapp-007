package com.whatsapp.android.service.impl.wam;

import Message.WhatsMessage;
import cn.hutool.core.util.ObjectUtil;
import lombok.experimental.UtilityClass;

@UtilityClass
public class WhatsAppContextInfoParseService {
    public WhatsMessage.WhatsAppContextInfo parse(WhatsMessage.WhatsAppMessage msg, String msgType, String mediaType) {
        if ("text".equals(msgType) || "url".equals(mediaType)) {
            if (msg.hasExtendedTextMessage()) {
                WhatsMessage.WhatsAppExtendedTextMessage extendedTextMessage = msg.getExtendedTextMessage();
                return extendedTextMessage.getContextInfo();
            } else if (msg.hasTimeLimitMessage()) {
                WhatsMessage.WhatsAppTimeLimitMessage timeLimitMessage = msg.getTimeLimitMessage();
                WhatsMessage.WhatsAppTimeLimitMessage.Content content = timeLimitMessage.getContent();
                if (content.hasExtendedTextMessage()) {
                    return content.getExtendedTextMessage().getContextInfo();
                }
            }
        } else if ("image".equals(mediaType)) {
            WhatsMessage.WhatsAppImageMessage message;
            if (msg.hasImageMessage()) {
                message = msg.getImageMessage();
            } else if (msg.hasTimeLimitMessage()) {
                message = msg.getTimeLimitMessage().getContent().getImageMessage();
            } else {
                return null;
            }
            return message.getContextInfo();
        } else if ("ptt".equals(mediaType) || "audio".equals(mediaType)) {
            WhatsMessage.WhatsAppAudioMessage message;
            if (msg.hasImageMessage()) {
                message = msg.getAudioMessage();
            } else if (msg.hasTimeLimitMessage()) {
                message = msg.getTimeLimitMessage().getContent().getAudioMessage();
            } else {
                return null;
            }
            return message.getContextInfo();
        } else if ("video".equals(mediaType)) {
            WhatsMessage.WhatsAppVideoMessage message;
            if (msg.hasImageMessage()) {
                message = msg.getVideoMessage();
            } else if (msg.hasTimeLimitMessage()) {
                message = msg.getTimeLimitMessage().getContent().getVideoMessage();
            } else {
                return null;
            }
            return message.getContextInfo();
        } else if ("document".equals(mediaType)) {
            WhatsMessage.WhatsAppDocumentMessage message;
            if (msg.hasImageMessage()) {
                message = msg.getDocumentMessage();
            } else if (msg.hasTimeLimitMessage()) {
                message = msg.getTimeLimitMessage().getContent().getDocumentMessage();
            } else {
                return null;
            }
            return message.getContextInfo();
        } else if ("location".equals(mediaType)) {
            WhatsMessage.WhatsAppLocationMessage message;
            if (msg.hasImageMessage()) {
                message = msg.getLocationMessage();
            } else if (msg.hasTimeLimitMessage()) {
                message = msg.getTimeLimitMessage().getContent().getLocationMessage();
            } else {
                return null;
            }
            return message.getContextInfo();
        } else if ("contact".equals(mediaType) || "vcard".equals(mediaType)) {
            WhatsMessage.WhatsAppContactMessage message;
            if (msg.hasImageMessage()) {
                message = msg.getContactMessage();
            } else if (msg.hasTimeLimitMessage()) {
                message = msg.getTimeLimitMessage().getContent().getContactMessage();
            } else {
                return null;
            }
            return message.getContextInfo();
        } else if ("gif".equals(mediaType)) {
            WhatsMessage.WhatsAppVideoMessage message;
            if (msg.hasImageMessage()) {
                message = msg.getVideoMessage();
            } else if (msg.hasTimeLimitMessage()) {
                message = msg.getTimeLimitMessage().getContent().getVideoMessage();
            } else {
                return null;
            }
            return message.getContextInfo();
        } else if ("sticker".equals(mediaType) || "1p_sticker".equals(mediaType) || "avatar_sticker".equals(mediaType)) {
            WhatsMessage.WhatsAppWebMessage message = null;
            if (msg.hasWebMessage()) {
                message = msg.getWebMessage();
            } else if (msg.hasLottieStickerMessage()) {
                WhatsMessage.FutureProofMessage lottieStickerMessage = msg.getLottieStickerMessage();
                message = lottieStickerMessage.getMessage().getWebMessage();
            } else {
                WhatsMessage.WhatsAppTimeLimitMessage.Content content = msg.getTimeLimitMessage().getContent();
                if (content.hasWebMessage()) {
                    message = msg.getWebMessage();
                }
            }
            if (ObjectUtil.isNotNull(message)) {
                return message.getContextInfo();
            }
        } else if ("contact_array".equals(mediaType)) {
            WhatsMessage.WhatsAppContactsArrayMessage message;
            if (msg.hasImageMessage()) {
                message = msg.getContactsArrayMessage();
            } else if (msg.hasTimeLimitMessage()) {
                message = msg.getTimeLimitMessage().getContent().getContactsArrayMessage();
            } else {
                return null;
            }
            return message.getContextInfo();
        }
        return null;
    }
}
