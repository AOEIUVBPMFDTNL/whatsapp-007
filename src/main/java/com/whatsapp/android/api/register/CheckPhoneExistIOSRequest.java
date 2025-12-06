package com.whatsapp.android.api.register;

import Env.DeviceEnv;
import ProtocolTree.ProtocolTreeNode;
import axolotl.AxolotlManager;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.crypto.symmetric.AES;
import cn.hutool.json.JSONObject;
import com.google.protobuf.ByteString;
import com.whatsapp.android.GorgeousEngine;
import com.whatsapp.android.config.DelayExecuteTask;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.OssConstant;
import com.whatsapp.android.entity.*;
import com.whatsapp.android.entity.pack.register.RegisterPack;
import com.whatsapp.android.entity.response.register.SendSmsRegisterResult;
import com.whatsapp.android.request.AbstractRequest;
import com.whatsapp.android.util.DeviceUtil;
import com.whatsapp.android.util.HttpClientUtil;
import com.whatsapp.android.util.WhatsAppUtils;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.util.Timeout;
import jni.NoiseJni;
import jni.Register;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.whispersystems.libsignal.ecc.*;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * 校验注册
 *
 * @author sunnoc
 * @date 2021-03-20 12:00
 */
@Slf4j
public class CheckPhoneExistIOSRequest extends AbstractRequest<SendSmsRegisterResult> {
    private static final byte[] PUBLIC_KEY = {-114, -116, 15, 116, -61, -21, -59, -41, -90, -122, 92, 108, 60, -124, 56, 86, -80, 97, 33, -52, -24, -22, 119, 77, 34, -5, 111, 18, 37, 18, 48, 45};
    //通过插件搜索：<-------wa_MD5String_DO_NOT_USE，得到的字符串前40个字符为STATIC_TOKEN_ENCODE，中间的32个字符为LAST_STATIC_SALT，最后为手机号
    public static final String LAST_STATIC_SALT = "95e155f3e10b9e47761052c36e88d1f3";
    //通过插件搜索：wa_stringValue--ret---->{length = 40
    public static final String STATIC_TOKEN_ENCODE = "0a1mLfGUIBVrMKF1RdvLI5lkRBvof6vn0fD2QRSM";
    public static final String version = "2.23.2.75";
    private final RegisterPack registerPack;
    private AxolotlManager axolotlManager;
    private boolean success = false;
    private boolean needRelease = false;
    private DeviceEnv.AndroidEnv.Builder envBuild = DeviceEnv.AndroidEnv.newBuilder();


    public CheckPhoneExistIOSRequest(RegisterPack registerPack) {
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
    public SendSmsRegisterResult execute() {
        String username = registerPack.getPhoneAreaCode() + registerPack.getPhone();
        String registerKey = IdUtil.simpleUUID();
        String dataDir = System.getProperty("user.dir") + "/out/register/" + registerKey;
        try {
            boolean mkdirs = new File(dataDir).mkdirs();
            if (!mkdirs) {
                return new SendSmsRegisterResult(StatusResult.fail("创建目录失败"));
            }
            String filePath = new File(dataDir, username).getAbsolutePath();
            ProxyInfo proxyInfo = registerPack.getProxyInfo();
            axolotlManager = new AxolotlManager(filePath, username);
            LinkedHashMap<String, Object> map = new LinkedHashMap<>();
            /**
             * cc=852&
             * in=56672698&
             * rc=0&
             * lg=en&
             * lc=US&
             * authkey=IV5KbByskrZ1bpz8SnjgdG8uuUIm21nYh4wmkFtVpWc=&
             * e_regid=Mi5Ciw==&
             * e_keytype=BQ==&
             * e_ident=baAI-u4e7Wi7NBZ9TuHEMxOOKEOZCbZkmNORTaRNu0s=&
             * e_skey_id=uvtE&
             * e_skey_val=KRFZT2PKO9gXFuVjzKQtxLFvcYnXKuO_87mC9tztN2M=&
             * e_skey_sig=kY1WHD-aWg5o7e_kQNO_0Q9JDzxwg3eD18cjiftEgu0W5TdUv_pE0Mga81uCl5n6sCtK5DCsYcrNdC6S75YTAA==&
             * fdid=88E83F96-1E2A-4B8D-81C6-FB76D0A90EEF&
             * expid=5q3oriHHSyiLbWqav0o-6Q==&
             * offline_ab=%7B%22%65%78%70%6F%73%75%72%65%22%3A%5B%5D%2C%22%6D%65%74%72%69%63%73%22%3A%7B%22%65%78%70%69%64%5F%63%22%3A%74%72%75%65%2C%22%66%64%69%64%5F%63%22%3A%74%72%75%65%2C%22%72%63%5F%63%22%3A%74%72%75%65%2C%22%65%78%70%69%64%5F%6D%64%22%3A%31%36%33%32%39%30%31%36%37%35%2C%22%65%78%70%69%64%5F%63%64%22%3A%31%36%33%32%39%30%31%36%37%35%7D%7D&
             * //16个长度的随机值
             * id=%34%11%F7%A6%D5%98%C8%98%CC%27%E3%7E%84%23%54%B5&
             * //20个长度的随机值
             * backup_token=%7F%0A%2F%CC%76%9B%FB%C9%BC%2E%79%CF%53%06%17%DF%DF%42%09%64
             */

            String regId = Base64.getUrlEncoder().encodeToString(ByteString.copyFrom(GorgeousEngine.AdjustId(axolotlManager.getLocalRegistrationId(), 4)).toByteArray());
            String ident = Base64.getUrlEncoder().encodeToString(ByteString.copyFrom(axolotlManager.GetIdentityKeyPair().getPublicKey().serialize(), 1, 32).toByteArray());
            ECKeyPair ecKeyPair = Curve.generateKeyPair();
            byte[] pubSerialize = ByteString.copyFrom(ecKeyPair.getPublicKey().serialize(), 1, 32).toByteArray();
            byte[] priSerialize = ecKeyPair.getPrivateKey().serialize();
            String authKeyPub = Base64.getUrlEncoder().encodeToString(pubSerialize);
            String authKeyPri = Base64.getUrlEncoder().encodeToString(priSerialize);
            SignedPreKeyRecord signedPreKeyRecord = axolotlManager.LoadLatestSignedPreKey(false);
            String sKeyId = Base64.getUrlEncoder().encodeToString(ByteString.copyFrom(GorgeousEngine.AdjustId(signedPreKeyRecord.getId(), 3)).toByteArray());
            String sKeyVal = Base64.getUrlEncoder().encodeToString(ByteString.copyFrom(signedPreKeyRecord.getKeyPair().getPublicKey().serialize(), 1, 32).toByteArray());
            String sKeySig = Base64.getUrlEncoder().encodeToString(signedPreKeyRecord.getSignature());
            String fdId = IdUtil.randomUUID().toUpperCase();
            String expId = Base64.getUrlEncoder().encodeToString(ByteString.copyFrom(IdUtil.randomUUID().toUpperCase().getBytes(), 1, 16).toByteArray());
            long currentSeconds = DateUtil.currentSeconds();
            String content = "{\"exposure\":[],\"metrics\":{\"expid_c\":true,\"fdid_c\":true,\"rc_c\":true,\"expid_md\":" + currentSeconds + ",\"expid_cd\":" + currentSeconds + "}}";
            String offlineAb = WhatsAppUtils.format(HexUtil.encodeHexStr(content.getBytes(StandardCharsets.UTF_8)).toUpperCase(), "%");
            String id = WhatsAppUtils.format(HexUtil.encodeHexStr(RandomUtil.randomBytes(16)).toUpperCase(), "%");
            String backupToken = WhatsAppUtils.format(HexUtil.encodeHexStr(RandomUtil.randomBytes(20)).toUpperCase(), "%");
            log.info("用户：{}，authKeyPub：{}，authKeyPri：{}", username, authKeyPub, authKeyPri);
            map.put("cc", registerPack.getPhoneAreaCode());
            map.put("in", registerPack.getPhone());
            map.put("rc", 0);
            map.put("lg", "en");
            map.put("lc", "US");
            map.put("authkey", authKeyPub);
            map.put("e_regid", regId);
            map.put("e_keytype", "BQ==");
            map.put("e_ident", ident);
            map.put("e_skey_id", sKeyId);
            map.put("e_skey_val", sKeyVal);
            map.put("e_skey_sig", sKeySig);
            map.put("fdid", fdId);
            map.put("expid", expId);
            map.put("offline_ab", offlineAb);
            map.put("id", id);
            map.put("backup_token", backupToken);
            String urlParams = WhatsAppUtils.getUrlParamsByMap(map).replace("+", "-").replace("/", "_");
            log.info("用户：{}，校验注册参数：{}", username, urlParams);
            String enc = Register.GenerateEnc(urlParams);
            log.info("用户：{}，校验注册enc参数：{}", username, enc);

            /**
             * static char static_public_key [32] = {0x8E,0x8C,0xF,0x74,0xC3,0xEB,0xC5,0xD7,0xA6,0x86,0x5C,0x6C,0x3C,0x84,0x38,0x56,0xB0,0x61,0x21,0xCC,0xE8,0xEA,0x77,0x4D,0x22,0xFB,0x6F,0x12,0x25,0x12,0x30,0x2D};
             */
            /*ECKeyPair ecKeyPair1 = Curve.generateKeyPair();
            ECPublicKey ecPublicKey = Curve.decodePoint(PUBLIC_KEY, 0);
            ECPrivateKey privateKey1 = ecKeyPair1.getPrivateKey();
            byte[] bytes = Curve.calculateAgreement(ecPublicKey, privateKey1);*/
            com.alibaba.fastjson.JSONObject jsonObject = DeviceUtil.getCountryInfo("US");
            String mcc = jsonObject.getString("mcc");
            String mnc = jsonObject.getString("mnc");
            if (axolotlManager.GetBytesSetting("env") == null) {
                if (creatIosEnv(username, map, pubSerialize, priSerialize, mcc, mnc, false, envBuild)) {
                    axolotlManager.SetBytesSetting("env", envBuild.build().toByteArray());
                } else {
                    return new SendSmsRegisterResult(StatusResult.fail("创建环境失败"));
                }
            }
            String url = "https://v.whatsapp.net/v2/exist?ENC=" + enc;
            DeviceEnv.UserAgent userAgent = envBuild.getUserAgent();
            String device = userAgent.getDevice();
            String osVersion = userAgent.getOsVersion();
            DefaultHttpHeaders entries = WhatsAppUtils.applyRegisterHeaders(version, osVersion, StrUtil.replace(device, " ", "_"));
            HttpClientUtil.ResponseResult responseResult = HttpClientUtil
                    .builder()
                    .proxyInfo(proxyInfo)
                    .headers(entries)
                    .url(url)
                    .connectTimeoutMillis(6000)
                    .responseTimeout(Duration.ofSeconds(40))
                    .build()
                    .get();
            String resultString = responseResult.getResultString();
            log.info("用户：{}，CheckPhoneExistResult：{}", username, resultString);
            String regOnboard = HttpClientUtil
                    .builder()
                    .proxyInfo(proxyInfo)
                    .headers(entries)
                    .url("https://v.whatsapp.net/v2/reg_onboard_abprop?cc=" + registerPack.getPhoneAreaCode() + "&in=" + registerPack.getPhone() + "&rc=0")
                    .connectTimeoutMillis(6000)
                    .responseTimeout(Duration.ofSeconds(40))
                    .build()
                    .get().getResultString();
            log.info("用户：{}，regOnboard：{}", username, regOnboard);
            ThreadUtil.sleep(RandomUtil.randomInt(2000, 5000));
            if (responseResult.isSuccess()) {
                JSONObject responseJson;
                try {
                    responseJson = new JSONObject(resultString);
                } catch (Exception e) {
                    return new SendSmsRegisterResult(StatusResult.fail(resultString));
                }
                String status = responseJson.getStr("status");
                if (Constant.OK.equals(status)) {
                    if (responseJson.containsKey("edge_routing_info")) {
                        envBuild.setEdgeRoutingInfo(ByteString.copyFrom(Base64.getDecoder().decode(responseJson.getStr("edge_routing_info"))));
                    } else {
                        envBuild.setEdgeRoutingInfo(ByteString.copyFrom(Base64.getDecoder().decode("CA0IDA==")));
                    }
                    if (!responseJson.containsKey("chat_dns_domain")) {
                        envBuild.setChatDnsDomain("fb");
                    } else {
                        envBuild.setChatDnsDomain(responseJson.getStr("chat_dns_domain"));
                    }
                    axolotlManager.SetBytesSetting("env", envBuild.build().toByteArray());
                }
                //发送注册验证码
                return RequestCode(username, registerPack, proxyInfo, registerKey, dataDir, map, mcc, mnc);
            } else {
                FileUtil.del(dataDir);
                return new SendSmsRegisterResult(StatusResult.fail("失败：" + responseResult.getErrMsg()));
            }
        } catch (Exception e) {
            return new SendSmsRegisterResult(StatusResult.fail("执行异常"));
        } finally {
            if (success && needRelease) {
                axolotlManager.Close();
                FileUtil.del(dataDir);
            } else if (!success) {
                axolotlManager.Close();
                FileUtil.del(dataDir);
            }
        }

    }

    @Override
    public SendSmsRegisterResult parseResult(ProtocolTreeNode node) {
        return null;
    }

    private SendSmsRegisterResult RequestCode(String username, RegisterPack registerPack, ProxyInfo proxyInfo, String registerKey, String dataDir, Map<String, Object> checkExistUrlParams, String mcc, String mnc) {
        String filePath = new File(dataDir, username).getAbsolutePath();

        checkExistUrlParams.remove("offline_ab");
        //method=sms&sim_mcc=%@&sim_mnc=%@&
        checkExistUrlParams.put("method", "sms");
        checkExistUrlParams.put("sim_mcc", mcc);
        checkExistUrlParams.put("sim_mnc", mnc);
        //"static_token_encode+last_static_salt+手机号"
        String token = DigestUtil.md5Hex(STATIC_TOKEN_ENCODE + LAST_STATIC_SALT + checkExistUrlParams.get("in"));
        checkExistUrlParams.put("token", token);
        String urlParams = WhatsAppUtils.getUrlParamsByMap(checkExistUrlParams).replace("+", "-").replace("/", "_");
        log.info("用户：{}，发送验证码参数：{}", username, urlParams);
        String enc = Register.GenerateEnc(urlParams);
        log.info("用户：{}，发送验证码enc参数：{}", username, enc);
        String sendVerifyCodeUrl = "https://v.whatsapp.net/v2/code?ENC=" + enc;
        String message = null;
        try {
            DeviceEnv.UserAgent userAgent = envBuild.getUserAgent();
            String device = userAgent.getDevice();
            String osVersion = userAgent.getOsVersion();
            DefaultHttpHeaders entries = WhatsAppUtils.applyRegisterHeaders(version, osVersion, StrUtil.replace(device, " ", "_"));
            HttpClientUtil.ResponseResult responseResult = HttpClientUtil
                    .builder()
                    .proxyInfo(proxyInfo)
                    .headers(entries)
                    .url(sendVerifyCodeUrl)
                    .connectTimeoutMillis(6000)
                    .responseTimeout(Duration.ofSeconds(40))
                    .build()
                    .get();
            if (responseResult.isSuccess()) {
                message = responseResult.getResultString();
                log.info("用户：{}，CheckPhoneExistResult->RequestCode：{}", username, message);
                JSONObject responseJson;
                try {
                    responseJson = new JSONObject(message);
                } catch (Exception e) {
                    return new SendSmsRegisterResult(StatusResult.fail(message));
                }
                String status = responseJson.getStr("status");
                if ("sent".equals(status)) {
                    ConcurrentMap<String, com.whatsapp.android.entity.Register> registerRecord = UserRecord.getRegisterRecord();
                    com.whatsapp.android.entity.Register register = new com.whatsapp.android.entity.Register();
                    register.setRegisterDataDir(dataDir);
                    register.setUsername(username);
                    register.setAxolotlManager(axolotlManager);
                    checkExistUrlParams.put("method", "sms");
                    checkExistUrlParams.remove("method");
                    checkExistUrlParams.remove("token");
                    register.setMap(checkExistUrlParams);
                    DelayTask delayTask = new DelayTask();
                    delayTask.setRegisterKey(registerKey);
                    delayTask.setUsername(username);
                    //添加延时校验是否提交验证码注册
                    Timeout timeout = DelayExecuteTask.addDelayTask(delayTask, 6 * 60 * 1000);
                    register.setTimeout(timeout);
                    registerRecord.put(registerKey, register);
                    success = true;
                    return new SendSmsRegisterResult(registerKey, responseJson, StatusResult.ok());
                } else if ("ok".equals(status)) {
                    //代表账号已经可以使用
                    StatusResult statusResult1 = SubmitRegisterIOSRequest.generateEnv(username, filePath, axolotlManager, responseJson);
                    if (Constant.FAIL.equals(statusResult1.getStatus())) {
                        return new SendSmsRegisterResult(statusResult1);
                    }
                    String url = OssConstant.PRE_BUCKET_URL + "env/" + username + ".db";
                    AES aes = SecureUtil.aes(Constant.ENV_ENCRYPT_KEY.getBytes());
                    String encryptBase64 = aes.encryptBase64(url);
                    success = true;
                    needRelease = true;
                    return new SendSmsRegisterResult(username, encryptBase64, true, StatusResult.ok());
                } else if ("fail".equals(status)) {
                    String reason = responseJson.getStr("reason");
                    if ("too_recent".equals(reason)) {
                        int smsWait = responseJson.getInt("sms_wait", 0);
                        return new SendSmsRegisterResult(registerKey, StatusResult.fail("请求太频繁，等待" + smsWait + "秒"));
                    }
                    return new SendSmsRegisterResult(registerKey, StatusResult.fail(responseResult.getErrMsg()));
                }
            } else {
                return new SendSmsRegisterResult(registerKey, StatusResult.fail(responseResult.getErrMsg()));

            }
        } catch (Exception e) {
            log.error("发送验证码异常", e);
        }
        if (StringUtils.hasLength(message)) {
            return new SendSmsRegisterResult(registerKey, StatusResult.fail("发送验证码失败：" + message));
        } else {
            return new SendSmsRegisterResult(registerKey, StatusResult.fail("发送验证码失败"));
        }
    }

    private boolean creatIosEnv(String username, Map<String, Object> map, byte[] publicKey, byte[] privateKey, String mcc, String mnc, boolean businessVersion, DeviceEnv.AndroidEnv.Builder envBuild) {
        try {
            envBuild.setFullphone(username);
            envBuild.setExpid(ByteString.copyFrom((String) map.get("expid"), "UTF-8"));
            envBuild.setFdid((String) map.get("fdid"));
            if (businessVersion) {
                envBuild.getUserAgentBuilder().setPlatform(DeviceEnv.Platform.SMB_IOS);
            } else {
                envBuild.getUserAgentBuilder().setPlatform(DeviceEnv.Platform.IOS);
            }
            DeviceEnv.UserAgent.Builder userAgentBuilder = DeviceEnv.UserAgent.newBuilder();
            DeviceEnv.AppVersion.Builder appVersionBuilder = userAgentBuilder.getAppVersionBuilder();
            String[] versions = version.split("\\.");
            appVersionBuilder.setPrimary(Integer.parseInt(versions[0]));
            appVersionBuilder.setSecondary(Integer.parseInt(versions[1]));
            appVersionBuilder.setTertiary(Integer.parseInt(versions[2]));
            if (versions.length >= 4) {
                appVersionBuilder.setQuaternary(Integer.parseInt(versions[3]));
            }
            userAgentBuilder.setReleaseChannel(DeviceEnv.ReleaseChannel.RELEASE);
            DeviceEnv.KeyPair.Builder clientStaticKeyPairBuilder = envBuild.getClientStaticKeyPairBuilder();
            clientStaticKeyPairBuilder.setStrPubKey(ByteString.copyFrom(publicKey));
            clientStaticKeyPairBuilder.setStrPrivateKey(ByteString.copyFrom(privateKey));
            DeviceInfo deviceInfo = DeviceUtil.randomGetOneIosDevice();
            if (deviceInfo == null) {
                return false;
            }
            userAgentBuilder.setManufacturer(deviceInfo.getManufacturer());
            userAgentBuilder.setDevice(deviceInfo.getDevice());
            userAgentBuilder.setOsVersion(deviceInfo.getVersion());
            userAgentBuilder.setOsBuildNumber(deviceInfo.getBuild());
            userAgentBuilder.setMcc(mcc);
            userAgentBuilder.setMnc(mnc);
            userAgentBuilder.setPhoneId((String) map.get("fdid"));
            userAgentBuilder.setLocaleLanguageIso6391((String) map.get("lg"));
            userAgentBuilder.setLocaleCountryIso31661Alpha2((String) map.get("lc"));
            envBuild.setUserAgent(userAgentBuilder);
            // envBuild.getOrignalToken()
        } catch (Exception ingore) {
            return false;
        }
        return true;
    }

}
