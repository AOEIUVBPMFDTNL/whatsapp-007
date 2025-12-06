package com.whatsapp.android.api.account;

import Handshake.NoiseHandshake;
import ProtocolTree.ProtocolTreeNode;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import cn.hutool.http.HttpException;
import cn.hutool.http.HttpUtil;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.ProxyInfo;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.pack.account.LoginPack;
import com.whatsapp.android.entity.response.account.LoginResult;
import com.whatsapp.android.request.AbstractRequest;
import jni.NoiseJni;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;


/**
 * 登陆账号
 *
 * @author sunnoc
 * @date 2021-03-09 10:13
 */
@Slf4j
public class LoginRequest extends AbstractRequest<LoginResult> {
    private final LoginPack loginPack;

    public LoginRequest(LoginPack loginPack) {
        this.loginPack = loginPack;
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.LOGIN;
    }

    @Override
    public long timeOut() {
        return 10;
    }

    @Override
    public boolean request() {
        String username = loginPack.getUsername();
        String envEncryptKey = loginPack.getEnvEncryptKey();
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(envEncryptKey)) {
            user.getTaskNotify().setEventContent(getTaskId(), ProtocolTreeNode.fail("loginFail", "用户名或环境不能为空"));
            return true;
        }
        String envUrl;
        try {
            AES aes = SecureUtil.aes(Constant.ENV_ENCRYPT_KEY.getBytes());
            envUrl = aes.decryptStr(envEncryptKey);
            if (StrUtil.indexOf(envUrl, "http", 0, false) == -1) {
                user.getTaskNotify().setEventContent(getTaskId(), ProtocolTreeNode.fail("loginFail", "加密环境异常"));
                return true;
            }
        } catch (Exception e) {
            user.getTaskNotify().setEventContent(getTaskId(), ProtocolTreeNode.fail("loginFail", "环境解密异常"));
            return true;
        }
        user.setEnvUrl(envUrl);
        String envPath = user.getEnvPath();
        if (!FileUtil.exist(envPath)) {
            try {
                long length = 0;
                if (envUrl.contains("aliyuncs.com")) {
                    try {
                        length = HttpUtil.downloadFile(envUrl, FileUtil.file(envPath), 60 * 1000);
                    } catch (HttpException e) {
                        //文件不存在的情况
                        if (e.getMessage().contains("404")) {
                            String cosUrl = "https://nanshan-whatsapp-1257892306.cos.ap-singapore.myqcloud.com/env/" + username + ".db";
                            length = HttpUtil.downloadFile(cosUrl, FileUtil.file(envPath), 60 * 1000);
                        }
                    }
                } else {
                    length = HttpUtil.downloadFile(envUrl, FileUtil.file(envPath), 60 * 1000);
                }
                if (length <= 200) {
                    user.getTaskNotify().setEventContent(getTaskId(), ProtocolTreeNode.fail("loginFail", "下载环境文件失败"));
                    return true;
                }
            } catch (Throwable throwable) {
                log.error("用户：{}，下载文件环境异常", username, throwable);
                user.getTaskNotify().setEventContent(getTaskId(), ProtocolTreeNode.fail("loginFail", "下载环境文件异常"));
                return true;
            }
        }
        NoiseHandshake.Proxy proxy = null;
        if (loginPack.isMakeProxy()) {
            ProxyInfo proxyInfo = loginPack.getProxyInfo();
            proxy = new NoiseHandshake.Proxy();
            proxy.type = proxyInfo.getType();
            proxy.server = proxyInfo.getProxyHost();
            proxy.port = proxyInfo.getProxyPort();
            proxy.userName = proxyInfo.getProxyUser();
            proxy.password = proxyInfo.getProxyPwd();
        }
        log.info("用户: {}, login ip: {}", username, loginPack.getProxyInfo());
        boolean start = user.start(username, envPath, proxy, getTaskId());
        if (!start) {
            user.getGorgeousEngine().StopEngine();
            user.getTaskNotify().setEventContent(getTaskId(), ProtocolTreeNode.fail("loginFail"));
            return true;
        }
        user.getLoginPack().setUserId(user.getGorgeousEngine().getUserId());
        return true;
    }

    @Override
    public boolean requiresLogin() {
        return false;
    }

    @Override
    public LoginResult parseResult(ProtocolTreeNode node) {
        LoginResult checkResult = checkResult(node, LoginResult.class);
        if (checkResult != null) {
            return checkResult;
        }
        if ("success".equals(node.GetTag())) {
            LoginResult loginResult = new LoginResult();
            try {
                String creationTime = user.getGorgeousEngine().getCreationTime();
                loginResult.setCreationTime(creationTime);
                loginResult.setUserId(user.getLoginPack().getUserId());
            } catch (Exception ignored) {
            }
            return loginResult;
        } else if ("loginFail".equals(node.GetTag())) {
            return LoginResult.fail(ProtocolTreeNode.getMessage(node));
        } else if ("failure".equals(node.GetTag())) {
            String reason = node.GetAttributeValue("reason");
            if ("403".equals(reason)) {
                String violationReason = node.GetAttributeValue("violation_reason");
                LoginResult loginResult = LoginResult.fail(Constant.OnlineStatus.KILL);
                loginResult.setViolationReason(violationReason);
                return loginResult;
            } else if ("401".equals(reason) || "404".equals(reason)) {
                return LoginResult.fail(Constant.OnlineStatus.ENV_FAILURE);
            } else if ("405".equals(reason)) {
                return LoginResult.fail(Constant.OnlineStatus.LOW_VERSION);
            } else if ("402".equals(reason)) {
                return LoginResult.fail(Constant.OnlineStatus.WA_LIMIT_LOGIN);
            } else if ("503".equals(reason)) {
                return LoginResult.fail(Constant.OnlineStatus.WA_SERVER_ERROR);
            } else if (Constant.STATUS_CODE.containsKey(reason)) {
                return LoginResult.fail((String) Constant.STATUS_CODE.get(reason));
            } else {
                return LoginResult.fail(node.toString());
            }
        } else {
            return LoginResult.fail(node.toString());
        }
    }

    /**
     * 校验whatsapp版本
     */
    private StatusResult checkWhatsAppVersion(NoiseHandshake.Proxy proxy_) {
        //proxyType :  "socks5" or "http"
        String status = null;
        String jniDir = System.getProperty("user.dir") + "/jni";
        if (proxy_ != null) {
            if (proxy_.type == 0) {
                if (StringUtils.hasLength(proxy_.userName)) {
                    status = NoiseJni.CheckWhatsappVersion("http", proxy_.server, proxy_.port, proxy_.userName, proxy_.password, jniDir);
                } else {
                    status = NoiseJni.CheckWhatsappVersion("http", proxy_.server, proxy_.port, "", "", jniDir);
                }
            } else {
                if (StringUtils.hasLength(proxy_.userName)) {
                    status = NoiseJni.CheckWhatsappVersion("socks5", proxy_.server, proxy_.port, proxy_.userName, proxy_.password, jniDir);
                } else {
                    status = NoiseJni.CheckWhatsappVersion("socks5", proxy_.server, proxy_.port, "", "", jniDir);
                }
            }
        }
        String errMsg;
        if ("whatsapp version is older".equals(status)) {
            errMsg = "校验whatsApp版本太旧了";
            log.error(errMsg);
            return StatusResult.fail(errMsg);
        }
        if (!"success".equals(status)) {
            errMsg = "校验whatsApp版本失败，可能代理ip存在问题";
            log.error(errMsg);
            return StatusResult.fail(errMsg);
        }
        return StatusResult.ok();
    }
}
