package com.whatsapp.android.api.register;

import Env.DeviceEnv;
import ProtocolTree.ProtocolTreeNode;
import axolotl.AxolotlManager;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import cn.hutool.json.JSONObject;
import com.google.protobuf.ByteString;
import com.whatsapp.android.config.DelayExecuteTask;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.OssConstant;
import com.whatsapp.android.entity.ProxyInfo;
import com.whatsapp.android.entity.Register;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.UserRecord;
import com.whatsapp.android.entity.pack.register.RegisterPack;
import com.whatsapp.android.entity.response.register.SubmitRegisterResult;
import com.whatsapp.android.request.AbstractRequest;
import com.whatsapp.android.util.*;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.util.Timeout;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.io.File;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * @author sunnoc
 * @date 2021-03-21 21:23
 */
@Slf4j
public class SubmitRegisterIOSRequest extends AbstractRequest<SubmitRegisterResult> {
    private final RegisterPack registerPack;
    private DeviceEnv.AndroidEnv.Builder envBuild;

    public SubmitRegisterIOSRequest(RegisterPack registerPack) {
        this.registerPack = registerPack;
    }

    @Override
    public String funcName() {
        return null;
    }

    @Override
    public boolean request() {
        return false;
    }

    @Override
    public boolean requiresLogin() {
        return false;
    }

    @Override
    public SubmitRegisterResult execute() {
        String registerKey = registerPack.getRegisterKey();
        if (StringUtils.isEmpty(registerKey)) {
            return new SubmitRegisterResult(StatusResult.fail("注册key不存在，请重新注册"));
        }
        String username = registerPack.getPhoneAreaCode() + registerPack.getPhone();
        String filePath = System.getProperty("user.dir") + "/out/register/" + registerKey + "/" + username;
        ConcurrentMap<String, Register> registerRecord = UserRecord.getRegisterRecord();
        Register register = registerRecord.get(registerKey);
        boolean exist = FileUtil.exist(filePath);
        if (!exist || register == null) {
            return new SubmitRegisterResult(StatusResult.fail("注册环境已释放，请重新注册"));
        }
        try {
            ProxyInfo proxyInfo = registerPack.getProxyInfo();
            AxolotlManager axolotlManager = register.getAxolotlManager();
            Map<String, Object> map = register.getMap();
            byte[] envBuffer = axolotlManager.GetBytesSetting("env");
            if (null != envBuffer) {
                envBuild = DeviceEnv.AndroidEnv.parseFrom(envBuffer).toBuilder();
            } else {
                return new SubmitRegisterResult(StatusResult.fail("注册提交环境为空"));
            }
            DeviceEnv.UserAgent userAgent = envBuild.getUserAgent();
            String device = userAgent.getDevice();
            String osVersion = userAgent.getOsVersion();
            DefaultHttpHeaders entries = WhatsAppUtils.applyRegisterHeaders(CheckPhoneExistIOSRequest.version, osVersion, StrUtil.replace(device, " ", "_"));
            //code=%@&entered=1
            map.put("code", registerPack.getVerifyCode());
            map.put("entered", 1);
            String urlParams = WhatsAppUtils.getUrlParamsByMap(map).replace("+", "-").replace("/", "_");
            log.info("参数：{}", urlParams);
            String enc = jni.Register.GenerateEnc(urlParams);
            String requestUrl = "https://v.whatsapp.net/v2/register?ENC=" + enc;
            HttpClientUtil.ResponseResult responseResult = HttpClientUtil
                    .builder()
                    .proxyInfo(proxyInfo)
                    .headers(entries)
                    .url(requestUrl)
                    .connectTimeoutMillis(6000)
                    .responseTimeout(Duration.ofSeconds(40))
                    .build()
                    .get();
            String message = responseResult.getResultString();
            log.info("用户：{}，SubmitRegisterResult：{}", username, message);
            if (responseResult.isSuccess()) {
                JSONObject responseJson;
                try {
                    responseJson = new JSONObject(message);
                } catch (Exception e) {
                    return new SubmitRegisterResult(StatusResult.fail(message));
                }
                String status = responseJson.getStr("status");
                if (!status.equals("ok")) {
                    String reason = responseJson.getStr("reason");
                    if ("mismatch".equals(reason)) {
                        return new SubmitRegisterResult(StatusResult.fail("验证码有误"));
                    }
                    return new SubmitRegisterResult(StatusResult.fail("注册失败：" + message));
                }
                StatusResult statusResult1 = generateEnv(username, filePath, axolotlManager, responseJson);
                if (Constant.FAIL.equals(statusResult1.getStatus())) {
                    return new SubmitRegisterResult(statusResult1);
                }
                String url = OssConstant.PRE_BUCKET_URL + "env/" + username + ".db";
                AES aes = SecureUtil.aes(Constant.ENV_ENCRYPT_KEY.getBytes());
                String encryptBase64 = aes.encryptBase64(url);
                return new SubmitRegisterResult(username, encryptBase64, StatusResult.ok());
            } else {
                return new SubmitRegisterResult(StatusResult.fail("失败：" + responseResult.getErrMsg()));
            }
        } catch (Exception e) {
            return new SubmitRegisterResult(StatusResult.fail("注册异常：" + e.getMessage()));
        } finally {
            DelayExecuteTask.removeRegisterEnv(registerKey);
            Timeout timeout = register.getTimeout();
            if (timeout != null) {
                timeout.cancel();
            }
        }
    }

    @Override
    public SubmitRegisterResult parseResult(ProtocolTreeNode node) {
        return null;
    }

    public static StatusResult generateEnv(String username, String filePath, AxolotlManager axolotlManager, JSONObject responseJson) {
        try {
            DeviceEnv.AndroidEnv.Builder envBuilder = DeviceEnv.AndroidEnv.parseFrom(axolotlManager.GetBytesSetting("env")).toBuilder();
            if (responseJson.containsKey("edge_routing_info")) {
                envBuilder.setEdgeRoutingInfo(ByteString.copyFrom(Base64.getDecoder().decode(responseJson.getStr("edge_routing_info"))));
            } else {
                envBuilder.setEdgeRoutingInfo(ByteString.copyFrom(Base64.getDecoder().decode("CA0IDA==")));
            }
            if (!responseJson.containsKey("chat_dns_domain")) {
                envBuilder.setChatDnsDomain("fb");
            } else {
                envBuilder.setChatDnsDomain(responseJson.getStr("chat_dns_domain"));
            }
            axolotlManager.SetBytesSetting("env", envBuilder.build().toByteArray());
            //上传环境到cos去
            OssService ossService = SpringUtils.getBean(OssService.class);
            boolean success = ossService.uploadOssEnvFile("env/" + username + ".db", new File(filePath));
            if (!success) {
                return StatusResult.fail("上传环境失败");
            }
        } catch (Exception e) {
            return StatusResult.fail("生成环境异常");
        }
        return StatusResult.ok();
    }
}
