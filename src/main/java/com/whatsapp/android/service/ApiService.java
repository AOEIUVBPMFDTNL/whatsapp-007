package com.whatsapp.android.service;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.config.ThreadPoolConfig;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.ServiceConstant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.entity.UserRecord;
import com.whatsapp.android.entity.pack.account.InitializePack;
import com.whatsapp.android.entity.pack.account.LoginPack;
import com.whatsapp.android.run.StartedUpRunner;
import com.whatsapp.android.service.impl.account.LoginService;
import com.whatsapp.android.util.RedisService;
import com.whatsapp.android.ws.WebSocketChatClient;
import com.whatsapp.android.ws.WebSocketClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.core.StandardThreadExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;


/**
 * @author sunnoc
 * @date 2020-07-28 19:37
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApiService {
    private final RedisService redisService;
    private final LoginService loginService;
    @Value("${terminal.version}")
    private String terminalVersion;

    @Autowired
    ServiceConstant serviceConstant;

    /**
     * 下载文件
     *
     * @param file 文件
     * @return byte[]
     */
    public byte[] downPhoto(String file) {
        byte[] fileBytes = null;
        if (StrUtil.startWith(file, "http")) {
            try (cn.hutool.http.HttpResponse response = HttpRequest.get(file).timeout(60 * 1000).execute()) {
                if (response.isOk()) {
                    fileBytes = response.bodyBytes();
                }
            } catch (Exception ignored) {
            }
        } else {
            fileBytes = Base64.decode(file);
        }
        return fileBytes;
    }

    /**
     * 下载文件
     *
     * @param file    文件
     * @param timeOut 超时时间
     * @return byte[]
     */
    public byte[] downPhoto(String file, int timeOut) {
        byte[] fileBytes = null;
        if (StrUtil.startWith(file, "http")) {
            try (cn.hutool.http.HttpResponse response = HttpRequest.get(file).timeout(timeOut).execute()) {
                if (response.isOk()) {
                    fileBytes = response.bodyBytes();
                }
            } catch (Exception ignored) {
            }
        } else {
            fileBytes = Base64.decode(file);
        }
        return fileBytes;
    }

    public String downloadResources(String file, int timeOut) {
        String content = null;
        if (StrUtil.startWith(file, "http")) {
            try (cn.hutool.http.HttpResponse response = HttpRequest.get(file).timeout(timeOut).execute()) {
                if (response.isOk()) {
                    content = response.body();
                }
            } catch (Exception ignored) {
            }
        } else {
            return file;
        }
        return content;
    }

    /**
     * 下载终端
     *
     * @param updateKey 更新key
     * @return bool
     */
    public boolean downTerminal(String updateKey) {
        AES aes = SecureUtil.aes(Constant.UPDATE_TERMINAL_URL_KEY.getBytes());
        String url = aes.decryptStr(updateKey);
        byte[] bytes = downPhoto(url);
        if (bytes != null && bytes.length > 500) {
            FileUtil.writeBytes(bytes, Constant.JAR_PATH + Constant.JAR_PACK_NAME + ".update");
            return true;
        } else {
            log.error("下载更新文件失败");
            return false;
        }
    }

    /**
     * 初始化账号上线，每个账号上线延时5-10秒
     */
    public void initializeUserOnline() {
        Map<Object, Object> map = redisService.hmget(Constant.USER_INFO_KEY);
        long startTime = System.currentTimeMillis();
        log.info("上线之前账号，数量：{}个", map.size());
        StandardThreadExecutor loginInitThreadExecutor = ThreadPoolConfig.startThreadPool("login-thread-", 9, 9);
        CountDownLatch countDownLatch = map.size() > 0 ? new CountDownLatch(map.size()) : null;
        for (Map.Entry<Object, Object> next : map.entrySet()) {
            LoginPack fbLoginPack = (LoginPack) next.getValue();
            fbLoginPack.setUpdateTerminalLogin(true);
            fbLoginPack.setAsync(false);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("data", fbLoginPack);
            loginInitThreadExecutor.execute(() -> {
                try {
                    //先删除再登录
                    String username = fbLoginPack.getUsername();
                    redisService.hdel(Constant.USER_INFO_KEY, username);
                    //登录
                    loginService.execute(TypeConstant.TaskType.LOGIN, jsonObject, UUID.randomUUID().toString(), null);
                } catch (Exception e) {
                    log.error("用户：{}，初始化上线异常", fbLoginPack.getUsername(), e);
                } finally {
                    countDownLatch.countDown();
                }
            });

        }
        if (map.size() > 0) {
            try {
                countDownLatch.await();
            } catch (Exception ignored) {
            }
        }
        ThreadPoolConfig.stopThreadPool(loginInitThreadExecutor);
        long endTime = System.currentTimeMillis();
        long time = (endTime - startTime) / 1000;
        log.info("上线完毕，耗时：{}秒", time);
    }

    /**
     * 初始化账号信息
     */
    public void initialize(String uuid, String name, String key, String type, boolean print) {
        //ConcurrentMap<String, User> record = UserRecord.getRecord();
        Set<Object> keys = redisService.hgetKeys(Constant.USER_INFO_KEY);
        Map<String, Object> map = new HashMap<>(4);
        map.put("type", type);
        map.put("version", terminalVersion);
        map.put("platformType", Constant.PLATFORM_TYPE);
        map.put("countryCode", StartedUpRunner.countryCode);
        map.put("data", new InitializePack(uuid, WebSocketChatClient.getTerminal().getIp(), name, key,keys.stream().map(Object::toString).collect(Collectors.toList())));
        WebSocketClient.sendMsg(JSONObject.toJSONString(map), print);
    }


    /**
     * 账号退出通知
     */
    public void onExit(String username, boolean abnormalExit) {
        onExit(username, abnormalExit, "账号退出");
    }

    public void onExit(String username, boolean abnormalExit, String message) {
        onExit(username, abnormalExit, message, "");
    }

    public void onExit(String username, boolean abnormalExit, String message, String violationReason) {
        HashMap<String, Object> map = new HashMap<>(5);
        map.put("status", Constant.OK);
        map.put("message", message);
        map.put("violationReason", violationReason);
        map.put("terminalName", WebSocketChatClient.getTerminal().getName());
        map.put("abnormalExit", abnormalExit);
        map.put("terminalOffline", false);
        if (serviceConstant.isGzip()) {
            if (WebSocketChatClient.getTerminal() != null) {
                initialize(WebSocketChatClient.getTerminal().getUuid(), WebSocketChatClient.getTerminal().getName(), Constant.KEY, TypeConstant.PING, false);
            }
        }
        WebSocketClient.sendMsg(username, Result.callback(TypeConstant.NotifyType.ON_EXIT, UUID.randomUUID().toString(), username, map).toJson());
    }

    /**
     * 登录成功
     */
    public void onLoginSuccess(String username, Object data) {
        if (serviceConstant.isGzip()) {
            if (WebSocketChatClient.getTerminal() != null) {
                initialize(WebSocketChatClient.getTerminal().getUuid(), WebSocketChatClient.getTerminal().getName(), Constant.KEY, TypeConstant.PING, false);
            }
        }
        WebSocketClient.sendMsg(username, Result.callback(TypeConstant.NotifyType.ON_LOGIN_SUCCESS, UUID.randomUUID().toString(), username, data).toJson());
    }

    /**
     * 登录失败
     */
    public void onLoginFail(String username, Object data) {
        WebSocketClient.sendMsg(username, Result.callback(TypeConstant.NotifyType.ON_LOGIN_FAIL, UUID.randomUUID().toString(), username, data).toJson());
    }


    /**
     * 缓存登录信息
     */
    public void cacheLoginInfo(User user) {
        redisService.hset(Constant.USER_INFO_KEY, user.getLoginPack().getUsername(), user.getLoginPack());
    }


}
