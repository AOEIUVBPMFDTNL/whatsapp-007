package com.whatsapp.android.service;

import Message.WhatsMessage;
import cn.hutool.core.util.IdUtil;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.entity.UserRecord;
import com.whatsapp.android.entity.pack.account.LoginPack;
import com.whatsapp.android.entity.response.goup.GroupNotifyResult;
import com.whatsapp.android.entity.response.message.VoipAcceptResult;
import com.whatsapp.android.entity.response.profile.TcToken;
import com.whatsapp.android.util.WhatsAppUtils;
import com.whatsapp.android.ws.WebSocketClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentMap;

/**
 * @author sunnoc
 * @date 2021-03-15 10:25
 */
@Slf4j
public class AsyncMessagePushService {
    /**
     * 文字消息
     */
    public void textMsg(String groupId, String from, String to, String content, String msgId, String username, long msgTime, boolean offlineMessage, boolean urlNumber) {
        Map<String, Object> map;
        if (StringUtils.isEmpty(groupId)) {
            map = publicPack(TypeConstant.MsgType.TEXT_MSG, null, from, to, content, msgId);
        } else {
            map = publicPack(TypeConstant.MsgType.TEXT_MSG, groupId, from, to, content, msgId);
        }
        map.put("offlineMessage", offlineMessage);
        map.put("urlNumber", urlNumber);
        push(groupId, username, map, msgId, msgTime);
    }


    /**
     * 图片消息
     */
    public void imageMsg(String groupId, String from, String to, int height, int width, String url, String caption, String msgId, String username, long msgTime, boolean offlineMessage, boolean urlNumber) {
        HashMap<String, Object> preview = new HashMap<>();
        preview.put("height", height);
        preview.put("width", width);
        preview.put("url", url);
        preview.put("caption", caption);
        Map<String, Object> map;
        if (StringUtils.isEmpty(groupId)) {
            map = publicPack(TypeConstant.MsgType.IMAGE_MSG, null, from, to, null, msgId);
        } else {
            map = publicPack(TypeConstant.MsgType.IMAGE_MSG, groupId, from, to, null, msgId);
        }
        map.put("data", preview);
        map.put("offlineMessage", offlineMessage);
        map.put("urlNumber", urlNumber);
        push(groupId, username, map, msgId, msgTime);
    }

    /**
     * 动画消息
     */
    public void gifMsg(String groupId, String from, String to, String url, String caption, String msgId, String username, long msgTime, boolean offlineMessage, boolean urlNumber) {
        HashMap<String, Object> dataMap = new HashMap<>();
        dataMap.put("url", url);
        dataMap.put("caption", caption);
        Map<String, Object> map;
        if (StringUtils.isEmpty(groupId)) {
            map = publicPack(TypeConstant.MsgType.GIF_MSG, null, from, to, null, msgId);
        } else {
            map = publicPack(TypeConstant.MsgType.GIF_MSG, groupId, from, to, null, msgId);
        }
        map.put("data", dataMap);
        map.put("offlineMessage", offlineMessage);
        map.put("urlNumber", urlNumber);
        push(groupId, username, map, msgId, msgTime);
    }

    /**
     * 贴纸消息
     */
    public void stickerMsg(String groupId, String from, String to, String url, String msgId, String username, long msgTime, boolean offlineMessage, boolean urlNumber) {
        HashMap<String, Object> dataMap = new HashMap<>();
        dataMap.put("url", url);
        Map<String, Object> map;
        if (StringUtils.isEmpty(groupId)) {
            map = publicPack(TypeConstant.MsgType.STICKER_MSG, null, from, to, null, msgId);
        } else {
            map = publicPack(TypeConstant.MsgType.STICKER_MSG, groupId, from, to, null, msgId);
        }
        map.put("data", dataMap);
        map.put("offlineMessage", offlineMessage);
        map.put("urlNumber", urlNumber);
        push(groupId, username, map, msgId, msgTime);
    }

    /**
     * lottie贴纸消息
     */
    public void LottieStickerMsg(String groupId, String from, String to, String url, String msgId, String username, long msgTime, boolean offlineMessage, boolean urlNumber) {
        HashMap<String, Object> dataMap = new HashMap<>();
        dataMap.put("url", url);
        Map<String, Object> map;
        if (StringUtils.isEmpty(groupId)) {
            map = publicPack(TypeConstant.MsgType.LOTTIE_STICKER_MSG, null, from, to, null, msgId);
        } else {
            map = publicPack(TypeConstant.MsgType.LOTTIE_STICKER_MSG, groupId, from, to, null, msgId);
        }
        map.put("data", dataMap);
        map.put("offlineMessage", offlineMessage);
        map.put("urlNumber", urlNumber);
        push(groupId, username, map, msgId, msgTime);
    }

    /**
     * 语音消息
     */
    public void voiceMsg(String groupId, String from, String to, String url, int playTime, boolean voiceMsg, String msgId, String username, long msgTime, boolean offlineMessage, boolean urlNumber) {
        HashMap<String, Object> dataMap = new HashMap<>(4);
        dataMap.put("url", url);
        dataMap.put("playTime", playTime);
        dataMap.put("voiceMsg", voiceMsg);
        Map<String, Object> map;
        if (StringUtils.isEmpty(groupId)) {
            map = publicPack(TypeConstant.MsgType.VOICE_MSG, null, from, to, null, msgId);
        } else {
            map = publicPack(TypeConstant.MsgType.VOICE_MSG, groupId, from, to, null, msgId);
        }
        map.put("data", dataMap);
        map.put("offlineMessage", offlineMessage);
        map.put("urlNumber", urlNumber);
        push(groupId, username, map, msgId, msgTime);
    }

    /**
     * 视屏消息
     */
    public void videoMsg(String groupId, String from, String to, String url, int playTime, String caption, String msgId, String username, long msgTime, boolean offlineMessage, boolean urlNumber) {
        HashMap<String, Object> dataMap = new HashMap<>(4);
        dataMap.put("url", url);
        dataMap.put("playTime", playTime);
        dataMap.put("caption", caption);
        Map<String, Object> map;
        if (StringUtils.isEmpty(groupId)) {
            map = publicPack(TypeConstant.MsgType.VIDEO_MSG, null, from, to, null, msgId);
        } else {
            map = publicPack(TypeConstant.MsgType.VIDEO_MSG, groupId, from, to, null, msgId);
        }
        map.put("data", dataMap);
        map.put("offlineMessage", offlineMessage);
        map.put("urlNumber", urlNumber);
        push(groupId, username, map, msgId, msgTime);
    }

    /**
     * 文件消息
     */
    public void fileMsg(String groupId, String from, String to, String url, String fileName, String caption, String msgId, String username, long msgTime, boolean offlineMessage, boolean urlNumber) {
        int index = fileName.lastIndexOf(".");
        String extensionName = "";
        if (index >= 0) {
            extensionName = fileName.substring(index + 1);
        }
        HashMap<String, Object> dataMap = new HashMap<>(5);
        dataMap.put("url", url);
        dataMap.put("fileName", fileName);
        dataMap.put("extensionName", extensionName);
        dataMap.put("caption", caption);
        Map<String, Object> map;
        if (StringUtils.isEmpty(groupId)) {
            map = publicPack(TypeConstant.MsgType.FILE_MSG, null, from, to, null, msgId);
        } else {
            map = publicPack(TypeConstant.MsgType.FILE_MSG, groupId, from, to, null, msgId);
        }
        map.put("data", dataMap);
        map.put("offlineMessage", offlineMessage);
        map.put("urlNumber", urlNumber);
        push(groupId, username, map, msgId, msgTime);
    }

    /**
     * 地理位置消息
     */
    public void locationMsg(String groupId, String from, String to, String latitude, String longitude, String msgId, String username, long msgTime, boolean offlineMessage, boolean urlNumber) {
        HashMap<String, Object> dataMap = new HashMap<>(4);
        dataMap.put("latitude", latitude);
        dataMap.put("longitude", longitude);
        Map<String, Object> map;
        if (StringUtils.isEmpty(groupId)) {
            map = publicPack(TypeConstant.MsgType.LOCATION_MSG, null, from, to, null, msgId);
        } else {
            map = publicPack(TypeConstant.MsgType.LOCATION_MSG, groupId, from, to, null, msgId);
        }
        map.put("data", dataMap);
        map.put("offlineMessage", offlineMessage);
        map.put("urlNumber", urlNumber);
        push(groupId, username, map, msgId, msgTime);
    }

    /**
     * 名片消息
     */
    public void contactCardMsg(String groupId, String from, String to, String displayName, String vcard_, String msgId, String username, long msgTime, boolean offlineMessage, boolean urlNumber) {
        HashMap<String, Object> dataMap = new HashMap<>(4);
        dataMap.put("displayName", displayName);
        dataMap.put("vcard_", vcard_);
        Map<String, Object> map;
        if (StringUtils.isEmpty(groupId)) {
            map = publicPack(TypeConstant.MsgType.CONTACT_CARD_MSG, null, from, to, null, msgId);
        } else {
            map = publicPack(TypeConstant.MsgType.CONTACT_CARD_MSG, groupId, from, to, null, msgId);
        }
        map.put("data", dataMap);
        map.put("offlineMessage", offlineMessage);
        map.put("urlNumber", urlNumber);
        push(groupId, username, map, msgId, msgTime);
    }

    /**
     * 名片数组消息
     */
    public void contactCardArrayMsg(String groupId, String from, String to, List<WhatsMessage.WhatsAppContactMessage> contactsList, String msgId, String username, long msgTime, boolean offlineMessage, boolean urlNumber) {
        if (contactsList != null) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (WhatsMessage.WhatsAppContactMessage whatsAppContactMessage : contactsList) {
                String displayName = whatsAppContactMessage.getDisplayName();
                String vcard = whatsAppContactMessage.getVcard();
                Map<String, Object> dataMap = new HashMap<>(4);
                dataMap.put("displayName", displayName);
                dataMap.put("vcard_", vcard);
                list.add(dataMap);
            }
            Map<String, Object> map;
            if (StringUtils.isEmpty(groupId)) {
                map = publicPack(TypeConstant.MsgType.CONTACT_CARD_ARRAY_MSG, null, from, to, null, msgId);
            } else {
                map = publicPack(TypeConstant.MsgType.CONTACT_CARD_ARRAY_MSG, groupId, from, to, null, msgId);
            }
            map.put("data", list);
            map.put("offlineMessage", offlineMessage);
            map.put("urlNumber", urlNumber);
            push(groupId, username, map, msgId, msgTime);
        }
    }

    /**
     * 收到语音/视频邀请
     */
    public void voiceCallInvite(String from, String to, String msgId, String username, long msgTime, boolean offlineMessage, boolean urlNumber) {
        Map<String, Object> map = publicPack(TypeConstant.MsgType.VOICE_CALL_INVITE, null, from, to, "收到语音/视频邀请", msgId);
        map.put("offlineMessage", offlineMessage);
        map.put("urlNumber", urlNumber);
        push(null, username, map, msgId, msgTime);
    }

    /**
     * 语音/视频邀请结束
     */
    public void voiceCallEnd(String from, String to, String msgId, String username, long msgTime, boolean offlineMessage, boolean urlNumber) {
        Map<String, Object> map = publicPack(TypeConstant.MsgType.VOICE_CALL_END, null, from, to, "语音/视频邀请结束", msgId);
        map.put("offlineMessage", offlineMessage);
        map.put("urlNumber", urlNumber);
        push(null, username, map, msgId, msgTime);
    }

    /**
     * 语音/视频邀请被拒绝
     */
    public void voiceCallReject(String from, String to, String msgId, String username, long msgTime, boolean offlineMessage, boolean urlNumber) {
        Map<String, Object> map = publicPack(TypeConstant.MsgType.VOICE_CALL_REJECT, null, from, to, "语音/视频邀请被拒绝", msgId);
        map.put("offlineMessage", offlineMessage);
        map.put("urlNumber", urlNumber);
        push(null, username, map, msgId, msgTime);
    }

    /**
     * 粉丝已读取消息
     */
    public void receiptMsgRead(String from, String to, String msgId, List<String> otherMsgIdList, String username, long msgTime, boolean offlineMessage) {
        Map<String, Object> map = publicPack(TypeConstant.MsgType.RECEIPT_MSG_READ, null, from, to, "粉丝已读消息", msgId);
        Map<String, List<String>> msgIdList = new HashMap<>(3);
        msgIdList.put("otherMsgIdList", otherMsgIdList);
        map.put("data", msgIdList);
        map.put("offlineMessage", offlineMessage);
        push(null, username, map, msgId, msgTime);
    }

    /**
     * 群通知消息
     */
    public void groupNotifyMsg(String from, String to, GroupNotifyResult groupNotifyResult, String username, long msgTime, boolean offlineMessage) {
        Map<String, Object> map = publicPack(TypeConstant.MsgType.GROUP_NOTIFY_MSG, null, from, to, null, IdUtil.simpleUUID().toUpperCase());
        map.put("data", groupNotifyResult);
        map.put("offlineMessage", offlineMessage);
        push(null, username, map, null, msgTime);
    }

    /**
     * 收到voip呼叫
     */
    public void voipCallReceived(String from, String to, String msgId, String username, long msgTime, boolean offlineMessage, VoipAcceptResult voipAcceptResult) {
        Map<String, Object> map = publicPack(TypeConstant.MsgType.VOIP_CALL_RECEIVED, null, from, to, "收到voip邀请", msgId);
        map.put("offlineMessage", offlineMessage);
        map.put("voipAcceptResult", voipAcceptResult);
        push(null, username, map, msgId, msgTime);
    }

    /**
     * 主动创建voip呼叫
     */
    public void voipCallCreate(String from, String to, String msgId, String username, long msgTime, boolean offlineMessage, VoipAcceptResult voipAcceptResult) {
        Map<String, Object> map = publicPack(TypeConstant.MsgType.VOIP_CALL_CREATE, null, from, to, "创建voip邀请", msgId);
        map.put("offlineMessage", offlineMessage);
        map.put("voipAcceptResult", voipAcceptResult);
        push(null, username, map, msgId, msgTime);
    }

    /**
     * 收到voip已连接
     */
    public void voipCallConnected(String from, String to, String msgId, String username, long msgTime, boolean offlineMessage) {
        Map<String, Object> map = publicPack(TypeConstant.MsgType.VOIP_CALL_CONNECTED, null, from, to, "voip已连接", msgId);
        map.put("offlineMessage", offlineMessage);
        push(null, username, map, msgId, msgTime);
    }

    /**
     * 联系人隐私令牌
     */
    public void contactsPrivacyToken(String username, TcToken tcToken) {
        Map<String, Object> map = new HashMap<>();
        map.put("type", TypeConstant.MsgType.CONTACTS_PRIVACY_TOKEN);
        map.put("tcToken", tcToken);
        push(username, map);
    }

    /**
     * 推送消息
     *
     * @param groupId  群id
     * @param username 当前账号
     * @param map      对象
     */
    private void push(String groupId, String username, Map<String, Object> map, String msgId, long msgTime) {
        if (StringUtils.hasLength(msgId) && msgId.length() >= 3) {
            if (WhatsAppUtils.isSystemMessage(msgId)) {
                log.info("用户：{}，小号标识消息不转发，回复内容：{}", username, Result.callback(TypeConstant.NotifyType.ASYNC_MESSAGE, UUID.randomUUID().toString(), username, map, msgTime).toJson());
                return;
            }
        }
        if (StringUtils.isEmpty(groupId)) {
            String mchId = getMchId(username);
            WebSocketClient.sendMsg(username, Result.callback(TypeConstant.NotifyType.ASYNC_MESSAGE, UUID.randomUUID().toString(), username, map, msgTime, mchId).toJson());
        } else {
            WebSocketClient.sendMsg(username, Result.callback(TypeConstant.NotifyType.GROUP_ASYNC_MESSAGE, UUID.randomUUID().toString(), username, map, msgTime).toJson());
        }
    }

    /**
     * 推送消息
     *
     * @param username 当前账号
     * @param map      对象
     */
    private void push(String username, Map<String, Object> map) {
        WebSocketClient.sendMsg(username, Result.callback(TypeConstant.NotifyType.ASYNC_MESSAGE, UUID.randomUUID().toString(), username, map).toJson());
    }

    private boolean isIosFeedMsgId(String msgId) {
        String end = msgId.substring(msgId.length() - 4);
        String prefix = msgId.substring(0, 2);
        return Constant.NT_MSG_TAG_FEED.equals(end) && Constant.IOS_MSG_PREFIX.equals(prefix);
    }

    /**
     * 获取商户id
     */
    private String getMchId(String username) {
        ConcurrentMap<String, User> record = UserRecord.getRecord();
        User user = record.get(username);
        if (user != null) {
            LoginPack loginPack = user.getLoginPack();
            if (loginPack != null) {
                return loginPack.getMchId();
            }
        }
        return null;
    }

    /**
     * 基本map
     */
    private Map<String, Object> publicPack(String type, String groupId, String from, String to, String content, String msgId) {
        Map<String, Object> map;
        if (StringUtils.isEmpty(groupId)) {
            map = new HashMap<>(8);
        } else {
            map = new HashMap<>(9);
            map.put("groupId", groupId);
        }
        map.put("from", from);
        map.put("to", to);
        map.put("content", content);
        map.put("type", type);
        map.put("msgId", msgId);
        return map;
    }
}
