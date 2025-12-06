package com.whatsapp.android.service.impl.account;

import cn.hutool.core.thread.ThreadUtil;
import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.annotation.ApiType;
import com.whatsapp.android.config.ThreadPoolConfig;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.ServiceConstant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.entity.UserRecord;
import com.whatsapp.android.entity.pack.account.LoginPack;
import com.whatsapp.android.entity.response.account.LoginResult;
import com.whatsapp.android.service.ApiService;
import com.whatsapp.android.service.ApiStrategy;
import com.whatsapp.android.util.RedisService;
import com.whatsapp.android.util.WhatsAppUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 账号登录
 *
 * @author sunnoc
 * @date 2021-03-16 18:02
 */
@Service
@Slf4j
@RequiredArgsConstructor
@ApiType(TypeConstant.TaskType.LOGIN)
public class LoginService implements ApiStrategy {
    private final RedisService redisService;
    @Lazy
    @Autowired(required = false)
    ApiService apiService;

    @Autowired
    ServiceConstant serviceConstant;

    @Override
    public Result execute(String type, JSONObject dataObject, String taskId, User user1) {
        LoginPack loginPack = dataObject.getObject("data", LoginPack.class);
        if (loginPack.isUpdateTerminalLogin()) {
            loginPack.setUpdateTerminalLogin(false);
        } else {
            //判断是否使用的动态ip，动态ip则需要自动切换
            boolean switchDynamicIp = false;
            for (int i = 0; i < 3; i++) {
                switchDynamicIp = User.switchDynamicIp(loginPack);
                if (!switchDynamicIp) {
                    if (StringUtils.isEmpty(loginPack.getRolaSwitchUrl())) {
                        break;
                    }
                    ThreadUtil.sleep(100);
                } else {
                    break;
                }
            }
            if (!switchDynamicIp) {
                LoginResult loginResult;
                if (StringUtils.isEmpty(loginPack.getRolaSwitchUrl())) {
                    loginResult = LoginResult.fail(Constant.OnlineStatus.DYNAMIC_IP_FORMAT_ERROR);
                } else {
                    loginResult = LoginResult.fail("切换rola动态ip失败");
                }
                apiService.onLoginFail(loginPack.getUsername(), loginResult);
                return Result.taskFail(type, taskId, loginPack.getUsername(), loginResult);
            }
        }
        String username = loginPack.getUsername();
        //登陆加锁
        String loginLockKey = "loginLock:" + Constant.KEY + ":" + username;
        try {
            Long loginIndex = redisService.incr(loginLockKey, 1L);
            redisService.expire(loginLockKey, 60 * 60 * 12L);
            if (loginIndex > 1) {
                //当前账号正在登录中
                return Result.taskFail(type, taskId, username, LoginResult.fail(Constant.OnlineStatus.USER_LOGGING));
            }
            //校验用户是否在线，在线则不让登录
            if (UserRecord.getRecord().containsKey(username)) {
                redisService.del(loginLockKey);
                return Result.taskFail(type, taskId, username, LoginResult.fail(Constant.OnlineStatus.USER_ONLINE));
            }
        } catch (Exception ignored) {
        }
        if (loginPack.isAsync()) {
            ThreadPoolConfig.loginThreadPool.execute(() -> {
                login(username, loginPack, type, taskId, loginLockKey);
            });
            return Result.taskSuccess(type, taskId, username, StatusResult.ok("开始异步登陆中.."));
        } else {
            return login(username, loginPack, type, taskId, loginLockKey);
        }
    }

    private Result login(String username, LoginPack loginPack, String type, String taskId, String loginLockKey) {
        User user = null;
        try {
            //开始登陆
            user = new User(loginPack);
            user.setLogging(true);
            user.setReLogin(true);
            boolean success = false;
            //失败重试最多三次
            LoginResult loginResult = user.login(3);
            if (Constant.OK.equals(loginResult.getStatus())) {
                success = true;
            }
            if (success && !user.isOnline()) {
                loginResult.setStatus(Constant.FAIL);
                loginResult.setMessage("登陆失败");
            } else if (success) {
                user.setLogging(false);
            }
            if (Constant.OK.equals(loginResult.getStatus())) {
                apiService.onLoginSuccess(username, loginResult);
                return Result.taskSuccess(type, taskId, username, loginResult);
            } else {
                if (needCheckIp(loginResult.getMessage())) {
                    if (loginPack.isMakeProxy()) {
                        //校验代理是否有网络
                        StatusResult statusResult = WhatsAppUtils.checkProxyIp(loginPack.getProxyInfo());
                        if (Constant.FAIL.equals(statusResult.getStatus())) {
                            loginResult.setMessage(statusResult.getMessage());
                        }
                    }
                }
                apiService.onLoginFail(username, loginResult);
                user.exit(username);
                return Result.taskFail(type, taskId, username, loginResult);
            }
        } catch (Exception ignore) {
            StatusResult statusResult = StatusResult.fail("登录异常");
            apiService.onLoginFail(username, statusResult);
            if (user != null) {
                user.exit(username);
            }
            return Result.taskFail(type, taskId, username, statusResult);
        } finally {
            redisService.del(loginLockKey);
        }
    }

    private boolean needCheckIp(String errMsg) {
        if (Constant.OnlineStatus.WA_SERVER_ERROR.equals(errMsg) || Constant.OnlineStatus.WA_LIMIT_LOGIN.equals(errMsg) || Constant.OnlineStatus.LOW_VERSION.equals(errMsg) ||
                Constant.OnlineStatus.KILL.equals(errMsg) || Constant.OnlineStatus.ENV_FAILURE.equals(errMsg) ||
                Constant.OnlineStatus.UPLOAD_KEY_FAIL.equals(errMsg) || Constant.OnlineStatus.GCM_DISCONNECT.equals(errMsg)) {
            return false;
        }
        return !Constant.OnlineStatus.DYNAMIC_IP_FORMAT_ERROR.equals(errMsg);
    }
}
