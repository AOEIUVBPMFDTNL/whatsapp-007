package com.whatsapp.android.service.impl.init;

import Message.WhatsMessage;
import ProtocolTree.ProtocolTreeNode;
import ProtocolTree.StanzaAttribute;
import ProtocolTree.XmppJid;
import Util.CryptUtil;
import Util.StringUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.google.protobuf.ByteString;
import com.whatsapp.android.GorgeousEngine;
import com.whatsapp.android.entity.ProfileInfo;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.whispersystems.curve25519.Curve25519;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.ZoneId;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.whatsapp.android.util.WhatsAppUtils.JidNormalize;
import static org.whispersystems.curve25519.Curve25519.BEST;

@Slf4j
@UtilityClass
public class TaskMethod {
    private final Map<String, BiConsumer<GorgeousEngine, Consumer<Boolean>>> taskMap = new HashMap<>();
    /**
     * 安卓个人版公告ID
     */
    public static final List<String> androidPersonDisclosureStageId = Collections.unmodifiableList(Arrays.asList("20601217", "20601218", "20601216", "20610210", "20610101", "20610220", "20241005", "20601217", "20601218", "20601216", "20610210", "20610101", "20610220"));
    /**
     * 安卓商业版公告ID
     */
    public static final List<String> androidBusinessDisclosureStageId = Collections.unmodifiableList(Arrays.asList("20250228", "20610104", "20250206", "20241209", "20601227", "20250213", "20250310", "20610220", "20250915", "20601228", "20250402", "20601218", "20610210", "20241005"));
    /**
     * 商业版商业id
     */
    public static final List<String> businessCategoryId = Collections.unmodifiableList(Arrays.asList("629412378414563", "1223524174334504", "1086422341396773", "133436743388217", "139225689474222", "2250", "193705277324704", "1022050661163852", "150108431712141", "164243073639257", "145118935550090", "2603", "273819889375819", "200600219953504", "128232937246338", "644728732639272"));
    /**
     * 全世界时区大全
     */
    public static final List<String> worldTimeZone = new ArrayList<>(ZoneId.getAvailableZoneIds());

    static {
        // 初始化任务方法
        registerTaskMethods();
    }

    /**
     * 注册所有任务方法到任务映射表
     */
    private void registerTaskMethods() {
        taskMap.put("sendPresenceUnavailable", TaskMethod::sendPresenceUnavailable);
        taskMap.put("sendXmlnsW", TaskMethod::sendXmlnsW);
        taskMap.put("sendGetGroups", TaskMethod::sendGetGroups);
        taskMap.put("sendXmlnsWB", TaskMethod::sendXmlnsWB);
        taskMap.put("getInitPicture", TaskMethod::getInitPicture);
        taskMap.put("sendUpdateUserStatus", TaskMethod::sendUpdateUserStatus);
        taskMap.put("setIntStatus", TaskMethod::setIntStatus);
        taskMap.put("sendGet2Fa", TaskMethod::sendGet2Fa);
        taskMap.put("getStatusPrivacy", TaskMethod::getStatusPrivacy);
        taskMap.put("getPrivacy", TaskMethod::getPrivacy);
        taskMap.put("getDisappearingMode", TaskMethod::getDisappearingMode);
        taskMap.put("sendGetNotice", TaskMethod::sendGetNotice);
        taskMap.put("getAccept", TaskMethod::getAccept);
        //taskMap.put("sendGetGroups", TaskMethod::sendGetGroups);
        taskMap.put("getBlockList", TaskMethod::getBlockList);
        taskMap.put("removeAllCompanionDevice", TaskMethod::removeAllCompanionDevice);
        taskMap.put("sendCleanGroups", TaskMethod::sendCleanGroups);
        taskMap.put("sendCleanAccountSync", TaskMethod::sendCleanAccountSync);
        taskMap.put("sendNewsletterSubscribed", TaskMethod::sendNewsletterSubscribed);
        taskMap.put("initDigest", TaskMethod::initDigest);
        taskMap.put("sendGetInviteInfo", TaskMethod::sendGetInviteInfo);
        taskMap.put("accountIOSInitSync", TaskMethod::accountIOSInitSync);
        taskMap.put("getContactPicture", TaskMethod::getContactPicture);
        taskMap.put("sendPresenceName", TaskMethod::sendPresenceName);
        taskMap.put("sendGetInviteSenderInfo", TaskMethod::sendGetInviteSenderInfo);
        taskMap.put("sendGetRegistrationUpsells", TaskMethod::sendGetRegistrationUpsells);
        taskMap.put("sendGetEmail", TaskMethod::sendGetEmail);
        taskMap.put("sendRegistrationUpsellShown", TaskMethod::sendRegistrationUpsellShown);
        taskMap.put("sendPresenceAvailable", TaskMethod::sendPresenceAvailable);
        taskMap.put("sendGetSuggestedContacts", TaskMethod::sendGetSuggestedContacts);
        taskMap.put("sendOhaiKeyConfigQuery", TaskMethod::sendOhaiKeyConfigQuery);
        taskMap.put("sendGetGroupParticipating", TaskMethod::sendGetGroupParticipating);
        taskMap.put("accountAndroidInitSync", TaskMethod::accountAndroidInitSync);
        taskMap.put("sendGetUserDisclosures", TaskMethod::sendGetUserDisclosures);
        taskMap.put("sendGetPreRegAddRequests", TaskMethod::sendGetPreRegAddRequests);
        taskMap.put("sendNewsletterAddons", TaskMethod::sendNewsletterAddons);
        taskMap.put("sendAndroidTrackable20401220One", TaskMethod::sendAndroidTrackable20401220One);
        taskMap.put("sendAndroidTrackable20401220Hundred", TaskMethod::sendAndroidTrackable20401220Hundred);
        taskMap.put("sendAndroidTrackable20601216One", TaskMethod::sendAndroidTrackable20601216One);
        taskMap.put("sendAndroidTrackable20601216Hundred", TaskMethod::sendAndroidTrackable20601216Hundred);
        taskMap.put("sendAndroidTrackable20601217One", TaskMethod::sendAndroidTrackable20601217One);
        taskMap.put("sendAndroidTrackable20601217Hundred", TaskMethod::sendAndroidTrackable20601217Hundred);
        taskMap.put("sendAndroidTrackable20601218One", TaskMethod::sendAndroidTrackable20601218One);
        taskMap.put("sendAndroidTrackable20601218Hundred", TaskMethod::sendAndroidTrackable20601218Hundred);
        taskMap.put("sendAndroidTrackable20900727One", TaskMethod::sendAndroidTrackable20900727One);
        taskMap.put("sendAndroidTrackable20900727Hundred", TaskMethod::sendAndroidTrackable20900727Hundred);
        taskMap.put("sendAndroidTrackable20610203One", TaskMethod::sendAndroidTrackable20610203One);
        taskMap.put("sendAndroidTrackable20610203Hundred", TaskMethod::sendAndroidTrackable20610203Hundred);
        taskMap.put("sendAndroidTrackable20610204One", TaskMethod::sendAndroidTrackable20610204One);
        taskMap.put("sendAndroidTrackable20610204Hundred", TaskMethod::sendAndroidTrackable20610204Hundred);
        taskMap.put("sendAndroidTrackable20230901One", TaskMethod::sendAndroidTrackable20230901One);
        taskMap.put("sendAndroidTrackable20230901Hundred", TaskMethod::sendAndroidTrackable20230901Hundred);
        taskMap.put("sendAndroidTrackable20240216One", TaskMethod::sendAndroidTrackable20240216One);
        taskMap.put("sendAndroidTrackable20240216Hundred", TaskMethod::sendAndroidTrackable20240216Hundred);
        taskMap.put("sendAndroidTrackable20230902One", TaskMethod::sendAndroidTrackable20230902One);
        taskMap.put("sendAndroidTrackable20230902Hundred", TaskMethod::sendAndroidTrackable20230902Hundred);
        taskMap.put("sendAndroidTrackable20240729One", TaskMethod::sendAndroidTrackable20240729One);
        taskMap.put("sendAndroidTrackable20240729Hundred", TaskMethod::sendAndroidTrackable20240729Hundred);
        taskMap.put("sendAndroidTrackable20241016One", TaskMethod::sendAndroidTrackable20241016One);
        taskMap.put("sendAndroidTrackable20241016Hundred", TaskMethod::sendAndroidTrackable20241016Hundred);
        taskMap.put("sendAndroidTrackable20250304One", TaskMethod::sendAndroidTrackable20250304One);
        taskMap.put("sendAndroidTrackable20250304Hundred", TaskMethod::sendAndroidTrackable20250304Hundred);
        taskMap.put("sendAndroidTrackable20250501One", TaskMethod::sendAndroidTrackable20250501One);
        taskMap.put("sendAndroidTrackable20250501Hundred", TaskMethod::sendAndroidTrackable20250501Hundred);
        taskMap.put("sendAndroidTrackable20610101One", TaskMethod::sendAndroidTrackable20610101One);
        taskMap.put("sendAndroidTrackable20610101Hundred", TaskMethod::sendAndroidTrackable20610101Hundred);
        taskMap.put("sendAndroidTrackable20610210One", TaskMethod::sendAndroidTrackable20610210One);
        taskMap.put("sendAndroidTrackable20610210Hundred", TaskMethod::sendAndroidTrackable20610210Hundred);
        taskMap.put("sendAndroidTrackable20610220One", TaskMethod::sendAndroidTrackable20610220One);
        taskMap.put("sendAndroidTrackable20610220Hundred", TaskMethod::sendAndroidTrackable20610220Hundred);
        taskMap.put("sendPasskeyExistResponseQuery", TaskMethod::sendPasskeyExistResponseQuery);
        taskMap.put("sendQueryParticipatingGroups", TaskMethod::sendQueryParticipatingGroups);
        taskMap.put("sendGetDynamicRegistrationUpsells", TaskMethod::sendGetDynamicRegistrationUpsells);
        taskMap.put("sendContactsBackupQuery", TaskMethod::sendContactsBackupQuery);
        taskMap.put("sendUsyncQuery", TaskMethod::sendUsyncQuery);
        taskMap.put("sendSelfContactsQuery", TaskMethod::sendSelfContactsQuery);
        taskMap.put("sendGetDisclosureStageById", TaskMethod::sendGetDisclosureStageById);
        taskMap.put("sendQueryBlockingStatus", TaskMethod::sendQueryBlockingStatus);
        taskMap.put("sendGetOptOutList", TaskMethod::sendGetOptOutList);
        taskMap.put("sendGetWBizBusinessProfile", TaskMethod::sendGetWBizBusinessProfile);
        taskMap.put("sendDeleteAllData", TaskMethod::sendDeleteAllData);
        taskMap.put("sendWBiz139", TaskMethod::sendWBiz139);
        taskMap.put("getInitPictureUrl", TaskMethod::getInitPictureUrl);
        taskMap.put("sendThriftIq118", TaskMethod::sendThriftIq118);
        taskMap.put("sendAndroidTrackable20250331One", TaskMethod::sendAndroidTrackable20250331One);
        taskMap.put("sendAndroidTrackable20250331Hundred", TaskMethod::sendAndroidTrackable20250331Hundred);
        taskMap.put("sendThriftIqCatKit", TaskMethod::sendThriftIqCatKit);
        taskMap.put("sendSetVerifiedName", TaskMethod::sendSetVerifiedName);
        taskMap.put("sendSetBusinessCategory", TaskMethod::sendSetBusinessCategory);
        taskMap.put("sendWBiz109", TaskMethod::sendWBiz109);
        taskMap.put("sendBusinessProfileMyself", TaskMethod::sendBusinessProfileMyself);
        taskMap.put("sendSMAXId42Request", TaskMethod::sendSMAXId42Request);
        taskMap.put("sendAndroidTrackable20601227One", TaskMethod::sendAndroidTrackable20601227One);
        taskMap.put("sendAndroidTrackable20601227Hundred", TaskMethod::sendAndroidTrackable20601227Hundred);
        taskMap.put("sendAndroidTrackable20601228One", TaskMethod::sendAndroidTrackable20601228One);
        taskMap.put("sendAndroidTrackable20601228Hundred", TaskMethod::sendAndroidTrackable20601228Hundred);
    }

    /**
     * 动态调用任务方法
     */
    public void invokeTaskMethod(String methodName, GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        BiConsumer<GorgeousEngine, Consumer<Boolean>> task = taskMap.get(methodName);
        if (task == null) {
            throw new IllegalArgumentException("Method not found: " + methodName);
        }
        task.accept(gorgeousEngine, consumer);
    }

    public void sendXmlnsWB(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq id='02b' xmlns='w:b' type='get' to='s.whatsapp.net'><lists addressing_mode='pn'/></iq>
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        iq.AddAttribute(new StanzaAttribute("xmlns", "w:b"));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        ProtocolTreeNode lists = new ProtocolTreeNode("lists");
        lists.AddAttribute(new StanzaAttribute("addressing_mode", "pn"));
        iq.AddChild(lists);
        gorgeousEngine.SetIqId(iq);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void initDigest(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        gorgeousEngine.digest();
        consumer.accept(true);
    }

    public void getStatusPrivacy(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq id='1730348720-12' to='s.whatsapp.net' xmlns='status' type='get'><privacy/></iq>
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        iq.AddAttribute(new StanzaAttribute("xmlns", "status"));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        ProtocolTreeNode privacy = new ProtocolTreeNode("privacy");
        iq.AddChild(privacy);
        gorgeousEngine.SetIqId(iq);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void getPrivacy(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq id='1730348720-13' to='s.whatsapp.net' xmlns='privacy' type='get'><privacy/></iq>
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        iq.AddAttribute(new StanzaAttribute("xmlns", "privacy"));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        ProtocolTreeNode privacy = new ProtocolTreeNode("privacy");
        iq.AddChild(privacy);
        gorgeousEngine.SetIqId(iq);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void getDisappearingMode(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq id='1730348720-14' to='s.whatsapp.net' xmlns='disappearing_mode' type='get'/>
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        iq.AddAttribute(new StanzaAttribute("xmlns", "disappearing_mode"));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        gorgeousEngine.SetIqId(iq);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }


    public void getAccept(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq id='1730348720-16' to='s.whatsapp.net' xmlns='urn:xmpp:whatsapp:account' type='get'><accept/></iq>
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        iq.AddAttribute(new StanzaAttribute("xmlns", "urn:xmpp:whatsapp:account"));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        ProtocolTreeNode accept = new ProtocolTreeNode("accept");
        iq.AddChild(accept);
        gorgeousEngine.SetIqId(iq);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void getBlockList(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq id='1730348720-21' to='s.whatsapp.net' xmlns='blocklist' type='get'/>
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        iq.AddAttribute(new StanzaAttribute("xmlns", "blocklist"));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        gorgeousEngine.SetIqId(iq);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void removeAllCompanionDevice(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq id='1730348720-22' to='s.whatsapp.net' xmlns='md' type='set'><remove-companion-device all='true' reason='user_initiated'/></iq>
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        iq.AddAttribute(new StanzaAttribute("xmlns", "md"));
        iq.AddAttribute(new StanzaAttribute("type", "set"));
        ProtocolTreeNode protocolTreeNode = new ProtocolTreeNode("remove-companion-device");
        protocolTreeNode.AddAttribute(new StanzaAttribute("all", "true"));
        protocolTreeNode.AddAttribute(new StanzaAttribute("reason", "user_initiated"));
        iq.AddChild(protocolTreeNode);
        gorgeousEngine.SetIqId(iq);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendPresenceUnavailable(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        ProtocolTreeNode iq = gorgeousEngine.sendPresence(false, null);
        gorgeousEngine.AddTask(iq);
        consumer.accept(true);
    }

    public void sendPresenceName(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        ProtocolTreeNode iq = gorgeousEngine.sendPresence(true, gorgeousEngine.getEnvBuilder_().getPushname());
        gorgeousEngine.AddTask(iq);
        consumer.accept(true);
    }

    public void sendPresenceAvailable(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        ProtocolTreeNode iq = gorgeousEngine.sendPresence(true, null);
        gorgeousEngine.AddTask(iq);
        consumer.accept(true);
    }

    public void sendXmlnsW(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq id='1731913513-5' to='s.whatsapp.net' xmlns='w' type='get'><props hash='' protocol='2'/></iq>
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        iq.AddAttribute(new StanzaAttribute("xmlns", "w"));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        ProtocolTreeNode props = new ProtocolTreeNode("props");
        props.AddAttribute(new StanzaAttribute("hash", ""));
        props.AddAttribute(new StanzaAttribute("protocol", "2"));
        iq.AddChild(props);
        gorgeousEngine.SetIqId(iq);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendGetGroups(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        //  <iq id='1731913513-6' to='g.us' xmlns='w:g2' type='get'><participating><description/><participants/></participating></iq>
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("to", "g.us"));
        iq.AddAttribute(new StanzaAttribute("xmlns", "w:g2"));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        ProtocolTreeNode participating = new ProtocolTreeNode("participating");
        ProtocolTreeNode description = new ProtocolTreeNode("description");
        ProtocolTreeNode participants = new ProtocolTreeNode("participants");
        participating.AddChild(description);
        participating.AddChild(participants);
        iq.AddChild(participating);
        gorgeousEngine.SetIqId(iq);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendGet2Fa(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq id='1731913513-14' to='s.whatsapp.net' xmlns='urn:xmpp:whatsapp:account' type='get'><2fa/></iq>
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        iq.AddAttribute(new StanzaAttribute("xmlns", "urn:xmpp:whatsapp:account"));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        iq.AddChild(new ProtocolTreeNode("2fa"));
        gorgeousEngine.SetIqId(iq);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendGetNotice(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // IOS: <iq id='1731913513-18' to='s.whatsapp.net' xmlns='tos' type='get'><request><notice id='20230902'/><notice id='20230901'/><notice id='20240729'/><notice id='20231027'/></request></iq>
        // Android:  <iq to='s.whatsapp.net' id='0a' xmlns='tos' type='get'><request><notice id='20210210'/></request></iq>
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        iq.AddAttribute(new StanzaAttribute("xmlns", "tos"));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        ProtocolTreeNode request = new ProtocolTreeNode("request");
        if (gorgeousEngine.isIosLogin()) {
            ProtocolTreeNode notice1 = new ProtocolTreeNode("notice");
            notice1.AddAttribute(new StanzaAttribute("id", "20230902"));
            ProtocolTreeNode notice2 = new ProtocolTreeNode("notice");
            notice2.AddAttribute(new StanzaAttribute("id", "20230901"));
            ProtocolTreeNode notice3 = new ProtocolTreeNode("notice");
            notice3.AddAttribute(new StanzaAttribute("id", "20240729"));
            ProtocolTreeNode notice4 = new ProtocolTreeNode("notice");
            notice4.AddAttribute(new StanzaAttribute("id", "20231027"));
            request.AddChild(notice1);
            request.AddChild(notice2);
            request.AddChild(notice3);
            request.AddChild(notice4);
        } else {
            ProtocolTreeNode notice = new ProtocolTreeNode("notice");
            notice.AddAttribute(new StanzaAttribute("id", "20210210"));
            request.AddChild(notice);
        }
        iq.AddChild(request);
        gorgeousEngine.SetIqId(iq);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendCleanGroups(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq id='1731913513-28' to='s.whatsapp.net' xmlns='urn:xmpp:whatsapp:dirty' type='set'><clean type='groups'/></iq>
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        iq.AddAttribute(new StanzaAttribute("xmlns", "urn:xmpp:whatsapp:dirty"));
        iq.AddAttribute(new StanzaAttribute("type", "set"));
        ProtocolTreeNode clean = new ProtocolTreeNode("clean");
        clean.AddAttribute(new StanzaAttribute("type", "groups"));
        iq.AddChild(clean);
        gorgeousEngine.SetIqId(iq);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendCleanAccountSync(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq id='1731913513-28' to='s.whatsapp.net' xmlns='urn:xmpp:whatsapp:dirty' type='set'><clean type='account_sync'/></iq>
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        iq.AddAttribute(new StanzaAttribute("xmlns", "urn:xmpp:whatsapp:dirty"));
        iq.AddAttribute(new StanzaAttribute("type", "set"));
        ProtocolTreeNode clean = new ProtocolTreeNode("clean");
        clean.AddAttribute(new StanzaAttribute("type", "account_sync"));
        iq.AddChild(clean);
        gorgeousEngine.SetIqId(iq);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendNewsletterSubscribed(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        iq.AddAttribute(new StanzaAttribute("xmlns", "w:mex"));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        ProtocolTreeNode query = new ProtocolTreeNode("query");
        if (gorgeousEngine.isIosLogin()) {
            query.AddAttribute(new StanzaAttribute("query_id", "8404355486287027"));
            query.SetData("{\"variables\":{}}".getBytes(StandardCharsets.UTF_8));
        } else {
            ProtocolTreeNode trace = new ProtocolTreeNode("trace");
            ProtocolTreeNode flowId = new ProtocolTreeNode("flow_id");
            flowId.SetData("8621797084555037".getBytes(StandardCharsets.UTF_8));
            trace.AddChild(flowId);
            iq.AddChild(trace);
            query.AddAttribute(new StanzaAttribute("query_id", "8621797084555037"));
            query.SetData("{\"queryId\":\"8621797084555037\",\"variables\":{}}".getBytes(StandardCharsets.UTF_8));
        }
        //NewsletterSubscribed
        iq.AddChild(query);
        gorgeousEngine.SetIqId(iq);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendGetInviteSenderInfo(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq id='1731913513-39' to='s.whatsapp.net' xmlns='w:mex' type='get'><query query_id='8250272358339496'>{"variables":{"input":{}}}</query></iq>
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        iq.AddAttribute(new StanzaAttribute("xmlns", "w:mex"));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        ProtocolTreeNode query = new ProtocolTreeNode("query");
        //GetInviteSenderInfo
        query.AddAttribute(new StanzaAttribute("query_id", "8250272358339496"));
        query.SetData("{\"variables\":{\"input\":{}}}".getBytes(StandardCharsets.UTF_8));
        iq.AddChild(query);
        gorgeousEngine.SetIqId(iq);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendGetEmail(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq id='1731913513-41' to='s.whatsapp.net' xmlns='urn:xmpp:whatsapp:account' type='get'><email/></iq>
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        iq.AddAttribute(new StanzaAttribute("xmlns", "urn:xmpp:whatsapp:account"));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        iq.AddChild(new ProtocolTreeNode("email"));
        gorgeousEngine.SetIqId(iq);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendRegistrationUpsellShown(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        //苹果 <iq id='1731913513-43' to='s.whatsapp.net' xmlns='w:mex' type='get'><query query_id='7480997188628461'>{"variables":{"input":"EMAIL"}}</query></iq>
        //安卓 <iq to='s.whatsapp.net' xmlns='w:mex' type='get' id='01d'><trace><flow_id>7480997188628461</flow_id></trace><query query_id='7480997188628461'>{"queryId":"7480997188628461","variables":{"input":"EMAIL"}}</query></iq>
        String queryId = "7480997188628461";
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        iq.AddAttribute(new StanzaAttribute("xmlns", "w:mex"));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        if (!gorgeousEngine.isIosLogin()) {
            addTranceNode(iq, queryId);
        }
        ProtocolTreeNode query = new ProtocolTreeNode("query");
        //RegistrationUpsellShown
        query.AddAttribute(new StanzaAttribute("query_id", queryId));
        query.SetData("{\"variables\":{\"input\":\"EMAIL\"}}".getBytes(StandardCharsets.UTF_8));
        iq.AddChild(query);
        gorgeousEngine.SetIqId(iq);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendGetSuggestedContacts(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq id='1731913513-45' to='s.whatsapp.net' xmlns='w:mex' type='get'><query query_id='7526283997388181'>{"variables":{"input":{"context":"LANDING_SCREEN","metadata":{"exclude_jids":[],"priority_jids":[]}}}}</query></iq>
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        iq.AddAttribute(new StanzaAttribute("xmlns", "w:mex"));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        ProtocolTreeNode query = new ProtocolTreeNode("query");
        //ios SuggestedContacts android GetSuggestedContacts
        if (gorgeousEngine.isIosLogin()) {
            query.AddAttribute(new StanzaAttribute("query_id", "7526283997388181"));
        } else {
            query.AddAttribute(new StanzaAttribute("query_id", "24497904963133841"));
        }
        query.SetData("{\"input\":{\"context\":\"LANDING_SCREEN\",\"metadata\":{\"exclude_jids\":[],\"priority_jids\":[]}}}}".getBytes(StandardCharsets.UTF_8));
        iq.AddChild(query);
        gorgeousEngine.SetIqId(iq);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendOhaiKeyConfigQuery(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq id='1731913513-56' to='s.whatsapp.net' xmlns='w:mex' type='get'><query query_id='8699487313397273'>{"variables":{}}</query></iq>
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        iq.AddAttribute(new StanzaAttribute("xmlns", "w:mex"));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        ProtocolTreeNode query = new ProtocolTreeNode("query");
        //OhaiKeyConfigQuery，安卓没有
        query.AddAttribute(new StanzaAttribute("query_id", "8699487313397273"));
        query.SetData("{\"variables\":{}}".getBytes(StandardCharsets.UTF_8));
        iq.AddChild(query);
        gorgeousEngine.SetIqId(iq);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendGetRegistrationUpsells(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // 苹果 <iq id='1731913513-56' to='s.whatsapp.net' xmlns='w:mex' type='get'><query query_id='7561558900567547'>{"variables":{}}</query></iq>
        // 安卓 <iq to='s.whatsapp.net' xmlns='w:mex' type='get' id='01b'><trace><flow_id>7561558900567547</flow_id></trace><query query_id='7561558900567547'>{"queryId":"7561558900567547","variables":{}}</query></iq>
        String queryId = "7561558900567547";
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        iq.AddAttribute(new StanzaAttribute("xmlns", "w:mex"));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        if (!gorgeousEngine.isIosLogin()) {
            addTranceNode(iq, queryId);
        }
        ProtocolTreeNode query = new ProtocolTreeNode("query");
        //GetRegistrationUpsells
        query.AddAttribute(new StanzaAttribute("query_id", queryId));
        query.SetData("{\"variables\":{}}".getBytes(StandardCharsets.UTF_8));
        iq.AddChild(query);
        gorgeousEngine.SetIqId(iq);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendGetGroupParticipating(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq id='1731913513-57' to='g.us' xmlns='w:g2' type='get'><participating/></iq>
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("to", "g.us"));
        iq.AddAttribute(new StanzaAttribute("xmlns", "w:g2"));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        ProtocolTreeNode participating = new ProtocolTreeNode("participating");
        iq.AddChild(participating);
        gorgeousEngine.SetIqId(iq);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendNewsletterAddons(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        iq.AddAttribute(new StanzaAttribute("xmlns", "newsletter"));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        ProtocolTreeNode myAddons = new ProtocolTreeNode("my_addons");
        myAddons.AddAttribute(new StanzaAttribute("limit", "1000"));
        iq.AddChild(myAddons);
        gorgeousEngine.SetIqId(iq);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    /**
     * Android账号初始化同步通讯录
     */
    public void accountAndroidInitSync(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        List<String> phoneNumbers = gorgeousEngine.axolotlManager_.contactBookManager.getAllPhoneNumbers();
        if (phoneNumbers.isEmpty()) {
            consumer.accept(false);
            return;
        }
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("xmlns", "usync"));
        iq.AddAttribute(new StanzaAttribute("id", gorgeousEngine.GenerateIqId()));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        ProtocolTreeNode usync = new ProtocolTreeNode("usync");
        usync.AddAttribute(new StanzaAttribute("sid", "ContactSyncHelper/sync_sid_full_" + cn.hutool.core.lang.UUID.randomUUID()));
        usync.AddAttribute(new StanzaAttribute("index", "0"));
        usync.AddAttribute(new StanzaAttribute("last", "true"));
        usync.AddAttribute(new StanzaAttribute("mode", "full"));
        usync.AddAttribute(new StanzaAttribute("context", "registration"));
        ProtocolTreeNode query = new ProtocolTreeNode("query");
        query.AddChild(new ProtocolTreeNode("contact"));
//        query.AddChild(new ProtocolTreeNode("status"));
//        ProtocolTreeNode business = new ProtocolTreeNode("business");
//        ProtocolTreeNode verifiedName = new ProtocolTreeNode("verified_name");
//        ProtocolTreeNode profile = new ProtocolTreeNode("profile");
//        profile.AddAttribute(new StanzaAttribute("v", "1908"));
//        business.AddChild(verifiedName);
//        business.AddChild(profile);
//        query.AddChild(business);
//        ProtocolTreeNode devices = new ProtocolTreeNode("devices");
//        devices.AddAttribute(new StanzaAttribute("version", "2"));
//        query.AddChild(devices);
//        query.AddChild(new ProtocolTreeNode("disappearing_mode"));
//        query.AddChild(new ProtocolTreeNode("lid"));
//        usync.AddChild(query);
//        ProtocolTreeNode list = new ProtocolTreeNode("list");
//        for (String fan : phoneNumbers) {
//            ProtocolTreeNode user = new ProtocolTreeNode("user");
//            ProtocolTreeNode c = new ProtocolTreeNode("contact");
//            String contactPhone = fan.startsWith("+") ? fan : "+" + fan;
//            c.SetData(contactPhone.getBytes(StandardCharsets.UTF_8));
//            user.AddChild(c);
//            list.AddChild(user);
//        }
        usync.AddChild(query);
        ProtocolTreeNode list = new ProtocolTreeNode("list");
        usync.AddChild(list);
        iq.AddChild(usync);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }


    /**
     * IOS账号初始化同步通讯录
     */
    public void accountIOSInitSync(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        //粉丝从数据库获取
        List<String> phoneNumbers = gorgeousEngine.axolotlManager_.contactBookManager.getAllPhoneNumbers();
        if (phoneNumbers.isEmpty()) {
            consumer.accept(false);
            return;
        }
        ProtocolTreeNode protocolTreeNode = sendSyncContactsDelta(gorgeousEngine, phoneNumbers);
        protocolTreeNode.AddAttribute(new StanzaAttribute("id", gorgeousEngine.GenerateIqId()));
        gorgeousEngine.AddTask(protocolTreeNode, ((srcNode, result) -> {
            ProtocolTreeNode usync = result.getOneChildren("usync");
            ProtocolTreeNode list = usync.getOneChildren("list");
            if (ObjectUtil.isNotNull(list) && !list.GetChildren("user").isEmpty()) {
                List<ProfileInfo> profileInfos = new ArrayList<>();
                LinkedList<ProtocolTreeNode> users = list.GetChildren("user");
                for (ProtocolTreeNode user : users) {
                    ProtocolTreeNode contact = user.getOneChildren("contact");
                    ProfileInfo profileInfo = new ProfileInfo();
                    profileInfo.setFansId(new String(contact.GetData(), StandardCharsets.UTF_8));
                    ProtocolTreeNode status = user.GetChild("status");
                    if (ObjectUtil.isNotNull(status)) {
                        profileInfo.setUpdateStatusTime(status.GetAttributeValue("t"));
                    }
                    ProtocolTreeNode lid = user.GetChild("lid");
                    if (ObjectUtil.isNotNull(lid)) {
                        profileInfo.setLid(lid.GetAttributeValue("val"));
                    }
                    ProtocolTreeNode business = user.GetChild("business");
                    if (ObjectUtil.isNotNull(business) && ObjectUtil.isNotNull(business.GetChild("profile"))) {
                        ProtocolTreeNode profile = business.GetChild("profile");
                        ProtocolTreeNode bizIdentityInfo = profile.GetChild("biz_identity_info");
                        if (ObjectUtil.isNotNull(bizIdentityInfo)) {
                            profileInfo.setVerifiedName(bizIdentityInfo.GetAttributeValue("serial"));
                        }
                    }
                    profileInfos.add(profileInfo);
                }
                ProtocolTreeNode protocolTreeNode1 = sendSyncContactsFull(gorgeousEngine, profileInfos);
                protocolTreeNode1.AddAttribute(new StanzaAttribute("id", gorgeousEngine.GenerateIqId()));
                gorgeousEngine.AddTask(protocolTreeNode1, (srcNode1, result1) -> consumer.accept(true));
            }
        }));
    }

    public ProtocolTreeNode sendSyncContactsFull(GorgeousEngine gorgeousEngine, List<ProfileInfo> syncList) {
        // <iq id='1731913513-38' xmlns='usync' type='get' to='85293284084@s.whatsapp.net'><usync mode='full' allow_mutation='true' context='background' last='true' sid='1731913671-458171847-2' index='0'><query><business><verified_name/><profile v='1396'/></business><contact/><devices version='2'/><disappearing_mode/><sidelist/><status/><lid/></query><list><user><contact>+639561782761</contact></user><user><contact>+639562168226</contact></user><user><business><verified_name serial='30190659483700644'/></business><contact>+2347034553965</contact><devices device_hash='2:kKWIyQXt'/><lid jid='102817725436124.1:0@lid'/></user><user><business><verified_name serial='20657856074147081'/></business><contact>+27611399711</contact><devices device_hash='2:7jAt8596'/><status t='1718956443'/><lid jid='191538848108684.1:0@lid'/></user><user><business><verified_name serial='42107615036215080'/></business><contact>+2347033483785</contact><devices device_hash='2:8+pnnDWc'/><status t='1703885461'/><lid jid='148906935640280.1:0@lid'/></user><user><contact>+852639562168226</contact></user></list><side_list/></usync></iq>
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");

        iq.AddAttribute(new StanzaAttribute("to", gorgeousEngine.getUserId()));
        iq.AddAttribute(new StanzaAttribute("xmlns", "usync"));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        ProtocolTreeNode usync = new ProtocolTreeNode("usync");
        usync.AddAttribute(new StanzaAttribute("mode", "full"));
        usync.AddAttribute(new StanzaAttribute("allow_mutation", "true"));
        usync.AddAttribute(new StanzaAttribute("context", "background"));
        usync.AddAttribute(new StanzaAttribute("last", "true"));
        String sid;
        if (gorgeousEngine.isIosLogin()) {
            sid = System.currentTimeMillis() / 1000 + "-" + new Random().nextInt(1000000000) + "-" + gorgeousEngine.getSyncId().incrementAndGet();
        } else {
            sid = "sync_sid_full_" + UUID.randomUUID();
        }
        usync.AddAttribute(new StanzaAttribute("sid", sid));
        usync.AddAttribute(new StanzaAttribute("index", "0"));
        ProtocolTreeNode query = new ProtocolTreeNode("query");
        ProtocolTreeNode business = new ProtocolTreeNode("business");
        ProtocolTreeNode verifiedName = new ProtocolTreeNode("verified_name");
        ProtocolTreeNode profile = new ProtocolTreeNode("profile");
        profile.AddAttribute(new StanzaAttribute("v", "1396"));
        business.AddChild(verifiedName);
        business.AddChild(profile);
        ProtocolTreeNode contact = new ProtocolTreeNode("contact");
        ProtocolTreeNode devices = new ProtocolTreeNode("devices");
        devices.AddAttribute(new StanzaAttribute("version", "2"));
        ProtocolTreeNode disappearingMode = new ProtocolTreeNode("disappearing_mode");
        ProtocolTreeNode sidelist = new ProtocolTreeNode("sidelist");
        ProtocolTreeNode status = new ProtocolTreeNode("status");
        ProtocolTreeNode lid = new ProtocolTreeNode("lid");
        query.AddChild(business);
        query.AddChild(contact);
        query.AddChild(devices);
        query.AddChild(disappearingMode);
        query.AddChild(sidelist);
        query.AddChild(status);
        query.AddChild(lid);
        ProtocolTreeNode list = new ProtocolTreeNode("list");
        for (ProfileInfo profileInfo : syncList) {
            ProtocolTreeNode user = new ProtocolTreeNode("user");
            ProtocolTreeNode c = new ProtocolTreeNode("contact");
            String contactPhone = profileInfo.getFansId().startsWith("+") ? profileInfo.getFansId() : "+" + profileInfo.getFansId();
            c.SetData(contactPhone.getBytes(StandardCharsets.UTF_8));
            user.AddChild(c);
            if (StrUtil.isNotEmpty(profileInfo.getVerifiedName())) {
                // 该粉丝是商业版whatsapp
                ProtocolTreeNode userBusiness = new ProtocolTreeNode("business");
                ProtocolTreeNode userVerifiedName = new ProtocolTreeNode("verified_name");
                userVerifiedName.AddAttribute(new StanzaAttribute("serial", profileInfo.getVerifiedName()));
                userBusiness.AddChild(userVerifiedName);
                user.AddChild(userBusiness);
                ProtocolTreeNode userDevices = new ProtocolTreeNode("devices");
                HashSet<String> hashSet = new HashSet<>();
                hashSet.add(StringUtil.ParseJid(profileInfo.getFansId()).toString());
                userDevices.AddAttribute(new StanzaAttribute("device_hash", CryptUtil.PHash(hashSet)));
                user.AddChild(userDevices);
                if (StrUtil.isNotEmpty(profileInfo.getUpdateStatusTime())) {
                    ProtocolTreeNode userStatus = new ProtocolTreeNode("status");
                    userStatus.AddAttribute(new StanzaAttribute("t", profileInfo.getUpdateStatusTime()));
                    user.AddChild(userStatus);
                }
                ProtocolTreeNode userLid = new ProtocolTreeNode("lid");
                userLid.AddAttribute(new StanzaAttribute("jid", profileInfo.getLid()));
                user.AddChild(userLid);
            }
            list.AddChild(user);
        }
        ProtocolTreeNode sideList = new ProtocolTreeNode("side_list");
        usync.AddChild(query);
        usync.AddChild(list);
        usync.AddChild(sideList);
        iq.AddChild(usync);
        return iq;
    }

    public ProtocolTreeNode sendSyncContactsDelta(GorgeousEngine gorgeousEngine, List<String> fans) {
        // <iq id='1731913513-36' xmlns='usync' type='get' to='85293284084@s.whatsapp.net'><usync mode='delta' allow_mutation='true' context='interactive' last='true' sid='1731913669-1274702800-1' index='0'><query><business><verified_name/><profile v='1396'/></business><contact/><devices version='2'/><disappearing_mode/><sidelist/><status/><lid/></query><list><user><contact>KzYzOTU2MjE2ODIyNg==</contact></user><user><contact>KzIzNDcwMzM0ODM3ODU=</contact></user><user><contact>KzIzNDcwMzQ1NTM5NjU=</contact></user><user><contact>Kzg1MjYzOTU2MjE2ODIyNg==</contact></user><user><contact>KzI3NjExMzk5NzEx</contact></user><user><contact>KzYzOTU2MTc4Mjc2MQ==</contact></user></list><side_list/></usync></iq>
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("to", gorgeousEngine.getUserId()));
        iq.AddAttribute(new StanzaAttribute("xmlns", "usync"));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        ProtocolTreeNode usync = new ProtocolTreeNode("usync");
        usync.AddAttribute(new StanzaAttribute("mode", "delta"));
        usync.AddAttribute(new StanzaAttribute("allow_mutation", "true"));
        usync.AddAttribute(new StanzaAttribute("context", "interactive"));
        usync.AddAttribute(new StanzaAttribute("last", "true"));
        String sid;
        if (gorgeousEngine.isIosLogin()) {
            sid = System.currentTimeMillis() / 1000 + "-" + new Random().nextInt(1000000000) + "-" + gorgeousEngine.getSyncId().incrementAndGet();
        } else {
            sid = "sync_sid_full_" + UUID.randomUUID();
        }
        usync.AddAttribute(new StanzaAttribute("sid", sid));
        usync.AddAttribute(new StanzaAttribute("index", "0"));
        ProtocolTreeNode query = new ProtocolTreeNode("query");
        ProtocolTreeNode business = new ProtocolTreeNode("business");
        ProtocolTreeNode verifiedName = new ProtocolTreeNode("verified_name");
        ProtocolTreeNode profile = new ProtocolTreeNode("profile");
        profile.AddAttribute(new StanzaAttribute("v", "1396"));
        business.AddChild(verifiedName);
        business.AddChild(profile);
        ProtocolTreeNode contact = new ProtocolTreeNode("contact");
        ProtocolTreeNode devices = new ProtocolTreeNode("devices");
        devices.AddAttribute(new StanzaAttribute("version", "2"));
        ProtocolTreeNode disappearingMode = new ProtocolTreeNode("disappearing_mode");
        ProtocolTreeNode sidelist = new ProtocolTreeNode("sidelist");
        ProtocolTreeNode status = new ProtocolTreeNode("status");
        ProtocolTreeNode lid = new ProtocolTreeNode("lid");
        query.AddChild(business);
        query.AddChild(contact);
        query.AddChild(devices);
        query.AddChild(disappearingMode);
        query.AddChild(sidelist);
        query.AddChild(status);
        query.AddChild(lid);
        ProtocolTreeNode list = new ProtocolTreeNode("list");
        for (String fan : fans) {
            ProtocolTreeNode user = new ProtocolTreeNode("user");
            ProtocolTreeNode c = new ProtocolTreeNode("contact");
            String contactPhone = fan.startsWith("+") ? fan : "+" + fan;
            c.SetData(contactPhone.getBytes(StandardCharsets.UTF_8));
            user.AddChild(c);
            list.AddChild(user);
        }
        ProtocolTreeNode sideList = new ProtocolTreeNode("side_list");
        usync.AddChild(query);
        usync.AddChild(list);
        usync.AddChild(sideList);
        iq.AddChild(usync);
        return iq;
    }

    public void getInitPicture(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq id='02f' xmlns='w:profile:picture' to='s.whatsapp.net' target='12318830493@s.whatsapp.net' type='get'><picture type='preview'/></iq>
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        iq.AddAttribute(new StanzaAttribute("xmlns", "w:profile:picture"));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        iq.AddAttribute(new StanzaAttribute("target", gorgeousEngine.getUserId()));
        ProtocolTreeNode picture = new ProtocolTreeNode("picture");
        picture.AddAttribute(new StanzaAttribute("type", "preview"));
        iq.AddChild(picture);
        gorgeousEngine.SetIqId(iq);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void getInitPictureUrl(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq id='016' xmlns='w:profile:picture' to='s.whatsapp.net' target='12706345611@s.whatsapp.net' type='get'><picture query='url' type='image'/></iq>
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        iq.AddAttribute(new StanzaAttribute("xmlns", "w:profile:picture"));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        iq.AddAttribute(new StanzaAttribute("target", gorgeousEngine.getUserId()));
        ProtocolTreeNode picture = new ProtocolTreeNode("picture");
        picture.AddAttribute(new StanzaAttribute("query", "url"));
        picture.AddAttribute(new StanzaAttribute("type", "image"));
        iq.AddChild(picture);
        gorgeousEngine.SetIqId(iq);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    /**
     * 获取联系人头像
     */
    public void getContactPicture(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        List<String> phoneNumbers = gorgeousEngine.axolotlManager_.contactBookManager.getAllPhoneNumbers();
        for (String phoneNumber : phoneNumbers) {
            ProtocolTreeNode iq = new ProtocolTreeNode("iq");
            iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
            iq.AddAttribute(new StanzaAttribute("xmlns", "w:profile:picture"));
            iq.AddAttribute(new StanzaAttribute("type", "get"));
            iq.AddAttribute(new StanzaAttribute("target", JidNormalize(phoneNumber)));
            ProtocolTreeNode picture = new ProtocolTreeNode("picture");
            picture.AddAttribute(new StanzaAttribute("query", "url"));
            picture.AddAttribute(new StanzaAttribute("type", "image"));
            iq.AddChild(picture);
            gorgeousEngine.SetIqId(iq);
            gorgeousEngine.AddTask(iq);
        }
        consumer.accept(true);
    }

    public void sendUpdateUserStatus(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq id='1731913513-65' to='s.whatsapp.net' xmlns='w:mex' type='get'><query query_id='8702204706516599'>{"variables":{"updates":["STATUS"],"users":[{"user_id":"85293284084"}]}}</query></iq>
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        iq.AddAttribute(new StanzaAttribute("xmlns", "w:mex"));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        ProtocolTreeNode query = new ProtocolTreeNode("query");
        //UpdateUserStatus
        query.AddAttribute(new StanzaAttribute("query_id", "8702204706516599"));
        String queryString = "{\"variables\":{\"updates\":[\"STATUS\"],\"users\":[{\"user_id\":\"" + StringUtil.ParseJid(gorgeousEngine.getUserId()).recipientId + "\"}]}}";
        query.SetData(queryString.getBytes(StandardCharsets.UTF_8));
        iq.AddChild(query);
        gorgeousEngine.SetIqId(iq);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    /**
     * 设置初始化描述
     */
    public void setIntStatus(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq id='1730348720-7' to='s.whatsapp.net' xmlns='status' type='get'><status><user jid='85254662129@s.whatsapp.net'/></status></iq>
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        iq.AddAttribute(new StanzaAttribute("xmlns", "status"));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        ProtocolTreeNode status = new ProtocolTreeNode("status");
        ProtocolTreeNode user = new ProtocolTreeNode("user");
        user.AddAttribute(new StanzaAttribute("jid", gorgeousEngine.getUserId()));
        status.AddChild(user);
        iq.AddChild(status);
        gorgeousEngine.SetIqId(iq);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            String describe = gorgeousEngine.getDescribe(result);
            ProtocolTreeNode iq1 = new ProtocolTreeNode("iq");
            iq1.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
            iq1.AddAttribute(new StanzaAttribute("xmlns", "status"));
            iq1.AddAttribute(new StanzaAttribute("type", "set"));
            ProtocolTreeNode s = new ProtocolTreeNode("status");
            // 若已经设置了前面且签名中不包含中文则设置为原始的即可
            if (StringUtils.hasLength(describe) && !StringUtil.isContainChinese(describe)) {
                s.SetData(describe.getBytes(StandardCharsets.UTF_8));
            } else {
                s.SetData("Hey there! I am using WhatsApp.".getBytes(StandardCharsets.UTF_8));
            }
            iq1.AddChild(s);
            gorgeousEngine.SetIqId(iq1);
            gorgeousEngine.AddTask(iq1, (srcNode1, result1) -> consumer.accept(true));
        });
    }

    private void addTranceNode(ProtocolTreeNode node, String queryId) {
        ProtocolTreeNode traceNode = new ProtocolTreeNode("trace");
        ProtocolTreeNode flowIdNode = new ProtocolTreeNode("flow_id");
        flowIdNode.SetData(queryId.getBytes(StandardCharsets.UTF_8));
        traceNode.AddChild(flowIdNode);
        node.AddChild(traceNode);
    }

    public void sendGetDisclosureStageById(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq to='s.whatsapp.net' type='get' xmlns='tos' id='030'><get_disclosure_stage_by_id id='20601217' t='1759042658'/><get_disclosure_stage_by_id id='20601218' t='1759042658'/><get_disclosure_stage_by_id id='20601216' t='1759042658'/><get_disclosure_stage_by_id id='20610210' t='1759042658'/><get_disclosure_stage_by_id id='20610101' t='1759042658'/><get_disclosure_stage_by_id id='20610220' t='1759042658'/><get_disclosure_stage_by_id id='20241005' t='1759042658'/><get_disclosure_stage_by_id id='20601217' t='1759042658'/><get_disclosure_stage_by_id id='20601218' t='1759042658'/><get_disclosure_stage_by_id id='20601216' t='1759042658'/><get_disclosure_stage_by_id id='20610210' t='1759042658'/><get_disclosure_stage_by_id id='20610101' t='1759042658'/><get_disclosure_stage_by_id id='20610220' t='1759042658'/></iq>"}
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        iq.AddAttribute(new StanzaAttribute("xmlns", "tos"));
        iq.AddAttribute(new StanzaAttribute("id", gorgeousEngine.GenerateIqId()));
        if (!gorgeousEngine.isBusinessVersion()) {
            for (String s : androidPersonDisclosureStageId) {
                ProtocolTreeNode getDisclosureStageById = new ProtocolTreeNode("get_disclosure_stage_by_id");
                getDisclosureStageById.AddAttribute(new StanzaAttribute("id", s));
                getDisclosureStageById.AddAttribute(new StanzaAttribute("t", String.valueOf(System.currentTimeMillis() / 1000)));
                iq.AddChild(getDisclosureStageById);
            }
        } else {
            for (String s : androidBusinessDisclosureStageId) {
                ProtocolTreeNode getDisclosureStageById = new ProtocolTreeNode("get_disclosure_stage_by_id");
                getDisclosureStageById.AddAttribute(new StanzaAttribute("id", s));
                getDisclosureStageById.AddAttribute(new StanzaAttribute("t", String.valueOf(System.currentTimeMillis() / 1000)));
                iq.AddChild(getDisclosureStageById);
            }
        }

        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    /**
     * 安卓获取用户公告
     */
    public void sendGetUserDisclosures(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq to='s.whatsapp.net' type='get' xmlns='tos' id='04'><get_user_disclosures t='1732551014'/></iq>
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        iq.AddAttribute(new StanzaAttribute("xmlns", "tos"));
        iq.AddAttribute(new StanzaAttribute("id", gorgeousEngine.GenerateIqId()));
        ProtocolTreeNode getUserDisclosures = new ProtocolTreeNode("get_user_disclosures");
        getUserDisclosures.AddAttribute(new StanzaAttribute("t", String.valueOf(System.currentTimeMillis() / 1000)));
        iq.AddChild(getUserDisclosures);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    /**
     * 安卓群组代审批请求
     */
    public void sendGetPreRegAddRequests(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq to='g.us' xmlns='w:g2' type='get' id='056'><pre_reg_add_requests/></iq>
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("to", "g.us"));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        iq.AddAttribute(new StanzaAttribute("xmlns", "w:g2"));
        iq.AddAttribute(new StanzaAttribute("id", gorgeousEngine.GenerateIqId()));
        ProtocolTreeNode preRegAddRequests = new ProtocolTreeNode("pre_reg_add_requests");
        iq.AddChild(preRegAddRequests);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public static ProtocolTreeNode makeTOSIqNode(GorgeousEngine gorgeousEngine) {
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        iq.AddAttribute(new StanzaAttribute("type", "set"));
        iq.AddAttribute(new StanzaAttribute("xmlns", "tos"));
        iq.AddAttribute(new StanzaAttribute("id", gorgeousEngine.GenerateIqId()));
        return iq;
    }

    /**
     * 安卓发送trackable
     */
    public void sendAndroidTrackable20401220One(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq to='s.whatsapp.net' type='set' xmlns='tos' id='0c'><trackable id='20401220' result='1'/></iq>
        ProtocolTreeNode iq = makeTOSIqNode(gorgeousEngine);
        ProtocolTreeNode trackable = new ProtocolTreeNode("trackable");
        trackable.AddAttribute(new StanzaAttribute("id", "20401220"));
        trackable.AddAttribute(new StanzaAttribute("result", "1"));
        iq.AddChild(trackable);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendAndroidTrackable20401220Hundred(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq to='s.whatsapp.net' type='set' xmlns='tos' id='0c'><trackable id='20401220' result='1'/></iq>
        ProtocolTreeNode iq = makeTOSIqNode(gorgeousEngine);
        ProtocolTreeNode trackable = new ProtocolTreeNode("trackable");
        trackable.AddAttribute(new StanzaAttribute("id", "20401220"));
        trackable.AddAttribute(new StanzaAttribute("result", "100"));
        iq.AddChild(trackable);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendAndroidTrackable20601216One(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq to='s.whatsapp.net' type='set' xmlns='tos' id='0c'><trackable id='20401220' result='1'/></iq>
        ProtocolTreeNode iq = makeTOSIqNode(gorgeousEngine);
        ProtocolTreeNode trackable = new ProtocolTreeNode("trackable");
        trackable.AddAttribute(new StanzaAttribute("id", "20601216"));
        trackable.AddAttribute(new StanzaAttribute("result", "1"));
        iq.AddChild(trackable);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendAndroidTrackable20601216Hundred(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq to='s.whatsapp.net' type='set' xmlns='tos' id='0c'><trackable id='20401220' result='1'/></iq>
        ProtocolTreeNode iq = makeTOSIqNode(gorgeousEngine);
        ProtocolTreeNode trackable = new ProtocolTreeNode("trackable");
        trackable.AddAttribute(new StanzaAttribute("id", "20601216"));
        trackable.AddAttribute(new StanzaAttribute("result", "100"));
        iq.AddChild(trackable);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendAndroidTrackable20601217One(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq to='s.whatsapp.net' type='set' xmlns='tos' id='0c'><trackable id='20401220' result='1'/></iq>
        ProtocolTreeNode iq = makeTOSIqNode(gorgeousEngine);
        ProtocolTreeNode trackable = new ProtocolTreeNode("trackable");
        trackable.AddAttribute(new StanzaAttribute("id", "20601217"));
        trackable.AddAttribute(new StanzaAttribute("result", "1"));
        iq.AddChild(trackable);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendAndroidTrackable20601217Hundred(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq to='s.whatsapp.net' type='set' xmlns='tos' id='0c'><trackable id='20401220' result='1'/></iq>
        ProtocolTreeNode iq = makeTOSIqNode(gorgeousEngine);
        ProtocolTreeNode trackable = new ProtocolTreeNode("trackable");
        trackable.AddAttribute(new StanzaAttribute("id", "20601217"));
        trackable.AddAttribute(new StanzaAttribute("result", "100"));
        iq.AddChild(trackable);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendAndroidTrackable20601218One(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq to='s.whatsapp.net' type='set' xmlns='tos' id='0c'><trackable id='20401220' result='1'/></iq>
        ProtocolTreeNode iq = makeTOSIqNode(gorgeousEngine);
        ProtocolTreeNode trackable = new ProtocolTreeNode("trackable");
        trackable.AddAttribute(new StanzaAttribute("id", "20601218"));
        trackable.AddAttribute(new StanzaAttribute("result", "1"));
        iq.AddChild(trackable);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendAndroidTrackable20601218Hundred(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq to='s.whatsapp.net' type='set' xmlns='tos' id='0c'><trackable id='20401220' result='1'/></iq>
        ProtocolTreeNode iq = makeTOSIqNode(gorgeousEngine);
        ProtocolTreeNode trackable = new ProtocolTreeNode("trackable");
        trackable.AddAttribute(new StanzaAttribute("id", "20601218"));
        trackable.AddAttribute(new StanzaAttribute("result", "100"));
        iq.AddChild(trackable);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendAndroidTrackable20900727One(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq to='s.whatsapp.net' type='set' xmlns='tos' id='0c'><trackable id='20401220' result='1'/></iq>
        ProtocolTreeNode iq = makeTOSIqNode(gorgeousEngine);
        ProtocolTreeNode trackable = new ProtocolTreeNode("trackable");
        trackable.AddAttribute(new StanzaAttribute("id", "20900727"));
        trackable.AddAttribute(new StanzaAttribute("result", "1"));
        iq.AddChild(trackable);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendAndroidTrackable20900727Hundred(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq to='s.whatsapp.net' type='set' xmlns='tos' id='0c'><trackable id='20401220' result='1'/></iq>
        ProtocolTreeNode iq = makeTOSIqNode(gorgeousEngine);
        ProtocolTreeNode trackable = new ProtocolTreeNode("trackable");
        trackable.AddAttribute(new StanzaAttribute("id", "20900727"));
        trackable.AddAttribute(new StanzaAttribute("result", "100"));
        iq.AddChild(trackable);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendAndroidTrackable20610203One(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq to='s.whatsapp.net' type='set' xmlns='tos' id='0c'><trackable id='20401220' result='1'/></iq>
        ProtocolTreeNode iq = makeTOSIqNode(gorgeousEngine);
        ProtocolTreeNode trackable = new ProtocolTreeNode("trackable");
        trackable.AddAttribute(new StanzaAttribute("id", "20610203"));
        trackable.AddAttribute(new StanzaAttribute("result", "1"));
        iq.AddChild(trackable);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendAndroidTrackable20610203Hundred(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq to='s.whatsapp.net' type='set' xmlns='tos' id='0c'><trackable id='20401220' result='1'/></iq>
        ProtocolTreeNode iq = makeTOSIqNode(gorgeousEngine);
        ProtocolTreeNode trackable = new ProtocolTreeNode("trackable");
        trackable.AddAttribute(new StanzaAttribute("id", "20610203"));
        trackable.AddAttribute(new StanzaAttribute("result", "100"));
        iq.AddChild(trackable);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendAndroidTrackable20610204One(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq to='s.whatsapp.net' type='set' xmlns='tos' id='0c'><trackable id='20401220' result='1'/></iq>
        ProtocolTreeNode iq = makeTOSIqNode(gorgeousEngine);
        ProtocolTreeNode trackable = new ProtocolTreeNode("trackable");
        trackable.AddAttribute(new StanzaAttribute("id", "20610204"));
        trackable.AddAttribute(new StanzaAttribute("result", "1"));
        iq.AddChild(trackable);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendAndroidTrackable20610204Hundred(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq to='s.whatsapp.net' type='set' xmlns='tos' id='0c'><trackable id='20401220' result='1'/></iq>
        ProtocolTreeNode iq = makeTOSIqNode(gorgeousEngine);
        ProtocolTreeNode trackable = new ProtocolTreeNode("trackable");
        trackable.AddAttribute(new StanzaAttribute("id", "20610204"));
        trackable.AddAttribute(new StanzaAttribute("result", "100"));
        iq.AddChild(trackable);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendAndroidTrackable20230901One(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq to='s.whatsapp.net' type='set' xmlns='tos' id='0c'><trackable id='20401220' result='1'/></iq>
        ProtocolTreeNode iq = makeTOSIqNode(gorgeousEngine);
        ProtocolTreeNode trackable = new ProtocolTreeNode("trackable");
        trackable.AddAttribute(new StanzaAttribute("id", "20230901"));
        trackable.AddAttribute(new StanzaAttribute("result", "1"));
        iq.AddChild(trackable);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendAndroidTrackable20230901Hundred(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq to='s.whatsapp.net' type='set' xmlns='tos' id='0c'><trackable id='20401220' result='1'/></iq>
        ProtocolTreeNode iq = makeTOSIqNode(gorgeousEngine);
        ProtocolTreeNode trackable = new ProtocolTreeNode("trackable");
        trackable.AddAttribute(new StanzaAttribute("id", "20230901"));
        trackable.AddAttribute(new StanzaAttribute("result", "100"));
        iq.AddChild(trackable);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendAndroidTrackable20240216One(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq to='s.whatsapp.net' type='set' xmlns='tos' id='0c'><trackable id='20401220' result='1'/></iq>
        ProtocolTreeNode iq = makeTOSIqNode(gorgeousEngine);
        ProtocolTreeNode trackable = new ProtocolTreeNode("trackable");
        trackable.AddAttribute(new StanzaAttribute("id", "20240216"));
        trackable.AddAttribute(new StanzaAttribute("result", "1"));
        iq.AddChild(trackable);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendAndroidTrackable20240216Hundred(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq to='s.whatsapp.net' type='set' xmlns='tos' id='0c'><trackable id='20401220' result='1'/></iq>
        ProtocolTreeNode iq = makeTOSIqNode(gorgeousEngine);
        ProtocolTreeNode trackable = new ProtocolTreeNode("trackable");
        trackable.AddAttribute(new StanzaAttribute("id", "20240216"));
        trackable.AddAttribute(new StanzaAttribute("result", "100"));
        iq.AddChild(trackable);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendAndroidTrackable20230902One(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq to='s.whatsapp.net' type='set' xmlns='tos' id='0c'><trackable id='20401220' result='1'/></iq>
        ProtocolTreeNode iq = makeTOSIqNode(gorgeousEngine);
        ProtocolTreeNode trackable = new ProtocolTreeNode("trackable");
        trackable.AddAttribute(new StanzaAttribute("id", "20230902"));
        trackable.AddAttribute(new StanzaAttribute("result", "1"));
        iq.AddChild(trackable);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendAndroidTrackable20230902Hundred(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq to='s.whatsapp.net' type='set' xmlns='tos' id='0c'><trackable id='20401220' result='1'/></iq>
        ProtocolTreeNode iq = makeTOSIqNode(gorgeousEngine);
        ProtocolTreeNode trackable = new ProtocolTreeNode("trackable");
        trackable.AddAttribute(new StanzaAttribute("id", "20230902"));
        trackable.AddAttribute(new StanzaAttribute("result", "100"));
        iq.AddChild(trackable);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendAndroidTrackable20240729One(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq to='s.whatsapp.net' type='set' xmlns='tos' id='0c'><trackable id='20401220' result='1'/></iq>
        ProtocolTreeNode iq = makeTOSIqNode(gorgeousEngine);
        ProtocolTreeNode trackable = new ProtocolTreeNode("trackable");
        trackable.AddAttribute(new StanzaAttribute("id", "20240729"));
        trackable.AddAttribute(new StanzaAttribute("result", "1"));
        iq.AddChild(trackable);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendAndroidTrackable20240729Hundred(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq to='s.whatsapp.net' type='set' xmlns='tos' id='0c'><trackable id='20401220' result='1'/></iq>
        ProtocolTreeNode iq = makeTOSIqNode(gorgeousEngine);
        ProtocolTreeNode trackable = new ProtocolTreeNode("trackable");
        trackable.AddAttribute(new StanzaAttribute("id", "20240729"));
        trackable.AddAttribute(new StanzaAttribute("result", "100"));
        iq.AddChild(trackable);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendAndroidTrackable20241016One(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq to='s.whatsapp.net' type='set' xmlns='tos' id='0c'><trackable id='20401220' result='1'/></iq>
        ProtocolTreeNode iq = makeTOSIqNode(gorgeousEngine);
        ProtocolTreeNode trackable = new ProtocolTreeNode("trackable");
        trackable.AddAttribute(new StanzaAttribute("id", "20241016"));
        trackable.AddAttribute(new StanzaAttribute("result", "1"));
        iq.AddChild(trackable);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendAndroidTrackable20241016Hundred(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq to='s.whatsapp.net' type='set' xmlns='tos' id='0c'><trackable id='20401220' result='1'/></iq>
        ProtocolTreeNode iq = makeTOSIqNode(gorgeousEngine);
        ProtocolTreeNode trackable = new ProtocolTreeNode("trackable");
        trackable.AddAttribute(new StanzaAttribute("id", "20241016"));
        trackable.AddAttribute(new StanzaAttribute("result", "100"));
        iq.AddChild(trackable);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendAndroidTrackable20250304One(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq to='s.whatsapp.net' type='set' xmlns='tos' id='0c'><trackable id='20401220' result='1'/></iq>
        ProtocolTreeNode iq = makeTOSIqNode(gorgeousEngine);
        ProtocolTreeNode trackable = new ProtocolTreeNode("trackable");
        trackable.AddAttribute(new StanzaAttribute("id", "20250304"));
        trackable.AddAttribute(new StanzaAttribute("result", "1"));
        iq.AddChild(trackable);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendAndroidTrackable20250304Hundred(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq to='s.whatsapp.net' type='set' xmlns='tos' id='0c'><trackable id='20401220' result='1'/></iq>
        ProtocolTreeNode iq = makeTOSIqNode(gorgeousEngine);
        ProtocolTreeNode trackable = new ProtocolTreeNode("trackable");
        trackable.AddAttribute(new StanzaAttribute("id", "20250304"));
        trackable.AddAttribute(new StanzaAttribute("result", "100"));
        iq.AddChild(trackable);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendAndroidTrackable20250501One(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq to='s.whatsapp.net' type='set' xmlns='tos' id='0c'><trackable id='20401220' result='1'/></iq>
        ProtocolTreeNode iq = makeTOSIqNode(gorgeousEngine);
        ProtocolTreeNode trackable = new ProtocolTreeNode("trackable");
        trackable.AddAttribute(new StanzaAttribute("id", "20250501"));
        trackable.AddAttribute(new StanzaAttribute("result", "1"));
        iq.AddChild(trackable);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendAndroidTrackable20250501Hundred(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq to='s.whatsapp.net' type='set' xmlns='tos' id='0c'><trackable id='20401220' result='1'/></iq>
        ProtocolTreeNode iq = makeTOSIqNode(gorgeousEngine);
        ProtocolTreeNode trackable = new ProtocolTreeNode("trackable");
        trackable.AddAttribute(new StanzaAttribute("id", "20250501"));
        trackable.AddAttribute(new StanzaAttribute("result", "100"));
        iq.AddChild(trackable);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendAndroidTrackable20610101One(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq to='s.whatsapp.net' type='set' xmlns='tos' id='0c'><trackable id='20401220' result='1'/></iq>
        ProtocolTreeNode iq = makeTOSIqNode(gorgeousEngine);
        ProtocolTreeNode trackable = new ProtocolTreeNode("trackable");
        trackable.AddAttribute(new StanzaAttribute("id", "20610101"));
        trackable.AddAttribute(new StanzaAttribute("result", "1"));
        iq.AddChild(trackable);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendAndroidTrackable20610101Hundred(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq to='s.whatsapp.net' type='set' xmlns='tos' id='0c'><trackable id='20401220' result='1'/></iq>
        ProtocolTreeNode iq = makeTOSIqNode(gorgeousEngine);
        ProtocolTreeNode trackable = new ProtocolTreeNode("trackable");
        trackable.AddAttribute(new StanzaAttribute("id", "20610101"));
        trackable.AddAttribute(new StanzaAttribute("result", "100"));
        iq.AddChild(trackable);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendAndroidTrackable20610210One(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq to='s.whatsapp.net' type='set' xmlns='tos' id='0c'><trackable id='20401220' result='1'/></iq>
        ProtocolTreeNode iq = makeTOSIqNode(gorgeousEngine);
        ProtocolTreeNode trackable = new ProtocolTreeNode("trackable");
        trackable.AddAttribute(new StanzaAttribute("id", "20610210"));
        trackable.AddAttribute(new StanzaAttribute("result", "1"));
        iq.AddChild(trackable);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendAndroidTrackable20610210Hundred(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq to='s.whatsapp.net' type='set' xmlns='tos' id='0c'><trackable id='20401220' result='1'/></iq>
        ProtocolTreeNode iq = makeTOSIqNode(gorgeousEngine);
        ProtocolTreeNode trackable = new ProtocolTreeNode("trackable");
        trackable.AddAttribute(new StanzaAttribute("id", "20610210"));
        trackable.AddAttribute(new StanzaAttribute("result", "100"));
        iq.AddChild(trackable);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendAndroidTrackable20610220One(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq to='s.whatsapp.net' type='set' xmlns='tos' id='0c'><trackable id='20401220' result='1'/></iq>
        ProtocolTreeNode iq = makeTOSIqNode(gorgeousEngine);
        ProtocolTreeNode trackable = new ProtocolTreeNode("trackable");
        trackable.AddAttribute(new StanzaAttribute("id", "20610220"));
        trackable.AddAttribute(new StanzaAttribute("result", "1"));
        iq.AddChild(trackable);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendAndroidTrackable20610220Hundred(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq to='s.whatsapp.net' type='set' xmlns='tos' id='0c'><trackable id='20401220' result='1'/></iq>
        ProtocolTreeNode iq = makeTOSIqNode(gorgeousEngine);
        ProtocolTreeNode trackable = new ProtocolTreeNode("trackable");
        trackable.AddAttribute(new StanzaAttribute("id", "20610220"));
        trackable.AddAttribute(new StanzaAttribute("result", "100"));
        iq.AddChild(trackable);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendPasskeyExistResponseQuery(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq to='s.whatsapp.net' xmlns='w:mex' type='get' id='029'><trace><flow_id>24221716147467611</flow_id></trace><query query_id='24221716147467611'>{"queryId":"24221716147467611","variables":{}}</query></iq>'}
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        iq.AddAttribute(new StanzaAttribute("xmlns", "w:mex"));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        ProtocolTreeNode trace = new ProtocolTreeNode("trace");
        ProtocolTreeNode flowId = new ProtocolTreeNode("flow_id");
        flowId.SetData("24221716147467611".getBytes(StandardCharsets.UTF_8));
        trace.AddChild(flowId);
        iq.AddChild(trace);
        ProtocolTreeNode query = new ProtocolTreeNode("query");
        //GetInviteSenderInfo
        query.AddAttribute(new StanzaAttribute("query_id", "24221716147467611"));
        query.SetData("{\"queryId\":\"24221716147467611\",\"variables\":{}}".getBytes(StandardCharsets.UTF_8));
        iq.AddChild(query);
        gorgeousEngine.SetIqId(iq);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendQueryParticipatingGroups(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq to='s.whatsapp.net' xmlns='w:mex' type='get' id='02a'><trace><flow_id>23940562365561484</flow_id></trace><query query_id='23940562365561484'>{"queryId":"23940562365561484","variables":{"input":{}}}</query></iq>
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        iq.AddAttribute(new StanzaAttribute("xmlns", "w:mex"));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        ProtocolTreeNode trace = new ProtocolTreeNode("trace");
        ProtocolTreeNode flowId = new ProtocolTreeNode("flow_id");
        flowId.SetData("23940562365561484".getBytes(StandardCharsets.UTF_8));
        trace.AddChild(flowId);
        iq.AddChild(trace);
        ProtocolTreeNode query = new ProtocolTreeNode("query");
        //GetInviteSenderInfo
        query.AddAttribute(new StanzaAttribute("query_id", "23940562365561484"));
        query.SetData("{\"queryId\":\"23940562365561484\",\"variables\":{\"input\":{}}}".getBytes(StandardCharsets.UTF_8));
        iq.AddChild(query);
        gorgeousEngine.SetIqId(iq);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendGetDynamicRegistrationUpsells(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq to='s.whatsapp.net' xmlns='w:mex' type='get' id='03a'><trace><flow_id>10043673822314027</flow_id></trace><query query_id='10043673822314027'>{"queryId":"10043673822314027","variables":{}}</query></iq>'}
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        iq.AddAttribute(new StanzaAttribute("xmlns", "w:mex"));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        ProtocolTreeNode trace = new ProtocolTreeNode("trace");
        ProtocolTreeNode flowId = new ProtocolTreeNode("flow_id");
        flowId.SetData("10043673822314027".getBytes(StandardCharsets.UTF_8));
        trace.AddChild(flowId);
        iq.AddChild(trace);
        ProtocolTreeNode query = new ProtocolTreeNode("query");
        //GetInviteSenderInfo
        query.AddAttribute(new StanzaAttribute("query_id", "10043673822314027"));
        query.SetData("{\"queryId\":\"10043673822314027\",\"variables\":{}}".getBytes(StandardCharsets.UTF_8));
        iq.AddChild(query);
        gorgeousEngine.SetIqId(iq);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendContactsBackupQuery(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq to='s.whatsapp.net' xmlns='w:mex' type='get' id='03c'><trace><flow_id>7307717639327756</flow_id></trace><query query_id='7307717639327756'>{"queryId":"7307717639327756","variables":{"input":{"query_input":[{"jid":"12318830493@s.whatsapp.net"}]}}}</query></iq>'}
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        iq.AddAttribute(new StanzaAttribute("xmlns", "w:mex"));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        ProtocolTreeNode trace = new ProtocolTreeNode("trace");
        ProtocolTreeNode flowId = new ProtocolTreeNode("flow_id");
        flowId.SetData("7307717639327756".getBytes(StandardCharsets.UTF_8));
        trace.AddChild(flowId);
        iq.AddChild(trace);
        ProtocolTreeNode query = new ProtocolTreeNode("query");
        //GetInviteSenderInfo
        query.AddAttribute(new StanzaAttribute("query_id", "7307717639327756"));
        String queryInput = "{\"queryId\":\"7307717639327756\",\"variables\":{\"input\":{\"query_input\":[{\"jid\":\"" + JidNormalize(gorgeousEngine.getUsername()) + "\"}]}}}";
        query.SetData(queryInput.getBytes(StandardCharsets.UTF_8));
        iq.AddChild(query);
        gorgeousEngine.SetIqId(iq);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendGetInviteInfo(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq to='s.whatsapp.net' xmlns='w:mex' type='get' id='04d'><trace><flow_id>9274981439273619</flow_id></trace><query query_id='9274981439273619'>{"queryId":"9274981439273619","variables":{}}</query></iq>'}
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        iq.AddAttribute(new StanzaAttribute("xmlns", "w:mex"));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        ProtocolTreeNode trace = new ProtocolTreeNode("trace");
        ProtocolTreeNode flowId = new ProtocolTreeNode("flow_id");
        flowId.SetData("9274981439273619".getBytes(StandardCharsets.UTF_8));
        trace.AddChild(flowId);
        iq.AddChild(trace);
        ProtocolTreeNode query = new ProtocolTreeNode("query");
        //GetInviteSenderInfo
        query.AddAttribute(new StanzaAttribute("query_id", "9274981439273619"));
        query.SetData("{\"queryId\":\"9274981439273619\",\"variables\":{}}".getBytes(StandardCharsets.UTF_8));
        iq.AddChild(query);
        gorgeousEngine.SetIqId(iq);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendUsyncQuery(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq to='s.whatsapp.net' xmlns='w:mex' type='get' id='051'><trace><flow_id>24421607300823808</flow_id></trace><query query_id='24421607300823808'>{"queryId":"24421607300823808","variables":{"include_lid":true,"include_linked_profiles":true,"input":{"query_input":[{"jid":"214250802946133@lid"}],"telemetry":{"context":"INTERACTIVE"}}}}</query></iq>'}
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        iq.AddAttribute(new StanzaAttribute("xmlns", "w:mex"));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        ProtocolTreeNode trace = new ProtocolTreeNode("trace");
        ProtocolTreeNode flowId = new ProtocolTreeNode("flow_id");
        flowId.SetData("24421607300823808".getBytes(StandardCharsets.UTF_8));
        trace.AddChild(flowId);
        iq.AddChild(trace);
        ProtocolTreeNode query = new ProtocolTreeNode("query");
        //GetInviteSenderInfo
        query.AddAttribute(new StanzaAttribute("query_id", "24421607300823808"));
        String queryInput;
        if (!gorgeousEngine.isBusinessVersion()) {
            queryInput = "{\"queryId\":\"24421607300823808\",\"variables\":{\"include_lid\":true,\"include_linked_profiles\":true,\"input\":{\"query_input\":[{\"jid\":\"" + gorgeousEngine.getUserLid() + "\"}],\"telemetry\":{\"context\":\"INTERACTIVE\"}}}}";
        } else {
            HashSet<String> hashSet = new HashSet<>();
            hashSet.add(XmppJid.of(gorgeousEngine.getUserId()).toLongString());
            queryInput = "{\"queryId\":\"24421607300823808\",\"variables\":{\"include_devices\":true,\"input\":{\"query_input\":[{\"devices\":{\"hash\":\"" + CryptUtil.PHash(hashSet) + "\"},\"jid\":\"" + gorgeousEngine.getUserLid() + "\"}],\"telemetry\":{\"context\":\"NOTIFICATION\"}}}}";
        }
        query.SetData(queryInput.getBytes(StandardCharsets.UTF_8));
        iq.AddChild(query);
        gorgeousEngine.SetIqId(iq);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendSelfContactsQuery(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq to='s.whatsapp.net' xmlns='w:mex' type='get' id='057'><trace><flow_id>9982837645142393</flow_id></trace><query query_id='9982837645142393'>{"queryId":"9982837645142393","variables":{"batch_size":3000,"include_encrypted_metadata_v2":false,"include_lid_info":true,"input":{"query_input":[{"jid":"12318830493@s.whatsapp.net"}],"telemetry":{"context":"REGISTRATION"}}}}</query></iq>
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        iq.AddAttribute(new StanzaAttribute("xmlns", "w:mex"));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        ProtocolTreeNode trace = new ProtocolTreeNode("trace");
        ProtocolTreeNode flowId = new ProtocolTreeNode("flow_id");
        flowId.SetData("9982837645142393".getBytes(StandardCharsets.UTF_8));
        trace.AddChild(flowId);
        iq.AddChild(trace);
        ProtocolTreeNode query = new ProtocolTreeNode("query");
        //GetInviteSenderInfo
        query.AddAttribute(new StanzaAttribute("query_id", "9982837645142393"));
        String inputQuery = "{\"queryId\":\"9982837645142393\",\"variables\":{\"batch_size\":3000,\"include_encrypted_metadata_v2\":false,\"include_lid_info\":true,\"input\":{\"query_input\":[{\"jid\":\"" + JidNormalize(gorgeousEngine.getUsername()) + "\"}],\"telemetry\":{\"context\":\"REGISTRATION\"}}}}";
        query.SetData(inputQuery.getBytes(StandardCharsets.UTF_8));
        iq.AddChild(query);
        gorgeousEngine.SetIqId(iq);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendQueryBlockingStatus(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq to='s.whatsapp.net' xmlns='w:comms:chat' id='046' type='get'><query><blocking_status/></query></iq>
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        iq.AddAttribute(new StanzaAttribute("xmlns", "w:comms:chat"));
        iq.AddAttribute(new StanzaAttribute("id", gorgeousEngine.GenerateIqId()));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        ProtocolTreeNode query = new ProtocolTreeNode("query");
        ProtocolTreeNode blockingStatus = new ProtocolTreeNode("blocking_status");
        query.AddChild(blockingStatus);
        iq.AddChild(query);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendGetOptOutList(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq to='s.whatsapp.net' xmlns='optoutlist' type='get' id='04f'/>
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        iq.AddAttribute(new StanzaAttribute("xmlns", "optoutlist"));
        iq.AddAttribute(new StanzaAttribute("id", gorgeousEngine.GenerateIqId()));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendGetWBizBusinessProfile(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq id='08' xmlns='w:biz' type='get'><business_profile v='1908'><profile jid='12706345611@s.whatsapp.net'/><settings/><server_configs/></business_profile></iq>
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("id", gorgeousEngine.GenerateIqId()));
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        iq.AddAttribute(new StanzaAttribute("xmlns", "w:biz"));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        ProtocolTreeNode businessProfile = new ProtocolTreeNode("business_profile");
        businessProfile.AddAttribute(new StanzaAttribute("v", "1908"));
        ProtocolTreeNode profile = new ProtocolTreeNode("profile");
        profile.AddAttribute(new StanzaAttribute("jid", gorgeousEngine.getUserId()));
        businessProfile.AddChild(profile);
        businessProfile.AddChild(new ProtocolTreeNode("settings"));
        businessProfile.AddChild(new ProtocolTreeNode("server_configs"));
        iq.AddChild(businessProfile);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendDeleteAllData(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq to='s.whatsapp.net' xmlns='w:sync:app:state' type='set' id='014'><delete_all_data/></iq>
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        iq.AddAttribute(new StanzaAttribute("xmlns", "w:sync:app:state"));
        iq.AddAttribute(new StanzaAttribute("id", gorgeousEngine.GenerateIqId()));
        iq.AddAttribute(new StanzaAttribute("type", "set"));
        iq.AddChild(new ProtocolTreeNode("delete_all_data"));
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendWBiz139(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq xmlns='w:biz' smax_id='139' to='s.whatsapp.net' type='get' id='015'><features meta_verified='true' marketing_messages='false'/></iq>
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("smax_id", "139"));
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        iq.AddAttribute(new StanzaAttribute("xmlns", "w:biz"));
        iq.AddAttribute(new StanzaAttribute("id", gorgeousEngine.GenerateIqId()));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        ProtocolTreeNode features = new ProtocolTreeNode("features");
        features.AddAttribute(new StanzaAttribute("meta_verified", "true"));
        features.AddAttribute(new StanzaAttribute("marketing_messages", "false"));
        iq.AddChild(features);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendThriftIq118(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq xmlns='fb:thrift_iq' smax_id='118' to='s.whatsapp.net' type='get' from='12706345611@s.whatsapp.net' id='01b'/>
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        iq.AddAttribute(new StanzaAttribute("xmlns", "fb:thrift_iq"));
        iq.AddAttribute(new StanzaAttribute("smax_id", "118"));
        iq.AddAttribute(new StanzaAttribute("id", gorgeousEngine.GenerateIqId()));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        iq.AddAttribute(new StanzaAttribute("from", gorgeousEngine.getUserId()));
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendAndroidTrackable20250331One(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq to='s.whatsapp.net' type='set' xmlns='tos' id='0c'><trackable id='20401220' result='1'/></iq>
        ProtocolTreeNode iq = makeTOSIqNode(gorgeousEngine);
        ProtocolTreeNode trackable = new ProtocolTreeNode("trackable");
        trackable.AddAttribute(new StanzaAttribute("id", "20610220"));
        trackable.AddAttribute(new StanzaAttribute("result", "1"));
        iq.AddChild(trackable);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendAndroidTrackable20250331Hundred(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq to='s.whatsapp.net' type='set' xmlns='tos' id='0c'><trackable id='20401220' result='1'/></iq>
        ProtocolTreeNode iq = makeTOSIqNode(gorgeousEngine);
        ProtocolTreeNode trackable = new ProtocolTreeNode("trackable");
        trackable.AddAttribute(new StanzaAttribute("id", "20610220"));
        trackable.AddAttribute(new StanzaAttribute("result", "100"));
        iq.AddChild(trackable);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendThriftIqCatKit(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq to='s.whatsapp.net' type='get' id='020' xmlns='fb:thrift_iq'><request type='catkit' op='profile_typeahead' v='1'><query></query></request></iq>
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        iq.AddAttribute(new StanzaAttribute("xmlns", "fb:thrift_iq"));
        iq.AddAttribute(new StanzaAttribute("id", gorgeousEngine.GenerateIqId()));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        ProtocolTreeNode request = new ProtocolTreeNode("request");
        request.AddAttribute(new StanzaAttribute("type", "catkit"));
        request.AddAttribute(new StanzaAttribute("op", "profile_typeahead"));
        request.AddAttribute(new StanzaAttribute("v", "1"));
        request.AddChild(new ProtocolTreeNode("query"));
        iq.AddChild(request);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    /**
     * 商业版设置verifiedName
     */
    public void sendSetVerifiedName(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq id='02b' xmlns='w:biz' type='set' to='s.whatsapp.net'><verified_name v='2'>ChgI/qeeybWm4qoWEgZzbWI6d2EiBFBybzMSQJTkYycKYfnRhi3B0guZZJ60zVpgS+NFvh1wzvqAYBN1RHjiOk3idQHSuKscn/9h9/2ptUjbg3HLK4SrEcQegAQ=</verified_name></iq>
        String pushName = gorgeousEngine.getEnvBuilder_().getPushname();
        Random random = new SecureRandom();
        long abs = Math.abs(random.nextLong());
        WhatsMessage.Verified.VerifiedOne v1 = WhatsMessage.Verified.VerifiedOne.newBuilder()
                .setVerifiedOne1(abs)
                .setVerifiedOne2("smb:wa")
                .setVerifiedOne4(pushName).build();  //最后一个是昵称参数
        //初始化数据表, 加密需要获取 privatekey
        byte[] byteSign = Curve25519.getInstance(BEST)
                .calculateSignature(gorgeousEngine.axolotlManager_.GetIdentityKeyPair().getPrivateKey().serialize(), v1.toByteArray());
        WhatsMessage.Verified verified = WhatsMessage.Verified.newBuilder().setVerifiedOne(v1).setVerifiedTow(ByteString.copyFrom(byteSign)).build();
        byte[] verifyName = verified.toByteArray();
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("id", gorgeousEngine.GenerateIqId()));
        iq.AddAttribute(new StanzaAttribute("xmlns", "w:biz"));
        iq.AddAttribute(new StanzaAttribute("type", "set"));
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        ProtocolTreeNode verifiedNameNode = new ProtocolTreeNode("verified_name");
        verifiedNameNode.AddAttribute(new StanzaAttribute("v", "2"));
        verifiedNameNode.SetData(verifyName);
        iq.AddChild(verifiedNameNode);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendSetBusinessCategory(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("xmlns", "w:biz"));
        iq.AddAttribute(new StanzaAttribute("type", "set"));
        iq.AddAttribute(new StanzaAttribute("id", gorgeousEngine.GenerateIqId()));
        ProtocolTreeNode businessProfile = new ProtocolTreeNode("business_profile");
        businessProfile.AddAttribute(new StanzaAttribute("v", "1908"));
        ProtocolTreeNode categories = new ProtocolTreeNode("categories");
        ProtocolTreeNode category = new ProtocolTreeNode("category");
        category.AddAttribute(new StanzaAttribute("id", RandomUtil.randomEle(businessCategoryId)));
        categories.AddChild(category);
        ProtocolTreeNode businessHours = new ProtocolTreeNode("business_hours");
        businessHours.AddAttribute(new StanzaAttribute("timezone", RandomUtil.randomEle(worldTimeZone)));
        ProtocolTreeNode sun = new ProtocolTreeNode("business_hours_config");
        sun.AddAttribute(new StanzaAttribute("day_of_week", "sun"));
        sun.AddAttribute(new StanzaAttribute("mode", "open_24h"));
        businessHours.AddChild(sun);
        ProtocolTreeNode mon = new ProtocolTreeNode("business_hours_config");
        mon.AddAttribute(new StanzaAttribute("day_of_week", "mon"));
        mon.AddAttribute(new StanzaAttribute("mode", "open_24h"));
        businessHours.AddChild(mon);
        ProtocolTreeNode tue = new ProtocolTreeNode("business_hours_config");
        tue.AddAttribute(new StanzaAttribute("day_of_week", "tue"));
        tue.AddAttribute(new StanzaAttribute("mode", "open_24h"));
        businessHours.AddChild(tue);
        ProtocolTreeNode wed = new ProtocolTreeNode("business_hours_config");
        wed.AddAttribute(new StanzaAttribute("day_of_week", "wed"));
        wed.AddAttribute(new StanzaAttribute("mode", "open_24h"));
        businessHours.AddChild(wed);
        ProtocolTreeNode thu = new ProtocolTreeNode("business_hours_config");
        thu.AddAttribute(new StanzaAttribute("day_of_week", "thu"));
        thu.AddAttribute(new StanzaAttribute("mode", "open_24h"));
        businessHours.AddChild(thu);
        ProtocolTreeNode fri = new ProtocolTreeNode("business_hours_config");
        fri.AddAttribute(new StanzaAttribute("day_of_week", "fri"));
        fri.AddAttribute(new StanzaAttribute("mode", "open_24h"));
        businessHours.AddChild(fri);
        ProtocolTreeNode sat = new ProtocolTreeNode("business_hours_config");
        sat.AddAttribute(new StanzaAttribute("day_of_week", "sat"));
        sat.AddAttribute(new StanzaAttribute("mode", "open_24h"));
        businessHours.AddChild(sat);
        businessProfile.AddChild(categories);
        businessProfile.AddChild(businessHours);
        iq.AddChild(businessProfile);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendWBiz109(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq id='02f' type='get' to='s.whatsapp.net' smax_id='109' xmlns='w:biz'><privacy/></iq>
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("smax_id", "109"));
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        iq.AddAttribute(new StanzaAttribute("xmlns", "w:biz"));
        iq.AddAttribute(new StanzaAttribute("id", gorgeousEngine.GenerateIqId()));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        iq.AddChild(new ProtocolTreeNode("privacy"));
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendBusinessProfileMyself(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq id='03d' xmlns='w:biz' type='get'><verified_name jid='639658318158@s.whatsapp.net'/></iq>
        gorgeousEngine.getVerifiedName();
        consumer.accept(true);
    }

    private void sendSMAXId42Request(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq id='016' xmlns='fb:thrift_iq' type='get' smax_id='42' to='s.whatsapp.net'><linked_accounts/></iq>
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("id", gorgeousEngine.GenerateIqId()));
        iq.AddAttribute(new StanzaAttribute("xmlns", "fb:thrift_iq"));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        iq.AddAttribute(new StanzaAttribute("smax_id", "42"));
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        ProtocolTreeNode linkedAccounts = new ProtocolTreeNode("linked_accounts");
        iq.AddChild(linkedAccounts);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });

    }

    public void sendAndroidTrackable20601227One(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq to='s.whatsapp.net' type='set' xmlns='tos' id='0c'><trackable id='20401220' result='1'/></iq>
        ProtocolTreeNode iq = makeTOSIqNode(gorgeousEngine);
        ProtocolTreeNode trackable = new ProtocolTreeNode("trackable");
        trackable.AddAttribute(new StanzaAttribute("id", "20601227"));
        trackable.AddAttribute(new StanzaAttribute("result", "1"));
        iq.AddChild(trackable);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendAndroidTrackable20601227Hundred(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq to='s.whatsapp.net' type='set' xmlns='tos' id='0c'><trackable id='20401220' result='1'/></iq>
        ProtocolTreeNode iq = makeTOSIqNode(gorgeousEngine);
        ProtocolTreeNode trackable = new ProtocolTreeNode("trackable");
        trackable.AddAttribute(new StanzaAttribute("id", "20601227"));
        trackable.AddAttribute(new StanzaAttribute("result", "100"));
        iq.AddChild(trackable);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendAndroidTrackable20601228One(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq to='s.whatsapp.net' type='set' xmlns='tos' id='0c'><trackable id='20401220' result='1'/></iq>
        ProtocolTreeNode iq = makeTOSIqNode(gorgeousEngine);
        ProtocolTreeNode trackable = new ProtocolTreeNode("trackable");
        trackable.AddAttribute(new StanzaAttribute("id", "20601228"));
        trackable.AddAttribute(new StanzaAttribute("result", "1"));
        iq.AddChild(trackable);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

    public void sendAndroidTrackable20601228Hundred(GorgeousEngine gorgeousEngine, Consumer<Boolean> consumer) {
        // <iq to='s.whatsapp.net' type='set' xmlns='tos' id='0c'><trackable id='20401220' result='1'/></iq>
        ProtocolTreeNode iq = makeTOSIqNode(gorgeousEngine);
        ProtocolTreeNode trackable = new ProtocolTreeNode("trackable");
        trackable.AddAttribute(new StanzaAttribute("id", "20601228"));
        trackable.AddAttribute(new StanzaAttribute("result", "100"));
        iq.AddChild(trackable);
        gorgeousEngine.AddTask(iq, (srcNode, result) -> {
            consumer.accept(true);
        });
    }

}
