package com.whatsapp.android.service;

import Message.WhatsMessage;
import ProtocolTree.ProtocolTreeNode;
import ProtocolTree.XmppJid;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;
import com.whatsapp.android.GorgeousEngine;
import com.whatsapp.android.config.ThreadPoolConfig;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.entity.JidMap;
import com.whatsapp.android.entity.response.goup.GroupMember;
import com.whatsapp.android.entity.response.goup.GroupNotifyResult;
import com.whatsapp.android.util.KeyLockUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.whatsapp.android.util.WhatsAppUtils.generateMediaQuoteMessage;

/**
 * @author sunnoc
 * @date 2021-03-15 14:16
 */
@Slf4j
public class AsyncMessageService {
    public static final AsyncMessagePushService asyncMessagePushService = new AsyncMessagePushService();

    public static void parse(WhatsMessage.WhatsAppMessage msg, String groupId, String from, String to, String msgId, String msgType, String mediaType, String username, long msgTime, boolean offlineMessage, boolean urlNumber) {
        //mediaType : "image" "ptt" "video" "document"
        if ("text".equals(msgType)) {
            String content = msg.getConversation();
            if (StringUtils.hasLength(content)) {
                asyncMessagePushService.textMsg(groupId, from, to, content, msgId, username, msgTime, offlineMessage, urlNumber);
            } else {
                String extContent = msg.getExtendedTextMessage().getText();
                if (StringUtils.hasLength(extContent)) {
                    asyncMessagePushService.textMsg(groupId, from, to, extContent, msgId, username, msgTime, offlineMessage, urlNumber);
                } else {
                    content = msg.getTimeLimitMessage().getContent().getExtendedTextMessage().getText();
                    if (StringUtils.hasLength(content)) {
                        asyncMessagePushService.textMsg(groupId, from, to, content, msgId, username, msgTime, offlineMessage, urlNumber);
                    } else {
                        content = msg.getUnknow25().getFacebook().getContent();
                        if (StringUtils.hasLength(content)) {
                            asyncMessagePushService.textMsg(groupId, from, to, content, msgId, username, msgTime, offlineMessage, urlNumber);
                        }
                    }
                }
            }
        } else if ("url".equals(mediaType)) {
            // url链接消息只往聊天室推链接即可
            String content = msg.getExtendedTextMessage().getText();
            if (StringUtils.hasLength(content)) {
                asyncMessagePushService.textMsg(groupId, from, to, content, msgId, username, msgTime, offlineMessage, urlNumber);
            }
        } else if ("image".equals(mediaType)) {
            WhatsMessage.WhatsAppImageMessage imageMessage = msg.getImageMessage();
            String url = imageMessage.getUrl();
            if (StringUtils.isEmpty(url)) {
                WhatsMessage.WhatsAppTimeLimitMessage timeLimitMessage = msg.getTimeLimitMessage();
                imageMessage = timeLimitMessage.getContent().getImageMessage();
                url = imageMessage.getUrl();
            }
            ByteString mediaKey = imageMessage.getMediaKey();
            if (mediaKey.isEmpty()) {
                //不处理没有mediaKey的加密消息
                return;
            }
            String caption = imageMessage.getCaption();
            String downloadUrl = generateDownloadUrl(username, url, mediaKey, mediaType, null, msg);
            asyncMessagePushService.imageMsg(groupId, from, to, imageMessage.getHeight(), imageMessage.getWidth(), downloadUrl, caption, msgId, username, msgTime, offlineMessage, urlNumber);
        } else if ("ptt".equals(mediaType) || "audio".equals(mediaType)) {
            WhatsMessage.WhatsAppAudioMessage audioMessage = msg.getAudioMessage();
            String url = audioMessage.getUrl();
            if (StringUtils.isEmpty(url)) {
                WhatsMessage.WhatsAppTimeLimitMessage timeLimitMessage = msg.getTimeLimitMessage();
                audioMessage = timeLimitMessage.getContent().getAudioMessage();
                url = audioMessage.getUrl();
            }
            int seconds = audioMessage.getSeconds();
            ByteString mediaKey = audioMessage.getMediaKey();
            if (mediaKey.isEmpty()) {
                //不处理没有mediaKey的加密消息
                return;
            }
            boolean ptt = audioMessage.getPtt();
            String downloadUrl = generateDownloadUrl(username, url, mediaKey, mediaType, null, msg);
            asyncMessagePushService.voiceMsg(groupId, from, to, downloadUrl, seconds, ptt, msgId, username, msgTime, offlineMessage, urlNumber);
        } else if ("video".equals(mediaType)) {
            WhatsMessage.WhatsAppVideoMessage videoMessage = msg.getVideoMessage();
            int seconds = videoMessage.getSeconds();
            String url = videoMessage.getUrl();
            if (StringUtils.isEmpty(url)) {
                WhatsMessage.WhatsAppTimeLimitMessage timeLimitMessage = msg.getTimeLimitMessage();
                videoMessage = timeLimitMessage.getContent().getVideoMessage();
                url = videoMessage.getUrl();
            }
            ByteString mediaKey = videoMessage.getMediaKey();
            if (mediaKey.isEmpty()) {
                //不处理没有mediaKey的加密消息
                return;
            }
            String caption = videoMessage.getCaption();
            String downloadUrl = generateDownloadUrl(username, url, mediaKey, mediaType, null, msg);
            asyncMessagePushService.videoMsg(groupId, from, to, downloadUrl, seconds, caption, msgId, username, msgTime, offlineMessage, urlNumber);
        } else if ("document".equals(mediaType)) {
            WhatsMessage.WhatsAppDocumentMessage documentMessage = null;
            if (msg.hasDocumentMessage()) {
                documentMessage = msg.getDocumentMessage();
            } else if (msg.hasTimeLimitMessage()) {
                WhatsMessage.WhatsAppTimeLimitMessage timeLimitMessage = msg.getTimeLimitMessage();
                documentMessage = timeLimitMessage.getContent().getDocumentMessage();
            } else if (msg.hasDocumentWithCaptionMessage()) {
                WhatsMessage.FutureProofMessage documentWithCaptionMessage = msg.getDocumentWithCaptionMessage();
                documentMessage = documentWithCaptionMessage.getMessage().getDocumentMessage();
            }
            if (ObjectUtil.isNull(documentMessage)) {
                return;
            }
            ByteString mediaKey = documentMessage.getMediaKey();
            if (mediaKey.isEmpty()) {
                //不处理没有mediaKey的加密消息
                return;
            }
            String url = documentMessage.getUrl();
            String fileName = documentMessage.getFileName();
            String caption = documentMessage.getCaption();
            String downloadUrl = generateDownloadUrl(username, url, mediaKey, mediaType, fileName, msg);
            asyncMessagePushService.fileMsg(groupId, from, to, downloadUrl, fileName, caption, msgId, username, msgTime, offlineMessage, urlNumber);
        } else if ("location".equals(mediaType)) {
            WhatsMessage.WhatsAppLocationMessage locationMessage = msg.getLocationMessage();
            if (StringUtils.isEmpty(locationMessage.toString())) {
                WhatsMessage.WhatsAppTimeLimitMessage timeLimitMessage = msg.getTimeLimitMessage();
                locationMessage = timeLimitMessage.getContent().getLocationMessage();
            }
            String latitude = String.valueOf(locationMessage.getDegreesLatitude());
            String longitude = String.valueOf(locationMessage.getDegreesLongitude());
            asyncMessagePushService.locationMsg(groupId, from, to, latitude, longitude, msgId, username, msgTime, offlineMessage, urlNumber);
        } else if ("contact".equals(mediaType) || "vcard".equals(mediaType)) {
            WhatsMessage.WhatsAppContactMessage contactMessage = msg.getContactMessage();
            if (StringUtils.isEmpty(contactMessage.toString())) {
                WhatsMessage.WhatsAppTimeLimitMessage timeLimitMessage = msg.getTimeLimitMessage();
                contactMessage = timeLimitMessage.getContent().getContactMessage();
            }
            asyncMessagePushService.contactCardMsg(groupId, from, to, contactMessage.getDisplayName(), contactMessage.getVcard(), msgId, username, msgTime, offlineMessage, urlNumber);
        } else if ("offer".equals(msgType)) {
            asyncMessagePushService.voiceCallInvite(from, to, msgId, username, msgTime, offlineMessage, urlNumber);
        } else if ("terminate".equals(msgType)) {
            asyncMessagePushService.voiceCallEnd(from, to, msgId, username, msgTime, offlineMessage, urlNumber);
        } else if ("reject".equals(msgType)) {
            asyncMessagePushService.voiceCallReject(from, to, msgId, username, msgTime, offlineMessage, urlNumber);
        } else if ("gif".equals(mediaType)) {
            WhatsMessage.WhatsAppVideoMessage videoMessage = msg.getVideoMessage();
            String url = videoMessage.getUrl();
            if (StringUtils.isEmpty(url)) {
                WhatsMessage.WhatsAppTimeLimitMessage timeLimitMessage = msg.getTimeLimitMessage();
                videoMessage = timeLimitMessage.getContent().getVideoMessage();
                url = videoMessage.getUrl();
            }
            ByteString mediaKey = videoMessage.getMediaKey();
            if (mediaKey.isEmpty()) {
                //不处理没有mediaKey的加密消息
                return;
            }
            String caption = videoMessage.getCaption();
            String downloadUrl = generateDownloadUrl(username, url, mediaKey, "gif", null, msg);
            asyncMessagePushService.gifMsg(groupId, from, to, downloadUrl, caption, msgId, username, msgTime, offlineMessage, urlNumber);
        } else if ("sticker".equals(mediaType) || "1p_sticker".equals(mediaType) || "avatar_sticker".equals(mediaType)) {
            if (msg.hasWebMessage()) {
                WhatsMessage.WhatsAppWebMessage stickerMessage = msg.getWebMessage();
                String url = stickerMessage.getUrl();
                if (StringUtils.isEmpty(url)) {
                    WhatsMessage.WhatsAppTimeLimitMessage timeLimitMessage = msg.getTimeLimitMessage();
                    stickerMessage = timeLimitMessage.getContent().getWebMessage();
                    url = stickerMessage.getUrl();
                }
                ByteString mediaKey = stickerMessage.getMediaKey();
                String downloadUrl = generateDownloadUrl(username, url, mediaKey, "sticker", null, msg);
                asyncMessagePushService.stickerMsg(groupId, from, to, downloadUrl, msgId, username, msgTime, offlineMessage, urlNumber);
            } else if (msg.hasLottieStickerMessage()) {
                // 处理lottie贴图
                WhatsMessage.WhatsAppWebMessage stickerMessage = msg.getLottieStickerMessage().getMessage().getWebMessage();
                String url = stickerMessage.getUrl();
                if (StringUtils.isEmpty(url)) {
                    WhatsMessage.WhatsAppTimeLimitMessage timeLimitMessage = msg.getTimeLimitMessage();
                    stickerMessage = timeLimitMessage.getContent().getWebMessage();
                    url = stickerMessage.getUrl();
                }
                ByteString mediaKey = stickerMessage.getMediaKey();
                String downloadUrl = generateDownloadUrl(username, url, mediaKey, "lottieSticker", null, msg);
                asyncMessagePushService.LottieStickerMsg(groupId, from, to, downloadUrl, msgId, username, msgTime, offlineMessage, urlNumber);
            } else {
                log.error("用户: {}, 接收粉丝: {} 发送未知贴图消息: {}", username, from, msg);
            }
        } else if ("contact_array".equals(mediaType)) {
            WhatsMessage.WhatsAppContactsArrayMessage contactsArrayMessage = msg.getContactsArrayMessage();
            List<WhatsMessage.WhatsAppContactMessage> contactsList = contactsArrayMessage.getContactsList();
            asyncMessagePushService.contactCardArrayMsg(groupId, from, to, contactsList, msgId, username, msgTime, offlineMessage, urlNumber);
        } else if ("reaction".equals(msgType)) {
            WhatsMessage.ReactionMessage reactionMessage = msg.getReactionMessage();
            String content = reactionMessage.getText();
            if (StrUtil.isNotEmpty(content)) {
                // 心情消息只要转发消息内容都聊天即可
                asyncMessagePushService.textMsg(groupId, from, to, content, msgId, username, msgTime, offlineMessage, urlNumber);
            }
        }

    }

    public static void parse(String from, String to, String msgId, List<String> otherMsgIdList, String username, long msgTime, boolean offlineMessage) {
        asyncMessagePushService.receiptMsgRead(from, to, msgId, otherMsgIdList, username, msgTime, offlineMessage);

    }

    /**
     * 解析群通知
     */
    public static void parseGroupNotify(String to, String username, ProtocolTreeNode treeNode, long msgTime, boolean offlineMessage, GorgeousEngine gorgeousEngine) {
        String from = treeNode.GetAttributeValue("from");
        String participant = treeNode.GetAttributeValue("participant");
        String participantPn = treeNode.GetAttributeValue("participant_pn");
        if (StrUtil.isNotEmpty(participantPn)) {
            participant = participantPn;
        }
        String notify = treeNode.GetAttributeValue("notify");
        ProtocolTreeNode oneChildrenNode = treeNode.getOneChildren();
        if (oneChildrenNode == null) {
            return;
        }
        List<GroupMember> groupMembers;
        GroupNotifyResult groupNotifyResult;
        String oneChildrenNodeTag = oneChildrenNode.GetTag();
        if ("create".equals(oneChildrenNodeTag)) {
            GroupNotifyResult.GroupNotifyResultBuilder builder = GroupNotifyResult.builder();
            ProtocolTreeNode groupNode = oneChildrenNode.getOneChildren("group");
            if (groupNode == null) {
                return;
            }
            // 判断是否为社群
            ProtocolTreeNode parentNode = groupNode.getOneChildren("parent");
            if (ObjectUtil.isNotNull(parentNode)) {
                // 暂时不返回社群通知到前端
                return;
            }
            boolean newGroup = "new".equals(oneChildrenNode.GetAttributeValue("type"));
            String subject = groupNode.GetAttributeValue("subject");
            groupMembers = generateGroupMember(groupNode);
            // 是否有已开启限时消息
            ProtocolTreeNode ephemeralNode = groupNode.getOneChildren("ephemeral");
            if (ObjectUtil.isNotNull(ephemeralNode)) {
                try {
                    KeyLockUtil.lock(username);
                    int expiration = Integer.parseInt(ephemeralNode.GetAttributeValue("expiration"));
                    gorgeousEngine.axolotlManager_.ephemeralMessageStore.insert(from, expiration);
                } catch (Exception ignored) {
                } finally {
                    KeyLockUtil.unlock(username);
                }
            }
            // 判断是否为公告群
            ProtocolTreeNode defaultSubGroup = groupNode.getOneChildren("default_sub_group");
            if (ObjectUtil.isNotNull(defaultSubGroup)) {
                builder.defaultSubGroup(true);
            }
            // 判断是否为子群
            ProtocolTreeNode linkedParent = groupNode.getOneChildren("linked_parent");
            if (ObjectUtil.isNotNull(linkedParent)) {
                builder.subGroup(true);
                builder.linkedParentGroupId(linkedParent.GetAttributeValue("jid"));
            }
            // 判断是否是所有人都可以邀请人员进群
            ProtocolTreeNode memberAddMode = groupNode.getOneChildren("member_add_mode");
            String add = new String(memberAddMode.GetData(), StandardCharsets.UTF_8);
            builder.allMemberAdd(add.equals("all_member_add"));
            // 判断新成员进群是否需要管理员进行审批
            ProtocolTreeNode membershipApprovalMode = groupNode.getOneChildren("membership_approval_mode");
            builder.memberAddApprovalOn(ObjectUtil.isNotNull(membershipApprovalMode));
            groupNotifyResult = builder.notifyType(oneChildrenNodeTag).groupId(from).participant(participant).notify(notify).newGroup(newGroup).subjectName(subject).members(groupMembers).build();
            // 首次进群保存JidLidMap
            saveGroupMembersJidMaps(gorgeousEngine, groupMembers);
        } else if ("remove".equals(oneChildrenNodeTag) || "promote".equals(oneChildrenNodeTag) || "demote".equals(oneChildrenNodeTag) ||
                "add".equals(oneChildrenNodeTag)) {
            String reason = oneChildrenNode.GetAttributeValue("reason");
            boolean inviteUrlJoinGroup = "invite".equals(reason);
            groupMembers = generateGroupMember(oneChildrenNode);
            if ("add".equals(oneChildrenNodeTag)) {
                //创建session
                List<String> userIds = groupMembers.stream().map(GroupMember::getUserId).collect(Collectors.toList());
                if (userIds.isEmpty()) {
                    return;
                }
                // 有新人加群也进行保存
                saveGroupMembersJidMaps(gorgeousEngine, groupMembers);
                gorgeousEngine.handleGroupInfo(from, userIds);
            } else if ("remove".equals(oneChildrenNodeTag)) {
                List<String> userIds = groupMembers.stream().map(GroupMember::getUserId).collect(Collectors.toList());
                try {
                    KeyLockUtil.lock(username);
                    gorgeousEngine.axolotlManager_.deletePreGroupFans(from, userIds);
                    // 删除对应的senderKey, 如果之前有发过消息
                    gorgeousEngine.axolotlManager_.deleteGroupSenderKey(from, userIds);
                } catch (Exception ignored) {
                } finally {
                    KeyLockUtil.unlock(username);
                }
            }
            groupNotifyResult = GroupNotifyResult.builder().notifyType(oneChildrenNodeTag).groupId(from).participant(participant).notify(notify).inviteUrlJoinGroup(inviteUrlJoinGroup).members(groupMembers).build();
        } else if ("invite".equals(oneChildrenNodeTag)) {
            //重置群链接：
            String code = oneChildrenNode.GetAttributeValue("code");
            if (StringUtils.isEmpty(code)) {
                return;
            }
            groupNotifyResult = GroupNotifyResult.builder().notifyType(oneChildrenNodeTag).groupId(from).participant(participant).notify(notify)
                    .inviteLink("https://chat.whatsapp.com/" + code).build();

        } else if ("subject".equals(oneChildrenNodeTag)) {
            //群名称更改通知：
            String subject = oneChildrenNode.GetAttributeValue("subject");
            if (StringUtils.isEmpty(subject)) {
                return;
            }
            groupNotifyResult = GroupNotifyResult.builder().notifyType(oneChildrenNodeTag).groupId(from).subjectName(subject).participant(participant).notify(notify).build();
        } else if ("not_announcement".equals(oneChildrenNodeTag)) {
            //允许所有群组成员发言
            groupNotifyResult = GroupNotifyResult.builder().notifyType("notAnnouncement").groupId(from).participant(participant).notify(notify).build();
        } else if ("announcement".equals(oneChildrenNodeTag) || "locked".equals(oneChildrenNodeTag) || "unlocked".equals(oneChildrenNodeTag) || "description".equals(oneChildrenNodeTag)) {
            groupNotifyResult = GroupNotifyResult.builder().notifyType(oneChildrenNodeTag).groupId(from).participant(participant).notify(notify).build();
        } else if ("member_add_mode".equals(oneChildrenNodeTag)) {
            // 添加新成员的规则
            String addMode = new String(oneChildrenNode.GetData(), StandardCharsets.UTF_8);
            groupNotifyResult = GroupNotifyResult.builder().notifyType(addMode).groupId(from).participant(participant).notify(notify).build();
        } else if ("membership_approval_mode".equals(oneChildrenNodeTag)) {
            // 新成员进群是否需要管理员审批
            ProtocolTreeNode groupJoin = oneChildrenNode.getOneChildren("group_join");
            String state = groupJoin.GetAttributeValue("state");
            if (state.equals("on")) {
                groupNotifyResult = GroupNotifyResult.builder().notifyType("membershipApprovalModeOn").groupId(from).participant(participant).notify(notify).build();
            } else {
                groupNotifyResult = GroupNotifyResult.builder().notifyType("membershipApprovalModeOff").groupId(from).participant(participant).notify(notify).build();
            }
        } else if ("suspended".equals(oneChildrenNodeTag)) {
            // 群封禁通知
            groupNotifyResult = GroupNotifyResult.builder().notifyType("suspended").suspended(true).groupId(from).build();
        } else if ("created_membership_requests".equals(oneChildrenNodeTag)) {
            // 成员进群请求
            String requestMethod = oneChildrenNode.GetAttributeValue("request_method");
            if (!StrUtil.isNotEmpty(requestMethod)) {
                log.error("用户: {}, 接受到未知审批入群通知, node: {}", username, treeNode);
                return;
            }
            if (requestMethod.equals("invite_link")) {
                // 通过邀请链接进群, 一般是单用户
                groupNotifyResult = GroupNotifyResult.builder().notifyType("createdMembershipRequests").groupId(from).participant(participant).notify(notify).build();
            } else if (requestMethod.equals("non_admin_add")) {
                // 非管理员邀请用户进群
                LinkedList<ProtocolTreeNode> requestedUser = oneChildrenNode.GetChildren("requested_user");
                List<String> requestsUser = new ArrayList<>();
                for (ProtocolTreeNode protocolTreeNode : requestedUser) {
                    requestsUser.add(protocolTreeNode.GetAttributeValue("jid"));
                }
                groupNotifyResult = GroupNotifyResult.builder().notifyType("created_membership_requests").groupId(from).participant(participant).requestedUser(requestsUser).notify(notify).build();
            } else {
                log.error("用户: {}, 接受到未知invite_link类型通知: {}, node: {}", username, requestMethod, treeNode);
                return;
            }
        } else if ("revoked_membership_requests".equals(oneChildrenNodeTag)) {
            LinkedList<ProtocolTreeNode> revokeList = oneChildrenNode.GetChildren("participant");
            for (ProtocolTreeNode protocolTreeNode : revokeList) {
                String jid = protocolTreeNode.GetAttributeValue("jid");
                String t = treeNode.GetAttributeValue("t");
                String datetime = null;
                if (StrUtil.isNotEmpty(t)) {
                    datetime = DateUtil.format(DateUtil.date(Convert.toLong(t, 0L) * 1000), DatePattern.NORM_DATETIME_PATTERN);
                }
                log.info("用户: {}, 接收审批群消息: 群id: {}, 时间: {}, 管理员: {}, 拒绝成员: {} 进群", username, from, datetime, participant, jid);
            }
            return;
        } else {
            if ("ephemeral".equals(oneChildrenNodeTag)) {
                String expiration = oneChildrenNode.GetAttributeValue("expiration");
                if (StringUtils.hasLength(expiration)) {
                    int expiredTime = Convert.toInt(expiration, 0);
                    try {
                        KeyLockUtil.lock(username);
                        gorgeousEngine.axolotlManager_.ephemeralMessageStore.insert(from, expiredTime);
                    } catch (Exception ignored) {
                    } finally {
                        KeyLockUtil.unlock(username);
                    }
                    return;
                }
            } else {
                log.info("用户: {}, 接收到未处理群组通知: {}", username, treeNode);
            }
            return;
        }
        ThreadPoolConfig.threadPoolExecutor.execute(() -> {
            asyncMessagePushService.groupNotifyMsg(from, to, groupNotifyResult, username, msgTime, offlineMessage);
        });
    }

    /**
     * 解析群头像修改通知
     */
    public static void parseGroupSetPictureNotify(String to, String username, ProtocolTreeNode treeNode, long msgTime, boolean offlineMessage) {
        String from = treeNode.GetAttributeValue("from");
        if (StrUtil.indexOf(from, "@s.whatsapp.net", 0, false) != -1) {
            //个人通知
            return;
        }
        String notify = treeNode.GetAttributeValue("notify");
        ProtocolTreeNode oneChildrenNode = treeNode.getOneChildren();
        if (oneChildrenNode == null) {
            return;
        }
        String addressingMode = treeNode.GetAttributeValue("addressing_mode");
        String participant;
        if (StrUtil.isNotEmpty(addressingMode) && addressingMode.equals("lid")) {
            participant = oneChildrenNode.GetAttributeValue("author_phone_number");
        } else {
            participant = oneChildrenNode.GetAttributeValue("author");
        }
        GroupNotifyResult groupNotifyResult = GroupNotifyResult.builder().notifyType("setPicture").groupId(from)
                .participant(participant).notify(notify).build();
        asyncMessagePushService.groupNotifyMsg(from, to, groupNotifyResult, username, msgTime, offlineMessage);
    }

    /**
     * 生成媒体资源下载链接
     *
     * @param username  用户
     * @param url       whatsApp 加密下载链接
     * @param mediaKey  媒体key
     * @param mediaType 媒体类型
     * @param msg       完整消息
     * @return 下载链接
     */
    private static String generateDownloadUrl(String username, String url, ByteString mediaKey, String mediaType, String fileName, WhatsMessage.WhatsAppMessage msg) {
        byte[] bytes = mediaKey.toByteArray();
        String mediaKeyEncode = Base64.encode(bytes);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("url", url);
        jsonObject.put("mediaKey", mediaKeyEncode);
        jsonObject.put("mediaType", mediaType);
        jsonObject.put("fileName", fileName);
        jsonObject.put("originalProto", generateMediaQuoteMessage(msg));
        if (StringUtils.isEmpty(mediaKeyEncode) || StringUtils.isEmpty(url)) {
            log.warn("用户：{}，资源消息未知情况：{}", username, HexUtil.encodeHexStr(msg.toByteArray()));
            log.warn("用户：{},资源消息存在未知情况：{}", username, msg.toString());
        }
        String encode = Base64.encode(jsonObject.toJSONString());
        return Constant.CDN_DOWNLOAD_ADDR + "/whatsapp/cdn/?downloadKey=" + encode;
    }

    private static List<GroupMember> generateGroupMember(ProtocolTreeNode oneChildrenNode) {
        List<GroupMember> groupMembers = new ArrayList<>();
        LinkedList<ProtocolTreeNode> participantChildrenNode = oneChildrenNode.GetChildren("participant");
        if (participantChildrenNode != null) {
            for (ProtocolTreeNode protocolTreeNode : participantChildrenNode) {
                int promoteType = 0;
                String userLid = null;
                String userId = protocolTreeNode.GetAttributeValue("jid");
                if (XmppJid.isIncognitoJid(userId)) {
                    String phoneNumber = protocolTreeNode.GetAttributeValue("phone_number");
                    if (StrUtil.isNotEmpty(phoneNumber)) {
                        userId = phoneNumber;
                        userLid = protocolTreeNode.GetAttributeValue("jid");
                    } else {
                        continue;
                    }
                }
                String authority = protocolTreeNode.GetAttributeValue("type");
                if (StringUtils.hasLength(authority)) {
                    if ("superadmin".equals(authority)) {
                        promoteType = 2;
                    } else if ("admin".equals(authority)) {
                        promoteType = 1;
                    }
                }
                if (StringUtils.hasLength(userId)) {
                    groupMembers.add(new GroupMember(promoteType, userId, userLid));
                }
            }
        }
        return groupMembers;
    }

    private static void saveGroupMembersJidMaps(GorgeousEngine gorgeousEngine, List<GroupMember> groupMembers) {
        try {
            List<JidMap> jidMaps = new ArrayList<>();
            for (GroupMember groupMember : groupMembers) {
                if (StrUtil.isNotEmpty(groupMember.getUserId()) && StrUtil.isNotEmpty(groupMember.getUserLid())) {
                    String jid = Objects.requireNonNull(XmppJid.of(groupMember.getUserId())).getUser();
                    String lid = Objects.requireNonNull(XmppJid.of(groupMember.getUserLid())).getUser();
                    jidMaps.add(new JidMap(jid, lid));
                }
            }
            if (!jidMaps.isEmpty()) {
                gorgeousEngine.saveNodeJidLid(jidMaps);
            }
        } catch (Exception e) {
            log.info("用户: {}, 存储群成员jidMap异常", gorgeousEngine.getUsername(), e);
        }
    }

    public static void main(String[] args) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("url", "https://mmg.whatsapp.net/v/t62.7119-24/40697622_1311872956340634_2875690863390781159_n.enc?ccb=11-4&oh=01_AdS7A6OoP6oUEam53qvl6QWiEL2ZSGou2tNNYxhs_fMSqA&oe=6471A5BE&mms3=true");
        jsonObject.put("mediaKey", "NdjFp6tAo3RCnWUHI8z/ganA9RQNVSKtq4WId00YV1Q=");
        jsonObject.put("mediaType", "document");
        jsonObject.put("fileName", "新建文本文档 (2).txt");

        String encode = Base64.encode(jsonObject.toJSONString());
        System.out.println(Constant.CDN_DOWNLOAD_ADDR + "/whatsapp/cdn/?downloadKey=" + encode);
    }
}
