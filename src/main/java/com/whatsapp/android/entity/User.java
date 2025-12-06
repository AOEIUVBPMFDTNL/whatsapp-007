package com.whatsapp.android.entity;

import Handshake.NoiseHandshake;
import ProtocolTree.ProtocolTreeNode;
import ProtocolTree.StanzaAttribute;
import Util.StringUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.GorgeousEngine;
import com.whatsapp.android.api.account.LoginRequest;
import com.whatsapp.android.api.register.*;
import com.whatsapp.android.config.DelayExecuteTask;
import com.whatsapp.android.config.ThreadPoolConfig;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.entity.pack.account.LoginPack;
import com.whatsapp.android.entity.pack.register.CheckPhoneBlockedPack;
import com.whatsapp.android.entity.pack.register.RegisterPack;
import com.whatsapp.android.entity.response.account.LoginResult;
import com.whatsapp.android.entity.response.profile.TcToken;
import com.whatsapp.android.entity.response.register.CheckPhoneBlockedResult;
import com.whatsapp.android.entity.response.register.SendSmsRegisterResult;
import com.whatsapp.android.entity.response.register.SubmitRegisterResult;
import com.whatsapp.android.request.AbstractRequest;
import com.whatsapp.android.service.ApiService;
import com.whatsapp.android.service.AsyncMessageService;
import com.whatsapp.android.service.WaOldRegistrationService;
import com.whatsapp.android.util.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * @author sunnoc
 * @date 2021-03-09 16:43
 */
@Slf4j
@Data
public class User implements GorgeousEngine.GorgeousEngineDelegate {
    /**
     * 最大重登时间，单位毫秒
     */
    private static final long MAX_RE_LOGIN_TIME = 5 * 60 * 1000;
    /**
     * 递增延时倍数
     */
    private static final double INC_DELAY_MULTIPLE = 1.5;
    private LoginPack loginPack;
    private GorgeousEngine gorgeousEngine;
    @Getter
    private boolean manualStop;
    /**
     * 重新登陆中
     */
    private boolean reLogin;
    /**
     * 环境下载地址
     */
    private String envUrl;
    /**
     * 是否登陆中
     */
    @Getter
    @Setter
    private boolean logging;
    /**
     * 是否在线
     */
    @Getter
    private boolean online;
    /**
     * 是否已注销
     */
    private boolean logout;
    /**
     * 任务通知
     */
    @Getter
    private TaskNotify taskNotify = new TaskNotify();

    /**
     * 重登计数
     */
    private AtomicInteger reLoginCount = new AtomicInteger(0);

    private MccMnc mccMnc;
    /**
     * 重新注册
     */
    private boolean reRegistering;
    /**
     * 重登开始时间
     */
    private long reLoginStartTime;
    /**
     * 重连延时，单位毫秒
     */
    private long reconnectDelay = 300;
    /**
     * 标记主动退出账号
     */
    private boolean markLogout;

    @Override
    public void OnLogin(int code, ProtocolTreeNode content, String loginId) {
        //标记在线状态
        online = code == 0;
        taskNotify.setEventContent(loginId, content);
    }


    @Override
    public void OnDisconnect(String desc, String loginId) {
        online = false;
        taskNotify.setEventContent(loginId, ProtocolTreeNode.fail("loginFail", desc));
        String username = loginPack.getUsername();
        GorgeousEngine gorgeousEngine = getGorgeousEngine();
        UserIdSubscribeRecord userIdSubscribeRecord = gorgeousEngine.getUserIdSubscribeRecord();
        if (userIdSubscribeRecord != null) {
            userIdSubscribeRecord.clear(username);
        }
        if (manualStop) {
            invalidTaskCallBack(Constant.OnlineStatus.USER_OFFLINE);
            return;
        }
        log.info("用户：{}，断开事件，内容：{}", username, desc);
        int index = StrUtil.indexOf(desc, "conflict type='replaced'", 0, false);
        try {
            if (index != -1) {
                if (!reLogin) {
                    if (exceededThreshold(username)) {
                        manualStop = true;
                        ThreadPoolConfig.loginThreadPool.execute(() -> {
                            gorgeousEngine.StopEngine();
                            onExit(true, "账号冲突，被挤掉线");
                            invalidTaskCallBack(Constant.OnlineStatus.USER_OFFLINE);
                        });
                        return;
                    }
                }
            }
            if (reLogin) {
                return;
            }
            // 掉线未在重登则重登，标志是登陆中
            logging = true;
            reLogin = true;
            reconnectDelay = 300;
            reLoginStartTime = System.currentTimeMillis();
            ThreadPoolConfig.loginThreadPool.execute(() -> {
                invalidTaskCallBack(Constant.OnlineStatus.RE_LOGIN);
                reLogin();
            });
        } catch (Exception ignore) {
            log.info("用户: {}，断开事件处理异常", username);
        }
    }

    @Override
    public void OnSync(ProtocolTreeNode content) {
        String username = loginPack.getUsername();
        if (log.isDebugEnabled()) {
            log.debug("用户：{}，同步事件，内容：{}", username, content);
        }
        String t = content.GetAttributeValue("t");
        long msgTime = Convert.toLong(t, 0L) * 1000;
        long loginTime = 0;
        if (gorgeousEngine != null) {
            loginTime = gorgeousEngine.getLoginTime();
        }
        boolean offlineMessage = WhatsAppUtils.checkIsOfflineMessage(loginTime, msgTime);
        if ("receipt".equals(content.GetTag())) {
            String type = content.GetAttributeValue("type");
            if ("read".equals(type)) {
                String from = content.GetAttributeValue("from");
                String msgId = content.IqId();
                List<String> otherMsgIdList = new ArrayList<>();
                ProtocolTreeNode listNode = content.getOneChildren("list");
                if (listNode != null) {
                    LinkedList<ProtocolTreeNode> itemList = listNode.GetChildren("item");
                    for (ProtocolTreeNode protocolTreeNode : itemList) {
                        String id = protocolTreeNode.GetAttributeValue("id");
                        if (StringUtils.hasLength(id)) {
                            otherMsgIdList.add(id);
                        }
                    }
                }
                ThreadPoolConfig.threadPoolExecutor.execute(() -> {
                    //粉丝已读消息回调
                    if (!StringUtil.IsGroupJid(from)) {
                        StringUtil.JidInfo jidInfo = StringUtil.ParseJid(from);
                        if (StringUtils.hasLength(jidInfo.recipientId)) {
                            String userId = gorgeousEngine.getUserId();
                            if (userId.equals(from)) {
                                return;
                            }
                            AsyncMessageService.parse(jidInfo.getFansId(), getGorgeousEngine().getUserId(), msgId, otherMsgIdList, username, msgTime, offlineMessage);
                        }
                    }
                });
            }
        } else if ("notification".equals(content.GetTag())) {
            String type = content.GetAttributeValue("type");
            if ("w:gp2".equals(type)) {
                AsyncMessageService.parseGroupNotify(getGorgeousEngine().getUserId(), username, content, msgTime, offlineMessage, gorgeousEngine);
            } else if ("picture".equals(type)) {
                ThreadPoolConfig.threadPoolExecutor.execute(() -> {
                    AsyncMessageService.parseGroupSetPictureNotify(getGorgeousEngine().getUserId(), username, content, msgTime, offlineMessage);
                });
            } else if ("privacy_token".equals(type)) {
                String from = content.GetAttributeValue("from");
                ProtocolTreeNode tokens = content.getOneChildren("tokens");
                LinkedList<ProtocolTreeNode> token = tokens.GetChildren("token");
                for (ProtocolTreeNode protocolTreeNode : token) {
                    String trustedContact = protocolTreeNode.GetAttributeValue("type");
                    if ("trusted_contact".equals(trustedContact)) {
                        StringUtil.JidInfo jidInfo = StringUtil.ParseJid(from);
                        byte[] bytes = protocolTreeNode.GetData();
                        if (gorgeousEngine != null) {
                            gorgeousEngine.axolotlManager_.trustedContactStore.setToken(jidInfo.recipientId, bytes);
                            AsyncMessageService.asyncMessagePushService.contactsPrivacyToken(username, new TcToken(jidInfo.recipientId, Base64.getEncoder().encodeToString(bytes)));
                        }
                    }
                }
            }
        }

    }

    @Override
    public void OnPresence(ProtocolTreeNode node) {
        String username = loginPack.getUsername();
        //<presence from='8615228092350@s.whatsapp.net' type='unavailable' last='1615649477'/>
        ThreadPoolConfig.threadPoolExecutor.execute(() -> {
            if (log.isDebugEnabled()) {
                log.debug("用户：{}，状态通知，内容：{}", username, node);
            }
            String from = node.GetAttributeValue("from");
            if (StringUtils.hasLength(from)) {
                String type = node.GetAttributeValue("type");
                long lastOnlineTime = 0;
                boolean nowOnline = false;
                if (StringUtils.hasLength(type)) {
                    String last = node.GetAttributeValue("last");
                    if (StringUtils.hasLength(last)) {
                        if ("deny".equals(last)) {
                            lastOnlineTime = DateUtil.currentSeconds();
                            node.AddAttribute(new StanzaAttribute("invisible", "invisible"));
                        } else {
                            lastOnlineTime = Convert.toLong(last, 0L);
                        }
                        if (lastOnlineTime == 0) {
                            taskNotify.setEventContent(from, ProtocolTreeNode.fail(Constant.FAIL));
                            return;
                        }
                    }
                } else {
                    lastOnlineTime = DateUtil.currentSeconds();
                    nowOnline = true;
                }
                String s = DateUtil.date(lastOnlineTime * 1000).toString();
                node.AddAttribute(new StanzaAttribute("lastOnlineTime", s));
                GorgeousEngine gorgeousEngine = getGorgeousEngine();
                UserIdSubscribeRecord userIdSubscribeRecord = gorgeousEngine.getUserIdSubscribeRecord();
                if (userIdSubscribeRecord != null) {
                    if (nowOnline) {
                        userIdSubscribeRecord.set(username, from, "online");
                    } else {
                        userIdSubscribeRecord.set(username, from, s);
                    }
                }
                taskNotify.setEventContent(from, node);
            }
        });
    }

    @Override
    public void OnPacketResponse(String type, ProtocolTreeNode content) {
        String username = loginPack.getUsername();
        String taskId = content.IqId();
        if (StringUtils.hasLength(taskId)) {
            taskNotify.setEventContent(taskId, content);
            if (this.gorgeousEngine != null) {
                gorgeousEngine.handResponseMsg(type, content);
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("用户：{}，数据包响应事件，类型：{}，内容：{}", username, type, content);
            }
        }
    }

    @Override
    public void taskNotify(GorgeousEngine gorgeousEngine, String username, Runnable runnable) {
        if (this.gorgeousEngine != null && this.gorgeousEngine.equals(gorgeousEngine)) {
            ThreadPoolExecutor thread = CustomThreadPool.getThread(username);
            thread.execute(runnable);
        }
    }

    public User() {
    }

    public User(LoginPack loginPack) {
        this.loginPack = loginPack;
    }

    public boolean start(String username, String configPath, NoiseHandshake.Proxy proxy, String loginId) {
        gorgeousEngine = new GorgeousEngine(username, configPath, this, proxy, mccMnc, loginPack.getWaVersion(), loginPack.isExecuteInit(), loginPack.isHijackMode());
        return gorgeousEngine.StartEngine(loginPack.isIosLogin(), loginPack.isBusinessVersion(), taskNotify, loginId);
    }

    public <T> T sendRequest(AbstractRequest<T> request) {
        if (!isOnline() && request.requiresLogin()) {
            return offLine(request);
        }
        request.setUser(this);
        return request.execute();
    }

    public <T> T sendRequest(AbstractRequest<T> request, String taskId) {
        if (!isOnline() && request.requiresLogin()) {
            return offLine(request);
        }
        request.setUser(this);
        return request.execute(taskId);
    }

    /**
     * 停止
     */
    public void stop() {
        manualStop = true;
        stopEngine();
        onExit(false);
    }

    /**
     * 掉线重登
     */
    public void reLogin() {
        log.info("用户：{}，准备自动重登", loginPack.getUsername());
        LoginResult loginResult = login(1);
        if (Constant.OK.equals(loginResult.getStatus())) {
            return;
        }
        if (markLogout) {
            stop();
            return;
        }
        String errMessage = loginResult.getMessage();
        String violationReason = loginResult.getViolationReason();
        if (Constant.OnlineStatus.WA_LIMIT_LOGIN.equals(errMessage) || Constant.OnlineStatus.LOW_VERSION.equals(errMessage) ||
                Constant.OnlineStatus.KILL.equals(errMessage) || Constant.OnlineStatus.ENV_FAILURE.equals(errMessage)) {
            onExit(true, errMessage, violationReason);
            return;
        }
        if (shouldRetry(errMessage)) {
            long delay = calculateDelay(errMessage);
            long reLoginTIme = System.currentTimeMillis() - reLoginStartTime;
            log.info("用户：{}，延时{}秒重登, 重登持续时间{}秒", loginPack.getUsername(), delay / 1000, reLoginTIme / 1000);
            DelayExecuteTask.addDelayReLogin(loginPack.getUsername(), this, delay, TimeUnit.MILLISECONDS);
            return;
        }
        // 处理退出账号逻辑
        handleExit(errMessage, violationReason);
    }

    private boolean shouldRetry(String errMessage) {
        //封号等情况不需要重试
        if (Constant.OnlineStatus.KILL.equals(errMessage) || Constant.OnlineStatus.ENV_FAILURE.equals(errMessage) ||
                Constant.OnlineStatus.LOW_VERSION.equals(errMessage) || Constant.OnlineStatus.WA_LIMIT_LOGIN.equals(errMessage)) {
            return false;
        }
        // 获取当前重试次数
        int count = reLoginCount.getAndIncrement();
        //503做重试
        if (Constant.OnlineStatus.WA_SERVER_ERROR.equals(errMessage) && count <= 10) {
            return true;
        }
        long reLoginTIme = System.currentTimeMillis() - reLoginStartTime;
        return reLoginTIme < MAX_RE_LOGIN_TIME;
    }

    /**
     * 计算延时事件，单位毫秒
     */
    private long calculateDelay(String errMessage) {
        if (Constant.OnlineStatus.WA_SERVER_ERROR.equals(errMessage)) {
            return 5000;
        }
        if (reconnectDelay >= 10 * 1000) {
            return RandomUtil.randomLong(7000, 10000);
        }
        reconnectDelay = Convert.toLong(reconnectDelay * INC_DELAY_MULTIPLE, 2000L);
        return reconnectDelay;
    }

    private void handleExit(String errMessage, String violationReason) {
        try {
            if (!StringUtils.hasLength(errMessage)) {
                onExit(true);
                return;
            }
            if (!loginPack.isMakeProxy()) {
                onExit(true, Constant.OK, violationReason);
            } else {
                StatusResult statusResult = WhatsAppUtils.checkProxyIp(loginPack.getProxyInfo());
                if (Constant.FAIL.equals(statusResult.getStatus())) {
                    onExit(true, statusResult.getMessage(), violationReason);
                } else {
                    onExit(true, errMessage, violationReason);
                }
            }
        } catch (Exception e) {
            log.error("掉线重登退出异常", e);
        }
        log.info("用户：{}，logging：{}，reLogin：{}，online：{}", loginPack.getUsername(), logging, reLogin, online);
    }


    /**
     * 发送登录请求
     *
     * @return loginResult
     */
    public LoginResult login(int loginCount) {
        String username = loginPack.getUsername();
        LoginResult loginResult = null;
        for (int i = 0; i < loginCount; i++) {
            try {
                stopEngine();
                taskNotify.clear();
                ProxyInfo proxyInfo = loginPack.getProxyInfo();
                /*
                if (ObjectUtil.isNull(this.mccMnc) && loginPack.isUseDynamicIp()) {
                    this.mccMnc = WhatsAppUtils.getMccMncFromProxyIp(username, proxyInfo);
                }
                */
                loginResult = sendRequest(new LoginRequest(loginPack), IdUtil.simpleUUID());
                if (Constant.OK.equals(loginResult.getStatus())) {
                    // 登录成功
                    log.info("用户: {},登录成功", username);
                    logging = false;
                    reLogin = false;
                    markLogout = false;
                    reLoginCount.set(0);
                    UserRecord.getRecord().put(username, this);
                    try {
                        RedisService redisService = SpringUtils.getBean(RedisService.class);
                        if (redisService != null) {
                            redisService.hset(Constant.USER_INFO_KEY, username, loginPack);
                        }
                    } catch (Exception ignored) {
                    }
                    return loginResult;
                } else {
                    stopEngine();
                    String message = loginResult.getMessage();
                    if (Constant.OnlineStatus.WA_SERVER_ERROR.equals(message) || Constant.OnlineStatus.WA_LIMIT_LOGIN.equals(message) || Constant.OnlineStatus.LOW_VERSION.equals(message) || Constant.OnlineStatus.KILL.equals(message) || Constant.OnlineStatus.ENV_FAILURE.equals(message) || Constant.OnlineStatus.UPLOAD_KEY_FAIL.equals(message) || Constant.OnlineStatus.GCM_DISCONNECT.equals(message)) {
                        return loginResult;
                    }
                    //判断是否使用的动态ip，动态ip则需要自动切换
                    boolean switchDynamicIp = switchDynamicIp(loginPack);
                    if (!switchDynamicIp) {
                        if (StringUtils.isEmpty(loginPack.getRolaSwitchUrl())) {
                            return LoginResult.fail(Constant.OnlineStatus.DYNAMIC_IP_FORMAT_ERROR);
                        }
                    }
                }
            } catch (Throwable throwable) {
                stopEngine();
            }
            /*if (loginCount != 1 && i < loginCount - 1) {
                ThreadUtil.sleep(3000);
            }*/
        }
        if (loginResult != null) {
            return loginResult;
        }
        return LoginResult.fail("登录失败");
    }

    public SendSmsRegisterResult checkPhoneExistRequest(RegisterPack registerPack) {
        return sendRequest(new CheckPhoneExistRequest(registerPack));
    }

    public SendSmsRegisterResult sendSms(RegisterPack registerPack) {
        return sendRequest(new SendSmsRequest(registerPack));
    }

    public SubmitRegisterResult submitRegisterRequest(RegisterPack registerPack) {
        return sendRequest(new SubmitRegisterRequest(registerPack));
    }

    public CheckPhoneBlockedResult checkPhoneBlockedRequest(CheckPhoneBlockedPack checkPhoneBlockedPack) {
        return sendRequest(new CheckPhoneBlockedRequest(checkPhoneBlockedPack));
    }

    /**
     * 获取环境信息路径
     */
    public String getEnvPath() {
        String name = FileUtil.getName(envUrl);
        return System.getProperty("user.dir") + "/out/" + name;
    }

    public void onExit(boolean abnormalExit, String message) {
        onExit(abnormalExit, message, "");
    }

    public void onExit(boolean abnormalExit, String message, String violationReason) {
        String username = loginPack.getUsername();
        String key = username + ":logout";
        try {
            KeyLockUtil.lock(key);
            if (logout) {
                return;
            }
            logout = true;
            exit(username, abnormalExit);
            try {
                ApiService apiService = SpringUtils.getBean(ApiService.class);
                if (apiService != null) {
                    apiService.onExit(username, abnormalExit, message, violationReason);
                }
            } catch (Exception ignored) {
            }
        } catch (Exception ignore) {

        } finally {
            KeyLockUtil.unlock(key);
        }
    }

    /**
     * 账号退出
     */
    public void onExit(boolean abnormalExit) {
        String username = loginPack.getUsername();
        String key = username + ":logout";
        try {
            KeyLockUtil.lock(key);
            if (logout) {
                return;
            }
            logout = true;
            exit(username, abnormalExit);
            try {
                ApiService apiService = SpringUtils.getBean(ApiService.class);
                if (apiService != null) {
                    apiService.onExit(username, abnormalExit);
                }
            } catch (Exception ignored) {
            }
        } catch (Exception ignore) {

        } finally {
            KeyLockUtil.unlock(key);
        }
    }

    private void exit(String username, boolean abnormalExit) {
        UserRecord.getRecord().remove(username);
        //发送账号退出事件
        if (!abnormalExit) {
            log.info("用户：{}，手动退出账号", username);
        } else {
            log.info("用户：{}，账号异常退出", username);
        }
        UserRecord.getRecord().remove(username);
        try {
            RedisService redisService = SpringUtils.getBean(RedisService.class);
            if (redisService != null) {
                redisService.hdel(Constant.USER_INFO_KEY, username);
            }
        } catch (Exception ignored) {
        }
        updateEnvInfo();
    }

    public void exit(String username) {
        UserRecord.getRecord().remove(username);
        try {
            RedisService redisService = SpringUtils.getBean(RedisService.class);
            if (redisService != null) {
                redisService.hdel(Constant.USER_INFO_KEY, username);
            }
        } catch (Exception ignored) {
        }
        updateEnvInfo();
    }

    /**
     * 更新环境信息到cos
     */
    public void updateEnvInfo() {
        String envPath = getEnvPath();
        if (!FileUtil.exist(envPath)) {
            return;
        }
        if (reRegistering) {
            String username = loginPack.getUsername();
            WaOldRegistrationService waOldRegistrationService = SpringUtils.getBean(WaOldRegistrationService.class);
            assert waOldRegistrationService != null;
            String verifyCode = waOldRegistrationService.getVerifyCode(username);
            if (StringUtils.hasLength(verifyCode)) {
                FileUtil.del(envPath);
                log.info("用户: {}, 正在重新注册忽略账号退出环境上传", username);
                return;
            }
        }
        String name = FileUtil.getName(envPath);
        String username = loginPack.getUsername();
        OssService ossService = SpringUtils.getBean(OssService.class);
        ThreadPoolConfig.logoutThreadPool.execute(() -> {
            //更新环境信息
            boolean success = false;
            for (int i = 0; i < 3; i++) {
                try {
                    success = ossService.uploadOssEnvFile("env/" + name, new File(envPath));
                } catch (Throwable ignored) {
                }
                if (success) {
                    log.info("用户：{}，退出成功，数据推送到OSS", username);
                    break;
                }
            }
            if (success) {
                FileUtil.del(envPath);
                return;
            }
            log.error("用户：{}，更新环境信息异常", username);
        });
    }

    private void stopEngine() {
        if (gorgeousEngine != null) {
            gorgeousEngine.StopEngine();
        }
    }

    private <T> T offLine(AbstractRequest<T> request) {
        if (reLogin) {
            return request.parseResult(ProtocolTreeNode.fail(Constant.FAIL, Constant.OnlineStatus.RE_LOGIN));
        } else {
            return request.parseResult(ProtocolTreeNode.fail(Constant.FAIL, Constant.OnlineStatus.OFFLINE));
        }
    }

    /**
     * 无效任务回执
     */
    private void invalidTaskCallBack(String message) {
        taskNotify.getRecordObject().keySet().forEach(taskId -> {
            taskNotify.setEventContent(taskId, ProtocolTreeNode.fail(Constant.FAIL, message));

        });
        taskNotify.clear();
    }

    public static boolean switchDynamicIp(LoginPack loginPack) {
        if (loginPack == null) {
            return true;
        }
        if (!loginPack.isUseDynamicIp()) {
            return true;
        } else {
            String rolaSwitchUrl = loginPack.getRolaSwitchUrl();
            if (!loginPack.isMakeProxy()) {
                return true;
            }
            ProxyInfo proxyInfo = loginPack.getProxyInfo();
            if (proxyInfo == null) {
                return true;
            }
            if (StringUtils.hasLength(rolaSwitchUrl)) {
                //切换rola动态ip
                String content = HttpUtils.get(rolaSwitchUrl, 4000);
                try {
                    JSONObject jsonObject = JSONObject.parseObject(content);
                    String ret = jsonObject.getString("Ret");
                    return "SUCCESS".equals(ret);
                } catch (Exception e) {
                    log.error("用户：{}，切换rola代理ip异常", loginPack.getUsername());
                    return false;
                }
            } else {
                String proxyHost = proxyInfo.getProxyHost();
                String proxyUser = proxyInfo.getProxyUser();
                if (proxyHost.contains("rola")) {
                    String randomValue = StrUtil.subBetween(proxyUser, "_", "-");
                    if (!StringUtils.hasLength(randomValue)) {
                        randomValue = StrUtil.subAfter(proxyUser, "_", false);
                    }
                    proxyInfo.setProxyUser(WhatsAppUtils.changeRolaProxy(proxyUser, randomValue));
                } else {
                    String proxyPwd = proxyInfo.getProxyPwd();
                    if (StringUtils.hasLength(proxyPwd)) {
                        String password = StrUtil.subBetween(proxyPwd, "session-", "_lifetime");
                        if (StringUtils.isEmpty(password)) {
                            return false;
                        }
                        proxyInfo.setProxyPwd(WhatsAppUtils.changeProxy(proxyPwd));
                    }
                }
            }
        }
        return true;
    }

    /**
     * 被挤掉线是否超过阈值
     *
     * @return 是否超过阈值
     */
    private boolean exceededThreshold(String username) {
        //如果短时间内账号冲突被挤掉线超过次数，则退出
        try {
            RedisService redisService = SpringUtils.getBean(RedisService.class);
            assert redisService != null;
            String key = "replaced:" + username;
            Long replacedCount = redisService.incrementWithExpire(key, 1, 10 * 60);
            if (replacedCount >= 30) {
                redisService.del(key);
                return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }
}
