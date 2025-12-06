package com.whatsapp.android.api.message;

import Message.WhatsMessage;
import ProtocolTree.ProtocolTreeNode;
import ProtocolTree.XmppEncode;
import Util.StringUtil;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.whatsapp.android.GorgeousEngine;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.pack.message.BatchSendMessagePack;
import com.whatsapp.android.entity.response.message.BatchSendMessageResult;
import com.whatsapp.android.entity.response.message.MessageResult;
import com.whatsapp.android.entity.response.message.SendMessageResult;
import com.whatsapp.android.request.AbstractRequest;
import com.whatsapp.android.util.KeyLockUtil;
import com.whatsapp.android.util.TaskNotify;
import com.whatsapp.android.util.WhatsAppUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.ecc.Curve;
import org.whispersystems.libsignal.ecc.ECKeyPair;
import org.whispersystems.libsignal.protocol.CiphertextMessage;
import org.whispersystems.libsignal.state.PreKeyBundle;
import org.whispersystems.libsignal.util.KeyHelper;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 批量发送消息
 */

@Slf4j
public class BatchSendFakeSessionMessageRequest extends AbstractRequest<BatchSendMessageResult> {
    private final BatchSendMessagePack batchSendMessagePack;
    private String username;
    private GorgeousEngine gorgeousEngine;

    public BatchSendFakeSessionMessageRequest(BatchSendMessagePack batchSendMessagePack) {
        this.batchSendMessagePack = batchSendMessagePack;
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.BATCH_SEND_FAKE_SESSION_MESSAGE;
    }

    @Override
    public boolean request() {
        return true;
    }

    @Override
    public long timeOut() {
        return 60L;
    }

    @Override
    public BatchSendMessageResult execute() {
        username = user.getLoginPack().getUsername();
        gorgeousEngine = user.getGorgeousEngine();
        List<String> userIds = batchSendMessagePack.getUserIds();
        //发送输入中
        sendChatComposing(userIds);
        ThreadUtil.sleep(1000);
        //发送输入结束
        sendChatPaused(userIds);
        //写入一次性密钥
        List<String> successList = getPreKey(userIds);
        if (successList.isEmpty()) {
            return BatchSendMessageResult.fail(userIds, "失败");
        }
        List<String> failList = findMissingElements(userIds, successList);
        //组装消息
        List<XmppTask> xmppTasks = generateMsgPack(successList, batchSendMessagePack.getContent());
        if (xmppTasks.isEmpty()) {
            return BatchSendMessageResult.fail(userIds, Constant.ExceptionReason.FANS_OFFLINE_TOO_LONG);
        }
        Map<String, MessageResult> map = new HashMap<>();
        for (String id : failList) {
            map.put(id, new MessageResult(null, false, Constant.ExceptionReason.FANS_OFFLINE_TOO_LONG));
        }
        List<String> sendSuccessList = new ArrayList<>();
        //批量发送消息
        TaskNotify taskNotify = user.getTaskNotify();
        CountDownLatch countDownLatch = new CountDownLatch(xmppTasks.size());
        for (XmppTask xmppTask : xmppTasks) {
            String userId = xmppTask.getUserId();
            String eventId = xmppTask.getTaskId();
            taskNotify.createEvent(eventId);
            taskNotify.addConsumer(eventId, node -> {
                SendMessageResult sendMessageResult = parseSendMessageResult(node);
                if (Constant.OK.equals(sendMessageResult.getStatus())) {
                    sendSuccessList.add(userId);
                    map.put(userId, new MessageResult(sendMessageResult.getMsgId(), true, "成功"));
                } else {
                    map.put(userId, new MessageResult(sendMessageResult.getMsgId(), false, "失败"));
                }
                countDownLatch.countDown();
            });
            try {
                gorgeousEngine.getNoiseHandshake_().sendXmpp(xmppTask.getNode(), xmppTask.getData(), success -> {
                    if (!success) {
                        taskNotify.setEventContent(eventId, ProtocolTreeNode.fail(Constant.FAIL));
                    }
                });
            } catch (Exception ignored) {
                taskNotify.setEventContent(eventId, ProtocolTreeNode.fail(Constant.FAIL));
            }
        }
        //等待回执通知
        try {
            countDownLatch.await(timeOut(), TimeUnit.SECONDS);
            //计算哪些没写入成功的，都标记为失败
            List<String> collect = new ArrayList<>(map.keySet());
            failList = findMissingElements(userIds, collect);
            for (String id : failList) {
                map.put(id, new MessageResult(null, false, "失败"));
            }
        } catch (Exception ignored) {

        } finally {
            for (XmppTask xmppTask : xmppTasks) {
                taskNotify.closeEvent(xmppTask.getTaskId());
            }
        }
        //信任联系人
        for (String jid : sendSuccessList) {
            gorgeousEngine.trustedContact(jid, gorgeousEngine.GenerateIqId());
        }
        BatchSendMessageResult batchSendMessageResult = new BatchSendMessageResult();
        batchSendMessageResult.setResult(map);
        return batchSendMessageResult;
    }

    @Override
    public BatchSendMessageResult parseResult(ProtocolTreeNode node) {
        return null;
    }

    @Override
    public String getTaskId() {
        String userId = user.getLoginPack().getUserId();
        return WhatsAppUtils.generateMsgId(userId, user.getLoginPack().isIosLogin());
    }

    /**
     * 上传通讯录
     */
    private boolean uploadContact(List<String> phones) {
        List<String> list = phones.stream().map(phone -> StrUtil.replace(phone, "@s.whatsapp.net", "")).map(s -> {
            char firstChar = s.charAt(0);
            if (firstChar != '+') {
                return "+" + s;
            }
            return s;
        }).collect(Collectors.toList());
        GorgeousEngine gorgeousEngine = user.getGorgeousEngine();
        if (ObjectUtil.isNotNull(gorgeousEngine)) {
            gorgeousEngine.SyncContact(list, gorgeousEngine.GenerateIqId());
            return true;
        }
        return false;
    }

    /**
     * 发送输入中
     */
    private void sendChatComposing(List<String> phones) {
        for (String phone : phones) {
            if (ObjectUtil.isNotNull(gorgeousEngine)) {
                gorgeousEngine.sendChatComposing(phone);
            }
        }
    }

    /**
     * 发送输入结束
     */
    private void sendChatPaused(List<String> phones) {
        for (String phone : phones) {
            if (ObjectUtil.isNotNull(gorgeousEngine)) {
                gorgeousEngine.sendChatPaused(phone);
            }
        }
    }

    private List<String> getPreKey(List<String> phones) {
        ArrayList<String> userIds = new ArrayList<>();
        KeyLockUtil.lock(username);
        List<String> existList = null;
        try {
            existList = gorgeousEngine.axolotlManager_.batchQuerySession(phones);
        } catch (Exception ignored) {
        } finally {
            KeyLockUtil.unlock(username);
        }
        List<String> missingElements;
        if (existList != null && !existList.isEmpty()) {
            missingElements = findMissingElements(phones, existList);
            if (missingElements.isEmpty()) {
                return existList;
            }
        } else {
            missingElements = phones;
        }
        for (String missingElement : missingElements) {
            StringUtil.JidInfo jidInfo;
            PreKeyBundle preKeyBundle;
            try {
                jidInfo = StringUtil.ParseJid(missingElement);
                ECKeyPair identityKeyPair = Curve.generateKeyPair();
                ECKeyPair signKeyPair = Curve.generateKeyPair();
                ECKeyPair preKeyPair = Curve.generateKeyPair();
                byte[] sign = Curve.calculateSignature(identityKeyPair.getPrivateKey(), signKeyPair.getPublicKey().serialize());
                preKeyBundle = new PreKeyBundle(KeyHelper.generateRegistrationId(true), jidInfo.deviceId, gorgeousEngine.DeAdjustId(Base64.decode("AAMH"), 3), preKeyPair.getPublicKey(),gorgeousEngine.DeAdjustId(Base64.decode("lMVV"), 3), signKeyPair.getPublicKey(), sign, new IdentityKey(identityKeyPair.getPublicKey()));
            } catch (Exception e) {
                log.error("用户:{}, 使用黑科技发送生成粉丝: {} session失败", username, missingElement, e);
                continue;
            }
            try {
                KeyLockUtil.lock(username);
                gorgeousEngine.axolotlManager_.CreateSession(jidInfo.recipientId, jidInfo.deviceId, preKeyBundle);
                userIds.add(jidInfo.recipientId);
            } catch (Exception ignore) {
                log.error("用户:{}, 使用黑科技发送保存粉丝: {} session失败", username, missingElement, ignore);
            } finally {
                KeyLockUtil.unlock(username);
            }
        }
        if (existList != null && !existList.isEmpty()){
            userIds.addAll(existList);
        }
        return userIds;
    }

    private List<XmppTask> generateMsgPack(List<String> userIds, String content) {
        List<XmppTask> xmppTasks = new ArrayList<>();
        for (String phone : userIds) {
            WhatsMessage.WhatsAppMessage.Builder protocolMessageBuild = WhatsMessage.WhatsAppMessage.newBuilder();
            int expirationTime = user.getGorgeousEngine().getEphemeralMessageTime(phone);
            if (expirationTime > 0) {
                //新方式
                WhatsMessage.WhatsAppExtendedTextMessage.Builder builder = WhatsMessage.WhatsAppExtendedTextMessage.newBuilder();
                builder.setText(content);
                builder.setPreviewType(WhatsMessage.WhatsAppPreviewType.PreviewType_NONE);
                WhatsMessage.WhatsAppContextInfo.Builder contextInfoBuilder = WhatsMessage.WhatsAppContextInfo.newBuilder();
                WhatsAppUtils.applyEphemeralMessage(contextInfoBuilder, expirationTime);
                builder.setContextInfo(contextInfoBuilder);
                protocolMessageBuild.setExtendedTextMessage(builder);
            } else {
                protocolMessageBuild.setConversation(content);
            }
            byte[] message = protocolMessageBuild.build().toByteArray();
            ArrayList<CiphertextMessage> cipherText = new ArrayList<>();
            ArrayList<StringUtil.JidInfo> participant = new ArrayList<>();
            try {
                KeyLockUtil.lock(username);
                StringUtil.JidInfo jidInfo = StringUtil.ParseJid(phone);
                List<Integer> deviceIds = gorgeousEngine.axolotlManager_.GetSubDeviceSessions(jidInfo.recipientId);
                for (Integer id : deviceIds) {
                    StringUtil.JidInfo part = jidInfo.Copy();
                    part.deviceId = id;
                    cipherText.add(gorgeousEngine.axolotlManager_.fakeSessionEncrypt(part, message));
                    participant.add(part);
                }
            } catch (Throwable ignored) {
            } finally {
                KeyLockUtil.unlock(username);
            }
            String jid = WhatsAppUtils.JidNormalize(phone);
            String eventId = getTaskId();
            gorgeousEngine.SaveFakeSessionMessage(phone, message, "text", "", eventId);
            ProtocolTreeNode node = gorgeousEngine.generateEncMessage(jid, participant, cipherText, "text", "", eventId);
            byte[] data;
            try {
                data = XmppEncode.encode(node);
            } catch (Exception e) {
                log.error("用户: {}, xmpp编码异常: {}", username, node, e);
                continue;
            }
            if (data.length == 0) {
                continue;
            }
            xmppTasks.add(new XmppTask(phone, eventId, node, data));
        }
        return xmppTasks;
    }

    private List<String> findMissingElements(List<String> list1, List<String> list2) {
        HashSet<String> set2 = new HashSet<>(list2);
        List<String> result = new ArrayList<>();
        for (String item : list1) {
            if (!set2.contains(item)) {
                result.add(item);
            }
        }
        return result;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class XmppTask {
        /**
         * 粉丝
         */
        private String userId;
        /**
         * 任务id
         */
        private String taskId;
        /**
         * 节点
         */
        private ProtocolTreeNode node;
        /**
         * 发送xmpp数据
         */
        private byte[] data;
    }
}
