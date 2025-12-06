package com.whatsapp.android;

import Env.DeviceEnv;
import Handshake.NoiseHandshake;
import Message.WhatsMessage;
import ProtocolTree.ProtocolTreeNode;
import ProtocolTree.StanzaAttribute;
import ProtocolTree.XmppJid;
import QRcode.*;
import Report.IdentifiedTelemetry;
import Util.CryptUtil;
import Util.StringUtil;
import Wam.Wam;
import Wam.proto.WamRecord;
import axolotl.AxolotlManager;
import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.*;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.imx.apns.activate.ActivationInfo;
import com.imx.apns.common.APNsState;
import com.imx.common.Pair;
import com.imx.common.util.ByteUtil;
import com.imx.netty.core.APNsClientProcessor;
import com.imx.netty.core.FutureNotification;
import com.imx.netty.core.PayloadResult;
import com.southernstorm.noise.protocol.DHState;
import com.southernstorm.noise.protocol.Noise;
import com.whatsapp.android.config.DelayExecuteTask;
import com.whatsapp.android.config.ThreadPoolConfig;
import com.whatsapp.android.constant.CacheConstants;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.crypto.MediaCipher;
import com.whatsapp.android.entity.*;
import com.whatsapp.android.entity.pack.message.VoipCallPack;
import com.whatsapp.android.entity.pack.profile.ScanWebSyncStatus;
import com.whatsapp.android.entity.response.contact.ContactInfoResult;
import com.whatsapp.android.entity.response.message.VoipAcceptResult;
import com.whatsapp.android.enums.NodeTaskType;
import com.whatsapp.android.metrics.MessagingMetrics;
import com.whatsapp.android.protocol.contact.ContactProtocol;
import com.whatsapp.android.run.StartedUpRunner;
import com.whatsapp.android.service.AsyncMessageService;
import com.whatsapp.android.service.GcmPnResultService;
import com.whatsapp.android.service.WaOldRegistrationService;
import com.whatsapp.android.service.impl.call.TE2IpParser;
import com.whatsapp.android.service.impl.init.InitExecuteTask;
import com.whatsapp.android.service.impl.wam.WamReportService;
import com.whatsapp.android.service.impl.wam.WhatsAppContextInfoParseService;
import com.whatsapp.android.util.*;
import com.whatsapp.android.util.cron.CronUtil;
import com.whatsapp.android.util.cron.task.Task;
import com.whatsapp.android.util.ffmpeg.MultimediaInfo;
import com.whatsapp.android.ws.WebSocketClient;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import jni.GCMLogin;
import jni.GCMService;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.core.StandardThreadExecutor;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.microg.gms.gcm.mcs.Mcs;
import org.springframework.util.StringUtils;
import org.whispersystems.curve25519.Curve25519;
import org.whispersystems.curve25519.NativeVOPRFExtension;
import org.whispersystems.libsignal.*;
import org.whispersystems.libsignal.ecc.Curve;
import org.whispersystems.libsignal.ecc.ECPublicKey;
import org.whispersystems.libsignal.groups.SenderKeyName;
import org.whispersystems.libsignal.groups.state.SenderKeyRecord;
import org.whispersystems.libsignal.groups.state.SenderKeyState;
import org.whispersystems.libsignal.protocol.CiphertextMessage;
import org.whispersystems.libsignal.protocol.SenderKeyDistributionMessage;
import org.whispersystems.libsignal.state.PreKeyBundle;
import org.whispersystems.libsignal.state.PreKeyRecord;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.whatsapp.android.crypto.ReportToken.generateEncryptProto;
import static com.whatsapp.android.crypto.ReportToken.generateEncryptToken;
import static com.whatsapp.android.util.RandomCategoryUtil.getRandomCategory;
import static org.whispersystems.curve25519.Curve25519.BEST;

@Slf4j
public class GorgeousEngine implements NoiseHandshake.HandshakeNotify, GCMLogin.GcmEvent, FutureNotification<PayloadResult> {
    private static final Set<String> MESSAGE_TYPE = new ConcurrentHashSet<>();
    private static final Set<String> MESSAGE_MEDIA_TYPE = new ConcurrentHashSet<>();
    private static final Set<String> NOTIFICATION_TYPE = new ConcurrentHashSet<>();

    static {
        MESSAGE_TYPE.addAll(Stream
                .of("text", "media", "offer", "terminate", "reject", "reaction", "event", "poll")
                .collect(Collectors.toSet()));

        MESSAGE_MEDIA_TYPE.addAll(Stream
                .of("url", "image", "ptt", "audio", "video",
                        "document", "location", "contact", "vcard", "gif",
                        "sticker", "1p_sticker", "avatar_sticker", "contact_array")
                .collect(Collectors.toSet()));

        NOTIFICATION_TYPE.addAll(Stream
                .of("encrypt", "devices", "account_sync", "registration", "mex", "w:gp2", "picture", "privacy_token", "contacts", "business", "psa")
                .collect(Collectors.toSet()));
    }


    /**
     * 小号账号
     */
    @Getter
    @Setter
    private String username;
    /**
     * 用户id
     */
    @Getter
    @Setter
    private String userId;
    public GorgeousEngineDelegate delegate_;
    String cdnAuthKey_;
    String cdnHost_;
    @Getter
    private NoiseHandshake noiseHandshake_ = null;
    private String configPath_;
    NoiseHandshake.Proxy proxy_;
    public AxolotlManager axolotlManager_;
    @Getter
    DeviceEnv.AndroidEnv.Builder envBuilder_;
    /**
     * 心跳定时任务id
     */
    private String heartBeatScheduleId;
    /**
     * cdn定时更新任务id
     */
    private String getCdnInfoScheduleId;
    /**
     * 心跳计次
     */
    private AtomicInteger heartbeat = new AtomicInteger();
    /**
     * 心跳任务id
     */
    private String heartbeatTaskId;
    @Getter
    private boolean iosLogin;
    @Getter
    private boolean businessVersion;
    private int pingId = 0;
    /**
     * 账号登陆成功时间
     */
    @Getter
    private long loginTime;
    /**
     * 账号注册时间
     */
    @Getter
    private String creationTime;
    @Getter
    private String waVersion;
    private String loginWaVersion;
    /**
     * 是否要执行初始化
     */
    private boolean executeInit;
    /**
     * 是否是劫持号模式
     */
    private boolean hijackMode;
    @Getter
    private MccMnc mccMnc;
    /**
     * ws返回的登录地址
     */
    @Getter
    private String location;
    @Getter
    private String abKey2;
    @Getter
    private String expoKey;
    /**
     * 用户lid
     */
    @Getter
    private String userLid;
    /**
     * unified_session
     */
    @Getter
    private String unifiedSession;

    /**
     * 扫码同步状态
     */
    @Getter
    private final ScanWebSyncStatus scanWebSyncStatus = new ScanWebSyncStatus();
    @Getter
    private GCMLogin gcmLogin;
    @Getter
    private AtomicBoolean gcmOnline = new AtomicBoolean(false);
    private AtomicInteger gcmConnectCount = new AtomicInteger();

    private AtomicBoolean gcmStop = new AtomicBoolean(false);

    private TaskNotify taskNotify;
    /**
     * 掉线事件防止重复
     */
    private final AtomicInteger preventDuplication = new AtomicInteger(0);

    /**
     * 最后一条pn
     */
    @Getter
    private String lastPn;
    /**
     * 最后收到pn时间
     */
    private String lastPnTime;
    /**
     * 锁定结束时间
     */
    @Getter
    private String timeLockEndTime;
    private static final HashedWheelTimer hashedWheelTimer = new HashedWheelTimer(10, TimeUnit.MILLISECONDS);
    private static final String[] ANDROID_VERSION = {"8.1.0", "9", "10"};
    private static final List<String> TOPICS = Collections.singletonList("net.whatsapp.WhatsApp");
    @Getter
    private APNsClientProcessor apNsClientManager;
    /**
     * 通知服务超时监听
     */
    private Timeout handlePlatformNotificationTimeOut;
    /**
     * iOS同步通讯录id
     */
    @Getter
    private AtomicInteger syncId = new AtomicInteger();
    @Setter
    private byte[] gcmCatValue;
    /**
     * 是否初始化
     */
    private boolean init;
    /**
     * 是否初始化完成
     */
    private boolean initCompleted;
    /**
     * 初始化任务执行
     */
    private InitExecuteTask initExecuteTask;
    /**
     * 唯一登录id
     */
    private String loginId;
    /**
     * apnsState
     */
    private APNsState apnsState;
    /**
     * gcmToken
     */
    private GcmTokenResult gcmToken;
    /**
     * 订阅记录
     */
    @Getter
    private final UserIdSubscribeRecord userIdSubscribeRecord = new UserIdSubscribeRecord();

    @Override
    public void OnGcmConnectSuccess() {
        log.info("用户：{}，gcm连接成功", username);
        gcmOnline.set(true);
        gcmConnectCount.set(0);
    }

    @Override
    public void OnGcmMessage(int mcsTag, byte[] message, int streamId) {
        // 处理完整的消息
        log.debug("用户：{}，Gcm Tag：{}，Received message: {}", username, mcsTag, HexUtil.encodeHexStr(message));
        ThreadPoolConfig.xmppPoolExecutor.execute(() -> {
            if (mcsTag == Constant.MCS_CLOSE_TAG) {
                /*try {
                    KeyLockUtil.lock(username);
                    axolotlManager_.delSetting("gcm_cat");
                } catch (Exception ignore) {
                } finally {
                    KeyLockUtil.unlock(username);
                }*/
            } else if (mcsTag == Constant.MCS_DATA_MESSAGE_STANZA_TAG) {
                try {
                    Mcs.DataMessageStanza dataMessageStanza = Mcs.DataMessageStanza.parseFrom(message);
                    log.debug("用户: {}, GCM message: {}", username, dataMessageStanza);
                    for (Mcs.AppData appData : dataMessageStanza.getAppDataList()) {
                        if ("pn".equals(appData.getKey())) {
                            String pn = appData.getValue();
                            if (pn.equals(this.lastPn)) {
                                //pn重复，不做处理
                                return;
                            }
                            this.lastPn = pn;
                            this.lastPnTime = DateUtil.now();
                            log.info("用户：{}，收到pn值：{}", username, pn);
                            GcmPnResultService.handlerPn(username, pn);
                        }
                    }
                    String persistentId = dataMessageStanza.getPersistentId();
                    if (StringUtils.hasLength(persistentId)) {
                        try {
                            KeyLockUtil.lock(username);
                            axolotlManager_.setGcmPersistentIdValue(persistentId);
                        } catch (Exception ignore) {
                        } finally {
                            KeyLockUtil.unlock(username);
                        }
                    }
                    gcmLogin.sendMsgAck(streamId);
                } catch (Exception ignored) {
                }
            }
        });
    }

    @Override
    public void OnGcmClose(String reason, boolean force) {
        log.info("用户：{}，gcm连接断开：{}", username, reason);
        gcmOnline.set(false);
        /*if (gcmStop.get()) {
            return;
        }
        int connectCount = gcmConnectCount.getAndIncrement();
        if (connectCount > 10) {
            //连接失败超出次数，停止连接
            gcmStop.set(true);
            return;
        }
        if (ObjectUtil.isNotNull(this.gcmToken)) {
            gcmConnect(this.gcmToken);
        }*/
    }

    @Override
    public void OnApnsConnectSuccess() {
        log.info("用户: {}, Apns连接成功", username);
        gcmOnline.set(true);
        gcmConnectCount.set(0);
    }

    @Override
    public void OnApnsNotify(PayloadResult payloadResult) {
        log.info("APNS Received notification: {}", JSONObject.toJSONString(payloadResult));
        ThreadPoolConfig.xmppPoolExecutor.execute(() -> {
            try {
                String payload = new String(payloadResult.getNotification().getPayload());
                JSONObject jsonObject = JSONObject.parseObject(payload);
                if (log.isDebugEnabled()) {
                    log.debug("用户: {}, APNS payload: {}", username, payload);
                }
                String pn = jsonObject.getString("pn");
                if (StringUtils.hasLength(pn)) {
                    if (pn.equals(this.lastPn)) {
                        //pn重复，不做处理
                        return;
                    }
                    this.lastPn = pn;
                    this.lastPnTime = DateUtil.now();
                    log.info("用户：{}，收到pn值：{}", username, pn);
                    GcmPnResultService.handlerPn(username, pn);
                }
            } catch (Exception ignored) {
            }
        });
    }

    @Override
    public void OnApnsState(APNsState apnsState) {
        //发送xmpp认证
        this.apnsState = apnsState;
        ThreadPoolConfig.xmppPoolExecutor.execute(() -> {
            byte[] token = this.apnsState.getToken();
            String id = HexUtil.encodeHexStr(token);
            sendGcm(id);
            try {
                KeyLockUtil.lock(username);
                String state = HexUtil.encodeHexStr(ByteUtil.serialize(this.apnsState));
                axolotlManager_.setApnsParamsValue(state);
            } catch (Exception e) {
                log.error("用户: {}, 存储APNS认证信息异常", username);
            } finally {
                KeyLockUtil.unlock(username);
            }
        });
    }

    @Override
    public void OnApnsClose(String reason, boolean force) {
        log.info("用户: {}, Apns连接断开: {}", username, reason);
        gcmOnline.set(false);
        /*if (gcmStop.get()) {
            return;
        }
        int connectCount = gcmConnectCount.getAndIncrement();
        if (connectCount > 10) {
            //连接失败超出次数，停止连接
            gcmStop.set(true);
            return;
        }
        if (ObjectUtil.isNotNull(this.apnsState)) {
            apnsConnect(this.apnsState);
        }*/
    }

    public interface GorgeousEngineDelegate {
        void OnLogin(int code, ProtocolTreeNode desc, String loginId);

        void OnDisconnect(String desc, String loginId);

        void OnSync(ProtocolTreeNode content);

        void OnPresence(ProtocolTreeNode content);

        void OnPacketResponse(String type, ProtocolTreeNode content);

        void taskNotify(GorgeousEngine gorgeousEngine, String username, Runnable runnable);

    }

    public interface NodeCallback {
        void Run(ProtocolTreeNode srcNode, ProtocolTreeNode result);
    }


    public static class NodeHandleInfo {
        NodeCallback callbackRunnable;
        ProtocolTreeNode srcNode;

        Timeout timeout;

        NodeHandleInfo(NodeCallback callback, ProtocolTreeNode srcNode) {
            this.callbackRunnable = callback;
            this.srcNode = srcNode;
        }

        NodeHandleInfo(NodeCallback callback, ProtocolTreeNode srcNode, Timeout timeout) {
            this.callbackRunnable = callback;
            this.srcNode = srcNode;
            this.timeout = timeout;
        }
    }


    public GorgeousEngine(String username, String configPath, GorgeousEngineDelegate delegate, NoiseHandshake.Proxy proxy, MccMnc mccMnc, String waVersion, boolean executeInit, boolean hijackMode) {
        this.username = username;
        configPath_ = configPath;
        delegate_ = delegate;
        proxy_ = proxy;
        this.mccMnc = mccMnc;
        this.loginWaVersion = waVersion;
        this.executeInit = executeInit;
        this.hijackMode = hijackMode;
    }

    public boolean StartEngine(boolean iosLogin, boolean businessVersion, TaskNotify taskNotify, String loginId) {
        this.iosLogin = iosLogin;
        this.businessVersion = businessVersion;
        this.taskNotify = taskNotify;
        this.loginId = loginId;
        axolotlManager_ = new AxolotlManager(configPath_, username);
        byte[] envBuffer;
        try {
            try {
                KeyLockUtil.lock(username);
                envBuffer = axolotlManager_.GetBytesSetting("env");
                this.lastPn = axolotlManager_.getLastPn();
                this.gcmCatValue = axolotlManager_.getGcmCatValue();
                this.timeLockEndTime = axolotlManager_.getTimeLockEndTime();
                this.init = axolotlManager_.isInit();
                if (!init && hijackMode) {
                    init = true;
                    initCompleted = true;
                    axolotlManager_.initCompleted();
                } else {
                    if (!init && executeInit) {
                        //入库初始化流程
                        axolotlManager_.taskQueueManager.addInitTask(iosLogin, businessVersion);
                    }
                    this.initCompleted = axolotlManager_.isInitCompleted();
                    if (!initCompleted && executeInit) {
                        initExecuteTask = new InitExecuteTask(this);
                    }
                }
            } catch (Exception ignore) {
                return false;
            } finally {
                KeyLockUtil.unlock(username);
            }
            envBuilder_ = DeviceEnv.AndroidEnv.parseFrom(envBuffer).toBuilder();
            //商业版的环境有点不一样
            //envBuilder_.getUserAgentBuilder().setPlatform(DeviceEnv.Platform.SMB_ANDROID);
            userId = envBuilder_.getFullphone() + "@s.whatsapp.net";
            DeviceEnv.AppVersion.Builder useragentBuilder = envBuilder_.getUserAgentBuilder().getAppVersionBuilder();
            DeviceEnv.UserAgent.Builder userAgentBuilder = envBuilder_.getUserAgentBuilder();
            //每次累加
            envBuilder_.setConnectionLc(envBuilder_.getConnectionLc() + 1);
            axolotlManager_.SetBytesSetting("env", envBuilder_.build().toByteArray());

            String manufacturer = userAgentBuilder.getManufacturer();
            if (iosLogin) {
                if (businessVersion) {
                    envBuilder_.getUserAgentBuilder().setPlatform(DeviceEnv.Platform.SMB_IOS);
                } else {
                    envBuilder_.getUserAgentBuilder().setPlatform(DeviceEnv.Platform.IOS);
                }
                WaVersion androidWaVersion = StringUtils.hasLength(this.loginWaVersion) ? new WaVersion(this.loginWaVersion, 0) : WhatsAppUtils.getWaVersion(businessVersion, envBuilder_, true);
                if (androidWaVersion == null) {
                    log.error("用户：{}，获取远程wa版本失败", username);
                    return false;
                }
                String version = androidWaVersion.getVersion();
                // String version = "2.23.2.75";
                this.waVersion = version;
                String[] versions = version.split("\\.");
                Integer releaseVersion = androidWaVersion.getReleaseVersion();
                useragentBuilder.setPrimary(Integer.parseInt(versions[0]));
                useragentBuilder.setSecondary(Integer.parseInt(versions[1]));
                useragentBuilder.setTertiary(Integer.parseInt(versions[2]));
                if (versions.length >= 4) {
                    useragentBuilder.setQuaternary(Integer.parseInt(versions[3]));
                }
                if (releaseVersion == 0) {
                    envBuilder_.getUserAgentBuilder().setReleaseChannel(DeviceEnv.ReleaseChannel.RELEASE);
                } else {
                    envBuilder_.getUserAgentBuilder().setReleaseChannel(DeviceEnv.ReleaseChannel.BETA);
                }
                //不是ios则分配一个ios设备信息
                if (!"Apple".equals(manufacturer)) {
                    DeviceInfo deviceInfo = axolotlManager_.getPlatformDeviceInfo(Constant.IOS);
                    if (deviceInfo == null) {
                        return false;
                    }
                    userAgentBuilder.setManufacturer(deviceInfo.getManufacturer());
                    userAgentBuilder.setDevice(deviceInfo.getDevice());
                    userAgentBuilder.setOsVersion(deviceInfo.getVersion());
                    userAgentBuilder.setOsBuildNumber(deviceInfo.getBuild());
                } else {
                    axolotlManager_.savePlatformDeviceInfo(Constant.IOS,
                            new DeviceInfo(userAgentBuilder.getManufacturer(), userAgentBuilder.getDevice(),
                                    userAgentBuilder.getOsVersion(), userAgentBuilder.getOsBuildNumber()));
                }
            } else {
                WaVersion androidWaVersion = StringUtils.hasLength(this.loginWaVersion) ? new WaVersion(this.loginWaVersion, 0) : WhatsAppUtils.getWaVersion(businessVersion, envBuilder_, false);
                if (androidWaVersion == null) {
                    log.error("用户：{}，获取远程wa版本失败", username);
                    return false;
                }
                String version = androidWaVersion.getVersion();
                this.waVersion = version;
                String[] versions = version.split("\\.");
                Integer releaseVersion = androidWaVersion.getReleaseVersion();
                if (businessVersion) {
                    envBuilder_.getUserAgentBuilder().setPlatform(DeviceEnv.Platform.SMB_ANDROID);
                } else {
                    envBuilder_.getUserAgentBuilder().setPlatform(DeviceEnv.Platform.ANDROID);
                }
                useragentBuilder.setPrimary(Integer.parseInt(versions[0]));
                useragentBuilder.setSecondary(Integer.parseInt(versions[1]));
                useragentBuilder.setTertiary(Integer.parseInt(versions[2]));
                if (versions.length >= 4) {
                    useragentBuilder.setQuaternary(Integer.parseInt(versions[3]));
                }
                if (releaseVersion == 0) {
                    envBuilder_.getUserAgentBuilder().setReleaseChannel(DeviceEnv.ReleaseChannel.RELEASE);
                } else {
                    envBuilder_.getUserAgentBuilder().setReleaseChannel(DeviceEnv.ReleaseChannel.BETA);
                }
                if ("Apple".equals(manufacturer)) {
                    DeviceInfo deviceInfo = axolotlManager_.getPlatformDeviceInfo(Constant.ANDROID);
                    if (deviceInfo == null) {
                        return false;
                    }
                    userAgentBuilder.setManufacturer(deviceInfo.getManufacturer());
                    userAgentBuilder.setDevice(deviceInfo.getDevice());
                    userAgentBuilder.setOsVersion(deviceInfo.getVersion());
                    userAgentBuilder.setOsBuildNumber(deviceInfo.getBuild());
                } else {
                    axolotlManager_.savePlatformDeviceInfo(Constant.ANDROID,
                            new DeviceInfo(userAgentBuilder.getManufacturer(), userAgentBuilder.getDevice(),
                                    userAgentBuilder.getOsVersion(), userAgentBuilder.getOsBuildNumber()));
                }
            }
            supplementDeviceInfo();
            noiseHandshake_ = new NoiseHandshake(this, proxy_, taskNotify, mccMnc);
            noiseHandshake_.StartNoiseHandShake(envBuilder_.build(), username, iosLogin, this.businessVersion, waVersion);
            return true;
        } catch (Exception e) {
            log.error("用户：{}，parse env failed", username, e);
            return false;
        }
    }

    private void supplementDeviceInfo() {
        boolean update = false;
        String pushName = envBuilder_.getPushname();
        if (!StringUtils.hasLength(pushName)) {
            String randomEnglishName = RandomNameUtil.getRandomEnglishName();
            envBuilder_.setPushname(randomEnglishName);
            update = true;
        }
        DeviceEnv.UserAgent.Builder userAgentBuilder = envBuilder_.getUserAgentBuilder();
        String phoneId = userAgentBuilder.getPhoneId();
        String osVersion = userAgentBuilder.getOsVersion();
        String deviceExpId = userAgentBuilder.getDeviceExpId();
        if (iosLogin) {
            if (StrUtil.isLowerCase(phoneId)) {
                phoneId = phoneId.toUpperCase();
                userAgentBuilder.setPhoneId(phoneId);
                update = true;
            }
            if (WhatsAppUtils.isVersionLessThan(osVersion, "12.0")) {
                DeviceInfo deviceInfo = axolotlManager_.getPlatformDeviceInfo(Constant.IOS);
                if (deviceInfo == null) {
                    return;
                }
                userAgentBuilder.setManufacturer(deviceInfo.getManufacturer());
                userAgentBuilder.setDevice(deviceInfo.getDevice());
                userAgentBuilder.setOsVersion(deviceInfo.getVersion());
                userAgentBuilder.setOsBuildNumber(deviceInfo.getBuild());
                update = true;
            }
        } else {
            if (StrUtil.isUpperCase(phoneId)) {
                phoneId = phoneId.toLowerCase();
                userAgentBuilder.setPhoneId(phoneId);
                update = true;
            }
            if (WhatsAppUtils.isVersionLessThan(osVersion, "5.0.0")) {
                userAgentBuilder.setOsVersion(ANDROID_VERSION[RandomUtil.randomInt(0, ANDROID_VERSION.length)]);
                axolotlManager_.savePlatformDeviceInfo(Constant.ANDROID,
                        new DeviceInfo(userAgentBuilder.getManufacturer(), userAgentBuilder.getDevice(),
                                userAgentBuilder.getOsVersion(), userAgentBuilder.getOsBuildNumber()));
                update = true;
            }
            if (!StringUtils.hasLength(deviceExpId)) {
                deviceExpId = Base64.getUrlEncoder().withoutPadding().encodeToString((HexUtil.decodeHex(IdUtil.simpleUUID())));
                userAgentBuilder.setDeviceExpId(deviceExpId);
                update = true;
            }
        }
        if (fillMissingUserAgentFields(userAgentBuilder)) {
            update = true;
        }
        if (update) {
            axolotlManager_.SetBytesSetting("env", envBuilder_.build().toByteArray());
        }
    }

    private boolean fillMissingUserAgentFields(DeviceEnv.UserAgent.Builder userAgentBuilder) {
        boolean update = false;
        if (!StringUtils.hasLength(userAgentBuilder.getMcc())) {
            userAgentBuilder.setMcc("000");
            update = true;
        }
        if (!StringUtils.hasLength(userAgentBuilder.getMnc())) {
            userAgentBuilder.setMnc("000");
            update = true;
        }
        if (!StringUtils.hasLength(userAgentBuilder.getLocaleLanguageIso6391())) {
            userAgentBuilder.setLocaleLanguageIso6391("en");
            update = true;
        }
        if (!StringUtils.hasLength(userAgentBuilder.getLocaleCountryIso31661Alpha2())) {
            userAgentBuilder.setLocaleCountryIso31661Alpha2("US");
            update = true;
        }

        return update;
    }


    public void StopEngine() {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        addTaskToQueue(() -> {
            try {
                if (gcmLogin != null) {
                    gcmStop.set(true);
                    gcmLogin.close();
                    gcmLogin = null;
                }
                if (apNsClientManager != null) {
                    gcmStop.set(true);
                    apNsClientManager.close();
                    apNsClientManager = null;
                }
                if (noiseHandshake_ != null) {
                    noiseHandshake_.StopNoiseHandShake();
                    noiseHandshake_ = null;
                }
                try {
                    KeyLockUtil.lock(username);
                    if (null != axolotlManager_) {
                        axolotlManager_.forceAutoCommit();
                        axolotlManager_.Close();
                        axolotlManager_ = null;
                    }
                } catch (Exception ignore) {
                } finally {
                    KeyLockUtil.unlock(username);
                }
                removeScheduleTask();
            } catch (Throwable ignored) {

            } finally {
                countDownLatch.countDown();
            }
        });
        try {
            countDownLatch.await();
        } catch (Exception ignored) {
        }
    }


    static class MessageInfo {
        String jid;
        String serialData;
        String messageType;
        String mediaType;

        public String getJid() {
            return jid;
        }

        public void setJid(String jid) {
            this.jid = jid;
        }

        public String getSerialData() {
            return serialData;
        }

        public void setSerialData(String serialData) {
            this.serialData = serialData;
        }

        public String getMessageType() {
            return messageType;
        }

        public void setMessageType(String messageType) {
            this.messageType = messageType;
        }

        public String getMediaType() {
            return mediaType;
        }

        public void setMediaType(String mediaType) {
            this.mediaType = mediaType;
        }
    }

    //LinkedHashMap<String, MessageInfo> msgMap_ = new LinkedHashMap<>(100);

    @Override
    public void OnLoginFail(ProtocolTreeNode node) {
        if (null != delegate_) {
            if (preventDuplication.incrementAndGet() > 1) {
                return;
            }
            delegate_.OnLogin(-1, node, loginId);
        }
    }

    @Override
    public void OnConnected(byte[] serverPublicKey) {
        if (serverPublicKey != null) {
            envBuilder_.setServerStaticPublic(ByteString.copyFrom(serverPublicKey));
            try {
                KeyLockUtil.lock(username);
                axolotlManager_.SetBytesSetting("env", envBuilder_.build().toByteArray());
            } catch (Exception ignore) {
            } finally {
                KeyLockUtil.unlock(username);
            }
        }
    }

    @Override
    public void OnDisconnected(String desc) {
        if (null != delegate_) {
            removeScheduleTask();
            if (preventDuplication.incrementAndGet() > 1) {
                return;
            }
            delegate_.OnDisconnect(desc, loginId);
        }
    }

    @Override
    public void OnPush(ProtocolTreeNode node) {
        if (HandleRegisterNode(node)) {
            return;
        }
        switch (node.GetTag()) {
            case "iq": {
                HandleIq(node);
                break;
            }
            case "call": {
                HandleCall(node);
                break;
            }
            case "stream:error": {
                HandleStreamError(node);
                break;
            }
            case "failure": {
                //登陆失败
                HandleFailure(node);
                break;
            }
            case "success": {
                //登陆成功
                HandleSuccess(node);
                break;
            }
            case "receipt": {
                HandleAeceipt(node);
                break;
            }
            case "message": {
                //文字，图片等消息通知
                HandleRecvMessage(node);
                break;
            }
            case "ack": {
                HandleAck(node);
                break;
            }
            case "notification": {
                HandleNotification(node);
                break;
            }
            case "presence": {
                HandlePresence(node);
                break;
            }
            case "ib": {
                handleIbNode(node);
                break;
            }
            default: {
                break;
            }
        }
    }

    private void handleIbNode(ProtocolTreeNode node) {
        String tag = node.getOneChildrenTag();
        if (tag == null) {
            return;
        }
        switch (tag) {
            case "edge_routing": {
                handleEdgeRouting(node.getOneChildren("edge_routing"));
                break;
            }
            case "dirty": {
                HandleDirty(node.getOneChildren("dirty"));
            }
            break;
        }
    }

    private void HandleDirty(ProtocolTreeNode dirty) {
        // <iq id='9' xmlns='urn:xmpp:whatsapp:dirty' type='set' to='s.whatsapp.net'><clean type='groups' timestamp='0'/></iq>
        ProtocolTreeNode iqNode = new ProtocolTreeNode("iq");
        //<ib from='s.whatsapp.net'><dirty type='account_sync' timestamp='1683189317'></dirty></ib>
        iqNode.AddAttribute(new StanzaAttribute("id", GenerateIqId()));
        iqNode.AddAttribute(new StanzaAttribute("xmlns", "urn:xmpp:whatsapp:dirty"));
        iqNode.AddAttribute(new StanzaAttribute("type", "set"));
        iqNode.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));

        ProtocolTreeNode cleanNode = new ProtocolTreeNode("clean");
        cleanNode.AddAttribute(new StanzaAttribute("type", dirty.GetAttributeValue("type")));

        iqNode.AddChild(cleanNode);
        AddTask(iqNode);
    }

    private void handleEdgeRouting(ProtocolTreeNode node) {
        ProtocolTreeNode edge = node.getOneChildren("routing_info");
        if (edge != null) {
            envBuilder_.setEdgeRoutingInfo(ByteString.copyFrom(edge.GetData()));
        }
        try {
            KeyLockUtil.lock(username);
            axolotlManager_.SetBytesSetting("env", envBuilder_.build().toByteArray());
        } catch (Exception ignore) {

        } finally {
            KeyLockUtil.unlock(username);
        }
    }

    @Override
    public void taskNotify(String username, Runnable runnable) {
        if (delegate_ != null) {
            delegate_.taskNotify(this, username, runnable);
        }
    }

    @Override
    public void OnClearServerStaticPublic() {
        envBuilder_.clearServerStaticPublic();
        axolotlManager_.SetBytesSetting("env", envBuilder_.build().toByteArray());
    }

    void HandlePresence(ProtocolTreeNode node) {
        if (null != delegate_) {
            delegate_.OnPresence(node);
        }
    }


    void HandleIq(ProtocolTreeNode node) {
        String taskId = node.IqId();
        if (StringUtils.hasLength(taskId) && taskId.equals(heartbeatTaskId)) {
            //代表是心跳任务
            int heartbeatNum = heartbeat.decrementAndGet();
            //log.info("用户：{}，接收心跳：{}", username, heartbeatNum);
        } else if (StringUtils.hasLength(taskId)) {
            taskNotify.setEventContent(taskId, node);
        } else {
            String type = node.GetAttributeValue("type");
            String xmlns = node.GetAttributeValue("xmlns");
            if ("get".equals(type) && "urn:xmpp:ping".equals(xmlns)) {
                //<iq type='result' to='s.whatsapp.net'></iq>
                ProtocolTreeNode iq = new ProtocolTreeNode("iq");
                iq.AddAttribute(new StanzaAttribute("type", "result"));
                iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
                AddTask(iq);
            }
        }
    }

    void HandleCall(ProtocolTreeNode node) {
        String id = node.GetAttributeValue("id");
        String from = node.GetAttributeValue("from");
        String t = node.GetAttributeValue("t");
        long msgTime = Convert.toLong(t, 0L) * 1000;
        boolean offlineMessage = WhatsAppUtils.checkIsOfflineMessage(loginTime, msgTime);
        ProtocolTreeNode offerNode = node.getOneChildren("offer");
        if (from.contains("@lid") && ObjectUtil.isNotNull(offerNode) && StrUtil.isNotEmpty(offerNode.GetAttributeValue("caller_pn"))) {
            // 处理lid的的语音通话呼叫
            from = offerNode.GetAttributeValue("caller_pn");
            ProtocolTreeNode enc = offerNode.getOneChildren("enc");
            if (ObjectUtil.isNotNull(enc) && "pkmsg".equals(enc.GetAttributeValue("type"))) {
                // 保存jid-lid
                saveNodeJidLid(from, node.GetAttributeValue("from"));
            }
        }
        if (from.contains("@s.whatsapp.net") && StrUtil.isNotEmpty(node.GetAttributeValue("sender_lid"))) {
            String senderLid = node.GetAttributeValue("sender_lid");
            ProtocolTreeNode enc = offerNode.getOneChildren("enc");
            if (ObjectUtil.isNotNull(enc) && "pkmsg".equals(enc.GetAttributeValue("type"))) {
                // 保存jid-lid
                saveNodeJidLid(from, senderLid);
            }
        }
        //只转发不是群的消息
        if (ObjectUtil.isNull(offerNode)) {
            String finalFrom = from;
            ThreadPoolConfig.threadPoolExecutor.execute(() -> {
                ProtocolTreeNode terminateNode = node.getOneChildren("terminate");
                if (terminateNode != null) {
                    String callId = terminateNode.GetAttributeValue("call-id");
                    if (StrUtil.indexOf(finalFrom, "@s.whatsapp.net", 0, false) != -1) {
                        AsyncMessageService.parse(null, null, finalFrom, userId, callId, "terminate", "terminate", username, msgTime, offlineMessage, false);
                    }
                }
                ProtocolTreeNode rejectNode = node.getOneChildren("reject");
                if (rejectNode != null) {
                    String callId = rejectNode.GetAttributeValue("call-id");
                    if (StrUtil.indexOf(finalFrom, "@s.whatsapp.net", 0, false) != -1) {
                        AsyncMessageService.parse(null, null, finalFrom, userId, callId, "reject", "reject", username, msgTime, offlineMessage, false);
                    }
                }
            });
        }
        ProtocolTreeNode childrenNode = node.getOneChildren();
        String tag = childrenNode.GetTag();
        if ("offer".equals(tag) && StrUtil.isNotEmpty(node.GetAttributeValue("version")) && StrUtil.isNotEmpty(node.GetAttributeValue("platform")) && ObjectUtil.isNotNull(offerNode)) {
            LinkedList<ProtocolTreeNode> encNodes = childrenNode.GetChildren("enc");
            StringUtil.JidInfo jidInfo = StringUtil.ParseJid(from);
            // 回复来电消息并接通
            // <receipt to='.0:0@s.whatsapp.net' id='1709004196-18'><offer call-id='65E7CC81A1533F24EE43453B96A98027' call-creator='.0:0@s.whatsapp.net'/></receipt>
            String callId = offerNode.GetAttributeValue("call-id");
            ProtocolTreeNode receipt = new ProtocolTreeNode("receipt");
            receipt.AddAttribute(new StanzaAttribute("id", id));
            receipt.AddAttribute(new StanzaAttribute("to", from));
            ProtocolTreeNode offer = new ProtocolTreeNode("offer");
            offer.AddAttribute(new StanzaAttribute("call-id", callId));
            offer.AddAttribute(new StanzaAttribute("call-creator", from));
            receipt.AddChild(offer);
            AddTask(receipt);
            byte[] callKey = getCallKey(encNodes, jidInfo);
            if (ObjectUtil.isNull(callKey)) {
                return;
            }
            String finalFrom1 = from;
            ThreadPoolConfig.threadPoolExecutor.execute(() -> {
                if (ObjectUtil.isNull(offerNode.getOneChildren("group_info"))) {
                    AsyncMessageService.parse(null, null, finalFrom1, userId, callId, "offer", "offer", username, msgTime, offlineMessage, false);
                }
            });
            ProtocolTreeNode videoNode = childrenNode.getOneChildren("video");
            // 发送preaccept节点
            ProtocolTreeNode preAcceptCallNode = new ProtocolTreeNode("call");
            preAcceptCallNode.AddAttribute(new StanzaAttribute("to", from));
            preAcceptCallNode.AddAttribute(new StanzaAttribute("id", IdUtil.simpleUUID().toUpperCase()));
            ProtocolTreeNode preAcceptNode = new ProtocolTreeNode("preaccept");
            preAcceptNode.AddAttribute(new StanzaAttribute("call-creator", from));
            preAcceptNode.AddAttribute(new StanzaAttribute("call-id", callId));
            ProtocolTreeNode audio = new ProtocolTreeNode("audio");
            audio.AddAttribute(new StanzaAttribute("rate", "16000"));
            audio.AddAttribute(new StanzaAttribute("enc", "opus"));
            preAcceptNode.AddChild(audio);
            if (videoNode != null) {
                ProtocolTreeNode video = new ProtocolTreeNode("video");
                video.AddAttribute(new StanzaAttribute("screen_width", "1080"));
                String dec = videoNode.GetAttributeValue("dec");
                video.AddAttribute(new StanzaAttribute("dec", StringUtils.hasLength(dec) ? dec : "H264,H265,AV1"));
                video.AddAttribute(new StanzaAttribute("screen_height", "1920"));
                video.AddAttribute(new StanzaAttribute("device_orientation", "0"));
                preAcceptNode.AddChild(video);
            }
            ProtocolTreeNode encopt = new ProtocolTreeNode("encopt");
            encopt.AddAttribute(new StanzaAttribute("keygen", "2"));
            preAcceptNode.AddChild(encopt);
            ProtocolTreeNode capability = new ProtocolTreeNode("capability");
            capability.AddAttribute(new StanzaAttribute("ver", "1"));
            capability.SetData("AQT3CcT6".getBytes(StandardCharsets.UTF_8));
            preAcceptNode.AddChild(capability);
            preAcceptCallNode.AddChild(preAcceptNode);
            AddTask(preAcceptCallNode);
            // 发送stun节点
            ProtocolTreeNode relayNode = offerNode.getOneChildren("relay");
            LinkedList<ProtocolTreeNode> te2 = relayNode.GetChildren("te2");
            LinkedList<ProtocolTreeNode> token = relayNode.GetChildren("token");
            String key = new String(relayNode.getOneChildren("key").GetData(), StandardCharsets.UTF_8);
            TurnIpData turnIpData = TE2IpParser.parseTurnIpData(token, te2);
            if (ObjectUtil.isNull(turnIpData)) {
                log.error("用户：{}，解析语音通话turnToken或turnIp失败", username);
                return;
            }
            ProtocolTreeNode stunCall = new ProtocolTreeNode("call");
            stunCall.AddAttribute(new StanzaAttribute("to", from));
            stunCall.AddAttribute(new StanzaAttribute("id", IdUtil.simpleUUID().toUpperCase()));
            ProtocolTreeNode relayLatency = new ProtocolTreeNode("relaylatency");
            relayLatency.AddAttribute(new StanzaAttribute("call-creator", from));
            relayLatency.AddAttribute(new StanzaAttribute("call-id", callId));
            ProtocolTreeNode te = new ProtocolTreeNode("te");
            te.AddAttribute(new StanzaAttribute("latency", "33554" + RandomUtil.randomInt(500, 700)));
            te.SetData(turnIpData.getIpv4Addr());
            relayLatency.AddChild(te);
            /*ProtocolTreeNode teIpv6 = new ProtocolTreeNode("te");
            teIpv6.AddAttribute(new StanzaAttribute("latency", "33554" + RandomUtil.randomInt(500, 700)));
            teIpv6.SetData(turnIpv6);
            relayLatency.AddChild(teIpv6);*/
            stunCall.AddChild(relayLatency);
            AddTask(stunCall);
            String masterKey = cn.hutool.core.codec.Base64.encode(callKey);
            Pair<String, Integer> ipv4Pair = IPUtil.IpParser(turnIpData.getIpv4Addr());
            String serverIp = ipv4Pair.getKey();
            int ipv4Port = ipv4Pair.getValue();
            Pair<String, Integer> ipv6Pair = IPUtil.IpParser(turnIpData.getIpv6Addr());
            String ipv6ServerIp = ipv6Pair.getKey();
            int ipv6Port = ipv6Pair.getValue();
            StringBuilder voipCommand = new StringBuilder();
            voipCommand.append("arch -x86_64 ")
                    .append("wavoce")
                    .append(" --wa_server=").append(serverIp)
                    .append(" --wa_port=").append(ipv4Port)
                    .append(" --listen_port=").append("4002")
                    .append(" --sender_jid=").append(userId)
                    .append(" --call_id=").append(callId)
                    .append(" --token=").append(turnIpData.getTurnToken())
                    .append(" --password=").append(key)
                    .append(" --master_key=").append(masterKey)
                    .append(" --receive_jid=").append(from)
                    /*.append(" --socks5_server=").append("127.0.0.1")
                    .append(" --socks5_port=").append("1080")*/
                    /*.append(" --socks5_server=").append("us1.ip007.cc")
                    .append(" --socks5_port=").append("32133")
                    .append(" --socks5_username=").append("42fd104d4cb")
                    .append(" --socks5_password=").append("55d65630ac7")*/
                    .append(" --native_record=").append("1");
            //来电话消息
            log.debug("[VOIP Commands]\n");
            log.debug(voipCommand + "\n");
            VoipAcceptResult voipAcceptResult = VoipAcceptResult.builder()
                    .masterKey(masterKey)
                    .senderJid(userId)
                    .receiveJid(from)
                    .callId(callId)
                    .serverIp(serverIp)
                    .port(String.valueOf(ipv4Port))
                    .ipv6ServerIp(ipv6ServerIp)
                    .ipv6Port(String.valueOf(ipv6Port))
                    .token(turnIpData.getTurnToken())
                    .password(key)
                    //.command(voipCommand.toString())
                    .build();
            //缓存到redis
            String redisKey = CacheConstants.VOIP_ACCEPT_RESULT_KEY + username + ":" + callId;
            RedisService.getInstance().set(redisKey, voipAcceptResult, 60L);
            //websocket推送
            AsyncMessageService.asyncMessagePushService.voipCallReceived(from, userId, callId, username, msgTime, offlineMessage, voipAcceptResult);
        } else if ("accept".equals(tag)) {
            ProtocolTreeNode recvAcceptNode = node.getOneChildren("accept");
            String callId = recvAcceptNode.GetAttributeValue("call-id");
            String callCreator = recvAcceptNode.GetAttributeValue("call-creator");
            ProtocolTreeNode receiptNode = new ProtocolTreeNode("receipt");
            receiptNode.AddAttribute(new StanzaAttribute("to", StringUtil.ParseJid(from).toString()));
            receiptNode.AddAttribute(new StanzaAttribute("id", id));
            ProtocolTreeNode accept = new ProtocolTreeNode("accept");
            accept.AddAttribute(new StanzaAttribute("call-id", callId));
            accept.AddAttribute(new StanzaAttribute("call-creator", StringUtil.ParseJid(callCreator).toString()));
            receiptNode.AddChild(accept);
            AddTask(receiptNode);
            ProtocolTreeNode video = recvAcceptNode.getOneChildren("video");
            if (ObjectUtil.isNotNull(video)) {
                //视频暂时不做处理
                log.info("用户: {}, 已接收视频通话", username);
                return;
            }
            //执行发送语音程序
            log.info("用户: {}, 已接收语音通话", username);
            String redisKey = CacheConstants.VOIP_ACCEPT_RESULT_KEY + username + ":" + callId;
            VoipAcceptResult voipAcceptResult = (VoipAcceptResult) RedisService.getInstance().get(redisKey);
            redisKey = CacheConstants.VOIP_CALL_PACK_KEY + username + ":" + callId;
            VoipCallPack voipCallPack = (VoipCallPack) RedisService.getInstance().get(redisKey);
            /*if (ObjectUtil.isNotNull(voipAcceptResult)) {
                String command = voipAcceptResult.getCommand();
                new Thread(() -> {
                    String pid = RuntimeUtil.execForStr("./manage_process.sh run " + command);
                    log.info("执行shell返回内容: {}", pid);
                }).start();
            }*/
            if (ObjectUtil.isNull(voipCallPack) || ObjectUtil.isNull(voipAcceptResult)) {
                log.warn("用户: {}, 获取voip播放信息失败", username);
                return;
            }
            //websocket推送
            AsyncMessageService.asyncMessagePushService.voipCallConnected(from, userId, callId, username, msgTime, offlineMessage);
        } else {
            ProtocolTreeNode ackNode = new ProtocolTreeNode("ack");
            ackNode.AddAttribute(new StanzaAttribute("id", id));
            ackNode.AddAttribute(new StanzaAttribute("class", "call"));
            ackNode.AddAttribute(new StanzaAttribute("type", tag));
            ackNode.AddAttribute(new StanzaAttribute("to", from));
            AddTask(ackNode);
        }
    }

    /**
     * 获取callKey
     *
     * @param encNodes 加密节点
     * @param jidInfo  jidInfo
     * @return byte[]
     */
    private byte[] getCallKey(LinkedList<ProtocolTreeNode> encNodes, StringUtil.JidInfo jidInfo) {
        try {
            byte[] data = null;
            ProtocolTreeNode pkMsgEncNode = GetEncNode(encNodes, "pkmsg");
            if (pkMsgEncNode != null) {
                data = HandlePreKeyWhisperMessage(jidInfo, pkMsgEncNode);
            } else {
                ProtocolTreeNode whisperEncNode = GetEncNode(encNodes, "msg");
                if (whisperEncNode != null) {
                    data = HandleWhisperMessage(jidInfo, whisperEncNode);
                }
            }
            if (ObjectUtil.isNull(data)) {
                log.error("用户：{}，解析语音通话异常：", username);
                return null;
            }
            // 提取SenderKey
            WhatsMessage.WhatsAppMessage callKey = WhatsMessage.WhatsAppMessage.parseFrom(data);
            byte[] byteArray = callKey.getCall().getCallkey().toByteArray();
            log.debug("用户: {}, 解密获得callKey序列化结果: {}", username, HexUtil.encodeHexStr(byteArray));
            return byteArray;
        } catch (Exception e) {
            log.error("用户：{}，解析语音通话SenderKey异常：", username, e);
            return null;
        }

    }

    void HandleStreamError(ProtocolTreeNode node) {
        if (null != delegate_) {
            if (preventDuplication.incrementAndGet() > 1) {
                return;
            }
            removeScheduleTask();
            delegate_.OnDisconnect(node.toString(), loginId);
        }
    }

    void HandleFailure(ProtocolTreeNode node) {
        if (null != delegate_) {
            if (preventDuplication.incrementAndGet() > 1) {
                return;
            }
            delegate_.OnLogin(-1, node, loginId);
        }
    }

    public void SendPing() {
        String hexString11 = generatePingId();
        ProtocolTreeNode ping = new ProtocolTreeNode("iq");
        ping.AddAttribute(new StanzaAttribute("id", hexString11));
        ping.AddAttribute(new StanzaAttribute("xmlns", "w:p"));
        ping.AddAttribute(new StanzaAttribute("type", "get"));
        ping.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        AddTask(ping);
    }

    public void SendPing(boolean timing) {
        if (!timing) {
            SendPing();
            return;
        }
        int num = heartbeat.get();
        if (num != 0) {
            if (null != delegate_) {
                removeScheduleTask();
                if (preventDuplication.incrementAndGet() > 1) {
                    return;
                }
                delegate_.OnDisconnect("心跳检查失败", loginId);
            }
            return;
        }
        ProtocolTreeNode ping = new ProtocolTreeNode("iq");
        if (!iosLogin) {
            heartbeatTaskId = generatePingId();
        } else {
            heartbeatTaskId = GenerateIqId();
        }
        ping.AddAttribute(new StanzaAttribute("id", heartbeatTaskId));
        ping.AddAttribute(new StanzaAttribute("xmlns", "w:p"));
        ping.AddAttribute(new StanzaAttribute("type", "get"));
        ping.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        int heartbeatNum = heartbeat.addAndGet(1);
        //log.info("用户：{}，发送心跳：{}", username, heartbeatNum);
        AddTask(ping);
    }

    private void removeScheduleTask() {
        try {
            if (handlePlatformNotificationTimeOut != null) {
                handlePlatformNotificationTimeOut.cancel();
                handlePlatformNotificationTimeOut = null;
            }
            //移除定时任务
            if (StringUtils.hasLength(this.heartBeatScheduleId)) {
                CronUtil.remove(this.heartBeatScheduleId);
                this.heartBeatScheduleId = null;
            }
            if (StringUtils.hasLength(this.getCdnInfoScheduleId)) {
                CronUtil.remove(this.getCdnInfoScheduleId);
                this.getCdnInfoScheduleId = null;
            }
        } catch (Exception ignored) {
        }
    }

    void HandleSuccess(ProtocolTreeNode node) {
        String t = node.GetAttributeValue("t");
        this.loginTime = Convert.toLong(t, 0L);
        this.creationTime = DateUtil.format(DateUtil.date(Convert.toLong(node.GetAttributeValue("creation"), 0L) * 1000), DatePattern.NORM_DATETIME_PATTERN);
        this.location = node.GetAttributeValue("location");
        this.userLid = XmppJid.of(node.GetAttributeValue("lid")).toShortString();
        if (null != delegate_) {
            delegate_.OnLogin(0, node, loginId);
        }
        if (!this.init && iosLogin) {
            AddTask(sendPresence(false, null));
            AddTask(sendXMLStreamEnd());
            axolotlManager_.markInit();
            return;
        }
        if (!executeInit && !initCompleted) {
            //直接标记已初始化
            initCompleted = true;
            axolotlManager_.initCompleted();
        }
        Optional.ofNullable(this.gcmCatValue).ifPresent(this::sendGcmCat);
        SendPing(false);
        GetCdnInfo();
        this.heartBeatScheduleId = CronUtil.schedule("0/30 * * * * ?", (Task) () -> {
            SendPing(true);
        });
        this.getCdnInfoScheduleId = CronUtil.schedule("0 0 0/5 * * ? *", (Task) this::GetCdnInfo);
        SendUnifiedSession();
        handlerLoginEvent();
        handleAbtConfig();
        connectNotifyService();
        if (!initCompleted) {
            //继续执行未完成的初始化任务
            initExecuteTask.scheduleNextTask();
        } else {
            // 发送presence接口, 保持在线
            AddTask(sendPresence(true, null));
        }
    }

    public void handleAbtConfig() {
        String abtConfig = null;
        try {
            KeyLockUtil.lock(username);
            abtConfig = axolotlManager_.getAbtConfig();
        } catch (Exception ignore) {
        } finally {
            KeyLockUtil.unlock(username);
        }
        if (StrUtil.isEmpty(abtConfig)) {
            getAbtConfig(null);
            return;
        }
        JSONObject jsonObject = JSONObject.parseObject(abtConfig);
        String nextAbtTime = jsonObject.getString("next_abt_time");
        String abtHash = jsonObject.getString("abt_hash");
        String abKey = jsonObject.getString("ab_key");
        String expoKey = jsonObject.getString("expo_key");
        log.debug("用户: {}, 获取 abtHash: {}, abKey: {}, expoKey: {}, nextAbtTime: {}", username, abtHash, abKey, expoKey, nextAbtTime);
        if (StrUtil.isNotEmpty(nextAbtTime) && StrUtil.isNotEmpty(abtHash) && StrUtil.isNotEmpty(abKey) && StrUtil.isNotEmpty(expoKey)) {
            // 判断是否到时间重新获取
            long time = Long.parseLong(nextAbtTime);
            long now = System.currentTimeMillis() / 1000;
            if (now - time >= 0) {
                log.debug("用户: {}, 达到时间获取AbtConfig", username);
                getAbtConfig(abtHash);
            } else {
                this.abKey2 = abKey;
                this.expoKey = expoKey;
            }
        } else {
            // 若该值不存在，直接获取
            getAbtConfig(null);
        }
    }

    public void connectNotifyService() {
        ThreadPoolConfig.xmppPoolExecutor.execute(() -> {
            //5秒后再连接gcm
            handlePlatformNotificationTimeOut = hashedWheelTimer.newTimeout(timeout -> {
                handlePlatformNotificationTimeOut = null;
                ThreadPoolConfig.xmppPoolExecutor.execute(() -> {
                    if (initCompleted) {
                        digest();
                    } else {
                        checkRegistrationId();
                    }
                    xMppPush((srcNode, result) -> {
                        log.info("用户: {}, platform info: {}", username, result);
                        handlePlatformNotification(result, iosLogin);
                    });
                });
            }, 5, TimeUnit.SECONDS);
        });
    }

    public void scheduleNotifyService(StandardThreadExecutor executor) {
        if (iosLogin && ObjectUtil.isNotNull(this.apnsState)) {
            apnsConnect(this.apnsState);
        } else if (!iosLogin && ObjectUtil.isNotNull(this.gcmToken)) {
            gcmConnect(this.gcmToken);
        } else {
            xMppPush((srcNode, result) -> {
                executor.execute(() -> {
                    log.info("用户: {}, platform info: {}", username, result);
                    handlePlatformNotification(executor, result, iosLogin);
                });
            });
        }
    }

    private void handlePlatformNotification(ProtocolTreeNode result, boolean iosLogin) {
        handlePlatformNotification(ThreadPoolConfig.xmppPoolExecutor, result, iosLogin);
    }


    private void handlePlatformNotification(StandardThreadExecutor executor, ProtocolTreeNode result, boolean iosLogin) {
        executor.execute(() -> {
            try {
                // 获取配置节点
                LinkedList<ProtocolTreeNode> configNodes = result.GetChildren("config");
                if (configNodes.isEmpty()) {
                    reAuthenticate(iosLogin);
                    return;
                }

                // 获取目标平台配置
                String targetPlatform = iosLogin ? "apple" : "gcm";
                Optional<ProtocolTreeNode> configNode = configNodes.stream()
                        .filter(node -> targetPlatform.equals(node.GetAttributeValue("platform")))
                        .findFirst();

                // 处理平台通知
                if (configNode.isPresent()) {
                    handleNotification(configNode.get().GetAttributeValue("id"), iosLogin);
                } else {
                    reAuthenticate(iosLogin);
                }

            } catch (Exception e) {
                log.error("用户: {}, 处理平台通知时发生错误", username, e);
            }
        });
    }

    private void handleNotification(String id, boolean iosLogin) {
        if (iosLogin) {
            handleAppleNotification(id);
        } else {
            handleGcmNotification(id);
        }
    }

    private void handleAppleNotification(String id) {
        log.info("用户: {}, 处理APNS通知", username);
        try {
            String params = axolotlManager_.getApnsParamsValue();
            if (StringUtils.hasLength(params)) {
                this.apnsState = ByteUtil.deserialize(ByteUtil.hexToBytes(params));
                String token = HexUtil.encodeHexStr(this.apnsState.getToken());
                if (id.equals(token)) {
                    apnsConnect(this.apnsState);
                    return;
                }
            }
            apnsReAuth();
        } catch (Exception e) {
            log.error("用户: {}, 处理APNS通知时发生错误: ", username, e);
        }
    }

    public void apnsConnect(APNsState apnsState) {
        ProxyInfo proxyInfo = Constant.GCM_INFO.randomAPNSProxy();
        apNsClientManager = new APNsClientProcessor(apnsState, TOPICS, proxyInfo, this);
        try {
            apNsClientManager.process2();
        } catch (Throwable t) {
            log.error("用户: {}, APNS Exception", username);
        }
    }

    private void apnsReAuth() {
        try {
            KeyLockUtil.lock(username);
            axolotlManager_.deleteGcmSettings();
        } catch (Exception ignore) {
        } finally {
            KeyLockUtil.unlock(username);
        }
        String serialNumber = RandomUtil.randomStringUpper(12);
        ActivationInfo activationInfo = new ActivationInfo.Builder()
                .serialNumber(serialNumber)
                .activationState("Unactivated")
                .buildVersion("22F82")
                .deviceClass("MacOS")
                .productType("MacBookPro18,3")
                .productVersion("13.4.1")
                .build();
        ProxyInfo proxyInfo = Constant.GCM_INFO.randomAPNSProxy();
        apNsClientManager = new APNsClientProcessor(null, activationInfo, TOPICS, proxyInfo, this);
        try {
            apNsClientManager.process();
        } catch (Throwable t) {
            log.error("用户: {}, APNS Exception", username);
        }
    }

    private void handleGcmNotification(String id) {
        log.info("用户: {}, 处理GCM通知", username);
        try {
            String params = getGcmParamsByDatabase();
            if (StringUtils.hasLength(params)) {
                this.gcmToken = getGcmToken(params);
                String token = this.gcmToken.getToken();
                if (id.equals(token)) {
                    gcmConnect(this.gcmToken);
                    return;
                }
            }
            gcmReAuth();
        } catch (Exception e) {
            log.error("用户: {}, 处理GCM通知时发生错误: ", username, e);
        }
    }

    private void reAuthenticate(boolean iosLogin) {
        if (iosLogin) {
            apnsReAuth();
        } else {
            gcmReAuth();
        }
    }

    public void gcmReAuth() {
        try {
            KeyLockUtil.lock(username);
            axolotlManager_.deleteGcmSettings();
        } catch (Exception ignore) {
        } finally {
            KeyLockUtil.unlock(username);
        }
        this.gcmToken = getGcmParams();
        if (gcmToken == null) {
            OnGcmClose("token获取失败", false);
            return;
        }
        String token = this.gcmToken.getToken();
        String params = this.gcmToken.getParams();
        String androidId = this.gcmToken.getAndroidId();
        String securityToken = this.gcmToken.getSecurityToken();
        sendGcm(token);
        gcmLogin = new GCMLogin(proxy_, this);
        gcmLogin.StartListen(params, androidId, securityToken, this, "");
    }

    public void gcmConnect(GcmTokenResult gcmToken) {
        String params = gcmToken.getParams();
        String androidId = gcmToken.getAndroidId();
        String securityToken = gcmToken.getSecurityToken();
        String gcmPersistentIdValue = "";
        try {
            KeyLockUtil.lock(username);
            gcmPersistentIdValue = axolotlManager_.getGcmPersistentIdValue();
        } catch (Exception ignore) {
        } finally {
            KeyLockUtil.unlock(username);
        }
        log.info("用户：{}，开始gcm连接", username);
        gcmLogin = new GCMLogin(proxy_, this);
        gcmLogin.StartListen(params, androidId, securityToken, this, gcmPersistentIdValue);
    }

    public void handlerLoginEvent() {
        SendStatus();
        if (businessVersion) {
            getVerifiedName();
        }
    }

    public void registerXmpp(String userId) {
        getBusinessProfile(userId);
        setBackupToken();
        setGoogleCode();
        if (!iosLogin) {
            setGooglePlayAttestation();
            setGooglePlayVerify();
        }
    }

    public void setCategory() {
        /*
         * <iq id='01b' xmlns='w:biz' type='set'><business_profile v='884'><categories><category id='133436743388217'/></categories></business_profile></iq>
         */
        ProtocolTreeNode node = new ProtocolTreeNode("iq");
        node.AddAttribute(new StanzaAttribute("id", GenerateIqId()));
        node.AddAttribute(new StanzaAttribute("xmlns", "w:biz"));
        node.AddAttribute(new StanzaAttribute("type", "set"));
        ProtocolTreeNode bizProfileNode = new ProtocolTreeNode("business_profile");
        bizProfileNode.AddAttribute(new StanzaAttribute("v", "884"));
        ProtocolTreeNode categoriesNode = new ProtocolTreeNode("categories");
        ProtocolTreeNode categoryNode = new ProtocolTreeNode("category");
        categoryNode.AddAttribute(new StanzaAttribute("id", getRandomCategory()));
        categoriesNode.AddChild(categoryNode);
        bizProfileNode.AddChild(categoriesNode);
        node.AddChild(bizProfileNode);
        AddTask(node);
    }

    public void getBusinessProfile(String UserId) {
        /**
         * android
         * <iq id='08' xmlns='w:biz' type='get'><business_profile v='116'><profile jid='254779630486@s.whatsapp.net'/></business_profile></iq>
         */

        /**
         * ios
         * <iq id='1689650281-9' xmlns='w:biz' type='get' to='s.whatsapp.net'><business_profile v='116'><profile jid='254772610257@s.whatsapp.net'/></business_profile></iq>
         */
        ProtocolTreeNode node = new ProtocolTreeNode("iq");
        node.AddAttribute(new StanzaAttribute("id", GenerateIqId()));
        node.AddAttribute(new StanzaAttribute("xmlns", "w:biz"));
        node.AddAttribute(new StanzaAttribute("type", "get"));
        if (iosLogin) {
            node.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        }
        ProtocolTreeNode businessProfileNode = new ProtocolTreeNode("business_profile");
        businessProfileNode.AddAttribute(new StanzaAttribute("v", "116"));
        ProtocolTreeNode profileNode = new ProtocolTreeNode("profile");
        profileNode.AddAttribute(new StanzaAttribute("jid", UserId));
        businessProfileNode.AddChild(profileNode);
        node.AddChild(businessProfileNode);
        AddTask(node);
    }

    public void setBackupToken() {
        /*
         * <iq to='s.whatsapp.net' xmlns='w:auth:backup:token' type='set' id='06'><token>KFo/uigN4CekK/wayztXRNZ1Gjs=</token></iq>
         */
        /**
         * <iq id='1689650281-22' xmlns='w:auth:token' type='set' to='s.whatsapp.net'><token>Iw7jlJsQh4i57FgrzPqLvg==</token></iq>
         */
        ProtocolTreeNode node = new ProtocolTreeNode("iq");
        node.AddAttribute(new StanzaAttribute("id", GenerateIqId()));
        if (iosLogin) {
            node.AddAttribute(new StanzaAttribute("xmlns", "w:auth:token"));
        } else {
            node.AddAttribute(new StanzaAttribute("xmlns", "w:auth:backup:token"));
        }
        node.AddAttribute(new StanzaAttribute("type", "set"));
        node.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        ProtocolTreeNode tokenNode = new ProtocolTreeNode("token");
        tokenNode.SetData(RandomUtil.randomBytes(0x14));
        node.AddChild(tokenNode);
        AddTask(node);
    }

    public void setGoogleCode() {
        /*
         * <iq to='s.whatsapp.net' xmlns='urn:xmpp:whatsapp:account' type='get' id='0a'><crypto action='create'><google>Y7+LhL6AEnRrttjD+TPTtrtaAzPWSqPdaPmbbRRrBI8=</google></crypto></iq>
         */
        /**
         * <iq id='1689650281-12' xmlns='urn:xmpp:whatsapp:account' type='get' to='s.whatsapp.net'><2fa/></iq>
         */
        ProtocolTreeNode node = new ProtocolTreeNode("iq");
        node.AddAttribute(new StanzaAttribute("id", GenerateIqId()));
        node.AddAttribute(new StanzaAttribute("xmlns", "urn:xmpp:whatsapp:account"));
        node.AddAttribute(new StanzaAttribute("type", "get"));
        node.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        ProtocolTreeNode cryptoNode;
        if (iosLogin) {
            cryptoNode = new ProtocolTreeNode("2fa");
        } else {
            cryptoNode = new ProtocolTreeNode("crypto");
            cryptoNode.AddAttribute(new StanzaAttribute("action", "create"));
            ProtocolTreeNode googleNode = new ProtocolTreeNode("google");
            googleNode.SetData(RandomUtil.randomBytes(0x20));
            cryptoNode.AddChild(googleNode);
        }
        node.AddChild(cryptoNode);
        AddTask(node);
    }

    public void setGooglePlayAttestation() {
        /*
         * <ib><attestation><error code='1001'>Google Play Services Unavailable. Connection result code: 2</error></attestation></ib>
         */
        ProtocolTreeNode node = new ProtocolTreeNode("ib");
        ProtocolTreeNode attestationNode = new ProtocolTreeNode("attestation");
        ProtocolTreeNode errorNode = new ProtocolTreeNode("error");
        errorNode.AddAttribute(new StanzaAttribute("code", "1001"));
        errorNode.SetData("Google Play Services Unavailable. Connection result code: 2".getBytes(StandardCharsets.UTF_8));
        attestationNode.AddChild(errorNode);
        node.AddChild(attestationNode);
        AddTask(node);
    }

    public void setGooglePlayVerify() {
        /*
         * <ib><verify_apps><error code='1001'>Google Play Services Unavailable. Connection result code: 2</error></verify_apps></ib>
         */
        ProtocolTreeNode node = new ProtocolTreeNode("ib");
        ProtocolTreeNode verifyNode = new ProtocolTreeNode("verify_apps");
        ProtocolTreeNode errorNode = new ProtocolTreeNode("error");
        errorNode.AddAttribute(new StanzaAttribute("code", "1001"));
        errorNode.SetData("Google Play Services Unavailable. Connection result code: 2".getBytes(StandardCharsets.UTF_8));
        verifyNode.AddChild(errorNode);
        node.AddChild(verifyNode);
        AddTask(node);
    }


    public void sendGcmCat(byte[] gcmCatValue) {
        //<ib><cat>L213MQABc9p4sUohosGblE/kcPhmh8SGnFNt9E3Y2GT85nwvvHrC1mbhqogy8NEQjV6PFB2ioV/3OHevlC6PoiuS/LtxbFEC080yUNUBMH6HNDWBs1fPnQu6WisnI/8criYlwWh+RPb6klesiE/LIxESSAqqn3Q=</cat></ib>
        ProtocolTreeNode available = new ProtocolTreeNode("ib");
        ProtocolTreeNode protocolTreeNode = new ProtocolTreeNode("cat");
        protocolTreeNode.SetData(gcmCatValue);
        available.AddChild(protocolTreeNode);
        AddTask(available);
    }

    public void sendGcm(String token) {
        ProtocolTreeNode node = new ProtocolTreeNode("iq");
        node.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        node.AddAttribute(new StanzaAttribute("id", GenerateIqId()));
        node.AddAttribute(new StanzaAttribute("xmlns", "urn:xmpp:whatsapp:push"));
        node.AddAttribute(new StanzaAttribute("type", "set"));
        ProtocolTreeNode removeDeviceNode = new ProtocolTreeNode("config");
        removeDeviceNode.AddAttribute(new StanzaAttribute("id", token));
        if (!iosLogin) {
            removeDeviceNode.AddAttribute(new StanzaAttribute("num_acc", "1"));
            removeDeviceNode.AddAttribute(new StanzaAttribute("platform", "gcm"));
        } else {
            removeDeviceNode.AddAttribute(new StanzaAttribute("platform", "apple"));
            removeDeviceNode.AddAttribute(new StanzaAttribute("preview", "1"));
            removeDeviceNode.AddAttribute(new StanzaAttribute("background_location", "1"));
            removeDeviceNode.AddAttribute(new StanzaAttribute("version", "2"));
            removeDeviceNode.AddAttribute(new StanzaAttribute("lg", envBuilder_.getUserAgentBuilder().getLocaleLanguageIso6391()));
            removeDeviceNode.AddAttribute(new StanzaAttribute("voip", HexUtil.encodeHexStr(RandomUtil.randomBytes(32))));
            removeDeviceNode.AddAttribute(new StanzaAttribute("voip_payload_type", "2"));
            removeDeviceNode.AddAttribute(new StanzaAttribute("default", "note.m4r"));
            removeDeviceNode.AddAttribute(new StanzaAttribute("lc", envBuilder_.getUserAgentBuilder().getLocaleCountryIso31661Alpha2()));
            removeDeviceNode.AddAttribute(new StanzaAttribute("nse_ver", "2"));
            removeDeviceNode.AddAttribute(new StanzaAttribute("reg_push", "1"));
            removeDeviceNode.AddAttribute(new StanzaAttribute("status_sound", "off"));
            removeDeviceNode.AddAttribute(new StanzaAttribute("nse_call", "0"));
            removeDeviceNode.AddAttribute(new StanzaAttribute("nse_read", "0"));
            removeDeviceNode.AddAttribute(new StanzaAttribute("groups", "note.m4r"));
            removeDeviceNode.AddAttribute(new StanzaAttribute("call", "Opening.m4r"));
            removeDeviceNode.AddAttribute(new StanzaAttribute("pkey", cn.hutool.core.codec.Base64.encode(RandomUtil.randomBytes(32))));
        }

        node.AddChild(removeDeviceNode);
        AddTask(node);
    }

    public void sendPn(String pn, NodeCallback callback) {
        //先发送一个pn包
        ProtocolTreeNode node = new ProtocolTreeNode("iq");
        node.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        node.AddAttribute(new StanzaAttribute("id", GenerateIqId()));
        node.AddAttribute(new StanzaAttribute("xmlns", "urn:xmpp:whatsapp:push"));
        node.AddAttribute(new StanzaAttribute("type", "get"));
        ProtocolTreeNode removeDeviceNode = new ProtocolTreeNode("pn");
        removeDeviceNode.SetData(pn.getBytes(StandardCharsets.UTF_8));
        node.AddChild(removeDeviceNode);
        AddTask(node, callback);
    }

    void createGoogleCrypto() {
        //<iq to='s.whatsapp.net' xmlns='urn:xmpp:whatsapp:account' type='get' id='06'><crypto action='create'><google>GatyzzGMqKq+hdtbx9qQFYTvev+yEZnODwEUs+1iNDI=</google></crypto></iq>
        ProtocolTreeNode available = new ProtocolTreeNode("iq");
        available.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        available.AddAttribute(new StanzaAttribute("xmlns", "urn:xmpp:whatsapp:account"));
        available.AddAttribute(new StanzaAttribute("type", "get"));
        available.AddAttribute(new StanzaAttribute("id", GenerateIqId()));
        ProtocolTreeNode protocolTreeNode = new ProtocolTreeNode("crypto");
        protocolTreeNode.AddAttribute(new StanzaAttribute("action", "create"));
        ProtocolTreeNode googleNode = new ProtocolTreeNode("google");
        googleNode.SetData(cn.hutool.core.codec.Base64.decode("GatyzzGMqKq+hdtbx9qQFYTvev+yEZnODwEUs+1iNDI="));
        protocolTreeNode.AddChild(googleNode);
        available.AddChild(protocolTreeNode);
        AddTask(available);
    }

    void xMppPush(NodeCallback callback) {
        //<iq id='1' xmlns='urn:xmpp:whatsapp:push' type='get' to='s.whatsapp.net'><config version='1'/></iq>
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("id", GenerateIqId()));
        iq.AddAttribute(new StanzaAttribute("xmlns", "urn:xmpp:whatsapp:push"));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        ProtocolTreeNode configNode = new ProtocolTreeNode("config");
        configNode.AddAttribute(new StanzaAttribute("version", "1"));
        iq.AddChild(configNode);
        AddTask(iq, callback);
    }

    void passive() {
        //<iq id='2' xmlns='passive' type='set' to='s.whatsapp.net'><active/></iq>
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("id", GenerateIqId()));
        iq.AddAttribute(new StanzaAttribute("xmlns", "passive"));
        iq.AddAttribute(new StanzaAttribute("type", "set"));
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        ProtocolTreeNode activeNode = new ProtocolTreeNode("active");
        iq.AddChild(activeNode);
        AddTask(iq);
    }

    public void checkRegistrationId() {
        //<iq id='4' xmlns='encrypt' type='get' to='s.whatsapp.net'><digest/></iq>
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("id", GenerateIqId()));
        iq.AddAttribute(new StanzaAttribute("xmlns", "encrypt"));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        ProtocolTreeNode digestNode = new ProtocolTreeNode("digest");
        iq.AddChild(digestNode);
        AddTask(iq, (srcNode, result) -> {
            //<iq from='s.whatsapp.net' type='result' id='02'><digest><registration>FJIbmw==</registration><type>BQ==</type><identity>5DxOCRs7m9rJ3l-vfaG2E3Ox8LUcglGl6EERS_ChvQ4=</identity><skey><id>AAAA</id><value>1iJr52ji0oAX-PpwdSjpFj8hs1UyiZCMHdkCB6QTZ3U=</value><signature>5hw7KoT2mOKrvQwfgc5Oy7PjowMR93ads2FKQ4gBUdHcONrEU4OhqTW7_tPhFLpT9nD18AQzz8ybjDpVMZU8Cw==</signature></skey><list><id>AEHu</id><id>AEHv</id><id>AEHw</id><id>AEHx</id><id>AEHy</id><id>AEHz</id><id>AEH0</id><id>AEH1</id><id>AEH2</id><id>AEH3</id><id>AEH4</id><id>AEH5</id><id>AEH6</id><id>AEH7</id><id>AEH8</id><id>AEH9</id><id>AEH-</id><id>AEH_</id><id>AEIA</id><id>AEIB</id><id>AEIC</id><id>AEID</id><id>AEIE</id><id>AEIF</id><id>AEIG</id><id>AEIH</id><id>AEII</id><id>AEIJ</id><id>AEIK</id><id>AEIL</id><id>AEIM</id><id>AEIN</id><id>AEIO</id><id>AEIP</id><id>AEIQ</id><id>AEIR</id><id>AEIS</id><id>AEIT</id><id>AEIU</id><id>AEIV</id><id>AEIW</id><id>AEIX</id><id>AEIY</id><id>AEIZ</id><id>AEIa</id><id>AEIb</id><id>AEIc</id><id>AEId</id><id>AEIe</id><id>AEIf</id><id>AEIg</id><id>AEIh</id><id>AEIi</id><id>AEIj</id><id>AEIk</id><id>AEIl</id><id>AEIm</id><id>AEIn</id><id>AEIo</id><id>AEIp</id><id>AEIq</id><id>AEIr</id><id>AEIs</id><id>AEIt</id><id>AEIu</id><id>AEIv</id><id>AEIw</id><id>AEIx</id><id>AEIy</id><id>AEIz</id><id>AEI0</id><id>AEI1</id><id>AEI2</id><id>AEI3</id><id>AEI4</id><id>AEI5</id><id>AEI6</id><id>AEI7</id><id>AEI8</id><id>AEI9</id><id>AEI-</id><id>AEI_</id><id>AEJA</id><id>AEJB</id><id>AEJC</id><id>AEJD</id><id>AEJE</id><id>AEJF</id><id>AEJG</id><id>AEJH</id><id>AEJI</id><id>AEJJ</id><id>AEJK</id><id>AEJL</id><id>AEJM</id><id>AEJN</id><id>AEJO</id><id>AEJP</id><id>AEJQ</id><id>AEJR</id><id>AEJS</id><id>AEJT</id><id>AEJU</id><id>AEJV</id><id>AEJW</id><id>AEJX</id><id>AEJY</id><id>AEJZ</id><id>AEJa</id><id>AEJb</id><id>AEJc</id><id>AEJd</id><id>AEJe</id><id>AEJf</id><id>AEJg</id><id>AEJh</id><id>AEJi</id><id>AEJj</id><id>AEJk</id><id>AEJl</id><id>AEJm</id><id>AEJn</id><id>AEJo</id><id>AEJp</id><id>AEJq</id><id>AEJr</id><id>AEJs</id><id>AEJt</id><id>AEJu</id><id>AEJv</id><id>AEJw</id><id>AEJx</id><id>AEJy</id><id>AEJz</id><id>AEJ0</id><id>AEJ1</id><id>AEJ2</id><id>AEJ3</id><id>AEJ4</id><id>AEJ5</id><id>AEJ6</id><id>AEJ7</id><id>AEJ8</id><id>AEJ9</id><id>AEJ-</id><id>AEJ_</id><id>AEKA</id><id>AEKB</id><id>AEKC</id><id>AEKD</id><id>AEKE</id><id>AEKF</id><id>AEKG</id><id>AEKH</id><id>AEKI</id><id>AEKJ</id><id>AEKK</id><id>AEKL</id><id>AEKM</id><id>AEKN</id><id>AEKO</id><id>AEKP</id><id>AEKQ</id><id>AEKR</id><id>AEKS</id><id>AEKT</id><id>AEKU</id><id>AEKV</id><id>AEKW</id><id>AEKX</id><id>AEKY</id><id>AEKZ</id><id>AEKa</id><id>AEKb</id><id>AEKc</id><id>AEKd</id><id>AEKe</id><id>AEKf</id><id>AEKg</id><id>AEKh</id><id>AEKi</id><id>AEKj</id><id>AEKk</id><id>AEKl</id><id>AEKm</id><id>AEKn</id><id>AEKo</id><id>AEKp</id><id>AEKq</id><id>AEKr</id><id>AEKs</id><id>AEKt</id><id>AEKu</id><id>AEKv</id><id>AEKw</id><id>AEKx</id><id>AEKy</id><id>AEKz</id><id>AEK0</id><id>AEK1</id><id>AEK2</id><id>AEK3</id><id>AEK4</id><id>AEK5</id><id>AEK6</id><id>AEK7</id><id>AEK8</id><id>AEK9</id><id>AEK-</id><id>AEK_</id><id>AELA</id><id>AELB</id><id>AELC</id><id>AELD</id><id>AELE</id><id>AELF</id><id>AELG</id><id>AELH</id><id>AELI</id><id>AELJ</id><id>AELK</id><id>AELL</id><id>AELM</id><id>AELN</id><id>AELO</id><id>AELP</id><id>AELQ</id><id>AELR</id><id>AELS</id><id>AELT</id><id>AELU</id><id>AELV</id><id>AELW</id><id>AELX</id><id>AELY</id><id>AELZ</id><id>AELa</id><id>AELb</id><id>AELc</id><id>AELd</id><id>AELe</id><id>AELf</id><id>AELg</id><id>AELh</id><id>AELi</id><id>AELj</id><id>AELk</id><id>AELl</id><id>AELm</id><id>AELn</id><id>AELo</id><id>AELp</id><id>AELq</id><id>AELr</id><id>AELs</id><id>AELt</id><id>AELu</id><id>AELv</id><id>AELw</id><id>AELx</id><id>AELy</id><id>AELz</id><id>AEL0</id><id>AEL1</id><id>AEL2</id><id>AEL3</id><id>AEL4</id><id>AEL5</id><id>AEL6</id><id>AEL7</id><id>AEL8</id><id>AEL9</id><id>AEL-</id><id>AEL_</id><id>AEMA</id><id>AEMB</id><id>AEMC</id><id>AEMD</id><id>AEME</id><id>AEMF</id><id>AEMG</id><id>AEMH</id><id>AEMI</id><id>AEMJ</id><id>AEMK</id><id>AEML</id><id>AEMM</id><id>AEMN</id><id>AEMO</id><id>AEMP</id><id>AEMQ</id><id>AEMR</id><id>AEMS</id><id>AEMT</id><id>AEMU</id><id>AEMV</id><id>AEMW</id><id>AEMX</id><id>AEMY</id><id>AEMZ</id><id>AEMa</id><id>AEMb</id><id>AEMc</id><id>AEMd</id><id>AEMe</id><id>AEMf</id><id>AEMg</id><id>AEMh</id><id>AEMi</id><id>AEMj</id><id>AEMk</id><id>AEMl</id><id>AEMm</id><id>AEMn</id><id>AEMo</id><id>AEMp</id><id>AEMq</id><id>AEMr</id><id>AEMs</id><id>AEMt</id><id>AEMu</id><id>AEMv</id><id>AEMw</id><id>AEMx</id><id>AEMy</id><id>AEMz</id><id>AEM0</id><id>AEM1</id><id>AEM2</id><id>AEM3</id><id>AEM4</id><id>AEM5</id><id>AEM6</id><id>AEM7</id><id>AEM8</id><id>AEM9</id><id>AEM-</id><id>AEM_</id><id>AENA</id><id>AENB</id><id>AENC</id><id>AEND</id><id>AENE</id><id>AENF</id><id>AENG</id><id>AENH</id><id>AENI</id><id>AENJ</id><id>AENK</id><id>AENL</id><id>AENM</id><id>AENN</id><id>AENO</id><id>AENP</id><id>AENQ</id><id>AENR</id><id>AENS</id><id>AENT</id><id>AENU</id><id>AENV</id><id>AENW</id><id>AENX</id><id>AENY</id><id>AENZ</id><id>AENa</id><id>AENb</id><id>AENc</id><id>AENd</id><id>AENe</id><id>AENf</id><id>AENg</id><id>AENh</id><id>AENi</id><id>AENj</id><id>AENk</id><id>AENl</id><id>AENm</id><id>AENn</id><id>AENo</id><id>AENp</id><id>AENq</id><id>AENr</id><id>AENs</id><id>AENt</id><id>AENu</id><id>AENv</id><id>AENw</id><id>AENx</id><id>AENy</id><id>AENz</id><id>AEN0</id><id>AEN1</id><id>AEN2</id><id>AEN3</id><id>AEN4</id><id>AEN5</id><id>AEN6</id><id>AEN7</id><id>AEN8</id><id>AEN9</id><id>AEN-</id><id>AEN_</id><id>AEOA</id><id>AEOB</id><id>AEOC</id><id>AEOD</id><id>AEOE</id><id>AEOF</id><id>AEOG</id><id>AEOH</id><id>AEOI</id><id>AEOJ</id><id>AEOK</id><id>AEOL</id><id>AEOM</id><id>AEON</id><id>AEOO</id><id>AEOP</id><id>AEOQ</id><id>AEOR</id><id>AEOS</id><id>AEOT</id><id>AEOU</id><id>AEOV</id><id>AEOW</id><id>AEOX</id><id>AEOY</id><id>AEOZ</id><id>AEOa</id><id>AEOb</id><id>AEOc</id><id>AEOd</id><id>AEOe</id><id>AEOf</id><id>AEOg</id><id>AEOh</id><id>AEOi</id><id>AEOj</id><id>AEOk</id><id>AEOl</id><id>AEOm</id><id>AEOn</id><id>AEOo</id><id>AEOp</id><id>AEOq</id><id>AEOr</id><id>AEOs</id><id>AEOt</id><id>AEOu</id><id>AEOv</id><id>AEOw</id><id>AEOx</id><id>AEOy</id><id>AEOz</id><id>AEO0</id><id>AEO1</id><id>AEO2</id><id>AEO3</id><id>AEO4</id><id>AEO5</id><id>AEO6</id><id>AEO7</id><id>AEO8</id><id>AEO9</id><id>AEO-</id><id>AEO_</id><id>AEPA</id><id>AEPB</id><id>AEPC</id><id>AEPD</id><id>AEPE</id><id>AEPF</id><id>AEPG</id><id>AEPH</id><id>AEPI</id><id>AEPJ</id><id>AEPK</id><id>AEPL</id><id>AEPM</id><id>AEPN</id><id>AEPO</id><id>AEPP</id><id>AEPQ</id><id>AEPR</id><id>AEPS</id><id>AEPT</id><id>AEPU</id><id>AEPV</id><id>AEPW</id><id>AEPX</id><id>AEPY</id><id>AEPZ</id><id>AEPa</id><id>AEPb</id><id>AEPc</id><id>AEPd</id><id>AEPe</id><id>AEPf</id><id>AEPg</id><id>AEPh</id><id>AEPi</id><id>AEPj</id><id>AEPk</id><id>AEPl</id><id>AEPm</id><id>AEPn</id><id>AEPo</id><id>AEPp</id><id>AEPq</id><id>AEPr</id><id>AEPs</id><id>AEPt</id><id>AEPu</id><id>AEPv</id><id>AEPw</id><id>AEPx</id><id>AEPy</id><id>AEPz</id><id>AEP0</id><id>AEP1</id><id>AEP2</id><id>AEP3</id><id>AEP4</id><id>AEP5</id><id>AEP6</id><id>AEP7</id><id>AEP8</id><id>AEP9</id><id>AEP-</id><id>AEP_</id><id>AEQA</id><id>AEQB</id><id>AEQC</id><id>AEQD</id><id>AEQE</id><id>AEQF</id><id>AEQG</id><id>AEQH</id><id>AEQI</id><id>AEQJ</id><id>AEQK</id><id>AEQL</id><id>AEQM</id><id>AEQN</id><id>AEQO</id><id>AEQP</id><id>AEQQ</id><id>AEQR</id><id>AEQS</id><id>AEQT</id><id>AEQU</id><id>AEQV</id><id>AEQW</id><id>AEQX</id><id>AEQY</id><id>AEQZ</id><id>AEQa</id><id>AEQb</id><id>AEQc</id><id>AEQd</id><id>AEQe</id><id>AEQf</id><id>AEQg</id><id>AEQh</id><id>AEQi</id><id>AEQj</id><id>AEQk</id><id>AEQl</id><id>AEQm</id><id>AEQn</id><id>AEQo</id><id>AEQp</id><id>AEQq</id><id>AEQr</id><id>AEQs</id><id>AEQt</id><id>AEQu</id><id>AEQv</id><id>AEQw</id><id>AEQx</id><id>AEQy</id><id>AEQz</id><id>AEQ0</id><id>AEQ1</id><id>AEQ2</id><id>AEQ3</id><id>AEQ4</id><id>AEQ5</id><id>AEQ6</id><id>AEQ7</id><id>AEQ8</id><id>AEQ9</id><id>AEQ-</id><id>AEQ_</id><id>AERA</id><id>AERB</id><id>AERC</id><id>AERD</id><id>AERE</id><id>AERF</id><id>AERG</id><id>AERH</id><id>AERI</id><id>AERJ</id><id>AERK</id><id>AERL</id><id>AERM</id><id>AERN</id><id>AERO</id><id>AERP</id><id>AERQ</id><id>AERR</id><id>AERS</id><id>AERT</id><id>AERU</id><id>AERV</id><id>AERW</id><id>AERX</id><id>AERY</id><id>AERZ</id><id>AERa</id><id>AERb</id><id>AERc</id><id>AERd</id><id>AERe</id><id>AERf</id><id>AERg</id><id>AERh</id><id>AERi</id><id>AERj</id><id>AERk</id><id>AERl</id><id>AERm</id><id>AERn</id><id>AERo</id><id>AERp</id><id>AERq</id><id>AERr</id><id>AERs</id><id>AERt</id><id>AERu</id><id>AERv</id><id>AERw</id><id>AERx</id><id>AERy</id><id>AERz</id><id>AER0</id><id>AER1</id><id>AER2</id><id>AER3</id><id>AER4</id><id>AER5</id><id>AER6</id><id>AER7</id><id>AER8</id><id>AER9</id><id>AER-</id><id>AER_</id><id>AESA</id><id>AESB</id><id>AESC</id><id>AESD</id><id>AESE</id><id>AESF</id><id>AESG</id><id>AESH</id><id>AESI</id><id>AESJ</id><id>AESK</id><id>AESL</id><id>AESM</id><id>AESN</id><id>AESO</id><id>AESP</id><id>AESQ</id><id>AESR</id><id>AESS</id><id>AEST</id><id>AESU</id><id>AESV</id><id>AESW</id><id>AESX</id><id>AESY</id><id>AESZ</id><id>AESa</id><id>AESb</id><id>AESc</id><id>AESd</id><id>AESe</id><id>AESf</id><id>AESg</id><id>AESh</id><id>AESi</id><id>AESj</id><id>AESk</id><id>AESl</id><id>AESm</id><id>AESn</id><id>AESo</id><id>AESp</id><id>AESq</id><id>AESr</id><id>AESs</id><id>AESt</id><id>AESu</id><id>AESv</id><id>AESw</id><id>AESx</id><id>AESy</id><id>AESz</id><id>AES0</id><id>AES1</id><id>AES2</id><id>AES3</id><id>AES4</id><id>AES5</id><id>AES6</id><id>AES7</id><id>AES8</id><id>AES9</id><id>AES-</id><id>AES_</id><id>AETA</id><id>AETB</id><id>AETC</id><id>AETD</id><id>AETE</id><id>AETF</id><id>AETG</id><id>AETH</id><id>AETI</id><id>AETJ</id><id>AETK</id><id>AETL</id><id>AETM</id><id>AETN</id><id>AETO</id><id>AETP</id><id>AETQ</id><id>AETR</id><id>AETS</id><id>AETT</id><id>AETU</id><id>AETV</id><id>AETW</id><id>AETX</id><id>AETY</id><id>AETZ</id><id>AETa</id><id>AETb</id><id>AETc</id><id>AETd</id><id>AETe</id><id>AETf</id><id>AETg</id><id>AETh</id><id>AETi</id><id>AETj</id><id>AETk</id><id>AETl</id><id>AETm</id><id>AETn</id><id>AETo</id><id>AETp</id><id>AETq</id><id>AETr</id><id>AETs</id><id>AETt</id><id>AETu</id><id>AETv</id><id>AETw</id><id>AETx</id><id>AETy</id><id>AETz</id><id>AET0</id><id>AET1</id><id>AET2</id><id>AET3</id><id>AET4</id><id>AET5</id><id>AET6</id><id>AET7</id><id>AET8</id><id>AET9</id><id>AET-</id><id>AET_</id><id>AEUA</id><id>AEUB</id><id>AEUC</id><id>AEUD</id><id>AEUE</id><id>AEUF</id><id>AEUG</id><id>AEUH</id><id>AEUI</id><id>AEUJ</id><id>AEUK</id><id>AEUL</id><id>AEUM</id><id>AEUN</id><id>AEUO</id><id>AEUP</id><id>AEUQ</id><id>AEUR</id><id>AEUS</id><id>AEUT</id><id>AEUU</id><id>AEUV</id><id>AEUW</id><id>AEUX</id><id>AEUY</id><id>AEUZ</id></list><hash>TE5zXgi_RvY5OM1doiJvrcspe1U=</hash></digest></iq>
            ProtocolTreeNode digest = result.getOneChildren("digest");
            if (digest != null) {
                ProtocolTreeNode identity = digest.getOneChildren("identity");
                if (identity != null) {
                    byte[] bytes = identity.GetData();
                    IdentityKeyPair identityKeyPair = axolotlManager_.GetIdentityKeyPair();
                    byte[] data = null;
                    byte[] privateKey = null;
                    if (identityKeyPair != null) {
                        byte[] serialize = identityKeyPair.getPublicKey().serialize();
                        byte[] originPrivateKey = identityKeyPair.getPrivateKey().serialize();
                        data = new byte[32];
                        privateKey = new byte[32];
                        System.arraycopy(serialize, 1, data, 0, 32);
                        System.arraycopy(originPrivateKey, 0, privateKey, 0, 32);
                    }
                    if (!Arrays.equals(bytes, data)) {
                        log.info("用户：{}，本地公钥与服务器不一致, 需要重新上传", username);
                        if (ObjectUtil.isNotNull(data) && ObjectUtil.isNotNull(privateKey)) {
                            // 判断原本的identityKey是否为一对
                            DHState dh = Noise.createDH("25519");
                            dh.setPrivateKey(privateKey, 0);
                            byte[] pk = new byte[32];
                            dh.getPublicKey(pk, 0);
                            if (!Arrays.equals(data, pk)) {
                                axolotlManager_.delPreKeysSent();
                                axolotlManager_.delIdentities();
                                axolotlManager_.getIdentityKeyStore_().clearIdentityKeyPair();
                                axolotlManager_.delSignPreKeys();
                                log.info("用户：{}，identity不一致，重新生成", username);
                            }
                        }
                        // 否则本身就为空, 则代表之前已经删除过
                    } else {
                        // 先判断registration_id是否对的上
                        ProtocolTreeNode registration = digest.getOneChildren("registration");
                        if (registration != null) {
                            byte[] serverRegistrationId = registration.GetData();
                            ByteBuffer buffer = ByteBuffer.wrap(serverRegistrationId);
                            int id = buffer.getInt();
                            if (id != axolotlManager_.getLocalRegistrationId()) {
                                // 判断不是为新号刚入库, 避免重复删除已生成的新的prekeys
                                try {
                                    KeyLockUtil.lock(username);
                                    // 更新registrationId与服务器保持一致
                                    axolotlManager_.updateRegistrationId(id);
                                    if (!axolotlManager_.isPreKeysInit()) {
                                        log.info("用户: {}, 服务器与本地RegistrationId不相等, 需要重新上传一次性密钥", username);
                                        axolotlManager_.delPreKeysSent();
                                    }
                                } catch (Exception ignore) {
                                } finally {
                                    KeyLockUtil.unlock(username);
                                }
                            }
                        }
                    }
                }
            }
            IdentityKeyPair identityKeyPair = axolotlManager_.GetIdentityKeyPair();
            if (identityKeyPair == null) {
                try {
                    KeyLockUtil.lock(username);
                    axolotlManager_.delPreKeysSent();
                    axolotlManager_.delIdentities();
                    axolotlManager_.getIdentityKeyStore_().clearIdentityKeyPair();
                    axolotlManager_.delSignPreKeys();
                    axolotlManager_.InitInstallData();
                } catch (Exception ignore) {
                } finally {
                    KeyLockUtil.unlock(username);
                }
            }
        });
    }

    public void digest() {
        //<iq id='4' xmlns='encrypt' type='get' to='s.whatsapp.net'><digest/></iq>
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("id", GenerateIqId()));
        iq.AddAttribute(new StanzaAttribute("xmlns", "encrypt"));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        ProtocolTreeNode digestNode = new ProtocolTreeNode("digest");
        iq.AddChild(digestNode);
        AddTask(iq, (srcNode, result) -> {
            //<iq from='s.whatsapp.net' type='result' id='02'><digest><registration>FJIbmw==</registration><type>BQ==</type><identity>5DxOCRs7m9rJ3l-vfaG2E3Ox8LUcglGl6EERS_ChvQ4=</identity><skey><id>AAAA</id><value>1iJr52ji0oAX-PpwdSjpFj8hs1UyiZCMHdkCB6QTZ3U=</value><signature>5hw7KoT2mOKrvQwfgc5Oy7PjowMR93ads2FKQ4gBUdHcONrEU4OhqTW7_tPhFLpT9nD18AQzz8ybjDpVMZU8Cw==</signature></skey><list><id>AEHu</id><id>AEHv</id><id>AEHw</id><id>AEHx</id><id>AEHy</id><id>AEHz</id><id>AEH0</id><id>AEH1</id><id>AEH2</id><id>AEH3</id><id>AEH4</id><id>AEH5</id><id>AEH6</id><id>AEH7</id><id>AEH8</id><id>AEH9</id><id>AEH-</id><id>AEH_</id><id>AEIA</id><id>AEIB</id><id>AEIC</id><id>AEID</id><id>AEIE</id><id>AEIF</id><id>AEIG</id><id>AEIH</id><id>AEII</id><id>AEIJ</id><id>AEIK</id><id>AEIL</id><id>AEIM</id><id>AEIN</id><id>AEIO</id><id>AEIP</id><id>AEIQ</id><id>AEIR</id><id>AEIS</id><id>AEIT</id><id>AEIU</id><id>AEIV</id><id>AEIW</id><id>AEIX</id><id>AEIY</id><id>AEIZ</id><id>AEIa</id><id>AEIb</id><id>AEIc</id><id>AEId</id><id>AEIe</id><id>AEIf</id><id>AEIg</id><id>AEIh</id><id>AEIi</id><id>AEIj</id><id>AEIk</id><id>AEIl</id><id>AEIm</id><id>AEIn</id><id>AEIo</id><id>AEIp</id><id>AEIq</id><id>AEIr</id><id>AEIs</id><id>AEIt</id><id>AEIu</id><id>AEIv</id><id>AEIw</id><id>AEIx</id><id>AEIy</id><id>AEIz</id><id>AEI0</id><id>AEI1</id><id>AEI2</id><id>AEI3</id><id>AEI4</id><id>AEI5</id><id>AEI6</id><id>AEI7</id><id>AEI8</id><id>AEI9</id><id>AEI-</id><id>AEI_</id><id>AEJA</id><id>AEJB</id><id>AEJC</id><id>AEJD</id><id>AEJE</id><id>AEJF</id><id>AEJG</id><id>AEJH</id><id>AEJI</id><id>AEJJ</id><id>AEJK</id><id>AEJL</id><id>AEJM</id><id>AEJN</id><id>AEJO</id><id>AEJP</id><id>AEJQ</id><id>AEJR</id><id>AEJS</id><id>AEJT</id><id>AEJU</id><id>AEJV</id><id>AEJW</id><id>AEJX</id><id>AEJY</id><id>AEJZ</id><id>AEJa</id><id>AEJb</id><id>AEJc</id><id>AEJd</id><id>AEJe</id><id>AEJf</id><id>AEJg</id><id>AEJh</id><id>AEJi</id><id>AEJj</id><id>AEJk</id><id>AEJl</id><id>AEJm</id><id>AEJn</id><id>AEJo</id><id>AEJp</id><id>AEJq</id><id>AEJr</id><id>AEJs</id><id>AEJt</id><id>AEJu</id><id>AEJv</id><id>AEJw</id><id>AEJx</id><id>AEJy</id><id>AEJz</id><id>AEJ0</id><id>AEJ1</id><id>AEJ2</id><id>AEJ3</id><id>AEJ4</id><id>AEJ5</id><id>AEJ6</id><id>AEJ7</id><id>AEJ8</id><id>AEJ9</id><id>AEJ-</id><id>AEJ_</id><id>AEKA</id><id>AEKB</id><id>AEKC</id><id>AEKD</id><id>AEKE</id><id>AEKF</id><id>AEKG</id><id>AEKH</id><id>AEKI</id><id>AEKJ</id><id>AEKK</id><id>AEKL</id><id>AEKM</id><id>AEKN</id><id>AEKO</id><id>AEKP</id><id>AEKQ</id><id>AEKR</id><id>AEKS</id><id>AEKT</id><id>AEKU</id><id>AEKV</id><id>AEKW</id><id>AEKX</id><id>AEKY</id><id>AEKZ</id><id>AEKa</id><id>AEKb</id><id>AEKc</id><id>AEKd</id><id>AEKe</id><id>AEKf</id><id>AEKg</id><id>AEKh</id><id>AEKi</id><id>AEKj</id><id>AEKk</id><id>AEKl</id><id>AEKm</id><id>AEKn</id><id>AEKo</id><id>AEKp</id><id>AEKq</id><id>AEKr</id><id>AEKs</id><id>AEKt</id><id>AEKu</id><id>AEKv</id><id>AEKw</id><id>AEKx</id><id>AEKy</id><id>AEKz</id><id>AEK0</id><id>AEK1</id><id>AEK2</id><id>AEK3</id><id>AEK4</id><id>AEK5</id><id>AEK6</id><id>AEK7</id><id>AEK8</id><id>AEK9</id><id>AEK-</id><id>AEK_</id><id>AELA</id><id>AELB</id><id>AELC</id><id>AELD</id><id>AELE</id><id>AELF</id><id>AELG</id><id>AELH</id><id>AELI</id><id>AELJ</id><id>AELK</id><id>AELL</id><id>AELM</id><id>AELN</id><id>AELO</id><id>AELP</id><id>AELQ</id><id>AELR</id><id>AELS</id><id>AELT</id><id>AELU</id><id>AELV</id><id>AELW</id><id>AELX</id><id>AELY</id><id>AELZ</id><id>AELa</id><id>AELb</id><id>AELc</id><id>AELd</id><id>AELe</id><id>AELf</id><id>AELg</id><id>AELh</id><id>AELi</id><id>AELj</id><id>AELk</id><id>AELl</id><id>AELm</id><id>AELn</id><id>AELo</id><id>AELp</id><id>AELq</id><id>AELr</id><id>AELs</id><id>AELt</id><id>AELu</id><id>AELv</id><id>AELw</id><id>AELx</id><id>AELy</id><id>AELz</id><id>AEL0</id><id>AEL1</id><id>AEL2</id><id>AEL3</id><id>AEL4</id><id>AEL5</id><id>AEL6</id><id>AEL7</id><id>AEL8</id><id>AEL9</id><id>AEL-</id><id>AEL_</id><id>AEMA</id><id>AEMB</id><id>AEMC</id><id>AEMD</id><id>AEME</id><id>AEMF</id><id>AEMG</id><id>AEMH</id><id>AEMI</id><id>AEMJ</id><id>AEMK</id><id>AEML</id><id>AEMM</id><id>AEMN</id><id>AEMO</id><id>AEMP</id><id>AEMQ</id><id>AEMR</id><id>AEMS</id><id>AEMT</id><id>AEMU</id><id>AEMV</id><id>AEMW</id><id>AEMX</id><id>AEMY</id><id>AEMZ</id><id>AEMa</id><id>AEMb</id><id>AEMc</id><id>AEMd</id><id>AEMe</id><id>AEMf</id><id>AEMg</id><id>AEMh</id><id>AEMi</id><id>AEMj</id><id>AEMk</id><id>AEMl</id><id>AEMm</id><id>AEMn</id><id>AEMo</id><id>AEMp</id><id>AEMq</id><id>AEMr</id><id>AEMs</id><id>AEMt</id><id>AEMu</id><id>AEMv</id><id>AEMw</id><id>AEMx</id><id>AEMy</id><id>AEMz</id><id>AEM0</id><id>AEM1</id><id>AEM2</id><id>AEM3</id><id>AEM4</id><id>AEM5</id><id>AEM6</id><id>AEM7</id><id>AEM8</id><id>AEM9</id><id>AEM-</id><id>AEM_</id><id>AENA</id><id>AENB</id><id>AENC</id><id>AEND</id><id>AENE</id><id>AENF</id><id>AENG</id><id>AENH</id><id>AENI</id><id>AENJ</id><id>AENK</id><id>AENL</id><id>AENM</id><id>AENN</id><id>AENO</id><id>AENP</id><id>AENQ</id><id>AENR</id><id>AENS</id><id>AENT</id><id>AENU</id><id>AENV</id><id>AENW</id><id>AENX</id><id>AENY</id><id>AENZ</id><id>AENa</id><id>AENb</id><id>AENc</id><id>AENd</id><id>AENe</id><id>AENf</id><id>AENg</id><id>AENh</id><id>AENi</id><id>AENj</id><id>AENk</id><id>AENl</id><id>AENm</id><id>AENn</id><id>AENo</id><id>AENp</id><id>AENq</id><id>AENr</id><id>AENs</id><id>AENt</id><id>AENu</id><id>AENv</id><id>AENw</id><id>AENx</id><id>AENy</id><id>AENz</id><id>AEN0</id><id>AEN1</id><id>AEN2</id><id>AEN3</id><id>AEN4</id><id>AEN5</id><id>AEN6</id><id>AEN7</id><id>AEN8</id><id>AEN9</id><id>AEN-</id><id>AEN_</id><id>AEOA</id><id>AEOB</id><id>AEOC</id><id>AEOD</id><id>AEOE</id><id>AEOF</id><id>AEOG</id><id>AEOH</id><id>AEOI</id><id>AEOJ</id><id>AEOK</id><id>AEOL</id><id>AEOM</id><id>AEON</id><id>AEOO</id><id>AEOP</id><id>AEOQ</id><id>AEOR</id><id>AEOS</id><id>AEOT</id><id>AEOU</id><id>AEOV</id><id>AEOW</id><id>AEOX</id><id>AEOY</id><id>AEOZ</id><id>AEOa</id><id>AEOb</id><id>AEOc</id><id>AEOd</id><id>AEOe</id><id>AEOf</id><id>AEOg</id><id>AEOh</id><id>AEOi</id><id>AEOj</id><id>AEOk</id><id>AEOl</id><id>AEOm</id><id>AEOn</id><id>AEOo</id><id>AEOp</id><id>AEOq</id><id>AEOr</id><id>AEOs</id><id>AEOt</id><id>AEOu</id><id>AEOv</id><id>AEOw</id><id>AEOx</id><id>AEOy</id><id>AEOz</id><id>AEO0</id><id>AEO1</id><id>AEO2</id><id>AEO3</id><id>AEO4</id><id>AEO5</id><id>AEO6</id><id>AEO7</id><id>AEO8</id><id>AEO9</id><id>AEO-</id><id>AEO_</id><id>AEPA</id><id>AEPB</id><id>AEPC</id><id>AEPD</id><id>AEPE</id><id>AEPF</id><id>AEPG</id><id>AEPH</id><id>AEPI</id><id>AEPJ</id><id>AEPK</id><id>AEPL</id><id>AEPM</id><id>AEPN</id><id>AEPO</id><id>AEPP</id><id>AEPQ</id><id>AEPR</id><id>AEPS</id><id>AEPT</id><id>AEPU</id><id>AEPV</id><id>AEPW</id><id>AEPX</id><id>AEPY</id><id>AEPZ</id><id>AEPa</id><id>AEPb</id><id>AEPc</id><id>AEPd</id><id>AEPe</id><id>AEPf</id><id>AEPg</id><id>AEPh</id><id>AEPi</id><id>AEPj</id><id>AEPk</id><id>AEPl</id><id>AEPm</id><id>AEPn</id><id>AEPo</id><id>AEPp</id><id>AEPq</id><id>AEPr</id><id>AEPs</id><id>AEPt</id><id>AEPu</id><id>AEPv</id><id>AEPw</id><id>AEPx</id><id>AEPy</id><id>AEPz</id><id>AEP0</id><id>AEP1</id><id>AEP2</id><id>AEP3</id><id>AEP4</id><id>AEP5</id><id>AEP6</id><id>AEP7</id><id>AEP8</id><id>AEP9</id><id>AEP-</id><id>AEP_</id><id>AEQA</id><id>AEQB</id><id>AEQC</id><id>AEQD</id><id>AEQE</id><id>AEQF</id><id>AEQG</id><id>AEQH</id><id>AEQI</id><id>AEQJ</id><id>AEQK</id><id>AEQL</id><id>AEQM</id><id>AEQN</id><id>AEQO</id><id>AEQP</id><id>AEQQ</id><id>AEQR</id><id>AEQS</id><id>AEQT</id><id>AEQU</id><id>AEQV</id><id>AEQW</id><id>AEQX</id><id>AEQY</id><id>AEQZ</id><id>AEQa</id><id>AEQb</id><id>AEQc</id><id>AEQd</id><id>AEQe</id><id>AEQf</id><id>AEQg</id><id>AEQh</id><id>AEQi</id><id>AEQj</id><id>AEQk</id><id>AEQl</id><id>AEQm</id><id>AEQn</id><id>AEQo</id><id>AEQp</id><id>AEQq</id><id>AEQr</id><id>AEQs</id><id>AEQt</id><id>AEQu</id><id>AEQv</id><id>AEQw</id><id>AEQx</id><id>AEQy</id><id>AEQz</id><id>AEQ0</id><id>AEQ1</id><id>AEQ2</id><id>AEQ3</id><id>AEQ4</id><id>AEQ5</id><id>AEQ6</id><id>AEQ7</id><id>AEQ8</id><id>AEQ9</id><id>AEQ-</id><id>AEQ_</id><id>AERA</id><id>AERB</id><id>AERC</id><id>AERD</id><id>AERE</id><id>AERF</id><id>AERG</id><id>AERH</id><id>AERI</id><id>AERJ</id><id>AERK</id><id>AERL</id><id>AERM</id><id>AERN</id><id>AERO</id><id>AERP</id><id>AERQ</id><id>AERR</id><id>AERS</id><id>AERT</id><id>AERU</id><id>AERV</id><id>AERW</id><id>AERX</id><id>AERY</id><id>AERZ</id><id>AERa</id><id>AERb</id><id>AERc</id><id>AERd</id><id>AERe</id><id>AERf</id><id>AERg</id><id>AERh</id><id>AERi</id><id>AERj</id><id>AERk</id><id>AERl</id><id>AERm</id><id>AERn</id><id>AERo</id><id>AERp</id><id>AERq</id><id>AERr</id><id>AERs</id><id>AERt</id><id>AERu</id><id>AERv</id><id>AERw</id><id>AERx</id><id>AERy</id><id>AERz</id><id>AER0</id><id>AER1</id><id>AER2</id><id>AER3</id><id>AER4</id><id>AER5</id><id>AER6</id><id>AER7</id><id>AER8</id><id>AER9</id><id>AER-</id><id>AER_</id><id>AESA</id><id>AESB</id><id>AESC</id><id>AESD</id><id>AESE</id><id>AESF</id><id>AESG</id><id>AESH</id><id>AESI</id><id>AESJ</id><id>AESK</id><id>AESL</id><id>AESM</id><id>AESN</id><id>AESO</id><id>AESP</id><id>AESQ</id><id>AESR</id><id>AESS</id><id>AEST</id><id>AESU</id><id>AESV</id><id>AESW</id><id>AESX</id><id>AESY</id><id>AESZ</id><id>AESa</id><id>AESb</id><id>AESc</id><id>AESd</id><id>AESe</id><id>AESf</id><id>AESg</id><id>AESh</id><id>AESi</id><id>AESj</id><id>AESk</id><id>AESl</id><id>AESm</id><id>AESn</id><id>AESo</id><id>AESp</id><id>AESq</id><id>AESr</id><id>AESs</id><id>AESt</id><id>AESu</id><id>AESv</id><id>AESw</id><id>AESx</id><id>AESy</id><id>AESz</id><id>AES0</id><id>AES1</id><id>AES2</id><id>AES3</id><id>AES4</id><id>AES5</id><id>AES6</id><id>AES7</id><id>AES8</id><id>AES9</id><id>AES-</id><id>AES_</id><id>AETA</id><id>AETB</id><id>AETC</id><id>AETD</id><id>AETE</id><id>AETF</id><id>AETG</id><id>AETH</id><id>AETI</id><id>AETJ</id><id>AETK</id><id>AETL</id><id>AETM</id><id>AETN</id><id>AETO</id><id>AETP</id><id>AETQ</id><id>AETR</id><id>AETS</id><id>AETT</id><id>AETU</id><id>AETV</id><id>AETW</id><id>AETX</id><id>AETY</id><id>AETZ</id><id>AETa</id><id>AETb</id><id>AETc</id><id>AETd</id><id>AETe</id><id>AETf</id><id>AETg</id><id>AETh</id><id>AETi</id><id>AETj</id><id>AETk</id><id>AETl</id><id>AETm</id><id>AETn</id><id>AETo</id><id>AETp</id><id>AETq</id><id>AETr</id><id>AETs</id><id>AETt</id><id>AETu</id><id>AETv</id><id>AETw</id><id>AETx</id><id>AETy</id><id>AETz</id><id>AET0</id><id>AET1</id><id>AET2</id><id>AET3</id><id>AET4</id><id>AET5</id><id>AET6</id><id>AET7</id><id>AET8</id><id>AET9</id><id>AET-</id><id>AET_</id><id>AEUA</id><id>AEUB</id><id>AEUC</id><id>AEUD</id><id>AEUE</id><id>AEUF</id><id>AEUG</id><id>AEUH</id><id>AEUI</id><id>AEUJ</id><id>AEUK</id><id>AEUL</id><id>AEUM</id><id>AEUN</id><id>AEUO</id><id>AEUP</id><id>AEUQ</id><id>AEUR</id><id>AEUS</id><id>AEUT</id><id>AEUU</id><id>AEUV</id><id>AEUW</id><id>AEUX</id><id>AEUY</id><id>AEUZ</id></list><hash>TE5zXgi_RvY5OM1doiJvrcspe1U=</hash></digest></iq>
            ProtocolTreeNode digest = result.getOneChildren("digest");
            if (digest != null) {
                ProtocolTreeNode identity = digest.getOneChildren("identity");
                if (identity != null) {
                    byte[] bytes = identity.GetData();
                    IdentityKeyPair identityKeyPair = axolotlManager_.GetIdentityKeyPair();
                    byte[] data = null;
                    byte[] privateKey = null;
                    if (identityKeyPair != null) {
                        byte[] serialize = identityKeyPair.getPublicKey().serialize();
                        byte[] originPrivateKey = identityKeyPair.getPrivateKey().serialize();
                        data = new byte[32];
                        privateKey = new byte[32];
                        System.arraycopy(serialize, 1, data, 0, 32);
                        System.arraycopy(originPrivateKey, 0, privateKey, 0, 32);
                    }
                    if (!Arrays.equals(bytes, data)) {
                        log.info("用户：{}，本地公钥与服务器不一致, 需要重新上传", username);
                        if (ObjectUtil.isNotNull(data) && ObjectUtil.isNotNull(privateKey)) {
                            // 判断原本的identityKey是否为一对
                            DHState dh = Noise.createDH("25519");
                            dh.setPrivateKey(privateKey, 0);
                            byte[] pk = new byte[32];
                            dh.getPublicKey(pk, 0);
                            if (!Arrays.equals(data, pk)) {
                                axolotlManager_.delPreKeysSent();
                                axolotlManager_.delIdentities();
                                axolotlManager_.getIdentityKeyStore_().clearIdentityKeyPair();
                                axolotlManager_.delSignPreKeys();
                                log.info("用户：{}，identity不一致，重新生成", username);
                            }
                        }
                        // 否则本身就为空, 则代表之前已经删除过
                    } else {
                        // 先判断registration_id是否对的上
                        ProtocolTreeNode registration = digest.getOneChildren("registration");
                        if (registration != null) {
                            byte[] serverRegistrationId = registration.GetData();
                            ByteBuffer buffer = ByteBuffer.wrap(serverRegistrationId);
                            int id = buffer.getInt();
                            if (id != axolotlManager_.getLocalRegistrationId()) {
                                // 判断不是为新号刚入库, 避免重复删除已生成的新的prekeys
                                try {
                                    KeyLockUtil.lock(username);
                                    // 更新registrationId与服务器保持一致
                                    axolotlManager_.updateRegistrationId(id);
                                    if (!axolotlManager_.isPreKeysInit()) {
                                        log.info("用户: {}, 服务器与本地RegistrationId不相等, 需要重新上传一次性密钥", username);
                                        axolotlManager_.delPreKeysSent();
                                    }
                                } catch (Exception ignore) {
                                } finally {
                                    KeyLockUtil.unlock(username);
                                }
                            }
                            /**
                             else {
                             // 若registrationId与服务器一致, 判断一次性密钥是否与数据库一致
                             ProtocolTreeNode list = digest.getOneChildren("list");
                             if (ObjectUtil.isNotNull(list)) {
                             LinkedList<ProtocolTreeNode> ids = list.GetChildren("id");
                             // 随机抽取最少5个
                             List<ProtocolTreeNode> randomList = RandomUtil.randomEleList(ids, 5);
                             List<Integer> randomIds = new ArrayList<>();
                             for (ProtocolTreeNode protocolTreeNode : randomList) {
                             byte[] pid = protocolTreeNode.GetData();
                             if (ObjectUtil.isNotNull(pid)) {
                             randomIds.add(bytesToInt(pid));
                             }
                             }
                             int exist = 0;
                             try {
                             KeyLockUtil.lock(username);
                             // 判断数据库内是否有该prekey
                             for (Integer randomId : randomIds) {
                             if (axolotlManager_.preKeyStore_.containsPreKey(randomId)) {
                             exist++;
                             }
                             }
                             } catch (Exception ignore) {
                             } finally {
                             KeyLockUtil.unlock(username);
                             }
                             if (exist < randomIds.size()) {
                             log.info("用户: {}, 抽取服务器prekey数: {}, 存在数: {}, 需要重新上传预密钥", username, randomIds.size(), exist);
                             try {
                             KeyLockUtil.lock(username);
                             axolotlManager_.delPreKeysSent();
                             } catch (Exception ignore) {
                             } finally {
                             KeyLockUtil.unlock(username);
                             }
                             }
                             }
                             }
                             **/
                        }
                    }
                }
            }
            IdentityKeyPair identityKeyPair = axolotlManager_.GetIdentityKeyPair();
            if (identityKeyPair == null) {
                try {
                    KeyLockUtil.lock(username);
                    axolotlManager_.delPreKeysSent();
                    axolotlManager_.delIdentities();
                    axolotlManager_.getIdentityKeyStore_().clearIdentityKeyPair();
                    axolotlManager_.delSignPreKeys();
                    axolotlManager_.InitInstallData();
                } catch (Exception ignore) {
                } finally {
                    KeyLockUtil.unlock(username);
                }
            }
            LinkedList<PreKeyRecord> unsentPreKeys = new LinkedList<>();
            try {
                KeyLockUtil.lock(username);
                axolotlManager_.LevelPreKeys(false);
                unsentPreKeys = axolotlManager_.LoadUnSendPreKey();
            } catch (Exception ignore) {
            } finally {
                KeyLockUtil.unlock(username);
            }
            if (!unsentPreKeys.isEmpty()) {
                passive();
                FlushKeys(axolotlManager_.LoadLatestSignedPreKey(false), unsentPreKeys);
            }
        });
    }

    void forceUploadOneTimePreKeys() {
        LinkedList<PreKeyRecord> unsentPreKeys = new LinkedList<>();
        try {
            KeyLockUtil.lock(username);
            axolotlManager_.delPreKeysSent();
            axolotlManager_.LevelPreKeys(true);
            unsentPreKeys = axolotlManager_.LoadUnSendPreKey();
        } catch (Exception ignore) {
        } finally {
            KeyLockUtil.unlock(username);
        }
        if (!unsentPreKeys.isEmpty()) {
            FlushKeys(axolotlManager_.LoadLatestSignedPreKey(false), unsentPreKeys);
        }
    }

    void SendCredential() {
        byte[] bArr = new byte[32];
        NativeVOPRFExtension.Sha1Random(bArr);
        byte[] A032 = new NativeVOPRFExtension().A03(bArr, StringUtil.HexToBytes("05a6987286f0ac0adf19a48af09a42453d48a85a61ee44aec17ec53f53719e1d"), 32);
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("id", GenerateIqId()));
        iq.AddAttribute(new StanzaAttribute("xmlns", "privatestats"));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));

        ProtocolTreeNode sign_credential = new ProtocolTreeNode("sign_credential");
        sign_credential.AddAttribute(new StanzaAttribute("version", "1"));

        ProtocolTreeNode blinded_credential = new ProtocolTreeNode("blinded_credential");


        sign_credential.AddChild(blinded_credential);
        blinded_credential.SetData(A032);
        iq.AddChild(sign_credential);
        AddTask(iq, (srcNode, result) -> HandleCredential(result));
        envBuilder_.setOrignalToken(Base64.getUrlEncoder().encodeToString(bArr));
        envBuilder_.clearSharedSecret();
    }

    void HandleCredential(ProtocolTreeNode node) {
        ProtocolTreeNode sign_credential = node.GetChild("sign_credential");
        byte[] acs_public_key = sign_credential.GetChild("acs_public_key").GetData();
        byte[] signed_credential = sign_credential.GetChild("signed_credential").GetData();
        byte[] a3 = new NativeVOPRFExtension().A02(acs_public_key, 32, StringUtil.HexToBytes("05a6987286f0ac0adf19a48af09a42453d48a85a61ee44aec17ec53f53719e1d"), 32, signed_credential);
        try {
            byte[] bArr = Base64.getUrlDecoder().decode(envBuilder_.getOrignalToken());
            MessageDigest instance = MessageDigest.getInstance("SHA-512");
            instance.update(bArr, 0, bArr.length);
            instance.update(a3, 0, a3.length);
            byte[] digest = instance.digest();
            envBuilder_.setSharedSecret(Base64.getUrlEncoder().encodeToString(digest));
            IdentifiedTelemetry.Report(envBuilder_.build(), proxy_);
        } catch (Exception e) {
            log.error("上报异常", e);
        }
        SendAttestation();
    }

    //<iq id='01' xmlns='w:stats' type='set' to='s.whatsapp.net'><add t='1649508951'>V0FNBQEiAAAwCwIYpxyADQ9Bc3VzLUFTVVNfWjAxUUSI6woDYXRuUC9DY1FiOHkGBEAFLgGIpRMYMTc3NiwyNzQ2LDIyNjIsMzEwMCwzNDcwQANiAih7BoAPBTUuMS4xiO8BCkFTVVNfWjAxUUQoaxggaSAXSDkK3wdIjwLAAIh5ER4zWU0sSFksNDcsNnosMlEsNVEsN28sNWksNGIsMU6AEQkyLjIyLjMuNziIIQEKQVNVU19aMDFRRIgfAQRBc3VzEBVIsQLeBynMASIKMgYDMgEFUgOsmAEAIgQSCBICNgcFUC+6Y1FiKcwBIgoyBgRSBXS0AQAyAQVSA8nPAQAiBBIIIgI2BwVQL6pkUWIpzAEiCjIGAzIBBVID9ScCACIEEggSAjYHBVAvI2VRYinMASIKMgYEUgWkqwEAMgEFUgOF2AEAIgQSCCICNgcFUC+cZVFiKcwBIgoyBgMyAQVSA5bZAQAiBBIIMgICNgcFUC8bZlFiKcwBIgoyBgMyAQVSA1fxAQAiBBIIMgIDNgcFUC+GZlFiKcwBIgoyBgMyAQVSA1+fAQAiBBIIMgIENgcFUC8JZ1FiKcwBIgoyBgMyAQVSA3cAAgAiBBIIMgIFNgcFUC+NZ1FiKcwBIgoyBgMyAQVSAzwDAgAiBBIIMgIGNgcFUC/7Z1FiKcwBIgoyBgMyAQVSA6euAQAiBBIIMgIHNgcFUC/IaFFiKcwBIgoyBgMyAQVSAwwaAgAiBBIIMgIINgcF</add></iq>
    void SendStatus() {
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("id", GenerateIqId()));
        iq.AddAttribute(new StanzaAttribute("xmlns", "w:stats"));
        iq.AddAttribute(new StanzaAttribute("type", "set"));
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));

        ProtocolTreeNode add = new ProtocolTreeNode("add");
        add.AddAttribute(new StanzaAttribute("t", String.valueOf(System.currentTimeMillis() / 1000)));
//        add.SetData(IdentifiedTelemetry.GenerateDataToSend(envBuilder_.build()));
        if (iosLogin) {
            add.SetData(generateIOSBasicWamRecord());
        } else {
            add.SetData(generateAndroidBasicWamRecord());
        }
        iq.AddChild(add);
        AddTask(iq);
    }

    void SendAttestation() {
        ProtocolTreeNode ib = new ProtocolTreeNode("ib");
        ib.AddAttribute(new StanzaAttribute("id", GenerateIqId()));

        ProtocolTreeNode attestation = new ProtocolTreeNode("attestation");

        ProtocolTreeNode error = new ProtocolTreeNode("error");
        error.AddAttribute(new StanzaAttribute("code", "0"));

        attestation.AddChild(error);
        ib.AddChild(attestation);
        AddTask(ib);
    }


    boolean GetImageThumb(String path, JSONObject mediaInfo) {
        String thumbnailPath = WhatsAppUtils.imageCapture(path, path + ".jpg");
        String thumbnailEncode = cn.hutool.core.codec.Base64.encode(FileUtil.readBytes(thumbnailPath));
        mediaInfo.put("thumbnail_path", thumbnailEncode);
        return true;
    }

    String getOpusVoice(String sourceVoicePath, JSONObject mediaInfo) {
        int playSecond;
        String path = sourceVoicePath + ".opus";
        if (StartedUpRunner.installFfmpeg) {
            MultimediaInfo multimediaInfo = WhatsAppUtils.mp3ToOpus(sourceVoicePath, path);
            if (multimediaInfo != null) {
                try {
                    playSecond = (int) (multimediaInfo.getDuration() / 1000);
                } catch (Exception e) {
                    return "";
                }
            } else {
                return "";
            }
        } else {
            AudioConvert.convert(sourceVoicePath, path, avcodec.AV_CODEC_ID_OPUS, 48000, 64 * 1000, 1);
            byte[] bytes = FileUtil.readBytes(path);
            playSecond = WhatsAppUtils.getVoiceLength(bytes);
        }
        if (playSecond == 0) {
            playSecond = 6;
        }
        mediaInfo.put("seconds", playSecond);
        return path;
    }

    boolean GetVideoThumb(String path, JSONObject mediaInfo, boolean isGif) {
        String imagePath = path + ".jpg";
        if (StartedUpRunner.installFfmpeg) {
            MultimediaInfo multimediaInfo;
            if (isGif) {
                multimediaInfo = WhatsAppUtils.videoGifCapture(path, imagePath);
            } else {
                multimediaInfo = WhatsAppUtils.videoCapture(path, imagePath);
            }
            if (multimediaInfo != null) {
                try {
                    long duration = multimediaInfo.getDuration();
                    MultimediaInfo.VideoInfo video = multimediaInfo.getVideo();
                    int width = video.getWidth();
                    int height = video.getHeight();
                    if (width == 0 || height == 0) {
                        return false;
                    }
                    mediaInfo.put("play_time", duration / 1000 + 1);
                    mediaInfo.put("width", width);
                    mediaInfo.put("height", height);
                    String thumbnailEncode = cn.hutool.core.codec.Base64.encode(FileUtil.readBytes(imagePath));
                    mediaInfo.put("thumbnail_path", thumbnailEncode);
                } catch (Exception e) {
                    return false;
                }
                return true;
            } else {
                return false;
            }
        }
        FFmpegFrameGrabber ff = null;
        try {
            ff = new FFmpegFrameGrabber(path);
            ff.start();
            mediaInfo.put("play_time", ff.getLengthInTime() / (1000 * 1000) + 1);
            mediaInfo.put("width", ff.getImageWidth());
            mediaInfo.put("height", ff.getImageHeight());
            //这里取第一帧，有可能是黑的，可以自己调
            Frame frame = null;
            for (int i = 0; i < ff.getLengthInFrames(); i++) {
                frame = ff.grabFrame();
                if (frame.image != null) {
                    break;
                }
            }
            Java2DFrameConverter converter = new Java2DFrameConverter();
            BufferedImage bi = converter.getBufferedImage(frame);
            Image scaledImage = bi.getScaledInstance(224, 224, Image.SCALE_DEFAULT);
            BufferedImage scaledImageBuffer = new BufferedImage(224, 224, BufferedImage.TYPE_3BYTE_BGR);
            scaledImageBuffer.getGraphics().drawImage(scaledImage, 0, 0, null);
            File thumbnailFile = new File(imagePath);
            try {
                ImageIO.write(scaledImageBuffer, "jpg", thumbnailFile);
            } catch (IOException e) {
                log.error("获取视频截图异常", e);
            }
            String thumbnailEncode = cn.hutool.core.codec.Base64.encode(thumbnailFile);
            mediaInfo.put("thumbnail_path", thumbnailEncode);
            return true;
        } catch (FrameGrabber.Exception e) {
            log.error("获取视频截图异常", e);
        } finally {
            if (ff != null) {
                try {
                    ff.close();
                } catch (Exception e) {
                    log.error("关闭ffmpeg异常", e);
                }
            }
            System.gc();
        }
        return false;
    }

    String GetUserAgent() {
        if (iosLogin) {
            return String.format("WhatsApp/%d.%d.%d.%d Ios/%s Device/%s-%s",
                    envBuilder_.getUserAgent().getAppVersion().getPrimary(),
                    envBuilder_.getUserAgent().getAppVersion().getSecondary(),
                    envBuilder_.getUserAgent().getAppVersion().getTertiary(),
                    envBuilder_.getUserAgent().getAppVersion().getQuaternary(),
                    envBuilder_.getUserAgent().getOsVersion(),
                    envBuilder_.getUserAgent().getManufacturer().replace("-", ""),
                    envBuilder_.getUserAgent().getDevice().replace("-", ""));
        }
        return String.format("WhatsApp/%d.%d.%d.%d Android/%s Device/%s-%s",
                envBuilder_.getUserAgent().getAppVersion().getPrimary(),
                envBuilder_.getUserAgent().getAppVersion().getSecondary(),
                envBuilder_.getUserAgent().getAppVersion().getTertiary(),
                envBuilder_.getUserAgent().getAppVersion().getQuaternary(),
                envBuilder_.getUserAgent().getOsVersion(),
                ReUtil.replaceAll(envBuilder_.getUserAgent().getManufacturer().replace("-", ""), "[^0-9a-zA-Z ]+", ""),
                ReUtil.replaceAll(envBuilder_.getUserAgent().getDevice().replace("-", ""), "[^0-9a-zA-Z ]+", ""));
    }


    void GenerateImageMessage(WhatsMessage.WhatsAppImageMessage.Builder builder, JSONObject mediaInfo) {
        try {
            String caption = mediaInfo.getString("caption");
            if (StringUtils.hasLength(caption)) {
                builder.setCaption(caption);
            }
            builder.setUrl(mediaInfo.getString("url"));
            builder.setMimetype(mediaInfo.getString("mime"));
            builder.setFileEncSha256(ByteString.copyFrom(Base64.getDecoder().decode(mediaInfo.getString("encrypt_hash"))));
            builder.setFileSha256(ByteString.copyFrom(Base64.getDecoder().decode(mediaInfo.getString("orign_hash"))));
            builder.setFileLength(mediaInfo.getIntValue("file_len"));
            builder.setWidth(mediaInfo.getIntValue("width"));
            builder.setHeight(mediaInfo.getIntValue("height"));
            String thumbnailPath = mediaInfo.getString("thumbnail_path");
            byte[] decode = cn.hutool.core.codec.Base64.decode(thumbnailPath);
            builder.setJpegThumbnail(ByteString.copyFrom(decode));
            //builder.setJpegThumbnail(ByteString.readFrom(new FileInputStream(mediaInfo.getString("thumbnail_path"))));
            builder.setMediaKey(ByteString.copyFrom(Base64.getDecoder().decode(mediaInfo.getString("media_key"))));
            builder.setDirectPath(mediaInfo.getString("direct_path"));
            builder.setFirstScanLength(0);
            // 适配图片新的proto参数
            WhatsMessage.WhatsAppContextInfo.Builder imageContextBuilder = WhatsMessage.WhatsAppContextInfo.newBuilder();
            if (isIosLogin()) {
                imageContextBuilder.setStatusSourceType(0);
                builder.setImageSourceType(0);
            } else {
                imageContextBuilder.setPairedMediaType(0);
            }
        } catch (Exception e) {
            log.error("image:" + e.getLocalizedMessage());
        }
    }

    void GenerateVideoMessage(WhatsMessage.WhatsAppVideoMessage.Builder builder, JSONObject mediaInfo) {
        try {
            String caption = mediaInfo.getString("caption");
            if (StringUtils.hasLength(caption)) {
                builder.setCaption(caption);
            }
            builder.setUrl(mediaInfo.getString("url"));
            builder.setMimetype(mediaInfo.getString("mime"));
            builder.setFileEncSha256(ByteString.copyFrom(Base64.getDecoder().decode(mediaInfo.getString("encrypt_hash"))));
            builder.setFileSha256(ByteString.copyFrom(Base64.getDecoder().decode(mediaInfo.getString("orign_hash"))));
            builder.setFileLength(mediaInfo.getIntValue("file_len"));
            builder.setWidth(mediaInfo.getIntValue("width"));
            builder.setHeight(mediaInfo.getIntValue("height"));
            String thumbnailPath = mediaInfo.getString("thumbnail_path");
            byte[] decode = cn.hutool.core.codec.Base64.decode(thumbnailPath);
            builder.setJpegThumbnail(ByteString.copyFrom(decode));
            //builder.setJpegThumbnail(ByteString.readFrom(new FileInputStream(mediaInfo.getString("thumbnail_path"))));
            builder.setMediaKey(ByteString.copyFrom(Base64.getDecoder().decode(mediaInfo.getString("media_key"))));
            builder.setDirectPath(mediaInfo.getString("direct_path"));
            builder.setSeconds(mediaInfo.getIntValue("play_time"));
        } catch (Exception e) {
            log.error("video:" + e.getLocalizedMessage());
        }
    }

    void GeneratePPtMessage(WhatsMessage.WhatsAppAudioMessage.Builder builder, JSONObject mediaInfo) {
        builder.setUrl(mediaInfo.getString("url"));
        builder.setMimetype(mediaInfo.getString("mime"));
        builder.setFileEncSha256(ByteString.copyFrom(Base64.getDecoder().decode(mediaInfo.getString("encrypt_hash"))));
        builder.setFileSha256(ByteString.copyFrom(Base64.getDecoder().decode(mediaInfo.getString("orign_hash"))));
        builder.setFileLength(mediaInfo.getIntValue("file_len"));
        builder.setMediaKey(ByteString.copyFrom(Base64.getDecoder().decode(mediaInfo.getString("media_key"))));
        builder.setDirectPath(mediaInfo.getString("direct_path"));
        builder.setPtt(true);
        builder.setWaveform(ByteString.copyFrom(cn.hutool.core.codec.Base64.decode("TwAvFkFTTlNaN2EsU1xSCWNjYVJjX01jGWNjQhtfYzdTN2NUYzEtWBxjY2NjYw1jY1NjP1hjXk9jQ0UeYyZYUg==")));
        //硬编码
        builder.setSeconds(mediaInfo.getIntValue("seconds"));
    }


    void GenerateDocMessage(WhatsMessage.WhatsAppDocumentMessage.Builder builder, JSONObject mediaInfo) {
        builder.setUrl(mediaInfo.getString("url"));
        builder.setMimetype(mediaInfo.getString("mime"));
        builder.setFileEncSha256(ByteString.copyFrom(Base64.getDecoder().decode(mediaInfo.getString("encrypt_hash"))));
        builder.setFileSha256(ByteString.copyFrom(Base64.getDecoder().decode(mediaInfo.getString("orign_hash"))));
        builder.setFileLength(mediaInfo.getIntValue("file_len"));
        builder.setMediaKey(ByteString.copyFrom(Base64.getDecoder().decode(mediaInfo.getString("media_key"))));
        builder.setDirectPath(mediaInfo.getString("direct_path"));
        builder.setFileName(mediaInfo.getString("file_name"));
        builder.setTitle(mediaInfo.getString("title"));
        builder.setPageCount(0);
    }

    void GenerateGifMessage(WhatsMessage.WhatsAppVideoMessage.Builder builder, JSONObject mediaInfo) {
        try {
            builder.setUrl(mediaInfo.getString("url"));
            builder.setMimetype(mediaInfo.getString("mime"));
            builder.setFileEncSha256(ByteString.copyFrom(Base64.getDecoder().decode(mediaInfo.getString("encrypt_hash"))));
            builder.setFileSha256(ByteString.copyFrom(Base64.getDecoder().decode(mediaInfo.getString("orign_hash"))));
            builder.setFileLength(mediaInfo.getIntValue("file_len"));
            builder.setWidth(mediaInfo.getIntValue("width"));
            builder.setHeight(mediaInfo.getIntValue("height"));
            String thumbnailPath = mediaInfo.getString("thumbnail_path");
            byte[] decode = cn.hutool.core.codec.Base64.decode(thumbnailPath);
            builder.setJpegThumbnail(ByteString.copyFrom(decode));
            //builder.setJpegThumbnail(ByteString.readFrom(new FileInputStream(mediaInfo.getString("thumbnail_path"))));
            builder.setMediaKey(ByteString.copyFrom(Base64.getDecoder().decode(mediaInfo.getString("media_key"))));
            builder.setDirectPath(mediaInfo.getString("direct_path"));
            builder.setSeconds(mediaInfo.getIntValue("play_time"));
            builder.setGifPlayback(true);
            builder.setGifAttribution(WhatsMessage.WhatsAppGifAttr.GifAttr_TENOR);
        } catch (Exception e) {
            log.error("gif:" + e.getLocalizedMessage());
        }
    }

    public void submitMediaRequest(String jid, List<String> userIds, JSONObject mediaInfo, String id, WhatsMessage.WhatsAppContextInfo.Builder contextInfoBuilder) {
        submitMediaRequest(jid, userIds, mediaInfo, id, contextInfoBuilder, null);
    }

    public void submitMediaRequest(String jid, List<String> userIds, JSONObject mediaInfo, String id, WhatsMessage.WhatsAppContextInfo.Builder contextInfoBuilder, String tcToken) {
        addTaskToQueue(() -> {
            String mediaType = mediaInfo.getString("media_type");
            WhatsMessage.WhatsAppMessage.Builder builder = WhatsMessage.WhatsAppMessage.newBuilder();
            switch (mediaType) {
                case "image": {
                    WhatsMessage.WhatsAppImageMessage.Builder messageBuilder = builder.getImageMessageBuilder();
                    GenerateImageMessage(messageBuilder, mediaInfo);
                    if (contextInfoBuilder != null) {
                        messageBuilder.setContextInfo(contextInfoBuilder);
                    }
                    break;
                }
                case "video": {
                    WhatsMessage.WhatsAppVideoMessage.Builder messageBuilder = builder.getVideoMessageBuilder();
                    GenerateVideoMessage(messageBuilder, mediaInfo);
                    if (contextInfoBuilder != null) {
                        messageBuilder.setContextInfo(contextInfoBuilder);
                    }
                    break;
                }
                case "ptt": {
                    WhatsMessage.WhatsAppAudioMessage.Builder messageBuilder = builder.getAudioMessageBuilder();
                    GeneratePPtMessage(messageBuilder, mediaInfo);
                    if (contextInfoBuilder != null) {
                        messageBuilder.setContextInfo(contextInfoBuilder);
                    }
                    break;
                }
                case "document": {
                    WhatsMessage.WhatsAppDocumentMessage.Builder messageBuilder = builder.getDocumentMessageBuilder();
                    GenerateDocMessage(messageBuilder, mediaInfo);
                    if (contextInfoBuilder != null) {
                        messageBuilder.setContextInfo(contextInfoBuilder);
                    }
                    break;
                }
                case "gif": {
                    WhatsMessage.WhatsAppVideoMessage.Builder messageBuilder = builder.getVideoMessageBuilder();
                    GenerateGifMessage(messageBuilder, mediaInfo);
                    if (contextInfoBuilder != null) {
                        messageBuilder.setContextInfo(contextInfoBuilder);
                    }
                    break;
                }
            }
            if (StringUtils.hasLength(jid)) {
                firstSendMsgToFans(jid, builder.build().toByteArray(), "media", mediaType, id, tcToken);
            } else {
                firstSendMsgToFans(null, userIds, builder.build().toByteArray(), "media", mediaType, id);
            }
        });
    }

    /**
     * 设置二次验证码，为空空
     *
     * @param taskId 任务id
     * @param code   为空则清除二次验证
     * @param email  忘记验证码备用邮箱
     * @return taskId
     */
    public String open2FAuth(String taskId, String code, String email) {
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("id", taskId));
        iq.AddAttribute(new StanzaAttribute("xmlns", "urn:xmpp:whatsapp:account"));
        iq.AddAttribute(new StanzaAttribute("type", "set"));
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        ProtocolTreeNode auth = new ProtocolTreeNode("2fa");
        ProtocolTreeNode codeNode = new ProtocolTreeNode("code");
        if (StringUtils.hasLength(code)) {
            codeNode.SetData(code.getBytes(StandardCharsets.UTF_8));
        } else {
            codeNode.SetData(null);
        }
        ProtocolTreeNode emailNode = new ProtocolTreeNode("email");
        if (StringUtils.hasLength(email)) {
            emailNode.SetData(email.getBytes(StandardCharsets.UTF_8));
            auth.AddChild(emailNode);
        } else {
            emailNode.SetData(null);
        }
        auth.AddChild(codeNode);
        iq.AddChild(auth);
        return AddTask(iq, new HandleResult("Open2FAuth"));
    }

    /**
     * 获取粉丝昵称
     */
    public void getFansNickName(String taskId, String userId) {
        ProtocolTreeNode getVerifiedIq = new ProtocolTreeNode("iq");
        getVerifiedIq.AddAttribute(new StanzaAttribute("id", taskId));
        getVerifiedIq.AddAttribute(new StanzaAttribute("xmlns", "w:biz"));
        getVerifiedIq.AddAttribute(new StanzaAttribute("type", "get"));

        ProtocolTreeNode v2 = new ProtocolTreeNode("verified_name");
        v2.AddAttribute(new StanzaAttribute("jid", JidNormalize(userId)));
        getVerifiedIq.AddChild(v2);
        AddTask(getVerifiedIq);
    }

    public void getVerifiedName() {
        ProtocolTreeNode getVerifiedIq = new ProtocolTreeNode("iq");
        getVerifiedIq.AddAttribute(new StanzaAttribute("id", GenerateIqId()));
        getVerifiedIq.AddAttribute(new StanzaAttribute("xmlns", "w:biz"));
        getVerifiedIq.AddAttribute(new StanzaAttribute("type", "get"));

        ProtocolTreeNode v2 = new ProtocolTreeNode("verified_name");
        v2.AddAttribute(new StanzaAttribute("jid", JidNormalize(envBuilder_.getFullphone())));
        getVerifiedIq.AddChild(v2);
        AddTask(getVerifiedIq, (srcNode1, result1) -> {
            ProtocolTreeNode verified_name = result1.GetChild("verified_name");
            if (!Objects.isNull(verified_name)) {
                byte[] data = verified_name.GetData();
                if (!Objects.isNull(data)) {
                    try {
                        WhatsMessage.Verified vip = WhatsMessage.Verified.parseFrom(data);
                        long number = vip.getVerifiedOne().getVerifiedOne1();
                        String newVerifiedName = Long.toString(number);
                        String oldVerifiedName = envBuilder_.getVerifyedName();
                        boolean update = false;
                        if (!newVerifiedName.equals(oldVerifiedName)) {
                            envBuilder_.setVerifyedName(newVerifiedName);
                            update = true;
                        }
                        String pushName = envBuilder_.getPushname();
                        String verifiedOne4 = vip.getVerifiedOne().getVerifiedOne4();
                        if (!pushName.equals(verifiedOne4)) {
                            envBuilder_.setPushname(verifiedOne4);
                            update = true;
                        }
                        //保存到数据库
                        if (update) {
                            axolotlManager_.SetBytesSetting("env", envBuilder_.build().toByteArray());
                        }
                    } catch (Exception e) {

                    }
                }
            }

            if (!StringUtils.hasLength(envBuilder_.getVerifyedName()) || !StringUtils.hasLength(envBuilder_.getPushname())) {
                modifyBusinessVersionNickname(null, GenerateIqId(), RandomNameUtil.getRandomEnglishName());
                setCategory();
            }
        });
    }

    public void modifyBusinessVersionNickname(String taskId, String availableId, String nickname) {
        /**
         * <iq id='08' xmlns='w:biz' type='set' to='s.whatsapp.net'>
         *     <verified_name v='2'>ChkIvImZoZK0opQGEgZzbWI6d2EiBUZ5aW5nEkBLEv1SLRTJfwG6yZVgXwALXT4YsWhhYjLfGK+iJWGIggbiCSDLzR+nYmSUMLgOZPo+7GtwqZKooMGzyaMJ3CYN</verified_name>
         * </iq>
         */
        if (StringUtils.isEmpty(nickname)) {
            //随机nickname
            nickname = RandomNameUtil.getRandomEnglishName();
        }
        Random random = new SecureRandom();
        long abs = Math.abs(random.nextLong());
        WhatsMessage.Verified.VerifiedOne v1 = WhatsMessage.Verified.VerifiedOne.newBuilder()
                .setVerifiedOne1(abs)
                .setVerifiedOne2("smb:wa")
                .setVerifiedOne4(nickname).build();  //最后一个是昵称参数
        //初始化数据表, 加密需要获取 privatekey
        byte[] byteSign = Curve25519.getInstance(BEST)
                .calculateSignature(axolotlManager_.GetIdentityKeyPair().getPrivateKey().serialize(), v1.toByteArray());
        WhatsMessage.Verified verified = WhatsMessage.Verified.newBuilder().setVerifiedOne(v1).setVerifiedTow(ByteString.copyFrom(byteSign)).build();
        byte[] verifyName = verified.toByteArray();
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("id", GenerateIqId()));
        iq.AddAttribute(new StanzaAttribute("xmlns", "w:biz"));
        iq.AddAttribute(new StanzaAttribute("type", "set"));
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        ProtocolTreeNode verifiedNameNode = new ProtocolTreeNode("verified_name");
        verifiedNameNode.AddAttribute(new StanzaAttribute("v", "2"));
        verifiedNameNode.SetData(verifyName);
        iq.AddChild(verifiedNameNode);
        String finalNickname = nickname;
        AddTask(iq, (srcNode, result) -> {
            {
                ProtocolTreeNode presence = new ProtocolTreeNode("presence");
                presence.AddAttribute(new StanzaAttribute("type", "available"));
                presence.AddAttribute(new StanzaAttribute("id", availableId));
                presence.AddAttribute(new StanzaAttribute(Constant.TASK_TAG, TypeConstant.TaskType.SET_NAME));
                presence.AddAttribute(new StanzaAttribute("name", finalNickname));
                AddTask(presence, NodeTaskType.MODIFY_NICKNAME);
                if (StringUtils.hasLength(taskId)) {
                    taskNotify.setEventContent(taskId, result);
                }
            }
            //获取 verified nam
            {
                //从服务器获取 verify name
                //<iq xmlns="w:biz" id="0" type="get"><verified_name jid="6283104453922@s.whatsapp.net"/></iq>
                ProtocolTreeNode getVerifiedIq = new ProtocolTreeNode("iq");
                getVerifiedIq.AddAttribute(new StanzaAttribute("id", GenerateIqId()));
                getVerifiedIq.AddAttribute(new StanzaAttribute("xmlns", "w:biz"));
                getVerifiedIq.AddAttribute(new StanzaAttribute("type", "get"));

                ProtocolTreeNode v2 = new ProtocolTreeNode("verified_name");
                v2.AddAttribute(new StanzaAttribute("jid", JidNormalize(envBuilder_.getFullphone())));
                getVerifiedIq.AddChild(v2);
                AddTask(getVerifiedIq, (srcNode1, result1) -> {
                    ProtocolTreeNode verified_name = result1.GetChild("verified_name");
                    if (!Objects.isNull(verified_name)) {
                        byte[] data = verified_name.GetData();
                        if (!Objects.isNull(data)) {
                            try {
                                WhatsMessage.Verified vip = WhatsMessage.Verified.parseFrom(data);
                                long number = vip.getVerifiedOne().getVerifiedOne1();
                                envBuilder_.setVerifyedName(Long.toString(number));
                                envBuilder_.setPushname(finalNickname);
                                //保存到数据库
                                axolotlManager_.SetBytesSetting("env", envBuilder_.build().toByteArray());
                            } catch (Exception e) {

                            }
                        }
                    }
                });

            }
        });
    }

    public String SyncContact(List<String> phones, String taskId) {
        ProtocolTreeNode iq = generateContactNode(phones, taskId);
        return AddTask(iq, new HandleResult("SyncContact"));
    }

    /**
     * 查询是否开通whatsapp
     */
    public ProtocolTreeNode generateContactNode(List<String> phones, String taskId) {
        return generateContactNode(phones, taskId, "interactive");
    }

    public ProtocolTreeNode queryContactNodeAllInfo(String taskId, List<String> phones) {
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("id", taskId));
        iq.AddAttribute(new StanzaAttribute("xmlns", "usync"));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        ProtocolTreeNode usync = new ProtocolTreeNode("usync");
        usync.AddAttribute(new StanzaAttribute("mode", "query"));
        usync.AddAttribute(new StanzaAttribute("last", "true"));
        usync.AddAttribute(new StanzaAttribute("index", "0"));
        ProtocolTreeNode profile = new ProtocolTreeNode("profile");
        if (iosLogin) {
            usync.AddAttribute(new StanzaAttribute("sid", System.currentTimeMillis() / 1000 + "-" + new Random().nextInt(1000000000) + "-" + this.getSyncId().incrementAndGet()));
            usync.AddAttribute(new StanzaAttribute("context", "add"));
            profile.AddAttribute(new StanzaAttribute("v", "1396"));
            iq.AddAttribute(new StanzaAttribute("to", JidNormalize(username)));
        } else {
            usync.AddAttribute(new StanzaAttribute("sid", "sync_sid_query_" + UUID.randomUUID()));
            usync.AddAttribute(new StanzaAttribute("context", "interactive"));
            profile.AddAttribute(new StanzaAttribute("v", "1908"));
        }
        // query 子节点
        ProtocolTreeNode query = new ProtocolTreeNode("query");
        query.AddChild(new ProtocolTreeNode("contact"));
        query.AddChild(new ProtocolTreeNode("status"));

        ProtocolTreeNode picture = new ProtocolTreeNode("picture");
        picture.AddAttribute(new StanzaAttribute("type", "image"));
        query.AddChild(picture);

        ProtocolTreeNode business = new ProtocolTreeNode("business");
        business.AddChild(new ProtocolTreeNode("verified_name"));
        business.AddChild(profile);
        query.AddChild(business);

        ProtocolTreeNode devicesNode = new ProtocolTreeNode("devices");
        devicesNode.AddAttribute(new StanzaAttribute("version", "2"));


        query.AddChild(devicesNode);
        query.AddChild(new ProtocolTreeNode("disappearing_mode"));
        query.AddChild(new ProtocolTreeNode("lid"));
        usync.AddChild(query);
        //列表
        ProtocolTreeNode list = new ProtocolTreeNode("list");
        for (String phone : phones) {
            ProtocolTreeNode user = new ProtocolTreeNode("user");
            ProtocolTreeNode contact = new ProtocolTreeNode("contact");
            char firstChar = phone.charAt(0);
            if (firstChar != '+') {
                // 如果不是加号，则在前面添加一个加号
                phone = "+" + phone;
            }
            contact.SetData(phone.getBytes(StandardCharsets.UTF_8));
            user.AddChild(contact);
            list.AddChild(user);
        }

        usync.AddChild(list);
        iq.AddChild(usync);
        return iq;
    }

    public ProtocolTreeNode generateContactNodeAllInfo(List<String> phones, String taskId, String context) {
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("id", taskId));
        iq.AddAttribute(new StanzaAttribute("xmlns", "usync"));
        iq.AddAttribute(new StanzaAttribute("type", "get"));

        ProtocolTreeNode usync = new ProtocolTreeNode("usync");
        usync.AddAttribute(new StanzaAttribute("index", "0"));
        usync.AddAttribute(new StanzaAttribute("last", "true"));
        usync.AddAttribute(new StanzaAttribute("mode", "delta"));
        usync.AddAttribute(new StanzaAttribute("context", context));
        ProtocolTreeNode profile = new ProtocolTreeNode("profile");
        if (iosLogin) {
            usync.AddAttribute(new StanzaAttribute("sid", System.currentTimeMillis() / 1000 + "-" + new Random().nextInt(1000000000) + "-" + this.getSyncId().incrementAndGet()));
            profile.AddAttribute(new StanzaAttribute("v", "1396"));

        } else {
            if (WhatsAppUtils.isGreaterThan(waVersion, "2.24.23.78")) {
                usync.AddAttribute(new StanzaAttribute("sid", "ContactSyncHelper/sync_sid_delta_" + UUID.randomUUID()));
            } else {
                usync.AddAttribute(new StanzaAttribute("sid", "sync_sid_delta_" + UUID.randomUUID()));
            }
            profile.AddAttribute(new StanzaAttribute("v", "1876"));
        }
        // query 子节点
        ProtocolTreeNode query = new ProtocolTreeNode("query");
        query.AddChild(new ProtocolTreeNode("contact"));
        query.AddChild(new ProtocolTreeNode("status"));

        ProtocolTreeNode picture = new ProtocolTreeNode("picture");
        picture.AddAttribute(new StanzaAttribute("type", "image"));
        query.AddChild(picture);

        ProtocolTreeNode business = new ProtocolTreeNode("business");
        business.AddChild(new ProtocolTreeNode("verified_name"));
        business.AddChild(profile);
        query.AddChild(business);

        ProtocolTreeNode devicesNode = new ProtocolTreeNode("devices");
        devicesNode.AddAttribute(new StanzaAttribute("version", "2"));


        query.AddChild(devicesNode);
        query.AddChild(new ProtocolTreeNode("disappearing_mode"));
        query.AddChild(new ProtocolTreeNode("lid"));
        usync.AddChild(query);
        //列表
        ProtocolTreeNode list = new ProtocolTreeNode("list");
        for (String phone : phones) {
            ProtocolTreeNode user = new ProtocolTreeNode("user");
            ProtocolTreeNode contact = new ProtocolTreeNode("contact");
            char firstChar = phone.charAt(0);
            if (firstChar != '+') {
                // 如果不是加号，则在前面添加一个加号
                phone = "+" + phone;
            }
            contact.SetData(phone.getBytes(StandardCharsets.UTF_8));
            user.AddChild(contact);
            list.AddChild(user);
        }

        usync.AddChild(list);
        iq.AddChild(usync);
        return iq;
    }

    public ProtocolTreeNode generateContactNode(List<String> phones, String taskId, String context) {
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("id", taskId));
        iq.AddAttribute(new StanzaAttribute("xmlns", "usync"));
        iq.AddAttribute(new StanzaAttribute("type", "get"));

        ProtocolTreeNode usync = new ProtocolTreeNode("usync");
        usync.AddAttribute(new StanzaAttribute("index", "0"));
        usync.AddAttribute(new StanzaAttribute("last", "true"));
        usync.AddAttribute(new StanzaAttribute("mode", "delta"));
        usync.AddAttribute(new StanzaAttribute("context", context));
        ProtocolTreeNode profile = new ProtocolTreeNode("profile");
        if (iosLogin) {
            usync.AddAttribute(new StanzaAttribute("sid", System.currentTimeMillis() / 1000 + "-" + new Random().nextInt(1000000000) + "-" + this.getSyncId().incrementAndGet()));
            profile.AddAttribute(new StanzaAttribute("v", "1396"));

        } else {
            if (WhatsAppUtils.isGreaterThan(waVersion, "2.24.23.78")) {
                usync.AddAttribute(new StanzaAttribute("sid", "ContactSyncHelper/sync_sid_delta_" + UUID.randomUUID()));
            } else {
                usync.AddAttribute(new StanzaAttribute("sid", "sync_sid_delta_" + UUID.randomUUID()));
            }
            profile.AddAttribute(new StanzaAttribute("v", "1908"));
        }

        // query 子节点
        ProtocolTreeNode query = new ProtocolTreeNode("query");
        query.AddChild(new ProtocolTreeNode("contact"));
        query.AddChild(new ProtocolTreeNode("status"));

        ProtocolTreeNode business = new ProtocolTreeNode("business");
        business.AddChild(new ProtocolTreeNode("verified_name"));
        business.AddChild(profile);
        query.AddChild(business);

        ProtocolTreeNode devicesNode = new ProtocolTreeNode("devices");
        devicesNode.AddAttribute(new StanzaAttribute("version", "2"));
        query.AddChild(devicesNode);
        query.AddChild(new ProtocolTreeNode("disappearing_mode"));
        query.AddChild(new ProtocolTreeNode("lid"));
        usync.AddChild(query);
        //列表
        ProtocolTreeNode list = new ProtocolTreeNode("list");
        for (String phone : phones) {
            ProtocolTreeNode user = new ProtocolTreeNode("user");
            ProtocolTreeNode contact = new ProtocolTreeNode("contact");
            char firstChar = phone.charAt(0);
            if (firstChar != '+') {
                // 如果不是加号，则在前面添加一个加号
                phone = "+" + phone;
            }
            contact.SetData(phone.getBytes(StandardCharsets.UTF_8));
            user.AddChild(contact);
            list.AddChild(user);
        }

        usync.AddChild(list);
        iq.AddChild(usync);
        return iq;
    }

    public String UrlSearchContact(String phone, String taskId) {
        return AddTask(urlSearchContactNode(phone, taskId), new HandleResult("UrlSearchContact"));
    }

    public void sendIk(String userId, NodeCallback callback) {
        /**
         * <iq id='02a' xmlns='encrypt' type='get' to='s.whatsapp.net'>
         *     <identity>
         *         <user jid='254100274658.0:0@s.whatsapp.net' />
         *     </identity>
         * </iq>
         */
        ProtocolTreeNode iqNode = new ProtocolTreeNode("iq");
        iqNode.AddAttribute(new StanzaAttribute("id", GenerateIqId()));
        iqNode.AddAttribute(new StanzaAttribute("xmlns", "encrypt"));
        iqNode.AddAttribute(new StanzaAttribute("type", "get"));
        iqNode.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        ProtocolTreeNode identityNode = new ProtocolTreeNode("identity");
        ProtocolTreeNode userNode = new ProtocolTreeNode("user");
        userNode.AddAttribute(new StanzaAttribute("jid", userId));
        identityNode.AddChild(userNode);
        iqNode.AddChild(identityNode);
        AddTask(iqNode, callback);
    }

    public void sendGetPicture(String userId) {
        /**
         * <iq id='02b' xmlns='w:profile:picture' to='s.whatsapp.net' target='254100274658@s.whatsapp.net'
         *     type='get'>
         *     <picture type='preview' />
         * </iq>
         */
        StringUtil.JidInfo jidInfo = StringUtil.ParseJid(userId);
        byte[] token = null;
        try {
            KeyLockUtil.lock(username);
            token = axolotlManager_.trustedContactStore.getToken(jidInfo.recipientId);
        } catch (Exception ignored) {
        } finally {
            KeyLockUtil.unlock(username);
        }
        sendGetPicture(userId, token);
    }

    public void sendGetPicture(String userId, byte[] token) {
        ProtocolTreeNode iqNode = new ProtocolTreeNode("iq");
        iqNode.AddAttribute(new StanzaAttribute("id", GenerateIqId()));
        iqNode.AddAttribute(new StanzaAttribute("xmlns", "w:profile:picture"));
        iqNode.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        iqNode.AddAttribute(new StanzaAttribute("target", userId));
        iqNode.AddAttribute(new StanzaAttribute("type", "get"));
        ProtocolTreeNode pictureNode = new ProtocolTreeNode("picture");
        pictureNode.AddAttribute(new StanzaAttribute("type", "preview"));
        if (token != null) {
            ProtocolTreeNode tcToken = new ProtocolTreeNode("tctoken");
            tcToken.SetData(token);
            pictureNode.AddChild(tcToken);
        }
        iqNode.AddChild(pictureNode);
        AddTask(iqNode);
    }

    private ProtocolTreeNode urlSearchContactNode(String phone, String taskId) {
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("xmlns", "usync"));
        iq.AddAttribute(new StanzaAttribute("id", taskId));
        iq.AddAttribute(new StanzaAttribute("type", "get"));

        ProtocolTreeNode usync = new ProtocolTreeNode("usync");
        ProtocolTreeNode profile = new ProtocolTreeNode("profile");
        if (iosLogin) {
            usync.AddAttribute(new StanzaAttribute("sid", System.currentTimeMillis() / 1000 + "-" + new Random().nextInt(1000000000) + "-" + this.getSyncId().incrementAndGet()));
            profile.AddAttribute(new StanzaAttribute("v", "1396"));

        } else {
            usync.AddAttribute(new StanzaAttribute("sid", "sync_sid_query_" + UUID.randomUUID()));
            profile.AddAttribute(new StanzaAttribute("v", "1876"));
        }
        usync.AddAttribute(new StanzaAttribute("index", "0"));
        usync.AddAttribute(new StanzaAttribute("last", "true"));
        usync.AddAttribute(new StanzaAttribute("mode", "query"));
        usync.AddAttribute(new StanzaAttribute("context", "interactive"));

        // query 子节点
        ProtocolTreeNode query = new ProtocolTreeNode("query");
        query.AddChild(new ProtocolTreeNode("contact"));
        query.AddChild(new ProtocolTreeNode("status"));

        ProtocolTreeNode business = new ProtocolTreeNode("business");
        business.AddChild(new ProtocolTreeNode("verified_name"));
        business.AddChild(profile);
        query.AddChild(business);
        ProtocolTreeNode pictureNode = new ProtocolTreeNode("picture");
        pictureNode.AddAttribute(new StanzaAttribute("type", "preview"));
        query.AddChild(pictureNode);
        query.AddChild(new ProtocolTreeNode("disappearing_mode"));
        usync.AddChild(query);
        //列表
        ProtocolTreeNode list = new ProtocolTreeNode("list");
        ProtocolTreeNode user = new ProtocolTreeNode("user");
        ProtocolTreeNode contact = new ProtocolTreeNode("contact");
        contact.SetData(phone.getBytes(StandardCharsets.UTF_8));
        user.AddChild(contact);
        list.AddChild(user);
        usync.AddChild(list);
        iq.AddChild(usync);
        return iq;
    }

    public String UrlAddContact(String taskId, String phone) {
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("xmlns", "usync"));
        iq.AddAttribute(new StanzaAttribute("id", taskId));
        iq.AddAttribute(new StanzaAttribute("type", "get"));

        ProtocolTreeNode usync = new ProtocolTreeNode("usync");
        ProtocolTreeNode profile = new ProtocolTreeNode("profile");
        if (iosLogin) {
            usync.AddAttribute(new StanzaAttribute("sid", System.currentTimeMillis() / 1000 + "-" + new Random().nextInt(1000000000) + "-" + this.getSyncId().incrementAndGet()));
            profile.AddAttribute(new StanzaAttribute("v", "1396"));
        } else {
            usync.AddAttribute(new StanzaAttribute("sid", "sync_sid_sidelist_" + UUID.randomUUID()));
            profile.AddAttribute(new StanzaAttribute("v", "1876"));
        }
        usync.AddAttribute(new StanzaAttribute("index", "0"));
        usync.AddAttribute(new StanzaAttribute("last", "true"));
        usync.AddAttribute(new StanzaAttribute("mode", "delta")); //full delta  query
        usync.AddAttribute(new StanzaAttribute("context", "interactive")); //registration interactive  background notification

        // query 子节点
        ProtocolTreeNode query = new ProtocolTreeNode("query");
        query.AddChild(new ProtocolTreeNode("status"));
        ProtocolTreeNode business = new ProtocolTreeNode("business");
        business.AddChild(new ProtocolTreeNode("verified_name"));
        business.AddChild(profile);

        query.AddChild(business);
        query.AddChild(new ProtocolTreeNode("sidelist"));
        ProtocolTreeNode queryDevicesNode = new ProtocolTreeNode("devices");
        queryDevicesNode.AddAttribute(new StanzaAttribute("version", "2"));
        query.AddChild(queryDevicesNode);
        query.AddChild(new ProtocolTreeNode("disappearing_mode"));
        usync.AddChild(query);
        //列表
        ProtocolTreeNode list = new ProtocolTreeNode("side_list");
        ProtocolTreeNode user = new ProtocolTreeNode("user");
        String jid = JidNormalize(phone);
        user.AddAttribute(new StanzaAttribute("jid", jid));
        ProtocolTreeNode devicesNode = new ProtocolTreeNode("devices");
        HashSet<String> hashSet = new HashSet<>();
        StringUtil.JidInfo jidInfo = StringUtil.ParseJid(jid);
        String s = jidInfo.toString();
        hashSet.add(s);
        String pHash = CryptUtil.PHash(hashSet);
        devicesNode.AddAttribute(new StanzaAttribute("device_hash", pHash));
        user.AddChild(devicesNode);
        list.AddChild(user);
        usync.AddChild(list);
        iq.AddChild(usync);
        return AddTask(iq, new HandleResult("AddContact"));
    }

    public String AddContact(String taskId, List<String> jids) {
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("id", taskId));
        iq.AddAttribute(new StanzaAttribute("xmlns", "usync"));
        iq.AddAttribute(new StanzaAttribute("type", "get"));

        ProtocolTreeNode usync = new ProtocolTreeNode("usync");
        usync.AddAttribute(new StanzaAttribute("index", "0"));
        usync.AddAttribute(new StanzaAttribute("last", "true"));
        usync.AddAttribute(new StanzaAttribute("mode", "query")); //full delta  query
        usync.AddAttribute(new StanzaAttribute("context", "add")); //registration interactive  background notification
        ProtocolTreeNode profile = new ProtocolTreeNode("profile");
        if (iosLogin) {
            usync.AddAttribute(new StanzaAttribute("sid", System.currentTimeMillis() / 1000 + "-" + new Random().nextInt(1000000000) + "-" + this.getSyncId().incrementAndGet()));
            profile.AddAttribute(new StanzaAttribute("v", "1396"));
        } else {
            usync.AddAttribute(new StanzaAttribute("sid", "sync_sid_query_" + UUID.randomUUID()));
            profile.AddAttribute(new StanzaAttribute("v", "1876"));
        }

        // query 子节点
        ProtocolTreeNode query = new ProtocolTreeNode("query");
        query.AddChild(new ProtocolTreeNode("contact"));
        query.AddChild(new ProtocolTreeNode("status"));

        ProtocolTreeNode business = new ProtocolTreeNode("business");
        business.AddChild(new ProtocolTreeNode("verified_name"));
        business.AddChild(profile);

        query.AddChild(business);
        query.AddChild(new ProtocolTreeNode("picture"));
        usync.AddChild(query);
        //列表
        ProtocolTreeNode list = new ProtocolTreeNode("list");
        for (String jid : jids) {
            ProtocolTreeNode user = new ProtocolTreeNode("user");
            user.AddAttribute(new StanzaAttribute("jid", JidNormalize(jid)));
            list.AddChild(user);
        }

        usync.AddChild(list);
        iq.AddChild(usync);
        return AddTask(iq, new HandleResult("AddContact"));
    }

    /**
     * 信任联系人
     */
    public String trustedContact(String userId, String taskId) {
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("id", taskId));
        iq.AddAttribute(new StanzaAttribute("xmlns", "privacy"));
        iq.AddAttribute(new StanzaAttribute("type", "set"));
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        ProtocolTreeNode tokens = new ProtocolTreeNode("tokens");
        ProtocolTreeNode token = new ProtocolTreeNode("token");
        token.AddAttribute(new StanzaAttribute("jid", JidNormalize(userId)));
        token.AddAttribute(new StanzaAttribute("type", "trusted_contact"));
        token.AddAttribute(new StanzaAttribute("t", String.valueOf(System.currentTimeMillis() / 1000)));
        tokens.AddChild(token);
        iq.AddChild(tokens);
        return AddTask(iq, new HandleResult("trustedContact"));
    }

    /**
     * 设置头像
     */
    public String SetHDHead(String path) {
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("id", GenerateIqId()));
        iq.AddAttribute(new StanzaAttribute("xmlns", "w:profile:picture"));
        iq.AddAttribute(new StanzaAttribute("type", "set"));
        iq.AddAttribute(new StanzaAttribute("to", JidNormalize(envBuilder_.getFullphone())));

        ProtocolTreeNode picture = new ProtocolTreeNode("picture");
        picture.AddAttribute(new StanzaAttribute("type", "image"));
        picture.SetData(StringUtil.ReadFileContent(path));

        iq.AddChild(picture);
        return AddTask(iq, new HandleResult("SetHDHead"));
    }

    /**
     * 查询上次在线时间
     */
    public void Subscribe(String jid) {
        StringUtil.JidInfo jidInfo = StringUtil.ParseJid(jid);
        byte[] token = null;
        try {
            KeyLockUtil.lock(username);
            token = axolotlManager_.trustedContactStore.getToken(jidInfo.recipientId);
        } catch (Exception ignored) {
        } finally {
            KeyLockUtil.unlock(username);
        }
        ProtocolTreeNode presence = new ProtocolTreeNode("presence");
        presence.AddAttribute(new StanzaAttribute("type", "subscribe"));
        presence.AddAttribute(new StanzaAttribute("to", JidNormalize(jid)));
        if (token != null) {
            ProtocolTreeNode tcToken = new ProtocolTreeNode("tctoken");
            tcToken.SetData(token);
            presence.AddChild(tcToken);
        }
        AddTask(presence);
    }

    /**
     * 取消订阅
     */
    public String unSubscribe(String jid) {
        ProtocolTreeNode presence = new ProtocolTreeNode("presence");
        presence.AddAttribute(new StanzaAttribute("type", "unsubscribe"));
        presence.AddAttribute(new StanzaAttribute("to", JidNormalize(jid)));
        return AddTask(presence);
    }

    /**
     * 设置状态
     *
     * @param status
     * @return
     */
    public String SetStatue(String status) {
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("id", GenerateIqId()));
        iq.AddAttribute(new StanzaAttribute("xmlns", "status"));
        iq.AddAttribute(new StanzaAttribute("type", "set"));
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));

        ProtocolTreeNode statusNode = new ProtocolTreeNode("status");
        statusNode.SetData(status.getBytes(StandardCharsets.UTF_8));

        iq.AddChild(statusNode);
        return AddTask(iq, new HandleResult("SetStatue"));
    }

    /**
     * 设置昵称
     *
     * @param pushName
     * @return
     */
    public String SetPushName(String pushName, String taskId) {
        ProtocolTreeNode presence = new ProtocolTreeNode("presence");
        presence.AddAttribute(new StanzaAttribute("xmlns", "available"));
        presence.AddAttribute(new StanzaAttribute("name", pushName));
        AddTask(presence);
        if (axolotlManager_.GetLastAppstateSyncKey() == null) {
            taskNotify.setEventContent(taskId, ProtocolTreeNode.success(Constant.OK));
            return "";
        }

        ProtocolTreeNode chatState = new ProtocolTreeNode("iq");
        chatState.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        chatState.AddAttribute(new StanzaAttribute("xmlns", "w:sync:app:state"));
        chatState.AddAttribute(new StanzaAttribute("type", "set"));
        chatState.AddAttribute(new StanzaAttribute("id", taskId));


        ProtocolTreeNode sync = new ProtocolTreeNode("sync");
        sync.AddAttribute(new StanzaAttribute("data_namespace", "3"));

        ProtocolTreeNode collection = new ProtocolTreeNode("collection");
        collection.AddAttribute(new StanzaAttribute("name", "critical_block"));
        collection.AddAttribute(new StanzaAttribute("order", "1"));
        int version = axolotlManager_.GetCollectionVersion("critical_block");
        if (version != 0) {
            collection.AddAttribute(new StanzaAttribute("version", String.valueOf(version)));
        }

        ProtocolTreeNode patch = new ProtocolTreeNode("patch");
        WhatsMessage.Qrcode3Jt patchData = GeneratePatch("", pushName);
        patch.SetData(patchData.toByteArray());
        collection.AddChild(patch);
        sync.AddChild(collection);
        chatState.AddChild(sync);
        AddTask(chatState, (srcNode, result) -> {
            taskNotify.setEventContent(taskId, ProtocolTreeNode.success(Constant.OK));
        });
        return "";
    }

    /**
     * 获取用户头像
     *
     * @param jid 8615228092350
     * @return
     */
    public String GetHDHead(String jid) {
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("id", GenerateIqId()));
        iq.AddAttribute(new StanzaAttribute("xmlns", "w:profile:picture"));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        iq.AddAttribute(new StanzaAttribute("to", JidNormalize(jid)));

        ProtocolTreeNode picture = new ProtocolTreeNode("picture");
        picture.AddAttribute(new StanzaAttribute("type", "image"));

        iq.AddChild(picture);
        return AddTask(iq, new HandleResult("GetHDHead"));
    }


    public void GetCdnInfo() {
        ProtocolTreeNode cdnNode = new ProtocolTreeNode("iq");
        cdnNode.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        cdnNode.AddAttribute(new StanzaAttribute("id", GenerateIqId()));
        cdnNode.AddAttribute(new StanzaAttribute("xmlns", "w:m"));
        cdnNode.AddAttribute(new StanzaAttribute("type", "set"));
        ProtocolTreeNode media = new ProtocolTreeNode("media_conn");
        cdnNode.AddChild(media);
        AddTask(cdnNode, (srcNode, result) -> {
            String type = result.GetAttributeValue("type");
            if (!type.equals("result")) {
                log.error("cdn 失败:" + result.toString());
                return;
            }
            ProtocolTreeNode media_conn = result.GetChild("media_conn");
            if (null == media_conn) {
                log.error("media_conn 节点:" + result.toString());
                return;
            }
            cdnAuthKey_ = media_conn.GetAttributeValue("auth");
            // 查找CDN主机
            findCdnHost(media_conn.GetChildren());
        });
    }

    private void findCdnHost(LinkedList<ProtocolTreeNode> children) {
        if (children == null || children.isEmpty()) {
            log.error("用户: {},CDN节点列表为空", username);
            return;
        }

        // 优先查找primary类型的主机
        cdnHost_ = findHostByType(children, "primary");

        // 如果没找到primary，则查找fallback
        if (!StringUtils.hasLength(cdnHost_)) {
            cdnHost_ = findHostByType(children, "fallback");
        }
        if (!StringUtils.hasLength(cdnHost_)) {
            log.error("用户: {},未找到可用的CDN主机", username);
        }
    }

    private String findHostByType(LinkedList<ProtocolTreeNode> children, String targetType) {
        return children.stream()
                .filter(child -> targetType.equals(child.GetAttributeValue("type")))
                .map(child -> child.GetAttributeValue("hostname"))
                .filter(StringUtils::hasLength)
                .findFirst()
                .orElse(null);
    }


    class HandleRetryGetKeysFor implements NodeCallback {
        String msgId_;
        LinkedList<String> jidList_;
        MessageInfo msg;
        SenderKeyRecord senderKeyRecord;
        boolean isNodeSession;
        String retryCount;

        HandleRetryGetKeysFor(String msgId, LinkedList<String> jidList, MessageInfo msg, SenderKeyRecord senderKeyRecord, boolean isNodeSession, String retryCount) {
            msgId_ = msgId;
            jidList_ = jidList;
            this.msg = msg;
            this.senderKeyRecord = senderKeyRecord;
            this.isNodeSession = isNodeSession;
            this.retryCount = retryCount;
        }

        @Override
        public void Run(ProtocolTreeNode srcNode, ProtocolTreeNode result) {
            boolean isGroup = msg.jid.contains("@g.us");
            if (isGroup) {
                EnsureSessionsAndSendToGroup(msg.jid, jidList_, cn.hutool.core.codec.Base64.decode(msg.serialData), msg.messageType, msg.mediaType, msgId_, true, senderKeyRecord, isNodeSession, retryCount);
            } else {
                SendToContact(msg.jid, cn.hutool.core.codec.Base64.decode(msg.serialData), msg.messageType, msg.mediaType, msgId_, retryCount);
            }
        }
    }


    void HandleAeceipt(ProtocolTreeNode node) {
        {
            //发送确认
            ProtocolTreeNode ack = new ProtocolTreeNode("ack");
            ack.AddAttribute(new StanzaAttribute("id", node.GetAttributeValue("id")));
            ack.AddAttribute(new StanzaAttribute("class", "receipt"));
            ack.AddAttribute(new StanzaAttribute("to", node.GetAttributeValue("from")));
            {
                //type
                String value = node.GetAttributeValue("type");
                if (!StringUtil.isEmpty(value)) {
                    ack.AddAttribute(new StanzaAttribute("type", value));
                }
            }

            {
                //participant
                String value = node.GetAttributeValue("participant");
                if (!StringUtil.isEmpty(value)) {
                    ack.AddAttribute(new StanzaAttribute("participant", value));
                }
            }

            /*{
                //list
                ProtocolTreeNode list = node.GetChild("list");
                if (null != list) {
                    ack.AddChild(list);
                }
            }*/
            AddTask(ack);
        }
        String type = node.GetAttributeValue("type");
        if (type.equals("retry")) {
            // 临时屏蔽对方发送的retry处理
            String jid = node.GetAttributeValue("participant");
            String from = node.GetAttributeValue("from");
            String msgId = node.GetAttributeValue("id");
            /*if (from.contains("@g.us")) {
                log.info("用户: {}, 群id为: {}, 粉丝为: {}，消息id: {}, 收到重试消息", username, from, jid, msgId);
                try {
                    StringUtil.JidInfo jidInfo = StringUtil.ParseJid(jid);
                    KeyLockUtil.lock(username);
                    axolotlManager_.saveGroupPreFansRecord(from, Collections.singletonList(jidInfo.toString()));
                } catch (Exception ignored) {
                } finally {
                    KeyLockUtil.unlock(username);
                }
            }*/
            LinkedList<String> jidList = new LinkedList<>();
            if (StringUtil.isEmpty(jid)) {
                jidList.add(from);
            } else {
                jidList.add(jid);
            }
            // 若收到带有一次性密钥的重试包, 则不需要再获取一次性密钥
            // <receipt from='918392998356@s.whatsapp.net' type='retry' id='0D5C08795CF71C2EA7FDB1DA8068AD2B' t='1715932735'><retry v='1' count='2' id='0D5C08795CF71C2EA7FDB1DA8068AD2B' t='1715932734'/><registration>b5dVWA==</registration><keys><identity>zOEUheIm5QT+O3TkDiIhh3AVsebmxg6Q+A296OoJZx8=</identity><type></type><key><id>AFHA</id><value>AdQnJnO75Na9iivQcHD5qXSKPwaLvbEsJgLo/QvHF1I=</value></key><skey><id>
            boolean sessionFromRetryNode = getSessionFromRetryNode(node);
            ProtocolTreeNode retryNode = node.GetChild("retry");
            String retryCount = retryNode.GetAttributeValue("count");
            //处理重试消息
            handlerRetryMsg(msgId, jidList, from, sessionFromRetryNode, retryCount);
        } else if ("peer_msg".equals(type) || "hist_sync".equals(type)) {
            //扫码流程
            if (scanWebSyncStatus.isEnable()) {
                executeScanWeb(type);
            }
        }
        if (delegate_ != null) {
            delegate_.OnSync(node);
        }
    }

    private boolean getSessionFromRetryNode(ProtocolTreeNode node) {
        ProtocolTreeNode keys = node.GetChild("keys");
        if (!ObjectUtil.isNotNull(keys)) {
            return false;
        }
        ProtocolTreeNode registration = node.GetChild("registration");
        if (!ObjectUtil.isNotNull(registration)) {
            return false;
        }
        ProtocolTreeNode identity = keys.GetChild("identity");
        ProtocolTreeNode preKeyNode = keys.GetChild("key");
        ProtocolTreeNode skey = keys.GetChild("skey");
        if ((!ObjectUtil.isNotNull(identity)) && (!ObjectUtil.isNotNull(preKeyNode)) && (!ObjectUtil.isNotNull(skey))) {
            return false;
        }
        // 提取session相关的值并存入数据库
        String jid = StrUtil.isEmpty(node.GetAttributeValue("participant")) ? node.GetAttributeValue("from") : node.GetAttributeValue("participant");
        // 如果有participant值, 说明为群消息的值并且提取为jid
        PreKeyBundle preKeyBundle;
        XmppJid xmppJid;
        try {
            xmppJid = XmppJid.of(jid);
            if (!ObjectUtil.isNotNull(xmppJid)) {
                return false;
            }
            ECPublicKey signedPreKeyPub = Curve.decodePoint(CombineDecodePoint(skey.GetChild("value").GetData()), 0);
            IdentityKey identityKey = new IdentityKey(Curve.decodePoint(CombineDecodePoint(identity.GetData()), 0));
            ECPublicKey preKeyPublic;
            preKeyPublic = Curve.decodePoint(CombineDecodePoint(preKeyNode.GetChild("value").GetData()), 0);
            int prekey_id = DeAdjustId(preKeyNode.GetChild("id").GetData(), 3);
            preKeyBundle = new PreKeyBundle(DeAdjustId(registration.GetData(), 4), xmppJid.getDevice(), prekey_id, preKeyPublic,
                    DeAdjustId(skey.GetChild("id").GetData(), 3), signedPreKeyPub, skey.GetChild("signature").GetData(), identityKey);
        } catch (Exception exception) {
            log.error("用户: {}, 收到retry后获取值失败", username, exception);
            return false;
        }
        // 写入session
        boolean flag = true;
        try {
            KeyLockUtil.lock(username);
            axolotlManager_.CreateSession(xmppJid.getUser(), xmppJid.getDevice(), preKeyBundle);
        } catch (Exception ignore) {
            flag = false;
        } finally {
            KeyLockUtil.unlock(username);
        }
        return flag;
    }

    private void handlerRetryMsg(String msgId, LinkedList<String> jidList, String groupId, boolean nodeHasSession, String retryCount) {
        String host = axolotlManager_.chatHistoryStore.getHost(username, msgId);
        if (!StringUtils.hasLength(host)) {
            return;
        }
        ThreadPoolConfig.chatHistoryPoolExecutor.execute(() -> {
            String encInfo = getChatMsg(host, msgId);
            if (StringUtil.isEmpty(encInfo)) {
                try {
                    KeyLockUtil.lock(username);
                    axolotlManager_.chatHistoryStore.deleteExpiredMsg(username, msgId);
                } catch (Exception ignored) {
                } finally {
                    KeyLockUtil.unlock(username);
                }
                return;
            }
            MessageInfo msg;
            try {
                msg = JSONObject.parseObject(encInfo, MessageInfo.class);
            } catch (Exception e) {
                return;
            }
            if (ObjectUtil.isNull(msg)) {
                return;
            }
            if (StrUtil.isNotEmpty(groupId) && StringUtil.IsGroupJid(groupId)) {
                // 获取senderKeyRecord
                SenderKeyRecord senderKeyRecord;
                try {
                    KeyLockUtil.lock(username);
                    senderKeyRecord = axolotlManager_.LoadSenderKey(groupId);
                } catch (Exception e) {
                    log.error("用户:{} , 群ID: {}, 处理重试消息获取senderKey失败", username, groupId);
                    return;
                } finally {
                    KeyLockUtil.unlock(username);
                }
                if (senderKeyRecord.isEmpty()) {
                    // 若senderKey为空, 目前没有维护群关系, 避免有冲突则不进行重试
                    return;
                }
                new HandleRetryGetKeysFor(msgId, jidList, msg, senderKeyRecord, nodeHasSession, retryCount).Run(null, null);
                return;
            }
            new HandleRetryGetKeysFor(msgId, jidList, msg, null, nodeHasSession, retryCount).Run(null, null);
        });

    }

    private String getChatMsg(String host, String msgId) {
        String url = "http://" + host + "/api/chatRecord?username=" + username + "&msgId=" + msgId;
        try (HttpResponse result = HttpRequest
                .get(url)
                .setConnectionTimeout(1000)
                .setReadTimeout(5000)
                .execute()) {
            int status = result.getStatus();
            if (status == HttpStatus.HTTP_NOT_FOUND) {
                return null;
            }
            String body = result.body();
            if (result.isOk()) {
                return body;
            }
        } catch (Exception e) {
            log.error("用户: {},下载聊天历史消息异常", username, e);
        }
        return null;
    }

    byte[] HandlePreKeyWhisperMessage(StringUtil.JidInfo jidInfo, ProtocolTreeNode encNode) throws UntrustedIdentityException, LegacyMessageException, InvalidVersionException, InvalidMessageException, DuplicateMessageException, InvalidKeyException, InvalidKeyIdException {
        byte[] result = null;
        try {
            KeyLockUtil.lock(username);
            result = axolotlManager_.DecryptPKMsg(jidInfo, encNode.GetData());
        } finally {
            KeyLockUtil.unlock(username);
        }
        if ("2".equals(encNode.GetAttributeValue("v"))) {
            ParseAndHandleMessageProto(jidInfo.recipientId, jidInfo.deviceId, result);
        }
        return result;
    }

    byte[] HandleWhisperMessage(StringUtil.JidInfo jidInfo, ProtocolTreeNode encNode) throws NoSessionException, DuplicateMessageException, InvalidMessageException, UntrustedIdentityException, LegacyMessageException {
        byte[] result = null;
        try {
            KeyLockUtil.lock(username);
            result = axolotlManager_.DecryptMsg(jidInfo, encNode.GetData());
        } finally {
            KeyLockUtil.unlock(username);
        }
        if ("2".equals(encNode.GetAttributeValue("v"))) {
            ParseAndHandleMessageProto(jidInfo.recipientId, jidInfo.deviceId, result);
        }
        return result;
    }

    byte[] HandleSenderKeyMessage(String recepid, int deviceId, ProtocolTreeNode messageNode) throws DuplicateMessageException, InvalidMessageException, LegacyMessageException, NoSessionException {
        LinkedList<ProtocolTreeNode> encNodes = messageNode.GetChildren("enc");
        ProtocolTreeNode encNode = GetEncNode(encNodes, "skmsg");
        byte[] result;
        try {
            KeyLockUtil.lock(username);
            result = axolotlManager_.GroupDecrypt(messageNode.GetAttributeValue("from"), recepid, deviceId, encNode.GetData());
        } finally {
            KeyLockUtil.unlock(username);
        }
        ParseAndHandleMessageProto(recepid, deviceId, result);
        return result;
    }

    void ParseAndHandleMessageProto(String recepid, int deviceId, byte[] serialData) {
        try {
            WhatsMessage.WhatsAppMessage message = WhatsMessage.WhatsAppMessage.parseFrom(serialData);
            if (message.hasSenderKeyDistributionMessage()) {
                try {
                    KeyLockUtil.lock(username);
                    axolotlManager_.GroupCreateSession(message.getSenderKeyDistributionMessage().getGroupId(), recepid, deviceId, message.getSenderKeyDistributionMessage().getAxolotlSenderKeyDistributionMessage().toByteArray());
                } catch (Exception e) {
                    log.error("用户: {}, GroupCreateSession异常", username, e);
                } finally {
                    KeyLockUtil.unlock(username);
                }
            }
        } catch (Exception e) {
            log.error("用户: {}, ParseAndHandleMessageProto异常", username, e);
        }
    }

    ProtocolTreeNode GetEncNode(LinkedList<ProtocolTreeNode> encNodes, String encType) {
        for (ProtocolTreeNode encNode : encNodes) {
            if (encNode.GetAttributeValue("type").equals(encType)) {
                return encNode;
            }
        }
        return null;
    }

    String getEncNodeMediaType(LinkedList<ProtocolTreeNode> encNodes) {
        for (ProtocolTreeNode encNode : encNodes) {
            String mediaType = encNode.GetAttributeValue("mediatype");
            if (StringUtils.hasLength(mediaType)) {
                return mediaType;
            }
        }
        return null;
    }

    int GetEncCount(LinkedList<ProtocolTreeNode> encNodes) {
        for (ProtocolTreeNode encNode : encNodes) {
            String encCount = encNode.GetAttributeValue("count");
            if (StringUtils.hasLength(encCount)) {
                return Integer.parseInt(encCount);
            }
        }
        return -1;
    }

    public byte[] CombineDecodePoint(byte[] buffer) {
        byte[] result = new byte[buffer.length + 1];
        result[0] = 5;
        System.arraycopy(buffer, 0, result, 1, buffer.length);
        return result;
    }

    class HandleGetKeysFor implements NodeCallback {
        NodeCallback resultCallback_;

        HandleGetKeysFor(NodeCallback resultCallback) {
            resultCallback_ = resultCallback;
        }

        @Override
        public void Run(ProtocolTreeNode srcNode, ProtocolTreeNode result) {
            LinkedList<ProtocolTreeNode> listNode = result.GetChildren("list");
            if (listNode.isEmpty()) {
                log.warn("用户: {}, GetKeysFor list empty", username);
                if (resultCallback_ != null) {
                    resultCallback_.Run(srcNode, result);
                }
                return;
            }
            ThreadPoolConfig.sendMsgPoolExecutor.execute(() -> {
                LinkedList<ProtocolTreeNode> users = listNode.get(0).GetChildren("user");
                for (ProtocolTreeNode user : users) {
                    try {
                        //保存会话
                        String jid = user.GetAttributeValue("jid");
                        StringUtil.JidInfo jidInfo = StringUtil.ParseJid(jid);

                        ProtocolTreeNode registration = user.GetChild("registration");

                        //skey
                        ProtocolTreeNode skey = user.GetChild("skey");
                        ECPublicKey signedPreKeyPub = Curve.decodePoint(CombineDecodePoint(skey.GetChild("value").GetData()), 0);

                        //identity
                        ProtocolTreeNode identity = user.GetChild("identity");
                        IdentityKey identityKey = new IdentityKey(Curve.decodePoint(CombineDecodePoint(identity.GetData()), 0));

                        //pre key
                        ProtocolTreeNode preKeyNode = user.GetChild("key");
                        ECPublicKey preKeyPublic = null;

                        int prekey_id = 0;
                        if (preKeyNode != null) {
                            preKeyPublic = Curve.decodePoint(CombineDecodePoint(preKeyNode.GetChild("value").GetData()), 0);
                            prekey_id = DeAdjustId(preKeyNode.GetChild("id").GetData(), 3);
                        }


                        PreKeyBundle preKeyBundle = new PreKeyBundle(DeAdjustId(registration.GetData(), 4), jidInfo.deviceId, prekey_id, preKeyPublic,
                                DeAdjustId(skey.GetChild("id").GetData(), 3), signedPreKeyPub, skey.GetChild("signature").GetData(), identityKey);
                        try {
                            KeyLockUtil.lock(username);
                            axolotlManager_.CreateSession(jidInfo.recipientId, jidInfo.deviceId, preKeyBundle);
                        } catch (Exception ignore) {
                        } finally {
                            KeyLockUtil.unlock(username);
                        }
                    } catch (Exception e) {
                        log.error("保存会话异常", e);
                    }
                }
                if (resultCallback_ != null) {
                    resultCallback_.Run(srcNode, result);
                }
            });
        }
    }


    void GetKeysFor(List<String> jids, NodeCallback callback) {
        if (jids.isEmpty()) {
            log.error("用户: {}, query list is null", username);
            return;
        }
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("xmlns", "encrypt"));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        iq.AddAttribute(new StanzaAttribute("id", GenerateIqId()));

        ProtocolTreeNode key = new ProtocolTreeNode("key");
        for (String jid : jids) {
            ProtocolTreeNode user = new ProtocolTreeNode("user");
            user.AddAttribute(new StanzaAttribute("jid", jid));
            key.AddChild(user);
        }
        iq.AddChild(key);
        AddTask(iq, new HandleGetKeysFor(callback));
    }

    void SendRetry(ProtocolTreeNode node, boolean keyInvalid) {
        String iqid = node.IqId();
        LinkedList<ProtocolTreeNode> encNodes = node.GetChildren("enc");
        Integer count = GetEncCount(encNodes);
        if (count == -1) {
            //没有发现count 节点，使用自己保存的
            count = retries_.get(iqid);
            if (count == null) {
                count = Integer.valueOf(1);
            } else {
                count++;
            }
            retries_.put(iqid, count);
        } else {
            count++;
        }
        if (count > 5) {
            //发送一个receipt
            retries_.remove(iqid);
            String participant = node.GetAttributeValue("participant");
            sendReceiptTag(node.IqId(), node.GetAttributeValue("from"), participant);
            //发送已读标志
            DelayTask delayTask = new DelayTask(username, node.IqId(), node.GetAttributeValue("from"), participant, "sendReadTag");
            DelayExecuteTask.addDelayTask(delayTask, RandomUtil.randomInt(2000, 4500));
            return;
        }
        ProtocolTreeNode receipt = new ProtocolTreeNode("receipt");
        receipt.AddAttribute(new StanzaAttribute("to", node.GetAttributeValue("from")));
        receipt.AddAttribute(new StanzaAttribute("id", iqid));
        receipt.AddAttribute(new StanzaAttribute("type", "retry"));
        String participant = node.GetAttributeValue("participant");
        if (!StringUtil.isEmpty(participant)) {
            receipt.AddAttribute(new StanzaAttribute("participant", participant));
        }

        String recipient = node.GetAttributeValue("recipient");
        if (!StringUtil.isEmpty(recipient) && !recipient.equals(node.GetAttributeValue("from"))) {
            receipt.AddAttribute(new StanzaAttribute("recipient", recipient));
        }
        //retry
        ProtocolTreeNode retry = new ProtocolTreeNode("retry");
        retry.AddAttribute(new StanzaAttribute("v", "1"));
        retry.AddAttribute(new StanzaAttribute("count", String.valueOf(count)));
        retry.AddAttribute(new StanzaAttribute("id", iqid));
        retry.AddAttribute(new StanzaAttribute("t", node.GetAttributeValue("t")));
        receipt.AddChild(retry);


        //register
        try {
            ProtocolTreeNode registration = new ProtocolTreeNode("registration");
            KeyLockUtil.lock(username);
            registration.SetData(AdjustId(axolotlManager_.getLocalRegistrationId(), 4));
            receipt.AddChild(registration);
            if (count == 2 && keyInvalid) {
                forceUploadOneTimePreKeys();
            }
            if (count >= 2) {
                ProtocolTreeNode keys = new ProtocolTreeNode("keys");
                //需要额外发送一些 skey
                {
                    //identify
                    ProtocolTreeNode identity = new ProtocolTreeNode("identity");
                    identity.SetData(axolotlManager_.GetIdentityKeyPair().getPublicKey().serialize(), 1, 32);
                    keys.AddChild(identity);
                }

                {
                    //type
                    ProtocolTreeNode type = new ProtocolTreeNode("type");
                    type.SetData(new byte[]{5});
                    keys.AddChild(type);
                }
                {
                    // prekey 节点
                    PreKeyRecord record = axolotlManager_.GetOnePreKeyRecord();
                    ProtocolTreeNode key = new ProtocolTreeNode("key");
                    key.AddChild(new ProtocolTreeNode("id", null, AdjustId(record.getId(), 3)));

                    ProtocolTreeNode value = new ProtocolTreeNode("value");
                    value.SetData(record.getKeyPair().getPublicKey().serialize(), 1, 32);
                    key.AddChild(value);
                    keys.AddChild(key);

                }

                {
                    SignedPreKeyRecord signedPreKeyRecord = axolotlManager_.LoadLatestSignedPreKey(false);
                    //signature  key skey
                    ProtocolTreeNode skey = new ProtocolTreeNode("skey");
                    {
                        ProtocolTreeNode id = new ProtocolTreeNode("id");
                        id.SetData(AdjustId(signedPreKeyRecord.getId(), 3));
                        skey.AddChild(id);
                    }
                    {
                        ProtocolTreeNode value = new ProtocolTreeNode("value");
                        value.SetData(signedPreKeyRecord.getKeyPair().getPublicKey().serialize(), 1, 32);
                        skey.AddChild(value);
                    }

                    {
                        ProtocolTreeNode signature = new ProtocolTreeNode("signature");
                        signature.SetData(signedPreKeyRecord.getSignature());
                        skey.AddChild(signature);
                    }
                    keys.AddChild(skey);
                }
                receipt.AddChild(keys);
            }
        } catch (Exception ignore) {
        } finally {
            KeyLockUtil.unlock(username);
        }
        AddTask(receipt);
    }


    class HandlePendingMessage implements NodeCallback {
        ProtocolTreeNode pendingMessage_;

        HandlePendingMessage(ProtocolTreeNode pendingMessage) {
            pendingMessage_ = pendingMessage;
        }

        @Override
        public void Run(ProtocolTreeNode srcNode, ProtocolTreeNode result) {
            HandleRecvMessage(pendingMessage_);
        }
    }

    public void HandleRecvMessage(ProtocolTreeNode node) {
        // 基础信息提取
        WhatsAppMessageContext msgContext = extractMessageContext(node);

        // 特殊消息快速处理
        if (handleSpecialMessages(msgContext, node)) {
            return;
        }
        // 存储jidLid
        if (msgContext.isPrivatePKMsg) {
            saveNodeJidLid(msgContext.from, msgContext.lid);
        }
        // 解密消息
        byte[] plainText = decryptMessage(node, msgContext);
        if (plainText == null) {
            return;
        }
        if (msgContext.isPrivatePKMsg) {
            log.info("用户: {} ,粉丝: {}, 明文消息: {}", username, msgContext.from, cn.hutool.core.codec.Base64.encode(plainText));
        }
        // 处理解密后的消息
        processDecryptedMessage(plainText, msgContext, node);
    }

    // 消息上下文信息类
    private static class WhatsAppMessageContext {
        String from;
        String participant;
        String type;
        String mediaType;
        String msgId;
        long msgTime;
        boolean isGroup;
        String fansNickName;
        String verifiedName;
        String senderLid;
        boolean urlNumber;
        StringUtil.JidInfo senderJidInfo;
        // 粉丝lid
        String lid;
        // 寻址类型pn(手机号模式)
        boolean AddressingPNMode;
        // 是否为机器人消息
        boolean isBot;
        // 是否为PKMsg个人消息
        boolean isPrivatePKMsg;
    }

    private WhatsAppMessageContext extractMessageContext(ProtocolTreeNode node) {
        String addressingMode = node.GetAttributeValue("addressing_mode");
        WhatsAppMessageContext context = new WhatsAppMessageContext();
        context.from = node.GetAttributeValue("from");
        context.participant = node.GetAttributeValue("participant");
        context.AddressingPNMode = true;
        if (StrUtil.isNotEmpty(addressingMode) && addressingMode.equals("lid")) {
            context.AddressingPNMode = false;
            String participantLid = node.GetAttributeValue("participant_lid");
            if (StrUtil.isNotEmpty(participantLid)) {
                context.lid = participantLid;
            } else {
                context.lid = node.GetAttributeValue("participant");
                if (StrUtil.isNotEmpty(node.GetAttributeValue("participant_pn"))) {
                    context.participant = node.GetAttributeValue("participant_pn");
                }
            }
        }
        context.isGroup = context.from.contains("@g.us");
        // 处理个人消息的lid格式消息
        if (context.from.contains("@lid") && StrUtil.isNotEmpty(node.GetAttributeValue("sender_pn")) && !context.isGroup) {
            context.AddressingPNMode = false;
            context.from = node.GetAttributeValue("sender_pn");
            context.lid = node.GetAttributeValue("from");
        }
        // 处理群聊消息中的机器人消息
        if (context.isGroup && StrUtil.isNotEmpty(context.participant) && context.participant.contains("@bot")) {
            context.isBot = true;
        }
        context.msgId = node.IqId();

        String t = node.GetAttributeValue("t");
        context.msgTime = Convert.toLong(t, 0L) * 1000;
        context.type = node.GetAttributeValue("type");
        context.fansNickName = node.GetAttributeValue("notify");
        context.verifiedName = node.GetAttributeValue("verified_name");
        context.senderLid = node.GetAttributeValue("sender_lid");
        context.urlNumber = node.getOneChildren("url_number") != null;

        String senderJid = context.isGroup ? context.participant : context.from;
        context.senderJidInfo = StringUtil.ParseJid(senderJid);

        LinkedList<ProtocolTreeNode> encNodes = node.GetChildren("enc");
        context.mediaType = getEncNodeMediaType(encNodes);
        context.isPrivatePKMsg = false;
        if (!context.isGroup) {
            context.isPrivatePKMsg = isPrivatePKMsg(node);
            if (context.from.contains("s.whatsapp.net") && StrUtil.isNotEmpty(node.GetAttributeValue("sender_lid"))) {
                context.lid = node.GetAttributeValue("sender_lid");
            }
            log.info("用户: {}, 接收消息: {}", username, node);
        }

        if (context.isGroup) {
            if (StringUtils.hasLength(context.type) && MESSAGE_TYPE.add(context.type)) {
                log.info("Group message type: {}", context.type);
            }
            if ("media".equals(context.mediaType) && MESSAGE_MEDIA_TYPE.add(context.mediaType)) {
                log.info("Group media message type: {}", context.type);
            }
        }
        return context;
    }

    public static boolean isPrivatePKMsg(ProtocolTreeNode node) {
        ProtocolTreeNode enc = node.getOneChildren("enc");
        if (ObjectUtil.isNotNull(enc)) {
            String type = enc.GetAttributeValue("type");
            if (StrUtil.isNotEmpty(type) && type.equals("pkmsg")) {
                return true;
            }
        }
        return false;
    }

    private boolean handleSpecialMessages(WhatsAppMessageContext msgContext, ProtocolTreeNode node) {
        // 处理频道内容
        if (msgContext.from.contains("@newsletter")) {
            replyAck(node);
            return true;
        }

        // 处理空加密节点
        LinkedList<ProtocolTreeNode> encNodes = node.GetChildren("enc");
        if (encNodes.isEmpty()) {
            sendReceiptTag(msgContext.msgId, msgContext.from, msgContext.participant);
            return true;
        }

        // 处理朋友圈消息
        if ("status@broadcast".equals(msgContext.from)) {
            sendReceiptTag(msgContext.msgId, msgContext.from, msgContext.participant);
            return true;
        }

        // 处理群聊bot消息
        if (msgContext.isGroup && msgContext.isBot) {
            sendReceiptTag(msgContext.msgId, msgContext.from, msgContext.participant);
            return true;
        }
        // 处理非群聊bot消息
        if (!msgContext.isGroup && msgContext.from.contains("@bot")) {
            sendReceiptTag(msgContext.msgId, msgContext.from, null);
            return true;
        }
        // 处理群聊消息中的心情消息
        if (msgContext.isGroup && "reaction".equals(msgContext.type)) {
            if (!msgContext.AddressingPNMode) {
                sendReactionReceiptTag(msgContext.msgId, msgContext.from, msgContext.lid);
            } else {
                sendReactionReceiptTag(msgContext.msgId, msgContext.from, msgContext.participant);
            }
            return true;
        }
        return false;
    }

    private byte[] decryptMessage(ProtocolTreeNode node, WhatsAppMessageContext msgContext) {
        try {
            byte[] plainText = tryDecryptMessage(node, msgContext);
            if (plainText == null) {
                SendRetry(node, false);
            }
            return plainText;
        } catch (Exception e) {
            handleDecryptionException(e, node, msgContext);
            return null;
        }
    }

    private void handleDecryptionException(Exception e, ProtocolTreeNode node, WhatsAppMessageContext msgContext) {
        String msgId = node.IqId();
        String from = msgContext.from;
        String participant = msgContext.participant;

        if (e instanceof InvalidKeyIdException) {
            log.error("用户：{}，InvalidKeyIdException", username);
            SendRetry(node, true);
        } else if (e instanceof NoSessionException) {
            log.error("用户：{}，NoSessionException", username);
            SendRetry(node, false);
        } else if (e instanceof LegacyMessageException) {
            log.error("用户：{}，LegacyMessageException", username);
            retries_.remove(msgId);
            sendReceiptTag(msgId, from, participant);
        } else if (e instanceof InvalidMessageException) {
            log.error("用户：{}，InvalidMessageException", username);
            SendRetry(node, false);
        } else if (e instanceof UntrustedIdentityException) {
            log.error("用户：{}，UntrustedIdentityException", username);
            retries_.remove(msgId);
            sendReceiptTag(msgId, from, participant);
        } else if (e instanceof InvalidKeyException) {
            log.error("用户：{}，InvalidKeyException", username);
            retries_.remove(msgId);
            sendReceiptTag(msgId, from, participant);
        } else if (e instanceof DuplicateMessageException) {
            log.error("用户：{}，DuplicateMessageException", username);
            SendRetry(node, false);
        } else if (e instanceof InvalidVersionException) {
            log.error("用户: {}, InvalidVersionException", username, e);
            retries_.remove(msgId);
            sendReceiptTag(msgId, from, participant);
        } else {
            log.error("用户：{}，解析消息异常：", username, e);
            retries_.remove(msgId);
            sendReceiptTag(msgId, from, participant);
        }
    }

    private byte[] tryDecryptMessage(ProtocolTreeNode node, WhatsAppMessageContext msgContext) throws Exception {
        LinkedList<ProtocolTreeNode> encNodes = node.GetChildren("enc");
        byte[] plainText = null;

        // 处理预密钥消息
        ProtocolTreeNode pkMsgEncNode = GetEncNode(encNodes, "pkmsg");
        if (pkMsgEncNode != null) {
            plainText = HandlePreKeyWhisperMessage(msgContext.senderJidInfo, pkMsgEncNode);
            if (!msgContext.isGroup) {
                handlePkMsgSyncDevices(msgContext);
            }
            // 处理非群组的首条消息
            if (!msgContext.from.contains("@g.us")) {
                handleFirstMessageRetry(msgContext.from);
            }
        }

        // 处理普通加密消息
        ProtocolTreeNode whisperEncNode = GetEncNode(encNodes, "msg");
        if (whisperEncNode != null) {
            plainText = HandleWhisperMessage(msgContext.senderJidInfo, whisperEncNode);
        }

        // 处理发送者密钥消息
        ProtocolTreeNode skMsgEncNode = GetEncNode(encNodes, "skmsg");
        if (skMsgEncNode != null) {
            plainText = HandleSenderKeyMessage(msgContext.senderJidInfo.recipientId,
                    msgContext.senderJidInfo.deviceId, node);
        }

        if (plainText != null) {
            retries_.remove(node.IqId());
        }

        return plainText;
    }

    private void processDecryptedMessage(byte[] plainText, WhatsAppMessageContext msgContext, ProtocolTreeNode node) {
        try {
            WhatsMessage.WhatsAppMessage msg = WhatsMessage.WhatsAppMessage.parseFrom(plainText);

            // 检查是否是自己的消息
            if (isSelfMessage(msgContext.senderJidInfo)) {
                //自己消息不做转发
                sendReceiptTag(msgContext.msgId, msgContext.from, msgContext.participant);
                return;
            }

            // 异步处理消息内容
            processMessageAsync(msg, msgContext);

            // 处理消息回执
            handleMessageReceipts(msg, msgContext, node);

        } catch (Exception e) {
            log.error("用户: {}, 协议解析异常", username, e);
        }
    }

    private void processMessageAsync(WhatsMessage.WhatsAppMessage msg, WhatsAppMessageContext msgContext) {
        ThreadPoolConfig.threadPoolExecutor.execute(() -> {
            if (log.isDebugEnabled()) {
                log.debug("用户：{}，消息类型：{}，媒体类型：{}，群id：{}，粉丝：{}，粉丝昵称：{}，接收消息：{}",
                        username, msgContext.type, msgContext.mediaType,
                        msgContext.isGroup ? msgContext.from : null,
                        msgContext.senderJidInfo.toString(), msgContext.fansNickName,
                        msg.toString());
            }
            String fansId = msgContext.senderJidInfo.recipientId + "@s.whatsapp.net";
            boolean offlineMessage = WhatsAppUtils.checkIsOfflineMessage(loginTime, msgContext.msgTime);

            // 处理消息
            if (!msgContext.isGroup) {
                AsyncMessageService.parse(msg, null, fansId, userId, msgContext.msgId,
                        msgContext.type, msgContext.mediaType, username,
                        msgContext.msgTime, offlineMessage, msgContext.urlNumber);
            } else {
                AsyncMessageService.parse(msg, msgContext.from, fansId, userId, msgContext.msgId,
                        msgContext.type, msgContext.mediaType, username,
                        msgContext.msgTime, offlineMessage, msgContext.urlNumber);
            }

            // 处理限时消息
            handleEphemeralMessage(msg, msgContext);
        });
    }

    private void handleEphemeralMessage(WhatsMessage.WhatsAppMessage msg, WhatsAppMessageContext msgContext) {
        WhatsMessage.WhatsAppProtocolMessage protocolMessage = msg.getProtocolMessage();
        if (WhatsMessage.WhatsAppProtocolMessageType.EPHEMERAL_SETTING.equals(protocolMessage.getType())) {
            updateEphemeralMessageSettings(msgContext, protocolMessage.getEphemeralExpiration());
            return;
        }

        // 处理扩展文本消息中的限时消息设置
        WhatsMessage.WhatsAppExtendedTextMessage extendedTextMessage = msg.getExtendedTextMessage();
        if (extendedTextMessage.hasContextInfo()) {
            handleExtendedMessageEphemeralSettings(extendedTextMessage, msgContext);
        }
    }

    private void handleMessageReceipts(WhatsMessage.WhatsAppMessage msg, WhatsAppMessageContext msgContext, ProtocolTreeNode node) {
        // 发送消息回执
        if (!msgContext.AddressingPNMode) {
            if (msgContext.isGroup) {
                sendReceiptTag(node.IqId(), msgContext.from, msgContext.lid);
            } else {
                // 个人匿名消息
                sendReceiptTag(node.IqId(), msgContext.lid, msgContext.participant);
            }

        } else {
            sendReceiptTag(node.IqId(), msgContext.from, msgContext.participant);
        }
        //发送wam日志
        if (msgContext.isPrivatePKMsg) {
            if (msgContext.urlNumber) {
                AddTask(WamReportService.reportPkMsgEvent(this, "click_to_chat_link"));
                log.info("用户：{}，已发送pkmsg wam日志", username);
            } else {
                String desc = null;
                WhatsMessage.WhatsAppContextInfo contextInfo = WhatsAppContextInfoParseService.parse(msg, msgContext.type, msgContext.mediaType);
                if (ObjectUtil.isNotNull(contextInfo)) {
                    desc = contextInfo.getDesc();
                }
                AddTask(WamReportService.reportPkMsgEvent(this, desc));
                log.info("用户：{}，已发送pkmsg wam日志, desc: {}", username, desc);
            }
        }

        // 发送已读标记
        if (msgContext.isGroup) {
            DelayTask delayTask;
            if (!msgContext.AddressingPNMode) {
                delayTask = new DelayTask(username, msgContext.msgId,
                        msgContext.from, msgContext.lid, "sendReadTag");
            } else {
                delayTask = new DelayTask(username, msgContext.msgId,
                        msgContext.from, msgContext.participant, "sendReadTag");
            }
            DelayExecuteTask.addDelayTask(delayTask, RandomUtil.randomInt(10000, 12000));
        } else {
            if (msgContext.isPrivatePKMsg) {
                //pkmsg不发送已读
                return;
            }
            if (StringUtils.hasLength(msgContext.from)) {
                DelayTask delayTask;
                if (msgContext.AddressingPNMode) {
                    delayTask = new DelayTask(username, msgContext.msgId,
                            msgContext.from, null, "sendReadTag");
                } else {
                    // 个人lid格式消息
                    delayTask = new DelayTask(username, msgContext.msgId,
                            msgContext.lid, null, "sendReadTag");
                }
                DelayExecuteTask.addDelayTask(delayTask, RandomUtil.randomInt(10000, 12000));
            }
        }
    }

    private void handlePkMsgSyncDevices(WhatsAppMessageContext msgContext) {
        if (getPkMsgSyncStatus(msgContext.senderJidInfo.recipientId) == 0) {
            ContactInfoResult contactInfoResult = createContactInfoResult(msgContext);
            hashedWheelTimer.newTimeout(timeout -> {
                handlerPkMsgSyncContact(msgContext.senderJidInfo, contactInfoResult);
            }, 2, TimeUnit.SECONDS);
        }
    }

    private void handleFirstMessageRetry(String from) {
        ThreadPoolConfig.sendMsgPoolExecutor.execute(() -> {
            try {
                XmppJid xmppJid = XmppJid.of(from);
                if (ObjectUtil.isNotNull(xmppJid)) {
                    List<String> msgIds = axolotlManager_.chatHistoryStore
                            .selectBeforeMessage(xmppJid.getUser());

                    for (String id : msgIds) {
                        processHistoryMessage(xmppJid, id, from);
                    }
                }
            } catch (Exception ignored) {
            }
        });
    }

    private void processHistoryMessage(XmppJid xmppJid, String id, String from) {
        String encInfo = null;
        try {
            KeyLockUtil.lock(username);
            String host = axolotlManager_.chatHistoryStore.getHost(xmppJid.getUser(), id);
            encInfo = getChatMsg(host, id);
        } catch (Exception ignored) {
        } finally {
            KeyLockUtil.unlock(username);
        }

        if (ObjectUtil.isNotNull(encInfo)) {
            MessageInfo messageInfo = JSONObject.parseObject(encInfo, MessageInfo.class);
            if (ObjectUtil.isNotNull(messageInfo)) {
                LinkedList<String> linkedList = new LinkedList<>();
                linkedList.add(from);
                new HandleRetryGetKeysFor(id, linkedList, messageInfo, null, true, "1")
                        .Run(null, null);
            }
        }
    }

    private void updateEphemeralMessageSettings(WhatsAppMessageContext msgContext, int expiredTime) {
        String tempId = msgContext.isGroup ? msgContext.from :
                msgContext.senderJidInfo.recipientId + "@s.whatsapp.net";
        try {
            KeyLockUtil.lock(username);
            axolotlManager_.ephemeralMessageStore.insert(tempId, expiredTime);
        } catch (Exception ignored) {
        } finally {
            KeyLockUtil.unlock(username);
        }
    }

    private void handleExtendedMessageEphemeralSettings(
            WhatsMessage.WhatsAppExtendedTextMessage extendedTextMessage,
            WhatsAppMessageContext msgContext) {
        WhatsMessage.WhatsAppContextInfo contextInfo = extendedTextMessage.getContextInfo();
        int expiredTime = contextInfo.getExpiredTime();
        String fansId = msgContext.senderJidInfo.recipientId + "@s.whatsapp.net";
        String tempId = msgContext.isGroup ? msgContext.from : fansId;

        try {
            KeyLockUtil.lock(username);
            int expirationTime = axolotlManager_.ephemeralMessageStore.getExpirationTime(fansId);
            if (expiredTime == expirationTime) {
                return;
            }
            axolotlManager_.ephemeralMessageStore.insert(tempId, expiredTime);
        } catch (Exception ignored) {
        } finally {
            KeyLockUtil.unlock(username);
        }
    }

    private ContactInfoResult createContactInfoResult(WhatsAppMessageContext msgContext) {
        ContactInfoResult result = new ContactInfoResult();
        result.setJid(WhatsAppUtils.JidNormalize(msgContext.senderJidInfo.recipientId));
        result.setLid(msgContext.senderLid);
        result.setSerial(msgContext.verifiedName);

        HashSet<String> hashSet = new HashSet<>();
        hashSet.add(msgContext.senderJidInfo.toString());
        result.setDeviceHash(CryptUtil.PHash(hashSet));

        return result;
    }

    private boolean isSelfMessage(StringUtil.JidInfo jidInfo) {
        return StrUtil.replace(userId, "@s.whatsapp.net", "").equals(jidInfo.recipientId);
    }

    /**
     * 回复ack
     *
     * @param node 节点
     */
    void replyAck(ProtocolTreeNode node) {
        String type = node.GetAttributeValue("type");
        ProtocolTreeNode ack = new ProtocolTreeNode("ack");
        ack.AddAttribute(new StanzaAttribute("id", node.GetAttributeValue("id")));
        ack.AddAttribute(new StanzaAttribute("class", node.GetTag()));
        ack.AddAttribute(new StanzaAttribute("to", node.GetAttributeValue("from")));
        if (!StringUtil.isEmpty(type)) {
            ack.AddAttribute(new StanzaAttribute("type", type));
        }
        AddTask(ack);
    }

    public int getEphemeralMessageTime(String fansId) {
        int expirationTime = 0;
        String jid = WhatsAppUtils.JidNormalize(fansId);
        try {
            KeyLockUtil.lock(username);
            expirationTime = axolotlManager_.ephemeralMessageStore.getExpirationTime(jid);
        } catch (Exception ignored) {
        } finally {
            KeyLockUtil.unlock(username);
        }
        return expirationTime;
    }

    public WhatsMessage.WhatsAppContextInfo.Builder applyEphemeralMessage(String fansId, WhatsMessage.WhatsAppContextInfo.Builder contextInfoBuilder) {
        int expirationTime = getEphemeralMessageTime(fansId);
        if (expirationTime > 0) {
            if (contextInfoBuilder == null) {
                contextInfoBuilder = WhatsMessage.WhatsAppContextInfo.newBuilder();
            }
            WhatsAppUtils.applyEphemeralMessage(contextInfoBuilder, expirationTime);
        }
        return contextInfoBuilder;
    }

    /**
     * 发送收据
     */
    public void sendReceiptTag(String iqId, String to, String participant) {
        ProtocolTreeNode receipt = new ProtocolTreeNode("receipt");
        receipt.AddAttribute(new StanzaAttribute("id", iqId));
        receipt.AddAttribute(new StanzaAttribute("to", to));
        if (StringUtils.hasLength(participant)) {
            receipt.AddAttribute(new StanzaAttribute("participant", participant));
        }
        AddTask(receipt);
    }

    /**
     * 发送收据
     */
    public void sendReactionReceiptTag(String iqId, String to, String participant) {
        ProtocolTreeNode receipt = new ProtocolTreeNode("receipt");
        receipt.AddAttribute(new StanzaAttribute("id", iqId));
        receipt.AddAttribute(new StanzaAttribute("to", to));
        receipt.AddAttribute(new StanzaAttribute("type", "inactive"));
        if (StringUtils.hasLength(participant)) {
            receipt.AddAttribute(new StanzaAttribute("participant", participant));
        }
        AddTask(receipt);
    }

    /**
     * 发送已读标志
     */
    public void sendReadTag(String iqId, String to, String participant) {
        ProtocolTreeNode receipt = new ProtocolTreeNode("receipt");
        receipt.AddAttribute(new StanzaAttribute("id", iqId));
        receipt.AddAttribute(new StanzaAttribute("to", to));
        receipt.AddAttribute(new StanzaAttribute("type", "read"));
        if (!StringUtil.isEmpty(participant)) {
            receipt.AddAttribute(new StanzaAttribute("participant", participant));
        }
        AddTask(receipt);
    }

    /**
     * 批量发送已读标志
     */
    public void sendReadTag(List<String> msgIds, String to, String participant) {
        ProtocolTreeNode receipt = new ProtocolTreeNode("receipt");
        String remove = msgIds.remove(msgIds.size() - 1);
        receipt.AddAttribute(new StanzaAttribute("id", remove));
        receipt.AddAttribute(new StanzaAttribute("to", to));
        receipt.AddAttribute(new StanzaAttribute("type", "read"));
        if (!StringUtil.isEmpty(participant)) {
            receipt.AddAttribute(new StanzaAttribute("participant", participant));
        }
        if (!msgIds.isEmpty()) {
            ProtocolTreeNode list = new ProtocolTreeNode("list");
            for (String msgId : msgIds) {
                ProtocolTreeNode item = new ProtocolTreeNode("item");
                item.AddAttribute(new StanzaAttribute("id", msgId));
                list.AddChild(item);
            }
            receipt.AddChild(list);
        }
        AddTask(receipt);
    }

    void HandleAck(ProtocolTreeNode node) {
        String type = node.GetAttributeValue("class");
        if (type.equals("message")) {
            if (delegate_ != null) {
                String taskId = node.IqId();
                if (StringUtils.hasLength(taskId)) {
                    taskNotify.setEventContent(taskId, node);
                }
            }
            return;
        }
        //处理主动任务响应
        String taskId = node.IqId();
        if (StringUtils.hasLength(taskId)) {
            taskNotify.setEventContent(taskId, node);
        }
        if (type.equals("call")) {
            // 处理通话相关的语音包
            // <ack from='@s.whatsapp.net' class='call' type='offer' id='AE95A7FEB6F367B52F8F24E685D96678'><relay attribute_padding='1' peer_pid='0' self_pid='1' uuid='0hai0rCZNsfd4gxb' call-creator='85294631974@s.whatsapp.net' call-id='D6E3FE8BEFD8487F6DB4FFA6F7400B90' joinable='1'><participant pid='0' jid='@s.whatsapp.net'/><token id='0'>CQM8blf0ROyDHTMZaQSPzBWfPETsDI5+DfQ6jw74SZC7uB5hZqjSrFuwtNpffcE0VWB+tsxdWi1qlGh+aqsPG+Rz5/AzZ6uRby6tBFM2oMEkWN9Y2P96cOtbwGR2rhBOd+qPZWbmmLGHfh1SiSV5p36xDqe1VSrJZpK2w5vjHxhTPtsrKF7cCZGBlr3IPtY2YoJfR5sb</token><token id='1'>CQNTwZcyk4ZsfG3p3bl1XAEfqnjZ19jm+L0QAxHQvdq903KncTLbai64nxgSxZ+VgRQw2I2TRmruQf6iCxLoAY0EZlK4A7k8ZnQfzGHExqC416d0Elc55PjbewnQjxS6mnNdLLzkummzwWhQ5RjyqyJVV87TpDueLj13/Fcyho0C3FMlUknsDRAEplIbflI7CLGZqnNI</token><token id='2'>CQNelDSaxygM/1c3NNQyZY08JIF3K9CqOpmU1lBUyL8VKZP6lQrXy9ZPsAb8Pgv/siXDwRpWtq3j8h751jCgqeTD+9KI3XBgPZI1wEonKjWyhATHC+bSBBkL2h38qKU433chVXEFDWYcviauupKWG1ZbWw1R4YPHiFARuAbX3FkslLz1hc5yMhIpbpdMdt37/xG2Bn1j</token><token id='3'>CQOjkpSuzhoGXQN7Y8ytuQ9VEO2dh+X0cD5YXq2NO644I755gq8f4Wtoy480Vbz5F+IbDHP4+t7wvon+ySDWevj/bB8Gj1/wDn8DazKeGNemcX26fMsXigul1T9ITjz7EYX8FN/Ipa7w1zE1Jd9vHyMiffQ+ogugoH4ihDrhIN8lmJEszFS64OAfsylGCfJ7EPmlHEwi</token><token id='4'>CQO++iXTu4MC5pw8MM6sMT62C91wlTTTNXPikghHB45OOx84dE5PFx2OLZocnYDH3B5pBi8ahvI1u3Fx7Imr53xSdUPzfTfUfgWBIs9MXL1S/kb18VsAMGB9KMHwyJKQycfPRRi/4skbvf3w2ExZ5vSf5QeWxDgGkk3trMnffHqSqUZJCR+tXiHzg6Y5WifN7g8hsCsk</token><key>r5FnOdZpRVoHSjnIktax7Q==</key><te2 protocol='1' relay_id='0' token_id='0'>Hw1XMg2W</te2><te2 protocol='1' relay_id='0' token_id='0'>KgMogPIXAMP6zrAMAAABdw2W</te2><te2 relay_id='0' token_id='0'>Hw1XMg2W</te2><te2 relay_id='0' token_id='0'>KgMogPIXAMP6zrAMAAABdw2W</te2><te2 protocol='1' relay_id='1' token_id='1'>nfAPPg2W</te2><te2 protocol='1' relay_id='1' token_id='1'>KgMogPIMAcH6zrAMAAABdw2W</te2><te2 relay_id='1' token_id='1'>nfAPPg2W</te2><te2 relay_id='1' token_id='1'>KgMogPIMAcH6zrAMAAABdw2W</te2><te2 protocol='1' relay_id='2' token_id='3'>nfDsPg2W</te2><te2 protocol='1' relay_id='2' token_id='3'>KgMogPJ6AMf6zrAMAAABdw2W</te2><te2 relay_id='2' token_id='3'>nfDsPg2W</te2><te2 relay_id='2' token_id='3'>KgMogPJ6AMf6zrAMAAABdw2W</te2><te2 protocol='1' relay_id='3' token_id='2'>Hw1fPg2W</te2><te2 protocol='1' relay_id='3' token_id='2'>KgMogPJNAcH6zrAMAAABdw2W</te2><te2 relay_id='3' token_id='2'>Hw1fPg2W</te2><te2 relay_id='3' token_id='2'>KgMogPJNAcH6zrAMAAABdw2W</te2><te2 protocol='1' relay_id='4' token_id='4'>Hw1SMA2W</te2><te2 protocol='1' relay_id='4' token_id='4'>KgMogPIPAMP6zrAMAAABdw2W</te2><te2 relay_id='4' token_id='4'>Hw1SMA2W</te2><te2 relay_id='4' token_id='4'>KgMogPIPAMP6zrAMAAABdw2W</te2><hbh_key>sgXQaP0bEzcSD8pUs9f82PJGGohVab9Cuf+/W47G</hbh_key></relay><user jid='@s.whatsapp.net'><device jid='@s.whatsapp.net'/></user><rte>JpYMgx9e</rte><uploadfieldstat/><userrate/><voip_settings uncompressed='1'></voip_settings></ack>
            String nodeType = node.GetAttributeValue("type");
            if (nodeType.equals("offer")) {

                /*ProtocolTreeNode video = recvAcceptNode.getOneChildren("video");
                if (ObjectUtil.isNotNull(video)) {
                    //视频暂时不做处理
                    log.info("用户: {}, 已接收视频通话", username);
                    return;
                }*/
                String to = node.GetAttributeValue("from");
                // 获取STUN相关的信息, TURN服务器, tokenId, key
                ProtocolTreeNode relayNode = node.getOneChildren("relay");
                String callId = relayNode.GetAttributeValue("call-id");
                // 获取key
                String key = new String(relayNode.getOneChildren("key").GetData(), StandardCharsets.UTF_8);
                LinkedList<ProtocolTreeNode> tokenNodes = relayNode.GetChildren("token");
                LinkedList<ProtocolTreeNode> te2 = relayNode.GetChildren("te2");
                TurnIpData turnIpData = TE2IpParser.parseTurnIpData(tokenNodes, te2);
                if (ObjectUtil.isNull(turnIpData)) {
                    log.error("用户：{}，解析语音通话turnToken或stunIp失败", username);
                    return;
                }
                // 发送STUN
                ProtocolTreeNode stunCall = new ProtocolTreeNode("call");
                stunCall.AddAttribute(new StanzaAttribute("to", to));
                stunCall.AddAttribute(new StanzaAttribute("id", IdUtil.simpleUUID().toUpperCase()));
                ProtocolTreeNode relayLatency = new ProtocolTreeNode("relaylatency");
                relayLatency.AddAttribute(new StanzaAttribute("call-creator", StringUtil.ParseJid(userId).toString()));
                relayLatency.AddAttribute(new StanzaAttribute("call-id", callId));
                ProtocolTreeNode te = new ProtocolTreeNode("te");
                te.AddAttribute(new StanzaAttribute("latency", "33554" + RandomUtil.randomInt(500, 700)));
                te.SetData(turnIpData.getIpv4Addr());
                relayLatency.AddChild(te);
                /*ProtocolTreeNode teIpv6 = new ProtocolTreeNode("te");
                teIpv6.AddAttribute(new StanzaAttribute("latency", "33554" + RandomUtil.randomInt(500, 700)));
                teIpv6.SetData(stunIpv6);
                relayLatency.AddChild(teIpv6);*/
                stunCall.AddChild(relayLatency);
                AddTask(stunCall);
                String redisKey = CacheConstants.VOIP_CALL_MASTER_KEY + username + ":" + callId;
                //master key放入缓存
                String masterKey = Convert.toStr(RedisService.getInstance().get(redisKey));
                if (!StringUtils.hasLength(masterKey)) {
                    log.warn("用户: {} ,callId: {}, masterKey未获取成功", username, callId);
                    return;
                }
                Pair<String, Integer> ipv4Pair = IPUtil.IpParser(turnIpData.getIpv4Addr());
                String serverIp = ipv4Pair.getKey();
                int ipv4Port = ipv4Pair.getValue();
                Pair<String, Integer> ipv6Pair = IPUtil.IpParser(turnIpData.getIpv6Addr());
                String ipv6ServerIp = ipv6Pair.getKey();
                int ipv6Port = ipv6Pair.getValue();
                log.debug("用户: {}, callId: {}, 接收到TURN服务器包, ip: {}, tokenId: {}, key: {}", username, callId, serverIp, turnIpData.getTurnToken(), key);
                StringBuilder voipCommand = new StringBuilder();
                voipCommand.append("arch -x86_64 ")
                        .append("wavoce")
                        .append(" --wa_server=").append(serverIp)
                        .append(" --wa_port=").append(ipv4Port)
                        .append(" --listen_port=").append("4002")
                        .append(" --sender_jid=").append(userId)
                        .append(" --call_id=").append(callId)
                        .append(" --token=").append(turnIpData.getTurnToken())
                        .append(" --password=").append(key)
                        .append(" --master_key=").append(masterKey)
                        .append(" --receive_jid=").append(to)
                        /*.append(" --socks5_server=").append("us1.ip007.cc")
                        .append(" --socks5_port=").append("32133")
                        .append(" --socks5_username=").append("42fd104d4cb")
                        .append(" --socks5_password=").append("55d65630ac7")*/
                        .append(" --native_record=").append("1");
                String command = voipCommand.toString();
                log.debug("[VOIP Commands]\n");
                log.debug(command + "\n");
                VoipAcceptResult voipAcceptResult = VoipAcceptResult.builder()
                        .masterKey(masterKey)
                        .senderJid(userId)
                        .receiveJid(to)
                        .callId(callId)
                        .serverIp(serverIp)
                        .port(String.valueOf(ipv4Port))
                        .ipv6ServerIp(ipv6ServerIp)
                        .ipv6Port(String.valueOf(ipv6Port))
                        .token(turnIpData.getTurnToken())
                        .password(key)
                        //.command(command)
                        .build();
                redisKey = CacheConstants.VOIP_ACCEPT_RESULT_KEY + username + ":" + callId;
                RedisService.getInstance().set(redisKey, voipAcceptResult, 60L);
                long msgTime = System.currentTimeMillis();
                boolean offlineMessage = WhatsAppUtils.checkIsOfflineMessage(loginTime, msgTime);
                AsyncMessageService.asyncMessagePushService.voipCallCreate(to, userId, callId, username, msgTime, offlineMessage, voipAcceptResult);
            }
        }
    }

    void HandleNotification(ProtocolTreeNode node) {
        String type = node.GetAttributeValue("type");
        ProtocolTreeNode ack = new ProtocolTreeNode("ack");
        ack.AddAttribute(new StanzaAttribute("id", node.GetAttributeValue("id")));
        ack.AddAttribute(new StanzaAttribute("class", "notification"));
        ack.AddAttribute(new StanzaAttribute("to", node.GetAttributeValue("from")));
        if (!StringUtil.isEmpty(type)) {
            ack.AddAttribute(new StanzaAttribute("type", type));
        }
        String participant = node.GetAttributeValue("participant");
        if (!StringUtil.isEmpty(participant)) {
            ack.AddAttribute(new StanzaAttribute("participant", participant));
        }
        AddTask(ack);
        if (StringUtils.hasLength(type) && NOTIFICATION_TYPE.add(type)) {
            log.info("Notification type: {}", type);
        }
        if (type.equals("encrypt")) {
            ProtocolTreeNode child = node.GetChild("count");
            if (child != null) {
                try {
                    KeyLockUtil.lock(username);
                    axolotlManager_.LevelPreKeys(true);
                    LinkedList<PreKeyRecord> unsentPreKeys = axolotlManager_.LoadUnSendPreKey();
                    FlushKeys(axolotlManager_.LoadLatestSignedPreKey(false), unsentPreKeys);
                } catch (Exception ignore) {
                } finally {
                    KeyLockUtil.unlock(username);
                }
            }
        } else if (type.equals("devices")) {
            //处理多设备
            String from = node.GetAttributeValue("from");
            try {
                XmppJid jidInfo = XmppJid.of(from);
                if (ObjectUtil.isNotNull(jidInfo) && StrUtil.isNotEmpty(jidInfo.getUser()) && !jidInfo.getUser().equals(envBuilder_.getFullphone())) {
                    // 需要判断多设备的类型, 添加或者删除
                    // 先判断session中是否有包含主设备
                    int tempNum = 0;
                    try {
                        KeyLockUtil.lock(username);
                        tempNum = axolotlManager_.containsSessionNum(Collections.singletonList(jidInfo.getUser()));
                    } catch (Exception ignore) {
                    } finally {
                        KeyLockUtil.unlock(username);
                    }
                    if (tempNum == 0) {
                        // 原session中没有该用户, 则直接获取多设备
                        HandleGetDevices(from, null);
                    } else {
                        LinkedList<ProtocolTreeNode> add = node.GetChildren("add");
                        LinkedList<ProtocolTreeNode> remove = node.GetChildren("remove");
                        if (!add.isEmpty()) {
                            List<String> addList = new ArrayList<>();
                            // 新增多设备信息
                            // <notification from='918373878251@s.whatsapp.net' type='devices' id='1693426161' lid='273512610721984@lid' t='1715159392'><add device_hash='2:7n0BB/lR' device_lid_hash='2:j6zgHSHh'><device lid='273512610721984.1:9@lid' jid='918373878251.0:9@s.whatsapp.net' key-index='5'/><key-index-list ts='1715159390'></key-index-list></add></notification>
                            for (ProtocolTreeNode addNode : add) {
                                ProtocolTreeNode deviceNode = addNode.getOneChildren("device");
                                String jid = deviceNode.GetAttributeValue("jid");
                                if (StrUtil.isNotEmpty(jid)) {
                                    addList.add(jid);
                                }
                            }
                            // 获取多设备信息即可
                            GetKeysFor(addList, null);
                        }
                        if (!remove.isEmpty()) {
                            for (ProtocolTreeNode removeNode : remove) {
                                ProtocolTreeNode deviceNode = removeNode.getOneChildren("device");
                                String jid = deviceNode.GetAttributeValue("jid");
                                if (StrUtil.isNotEmpty(jid)) {
                                    XmppJid removeJid = XmppJid.of(jid);
                                    if (ObjectUtil.isNotNull(removeJid)) {
                                        try {
                                            KeyLockUtil.lock(username);
                                            axolotlManager_.DeleteSession(removeJid.getUser(), removeJid.getDevice());
                                        } catch (Exception ignore) {
                                        } finally {
                                            KeyLockUtil.unlock(username);
                                        }
                                    }
                                }
                            }
                        }
                    }

                }
            } catch (Exception e) {
                log.error("用户：{}，粉丝：{}, 多设备解析xmppJid异常", username, from);
            }
        } else if (type.equals("account_sync")) {
            HandleSyncAccount(node);
        } else if ("registration".equals(type)) {
            if (ObjectUtil.isNotNull(node.getOneChildren("wa_old_registration"))) {
                //处理注册验证码
                ProtocolTreeNode waOldRegistrationNode = node.getOneChildren("wa_old_registration");
                String verifyCode = waOldRegistrationNode.GetAttributeValue("code");
                log.info("用户: {}, 收到注册验证码: {}", username, verifyCode);
                if (StringUtils.hasLength(verifyCode)) {
                    WaOldRegistrationService waOldRegistrationService = SpringUtils.getBean(WaOldRegistrationService.class);
                    waOldRegistrationService.saveVerifyCode(username, verifyCode);
                }
            }
            if (ObjectUtil.isNotNull(node.getOneChildren("device_logout"))) {
                log.info("用户: {}, 收到是否允许设备转移通知: {}", username, node);
                ProtocolTreeNode deviceLogout = node.getOneChildren("device_logout");
                String id = deviceLogout.GetAttributeValue("id");
                if (StringUtils.hasLength(id)) {
                    ApproveDeviceLogout(id, true);
                }
            }

        } else if ("mex".equals(type)) {
            ProtocolTreeNode updateNode = node.getOneChildren("update");
            if (ObjectUtil.isNotNull(updateNode) && "NotificationUserReachoutTimelockUpdate".equals(updateNode.GetAttributeValue("op_name"))) {
                String userTimeLock = new String(updateNode.GetData(), StandardCharsets.UTF_8);
                JSONObject jsonObject = JSONObject.parseObject(userTimeLock);
                JSONObject notifyObject = Optional.ofNullable(jsonObject)
                        .map(obj -> obj.getJSONObject("data"))
                        .map(data -> data.getJSONObject("xwa2_notify_account_reachout_timelock"))
                        .orElse(null);
                Optional.ofNullable(notifyObject)
                        .map(obj -> obj.getString("time_enforcement_ends"))
                        .filter(StringUtils::hasLength)
                        .ifPresent(time -> {
                            timeLockEndTime = time;
                            axolotlManager_.setTimeLockEndTime(time);
                        });
            }
        }
        if (null != delegate_) {
            delegate_.OnSync(node);
        }
    }

    private void executeScanWeb(String syncType) {
        AtomicInteger inc = scanWebSyncStatus.getInc();
        int type = inc.get();
        if (type == 1 && "hist_sync".equals(syncType)) {
            inc.incrementAndGet();
            log.info("用户：{}，扫码步骤1执行完毕", username);
            SendTwoPacket();
        } else if (type == 2) {
            inc.incrementAndGet();
            log.info("用户：{}，扫码步骤2执行完毕", username);
            SendThreePacket();
        } else if (type == 3) {
            inc.incrementAndGet();
            log.info("用户：{}，扫码步骤3执行完毕", username);
            ProtocolTreeNode chatState = new ProtocolTreeNode("iq");
            chatState.AddAttribute(new StanzaAttribute("id", GenerateIqId()));
            chatState.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
            chatState.AddAttribute(new StanzaAttribute("xmlns", "privacy"));
            chatState.AddAttribute(new StanzaAttribute("type", "set"));
            ProtocolTreeNode tokens = new ProtocolTreeNode("tokens");
            ProtocolTreeNode token = new ProtocolTreeNode("token");
            token.AddAttribute(new StanzaAttribute("jid", JidNormalize(envBuilder_.getFullphone())));
            token.AddAttribute(new StanzaAttribute("type", "trusted_contact"));
            token.AddAttribute(new StanzaAttribute("t", String.valueOf(System.currentTimeMillis() / 1000)));
            tokens.AddChild(token);
            chatState.AddChild(tokens);
            AddTask(chatState, (srcNode1, result1) -> {
                SendFourPacket();
            });
        } else if (type == 4 && "hist_sync".equals(syncType)) {
            inc.incrementAndGet();
            log.info("用户：{}，扫码步骤4执行完毕", username);
            SendFivePacket();
        } else if (type == 5 && "hist_sync".equals(syncType)) {
            inc.incrementAndGet();
            log.info("用户：{}，扫码步骤5执行完毕", username);
            SendSixPacket();
        } else if (type == 6 && "hist_sync".equals(syncType)) {
            log.info("用户：{}，扫码步骤6执行完毕", username);
            PresenceWeb();
            log.info("用户：{}，扫码步骤7执行完毕", username);
            if (!axolotlManager_.isScanWeb()) {
                String pushName = envBuilder_.getPushname();
                Send8Packet("en_US", pushName);
                axolotlManager_.markCompletedScanWeb();
                log.info("用户：{}，扫码步骤8执行完毕", username);
            } else {
                log.info("用户：{}，已扫码过，不做步骤8", username);
            }
            scanWebSyncStatus.setEnable(false);
            //恢复为初始值
            inc.set(1);
        }
    }


    private void firstSendMsg(String from, NodeCallback callback) {
        String phone = "+" + StrUtil.subBefore(from, "@", false);
        ProtocolTreeNode iq = urlSearchContactNode(phone, GenerateIqId());
        AddTask(iq, (srcNode, result) -> HandleGetDevices(from, callback));
    }

    private void handlePreKey(String from, NodeCallback callback) {
        String jid = JidNormalize(from);
        StringUtil.JidInfo jidInfo = StringUtil.ParseJid(jid);
        String s = jidInfo.toString();
        ArrayList<String> list = new ArrayList<>();
        list.add(s);
        GetKeysFor(list, callback);
    }

    /**
     * 处理多设备
     */
    private void HandleGetDevices(String from, NodeCallback callback) {
        if (callback != null) {
            executeUserIdSubscribeInternal(from);
        }
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("id", GenerateIqId()));
        iq.AddAttribute(new StanzaAttribute("xmlns", "usync"));
        iq.AddAttribute(new StanzaAttribute("type", "get"));

        ProtocolTreeNode usync = new ProtocolTreeNode("usync");
        usync.AddAttribute(new StanzaAttribute("index", "0"));
        usync.AddAttribute(new StanzaAttribute("last", "true"));
        usync.AddAttribute(new StanzaAttribute("mode", "query"));
        usync.AddAttribute(new StanzaAttribute("context", "background"));
        if (iosLogin) {
            usync.AddAttribute(new StanzaAttribute("sid", System.currentTimeMillis() / 1000 + "-" + new Random().nextInt(1000000000) + "-" + this.getSyncId().incrementAndGet()));

        } else {
            usync.AddAttribute(new StanzaAttribute("sid", "sync_sid_multi_protocols_" + UUID.randomUUID()));
        }
        // query 子节点
        ProtocolTreeNode query = new ProtocolTreeNode("query");
        ProtocolTreeNode devices = new ProtocolTreeNode("devices");
        devices.AddAttribute(new StanzaAttribute("version", "2"));
        query.AddChild(devices);

        usync.AddChild(query);
        //列表
        ProtocolTreeNode list = new ProtocolTreeNode("list");
        ProtocolTreeNode user = new ProtocolTreeNode("user");
        user.AddAttribute(new StanzaAttribute("jid", from));

        ProtocolTreeNode userDevices = new ProtocolTreeNode("devices");
        userDevices.AddAttribute(new StanzaAttribute("ts", String.valueOf(System.currentTimeMillis() / 1000)));
        user.AddChild(userDevices);
        list.AddChild(user);

        usync.AddChild(list);
        iq.AddChild(usync);
        AddTask(iq, (srcNode, result) -> HandleGetDeviceResult(srcNode, result, callback));
    }

    /**
     * 生成获取多设备信息节点
     */
    public ProtocolTreeNode generateUserDeviceInfo(String taskId, List<String> userList) {
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("id", taskId));
        iq.AddAttribute(new StanzaAttribute("xmlns", "usync"));
        iq.AddAttribute(new StanzaAttribute("type", "get"));

        ProtocolTreeNode usync = new ProtocolTreeNode("usync");
        usync.AddAttribute(new StanzaAttribute("index", "0"));
        usync.AddAttribute(new StanzaAttribute("last", "true"));
        usync.AddAttribute(new StanzaAttribute("mode", "query"));
        usync.AddAttribute(new StanzaAttribute("context", "background"));
        if (iosLogin) {
            usync.AddAttribute(new StanzaAttribute("sid", System.currentTimeMillis() / 1000 + "-" + new Random().nextInt(1000000000) + "-" + this.getSyncId().incrementAndGet()));

        } else {
            usync.AddAttribute(new StanzaAttribute("sid", "sync_sid_multi_protocols_" + UUID.randomUUID()));
        }
        // query 子节点
        ProtocolTreeNode query = new ProtocolTreeNode("query");
        ProtocolTreeNode devices = new ProtocolTreeNode("devices");
        devices.AddAttribute(new StanzaAttribute("version", "2"));
        query.AddChild(devices);

        usync.AddChild(query);
        //列表
        ProtocolTreeNode list = new ProtocolTreeNode("list");
        for (String from : userList) {
            ProtocolTreeNode user = new ProtocolTreeNode("user");
            user.AddAttribute(new StanzaAttribute("jid", from));

            ProtocolTreeNode userDevices = new ProtocolTreeNode("devices");
            userDevices.AddAttribute(new StanzaAttribute("ts", String.valueOf(System.currentTimeMillis() / 1000)));
            user.AddChild(userDevices);
            list.AddChild(user);
        }
        usync.AddChild(list);
        iq.AddChild(usync);
        return iq;
    }

    //<iq from='8613590316774@s.whatsapp.net' type='result' id='03'>
    //    <usync sid='sync_sid_devices_a0ebb69f-c4d7-4380-8e25-503ea6e546a3' index='0' last='true' mode='query' context='notification'>
    //        <result>
    //            <devices/>
    //        </result>
    //        <list>
    //            <user jid='8617748754950@s.whatsapp.net'>
    //                <devices>
    //                    <device-list>
    //                        <device id='0'/>
    //                        <device id='1' key-index='1'/>
    //                    </device-list>
    //                    <key-index-list ts='1637898004'>ChII2aiG4AUQlK6BjQYYASICAAESQBVSmiGsdyYZviPjrzYqKXAs24TOteQA7CRwck231FLy3Tfo4onuxaYaW7FU0LYXUhVqjE8ajwNyX1lC2ez4bA8=</key-index-list>
    //                </devices>
    //            </user>
    //        </list>
    //    </usync>
    //</iq>
    void HandleGetDeviceResult(ProtocolTreeNode srcNode, ProtocolTreeNode result, NodeCallback callback) {
        ProtocolTreeNode usyncNode = result.getOneChildren("usync");
        if (null == usyncNode) {
            if (callback != null) {
                callback.Run(null, null);
            }
            return;
        }
        ProtocolTreeNode listNode = usyncNode.getOneChildren("list");
        if (null == listNode) {
            if (callback != null) {
                callback.Run(null, null);
            }
            return;
        }
        LinkedList<ProtocolTreeNode> userNodes = listNode.GetChildren("user");
        if (null == userNodes || userNodes.size() == 0) {
            if (callback != null) {
                callback.Run(null, null);
            }
            return;
        }
        List<String> needQueryJids = new ArrayList<>();
        for (ProtocolTreeNode userNode : userNodes) {
            ProtocolTreeNode devicesNode = userNode.getOneChildren("devices");
            if (null == devicesNode) {
                if (callback != null) {
                    callback.Run(null, null);
                }
                continue;
            }

            ProtocolTreeNode device_listNode = devicesNode.getOneChildren("device-list");
            if (null == device_listNode) {
                if (callback != null) {
                    callback.Run(null, null);
                }
                continue;
            }
            StringUtil.JidInfo jidInfo = StringUtil.ParseJid(userNode.GetAttributeValue("jid"));
            LinkedList<ProtocolTreeNode> devices = device_listNode.GetChildren();
            if (devices == null) {
                if (callback != null) {
                    callback.Run(null, null);
                }
                continue;
            }
            for (ProtocolTreeNode device : devices) {
                String id = device.GetAttributeValue("id");
                jidInfo.deviceId = Integer.parseInt(id);
                try {
                    KeyLockUtil.lock(username);
                    if (!axolotlManager_.ContainsSession(jidInfo)) {
                        //获取session
                        needQueryJids.add(jidInfo.toString());
                    }
                } catch (Exception ignore) {
                } finally {
                    KeyLockUtil.unlock(username);
                }
            }
        }
        if (needQueryJids.size() == 0) {
            if (callback != null) {
                callback.Run(null, null);
            }
            log.info("用户：{}，查询粉丝多设备，query list is null", username);
            return;
        }
        GetKeysFor(needQueryJids, callback);
    }

    public List<String> parseDeviceResult(ProtocolTreeNode result) {
        ProtocolTreeNode usyncNode = result.getOneChildren("usync");
        if (null == usyncNode) {
            return null;
        }
        ProtocolTreeNode listNode = usyncNode.getOneChildren("list");
        if (null == listNode) {
            return null;
        }
        LinkedList<ProtocolTreeNode> usersNode = listNode.GetChildren("user");
        if (null == usersNode) {
            return null;
        }
        List<String> userList = new ArrayList<>();
        for (ProtocolTreeNode userNode : usersNode) {
            ProtocolTreeNode devicesNode = userNode.getOneChildren("devices");
            if (null == devicesNode) {
                continue;
            }
            ProtocolTreeNode device_listNode = devicesNode.getOneChildren("device-list");
            if (null == device_listNode) {
                continue;
            }
            StringUtil.JidInfo jidInfo = StringUtil.ParseJid(userNode.GetAttributeValue("jid"));
            LinkedList<ProtocolTreeNode> devices = device_listNode.GetChildren("device");
            if (devices == null) {
                jidInfo.deviceId = 0;
                userList.add(jidInfo.toString());
                continue;
            }
            for (ProtocolTreeNode device : devices) {
                String id = device.GetAttributeValue("id");
                jidInfo.deviceId = Integer.parseInt(id);
                userList.add(jidInfo.toString());
            }
        }
        return userList;
    }


    class HandleResult implements NodeCallback {
        String type_;

        HandleResult(String type) {
            type_ = type;
        }

        @Override
        public void Run(ProtocolTreeNode srcNode, ProtocolTreeNode result) {
            if (null != delegate_) {
                delegate_.OnPacketResponse(type_, result);
            }
        }
    }

    /**
     * 创建群
     *
     * @param subjectName
     * @param members
     * @return
     */
    public String CreateGroup(String subjectName, List<String> members) {
        ProtocolTreeNode node = new ProtocolTreeNode("iq");
        node.AddAttribute(new StanzaAttribute("id", GenerateIqId()));
        node.AddAttribute(new StanzaAttribute("xmlns", "w:g2"));
        node.AddAttribute(new StanzaAttribute("type", "set"));
        node.AddAttribute(new StanzaAttribute("to", "g.us"));

        ProtocolTreeNode create = new ProtocolTreeNode("create");
        create.AddAttribute(new StanzaAttribute("subject", subjectName));
        for (String member : members) {
            ProtocolTreeNode participant = new ProtocolTreeNode("participant");
            participant.AddAttribute(new StanzaAttribute("jid", JidNormalize(member)));
            create.AddChild(participant);
        }
        node.AddChild(create);
        return AddTask(node, new HandleResult("CreateGroup"));
    }

    /**
     * 接收群邀请
     *
     * @param token
     * @return
     */
    public String AcceptInviteToGroup(String token) {
        ProtocolTreeNode node = new ProtocolTreeNode("iq");
        node.AddAttribute(new StanzaAttribute("id", GenerateIqId()));
        node.AddAttribute(new StanzaAttribute("xmlns", "w:g2"));
        node.AddAttribute(new StanzaAttribute("type", "set"));
        node.AddAttribute(new StanzaAttribute("to", "g.us"));

        ProtocolTreeNode invite = new ProtocolTreeNode("invite");
        invite.AddAttribute(new StanzaAttribute("code", token));
        node.AddChild(invite);
        return AddTask(node, new HandleResult("AcceptInviteToGroup"));
    }

    /**
     * 发送文字动态
     */
    public String sendMomentText(String taskId, String content, List<String> toFriends) {
        WhatsMessage.WhatsAppMessage.Builder appMessage = WhatsMessage.WhatsAppMessage.newBuilder();
        WhatsMessage.WhatsAppExtendedTextMessage.Builder extendMessage = WhatsMessage.WhatsAppExtendedTextMessage.newBuilder();
        extendMessage.setText(content);
        // 设置字体颜色
        extendMessage.setTextArgb(0xffffffff);
        // 设置背景颜色
        extendMessage.setBackgroundArgb(0xff8b6990);
        extendMessage.setFont(WhatsMessage.WhatsAppFont.SANS_SERIF);
        extendMessage.setPreviewType(WhatsMessage.WhatsAppPreviewType.PreviewType_NONE);
        extendMessage.setInviteLinkGroupTypeV2(WhatsMessage.GroupType.DEFAULT);
        appMessage.setExtendedTextMessage(extendMessage);
        byte[] serialData = appMessage.buildPartial().toByteArray();
        // 每次发送都使用新的senderKey(无需保存)
        SenderKeyRecord senderKeyRecord = axolotlManager_.GroupCreateSenderKeyRecord();
        groupMsgSecondSend("status@broadcast", serialData, "text", "", taskId, toFriends, senderKeyRecord);
        return taskId;
    }

    /**
     * 修改群描述
     */
    public String ModifyGroupDesc(String taskId, String groupId, String preId, String desc) {
        ProtocolTreeNode node = new ProtocolTreeNode("iq");
        node.AddAttribute(new StanzaAttribute("id", taskId));
        node.AddAttribute(new StanzaAttribute("xmlns", "w:g2"));
        node.AddAttribute(new StanzaAttribute("type", "set"));
        node.AddAttribute(new StanzaAttribute("to", JidNormalize(groupId)));

        ProtocolTreeNode description = new ProtocolTreeNode("description");
        if (StringUtils.hasLength(preId)) {
            description.AddAttribute(new StanzaAttribute("prev", preId));
        }
        description.AddAttribute(new StanzaAttribute("id", GenerateIqId()));

        ProtocolTreeNode body = new ProtocolTreeNode("body");
        body.SetData(desc.getBytes(StandardCharsets.UTF_8));
        description.AddChild(body);

        node.AddChild(description);
        return AddTask(node, new HandleResult("ModifyGroupDesc"));
    }

    /**
     * 是否允许设备转移(转环境)
     */
    public void ApproveDeviceLogout(String id, boolean approve) {
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("id", GenerateIqId()));
        iq.AddAttribute(new StanzaAttribute("xmlns", "w:account_defence"));
        iq.AddAttribute(new StanzaAttribute("type", "set"));
        iq.AddAttribute(new StanzaAttribute("smax_id", "87"));
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        ProtocolTreeNode deviceLogout = new ProtocolTreeNode("device_logout");
        deviceLogout.AddAttribute(new StanzaAttribute("approve", String.valueOf(approve)));
        deviceLogout.AddAttribute(new StanzaAttribute("id", id));
        iq.AddChild(deviceLogout);
        AddTask(iq);
    }


    /**
     * 修改群主题
     *
     * @param jid
     * @param subjectName
     * @return
     */
    public String ModifyGroupSubject(String jid, String subjectName) {
        ProtocolTreeNode node = new ProtocolTreeNode("iq");
        node.AddAttribute(new StanzaAttribute("id", GenerateIqId()));
        node.AddAttribute(new StanzaAttribute("xmlns", "w:g2"));
        node.AddAttribute(new StanzaAttribute("type", "set"));
        node.AddAttribute(new StanzaAttribute("to", JidNormalize(jid)));

        ProtocolTreeNode subject = new ProtocolTreeNode("subject");
        subject.SetData(subjectName.getBytes(StandardCharsets.UTF_8));
        node.AddChild(subject);

        return AddTask(node, new HandleResult("ModifyGroupSubject"));
    }

    /**
     * 邀请群成员
     *
     * @param jid
     * @param members
     * @return
     */
    public String InviteGroupMembers(String jid, List<String> members) {
        ProtocolTreeNode node = new ProtocolTreeNode("iq");
        node.AddAttribute(new StanzaAttribute("id", GenerateIqId()));
        node.AddAttribute(new StanzaAttribute("xmlns", "w:g2"));
        node.AddAttribute(new StanzaAttribute("type", "set"));
        node.AddAttribute(new StanzaAttribute("to", JidNormalize(jid)));

        ProtocolTreeNode add = new ProtocolTreeNode("add");
        for (String member : members) {
            ProtocolTreeNode participant = new ProtocolTreeNode("participant");
            participant.AddAttribute(new StanzaAttribute("jid", JidNormalize(member)));
            add.AddChild(participant);
        }
        node.AddChild(add);
        return AddTask(node, new HandleResult("InviteGroupMembers"));
    }

    /**
     * 移除群成员
     *
     * @param jid
     * @param members
     * @return
     */
    public String RemoveGroupMembers(String jid, List<String> members) {
        ProtocolTreeNode node = new ProtocolTreeNode("iq");
        node.AddAttribute(new StanzaAttribute("id", GenerateIqId()));
        node.AddAttribute(new StanzaAttribute("xmlns", "w:g2"));
        node.AddAttribute(new StanzaAttribute("type", "set"));
        node.AddAttribute(new StanzaAttribute("to", JidNormalize(jid)));

        ProtocolTreeNode remove = new ProtocolTreeNode("remove");
        for (String member : members) {
            ProtocolTreeNode participant = new ProtocolTreeNode("participant");
            participant.AddAttribute(new StanzaAttribute("jid", JidNormalize(member)));
            remove.AddChild(participant);
        }
        node.AddChild(remove);
        return AddTask(node, new HandleResult("RemoveGroupMembers"));
    }

    /**
     * 设置管理员
     *
     * @param jid
     * @param members
     * @return
     */
    public String PromoteGroupMember(String jid, List<String> members) {
        ProtocolTreeNode node = new ProtocolTreeNode("iq");
        node.AddAttribute(new StanzaAttribute("id", GenerateIqId()));
        node.AddAttribute(new StanzaAttribute("xmlns", "w:g2"));
        node.AddAttribute(new StanzaAttribute("type", "set"));
        node.AddAttribute(new StanzaAttribute("to", JidNormalize(jid)));

        ProtocolTreeNode remove = new ProtocolTreeNode("promote");
        for (String member : members) {
            ProtocolTreeNode participant = new ProtocolTreeNode("participant");
            participant.AddAttribute(new StanzaAttribute("jid", JidNormalize(member)));
            remove.AddChild(participant);
        }
        node.AddChild(remove);
        return AddTask(node, new HandleResult("PromoteGroupMember"));
    }

    /**
     * 取消群管理员
     *
     * @param jid
     * @param members
     * @return
     */
    public String DemoteGroupMember(String jid, List<String> members) {
        ProtocolTreeNode node = new ProtocolTreeNode("iq");
        node.AddAttribute(new StanzaAttribute("id", GenerateIqId()));
        node.AddAttribute(new StanzaAttribute("xmlns", "w:g2"));
        node.AddAttribute(new StanzaAttribute("type", "set"));
        node.AddAttribute(new StanzaAttribute("to", JidNormalize(jid)));

        ProtocolTreeNode demote = new ProtocolTreeNode("demote");
        for (String member : members) {
            ProtocolTreeNode participant = new ProtocolTreeNode("participant");
            participant.AddAttribute(new StanzaAttribute("jid", JidNormalize(member)));
            demote.AddChild(participant);
        }
        node.AddChild(demote);
        return AddTask(node, new HandleResult("DemoteGroupMember"));
    }

    /**
     * 离开群组
     *
     * @param groupJids
     * @return
     */
    public String LeaveGroup(List<String> groupJids) {
        ProtocolTreeNode node = new ProtocolTreeNode("iq");
        node.AddAttribute(new StanzaAttribute("id", GenerateIqId()));
        node.AddAttribute(new StanzaAttribute("xmlns", "w:g2"));
        node.AddAttribute(new StanzaAttribute("type", "set"));
        node.AddAttribute(new StanzaAttribute("to", "g.us"));

        ProtocolTreeNode leave = new ProtocolTreeNode("leave");
        for (String groupJid : groupJids) {
            ProtocolTreeNode group = new ProtocolTreeNode("group");
            group.AddAttribute(new StanzaAttribute("id", JidNormalize(groupJid)));
            leave.AddChild(group);
        }
        node.AddChild(leave);
        return AddTask(node, new HandleResult("LeaveGroup"));
    }

    /**
     * 获取群组信息
     *
     * @param jid
     * @return
     */
    String getGroupInfo(String jid) {
        return InnerGetGroupInfo(jid, new HandleResult("GetGroupInfo"));
    }

    public static byte[] AdjustId(int input, int length) {
        byte[] result = new byte[length];
        for (int i = length - 1; i >= 0; i--) {
            result[i] = (byte) (255 & input);
            input >>>= 8;
        }

        return result;
    }

    public int DeAdjustId(byte[] bytes, int length) {
        int result = 0;
        for (int i = 0; i < length; i++) {
            result = 256 * result + Byte.toUnsignedInt(bytes[i]);
        }

        return result;
    }


    void FlushKeys(SignedPreKeyRecord signedPreKeyRecord, List<PreKeyRecord> unsentPreKeys) {
        StanzaAttribute[] attributes = new StanzaAttribute[4];
        attributes[0] = new StanzaAttribute("id", GenerateIqId());
        attributes[3] = new StanzaAttribute("to", "s.whatsapp.net");
        attributes[2] = new StanzaAttribute("type", "set");
        attributes[1] = new StanzaAttribute("xmlns", "encrypt");
        ProtocolTreeNode iqNode = new ProtocolTreeNode("iq", attributes);
        {
            //identify
            ProtocolTreeNode identity = new ProtocolTreeNode("identity");
            try {
                KeyLockUtil.lock(username);
                identity.SetData(axolotlManager_.GetIdentityKeyPair().getPublicKey().serialize(), 1, 32);
            } catch (Exception e) {
                log.error("用户：{}，identity异常", username, e);
            } finally {
                KeyLockUtil.unlock(username);
            }

            iqNode.AddChild(identity);
        }

        {
            //registration
            ProtocolTreeNode registration = new ProtocolTreeNode("registration");
            try {
                KeyLockUtil.lock(username);
                int localRegistrationId = axolotlManager_.getLocalRegistrationId();
                registration.SetData(AdjustId(localRegistrationId, 4));
            } catch (Exception ignore) {
            } finally {
                KeyLockUtil.unlock(username);
            }
            iqNode.AddChild(registration);
        }

        {
            //type
            ProtocolTreeNode type = new ProtocolTreeNode("type");
            type.SetData(new byte[]{5});
            iqNode.AddChild(type);
        }
        {
            //signature  key skey
            ProtocolTreeNode skey = new ProtocolTreeNode("skey");
            {
                ProtocolTreeNode id = new ProtocolTreeNode("id");
                id.SetData(AdjustId(signedPreKeyRecord.getId(), 3));
                skey.AddChild(id);
            }
            {
                ProtocolTreeNode value = new ProtocolTreeNode("value");
                value.SetData(signedPreKeyRecord.getKeyPair().getPublicKey().serialize(), 1, 32);
                skey.AddChild(value);
            }

            {
                ProtocolTreeNode signature = new ProtocolTreeNode("signature");
                signature.SetData(signedPreKeyRecord.getSignature());
                skey.AddChild(signature);
            }
            iqNode.AddChild(skey);
        }
        {
            // prekey 节点
            ProtocolTreeNode list = new ProtocolTreeNode("list");
            LinkedList<Integer> sentPrekeyIds = new LinkedList<>();
            for (int i = 0; i < unsentPreKeys.size(); i++) {
                PreKeyRecord record = unsentPreKeys.get(i);
                sentPrekeyIds.add(record.getId());
                ProtocolTreeNode key = new ProtocolTreeNode("key");
                {
                    ProtocolTreeNode idNode = new ProtocolTreeNode("id");
                    idNode.SetData(AdjustId(record.getId(), 3));
                    key.AddChild(idNode);
                }
                {
                    ProtocolTreeNode value = new ProtocolTreeNode("value");
                    value.SetData(record.getKeyPair().getPublicKey().serialize(), 1, 32);
                    key.AddChild(value);
                }
                list.AddChild(key);
            }
            iqNode.AddChild(list);
            iqNode.SetCustomParams(sentPrekeyIds);
        }

        AddTask(iqNode, (srcNode, result) -> HandleFlushKey(srcNode, result));
    }

    public String sendMsgTask(ProtocolTreeNode node, NodeCallback callback) {
        return noiseHandshake_.SendNode(node, registerHandleMap_, NodeTaskType.DEFAULT, () -> {
            if (callback != null) {
                String lastSendMsgTime = DateUtil.now();
                String iqId = node.IqId();
                Runnable retryTask = () -> {
                    if (registerHandleMap_.containsKey(iqId)) {
                        if (noiseHandshake_ != null && gcmOnline.get()) {
                            log.info("用户: {}, 发送重试消息，上次发送消息时间: {}, 上次pn时间: {}", username, lastSendMsgTime, lastPnTime);
                            registerHandleMap_.remove(iqId);
                            // 重试发送消息
                            AddTask(node, callback);
                        }
                    }
                };
                // 第一次超时的定时器
                // 超时时间不能太短，太短消息wa不会转发
                Timeout timeout = hashedWheelTimer.newTimeout(timeout1 -> retryTask.run(), 15, TimeUnit.SECONDS);
                registerHandleMap_.put(iqId, new NodeHandleInfo(callback, node, timeout));
            }
        });
    }


    public String AddTask(ProtocolTreeNode node, NodeCallback callback) {
        return noiseHandshake_.SendNode(node, registerHandleMap_, NodeTaskType.DEFAULT, () -> {
            if (null != callback) {
                String iqId = node.IqId();
                Timeout timeout = hashedWheelTimer.newTimeout(timeout1 -> {
                    registerHandleMap_.remove(iqId);
                }, 60, TimeUnit.SECONDS);
                registerHandleMap_.put(iqId, new NodeHandleInfo(callback, node, timeout));
            }
        });
    }

    /**
     * 添加任务
     *
     * @param node     节点
     * @param taskType 任务类型
     * @return 任务id
     */
    public String AddTask(ProtocolTreeNode node, NodeTaskType taskType) {
        return noiseHandshake_.SendNode(node, registerHandleMap_, taskType);
    }

    public String AddTask(String type, ProtocolTreeNode node) {
        return noiseHandshake_.SendNode(node, registerHandleMap_, NodeTaskType.DEFAULT, () -> {
            if (!StringUtil.isEmpty(type)) {
                String iqId = node.IqId();
                Timeout timeout = hashedWheelTimer.newTimeout(timeout1 -> {
                    registerHandleMap_.remove(iqId);
                }, 60, TimeUnit.SECONDS);
                registerHandleMap_.put(node.IqId(), new NodeHandleInfo(new HandleResult(type), node, timeout));
            }
        });
    }

    public String AddTask(ProtocolTreeNode node) {
        return noiseHandshake_.SendNode(node, registerHandleMap_);
    }

    boolean HandleRegisterNode(ProtocolTreeNode node) {
        if (node == null) {
            return false;
        }
        String iqId = node.IqId();
        if (!StringUtils.hasLength(iqId)) {
            return false;
        }
        NodeHandleInfo handleInfo = registerHandleMap_.remove(iqId);
        if (handleInfo == null) {
            return false;
        }
        if (ObjectUtil.isNotNull(handleInfo.timeout)) {
            handleInfo.timeout.cancel();
        }
        if (handleInfo.callbackRunnable == null) {
            return false;
        }
        handleInfo.callbackRunnable.Run(handleInfo.srcNode, node);
        return true;
    }

    ConcurrentHashMap<String, NodeHandleInfo> registerHandleMap_ = new ConcurrentHashMap<>();
    ConcurrentHashMap<String, Integer> retries_ = new ConcurrentHashMap<>();

    void HandleFlushKey(ProtocolTreeNode srcNode, ProtocolTreeNode result) {
        StanzaAttribute type = result.GetAttribute("type");
        if ((type != null) && (type.value_.equals("result"))) {
            //更新db
            LinkedList<Integer> sentPrekeyIds = (LinkedList<Integer>) srcNode.GetCustomParams();
            try {
                KeyLockUtil.lock(username);
                axolotlManager_.SetAsSent(sentPrekeyIds);
            } catch (Exception ignore) {
            } finally {
                KeyLockUtil.unlock(username);
            }
            log.info("用户：{}，上传预密钥成功", username);
        } else {
            //ThreadPoolConfig.gcmAndGooglePoolExecutor.execute(()->callGcmAndGoogle(username));
            log.error("用户：{}，上传密钥处理失败：{},正在进行修复", username, result);
            // 若上传失败, 则删除下原来的gcm-token, 避免旧token已经失效, 并且无法接收到pn, 导致一直无法修复
            try {
                KeyLockUtil.lock(username);
                axolotlManager_.delPreKeysSent();
            } catch (Exception ignore) {
            } finally {
                KeyLockUtil.unlock(username);
            }
        }
    }

/*    public void callGcmAndGoogle(String userName){
        log.info("Call Gcm repair userName:{}",userName);
        RedisService redisService = SpringUtils.getBean(RedisService.class);
        String key = String.format(Constant.USERNAME_HAS_RETRY_KEY,userName);
        if (!redisService.hasKey(key)) {
            for (int i = 0; i < 3; i++) {
                GcmTokenResult gcmToken = getGcmToken();
                if (gcmToken == null) {
                    OnGcmClose("token获取失败", false);
                    if (i==2){
                        redisService.set(key,0,Long.parseLong(redisService.get2(Constant.USERNAME_TIME_KEY)==null
                                ?"600":redisService.get2(Constant.USERNAME_TIME_KEY).toString()));
                    }
                    continue;
                }
                gcmLogin = new GCMLogin(proxy_, this);
                String gcmPersistentIdValue = "";
                try {
                    KeyLockUtil.lock(username);
                    gcmPersistentIdValue = axolotlManager_.getGcmPersistentcallGcmAndGoogleIdValue();
                } catch (Exception ignore) {
                    log.error("get Gcm Persistent Id Exception:{}",ignore.getMessage());
                } finally {
                    KeyLockUtil.unlock(username);
                }
                gcmLogin.StartListen(gcmToken.getParams(), gcmToken.getAndroidId(), gcmToken.getSecurityToken(), this, gcmPersistentIdValue);
            }
        }
    }*/

    public String JidNormalize(String jid) {
        int pos = jid.indexOf("@");
        if (pos != -1) {
            return jid;
        }
        return jid + "@s.whatsapp.net";
    }


    void SaveSentMessage(String jid, byte[] serialData, String messageType, String mediaType, String iqId) {
        MessageInfo info = new MessageInfo();
        info.jid = jid;
        info.serialData = cn.hutool.core.codec.Base64.encode(serialData);
        info.messageType = messageType;
        info.mediaType = mediaType;
        String content = JSONObject.toJSONString(info);
        String msgPath = System.getProperty("user.dir") + "/out/groupMsg/" + username + "_" + iqId;
        FileUtil.writeBytes(content.getBytes(StandardCharsets.UTF_8), msgPath);
        //数据库存储消息索引记录
        try {
            String host = WebSocketClient.terminalIp + ":" + StartedUpRunner.tomcatPort;
            KeyLockUtil.lock(username);
            axolotlManager_.chatHistoryStore.insert(username, iqId, host, 2);
        } catch (Exception ignored) {
        } finally {
            KeyLockUtil.unlock(username);
        }
    }

    public void SaveFakeSessionMessage(String jid, byte[] serialData, String messageType, String mediaType, String iqId) {
        MessageInfo info = new MessageInfo();
        info.jid = JidNormalize(jid);
        info.serialData = cn.hutool.core.codec.Base64.encode(serialData);
        info.messageType = messageType;
        info.mediaType = mediaType;
        String content = JSONObject.toJSONString(info);
        String msgPath = System.getProperty("user.dir") + "/out/groupMsg/" + username + "_" + iqId;
        FileUtil.writeBytes(content.getBytes(StandardCharsets.UTF_8), msgPath);
        //数据库存储消息索引记录
        try {
            String host = WebSocketClient.terminalIp + ":" + StartedUpRunner.tomcatPort;
            KeyLockUtil.lock(username);
            axolotlManager_.chatHistoryStore.insert(jid, iqId, host, 2);
        } catch (Exception ignored) {
        } finally {
            KeyLockUtil.unlock(username);
        }
    }

    public String SendSerialData(String jid, byte[] serialData, String messageType, String mediaType, String iqId) {
        if (StringUtil.isEmpty(iqId)) {
            iqId = GenerateMessageId();
        }
        jid = JidNormalize(jid);
        SaveSentMessage(jid, serialData, messageType, mediaType, iqId);

        StringUtil.JidInfo jidInfo = StringUtil.ParseJid(jid);

        if (StringUtil.IsGroupJid(jid)) {
            return SendToGroup(jid, serialData, messageType, mediaType, iqId);
        }
        int containsSessionNum = 0;
        try {
            KeyLockUtil.lock(username);
            containsSessionNum = axolotlManager_.containsSessionNum(jidInfo);
        } catch (Exception ignore) {
        } finally {
            KeyLockUtil.unlock(username);
        }
        if (containsSessionNum > 1) {
            executeUserIdSubscribeInternal(jid);
            //大于1则代表获取过多设备，则直接发，否则获取一遍多设备
            return SendToContact(jid, serialData, messageType, mediaType, iqId);
        } else {
            String fansKey = Constant.WA_SEND_RECORD + DigestUtil.md5Hex(username + jid);
            RedisService redisService = SpringUtils.getBean(RedisService.class);
            if (redisService == null) {
                taskNotify.setEventContent(iqId, ProtocolTreeNode.fail(Constant.FAIL, "获取粉丝信息失败"));
                return iqId;
            }
            Boolean exist = redisService.hasKey(fansKey);
            if (exist && containsSessionNum != 0) {
                executeUserIdSubscribeInternal(jid);
                return SendToContact(jid, serialData, messageType, mediaType, iqId);
            }
            //陌生人
            String finalJid = jid;
            String finalIqId = iqId;

            String tempIqId = iqId;
            int finalContainsSessionNum = containsSessionNum;
            firstSendMsg(jid, (srcNode, result) -> {
                int tempNum = 0;
                try {
                    KeyLockUtil.lock(username);
                    tempNum = axolotlManager_.containsSessionNum(jidInfo);
                } catch (Exception ignore) {
                } finally {
                    KeyLockUtil.unlock(username);
                }
                if (tempNum == 0) {
                    taskNotify.setEventContent(tempIqId, ProtocolTreeNode.fail(Constant.FAIL, Constant.ExceptionReason.FANS_OFFLINE_TOO_LONG));
                    return;
                } else if (tempNum == 1) {
                    redisService.set(fansKey, 1, 60 * 60 * 12L);
                }
                if (finalContainsSessionNum == 0) {
                    //查询用户
                    UrlAddContact(GenerateIqId(), finalJid);
                    //需要信任联系人
                    SendToContact(finalJid, serialData, messageType, mediaType, finalIqId, true);
                } else {
                    SendToContact(finalJid, serialData, messageType, mediaType, finalIqId);

                }
            });
            return iqId;
        }
    }

    public void firstSendMsgToFans(String jid, byte[] serialData, String messageType, String mediaType, String iqId, String tcToken) {
        // 参数预处理
        if (StringUtil.isEmpty(iqId)) {
            iqId = GenerateMessageId();
        }
        jid = JidNormalize(jid);

        // 保存发送消息
        SaveSentMessage(jid, serialData, messageType, mediaType, iqId);
        StringUtil.JidInfo jidInfo = StringUtil.ParseJid(jid);

        // 处理群消息
        if (StringUtil.IsGroupJid(jid)) {
            SendToGroup(jid, serialData, messageType, mediaType, iqId);
            return;
        }

        // 处理私聊消息
        handlePrivateMessage(jid, jidInfo, serialData, messageType, mediaType, iqId, tcToken);
    }

    private void handlePrivateMessage(String jid, StringUtil.JidInfo jidInfo, byte[] serialData,
                                      String messageType, String mediaType, String iqId, String tcToken) {
        int syncMultipleDevicesCount = getSyncDevicesCount(jidInfo.recipientId);

        if (syncMultipleDevicesCount == 0) {
            handleUnsyncedContact(jid, jidInfo, serialData, messageType, mediaType, iqId, tcToken);
        } else {
            handleSyncedContact(jid, jidInfo, serialData, messageType, mediaType, iqId, tcToken);
        }
    }

    private int getSyncDevicesCount(String recipientId) {
        try {
            KeyLockUtil.lock(username);
            return axolotlManager_.syncMultipleDevicesStore.getCount(recipientId);
        } catch (Exception e) {
            return 0;
        } finally {
            KeyLockUtil.unlock(username);
        }
    }

    private void handleUnsyncedContact(String jid, StringUtil.JidInfo jidInfo, byte[] serialData,
                                       String messageType, String mediaType, String iqId, String tcToken) {
        List<String> userIds = axolotlManager_.contactSyncStore.getContacts(jidInfo.recipientId);
        if (!userIds.isEmpty()) {
            handlerSendMsg(userIds, jid, iqId, jidInfo, serialData, messageType, mediaType, tcToken);
            return;
        }
        ProtocolTreeNode contactNode = generateContactNode(Collections.singletonList(jidInfo.recipientId), GenerateIqId());
        AtomicBoolean success = new AtomicBoolean();

        // 设置超时处理
        scheduleTimeoutHandler(jid, jidInfo, serialData, messageType, mediaType, iqId, success, tcToken);

        // 添加同步任务
        AddTask(contactNode, (srcNode, result) -> {
            if (!success.getAndSet(true)) {
                handleSyncResult(result, jid, iqId, jidInfo, serialData, messageType, mediaType, tcToken);
            }
        });
    }

    private void scheduleTimeoutHandler(String jid, StringUtil.JidInfo jidInfo, byte[] serialData,
                                        String messageType, String mediaType, String iqId, AtomicBoolean success, String tcToken) {
        hashedWheelTimer.newTimeout(timeout -> {
            if (!success.getAndSet(true)) {
                ThreadPoolConfig.xmppPoolExecutor.execute(() -> {
                    List<String> userIds = Collections.singletonList(jid);
                    handlerSendMsg(userIds, jid, iqId, jidInfo, serialData, messageType, mediaType, tcToken);
                });
            }
        }, 3, TimeUnit.SECONDS);
    }

    private void handleSyncResult(ProtocolTreeNode result, String jid, String iqId, StringUtil.JidInfo jidInfo,
                                  byte[] serialData, String messageType, String mediaType, String tcToken) {
        List<String> userIds = handlerUserDeviceListAndSaveLid(result);
        if (userIds == null || userIds.isEmpty()) {
            taskNotify.setEventContent(iqId, ProtocolTreeNode.fail(Constant.FAIL, Constant.ExceptionReason.FANS_OFFLINE_TOO_LONG));
            return;
        }
        List<StringUtil.JidInfo> jidInfos = new ArrayList<>();
        for (String userId : userIds) {
            jidInfos.add(StringUtil.ParseJid(userId));
        }
        axolotlManager_.contactSyncStore.batchInsertSyncContact(jidInfos);
        handlerSendMsg(userIds, jid, iqId, jidInfo, serialData, messageType, mediaType, tcToken);
    }

    private void handleSyncedContact(String jid, StringUtil.JidInfo jidInfo, byte[] serialData, String messageType, String mediaType, String iqId, String tcToken) {
        executeUserIdSubscribeInternal(jid);

        // 如果是文本消息，添加输入状态模拟
        if ("text".equals(messageType)) {
            scheduleTypingIndicators(jid, jidInfo, serialData, messageType, mediaType, iqId, tcToken);
        } else {
            // 非文本消息直接发送
            sendMessage(jid, jidInfo, serialData, messageType, mediaType, iqId, tcToken);
        }
    }

    // 发送消息的逻辑提取为单独方法
    private void sendMessage(String jid, StringUtil.JidInfo jidInfo, byte[] serialData,
                             String messageType, String mediaType, String iqId, String tcToken) {
        byte[] tcTokenByte = null;
        if (StringUtils.hasLength(tcToken)) {
            tcTokenByte = Base64.getDecoder().decode(tcToken);
        }
        boolean isPkMsgStatusPending = getPkMsgSyncStatus(jidInfo.recipientId) == 1;

        if (isPkMsgStatusPending) {
            String result = SendToContact(jid, serialData, messageType, mediaType, iqId, true, tcTokenByte);
            if (StringUtils.hasLength(result)) {
                markSyncedAndSendPkMsg(jidInfo.recipientId);
            }
        } else {
            SendToContact(jid, serialData, messageType, mediaType, iqId, false, tcTokenByte);
        }
    }

    // 为文本消息安排输入中
    private void scheduleTypingIndicators(String jid, StringUtil.JidInfo jidInfo, byte[] serialData,
                                          String messageType, String mediaType, String iqId, String tcToken) {
        // 1秒后显示"正在输入"状态
        hashedWheelTimer.newTimeout(timeout -> {
            sendChatComposing(jid);

            // 再过2秒后暂停输入并发送消息
            hashedWheelTimer.newTimeout(timeout1 -> {
                sendChatPaused(jid);
                ThreadPoolConfig.xmppPoolExecutor.execute(() ->
                        sendMessage(jid, jidInfo, serialData, messageType, mediaType, iqId, tcToken));
            }, 2, TimeUnit.SECONDS);
        }, 1, TimeUnit.SECONDS);
    }

    private void handlerSendMsg(List<String> userIds, String finalJid, String tempIqId, StringUtil.JidInfo jidInfo, byte[] serialData, String messageType, String mediaType, String tcToken) {
        // 判断数据库中是否已经有了某一个设备的session(主设备或者某一个多设备)
        List<String> shouldGetKeyList = new ArrayList<>();
        for (String id : userIds) {
            try {
                KeyLockUtil.lock(username);
                XmppJid xmppJid = XmppJid.of(id);
                if (ObjectUtil.isNotNull(xmppJid)) {
                    boolean hasSession = axolotlManager_.sessionStore_.containsSession(new SignalProtocolAddress(xmppJid.getUser(), xmppJid.getDevice()));
                    if (!hasSession) {
                        // 若没有session则需要获取一次性密钥进行发送
                        shouldGetKeyList.add(xmppJid.toString());
                    }
                }
            } catch (Exception ignore) {
            } finally {
                KeyLockUtil.unlock(username);
            }
        }
        // 获取图片, 获取主设备的即可
        sendGetPicture(finalJid);
        executeUserIdSubscribeInternal(finalJid);
        hashedWheelTimer.newTimeout(timeout -> {
            ThreadPoolConfig.xmppPoolExecutor.execute(() -> {
                sendChatComposing(finalJid);
                //获取一次性密钥, 只获取库里面没有的
                if (!shouldGetKeyList.isEmpty()) {
                    GetKeysFor(shouldGetKeyList, (srcNode2, result2) -> {
                        boolean isGetKeySuccess = false;
                        try {
                            KeyLockUtil.lock(username);
                            XmppJid xmppJid = XmppJid.of(finalJid);
                            if (ObjectUtil.isNotNull(xmppJid)) {
                                // 判断数据库内session是否大于等于1, 则判断同步成功
                                int size = axolotlManager_.sessionStore_.getSubDeviceSessions(xmppJid.getUser()).size();
                                if (size >= 1) {
                                    isGetKeySuccess = true;
                                }
                            }
                        } catch (Exception ignore) {
                        } finally {
                            KeyLockUtil.unlock(username);
                        }
                        if (!isGetKeySuccess) {
                            taskNotify.setEventContent(tempIqId, ProtocolTreeNode.fail(Constant.FAIL, Constant.ExceptionReason.FANS_OFFLINE_TOO_LONG));
                        }
                    });
                }
                //标记已获取多设备
                markMultipleDevicesSynced(jidInfo.recipientId);
                // 获取多设备session完毕, 可以带上输入状态发送消息
                hashedWheelTimer.newTimeout(timeout2 -> {
                    sendChatPaused(finalJid);
                    hashedWheelTimer.newTimeout(timeout1 -> {
                        byte[] tcTokenByte;
                        if (StringUtils.hasLength(tcToken)) {
                            tcTokenByte = Base64.getDecoder().decode(tcToken);
                        } else {
                            tcTokenByte = null;
                        }
                        //需要信任联系人
                        ThreadPoolConfig.xmppPoolExecutor.execute(() -> SendToContact(finalJid, serialData, messageType, mediaType, tempIqId, true, tcTokenByte));
                    }, 3, TimeUnit.SECONDS);
                }, 5, TimeUnit.SECONDS);
            });
        }, 1, TimeUnit.SECONDS);
    }

    public void firstSendMsgToFans(String jid, List<String> userIds, byte[] serialData, String messageType, String mediaType, String iqId) {
        if (StringUtil.isEmpty(iqId)) {
            iqId = GenerateMessageId();
        }
        if (StringUtils.isEmpty(jid) && (userIds == null || userIds.size() == 0)) {
            taskNotify.setEventContent(iqId, ProtocolTreeNode.fail(Constant.FAIL, "粉丝id不能为空"));
            return;
        }
        if (userIds != null && userIds.size() > 0) {
            jid = userIds.get(0);
        } else {
            userIds = Collections.singletonList(jid);
        }
        jid = JidNormalize(jid);
        SaveSentMessage(jid, serialData, messageType, mediaType, iqId);
        StringUtil.JidInfo jidInfo = StringUtil.ParseJid(jid);

        if (StringUtil.IsGroupJid(jid)) {
            SendToGroup(jid, serialData, messageType, mediaType, iqId);
            return;
        }
        //获取没获取过多设备的用户
        List<String> noSessionList = getNoSessionList(userIds);
        if (noSessionList.isEmpty()) {
            executeUserIdSubscribeInternal(jid);
            //代表获取过多设备，则直接发，否则获取一遍多设备
            SendToContact(jid, userIds, serialData, messageType, mediaType, iqId, false);
            return;
        }
        String finalJid = jid;
        String tempIqId = iqId;
        List<String> recipientIds = userIds.stream().map(s -> StrUtil.replace(s, "@s.whatsapp.net", "")).collect(Collectors.toList());
        List<String> finalUserIds = userIds.stream().map(this::JidNormalize).collect(Collectors.toList());
        ProtocolTreeNode contactNode = queryContact(GenerateIqId(), recipientIds);
        AddTask(contactNode, (srcNode, result) -> {
            List<String> uploadUserIds = handlerUserDeviceList(result);
            if (uploadUserIds != null && uploadUserIds.size() == 0) {
                taskNotify.setEventContent(tempIqId, ProtocolTreeNode.fail(Constant.FAIL, Constant.ExceptionReason.FANS_OFFLINE_TOO_LONG));
                return;
            }
            sendIk(jidInfo.toString(), (srcNode1, result1) -> {
                sendGetPicture(finalJid);
                Subscribe(finalJid);
                sendChatComposing(finalJid);
                //获取一次性密钥
                GetKeysFor(uploadUserIds == null ? Collections.singletonList(finalJid) : uploadUserIds, (srcNode3, result3) -> {
                    int tempNum = 0;
                    try {
                        KeyLockUtil.lock(username);
                        tempNum = axolotlManager_.containsSessionNum(recipientIds);
                    } catch (Exception ignore) {
                    } finally {
                        KeyLockUtil.unlock(username);
                    }
                    if (tempNum == 0) {
                        taskNotify.setEventContent(tempIqId, ProtocolTreeNode.fail(Constant.FAIL, Constant.ExceptionReason.FANS_OFFLINE_TOO_LONG));
                        return;
                    }
                    hashedWheelTimer.newTimeout(timeout -> {
                        sendChatPaused(finalJid);
                        hashedWheelTimer.newTimeout(timeout1 -> {
                            //需要信任联系人
                            ThreadPoolConfig.xmppPoolExecutor.execute(() -> SendToContact(finalJid, finalUserIds, serialData, messageType, mediaType, tempIqId, true));
                        }, 3, TimeUnit.SECONDS);
                    }, 5, TimeUnit.SECONDS);
                });
            });
        });
    }

    private List<String> handlerUserDeviceList(ProtocolTreeNode node) {
        ProtocolTreeNode syncNode = node.getOneChildren("usync");
        if (syncNode == null) {
            return null;
        }
        ProtocolTreeNode listNode = syncNode.getOneChildren("list");
        if (listNode == null) {
            return null;
        }
        List<String> list = new ArrayList<>();
        LinkedList<ProtocolTreeNode> userList = listNode.GetChildren("user");
        for (ProtocolTreeNode userNode : userList) {
            String jid = userNode.GetAttributeValue("jid");
            if (StringUtils.hasLength(jid)) {
                ProtocolTreeNode contact = userNode.getOneChildren("contact");
                String type = contact.GetAttributeValue("type");
                boolean exist = "in".equals(type);
                if (exist) {
                    StringUtil.JidInfo jidInfo = StringUtil.ParseJid(jid);
                    ProtocolTreeNode devicesNode = userNode.getOneChildren("devices");
                    LinkedList<ProtocolTreeNode> deviceListNode = devicesNode.GetChildren("device-list");
                    for (ProtocolTreeNode deviceNode : deviceListNode) {
                        LinkedList<ProtocolTreeNode> deviceList = deviceNode.GetChildren("device");
                        if (ObjectUtil.isNull(deviceList)) {
                            // <devices> <device-list /> </devices>
                            // 判断这种情况, 直接添加xxx.0:0@s.whatsapp.net即可
                            String userId = String.format("%s.0:0@%s", jidInfo.recipientId, "s.whatsapp.net");
                            list.add(userId);
                            break;
                        }
                        for (ProtocolTreeNode device : deviceList) {
                            String deviceId = device.GetAttributeValue("id");
                            String userId = String.format("%s.0:%s@%s", jidInfo.recipientId, deviceId, "s.whatsapp.net");
                            list.add(userId);
                        }
                    }
                }
            }
        }
        return list;
    }

    private List<String> handlerUserDeviceListAndSaveLid(ProtocolTreeNode node) {
        ProtocolTreeNode syncNode = node.getOneChildren("usync");
        if (syncNode == null) {
            return null;
        }
        ProtocolTreeNode listNode = syncNode.getOneChildren("list");
        if (listNode == null) {
            return null;
        }
        List<String> list = new ArrayList<>();
        LinkedList<ProtocolTreeNode> userList = listNode.GetChildren("user");
        for (ProtocolTreeNode userNode : userList) {
            String jid = userNode.GetAttributeValue("jid");
            if (StringUtils.hasLength(jid)) {
                ProtocolTreeNode contact = userNode.getOneChildren("contact");
                String type = contact.GetAttributeValue("type");
                boolean exist = "in".equals(type);
                if (exist) {
                    // 获取Lid并保存
                    ProtocolTreeNode lid = userNode.getOneChildren("lid");
                    if (ObjectUtil.isNotNull(lid) && StringUtils.hasLength(lid.GetAttributeValue("val"))) {
                        saveNodeJidLid(jid, lid.GetAttributeValue("val"));
                    }
                    StringUtil.JidInfo jidInfo = StringUtil.ParseJid(jid);
                    ProtocolTreeNode devicesNode = userNode.getOneChildren("devices");
                    LinkedList<ProtocolTreeNode> deviceListNode = devicesNode.GetChildren("device-list");
                    for (ProtocolTreeNode deviceNode : deviceListNode) {
                        LinkedList<ProtocolTreeNode> deviceList = deviceNode.GetChildren("device");
                        if (ObjectUtil.isNull(deviceList)) {
                            // <devices> <device-list /> </devices>
                            // 判断这种情况, 直接添加xxx.0:0@s.whatsapp.net即可
                            String userId = String.format("%s.0:0@%s", jidInfo.recipientId, "s.whatsapp.net");
                            list.add(userId);
                            break;
                        }
                        for (ProtocolTreeNode device : deviceList) {
                            String deviceId = device.GetAttributeValue("id");
                            String userId = String.format("%s.0:%s@%s", jidInfo.recipientId, deviceId, "s.whatsapp.net");
                            list.add(userId);
                        }
                    }
                }
            }
        }
        return list;
    }

    private List<String> handlerUserDeviceListByJid(ProtocolTreeNode node) {
        ProtocolTreeNode syncNode = node.getOneChildren("usync");
        if (syncNode == null) {
            return null;
        }
        ProtocolTreeNode listNode = syncNode.getOneChildren("side_list");
        if (listNode == null) {
            return null;
        }
        List<String> list = new ArrayList<>();
        LinkedList<ProtocolTreeNode> userList = listNode.GetChildren("user");
        for (ProtocolTreeNode userNode : userList) {
            String jid = userNode.GetAttributeValue("jid");
            if (StringUtils.hasLength(jid)) {
                ProtocolTreeNode sideList = userNode.getOneChildren("sidelist");
                String type = sideList.GetAttributeValue("type");
                boolean exist = "in".equals(type);
                if (exist) {
                    StringUtil.JidInfo jidInfo = StringUtil.ParseJid(jid);
                    ProtocolTreeNode devicesNode = userNode.getOneChildren("devices");
                    if (ObjectUtil.isNull(devicesNode)) {
                        String userId = String.format("%s.0:0@%s", jidInfo.recipientId, "s.whatsapp.net");
                        list.add(userId);
                        continue;
                    }
                    LinkedList<ProtocolTreeNode> deviceListNode = devicesNode.GetChildren("device-list");
                    for (ProtocolTreeNode deviceNode : deviceListNode) {
                        LinkedList<ProtocolTreeNode> deviceList = deviceNode.GetChildren("device");
                        if (ObjectUtil.isNull(deviceList)) {
                            // <devices> <device-list /> </devices>
                            // 判断这种情况, 直接添加xxx.0:0@s.whatsapp.net即可
                            String userId = String.format("%s.0:0@%s", jidInfo.recipientId, "s.whatsapp.net");
                            list.add(userId);
                            break;
                        }
                        for (ProtocolTreeNode device : deviceList) {
                            String deviceId = device.GetAttributeValue("id");
                            String userId = String.format("%s.0:%s@%s", jidInfo.recipientId, deviceId, "s.whatsapp.net");
                            list.add(userId);
                        }
                    }
                }
            }
        }
        return list;
    }

    class HandleGetGroupInfo implements NodeCallback {
        byte[] message_;
        String messageType_;
        String mediaType_;
        String iqId_;
        String groupId;
        SenderKeyRecord senderKeyRecord;

        HandleGetGroupInfo(byte[] message, String messageType, String mediaType, String iqId, String groupId, SenderKeyRecord senderKeyRecord) {
            message_ = message;
            messageType_ = messageType;
            mediaType_ = mediaType;
            iqId_ = iqId;
            this.groupId = groupId;
            this.senderKeyRecord = senderKeyRecord;
        }

        @Override
        public void Run(ProtocolTreeNode srcNode, ProtocolTreeNode result) {
            ProtocolTreeNode group = result.GetChild("group");
            if (group == null) {
                handleSendMessageFail(iqId_, "获取群失败，请确保当前账号在此群");
                log.info("用户：{}，获取群: {}, 失败：{}", username, groupId, srcNode.toString());
                return;
            }
            boolean addressingModeJid = true;
            String addressingMode = group.GetAttributeValue("addressing_mode");
            if (StrUtil.isNotBlank(addressingMode)) {
                if (addressingMode.equals("lid")) {
                    addressingModeJid = false;
                }
            }
            String selfJid = JidNormalize(envBuilder_.getFullphone());
            List<String> sessionJids = new LinkedList<>();
            LinkedList<ProtocolTreeNode> participants = group.GetChildren("participant");
            for (ProtocolTreeNode participant : participants) {
                String jid;
                if (addressingModeJid) {
                    jid = participant.GetAttributeValue("jid");
                } else {
                    jid = participant.GetAttributeValue("phone_number");
                    if (StrUtil.isEmpty(jid)) {
                        continue;
                    }
                }
                if (jid.equals(selfJid)) {
                    continue;
                }
                sessionJids.add(jid);
            }
            if (sessionJids.isEmpty()) {
                handleSendMessageFail(iqId_, "获取群成员为空");
                log.info("用户：{}，获取群: {}, 成员为空：{}", username, groupId, srcNode.toString());
                return;
            }
            String to = srcNode.GetAttributeValue("to");
            ProtocolTreeNode protocolTreeNode = generateUserDeviceInfo(GenerateIqId(), sessionJids);
            AddTask(protocolTreeNode, (srcNode1, result1) -> {
                List<String> userList = parseDeviceResult(result1);
                if (userList != null && !userList.isEmpty()) {
                    EnsureSessionsAndSendToGroup(to, userList, message_, messageType_, mediaType_, iqId_, false, senderKeyRecord, false, null);
                } else {
                    handleSendMessageFail(iqId_, "解析多设备结果失败");
                }
            });
        }
    }


    void EnsureSessionsAndSendToGroup(String groupId, List<String> sessionJids, byte[] message, String messageType, String mediaType, String iqId, boolean isRetry, SenderKeyRecord senderKeyRecord, boolean isNodeSession, String retryCount) {
        if (isNodeSession) {
            try {
                SendToGroupWithSessions(groupId, sessionJids, message, messageType, mediaType, iqId, isRetry, senderKeyRecord, retryCount);
            } catch (UntrustedIdentityException | NoSessionException | InvalidKeyException | InvalidKeyIdException e) {
                handleSendMessageFail(iqId, e.getMessage());
                log.error("异常：{}", e.getMessage());
            }
        } else {
            GetKeysFor(sessionJids, (srcNode, result) -> {
                try {
                    SendToGroupWithSessions(groupId, sessionJids, message, messageType, mediaType, iqId, isRetry, senderKeyRecord, retryCount);
                } catch (UntrustedIdentityException | NoSessionException | InvalidKeyException |
                         InvalidKeyIdException e) {
                    handleSendMessageFail(iqId, e.getMessage());
                    log.error("异常：{}", e.getMessage());
                }
            });
        }
    }

    void SendToGroupWithSessions(String groupId, List<String> sessionJids, byte[] message, String messageType, String mediaType, String iqId, boolean isRetry, SenderKeyRecord senderKeyRecord, String retryCount) throws UntrustedIdentityException, NoSessionException, InvalidKeyException, InvalidKeyIdException {
        ByteString senderKeySerial;
        if (!sessionJids.isEmpty()) {
            SenderKeyState state = senderKeyRecord.getSenderKeyState();
            SenderKeyDistributionMessage senderKeyDistributionMessage = new SenderKeyDistributionMessage(state.getKeyId(),
                    state.getSenderChainKey().getIteration(),
                    state.getSenderChainKey().getSeed(),
                    state.getSigningKeyPublic());
            senderKeySerial = ByteString.copyFrom(senderKeyDistributionMessage.serialize());
        } else {
            senderKeySerial = null;
        }
        // 添加reportToken
        byte[] reportToken = null;
        try {
            WhatsMessage.WhatsAppMessage whatsAppMessage = WhatsMessage.WhatsAppMessage.parseFrom(message);
            byte[] reportTokenProto = generateEncryptProto(whatsAppMessage);
            if (ObjectUtil.isNotNull(reportTokenProto)) {
                log.debug("用户:{}, 生成计算reportProto: {}", username, HexUtil.encodeHexStr(reportTokenProto));
                // 生成messageSecret
                byte[] messageSecret = RandomUtil.randomBytes(32);
                reportToken = generateEncryptToken(reportTokenProto, messageSecret, iqId, userId, groupId);
                // 将 messageSecret 放到message中
                WhatsMessage.MessageContextInfo.Builder messageContextInfo = WhatsMessage.MessageContextInfo.newBuilder();
                if (whatsAppMessage.hasMessageContextInfo()) {
                    messageContextInfo.mergeFrom(whatsAppMessage.getMessageContextInfo());
                }
                messageContextInfo.setMessageSecret(ByteString.copyFrom(messageSecret));
                WhatsMessage.WhatsAppMessage.Builder newMessageProto = whatsAppMessage.toBuilder();
                newMessageProto.setMessageContextInfo(messageContextInfo);
                message = newMessageProto.build().toByteArray();
                log.debug("用户:{}, 生成新的message: {}", username, HexUtil.encodeHexStr(message));
            }
        } catch (InvalidProtocolBufferException ignore) {
        }
        byte[] finalMessage = message;
        byte[] finalReportToken = reportToken;
        ThreadPoolConfig.sendMsgPoolExecutor.execute(() -> {
            ProtocolTreeNode participants = new ProtocolTreeNode("participants");
            //组装消息
            ProtocolTreeNode msg = new ProtocolTreeNode("message");
            if (hasUploadReport() && ObjectUtil.isNotNull(finalReportToken)) {
                ProtocolTreeNode reporting = new ProtocolTreeNode("reporting");
                ProtocolTreeNode reportingToken = new ProtocolTreeNode("reporting_token");
                reportingToken.AddAttribute(new StanzaAttribute("v", "1"));
                reportingToken.SetData(finalReportToken);
                reporting.AddChild(reportingToken);
                msg.AddChild(reporting);
            }
            msg.AddAttribute(new StanzaAttribute("id", iqId));
            if (!sessionJids.isEmpty()) {
                for (String jid : sessionJids) {
                    StringUtil.JidInfo jidInfo = StringUtil.ParseJid(jid);
                    WhatsMessage.WhatsAppMessage.Builder messageBuild;
                    try {
                        messageBuild = WhatsMessage.WhatsAppMessage.parseFrom(finalMessage).toBuilder();
                    } catch (InvalidProtocolBufferException e) {
                        messageBuild = WhatsMessage.WhatsAppMessage.newBuilder();
                    }
                    SerializeSenderKeyDistributionMessageToProtobuf(messageBuild, groupId, senderKeySerial);
                    CiphertextMessage cipherText = null;
                    try {
                        KeyLockUtil.lock(username);
                        cipherText = axolotlManager_.Encrypt(jidInfo, messageBuild.build().toByteArray());
                    } catch (Throwable ignore) {
                        continue;
                    } finally {
                        KeyLockUtil.unlock(username);
                    }
                    ProtocolTreeNode encNode = new ProtocolTreeNode("enc");
                    encNode.AddAttribute(new StanzaAttribute("v", "2"));
                    encNode.SetData(cipherText.serialize());
                    if (!StringUtil.isEmpty(mediaType)) {
                        encNode.AddAttribute(new StanzaAttribute("mediatype", mediaType));
                    }
                    switch (cipherText.getType()) {
                        case CiphertextMessage.PREKEY_TYPE: {
                            encNode.AddAttribute(new StanzaAttribute("type", "pkmsg"));
                        }
                        break;
                        case CiphertextMessage.SENDERKEY_TYPE: {
                            encNode.AddAttribute(new StanzaAttribute("type", "skmsg"));
                        }
                        break;
                        default: {
                            encNode.AddAttribute(new StanzaAttribute("type", "msg"));
                        }
                    }
                    if (messageType.equals("reaction") || TypeConstant.TaskType.REVOKE_REACTION_MESSAGE.equals(messageType)) {
                        encNode.AddAttribute(new StanzaAttribute("decrypt-fail", "hide"));
                    }
                    if (!isRetry) {
                        ProtocolTreeNode to = new ProtocolTreeNode("to");
                        to.AddAttribute(new StanzaAttribute("jid", jid));
                        to.AddChild(encNode);
                        participants.AddChild(to);
                    } else {
                        // 重试消息一般只对单人, 直接把加入msg的attributeAttr即可
                        encNode.AddAttribute(new StanzaAttribute("count", retryCount));
                        msg.AddChild(encNode);
                        msg.AddAttribute(new StanzaAttribute("participant", jid));
                    }
                }
            }
            if (TypeConstant.TaskType.REVOKE_MESSAGE.equals(messageType)) {
                // 反序列message, 判断是否为管理员删除消息
                try {
                    WhatsMessage.WhatsAppMessage whatsAppMessage = WhatsMessage.WhatsAppMessage.parseFrom(finalMessage);
                    WhatsMessage.WhatsAppProtocolMessage protocolMessage = whatsAppMessage.getProtocolMessage();
                    WhatsMessage.WhatsAppProtocolMessage.MessageKey key = protocolMessage.getKey();
                    if (StrUtil.isNotEmpty(key.getParticipant()) && key.getFromMe() == 0) {
                        msg.AddAttribute(new StanzaAttribute("edit", "8"));
                    } else {
                        msg.AddAttribute(new StanzaAttribute("edit", "7"));
                    }
                    msg.AddAttribute(new StanzaAttribute("type", "text"));
                } catch (InvalidProtocolBufferException ignore) {
                    log.error("用户: {}, 撤回消息反序列化失败", username);
                }
            } else if (TypeConstant.TaskType.REVOKE_REACTION_MESSAGE.equals(messageType)) {
                msg.AddAttribute(new StanzaAttribute("edit", "7"));
                msg.AddAttribute(new StanzaAttribute("type", "reaction"));
            } else {
                msg.AddAttribute(new StanzaAttribute("type", messageType));
            }
            msg.AddAttribute(new StanzaAttribute("to", groupId));
            if (businessVersion && envBuilder_.hasVerifyedName()) {
                msg.AddAttribute(new StanzaAttribute("verified_name", envBuilder_.getVerifyedName()));
            }
            msg.AddAttribute(new StanzaAttribute("t", String.valueOf(System.currentTimeMillis() / 1000)));
            //添加group 消息
            if (!isRetry) {
                ProtocolTreeNode encNode = new ProtocolTreeNode("enc");
                encNode.AddAttribute(new StanzaAttribute("v", "2"));
                encNode.AddAttribute(new StanzaAttribute("type", "skmsg"));
                if (!StringUtil.isEmpty(mediaType)) {
                    encNode.AddAttribute(new StanzaAttribute("mediatype", mediaType));
                }
                try {
                    KeyLockUtil.lock(username);
                    encNode.SetData(axolotlManager_.GroupEncrypt(groupId, 0, finalMessage, senderKeyRecord));
                } catch (Exception ignore) {
                } finally {
                    KeyLockUtil.unlock(username);
                }
                if (messageType.equals("reaction") || TypeConstant.TaskType.REVOKE_REACTION_MESSAGE.equals(messageType)) {
                    encNode.AddAttribute(new StanzaAttribute("decrypt-fail", "hide"));
                }
                msg.AddChild(encNode);
            }
            if (participants.GetChildren() != null) {
                msg.AddChild(participants);
            }
            if (isRetry) {
                AddTask(msg);
            } else {
                sendMsgTask(msg, (srcNode, result) -> {
                    if (result != null && "ack".equals(result.GetTag())) {
                        HandleAck(result);
                    }
                });
            }
        });
    }

    WhatsMessage.WhatsAppMessage.Builder SerializeSenderKeyDistributionMessageToProtobuf(WhatsMessage.WhatsAppMessage.Builder builder, String groupId, ByteString senderKeySerial) {
        WhatsMessage.WhatsAppSenderKeyDistributionMessage.Builder senderKeyBuilder = builder.getSenderKeyDistributionMessageBuilder();
        senderKeyBuilder.setGroupId(groupId);
        senderKeyBuilder.setAxolotlSenderKeyDistributionMessage(senderKeySerial);
        return builder;
    }

    String SendToGroup(String jid, byte[] message, String messageType, String mediaType, String iqId) {
        SenderKeyRecord senderKeyRecord;
        try {
            KeyLockUtil.lock(username);
            senderKeyRecord = axolotlManager_.LoadSenderKey(jid);
        } catch (Exception e) {
            handleSendMessageFail(iqId, e.getMessage());
            return null;
        } finally {
            KeyLockUtil.unlock(username);
        }
        if (senderKeyRecord.isEmpty()) {
            try {
                KeyLockUtil.lock(username);
                //删除预存储的未发送的群成员信息
                axolotlManager_.deletePreGroupFans(jid);
            } catch (Exception ignored) {
            } finally {
                KeyLockUtil.unlock(username);
            }
            senderKeyRecord = axolotlManager_.GroupCreateSenderKeyRecord();
            //获取群信息
            InnerGetGroupInfo(jid, new HandleGetGroupInfo(message, messageType, mediaType, iqId, jid, senderKeyRecord));
            SenderKeyRecord finalSenderKeyRecord = senderKeyRecord;
            taskNotify.addConsumer(iqId, node -> {
                // 判断node是否为ack
                if (!"ack".equals(node.GetTag())) {
                    return;
                }
                String messageId = node.IqId();
                if (!StringUtils.hasLength(messageId)) {
                    return;
                }
                String error = node.GetAttributeValue("error");
                if (StringUtils.hasLength(error)) {
                    return;
                }
                // 首条消息发送成功才将senderKeyRecord写入
                try {
                    KeyLockUtil.lock(username);
                    axolotlManager_.senderKeyStore_.storeSenderKey(new SenderKeyName(jid, new SignalProtocolAddress(username, 0)), finalSenderKeyRecord);
                } catch (Exception ignore) {
                    log.error("用户: {} 群id: {} 发送首条消息写入失败", username, jid);
                } finally {
                    KeyLockUtil.unlock(username);
                }
            });
        } else {
            List<String> fansIds = axolotlManager_.getPreGroupFans(jid);
            taskNotify.addConsumer(iqId, node -> {
                //发送消息成功后再移除粉丝
                removeGroupFansRecord(node, jid, fansIds);
            });
            groupMsgSecondSend(jid, message, messageType, mediaType, iqId, fansIds, senderKeyRecord);
        }
        return iqId;
    }

    /**
     * 群组消息二次发送
     */
    private void groupMsgSecondSend(String jid, byte[] message, String messageType, String mediaType,
                                    String iqId, List<String> fansIds, SenderKeyRecord senderKeyRecord) {
        Set<XmppJid> noSessionIds = new HashSet<>();
        Set<String> fansIdSet = new HashSet<>();
        // 判断是否为主设备, 则获取所有设备进行发送
        try {
            for (String fansId : fansIds) {
                StringUtil.JidInfo jidInfo = StringUtil.ParseJid(fansId);
                // 判断是否为lid
                if (XmppJid.isIncognitoJid(jidInfo.toString())) {
                    continue;
                }
                boolean exist = axolotlManager_.ContainsSession(jidInfo);
                XmppJid xmppJid = XmppJid.of(jidInfo.toString());
                if (!ObjectUtil.isNotNull(xmppJid)) {
                    log.error("用户 {}, XmppJid类解析jid:{} 返回空", username, fansId);
                    continue;
                }
                if (exist) {
                    if (xmppJid.getDevice() == 0) {
                        // 一般来说session中有主设备的都是同步了通讯录及多设备的
                        String user = xmppJid.getUser();
                        try {
                            KeyLockUtil.lock(username);
                            List<Integer> subDeviceSessions = axolotlManager_.GetSubDeviceSessions(xmppJid.getUser());
                            for (Integer deviceId : subDeviceSessions) {
                                fansIdSet.add(new XmppJid(user, "s.whatsapp.net", deviceId, 0).toString());
                            }
                        } catch (Exception e) {
                            log.error("用户: {}, 数据库获取 {} 多设备异常", username, xmppJid.getUser());
                        } finally {
                            KeyLockUtil.unlock(username);
                        }
                    } else {
                        // 为重试的多设备, 为了兼容也添加进去
                        fansIdSet.add(xmppJid.toString());
                    }
                } else {
                    // 判断是否为主设备, 若为主设备则同步一编通讯录
                    noSessionIds.add(xmppJid);
                }
            }
            if (noSessionIds.isEmpty()) {
                SendToGroupWithSessions(jid, new ArrayList<>(fansIdSet), message, messageType, mediaType, iqId, false, senderKeyRecord, null);
                return;
            }
            List<String> noSessionMasterIds = new ArrayList<>();
            Set<String> noSessionJids = new HashSet<>();
            // 判断无session的粉丝是否是主设备
            for (XmppJid noSessionJid : noSessionIds) {
                if (noSessionJid.getDevice() == 0) {
                    // 为主设备则需要获取多设备信息
                    noSessionMasterIds.add(noSessionJid.toString());
                }
                noSessionJids.add(noSessionJid.toString());
                fansIdSet.add(noSessionJid.toString());
            }
            // 获取多设备信息
            ProtocolTreeNode protocolTreeNode = generateUserDeviceInfo(GenerateIqId(), noSessionMasterIds);
            AddTask(protocolTreeNode, (srcNode, result) -> {
                try {
                    List<String> userList = parseDeviceResult(result);
                    // 将userList添加进fansIdSet中
                    fansIdSet.addAll(userList);
                    // 将userList添加进noSessionJids中获取session
                    noSessionJids.addAll(userList);
                    // 将获取的多设备信息获取session
                    GetKeysFor(new ArrayList<>(noSessionJids), (srcNode1, result1) -> {
                        try {
                            SendToGroupWithSessions(jid, new ArrayList<>(fansIdSet), message, messageType, mediaType, iqId, false, senderKeyRecord, null);
                        } catch (UntrustedIdentityException | NoSessionException | InvalidKeyException |
                                 InvalidKeyIdException e) {
                            handleSendMessageFail(iqId, e.getMessage());
                            log.error("用户 {} 异常 {}", username, e.getMessage());
                        }
                    });
                } catch (Exception e) {
                    log.error("用户: {}, 发送群聊消息异常", username, e);
                }
            });
        } catch (UntrustedIdentityException | NoSessionException | InvalidKeyException e) {
            handleSendMessageFail(iqId, e.getMessage());
            log.error("用户: {}, 发送群聊消息异常", username, e);
        } catch (Exception e) {
            log.error("用户: {}, 转换Jid异常", username, e);
        }
    }

    void removeGroupFansRecord(ProtocolTreeNode node, String jid, List<String> fansIds) {
        if (!"ack".equals(node.GetTag())) {
            return;
        }
        String messageId = node.IqId();
        if (!StringUtils.hasLength(messageId)) {
            return;
        }
        String error = node.GetAttributeValue("error");
        if (StringUtils.hasLength(error)) {
            return;
        }
        try {
            KeyLockUtil.lock(username);
            axolotlManager_.deletePreGroupFans(jid, fansIds);
        } catch (Exception ignored) {
        } finally {
            KeyLockUtil.unlock(username);
        }
    }

    String InnerGetGroupInfo(String jid, NodeCallback callback) {
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("id", GenerateIqId()));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        iq.AddAttribute(new StanzaAttribute("to", jid));
        iq.AddAttribute(new StanzaAttribute("xmlns", "w:g2"));

        ProtocolTreeNode query = new ProtocolTreeNode("query");
        query.AddAttribute(new StanzaAttribute("request", "interactive"));
        iq.AddChild(query);
        return AddTask(iq, callback);
    }

    String SendToContact(String jid, byte[] message, String messageType, String mediaType, String iqId) {
        return SendToContact(jid, message, messageType, mediaType, iqId, false);
    }

    String SendToContact(String jid, byte[] message, String messageType, String mediaType, String iqId, boolean needTrustedContact) {
        return SendToContact(jid, message, messageType, mediaType, iqId, needTrustedContact, null);
    }

    String SendToContact(String jid, byte[] message, String messageType, String mediaType, String iqId, boolean needTrustedContact, byte[] tcToken) {
        ArrayList<CiphertextMessage> cipherText = new ArrayList<>();
        ArrayList<StringUtil.JidInfo> participant = new ArrayList<>();
        byte[] reportToken = null;
        try {
            WhatsMessage.WhatsAppMessage whatsAppMessage = WhatsMessage.WhatsAppMessage.parseFrom(message);
            byte[] reportTokenProto = generateEncryptProto(whatsAppMessage);
            if (ObjectUtil.isNotNull(reportTokenProto)) {
                log.debug("用户:{}, 生成计算reportProto: {}", username, HexUtil.encodeHexStr(reportTokenProto));
                // 生成messageSecret
                byte[] messageSecret = RandomUtil.randomBytes(32);
                reportToken = generateEncryptToken(reportTokenProto, messageSecret, iqId, userId, jid);
                // 将 messageSecret 放到message中
                WhatsMessage.MessageContextInfo.Builder messageContextInfo = WhatsMessage.MessageContextInfo.newBuilder();
                if (whatsAppMessage.hasMessageContextInfo()) {
                    messageContextInfo.mergeFrom(whatsAppMessage.getMessageContextInfo());
                }
                messageContextInfo.setMessageSecret(ByteString.copyFrom(messageSecret));
                WhatsMessage.WhatsAppMessage.Builder newMessageProto = whatsAppMessage.toBuilder();
                newMessageProto.setMessageContextInfo(messageContextInfo);
                message = newMessageProto.build().toByteArray();
                log.debug("用户:{}, 生成新的message: {}", username, HexUtil.encodeHexStr(message));
            }
        } catch (InvalidProtocolBufferException ignore) {
        }
        try {
            KeyLockUtil.lock(username);
            StringUtil.JidInfo jidInfo = StringUtil.ParseJid(jid);
            List<Integer> deviceids = axolotlManager_.GetSubDeviceSessions(jidInfo.recipientId);

            for (Integer id : deviceids) {
                StringUtil.JidInfo part = jidInfo.Copy();
                part.deviceId = id;
                cipherText.add(axolotlManager_.Encrypt(part, message));
                participant.add(part);
            }
        } catch (Throwable e) {
            handleSendMessageFail(iqId, e.getMessage());
            return null;
        } finally {
            KeyLockUtil.unlock(username);
        }
        try {
            return SendEncMessage(jid, participant, cipherText, messageType, mediaType, iqId, needTrustedContact, reportToken, tcToken);
        } catch (Exception e) {
            handleSendMessageFail(iqId, e.getMessage());
            log.error("用户: {}, 发送消息异常", username, e);
            return null;
        }
    }

    String SendToContact(String jid, byte[] message, String messageType, String mediaType, String iqId, String retryCount) {
        ArrayList<CiphertextMessage> cipherText = new ArrayList<>();
        ArrayList<StringUtil.JidInfo> participant = new ArrayList<>();
        byte[] reportToken = null;
        try {
            WhatsMessage.WhatsAppMessage whatsAppMessage = WhatsMessage.WhatsAppMessage.parseFrom(message);
            byte[] reportTokenProto = generateEncryptProto(whatsAppMessage);
            if (ObjectUtil.isNotNull(reportTokenProto)) {
                log.debug("用户:{}, 生成计算reportProto: {}", username, HexUtil.encodeHexStr(reportTokenProto));
                // 生成messageSecret
                byte[] messageSecret = RandomUtil.randomBytes(32);
                reportToken = generateEncryptToken(reportTokenProto, messageSecret, iqId, userId, jid);
                // 将 messageSecret 放到message中
                WhatsMessage.MessageContextInfo.Builder messageContextInfo = WhatsMessage.MessageContextInfo.newBuilder();
                if (whatsAppMessage.hasMessageContextInfo()) {
                    messageContextInfo.mergeFrom(whatsAppMessage.getMessageContextInfo());
                }
                messageContextInfo.setMessageSecret(ByteString.copyFrom(messageSecret));
                WhatsMessage.WhatsAppMessage.Builder newMessageProto = whatsAppMessage.toBuilder();
                newMessageProto.setMessageContextInfo(messageContextInfo);
                message = newMessageProto.build().toByteArray();
                log.debug("用户:{}, 生成新的message: {}", username, HexUtil.encodeHexStr(message));
            }
        } catch (InvalidProtocolBufferException ignore) {
        }
        try {
            KeyLockUtil.lock(username);
            StringUtil.JidInfo jidInfo = StringUtil.ParseJid(jid);
            List<Integer> deviceids = axolotlManager_.GetSubDeviceSessions(jidInfo.recipientId);

            for (Integer id : deviceids) {
                StringUtil.JidInfo part = jidInfo.Copy();
                part.deviceId = id;
                cipherText.add(axolotlManager_.Encrypt(part, message));
                participant.add(part);
            }
        } catch (Throwable e) {
            handleSendMessageFail(iqId, e.getMessage());
            return null;
        } finally {
            KeyLockUtil.unlock(username);
        }
        try {
            ProtocolTreeNode node = generateEncMessage(jid, participant, cipherText, messageType, mediaType, iqId, retryCount, reportToken);
            return AddTask(node);
        } catch (Exception e) {
            handleSendMessageFail(iqId, e.getMessage());
            log.error("用户: {}, 发送消息异常", username, e);
            return null;
        }
    }

    public String SendToContact(String jid, List<String> jids, byte[] message, String messageType, String mediaType, String iqId, boolean needTrustedContact) {
        ArrayList<CiphertextMessage> cipherText = new ArrayList<>();
        ArrayList<StringUtil.JidInfo> participant = new ArrayList<>();
        for (String id : jids) {
            try {
                KeyLockUtil.lock(username);
                StringUtil.JidInfo jidInfo = StringUtil.ParseJid(id);
                List<Integer> deviceids = axolotlManager_.GetSubDeviceSessions(jidInfo.recipientId);
                for (Integer deviceId : deviceids) {
                    StringUtil.JidInfo part = jidInfo.Copy();
                    part.deviceId = deviceId;
                    cipherText.add(axolotlManager_.Encrypt(part, message));
                    participant.add(part);
                }
            } catch (Throwable e) {
                handleSendMessageFail(iqId, e.getMessage());
                return null;
            } finally {
                KeyLockUtil.unlock(username);
            }
        }
        try {
            if (needTrustedContact) {
                return SendEncMessage(jid, participant, cipherText, messageType, mediaType, iqId, true);
            } else {
                return SendEncMessage(jid, participant, cipherText, messageType, mediaType, iqId);
            }
        } catch (Exception e) {
            handleSendMessageFail(iqId, e.getMessage());
            log.error("用户: {}, 发送消息异常", username, e);
            return null;
        }
    }

    String SendEncMessage(String jid, ArrayList<StringUtil.JidInfo> participant, ArrayList<CiphertextMessage> cipherText, String messageType, String mediaType, String iqId) {
        return SendEncMessage(jid, participant, cipherText, messageType, mediaType, iqId, false);
    }

    public ProtocolTreeNode generateEncMessage(String jid, ArrayList<StringUtil.JidInfo> participant, ArrayList<CiphertextMessage> cipherText, String messageType, String mediaType, String iqId, byte[] reportToken, byte[] tcToken) {
        ProtocolTreeNode msg = new ProtocolTreeNode("message");
        addTcTokenNode(msg, StringUtil.ParseJid(jid), tcToken);
        msg.AddAttribute(new StanzaAttribute("to", jid));
        if (StringUtil.isEmpty(iqId)) {
            iqId = GenerateMessageId();
        }
        if (TypeConstant.TaskType.REVOKE_MESSAGE.equals(messageType)) {
            msg.AddAttribute(new StanzaAttribute("type", "text"));
            msg.AddAttribute(new StanzaAttribute("edit", "7"));
        } else if (TypeConstant.TaskType.REVOKE_REACTION_MESSAGE.equals(messageType)) {
            msg.AddAttribute(new StanzaAttribute("type", "reaction"));
            msg.AddAttribute(new StanzaAttribute("edit", "7"));
        } else {
            msg.AddAttribute(new StanzaAttribute("type", messageType));
        }
        //msg.AddAttribute(new StanzaAttribute("t", String.valueOf(System.currentTimeMillis() / 1000)));
        if (businessVersion && envBuilder_.hasVerifyedName()) {
            msg.AddAttribute(new StanzaAttribute("verified_name", envBuilder_.getVerifyedName()));
        }
        /*if (needTrustedContact) {
            msg.AddChild(new ProtocolTreeNode("url_number"));
        }*/
        if (participant.size() <= 1) {
            ProtocolTreeNode encNode = new ProtocolTreeNode("enc");
            if (messageType.equals("reaction") || TypeConstant.TaskType.REVOKE_REACTION_MESSAGE.equals(messageType)) {
                encNode.AddAttribute(new StanzaAttribute("decrypt-fail", "hide"));
            }
            encNode.AddAttribute(new StanzaAttribute("v", "2"));
            encNode.SetData(cipherText.get(0).serialize());
            if (!StringUtil.isEmpty(mediaType)) {
                encNode.AddAttribute(new StanzaAttribute("mediatype", mediaType));
            }
            switch (cipherText.get(0).getType()) {
                case CiphertextMessage.PREKEY_TYPE: {
                    encNode.AddAttribute(new StanzaAttribute("type", "pkmsg"));
                }
                break;
                case CiphertextMessage.SENDERKEY_TYPE: {
                    encNode.AddAttribute(new StanzaAttribute("type", "skmsg"));
                }
                break;
                default: {
                    encNode.AddAttribute(new StanzaAttribute("type", "msg"));
                }
            }
            msg.AddChild(encNode);
        } else {
            ProtocolTreeNode participants = new ProtocolTreeNode("participants");
            for (int i = 0; i < participant.size(); i++) {
                ProtocolTreeNode to = new ProtocolTreeNode("to");
                to.AddAttribute(new StanzaAttribute("jid", participant.get(i).toString()));

                ProtocolTreeNode encNode = new ProtocolTreeNode("enc");
                encNode.AddAttribute(new StanzaAttribute("v", "2"));
                if (messageType.equals("reaction") || TypeConstant.TaskType.REVOKE_REACTION_MESSAGE.equals(messageType)) {
                    encNode.AddAttribute(new StanzaAttribute("decrypt-fail", "hide"));
                }
                encNode.SetData(cipherText.get(i).serialize());
                if (!StringUtil.isEmpty(mediaType)) {
                    encNode.AddAttribute(new StanzaAttribute("mediatype", mediaType));
                }
                switch (cipherText.get(i).getType()) {
                    case CiphertextMessage.PREKEY_TYPE: {
                        encNode.AddAttribute(new StanzaAttribute("type", "pkmsg"));
                    }
                    break;
                    case CiphertextMessage.SENDERKEY_TYPE: {
                        encNode.AddAttribute(new StanzaAttribute("type", "skmsg"));
                    }
                    break;
                    default: {
                        encNode.AddAttribute(new StanzaAttribute("type", "msg"));
                    }
                }
                //enc node
                to.AddChild(encNode);
                //to node
                participants.AddChild(to);
            }
            msg.AddChild(participants);
        }
        msg.AddAttribute(new StanzaAttribute("id", iqId));
        if (ObjectUtil.isNotNull(reportToken)) {
            ProtocolTreeNode reporting = new ProtocolTreeNode("reporting");
            ProtocolTreeNode reportingToken = new ProtocolTreeNode("reporting_token");
            reportingToken.AddAttribute(new StanzaAttribute("v", "2"));
            reportingToken.SetData(reportToken);
            reporting.AddChild(reportingToken);
            msg.AddChild(reporting);
        }
        return msg;
    }

    public ProtocolTreeNode generateEncMessage(String jid, ArrayList<StringUtil.JidInfo> participant, ArrayList<CiphertextMessage> cipherText, String messageType, String mediaType, String iqId) {
        ProtocolTreeNode msg = new ProtocolTreeNode("message");
        if (StringUtil.isEmpty(iqId)) {
            iqId = GenerateMessageId();
        }
        if (TypeConstant.TaskType.REVOKE_MESSAGE.equals(messageType)) {
            msg.AddAttribute(new StanzaAttribute("type", "text"));
            msg.AddAttribute(new StanzaAttribute("edit", "7"));
        } else if (TypeConstant.TaskType.REVOKE_REACTION_MESSAGE.equals(messageType)) {
            msg.AddAttribute(new StanzaAttribute("type", "reaction"));
            msg.AddAttribute(new StanzaAttribute("edit", "7"));
        } else {
            msg.AddAttribute(new StanzaAttribute("type", messageType));
        }
        msg.AddAttribute(new StanzaAttribute("to", jid));
        //msg.AddAttribute(new StanzaAttribute("t", String.valueOf(System.currentTimeMillis() / 1000)));
        if (businessVersion && envBuilder_.hasVerifyedName()) {
            msg.AddAttribute(new StanzaAttribute("verified_name", envBuilder_.getVerifyedName()));
        }
        /*if (needTrustedContact) {
            msg.AddChild(new ProtocolTreeNode("url_number"));
        }*/
        if (participant.size() <= 1) {
            ProtocolTreeNode encNode = new ProtocolTreeNode("enc");
            if (messageType.equals("reaction") || TypeConstant.TaskType.REVOKE_REACTION_MESSAGE.equals(messageType)) {
                encNode.AddAttribute(new StanzaAttribute("decrypt-fail", "hide"));
            }
            encNode.AddAttribute(new StanzaAttribute("v", "2"));
            encNode.SetData(cipherText.get(0).serialize());
            if (!StringUtil.isEmpty(mediaType)) {
                encNode.AddAttribute(new StanzaAttribute("mediatype", mediaType));
            }
            switch (cipherText.get(0).getType()) {
                case CiphertextMessage.PREKEY_TYPE: {
                    encNode.AddAttribute(new StanzaAttribute("type", "pkmsg"));
                }
                break;
                case CiphertextMessage.SENDERKEY_TYPE: {
                    encNode.AddAttribute(new StanzaAttribute("type", "skmsg"));
                }
                break;
                default: {
                    encNode.AddAttribute(new StanzaAttribute("type", "msg"));
                }
            }
            msg.AddChild(encNode);
        } else {
            ProtocolTreeNode participants = new ProtocolTreeNode("participants");
            for (int i = 0; i < participant.size(); i++) {
                ProtocolTreeNode to = new ProtocolTreeNode("to");
                to.AddAttribute(new StanzaAttribute("jid", participant.get(i).toString()));

                ProtocolTreeNode encNode = new ProtocolTreeNode("enc");
                encNode.AddAttribute(new StanzaAttribute("v", "2"));
                if (messageType.equals("reaction") || TypeConstant.TaskType.REVOKE_REACTION_MESSAGE.equals(messageType)) {
                    encNode.AddAttribute(new StanzaAttribute("decrypt-fail", "hide"));
                }
                encNode.SetData(cipherText.get(i).serialize());
                if (!StringUtil.isEmpty(mediaType)) {
                    encNode.AddAttribute(new StanzaAttribute("mediatype", mediaType));
                }
                switch (cipherText.get(i).getType()) {
                    case CiphertextMessage.PREKEY_TYPE: {
                        encNode.AddAttribute(new StanzaAttribute("type", "pkmsg"));
                    }
                    break;
                    case CiphertextMessage.SENDERKEY_TYPE: {
                        encNode.AddAttribute(new StanzaAttribute("type", "skmsg"));
                    }
                    break;
                    default: {
                        encNode.AddAttribute(new StanzaAttribute("type", "msg"));
                    }
                }
                //enc node
                to.AddChild(encNode);
                //to node
                participants.AddChild(to);
            }
            msg.AddChild(participants);
        }
        msg.AddAttribute(new StanzaAttribute("id", iqId));
        return msg;
    }

    public ProtocolTreeNode generateEncMessage(String jid, ArrayList<StringUtil.JidInfo> participant, ArrayList<CiphertextMessage> cipherText, String messageType, String mediaType, String iqId, String retryCount, byte[] reportToken) {
        ProtocolTreeNode msg = new ProtocolTreeNode("message");
        addTcTokenNode(msg, StringUtil.ParseJid(jid));
        if (hasUploadReport() && ObjectUtil.isNotNull(reportToken)) {
            ProtocolTreeNode reporting = new ProtocolTreeNode("reporting");
            ProtocolTreeNode reportingToken = new ProtocolTreeNode("reporting_token");
            reportingToken.AddAttribute(new StanzaAttribute("v", "2"));
            reportingToken.SetData(reportToken);
            reporting.AddChild(reportingToken);
            msg.AddChild(reporting);
        }
        if (StringUtil.isEmpty(iqId)) {
            iqId = GenerateMessageId();
        }
        if (TypeConstant.TaskType.REVOKE_MESSAGE.equals(messageType)) {
            msg.AddAttribute(new StanzaAttribute("type", "text"));
            msg.AddAttribute(new StanzaAttribute("edit", "7"));
        } else {
            msg.AddAttribute(new StanzaAttribute("type", messageType));
        }
        msg.AddAttribute(new StanzaAttribute("to", jid));
        //msg.AddAttribute(new StanzaAttribute("t", String.valueOf(System.currentTimeMillis() / 1000)));
        if (businessVersion && envBuilder_.hasVerifyedName()) {
            msg.AddAttribute(new StanzaAttribute("verified_name", envBuilder_.getVerifyedName()));
        }
        /*if (needTrustedContact) {
            msg.AddChild(new ProtocolTreeNode("url_number"));
        }*/
        if (participant.size() <= 1) {
            ProtocolTreeNode encNode = new ProtocolTreeNode("enc");
            encNode.AddAttribute(new StanzaAttribute("v", "2"));
            encNode.SetData(cipherText.get(0).serialize());
            encNode.AddAttribute(new StanzaAttribute("count", retryCount));
            if (!StringUtil.isEmpty(mediaType)) {
                encNode.AddAttribute(new StanzaAttribute("mediatype", mediaType));
            }
            switch (cipherText.get(0).getType()) {
                case CiphertextMessage.PREKEY_TYPE: {
                    encNode.AddAttribute(new StanzaAttribute("type", "pkmsg"));
                }
                break;
                case CiphertextMessage.SENDERKEY_TYPE: {
                    encNode.AddAttribute(new StanzaAttribute("type", "skmsg"));
                }
                break;
                default: {
                    encNode.AddAttribute(new StanzaAttribute("type", "msg"));
                }
            }
            msg.AddChild(encNode);
        } else {
            ProtocolTreeNode participants = new ProtocolTreeNode("participants");
            for (int i = 0; i < participant.size(); i++) {
                ProtocolTreeNode to = new ProtocolTreeNode("to");
                to.AddAttribute(new StanzaAttribute("jid", participant.get(i).toString()));

                ProtocolTreeNode encNode = new ProtocolTreeNode("enc");
                encNode.AddAttribute(new StanzaAttribute("v", "2"));
                encNode.SetData(cipherText.get(i).serialize());
                if (!StringUtil.isEmpty(mediaType)) {
                    encNode.AddAttribute(new StanzaAttribute("mediatype", mediaType));
                }
                switch (cipherText.get(i).getType()) {
                    case CiphertextMessage.PREKEY_TYPE: {
                        encNode.AddAttribute(new StanzaAttribute("type", "pkmsg"));
                    }
                    break;
                    case CiphertextMessage.SENDERKEY_TYPE: {
                        encNode.AddAttribute(new StanzaAttribute("type", "skmsg"));
                    }
                    break;
                    default: {
                        encNode.AddAttribute(new StanzaAttribute("type", "msg"));
                    }
                }
                //enc node
                to.AddChild(encNode);
                //to node
                participants.AddChild(to);
            }
            msg.AddChild(participants);
        }
        msg.AddAttribute(new StanzaAttribute("id", iqId));
        return msg;
    }

    String SendEncMessage(String jid, ArrayList<StringUtil.JidInfo> participant, ArrayList<CiphertextMessage> cipherText, String messageType, String mediaType, String iqId, boolean needTrustedContact) {
        ProtocolTreeNode msg = generateEncMessage(jid, participant, cipherText, messageType, mediaType, iqId);
        if (needTrustedContact) {
            return sendMsgTask(msg, (srcNode, result) -> {
                //信任联系人
                trustedContact(jid, GenerateIqId());
                if (result != null && "ack".equals(result.GetTag())) {
                    HandleAck(result);
                }
            });
        } else {
            return sendMsgTask(msg, (srcNode, result) -> {
                if (result != null && "ack".equals(result.GetTag())) {
                    HandleAck(result);
                }
            });
        }
    }

    String SendEncMessage(String jid, ArrayList<StringUtil.JidInfo> participant, ArrayList<CiphertextMessage> cipherText, String messageType, String mediaType, String iqId, boolean needTrustedContact, byte[] reportingToken, byte[] tcToken) {
        ProtocolTreeNode msg = generateEncMessage(jid, participant, cipherText, messageType, mediaType, iqId, reportingToken, tcToken);
        if (needTrustedContact) {
            return sendMsgTask(msg, (srcNode, result) -> {
                //信任联系人
                trustedContact(jid, GenerateIqId());
                if (result != null && "ack".equals(result.GetTag())) {
                    HandleAck(result);
                    String messageId = result.IqId();
                    if (!StringUtils.hasLength(messageId)) {
                        return;
                    }
                    String error = result.GetAttributeValue("error");
                    if (StringUtils.hasLength(error)) {
                        return;
                    }
                    if (WhatsAppUtils.isSystemMessage(messageId)) {
                        return;
                    }
                    String sender = StringUtil.ParseJid(userId).recipientId;
                    String fans = StringUtil.ParseJid(jid).recipientId;
                    String senderCountry = PhoneNumberUtils.getCountry(sender, "US");
                    String receiverCountry = PhoneNumberUtils.getCountry(fans, "US");
                    //上报第一次发送成功记录
                    MessagingMetrics.getInstance().incrementSuccess(senderCountry, receiverCountry);
                    log.info("用户: {},发送记录,sender_{},receiver_{}", username, senderCountry, receiverCountry);
                }
            });
        } else {
            return sendMsgTask(msg, (srcNode, result) -> {
                if (result != null && "ack".equals(result.GetTag())) {
                    HandleAck(result);
                }
            });
        }
    }


    public void sendVoipMsg(String userId, String taskId, String callId, int delay, String tcToken) {
        /**
         * <call to='8618027170662@s.whatsapp.net' id='909D5CF8E6061E8A7676F5D51E79E1A0'>
         *     <offer call-creator='66958129618.0:0@s.whatsapp.net' call-id='880EBA0A6451BA28560BBD18DF31B30F'
         *         device_class='2015'>
         *         <audio rate='8000' enc='opus' />
         *         <audio rate='16000' enc='opus' />
         *         <net medium='3' />
         *         <capability ver='1'>AQT3C84a</capability>
         *         <enc v='2' type='pkmsg'>
         *             MwiFBhIhBRE+8IYJ4rTvOJV4VfxcXvcTontZBz0bx7bxUpfOFQ5HGiEFYl47S8HlKrOoMqMxT8oxn4HuBal5baxV+fvQD58Hmw8icjMKIQVZ6HEY7yA5wTA2AJPYNSPy/iEcVCqTQG0TADBl/pB1AxACGAAiQKLYdXauXiPQPUeo+eQzDPkxH8cuBDXvPxuw/hfFkP1rlmTiP5sF5XLAV8iI+dZUOG3uUgwUzzP3ZIDhyRfa23Wodd1H/R+mHiif2YvCAjDQ9ekH</enc>
         *         <encopt keygen='2' />
         *     </offer>
         * </call>
         */

        /* 视频
        *<call to='8613590316774@s.whatsapp.net' id='9B6F5BF25E399CB9369EF9C595718E94'>
            <offer call-creator='447405860361.0:0@s.whatsapp.net' device_class='2015' call-id='0E5F6D061E2DD3D78D70F65920802A98'>
                <audio rate='8000' enc='opus'/>
                <audio rate='16000' enc='opus'/>
                <video device_orientation='0' dec='H264,VP8,VP9' screen_width='1280' enc='h.264' screen_height='720'/>
                <video device_orientation='0' screen_width='1280' enc='vp8' screen_height='720'/>
                <video device_orientation='0' screen_width='1280' enc='vp8/h.264' screen_height='720'/>
                <net medium='3'/>
                <capability ver='1'>AQT3Ccwa</capability>
                <enc v='2' type='msg'>MwohBffGnedaLOccBYdC2BVVPOS4AjCwohvtWJqFVxj3MqkmEAEYCyJAGQcqDKoX77mYsTs8513J9NwDGOMtouo1PquBiFGfV1CCFt1blP1NfQIAsEnvOhIaU7yfDb7I419tPV+MJCIwBuTM5scdALeE</enc>
                <encopt keygen='2'/>
            </offer>
        </call>

        *
        * */


        ProtocolTreeNode callNode = new ProtocolTreeNode("call");
        callNode.AddAttribute(new StanzaAttribute("to", userId));
        callNode.AddAttribute(new StanzaAttribute("id", taskId));
        ProtocolTreeNode offerNode = new ProtocolTreeNode("offer");
        offerNode.AddAttribute(new StanzaAttribute("call-creator", envBuilder_.getFullphone() + ".0:0@s.whatsapp.net"));
        offerNode.AddAttribute(new StanzaAttribute("call-id", callId));
        offerNode.AddAttribute(new StanzaAttribute("device_class", "2015"));

        ProtocolTreeNode audioNode1 = new ProtocolTreeNode("audio");
        audioNode1.AddAttribute(new StanzaAttribute("rate", "8000"));
        audioNode1.AddAttribute(new StanzaAttribute("enc", "opus"));

        ProtocolTreeNode audioNode2 = new ProtocolTreeNode("audio");
        audioNode2.AddAttribute(new StanzaAttribute("rate", "16000"));
        audioNode2.AddAttribute(new StanzaAttribute("enc", "opus"));

        byte[] token = new byte[]{};
        StringUtil.JidInfo info = StringUtil.ParseJid(userId);
        // 获取联系人token
        try {
            KeyLockUtil.lock(username);
            token = axolotlManager_.trustedContactStore.getToken(info.recipientId);
            log.info("获取到用户privacy: " + cn.hutool.core.codec.Base64.encode(token));
        } catch (Exception ignored) {
        } finally {
            KeyLockUtil.unlock(username);
        }
        if ((ObjectUtil.isNull(token) || token.length == 0) && StrUtil.isNotEmpty(tcToken)) {
            try {
                token = cn.hutool.core.codec.Base64.decode(tcToken);
            } catch (Exception ignored) {
            }
        }
        if (token != null && token.length > 0) {
            ProtocolTreeNode privacy = new ProtocolTreeNode("privacy");
            privacy.SetData(token);
            offerNode.AddChild(privacy);
        }
        //视频节点
        ProtocolTreeNode video = new ProtocolTreeNode("video");
        video.AddAttribute(new StanzaAttribute("device_orientation", "0"));
        video.AddAttribute(new StanzaAttribute("dec", "H264,VP8,VP9"));
        video.AddAttribute(new StanzaAttribute("screen_width", "1280"));
        video.AddAttribute(new StanzaAttribute("enc", "h.264"));
        video.AddAttribute(new StanzaAttribute("screen_height", "720"));

        ProtocolTreeNode video1 = new ProtocolTreeNode("video");
        video1.AddAttribute(new StanzaAttribute("device_orientation", "0"));
        video1.AddAttribute(new StanzaAttribute("screen_width", "1280"));
        video1.AddAttribute(new StanzaAttribute("enc", "vp8"));
        video1.AddAttribute(new StanzaAttribute("screen_height", "720"));

        ProtocolTreeNode video2 = new ProtocolTreeNode("video");
        video2.AddAttribute(new StanzaAttribute("device_orientation", "0"));
        video2.AddAttribute(new StanzaAttribute("screen_width", "1280"));
        video2.AddAttribute(new StanzaAttribute("enc", "vp8/h.264"));
        video2.AddAttribute(new StanzaAttribute("screen_height", "720"));

        ProtocolTreeNode netNode = new ProtocolTreeNode("net");
        netNode.AddAttribute(new StanzaAttribute("medium", "3"));

        ProtocolTreeNode capability = new ProtocolTreeNode("capability");
        capability.AddAttribute(new StanzaAttribute("ver", "1"));
        capability.SetData("AQT3C84a".getBytes(StandardCharsets.UTF_8));

        offerNode.AddChild(audioNode1);
        offerNode.AddChild(audioNode2);
        //视频节点
        offerNode.AddChild(video);
        offerNode.AddChild(video1);
        offerNode.AddChild(video2);


        offerNode.AddChild(netNode);
        offerNode.AddChild(capability);

        ProtocolTreeNode encNode = new ProtocolTreeNode("enc");
        encNode.AddAttribute(new StanzaAttribute("v", "2"));

        WhatsMessage.WhatsAppMessage.Builder builder = WhatsMessage.WhatsAppMessage.newBuilder();
        byte[] callKey = RandomUtil.randomBytes(32);
        String key = CacheConstants.VOIP_CALL_MASTER_KEY + username + ":" + callId;
        String callMasterKey = cn.hutool.core.codec.Base64.encode(callKey);
        log.info("用户: {}, callId: {}, 生成视频加密密钥: {}", username, callId, callMasterKey);
        //master key放入缓存
        RedisService.getInstance().set(key, callMasterKey, 60L);
        builder.getCallBuilder().setCallkey(ByteString.copyFrom(callKey));
        StringUtil.JidInfo jidInfo = StringUtil.ParseJid(userId);
        String jid = jidInfo.getFansId();
        int containsSessionNum = 0;
        try {
            KeyLockUtil.lock(username);
            containsSessionNum = axolotlManager_.containsSessionNum(jidInfo);
        } catch (Exception ignore) {
        } finally {
            KeyLockUtil.unlock(username);
        }
        if (containsSessionNum >= 1) {
            //大于1则代表获取过多设备，则直接发，否则获取一遍多设备
            sendVoipMsg(encNode, offerNode, callNode, userId, taskId, callId, jidInfo, builder, delay);
        } else {
            firstSendMsg(jid, (srcNode, result) -> {
                try {
                    KeyLockUtil.lock(username);
                    int num = axolotlManager_.containsSessionNum(jidInfo);
                    if (num == 0) {
                        taskNotify.setEventContent(taskId, ProtocolTreeNode.fail(Constant.FAIL, Constant.ExceptionReason.FANS_OFFLINE_TOO_LONG));
                        return;
                    }
                } catch (Exception ignore) {
                } finally {
                    KeyLockUtil.unlock(username);
                }
                sendVoipMsg(encNode, offerNode, callNode, userId, taskId, callId, jidInfo, builder, delay);
            });
        }
    }

    public void sendVoipMsg(String userId, String taskId, String callId, int delay, String tcToken, boolean fake) {

        ProtocolTreeNode callNode = new ProtocolTreeNode("call");
        callNode.AddAttribute(new StanzaAttribute("to", userId));
        callNode.AddAttribute(new StanzaAttribute("id", taskId));
        ProtocolTreeNode offerNode = new ProtocolTreeNode("offer");
        offerNode.AddAttribute(new StanzaAttribute("call-creator", envBuilder_.getFullphone() + ".0:0@s.whatsapp.net"));
        offerNode.AddAttribute(new StanzaAttribute("call-id", callId));
        offerNode.AddAttribute(new StanzaAttribute("device_class", "2015"));

        ProtocolTreeNode audioNode1 = new ProtocolTreeNode("audio");
        audioNode1.AddAttribute(new StanzaAttribute("rate", "8000"));
        audioNode1.AddAttribute(new StanzaAttribute("enc", "opus"));

        ProtocolTreeNode audioNode2 = new ProtocolTreeNode("audio");
        audioNode2.AddAttribute(new StanzaAttribute("rate", "16000"));
        audioNode2.AddAttribute(new StanzaAttribute("enc", "opus"));

        byte[] token = new byte[]{};
        StringUtil.JidInfo info = StringUtil.ParseJid(userId);
        // 获取联系人token
        try {
            KeyLockUtil.lock(username);
            token = axolotlManager_.trustedContactStore.getToken(info.recipientId);
            log.info("获取到用户privacy: " + cn.hutool.core.codec.Base64.encode(token));
        } catch (Exception ignored) {
        } finally {
            KeyLockUtil.unlock(username);
        }
        if ((ObjectUtil.isNull(token) || token.length == 0) && StrUtil.isNotEmpty(tcToken)) {
            try {
                token = cn.hutool.core.codec.Base64.decode(tcToken);
            } catch (Exception ignored) {
            }
        }
        if (token != null && token.length > 0) {
            ProtocolTreeNode privacy = new ProtocolTreeNode("privacy");
            privacy.SetData(token);
            offerNode.AddChild(privacy);
        }
        //视频节点
        ProtocolTreeNode video = new ProtocolTreeNode("video");
        video.AddAttribute(new StanzaAttribute("device_orientation", "0"));
        video.AddAttribute(new StanzaAttribute("dec", "H264,VP8,VP9"));
        video.AddAttribute(new StanzaAttribute("screen_width", "1280"));
        video.AddAttribute(new StanzaAttribute("enc", "h.264"));
        video.AddAttribute(new StanzaAttribute("screen_height", "720"));

        ProtocolTreeNode video1 = new ProtocolTreeNode("video");
        video1.AddAttribute(new StanzaAttribute("device_orientation", "0"));
        video1.AddAttribute(new StanzaAttribute("screen_width", "1280"));
        video1.AddAttribute(new StanzaAttribute("enc", "vp8"));
        video1.AddAttribute(new StanzaAttribute("screen_height", "720"));

        ProtocolTreeNode video2 = new ProtocolTreeNode("video");
        video2.AddAttribute(new StanzaAttribute("device_orientation", "0"));
        video2.AddAttribute(new StanzaAttribute("screen_width", "1280"));
        video2.AddAttribute(new StanzaAttribute("enc", "vp8/h.264"));
        video2.AddAttribute(new StanzaAttribute("screen_height", "720"));

        ProtocolTreeNode netNode = new ProtocolTreeNode("net");
        netNode.AddAttribute(new StanzaAttribute("medium", "3"));

        ProtocolTreeNode capability = new ProtocolTreeNode("capability");
        capability.AddAttribute(new StanzaAttribute("ver", "1"));
        capability.SetData("AQT3C84a".getBytes(StandardCharsets.UTF_8));

        offerNode.AddChild(audioNode1);
        offerNode.AddChild(audioNode2);
        //视频节点
        offerNode.AddChild(video);
        offerNode.AddChild(video1);
        offerNode.AddChild(video2);


        offerNode.AddChild(netNode);
        offerNode.AddChild(capability);

        ProtocolTreeNode encNode = new ProtocolTreeNode("enc");
        encNode.AddAttribute(new StanzaAttribute("v", "2"));

        WhatsMessage.WhatsAppMessage.Builder builder = WhatsMessage.WhatsAppMessage.newBuilder();
        byte[] callKey = RandomUtil.randomBytes(32);
        String key = CacheConstants.VOIP_CALL_MASTER_KEY + username + ":" + callId;
        String callMasterKey = cn.hutool.core.codec.Base64.encode(callKey);
        log.info("用户: {}, callId: {}, 生成视频加密密钥: {}", username, callId, callMasterKey);
        //master key放入缓存
        RedisService.getInstance().set(key, callMasterKey, 60L);
        builder.getCallBuilder().setCallkey(ByteString.copyFrom(callKey));
        StringUtil.JidInfo jidInfo = StringUtil.ParseJid(userId);
        String jid = jidInfo.getFansId();
        int containsSessionNum = 0;
        try {
            KeyLockUtil.lock(username);
            containsSessionNum = axolotlManager_.containsSessionNum(jidInfo);
        } catch (Exception ignore) {
        } finally {
            KeyLockUtil.unlock(username);
        }
        if (containsSessionNum >= 1) {
            //大于1则代表获取过多设备，则直接发，否则获取一遍多设备
            sendVoipMsg(encNode, offerNode, callNode, userId, taskId, callId, jidInfo, builder, delay);
        } else {
            if (fake) {
                // 如果没有过session, 说明为新粉丝，则走假session发送
                try {
                    KeyLockUtil.lock(username);
                    axolotlManager_.CreateFakeSession(jidInfo.recipientId, jidInfo.deviceId, "CrwFCAMSIQXqvHX2AbVKTAKeFxPtuaV4PHmMAElwE4SWsQhBRkFSFxohBYxjza1/xPn8JLeJqWirFjJUPtbT7qcrELDnVAiDRyk7IiCl5tJ+gOQUvBxn+K7VlBcI2tansSmwbv3posCfKUHZhSgAMmsKIQWojQyCNKxVmAcxaE6ISjKFeiYAVhi+ccpBs6gNjeohUhIg8AVD5LBcbAaUDD/TGm5drQ6pS9nRv0q0Kvyo9TqzbVEaJAgBEiCSXi5UzZjB+gQ4Lr9Gw9tzW0FHN7uiWJ1vcV7IonLiijqxAwohBeaV8Fm3wynnyYSXBMVysgQLteaNZ5u3gvpcaUxF2BwoGiQIBRIgYpxAsi3+c0oaux0O3OQYBqgjWcdn5hlMLOPaBeTgq70iWAgAEiCDmkMsGV+HQ7n7ZAKuKlr2RKlieKUxj9yAXWjpJjl6hBogpQCGkvAdoPOYieLCEDv8jNqSqAHvG5VENJvCsoc4pmEiEFw3fTJN2PXYUqzgiGWb660iWAgBEiAUz/tZYfkMivgtpH8eN4IWYUrY/Iz2Po2CfTwvMR0c2xogx8yRzgQOp5NA+kuZEDbUI8DwHChT6XruKKjOIa4CQQwiENQZwD5Mg2ctNVx/HxVH218iWAgCEiCM5FiBOETUM7y+dglpWLjKuzdvnHgWXoA7bHr6HanRrBogb4mR7BLafSrbbkXLkaJmZCA0GpZmd6FOj2tWCVTyOjkiEA+2kvNQFifwtJTWBWY3uyQiWAgDEiB7E0pCuOJpNCC1Fv7iQSFaJkO9s3gevRl0Go7KqkIfRxogyu6D1tMIXzsL5HJh8FyOtiUO/CheW9I6zUQBUhtMQlciEPYQbZkculzut7D+CjaWaEFQg8XcwQZYuK2u5AdqIQWHj0Ux/SUTgMX1/Rz3vDATn0ejVu98TdFeiT+hMHAQCg==");
                    int num = axolotlManager_.containsSessionNum(jidInfo);
                    if (num == 0) {
                        taskNotify.setEventContent(taskId, ProtocolTreeNode.fail(Constant.FAIL, Constant.ExceptionReason.FANS_OFFLINE_TOO_LONG));
                        return;
                    }
                } catch (Exception ignore) {
                } finally {
                    KeyLockUtil.unlock(username);
                }
                sendVoipMsg(encNode, offerNode, callNode, userId, taskId, callId, jidInfo, builder, delay);
                return;
            }
            firstSendMsg(jid, (srcNode, result) -> {
                try {
                    KeyLockUtil.lock(username);
                    int num = axolotlManager_.containsSessionNum(jidInfo);
                    if (num == 0) {
                        taskNotify.setEventContent(taskId, ProtocolTreeNode.fail(Constant.FAIL, Constant.ExceptionReason.FANS_OFFLINE_TOO_LONG));
                        return;
                    }
                } catch (Exception ignore) {
                } finally {
                    KeyLockUtil.unlock(username);
                }
                sendVoipMsg(encNode, offerNode, callNode, userId, taskId, callId, jidInfo, builder, delay);
            });
        }
    }


    public void sendVoipVoiceMsg(String userId, String taskId, String callId) {
        ProtocolTreeNode callNode = new ProtocolTreeNode("call");
        callNode.AddAttribute(new StanzaAttribute("to", userId));
        callNode.AddAttribute(new StanzaAttribute("id", taskId));
        ProtocolTreeNode offerNode = new ProtocolTreeNode("offer");
        offerNode.AddAttribute(new StanzaAttribute("call-creator", envBuilder_.getFullphone() + ".0:0@s.whatsapp.net"));
        offerNode.AddAttribute(new StanzaAttribute("call-id", callId));
        offerNode.AddAttribute(new StanzaAttribute("device_class", "2015"));

        ProtocolTreeNode audioNode1 = new ProtocolTreeNode("audio");
        audioNode1.AddAttribute(new StanzaAttribute("rate", "8000"));
        audioNode1.AddAttribute(new StanzaAttribute("enc", "opus"));

        ProtocolTreeNode audioNode2 = new ProtocolTreeNode("audio");
        audioNode2.AddAttribute(new StanzaAttribute("rate", "16000"));
        audioNode2.AddAttribute(new StanzaAttribute("enc", "opus"));

        byte[] token = new byte[]{};
        StringUtil.JidInfo info = StringUtil.ParseJid(userId);
        // 获取联系人token
        try {
            KeyLockUtil.lock(username);
            token = axolotlManager_.trustedContactStore.getToken(info.recipientId);
            log.info("获取到用户privacy: " + cn.hutool.core.codec.Base64.encode(token));
        } catch (Exception ignored) {
        } finally {
            KeyLockUtil.unlock(username);
        }
        if (token != null && token.length > 0) {
            ProtocolTreeNode privacy = new ProtocolTreeNode("privacy");
            privacy.SetData(token);
            offerNode.AddChild(privacy);
        }

        ProtocolTreeNode netNode = new ProtocolTreeNode("net");
        netNode.AddAttribute(new StanzaAttribute("medium", "3"));

        ProtocolTreeNode capability = new ProtocolTreeNode("capability");
        capability.AddAttribute(new StanzaAttribute("ver", "1"));
        capability.SetData("AQT3CcT6".getBytes(StandardCharsets.UTF_8));

        offerNode.AddChild(audioNode1);
        offerNode.AddChild(audioNode2);

        offerNode.AddChild(netNode);
        offerNode.AddChild(capability);

        ProtocolTreeNode encNode = new ProtocolTreeNode("enc");
        encNode.AddAttribute(new StanzaAttribute("v", "2"));

        WhatsMessage.WhatsAppMessage.Builder builder = WhatsMessage.WhatsAppMessage.newBuilder();
        byte[] callKey = RandomUtil.randomBytes(32);
        String key = CacheConstants.VOIP_CALL_MASTER_KEY + username + ":" + callId;
        String callMasterKey = cn.hutool.core.codec.Base64.encode(callKey);
        log.info("用户: {}, callId: {}, 生成语音加密密钥: {}", username, callId, callMasterKey);
        //master key放入缓存
        RedisService.getInstance().set(key, callMasterKey, 60L);
        builder.getCallBuilder().setCallkey(ByteString.copyFrom(callKey));
        StringUtil.JidInfo jidInfo = StringUtil.ParseJid(userId);
        String jid = jidInfo.getFansId();
        int containsSessionNum = 0;
        try {
            KeyLockUtil.lock(username);
            containsSessionNum = axolotlManager_.containsSessionNum(jidInfo);
        } catch (Exception ignore) {
        } finally {
            KeyLockUtil.unlock(username);
        }
        if (containsSessionNum >= 1) {
            //大于1则代表获取过多设备，则直接发，否则获取一遍多设备
            sendVoipMsg(encNode, offerNode, callNode, taskId, jidInfo, builder);
        } else {
            firstSendMsg(jid, (srcNode, result) -> {
                try {
                    KeyLockUtil.lock(username);
                    int num = axolotlManager_.containsSessionNum(jidInfo);
                    if (num == 0) {
                        taskNotify.setEventContent(taskId, ProtocolTreeNode.fail(Constant.FAIL, Constant.ExceptionReason.FANS_OFFLINE_TOO_LONG));
                        return;
                    }
                } catch (Exception ignore) {
                } finally {
                    KeyLockUtil.unlock(username);
                }
                sendVoipMsg(encNode, offerNode, callNode, taskId, jidInfo, builder);
            });
        }
    }

    private void sendVoipMsg(ProtocolTreeNode encNode, ProtocolTreeNode offerNode, ProtocolTreeNode callNode, String taskId, StringUtil.JidInfo jidInfo, WhatsMessage.WhatsAppMessage.Builder builder) {
        //如果是陌生人，需要走 sync 获取对方的key，这里是为了简单测试，直接加密发送， 正常流程和 发送普通消息一样
        CiphertextMessage text;
        try {
            text = axolotlManager_.Encrypt(jidInfo, builder.build().toByteArray());
        } catch (Throwable e) {
            log.error("用户：{}，sendVoipMsg加密异常", username, e);
            taskNotify.setEventContent(taskId, ProtocolTreeNode.fail(Constant.FAIL, "粉丝存在问题"));
            return;
        }
        encNode.SetData(text.serialize());

        switch (text.getType()) {
            case CiphertextMessage.PREKEY_TYPE: {
                encNode.AddAttribute(new StanzaAttribute("type", "pkmsg"));
            }
            break;
            case CiphertextMessage.SENDERKEY_TYPE: {
                encNode.AddAttribute(new StanzaAttribute("type", "skmsg"));
            }
            break;
            default: {
                encNode.AddAttribute(new StanzaAttribute("type", "msg"));
            }
        }
        //enc node
        offerNode.AddChild(encNode);

        ProtocolTreeNode encoptNode = new ProtocolTreeNode("encopt");
        encoptNode.AddAttribute(new StanzaAttribute("keygen", "2"));
        offerNode.AddChild(encoptNode);
        callNode.AddChild(offerNode);
        AddTask(callNode);
        taskNotify.setEventContent(taskId, ProtocolTreeNode.success(Constant.OK));
    }

    private void sendVoipMsg(ProtocolTreeNode encNode, ProtocolTreeNode offerNode, ProtocolTreeNode callNode, String userId, String taskId, String callId, StringUtil.JidInfo jidInfo, WhatsMessage.WhatsAppMessage.Builder builder, int delay) {
        if (delay > 15) {
            delay = 15;
        }
        //如果是陌生人，需要走 sync 获取对方的key，这里是为了简单测试，直接加密发送， 正常流程和 发送普通消息一样
        CiphertextMessage text = null;
        try {
            text = axolotlManager_.Encrypt(jidInfo, builder.build().toByteArray());
        } catch (Throwable e) {
            log.error("用户：{}，sendVoipMsg加密异常", username, e);
            taskNotify.setEventContent(taskId, ProtocolTreeNode.fail(Constant.FAIL, "粉丝存在问题"));
            return;
        }
        encNode.SetData(text.serialize());

        switch (text.getType()) {
            case CiphertextMessage.PREKEY_TYPE: {
                encNode.AddAttribute(new StanzaAttribute("type", "pkmsg"));
            }
            break;
            case CiphertextMessage.SENDERKEY_TYPE: {
                encNode.AddAttribute(new StanzaAttribute("type", "skmsg"));
            }
            break;
            default: {
                encNode.AddAttribute(new StanzaAttribute("type", "msg"));
            }
        }
        //enc node
        offerNode.AddChild(encNode);

        ProtocolTreeNode encoptNode = new ProtocolTreeNode("encopt");
        encoptNode.AddAttribute(new StanzaAttribute("keygen", "2"));
        offerNode.AddChild(encoptNode);
        callNode.AddChild(offerNode);
        int finalDelay = delay;
        AddTask(callNode, (srcNode, result) -> {
            //添加延时关闭语音视频通话消息
            if (finalDelay > 0) {
                DelayExecuteTask.hashedWheelTimer.newTimeout(timeout -> {
                    //taskId
                    ProtocolTreeNode call = new ProtocolTreeNode("call");
                    call.AddAttribute(new StanzaAttribute("to", userId));
                    if (iosLogin) {
                        call.AddAttribute(new StanzaAttribute("id", GenerateIqId()));
                    } else {
                        call.AddAttribute(new StanzaAttribute("id", IdUtil.simpleUUID().toUpperCase()));
                    }
                    ProtocolTreeNode terminateNode = new ProtocolTreeNode("terminate");
                    terminateNode.AddAttribute(new StanzaAttribute("call-creator", envBuilder_.getFullphone() + ".0:0@s.whatsapp.net"));
                    terminateNode.AddAttribute(new StanzaAttribute("reason", "timeout"));
                    terminateNode.AddAttribute(new StanzaAttribute("call-id", callId));
                    call.AddChild(terminateNode);
                    AddTask(call, (srcNode1, result1) -> {
                    });
                }, finalDelay, TimeUnit.SECONDS);
            }
            HandleAck(result);
        });
    }

    private int iqid = 0;

    public String GenerateIqId() {
        if (iosLogin) {
            synchronized (this) {
                iqid++;
                return loginTime + "-" + iqid;
            }
        } else {
            return generatePingId();
        }
    }

    String GenerateMessageId() {
        return IdUtil.simpleUUID().toUpperCase();
    }

    /**
     * 发送媒体信息
     *
     * @param jid                userId
     * @param path               文件路径
     * @param mediaType          媒体类型，图片：image，视频消息：video，语音消息:ppt，文件消息：document
     * @param id                 任务id
     * @param fileName           文件名称，可以不传，传的话必须带后缀
     * @param caption            说明文字
     * @param contextInfoBuilder 引用消息
     * @return
     */
    public String SendMedia(String jid, String path, String mediaType, String id, String fileName, String caption, WhatsMessage.WhatsAppContextInfo.Builder contextInfoBuilder, long socketTimeout, String tcToken) {
        JSONObject mediaInfo = new JSONObject();
        String taskId = uploadMedia(path, mediaType, mediaInfo, id, fileName, caption, socketTimeout);
        if (StringUtils.isEmpty(taskId)) {
            return null;
        }
        submitMediaRequest(jid, null, mediaInfo, id, contextInfoBuilder, tcToken);
        return id;
    }

    public String SendMedia(List<String> userIds, String path, String mediaType, String id, String fileName, String caption, WhatsMessage.WhatsAppContextInfo.Builder contextInfoBuilder, long socketTimeout) {
        JSONObject mediaInfo = new JSONObject();
        String taskId = uploadMedia(path, mediaType, mediaInfo, id, fileName, caption, socketTimeout);
        if (StringUtils.isEmpty(taskId)) {
            return null;
        }
        submitMediaRequest(null, userIds, mediaInfo, id, contextInfoBuilder);
        return id;
    }

    public String uploadMedia(String path, String mediaType, JSONObject mediaInfo, String id, String fileName, String caption, long socketTimeout) {
        File file = new File(path);
        if (!file.exists()) {
            handleUploadFail(id, "上传文件为空");
            return "";
        }
        StringBuilder sb = new StringBuilder("https://");
        sb.append(cdnHost_);
        String mime;
        switch (mediaType) {
            case "image": {
                mime = "image/jpeg";
                sb.append("/mms/image");
                mediaInfo.put("caption", caption);
                boolean success = GetImageThumb(path, mediaInfo);
                if (!success) {
                    handleUploadFail(id, "获取图片缩略图失败");
                    return "";
                }
                break;
            }
            case "video": {
                mime = "video/mp4";
                sb.append("/mms/video");
                mediaInfo.put("caption", caption);
                boolean success = GetVideoThumb(path, mediaInfo, false);
                if (!success) {
                    handleUploadFail(id, "获取视频截图失败");
                    return "";
                }
                break;
            }
            case "ptt": {
                mime = "audio/ogg; codecs=opus";
                sb.append("/mms/audio");
                path = getOpusVoice(path, mediaInfo);
                if (StringUtils.isEmpty(path)) {
                    handleUploadFail(id, "语音格式转换失败");
                    return "";
                }
                break;
            }
            case "document": {
                sb.append("/mms/document");
                String ext = FileUtil.getSuffix(file);
                if (StringUtil.isEmpty(ext)) {
                    mime = "text/txt";
                } else {
                    mime = "text/" + ext;
                }
                break;
            }
            case "gif": {
                sb.append("/mms/gif");
                mime = "video/mp4";
                boolean success = GetVideoThumb(path, mediaInfo, true);
                if (!success) {
                    handleUploadFail(id, "获取视频截图失败");
                    return "";
                }
                break;
            }
            default: {
                handleUploadFail(id, "发送类型错误");
                return "";
            }
        }
        MediaCipher.MediaEncryptInfo encryptInfo = MediaCipher.encrypt(path, mediaType);
        mediaInfo.put("media_type", mediaType);
        mediaInfo.put("mime", mime);
        mediaInfo.put("encrypt_hash", Base64.getEncoder().encodeToString(encryptInfo.contentHash));
        mediaInfo.put("orign_hash", Base64.getEncoder().encodeToString(encryptInfo.origHash));
        mediaInfo.put("file_len", file.length());
        mediaInfo.put("media_key", encryptInfo.mediaKey);
        if (StringUtils.hasLength(fileName)) {
            mediaInfo.put("file_name", fileName);
            mediaInfo.put("title", FileUtil.getPrefix(fileName));
        } else {
            mediaInfo.put("file_name", file.getName());
            mediaInfo.put("title", FileUtil.getPrefix(file));
        }
        sb.append("/").append(encryptInfo.token).append("?")
                .append("auth=").append(cdnAuthKey_)
                .append("&token=").append(encryptInfo.token);

        ProxyInfo proxyInfo = null;
        if (proxy_ != null) {
            proxyInfo = new ProxyInfo();
            proxyInfo.setType(proxy_.type);
            proxyInfo.setProxyHost(proxy_.server);
            proxyInfo.setProxyPort(proxy_.port);
            proxyInfo.setProxyUser(proxy_.userName);
            proxyInfo.setProxyPwd(proxy_.password);
        }
        DefaultHttpHeaders entries = new DefaultHttpHeaders();
        entries.set(HttpHeaderNames.USER_AGENT, GetUserAgent());
        HttpClientUtil.ResponseResult responseResult = null;
        for (int i = 0; i < 3; i++) {
            responseResult = HttpClientUtil
                    .builder()
                    .proxyInfo(proxyInfo)
                    .headers(entries)
                    .url(sb.toString())
                    .connectTimeoutMillis(6000)
                    .responseTimeout(Duration.ofMillis(socketTimeout))
                    .data(encryptInfo.data)
                    .build()
                    .post();
            if (responseResult.isSuccess()) {
                break;
            } else {
                if (!HttpClientUtil.REQUEST_EXCEPTION.equals(responseResult.getErrMsg())) {
                    break;
                }
            }
        }
        if (!responseResult.isSuccess()) {
            handleUploadFail(id, responseResult.getErrMsg());
            return "";
        }
        String content = responseResult.getResultString();
        try {
            JSONObject res = JSONObject.parseObject(content);
            String url = res.getString("url");
            String directPath = res.getString("direct_path");
            if (StringUtils.hasLength(url) && StringUtils.hasLength(directPath)) {
                mediaInfo.put("url", url);
                mediaInfo.put("direct_path", directPath);
            } else {
                handleUploadFail(id, "上传媒体失败");
                return "";
            }
        } catch (Exception ignored) {
            handleUploadFail(id, "上传媒体失败");
            return "";
        }
        return id;
    }

    /**
     * 发送聊天输入中开始标识
     */
    public void sendChatComposing(String jid) {
        ProtocolTreeNode chatState = new ProtocolTreeNode("chatstate");
        chatState.AddAttribute(new StanzaAttribute("to", JidNormalize(jid)));
        ProtocolTreeNode composing = new ProtocolTreeNode("composing");
        chatState.AddChild(composing);
        AddTask(chatState);
    }

    /**
     * 发送聊天框语音录制中
     */
    public void sendVoiceRecording(String jid) {
        ProtocolTreeNode chatState = new ProtocolTreeNode("chatstate");
        chatState.AddAttribute(new StanzaAttribute("to", JidNormalize(jid)));
        ProtocolTreeNode composing = new ProtocolTreeNode("composing");
        composing.AddAttribute(new StanzaAttribute("media", "audio"));
        chatState.AddChild(composing);
        AddTask(chatState);
    }

    /**
     * 发送聊天结束标识
     */
    public void sendChatPaused(String jid) {
        ProtocolTreeNode chatState = new ProtocolTreeNode("chatstate");
        chatState.AddAttribute(new StanzaAttribute("to", JidNormalize(jid)));
        ProtocolTreeNode paused = new ProtocolTreeNode("paused");
        chatState.AddChild(paused);
        AddTask(chatState);
    }

    /**
     * 设置pb数据昵称
     */
    public void setPbNickname(String nickname) {
        envBuilder_.setPushname(nickname);
        axolotlManager_.SetBytesSetting("env", envBuilder_.build().toByteArray());
    }

    /**
     * 处理上传媒体文件失败
     */
    private void handleUploadFail(String taskId, String desc) {
        handleFail("SendMedia", taskId, desc);
    }

    /**
     * 处理发送消息失败
     */
    private void handleSendMessageFail(String taskId, String desc) {
        handleFail("sendMessageFail", taskId, desc);
//        log.info("username:{} Repair acceptance information",username);
//        ThreadPoolConfig.gcmAndGooglePoolExecutor.execute(()->callGcmAndGoogle(username));

    }

    /**
     * 处理失败
     */
    private void handleFail(String type, String taskId, String desc) {
        ProtocolTreeNode failedNode = new ProtocolTreeNode("iq");
        failedNode.AddAttribute(new StanzaAttribute("id", taskId));
        ProtocolTreeNode error = new ProtocolTreeNode("error");
        error.AddAttribute(new StanzaAttribute("desc", desc));
        failedNode.AddChild(error);
        if (delegate_ != null) {
            delegate_.OnPacketResponse(type, failedNode);
        }
    }

    public void addTaskToQueue(Runnable runnable) {
        taskNotify(this.username, runnable);
    }

    public void handResponseMsg(String type, ProtocolTreeNode protocolTreeNode) {
        if ("LeaveGroup".equals(type)) {
            //<iq from='g.us' type='result' id='AF8119F0A2854983BE7A42AEDC8A4FEC'><leave><group id='120363038947378900@g.us'/></leave></iq>
            ProtocolTreeNode leaveNode = protocolTreeNode.getOneChildren("leave");
            if (leaveNode != null) {
                ProtocolTreeNode groupNode = leaveNode.getOneChildren("group");
                if (groupNode != null) {
                    String groupId = groupNode.GetAttributeValue("id");
                    if (StringUtils.hasLength(groupId)) {
                        try {
                            KeyLockUtil.lock(username);
                            axolotlManager_.senderKeyStore_.deleteGroupRecord(groupId);
                            axolotlManager_.deletePreGroupFans(groupId);
                        } catch (Exception ignored) {
                        } finally {
                            KeyLockUtil.unlock(username);
                        }

                    }
                }
            }
        }
    }

    /**
     * 移除多设备设备
     *
     * @param userId 用户id
     * @param taskId 任务id
     */
    public void removeCompanionDevice(String userId, String taskId) {
        ProtocolTreeNode node = new ProtocolTreeNode("iq");
        node.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        node.AddAttribute(new StanzaAttribute("id", taskId));
        node.AddAttribute(new StanzaAttribute("xmlns", "md"));
        node.AddAttribute(new StanzaAttribute("type", "set"));
        ProtocolTreeNode removeDeviceNode = new ProtocolTreeNode("remove-companion-device");
        removeDeviceNode.AddAttribute(new StanzaAttribute("jid", userId));
        removeDeviceNode.AddAttribute(new StanzaAttribute("reason", "user_initiated"));
        node.AddChild(removeDeviceNode);
        AddTask(node);
    }

    public String ScanWebWhatsapp(String qrcode, String taskId) {
        QRCodeInfo codeInfo = QRCodeInfo.ParseQRcode(qrcode);
        WhatsMessage.Qrcode3LT.Builder A002 = WhatsMessage.Qrcode3LT.newBuilder();
        A002.setAdvRawId(axolotlManager_.GetAdvRawId());
        A002.setAdvCurrentKeyIndex(1);
        A002.setTimestamp(System.currentTimeMillis() / 1000);

        AnonymousClass0DP r0 = codeInfo.xpub_key;
        WhatsMessage.Qrcode3LY A012 = C05350Mv.A01(C05350Mv.A04(axolotlManager_, A002.build(), r0.A00.key), codeInfo.part_4);

        WhatsMessage.Qrcode3LU A032 = C05350Mv.A03(A002.build());
        WhatsMessage.Qrcode3LV A052 = C05350Mv.A05(axolotlManager_, A032);


        ProtocolTreeNode node = new ProtocolTreeNode("iq");
        node.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        node.AddAttribute(new StanzaAttribute("id", taskId));
        node.AddAttribute(new StanzaAttribute("xmlns", "md"));
        node.AddAttribute(new StanzaAttribute("type", "set"));

        ProtocolTreeNode pair_device = new ProtocolTreeNode("pair-device");
        {
            ProtocolTreeNode value = new ProtocolTreeNode("ref");
            value.SetData(codeInfo.ref.getBytes(StandardCharsets.UTF_8));
            pair_device.AddChild(value);
        }
        {
            ProtocolTreeNode value = new ProtocolTreeNode("pub-key");
            value.SetData(codeInfo.pub_key);
            pair_device.AddChild(value);
        }
        {
            ProtocolTreeNode value = new ProtocolTreeNode("device-identity");
            value.SetData(A012.toByteArray());
            pair_device.AddChild(value);
        }
        {
            ProtocolTreeNode value = new ProtocolTreeNode("key-index-list");
            value.AddAttribute(new StanzaAttribute("ts", String.valueOf(A032.getTimestamp())));
            value.SetData(A052.toByteArray());
            pair_device.AddChild(value);
        }

        node.AddChild(pair_device);
        return AddTask(node, (srcNode, result) -> {
            taskNotify.setEventContent(taskId, result);
            //<iq from='s.whatsapp.net' type='result' id='09'><companion-props>CgdXaW5kb3dzEgIIChgBIAA=</companion-props><device jid='959401677350.0:8@s.whatsapp.net'/></iq>
            axolotlManager_.SetAdvRawId(axolotlManager_.GetAdvRawId() + 1);
            ProtocolTreeNode device = result.getOneChildren("device");
            if (null == device) {
                device = result.getOneChildren("media.fagr1-1.fna.whatsapp.net");
                if (device == null) {
                    return;
                }
            }
            webJid_ = device.GetAttributeValue("jid");
        });
    }

    String webJid_;

    private void HandleSyncAccount(ProtocolTreeNode node) {
        ProtocolTreeNode devices = node.GetChild("devices");
        if (null == devices) {
            return;
        }
        /*axolotlManager_.ClearDevice();
        LinkedList<ProtocolTreeNode> device_list = devices.GetChildren("device");
        int max_index = 0;
        for (ProtocolTreeNode device : device_list) {
            String index = device.GetAttributeValue("key-index");
            if (StringUtil.isEmpty(index)) {
                continue;
            }
            int current_index = Integer.valueOf(index);
            if (current_index > max_index) {
                max_index =current_index;
            }
            axolotlManager_.InsertDevice(device.GetAttributeValue("jid"), current_index);
        }
        if (0 != max_index) {
            axolotlManager_.SetAdvCurrentKeyIndex(max_index);
        }*/
        if (StringUtils.isEmpty(webJid_)) {
            return;
        }
        LinkedList<String> jidList = new LinkedList<>();
        jidList.add(webJid_);
        GetKeysFor(jidList, new HandleWebSync());
    }


    class HandleWebSync implements NodeCallback {
        @Override
        public void Run(ProtocolTreeNode srcNode, ProtocolTreeNode result) {
            SendOnePacket();
        }
    }

    public void PresenceWeb() {
        //<presence type='probe' to='8617748754950.0:10@s.whatsapp.net'/>
        ProtocolTreeNode chatState = new ProtocolTreeNode("presence");
        chatState.AddAttribute(new StanzaAttribute("type", "probe"));
        chatState.AddAttribute(new StanzaAttribute("to", webJid_));
        AddTask(chatState);
    }

    /**
     * 更新隐私设置
     */
    public void UpdatePrivacySetting(String name, String value, String taskId) {
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        iq.AddAttribute(new StanzaAttribute("type", "set"));
        iq.AddAttribute(new StanzaAttribute("id", taskId));
        iq.AddAttribute(new StanzaAttribute("xmlns", "privacy"));
        ProtocolTreeNode privacy = new ProtocolTreeNode("privacy");
        ProtocolTreeNode category = new ProtocolTreeNode("category");
        category.AddAttribute(new StanzaAttribute("name", name));
        category.AddAttribute(new StanzaAttribute("value", value));
        privacy.AddChild(category);
        iq.AddChild(privacy);
        AddTask(iq);
    }

    public String uploadMediaData(byte[] data, String mediaType, JSONObject mediaInfo, String id, String fileName) {
        MediaCipher.MediaEncryptInfo encryptInfo = MediaCipher.encryptMemory(data, mediaType);
        StringBuilder sb = new StringBuilder("https://");
        sb.append(cdnHost_);
        String mime;
        switch (mediaType) {
            case "image": {
                mime = "image/jpeg";
                sb.append("/mms/image");
                break;
            }
            case "video": {
                mime = "video/mp4";
                sb.append("/mms/video");
                break;
            }
            case "ptt": {
                mime = "audio/ogg; codecs=opus";
                sb.append("/mms/audio");
                break;
            }
            case "document": {
                sb.append("/mms/document");
                String ext = FileUtil.getSuffix(fileName);
                if (StringUtil.isEmpty(ext)) {
                    mime = "text/txt";
                } else {
                    mime = "text/" + ext;
                }
                break;
            }
            case "history": {
                mime = "application/octet-stream";
                sb.append("/mms/md-msg-hist");
            }
            break;
            default: {
                handleUploadFail(id, "发送类型错误");
                return "";
            }
        }
        mediaInfo.put("media_type", mediaType);
        mediaInfo.put("mime", mime);
        mediaInfo.put("encrypt_hash", Base64.getEncoder().encodeToString(encryptInfo.contentHash));
        mediaInfo.put("orign_hash", Base64.getEncoder().encodeToString(encryptInfo.origHash));
        mediaInfo.put("file_len", data.length);
        mediaInfo.put("media_key", encryptInfo.mediaKey);
        if (StringUtils.hasLength(fileName)) {
            mediaInfo.put("file_name", fileName);
            mediaInfo.put("title", FileUtil.getPrefix(fileName));
        } else {
            mediaInfo.put("file_name", fileName);
            mediaInfo.put("title", FileUtil.getPrefix(fileName));
        }
        sb.append("/").append(encryptInfo.token).append("?")
                .append("auth=").append(cdnAuthKey_)
                .append("&token=").append(encryptInfo.token);
        ProxyInfo proxyInfo = null;
        if (proxy_ != null) {
            proxyInfo = new ProxyInfo();
            proxyInfo.setType(proxy_.type);
            proxyInfo.setProxyHost(proxy_.server);
            proxyInfo.setProxyPort(proxy_.port);
            proxyInfo.setProxyUser(proxy_.userName);
            proxyInfo.setProxyPwd(proxy_.password);
        }
        DefaultHttpHeaders entries = new DefaultHttpHeaders();
        entries.set(HttpHeaderNames.USER_AGENT, GetUserAgent());
        HttpClientUtil.ResponseResult responseResult = HttpClientUtil
                .builder()
                .proxyInfo(proxyInfo)
                .headers(entries)
                .url(sb.toString())
                .connectTimeoutMillis(5000)
                .responseTimeout(Duration.ofSeconds(40))
                .data(encryptInfo.data)
                .build()
                .post();
        if (!responseResult.isSuccess()) {
            handleUploadFail(id, responseResult.getErrMsg());
            return "";
        }
        String content = responseResult.getResultString();
        try {
            JSONObject res = JSONObject.parseObject(content);
            String url = res.getString("url");
            String directPath = res.getString("direct_path");
            if (StringUtils.hasLength(url) && StringUtils.hasLength(directPath)) {
                mediaInfo.put("url", url);
                mediaInfo.put("direct_path", directPath);
            } else {
                handleUploadFail(id, "上传媒体失败");
                return "";
            }
        } catch (Exception ignored) {
            handleUploadFail(id, "上传媒体失败");
            return "";
        }
        return id;
    }


    void SendToMyWeb(String jid, byte[] data, boolean high, String type) {
        ProtocolTreeNode msg = new ProtocolTreeNode("message");
        msg.AddAttribute(new StanzaAttribute("to", jid));
        msg.AddAttribute(new StanzaAttribute("type", "text"));
        msg.AddAttribute(new StanzaAttribute("id", GenerateIqId()));
        msg.AddAttribute(new StanzaAttribute("category", "peer"));
        if (high) {
            msg.AddAttribute(new StanzaAttribute("push_priority", "high"));
        }
        ProtocolTreeNode encNode = new ProtocolTreeNode("enc");
        encNode.AddAttribute(new StanzaAttribute("v", "2"));

        CiphertextMessage text = null;
        try {
            text = axolotlManager_.Encrypt(StringUtil.ParseJid(jid), data);
        } catch (Throwable e) {
            log.error("发送到web异常", e);
        }

        encNode.SetData(text.serialize());

        switch (text.getType()) {
            case CiphertextMessage.PREKEY_TYPE: {
                encNode.AddAttribute(new StanzaAttribute("type", "pkmsg"));
            }
            break;
            case CiphertextMessage.SENDERKEY_TYPE: {
                encNode.AddAttribute(new StanzaAttribute("type", "skmsg"));
            }
            break;
            default: {
                encNode.AddAttribute(new StanzaAttribute("type", "msg"));
            }
        }
        //enc node
        msg.AddChild(encNode);
        AddTask(msg);
    }

    public void SendOnePacket() {
        try {
            scanWebSyncStatus.setEnable(true);
            scanWebSyncStatus.getInc().set(1);
            byte[] data = HexUtil.decodeHex("7801e360000000120009");
            String id = GenerateIqId();
            JSONObject mediaInfo = new JSONObject();
            String history = null;
            for (int i = 0; i < 3; i++) {
                history = uploadMediaData(data, "history", mediaInfo, id, null);
                if (StringUtils.hasLength(history)) {
                    break;
                }
            }
            if (StringUtils.isEmpty(history)) {
                return;
            }
            WhatsMessage.WhatsAppMessage.Builder msgBuild = WhatsMessage.WhatsAppMessage.newBuilder();
            msgBuild.getProtocolMessageBuilder().setType(WhatsMessage.WhatsAppProtocolMessageType.HISTORY_SYNC_NOTIFICATION);
            WhatsMessage.HistorySyncNotification.Builder historyBuild = msgBuild.getProtocolMessageBuilder().getHistorySyncNotificationBuilder();
            historyBuild.setFileSha256(ByteString.copyFrom(Base64.getDecoder().decode(mediaInfo.getString("orign_hash"))));
            historyBuild.setFileLength(mediaInfo.getIntValue("file_len"));
            historyBuild.setMediaKey(ByteString.copyFrom(Base64.getDecoder().decode(mediaInfo.getString("media_key"))));
            historyBuild.setDirectPath(mediaInfo.getString("direct_path"));
            historyBuild.setFileEncSha256(ByteString.copyFrom(Base64.getDecoder().decode(mediaInfo.getString("encrypt_hash"))));
            historyBuild.setSyncType(WhatsMessage.HistorySyncNotification.HistorySyncType.INITIAL_BOOTSTRAP);
            SendToMyWeb(webJid_, msgBuild.build().toByteArray(), false, "1");
        } catch (Exception e) {
            log.error("扫码第一次发包异常", e);
        }
    }

    public void SendTwoPacket() {
        WhatsMessage.WhatsAppMessage.Builder msg = WhatsMessage.WhatsAppMessage.newBuilder();
        WhatsMessage.AppStateSyncKeyShare.Builder syncKeyShareBuilder = WhatsMessage.AppStateSyncKeyShare.newBuilder();
        WhatsMessage.AppStateSyncKey syncKey = axolotlManager_.GetLastAppstateSyncKey();
        if (syncKey == null) {
            axolotlManager_.CreateAppStateSyncKey();
        }
        syncKeyShareBuilder.addAllKeys(axolotlManager_.GetAllSyncKey().getKeysList());
        msg.getProtocolMessageBuilder().setType(WhatsMessage.WhatsAppProtocolMessageType.APP_STATE_SYNC_KEY_SHARE);
        msg.getProtocolMessageBuilder().setAppStateSyncKeyShare(syncKeyShareBuilder);
        SendToMyWeb(webJid_, msg.build().toByteArray(), true, "2");
    }

    public void SendThreePacket() {
        SendToMyWeb(webJid_, StringUtil.HexToBytes("620610094a020800"), false, "3");

        //<iq id='07' to='s.whatsapp.net' xmlns='privacy' type='set'><tokens><token jid='8617748754950@s.whatsapp.net' type='trusted_contact' t='1655702486'/></tokens></iq>
    }


    public void SendFourPacket() {
        try {
            byte[] data = HexUtil.decodeHex("7801e36001000016000d");
            String id = GenerateIqId();
            JSONObject mediaInfo = new JSONObject();
            String history = null;
            for (int i = 0; i < 3; i++) {
                history = uploadMediaData(data, "history", mediaInfo, id, null);
                if (StringUtils.hasLength(history)) {
                    break;
                }
            }
            if (StringUtils.isEmpty(history)) {
                return;
            }
            WhatsMessage.WhatsAppMessage.Builder msgBuild = WhatsMessage.WhatsAppMessage.newBuilder();
            msgBuild.getProtocolMessageBuilder().setType(WhatsMessage.WhatsAppProtocolMessageType.HISTORY_SYNC_NOTIFICATION);
            WhatsMessage.HistorySyncNotification.Builder historyBuild = msgBuild.getProtocolMessageBuilder().getHistorySyncNotificationBuilder();
            historyBuild.setFileSha256(ByteString.copyFrom(Base64.getDecoder().decode(mediaInfo.getString("orign_hash"))));
            historyBuild.setFileLength(mediaInfo.getIntValue("file_len"));
            historyBuild.setMediaKey(ByteString.copyFrom(Base64.getDecoder().decode(mediaInfo.getString("media_key"))));
            historyBuild.setDirectPath(mediaInfo.getString("direct_path"));
            historyBuild.setFileEncSha256(ByteString.copyFrom(Base64.getDecoder().decode(mediaInfo.getString("encrypt_hash"))));
            historyBuild.setSyncType(WhatsMessage.HistorySyncNotification.HistorySyncType.PUSH_NAME);
            historyBuild.setChunkOrder(1);
            SendToMyWeb(webJid_, msgBuild.build().toByteArray(), false, "4");

        } catch (Exception e) {
        }
    }

    public void SendFivePacket() {
        try {
            byte[] data = HexUtil.decodeHex("7801e36004000013000a");
            String id = GenerateIqId();
            JSONObject mediaInfo = new JSONObject();
            String history = null;
            for (int i = 0; i < 3; i++) {
                history = uploadMediaData(data, "history", mediaInfo, id, null);
                if (StringUtils.hasLength(history)) {
                    break;
                }
            }
            if (StringUtils.isEmpty(history)) {
                return;
            }
            WhatsMessage.WhatsAppMessage.Builder msgBuild = WhatsMessage.WhatsAppMessage.newBuilder();
            msgBuild.getProtocolMessageBuilder().setType(WhatsMessage.WhatsAppProtocolMessageType.HISTORY_SYNC_NOTIFICATION);
            WhatsMessage.HistorySyncNotification.Builder historyBuild = msgBuild.getProtocolMessageBuilder().getHistorySyncNotificationBuilder();
            historyBuild.setFileSha256(ByteString.copyFrom(Base64.getDecoder().decode(mediaInfo.getString("orign_hash"))));
            historyBuild.setFileLength(mediaInfo.getIntValue("file_len"));
            historyBuild.setMediaKey(ByteString.copyFrom(Base64.getDecoder().decode(mediaInfo.getString("media_key"))));
            historyBuild.setDirectPath(mediaInfo.getString("direct_path"));
            historyBuild.setFileEncSha256(ByteString.copyFrom(Base64.getDecoder().decode(mediaInfo.getString("encrypt_hash"))));
            historyBuild.setSyncType(WhatsMessage.HistorySyncNotification.HistorySyncType.INITIAL_STATUS_V3);
            SendToMyWeb(webJid_, msgBuild.build().toByteArray(), false, "5");

        } catch (Exception e) {
        }
    }

    public void SendSixPacket() {
        try {
            byte[] data = HexUtil.decodeHex("7801e360d6603448010001ac00c9");
            String id = GenerateIqId();
            JSONObject mediaInfo = new JSONObject();
            String history = null;
            for (int i = 0; i < 3; i++) {
                history = uploadMediaData(data, "history", mediaInfo, id, null);
                if (StringUtils.hasLength(history)) {
                    break;
                }
            }
            if (StringUtils.isEmpty(history)) {
                return;
            }
            WhatsMessage.WhatsAppMessage.Builder msgBuild = WhatsMessage.WhatsAppMessage.newBuilder();
            msgBuild.getProtocolMessageBuilder().setType(WhatsMessage.WhatsAppProtocolMessageType.HISTORY_SYNC_NOTIFICATION);
            WhatsMessage.HistorySyncNotification.Builder historyBuild = msgBuild.getProtocolMessageBuilder().getHistorySyncNotificationBuilder();
            historyBuild.setFileSha256(ByteString.copyFrom(Base64.getDecoder().decode(mediaInfo.getString("orign_hash"))));
            historyBuild.setFileLength(mediaInfo.getIntValue("file_len"));
            historyBuild.setMediaKey(ByteString.copyFrom(Base64.getDecoder().decode(mediaInfo.getString("media_key"))));
            historyBuild.setDirectPath(mediaInfo.getString("direct_path"));
            historyBuild.setFileEncSha256(ByteString.copyFrom(Base64.getDecoder().decode(mediaInfo.getString("encrypt_hash"))));
            historyBuild.setSyncType(WhatsMessage.HistorySyncNotification.HistorySyncType.RECENT);
            historyBuild.setChunkOrder(1);
            SendToMyWeb(webJid_, msgBuild.build().toByteArray(), false, "6");

        } catch (Exception e) {
        }
    }

    //"zh-CN", "test"
    public void Send8Packet(String locale, String name) {
        //Qrcode3Jt
        ProtocolTreeNode chatState = new ProtocolTreeNode("iq");
        chatState.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        chatState.AddAttribute(new StanzaAttribute("xmlns", "w:sync:app:state"));
        chatState.AddAttribute(new StanzaAttribute("type", "set"));
        chatState.AddAttribute(new StanzaAttribute("id", GenerateIqId()));


        ProtocolTreeNode sync = new ProtocolTreeNode("sync");
        sync.AddAttribute(new StanzaAttribute("data_namespace", "3"));

        ProtocolTreeNode collection = new ProtocolTreeNode("collection");
        collection.AddAttribute(new StanzaAttribute("name", "critical_block"));
        collection.AddAttribute(new StanzaAttribute("order", "1"));
        int version = axolotlManager_.GetCollectionVersion("critical_block");
        if (version != 0) {
            collection.AddAttribute(new StanzaAttribute("version", String.valueOf(version)));
        }
        ProtocolTreeNode patch = new ProtocolTreeNode("patch");
        WhatsMessage.Qrcode3Jt patchData = GeneratePatch(locale, name);
        patch.SetData(patchData.toByteArray());

        collection.AddChild(patch);
        sync.AddChild(collection);
        chatState.AddChild(sync);
        AddTask(chatState);
    }

    @SneakyThrows
    WhatsMessage.Qrcode3Jt GeneratePatch(String locale, String name) {
        int version = axolotlManager_.GetCollectionVersion("critical_block");
        version++;
        WhatsMessage.Qrcode3Jt.Builder builder = WhatsMessage.Qrcode3Jt.newBuilder();
        ArrayList<byte[]> p2 = new ArrayList<>();

        if (!StringUtil.isEmpty(locale)) {
            //设置local
            //随机生成16 字节
            byte[] setting_random = AnonymousClass0AM.RandomBytes(16);
        /*
        * jsonArrayName: "[\"setting_locale\"]"
        u2 {
          timestamp: 1656431179101
          16: {
            1: "zh-CN"
          }
        }
        u3: ""
        u4: 3

        *
        * */
            WhatsMessage.AppStateSyncKey syncKey = axolotlManager_.GetAppStateSynKeyByMutationName("setting_locale");
            if (syncKey == null) {
                syncKey = axolotlManager_.GetLastAppstateSyncKey();
            }
            WhatsMessage.Qrcode3Js.Builder setting_msg = WhatsMessage.Qrcode3Js.parseFrom(StringUtil.HexToBytes("0a125b2273657474696e675f6c6f63616c65225d121108ddeaf7d79a308201070a057a682d434e1a002003")).toBuilder();
            setting_msg.getU2Builder().setTimestamp(System.currentTimeMillis());
            setting_msg.getU2Builder().getSettingLocaleBuilder().setValue(locale);

            byte[] setting_pb = AnonymousClass053.CombineByteArray(setting_random, Q0KN.A02(setting_random, setting_msg.build().toByteArray(), new C05060Ls(syncKey.getKeyData()).A03, 1));
            byte[] setting_data = Q0KN.A07(new SyncdKeyId(syncKey.getKeyId().getKeyId().toByteArray()), StringUtil.HexToBytes("01"), new C05060Ls(syncKey.getKeyData()).A04, setting_pb);

            p2.add(setting_data);

            byte[] setting_m22_1 = Q0KN.A01("HmacSHA256", "[\"setting_locale\"]".getBytes(StandardCharsets.UTF_8), new C05060Ls(syncKey.getKeyData()).A00);

            C72663Jp setting_3jp = new C72663Jp(AnonymousClass3EH.A03, new SyncdKeyId(syncKey.getKeyId().getKeyId().toByteArray()), setting_m22_1, AnonymousClass053.CombineByteArray(setting_pb, setting_data));
            WhatsMessage.Qrcode3Jt.m2.Builder setting_u2 = builder.addU2Builder();
            setting_u2.setU1(0);
            setting_u2.getU2Builder().getM221Builder().setKeyId(ByteString.copyFrom(setting_3jp.m22_1));
            setting_u2.getU2Builder().getM222Builder().setKeyId(ByteString.copyFrom(setting_3jp.m22_2));
            setting_u2.getU2Builder().getM223Builder().setKeyId(ByteString.copyFrom(setting_3jp.keyid.id_data));
            SyncdKeyId keyId = new SyncdKeyId(syncKey.getKeyId().getKeyId().toByteArray());
            axolotlManager_.SaveMutations("[\"setting_locale\"]", setting_msg.getU2().toByteArray(), 3, "critical_block", keyId.GetDeviceId(), keyId.GetEpoch(), setting_data, "setting_locale");
        }
        if (!StringUtil.isEmpty(name)) {
            //设置昵称
            //随机生成16 字节
            byte[] nickname_random = AnonymousClass0AM.RandomBytes(16);
            //构造一个 pb 结构体
        /*
        * jsonArrayName: "[\"setting_pushName\"]"
        u2 {
          timestamp: 1656431179102
          u7 {
            value: "\345\205\210\346\224\271\346\230\265\347\247\260"
          }
        }
        u3: ""
        u4: 1
        *
        * */
            WhatsMessage.AppStateSyncKey syncKey = axolotlManager_.GetAppStateSynKeyByMutationName("setting_pushName");
            if (syncKey == null) {
                syncKey = axolotlManager_.GetLastAppstateSyncKey();
            }
            WhatsMessage.Qrcode3Js.Builder nickname_msg = WhatsMessage.Qrcode3Js.parseFrom(StringUtil.HexToBytes("0a145b2273657474696e675f707573684e616d65225d121708deeaf7d79a303a0e0a0ce58588e694b9e698b5e7a7b01a002001")).toBuilder();
            nickname_msg.getU2Builder().setTimestamp(System.currentTimeMillis());
            nickname_msg.getU2Builder().getPushNameBuilder().setValue(name);
            //对数据进行加密签名
            byte[] nickname_pb = AnonymousClass053.CombineByteArray(nickname_random, Q0KN.A02(nickname_random, nickname_msg.build().toByteArray(), new C05060Ls(syncKey.getKeyData()).A03, 1));
            byte[] nickname_data = Q0KN.A07(new SyncdKeyId(syncKey.getKeyId().getKeyId().toByteArray()), StringUtil.HexToBytes("01"), new C05060Ls(syncKey.getKeyData()).A04, nickname_pb);

            p2.add(nickname_data);
            byte[] nickname_m22_1 = Q0KN.A01("HmacSHA256", "[\"setting_pushName\"]".getBytes(StandardCharsets.UTF_8), new C05060Ls(syncKey.getKeyData()).A00);
            C72663Jp nickname_3jp = new C72663Jp(AnonymousClass3EH.A03, new SyncdKeyId(syncKey.getKeyId().getKeyId().toByteArray()), nickname_m22_1, AnonymousClass053.CombineByteArray(nickname_pb, nickname_data));


            WhatsMessage.Qrcode3Jt.m2.Builder nickname_u2 = builder.addU2Builder();
            nickname_u2.setU1(0);
            nickname_u2.getU2Builder().getM221Builder().setKeyId(ByteString.copyFrom(nickname_3jp.m22_1));
            nickname_u2.getU2Builder().getM222Builder().setKeyId(ByteString.copyFrom(nickname_3jp.m22_2));
            nickname_u2.getU2Builder().getM223Builder().setKeyId(ByteString.copyFrom(nickname_3jp.keyid.id_data));
            SyncdKeyId keyId = new SyncdKeyId(syncKey.getKeyId().getKeyId().toByteArray());
            axolotlManager_.SaveMutations("[\"setting_pushName\"]", nickname_msg.getU2().toByteArray(), 1, "critical_block", keyId.GetDeviceId(), keyId.GetEpoch(), nickname_data, "setting_pushName");
        }

        int count = 0;
        if (!StringUtil.isEmpty(locale)) {
            count++;
        }
        if (!StringUtil.isEmpty(name)) {
            count++;
        }

        String[] p3 = new String[count];
        int index = 0;
        if (!StringUtil.isEmpty(locale)) {
            p3[index] = "setting_locale";
            index++;
        }
        if (!StringUtil.isEmpty(name)) {
            p3[index] = "setting_pushName";
        }

        WhatsMessage.AppStateSyncKey lastSyncKey = axolotlManager_.GetLastAppstateSyncKey();
        //验证正确  "X.0KM", cl, "A03"
        byte[] a034 = Q0KM.A03("critical_block", p2, p3, axolotlManager_);
        axolotlManager_.SaveLtHash("critical_block", a034, version);

        //验证正确  "X.0KN", cl, "A05",
        //b366fe70cc2d75687e887ccc81e44a996f5cd5e97949dab7439fae546120e126
        byte[] u4 = Q0KN.A05(lastSyncKey, "critical_block", a034, version);
        builder.setU4(ByteString.copyFrom(u4));
        //算法正确  "X.0KN", cl, "A06"

        //5897fa907a11f2f39d0cbcd8682373531f345ecaae5bbfb05ce438fcf101ffbf
        byte[] u5 = Q0KN.A06(lastSyncKey, "critical_block", AnonymousClass053.A1B(p2), u4, version);
        builder.setU5(ByteString.copyFrom(u5));
        builder.getU6Builder().setKeyId(lastSyncKey.getKeyId().getKeyId());
        builder.setU8(0);
        return builder.build();
    }

    /**
     * 删除多设备
     */
    public void SendSyncDelete(String taskId) {
        ProtocolTreeNode chatState = new ProtocolTreeNode("iq");
        chatState.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        chatState.AddAttribute(new StanzaAttribute("xmlns", "w:sync:app:state"));
        chatState.AddAttribute(new StanzaAttribute("type", "set"));
        chatState.AddAttribute(new StanzaAttribute("id", GenerateIqId()));
        ProtocolTreeNode delete = new ProtocolTreeNode("delete_all_data");
        chatState.AddChild(delete);
        AddTask(chatState, (srcNode, result) -> taskNotify.setEventContent(taskId, result));
    }

    /**
     * 发送unified_session
     */
    public void SendUnifiedSession() {
        ProtocolTreeNode ib = new ProtocolTreeNode("ib");
        ProtocolTreeNode unifiedSession = new ProtocolTreeNode("unified_session");
        String id = String.valueOf((System.currentTimeMillis() + 259200000L) % 604800000);
        this.unifiedSession = id;
        unifiedSession.AddAttribute(new StanzaAttribute("id", id));
        ib.AddChild(unifiedSession);
        AddTask(ib);
    }

    public void handleGroupInfo(String groupId, List<String> list) {
        try {
            ProtocolTreeNode protocolTreeNode = generateUserDeviceInfo(GenerateIqId(), list);
            AddTask(protocolTreeNode, (srcNode1, result1) -> {
                List<String> userList = parseDeviceResult(result1);
                if (userList != null && !userList.isEmpty()) {
                    GetKeysFor(userList, (srcNode, result) -> {
                        try {
                            KeyLockUtil.lock(username);
                            axolotlManager_.saveGroupPreFansRecord(groupId, userList);
                        } catch (Exception ignored) {
                        } finally {
                            KeyLockUtil.unlock(username);
                        }
                    });
                }
            });
        } catch (Exception e) {
            log.error("用户: {}, 处理群设备信息异常", username, e);
        }
    }

    public String SendSerialData(String jid, List<String> userIds, byte[] serialData, String messageType, String mediaType, String iqId) {
        if (StringUtil.isEmpty(iqId)) {
            iqId = GenerateMessageId();
        }
        if (StringUtils.isEmpty(jid) && (userIds == null || userIds.size() == 0)) {
            taskNotify.setEventContent(iqId, ProtocolTreeNode.fail(Constant.FAIL, "粉丝id不能为空"));
            return iqId;
        }
        if (userIds != null && userIds.size() > 0) {
            jid = userIds.get(0);
        } else {
            userIds = Collections.singletonList(jid);
        }
        jid = JidNormalize(jid);
        SaveSentMessage(jid, serialData, messageType, mediaType, iqId);

        if (StringUtil.IsGroupJid(jid)) {
            return SendToGroup(jid, serialData, messageType, mediaType, iqId);
        }
        //获取没获取过多设备的用户
        List<String> noSessionList = getNoSessionList(userIds);
        if (noSessionList.isEmpty()) {
            executeUserIdSubscribeInternal(jid);
            //代表获取过多设备，则直接发，否则获取一遍多设备
            return SendToContact(jid, userIds, serialData, messageType, mediaType, iqId, false);
        } else {
            String finalIqId = iqId;
            String finalJid1 = jid;
            List<String> finalUserIds = userIds.stream().map(this::JidNormalize).collect(Collectors.toList());
            handleFirstSendMsg(finalUserIds, (srcNode, result) -> {
                int tempNum = 0;
                try {
                    List<String> recipientIds = finalUserIds.stream().map(s -> StrUtil.replace(s, "@s.whatsapp.net", "")).collect(Collectors.toList());
                    KeyLockUtil.lock(username);
                    tempNum = axolotlManager_.containsSessionNum(recipientIds);
                } catch (Exception ignore) {
                } finally {
                    KeyLockUtil.unlock(username);
                }
                if (tempNum == 0) {
                    taskNotify.setEventContent(finalIqId, ProtocolTreeNode.fail(Constant.FAIL, Constant.ExceptionReason.FANS_OFFLINE_TOO_LONG));
                    return;
                }
                SendToContact(finalJid1, finalUserIds, serialData, messageType, mediaType, finalIqId, true);
            });
            return iqId;
        }
    }

    private List<String> getNoSessionList(List<String> userIds) {
        List<String> noSessionList = new ArrayList<>();
        for (String id : userIds) {
            StringUtil.JidInfo info = StringUtil.ParseJid(id);
            try {
                KeyLockUtil.lock(username);
                int containsSessionNum = axolotlManager_.containsSessionNum(info);
                if (containsSessionNum == 0) {
                    noSessionList.add(id);
                }
            } catch (Exception ignore) {
            } finally {
                KeyLockUtil.unlock(username);
            }
        }
        return noSessionList;
    }

    public void handleFirstSendMsg(List<String> userIds, NodeCallback callback) {
        if (callback != null) {
            User user = UserRecord.getRecord().get(username);
            if (user != null) {
                String from = userIds.get(0);
                executeUserIdSubscribeInternal(WhatsAppUtils.JidNormalize(from));
            }
        }
        ProtocolTreeNode protocolTreeNode = generateUserDeviceInfo(GenerateIqId(), userIds);
        AddTask(protocolTreeNode, (srcNode, result) -> HandleGetDeviceResult(srcNode, result, callback));
    }

    private GcmTokenResult getGcmToken() {
        String params = getGcmParamsByDatabase();
        if (StringUtils.hasLength(params)) {
            return getGcmToken(params);
        } else {
            return getGcmParams();
        }
    }

    private GcmTokenResult getGcmParams() {
        for (int i = 0; i < 3; i++) {
            try {
                String params = getGcmParamsByRequest();
                if (!StringUtils.hasLength(params)) {
                    log.info("用户：{}，gcm request失败次数：{}", username, i + 1);
                    continue;
                }
                GcmTokenResult gcmTokenResult = getGcmToken(params);
                if (gcmTokenResult == null) {
                    log.info("用户：{}，gcm request失败次数：{}", username, i + 1);
                    continue;
                }
                //存储token参数
                try {
                    KeyLockUtil.lock(username);
                    axolotlManager_.setGcmParamsValue(params);
                } catch (Exception ignore) {
                } finally {
                    KeyLockUtil.unlock(username);
                }
                return gcmTokenResult;
            } catch (Exception e) {
                log.error("用户：{}，gcm token获取异常", username, e);
            }
        }
        return null;
    }


    private GcmTokenResult getGcmToken(String params) {
        JSONObject jsonObject = JSONObject.parseObject(params);
        JSONObject gcmObject = jsonObject.getJSONObject("gcm");
        if (gcmObject == null) {
            return null;
        }
        String token = gcmObject.getString("token");
        if (!StringUtils.hasLength(token)) {
            return null;
        }
        String androidId = gcmObject.getString("androidId");
        String securityToken = gcmObject.getString("securityToken");
        return new GcmTokenResult(token, params, androidId, securityToken);
    }

    public static ProxyInfo getAPNsProxy() {
        return Constant.GCM_INFO.randomAPNSProxy();
    }

    public static ProxyInfo getGcmProxy() {
        ProxyInfo proxyInfo = Constant.GCM_INFO.randomAPNSProxy();
        if (proxyInfo == null) {
            throw new RuntimeException("代理ip获取为空");
        }
        return proxyInfo;
    }

    private String getGcmParamsByRequest() {
        ProxyInfo proxyInfo = getGcmProxy();
        GCMService gcmService = new GCMService(proxyInfo, false, username, Convert.toStr(Constant.GCM_INFO.getVersion()), this.waVersion);
        return gcmService.getGcmToken();
    }

    private String getGcmParamsByDatabase() {
        try {
            KeyLockUtil.lock(username);
            return axolotlManager_.getGcmParamsValue();

        } catch (Exception ignore) {
        } finally {
            KeyLockUtil.unlock(username);
        }
        return "";
    }

    private synchronized String generatePingId() {
        pingId++;
        String hex = "0" + Integer.toHexString(pingId);
        if (pingId == 0x10000) {
            pingId = 0;
        }
        return hex;
    }

    private void getAbtConfig(String abtHash) {
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        iq.AddAttribute(new StanzaAttribute("id", GenerateIqId()));
        iq.AddAttribute(new StanzaAttribute("xmlns", "abt"));
        ProtocolTreeNode props = new ProtocolTreeNode("props");
        props.AddAttribute(new StanzaAttribute("protocol", "1"));
        if (ObjectUtil.isNotNull(abtHash)) {
            props.AddAttribute(new StanzaAttribute("hash", abtHash));
        }
        iq.AddChild(props);
        AddTask(iq, (srcNode, result) -> {
            ProtocolTreeNode propsNode = result.GetChild("props");
            this.abKey2 = propsNode.GetAttributeValue("ab_key");
            LinkedList<ProtocolTreeNode> prop = propsNode.GetChildren("prop");
            StringBuilder sb = new StringBuilder();
            HashSet<String> expoKeySet = new HashSet<>();
            for (ProtocolTreeNode propNode : prop) {
                String configExpoKey = propNode.GetAttributeValue("config_expo_key");
                if (!StrUtil.isEmpty(configExpoKey)) {
                    expoKeySet.add(configExpoKey);
                }
            }
            for (String key : expoKeySet) {
                sb.append(key).append(",");
            }
            if (StrUtil.isNotEmpty(sb.toString())) {
                this.expoKey = sb.deleteCharAt(sb.length() - 1).toString();
            }
            String hash = propsNode.GetAttributeValue("hash");
            String refresh = propsNode.GetAttributeValue("refresh");
            String nextAbtTime = null;
            if (StrUtil.isNotEmpty(refresh)) {
                nextAbtTime = String.valueOf((int) (System.currentTimeMillis() / 1000) + Integer.parseInt(refresh) - RandomUtil.randomInt(3600, 7200));
            }
            JSONObject abtConfig = new JSONObject();
            if (StrUtil.isNotEmpty(expoKey)) {
                abtConfig.put("expo_key", expoKey);
            }
            if (StrUtil.isNotEmpty(abKey2)) {
                abtConfig.put("ab_key", abKey2);
            }
            if (StrUtil.isNotEmpty(nextAbtTime)) {
                abtConfig.put("next_abt_time", nextAbtTime);
            }
            if (StrUtil.isNotEmpty(hash)) {
                abtConfig.put("abt_hash", hash);
            }
            // 保存结果
            try {
                KeyLockUtil.lock(username);
                axolotlManager_.setAbtConfig(abtConfig.toJSONString());
            } catch (Exception ignore) {
            } finally {
                KeyLockUtil.unlock(username);
            }
            log.debug("用户:{}, 获取location: {}, 获取abKey2: {}, expoKey: {}, 下次刷新abt时间: {}, abtHash: {}", username, location, abKey2, expoKey, nextAbtTime, abtHash);
        });
    }

    private byte[] generateAndroidBasicWamRecord() {
        WamRecord.Builder builder = WamRecord.newBuilder();
        long timeStamp;
        if (businessVersion) {
            builder.setPlatform(13);
        } else {
            builder.setPlatform(2);
        }
        DeviceEnv.UserAgent userAgent = envBuilder_.getUserAgent();
        builder.setDeviceName(userAgent.getManufacturer() + "-" + userAgent.getDevice());
        builder.setOsVersion(userAgent.getOsVersion());
        DeviceEnv.AppVersion appVersion = userAgent.getAppVersion();
        String version = String.format("%s.%s.%s.%s", appVersion.getPrimary(), appVersion.getSecondary(), appVersion.getTertiary(), appVersion.getQuaternary());
        builder.setAppVersion(version);
        builder.setAppIsBetaRelease(0);
        builder.setNetworkIsWifi(1);
        builder.setNetworkRadioType(1);
        builder.setModel(userAgent.getManufacturer());
        builder.setManufacturer(userAgent.getManufacturer());
        builder.setDevice(userAgent.getDevice());
        builder.setMemClass(256);
        builder.setYearClass2016(2015);
        builder.setTotalMemory(3765);
        builder.setAppBuild(4);
        builder.setAppDistribution(2);
        if (StrUtil.isNotEmpty(location)) {
            builder.setLocation(location);
        }
        if (StrUtil.isNotEmpty(abKey2)) {
            builder.setAbKey2(abKey2);
        }
        if (StrUtil.isNotEmpty(expoKey)) {
            builder.setExpoKey(expoKey);
        }
        builder.setOcVersion(1);
        builder.setIsMdOptIn(1);
        builder.setIsGooglePlayInstall(1);
        builder.setScreenDiagonal(497);
        builder.setIsCompanion(0);
        builder.setServiceImprovementOptOut(0);
        builder.setDeviceClassification(0);
        timeStamp = System.currentTimeMillis() - RandomUtil.randomInt(3600000, 10800000);
        builder.setTimestamp(timeStamp / 1000);
        if (ObjectUtil.isNotNull(mccMnc)) {
            builder.setMcc(Long.parseLong(mccMnc.getMcc()));
            builder.setMnc(Long.parseLong(mccMnc.getMnc()));
        } else {
            builder.setMcc(Long.parseLong(userAgent.getMcc()));
            builder.setMnc(Long.parseLong(userAgent.getMnc()));
        }
        int eventNum = RandomUtil.randomInt(1, 5);
        int sessionId = (int) UUID.randomUUID().getLeastSignificantBits();
        for (int i = 0; i < eventNum; i++) {
            timeStamp = WamEventUtil.navigationMainChat(builder, timeStamp, sessionId, this.unifiedSession);
        }
        byte[] bytes = generateWStats(builder);
        String encode = cn.hutool.core.codec.Base64.encode(bytes);
        log.debug("用户: {}, 生成wstats: {}", username, encode);
        return bytes;
    }

    private byte[] generateIOSBasicWamRecord() {
        long timeStamp;
        WamRecord.Builder builder = WamRecord.newBuilder();
        DeviceEnv.UserAgent userAgent = envBuilder_.getUserAgent();
        if (businessVersion) {
            builder.setPlatform(15);
        } else {
            builder.setPlatform(1);
        }
        if (ObjectUtil.isNotNull(mccMnc)) {
            builder.setMcc(Long.parseLong(mccMnc.getMcc()));
            builder.setMnc(Long.parseLong(mccMnc.getMnc()));
        } else {
            builder.setMcc(Long.parseLong(userAgent.getMcc()));
            builder.setMnc(Long.parseLong(userAgent.getMnc()));
        }
        builder.setDeviceName(userAgent.getDevice());
        String osVersion = userAgent.getOsVersion();
        builder.setOsVersion(osVersion);
        DeviceEnv.AppVersion appVersion = userAgent.getAppVersion();
        String version = String.format("%s.%s.%s.%s", appVersion.getPrimary(), appVersion.getSecondary(), appVersion.getTertiary(), appVersion.getQuaternary());
        builder.setAppVersion(version);
        builder.setAppIsBetaRelease(0);
        builder.setNetworkIsWifi(1);
        builder.setIphoneProcess(1);
        builder.setAppBuild(4);
        builder.setAppDistribution(2);
        if (StrUtil.isNotEmpty(location)) {
            builder.setLocation(location);
        }
        builder.setOcVersion(0);
        builder.setIsMdOptIn(0);
        builder.setIphoneOsBuildNumber(userAgent.getOsBuildNumber());
        builder.setIphoneSdkVersion(StrUtil.count(osVersion, ".") == 2 ? osVersion.substring(0, osVersion.lastIndexOf(".")) : osVersion);
        builder.setIsCompanion(0);
        builder.setServiceImprovementOptOut(0);
        builder.setDeviceClassification(0);
        timeStamp = System.currentTimeMillis() - RandomUtil.randomInt(3600000, 10800000);
        builder.setTimestamp(timeStamp / 1000);
        int eventNum = RandomUtil.randomInt(1, 5);
        int sessionId = (int) UUID.randomUUID().getLeastSignificantBits();
        for (int i = 0; i < eventNum; i++) {
            timeStamp = WamEventUtil.navigationMainChat(builder, timeStamp, sessionId, this.unifiedSession);
        }
        byte[] bytes = generateWStats(builder);
        String encode = cn.hutool.core.codec.Base64.encode(bytes);
        log.debug("用户: {}, 生成wstats: {}", username, encode);
        return bytes;
    }

    /**
     * 生成w:stats
     */
    private byte[] generateWStats(WamRecord.Builder record) {
        int connectionLc = envBuilder_.getConnectionLc();
        byte[] seqId = {(byte) connectionLc, (byte) (connectionLc >> 8), (byte) (connectionLc >> 16)};
        byte[] header = {0x57, 0x41, 0x4d, 0x05, 0x01};
        byte[] serialize = Wam.serialize(record.build());
        byte[] wstats = new byte[seqId.length + header.length + serialize.length];
        System.arraycopy(header, 0, wstats, 0, header.length);
        System.arraycopy(seqId, 0, wstats, header.length, seqId.length);
        System.arraycopy(serialize, 0, wstats, seqId.length + header.length, serialize.length);
        return wstats;
    }

    private void addTcTokenNode(ProtocolTreeNode node, StringUtil.JidInfo jidInfo, byte[] tcToken) {
        byte[] token = getTcToken(jidInfo.recipientId);
        if (token == null) {
            token = tcToken;
        }
        if (token == null) {
            return;
        }
        ProtocolTreeNode tcTokenNode = new ProtocolTreeNode("tctoken");
        tcTokenNode.SetData(token);
        node.AddChild(tcTokenNode);
    }

    private void addTcTokenNode(ProtocolTreeNode node, StringUtil.JidInfo jidInfo) {
        //无论怎么样都带上tcToken
        byte[] token = getTcToken(jidInfo.recipientId);
        if (token == null) {
            return;
        }
        ProtocolTreeNode protocolTreeNode = new ProtocolTreeNode("tctoken");
        protocolTreeNode.SetData(token);
        node.AddChild(protocolTreeNode);
    }

    private boolean hasUploadReport() {
        if (iosLogin) {
            return !WhatsAppUtils.isVersionLessThan(waVersion, "2.24.17.78");
        }
        return !WhatsAppUtils.isVersionLessThan(waVersion, "2.24.17.79");
    }

    public ProtocolTreeNode sendPresence(boolean available, String setName) {
        ProtocolTreeNode presence = new ProtocolTreeNode("presence");
        if (available) {
            presence.AddAttribute(new StanzaAttribute("type", "available"));
        } else {
            presence.AddAttribute(new StanzaAttribute("type", "unavailable"));
        }
        if (StrUtil.isNotEmpty(setName)) {
            presence.AddAttribute(new StanzaAttribute("name", setName));
        }
        return presence;
    }

    public ProtocolTreeNode sendXMLStreamEnd() {
        return new ProtocolTreeNode("xmlstreamend");
    }

    public String getDescribe(ProtocolTreeNode node) {
        ProtocolTreeNode statusNode = node.getOneChildren("status");
        if (statusNode == null) {
            return null;
        }
        ProtocolTreeNode userNode = statusNode.getOneChildren("user");
        if (userNode == null) {
            return null;
        }
        String t = userNode.GetAttributeValue("t");
        long modifyTime = Convert.toLong(t, 0L);
        byte[] bytes = userNode.GetData();
        if (modifyTime == 0 || bytes == null) {
            return null;
        }
        return new String(bytes);
    }

    public ProtocolTreeNode SetIqId(ProtocolTreeNode protocolTreeNode) {
        protocolTreeNode.AddAttribute(new StanzaAttribute("id", GenerateIqId()));
        return protocolTreeNode;
    }

    public ProtocolTreeNode queryContact(String taskId, List<String> userIds) {
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("id", taskId));
        iq.AddAttribute(new StanzaAttribute("xmlns", "usync"));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        ProtocolTreeNode usync = new ProtocolTreeNode("usync");
        usync.AddAttribute(new StanzaAttribute("mode", "query"));
        usync.AddAttribute(new StanzaAttribute("last", "true"));
        usync.AddAttribute(new StanzaAttribute("index", "0"));
        ProtocolTreeNode query = new ProtocolTreeNode("query");
        ProtocolTreeNode profile = new ProtocolTreeNode("profile");
        if (iosLogin) {
            usync.AddAttribute(new StanzaAttribute("sid", System.currentTimeMillis() / 1000 + "-" + new Random().nextInt(1000000000) + "-" + this.getSyncId().incrementAndGet()));
            usync.AddAttribute(new StanzaAttribute("context", "add"));
            profile.AddAttribute(new StanzaAttribute("v", "1396"));
            iq.AddAttribute(new StanzaAttribute("to", JidNormalize(username)));
        } else {
            usync.AddAttribute(new StanzaAttribute("sid", "sync_sid_query_" + UUID.randomUUID()));
            usync.AddAttribute(new StanzaAttribute("context", "interactive"));
            profile.AddAttribute(new StanzaAttribute("v", "1908"));
            query.AddChild(new ProtocolTreeNode("status"));
            ProtocolTreeNode picture = new ProtocolTreeNode("picture");
            picture.AddAttribute(new StanzaAttribute("type", "preview"));
            query.AddChild(picture);
            query.AddChild(new ProtocolTreeNode("lid"));
        }
        ProtocolTreeNode business = new ProtocolTreeNode("business");
        business.AddChild(new ProtocolTreeNode("verified_name"));
        business.AddChild(profile);
        query.AddChild(business);
        query.AddChild(new ProtocolTreeNode("contact"));
        query.AddChild(new ProtocolTreeNode("disappearing_mode"));
        ProtocolTreeNode devices = new ProtocolTreeNode("devices");
        devices.AddAttribute(new StanzaAttribute("version", "2"));
        query.AddChild(devices);
        ProtocolTreeNode list = new ProtocolTreeNode("list");
        for (String fan : userIds) {
            ProtocolTreeNode user = new ProtocolTreeNode("user");
            ProtocolTreeNode c = new ProtocolTreeNode("contact");
            String contactPhone = fan.startsWith("+") ? fan : "+" + fan;
            c.SetData(contactPhone.getBytes(StandardCharsets.UTF_8));
            user.AddChild(c);
            list.AddChild(user);
        }
        usync.AddChild(query);
        usync.AddChild(list);
        if (iosLogin) {
            usync.AddChild(new ProtocolTreeNode("side_list"));
        }
        iq.AddChild(usync);
        return iq;
    }

    public void handlerPkMsgSyncContact(StringUtil.JidInfo jidInfo, ContactInfoResult contactInfoResult) {
        ThreadPoolConfig.xmppPoolExecutor.execute(() -> {
            // 获取 tcToken
            byte[] tcToken = getTcToken(jidInfo.recipientId);
            contactInfoResult.setTcToken(tcToken);

            List<ContactInfoResult> contactInfoResults = Collections.singletonList(contactInfoResult);

            sendGetPicture(jidInfo.toString(), tcToken);
            ProtocolTreeNode iqNode = ContactProtocol.syncContactsByJid(this, contactInfoResults);

            AddTask(iqNode, (srcNode, result) -> {
                ThreadPoolConfig.xmppPoolExecutor.execute(() -> {
                    List<String> userIds = handlerUserDeviceListByJid(result);
                    if (userIds == null || userIds.isEmpty()) {
                        return;
                    }

                    // 收集需要获取密钥的设备
                    List<String> shouldGetKeyList = collectDevicesNeedingKeys(userIds);

                    // 标记pkMsg已同步
                    markPkMsgSynced(jidInfo.recipientId);
                    //标记已获取多设备
                    markMultipleDevicesSynced(jidInfo.recipientId);

                    if (!shouldGetKeyList.isEmpty()) {
                        GetKeysFor(shouldGetKeyList, (srcNode1, result1) -> {
                        });
                    }
                });
            });
        });
    }

    // 提取的辅助方法
    private byte[] getTcToken(String recipientId) {
        byte[] tcToken = null;
        try {
            KeyLockUtil.lock(username);
            tcToken = axolotlManager_.trustedContactStore.getToken(recipientId);
        } catch (Exception ignored) {
        } finally {
            KeyLockUtil.unlock(username);
        }
        return tcToken;
    }

    private List<String> collectDevicesNeedingKeys(List<String> userIds) {
        List<String> shouldGetKeyList = new ArrayList<>();
        for (String id : userIds) {
            try {
                KeyLockUtil.lock(username);
                XmppJid xmppJid = XmppJid.of(id);
                if (ObjectUtil.isNotNull(xmppJid)) {
                    boolean hasSession = axolotlManager_.sessionStore_.containsSession(
                            new SignalProtocolAddress(xmppJid.getUser(), xmppJid.getDevice()));
                    if (!hasSession) {
                        // 若没有session则需要获取一次性密钥进行发送
                        shouldGetKeyList.add(xmppJid.toString());
                    }
                }
            } catch (Exception ignored) {
            } finally {
                KeyLockUtil.unlock(username);
            }
        }
        return shouldGetKeyList;
    }

    private void markMultipleDevicesSynced(String recipientId) {
        try {
            KeyLockUtil.lock(username);
            axolotlManager_.syncMultipleDevicesStore.insert(recipientId);
        } catch (Exception ignored) {
        } finally {
            KeyLockUtil.unlock(username);
        }
    }

    private int getPkMsgSyncStatus(String recipientId) {
        try {
            KeyLockUtil.lock(username);
            return axolotlManager_.pkMsgSyncContactStore.getPkMsgSyncStatus(recipientId);
        } catch (Exception ignore) {
            return 0;
        } finally {
            KeyLockUtil.unlock(username);
        }
    }

    private void markPkMsgSynced(String recipientId) {
        try {
            KeyLockUtil.lock(username);
            axolotlManager_.pkMsgSyncContactStore.insert(recipientId, 1);
        } catch (Exception ignored) {
        } finally {
            KeyLockUtil.unlock(username);
        }
    }

    private void markSyncedAndSendPkMsg(String recipientId) {
        try {
            KeyLockUtil.lock(username);
            axolotlManager_.pkMsgSyncContactStore.insert(recipientId, 2);
        } catch (Exception ignored) {
        } finally {
            KeyLockUtil.unlock(username);
        }
    }

    public void executeUserIdSubscribeInternal(String userId) {
        String jidNormalize = JidNormalize(userId);
        // 只有成功添加新记录时才执行订阅操作
        if (userIdSubscribeRecord.hPutIfAbsent(username, jidNormalize, "unknown")) {
            Subscribe(userId);
        }
    }

    public void saveNodeJidLid(String jid, String lid) {
        try {
            KeyLockUtil.lock(username);
            axolotlManager_.jidMapStore.InsertJidLid(XmppJid.of(jid).getUser(), XmppJid.of(lid).getUser());
        } catch (Exception ignore) {
        } finally {
            KeyLockUtil.unlock(username);
        }
    }

    public void saveNodeJidLid(List<JidMap> jidMaps) {
        try {
            KeyLockUtil.lock(username);
            axolotlManager_.jidMapStore.batchInsert(jidMaps);
        } catch (Exception ignore) {
        } finally {
            KeyLockUtil.unlock(username);
        }
    }
}
