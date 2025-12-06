package com.whatsapp.android.util;

import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;
import com.whatsapp.android.constant.RegisterInfoConstant;
import com.whatsapp.android.entity.DeviceInfo;
import com.whatsapp.android.entity.pack.register.AndroidRegisterEnv;
import com.whatsapp.android.entity.pack.register.RegisterPack;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.util.StringUtils;
import org.whispersystems.curve25519.Curve25519;
import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.ecc.Curve;
import org.whispersystems.libsignal.ecc.DjbECPrivateKey;
import org.whispersystems.libsignal.ecc.DjbECPublicKey;
import org.whispersystems.libsignal.ecc.ECKeyPair;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;
import org.whispersystems.libsignal.util.ByteUtil;
import org.whispersystems.libsignal.util.KeyHelper;

import javax.crypto.KeyGenerator;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.Security;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static com.whatsapp.android.GorgeousEngine.AdjustId;
import static org.whispersystems.curve25519.Curve25519.BEST;

/**
 * wa注册助手
 *
 * @author sunnoc
 * @date 2023-07-19 11:39
 */

@Data
@Slf4j
public class WaRegisterHelper {
    /**
     * wa注册公钥
     */
    public static final byte[] REGISTRATION_PUBLIC_KEY = HexUtil.decodeHex("8e8c0f74c3ebc5d7a6865c6c3c843856b06121cce8ea774d22fb6f122512302d");
    private static final String[] DEVICE_RAM_LIST = {"3.9", "5.58", "7.44"};
    private String username;
    private RegisterPack registerPack;
    private AndroidRegisterEnv androidRegisterEnv;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public WaRegisterHelper() {
    }

    public WaRegisterHelper(String username, RegisterPack registerPack) {
        this.username = username;
        this.registerPack = registerPack;
        this.androidRegisterEnv = generateEnv();
    }

    public HttpClientUtil.ResponseResult checkExistRequest() {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        baseParameter(map);
        map.put("offline_ab", androidRegisterEnv.getOfflineAb());
        map.put("language_selector_clicked_count", 0);
        map.put("mistyped", androidRegisterEnv.getMistyped());
        map.put("language_selector_time_spent", 0);
        map.put("client_metrics", WhatsAppUtils.urlEncode("{\"attempts\":1}"));
        // map.put("feo2_query_status", "error_security_exception");
        String urlParams = WhatsAppUtils.getUrlParamsByMap(map).replace("+", "-").replace("/", "_");
        log.info("用户：{}，校验注册参数：{}", username, urlParams);
        String enc = generateEnc(urlParams);
        String url = "https://v.whatsapp.net/v2/exist?ENC=" + enc;
        return sendRequest(url);
    }

    public HttpClientUtil.ResponseResult regOnboardAbProp() {
        String url = "https://v.whatsapp.net/v2/reg_onboard_abprop";
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        baseParameter(map);
        map.remove("read_phone_permission_granted");
        map.remove("device_ram");
        map.remove("token");
        map.remove("hasinrc");
        map.remove("network_radio_type");
        map.remove("network_operator_name");
        map.remove("sim_operator_name");
        map.remove("fdid");
        map.remove("expid");
        map.remove("id");
        map.remove("backup_token");
        map.remove("pid");
        String urlParams = WhatsAppUtils.getUrlParamsByMap(map).replace("+", "-").replace("/", "_");
        log.info("用户：{}，校验注册regOnboardAbProp参数：{}", username, urlParams);
        String enc = generateEnc(urlParams);
        String body = "ENC=" + enc;
        return sendRequest(url, body);
    }

    @SneakyThrows
    public HttpClientUtil.ResponseResult sendSmsRequest() {
        String method = registerPack.getMethod();
        if (StringUtils.hasLength(method)) {
            return sendSmsRequest(method);
        }
        if (registerPack.isVoiceCallSms()) {
            return sendSmsRequest("voice");
        }
        return sendSmsRequest("sms");
    }

    @SneakyThrows
    public HttpClientUtil.ResponseResult sendSmsRequest(String method) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        baseParameter(map);
        map.put("reason", "");
        map.put("method", method);
        applySimCardInfo(map);
        map.put("hasav", "2");
        map.put("prefer_sms_over_flash", "false");
        map.put("education_screen_displayed", "false");
        map.put("mistyped", androidRegisterEnv.getMistyped());
        map.put("client_metrics", WhatsAppUtils.urlEncode("{\"attempts\":1}"));
        map.put("feo2_query_status", "error_security_exception");
        String urlParams = WhatsAppUtils.getUrlParamsByMap(map).replace("+", "-").replace("/", "_");
        log.info("用户：{}，发送验证码参数：{}", username, urlParams);
        String enc = generateEnc(urlParams);
        String url = "https://v.whatsapp.net/v2/code?ENC=" + enc;
        return sendRequest(url);

    }

    @SneakyThrows
    public HttpClientUtil.ResponseResult getImageCaptcha() {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        baseParameter(map);
        map.put("reason", "");
        map.put("method", "captcha");
        applySimCardInfo(map);
        map.put("prefer_sms_over_flash", "false");
        map.put("mistyped", androidRegisterEnv.getMistyped());
        map.put("client_metrics", WhatsAppUtils.urlEncode("{\"attempts\":1}"));
        String urlParams = WhatsAppUtils.getUrlParamsByMap(map).replace("+", "-").replace("/", "_");
        log.info("用户：{}，获取图片验证码参数：{}", username, urlParams);
        String enc = generateEnc(urlParams);
        String url = "https://v.whatsapp.net/v2/code?ENC=" + enc;
        return sendRequest(url);
    }

    public HttpClientUtil.ResponseResult sendCaptchaVerifyRequest(String code) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        baseParameter(map);
        map.remove("sim_operator_name");
        map.remove("read_phone_permission_granted");
        map.remove("network_operator_name");
        map.remove("device_ram");
        map.remove("token");
        if (registerPack.isNoSimCard()) {
            map.put("simnum", "0");
        } else {
            map.put("simnum", "1");
        }
        map.put("audio_button_tap_count", "0");
        map.put("time_until_first_key_tap", "23930");
        map.put("time_until_code_submit", "26799");
        map.put("refresh_button_tap_count", "0");
        map.put("fraud_checkpoint_code", code);
        String urlParams = WhatsAppUtils.getUrlParamsByMap(map).replace("+", "-").replace("/", "_");
        log.info("用户：{}，验证图片验证码参数：{}", username, urlParams);
        String enc = generateEnc(urlParams);
        log.info("用户：{}，验证图片验证码参数enc参数：{}", username, enc);
        String url = "https://v.whatsapp.net/v2/captcha_verify";
        return sendRequest(url, "ENC=" + enc);
    }

    public String ocrCaptcha(String imageBlob) {
        RegisterInfoConstant registerInfoConstant = SpringUtils.getBean(RegisterInfoConstant.class);
        if (registerInfoConstant == null) {
            return null;
        }
        String url = registerInfoConstant.getOcrUrl();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("image", imageBlob);
        String content = HttpUtils.postToJson(url, jsonObject.toJSONString());
        try {
            jsonObject = JSONObject.parseObject(content);
            if (jsonObject.getIntValue("code") == 200) {
                return jsonObject.getJSONObject("data").getString("result");
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    @SneakyThrows
    public HttpClientUtil.ResponseResult sendRegisterRequest(String code) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        baseParameter(map);
        map.put("code", code);
        applySimCardInfo(map);
        map.put("entered", "1");
        map.put("mistyped", androidRegisterEnv.getMistyped());
        map.put("client_metrics", WhatsAppUtils.urlEncode("{\"attempts\":1}"));
        // map.put("feo2_query_status", "error_security_exception");
        String urlParams = WhatsAppUtils.getUrlParamsByMap(map).replace("+", "-").replace("/", "_");
        log.info("用户：{}，发送验证码参数：{}", username, urlParams);
        String enc = generateEnc(urlParams);
        String url = "https://v.whatsapp.net/v2/register?ENC=" + enc;
        return sendRequest(url);
    }

    @SneakyThrows
    private void baseParameter(Map<String, Object> map) {
        String regId = Base64.getUrlEncoder().withoutPadding().encodeToString(ByteString.copyFrom(AdjustId(androidRegisterEnv.getRegistrationId(), 4)).toByteArray());
        IdentityKeyPair identityKeyPair = new IdentityKeyPair(androidRegisterEnv.getIdentityKeyPair());
        String ident = Base64.getUrlEncoder().withoutPadding().encodeToString(ByteString.copyFrom(identityKeyPair.getPublicKey().serialize(), 1, 32).toByteArray());
        String authKeyPub = Base64.getUrlEncoder().withoutPadding().encodeToString(androidRegisterEnv.getPublicKey());
        SignedPreKeyRecord signedPreKeyRecord = new SignedPreKeyRecord(androidRegisterEnv.getSignedPreKey());
        String sKeyId = Base64.getUrlEncoder().withoutPadding().encodeToString(ByteString.copyFrom(AdjustId(signedPreKeyRecord.getId(), 3)).toByteArray());
        String sKeyVal = Base64.getUrlEncoder().withoutPadding().encodeToString(ByteString.copyFrom(signedPreKeyRecord.getKeyPair().getPublicKey().serialize(), 1, 32).toByteArray());
        String sKeySig = Base64.getUrlEncoder().withoutPadding().encodeToString(signedPreKeyRecord.getSignature());
        map.put("cc", registerPack.getPhoneAreaCode());
        map.put("in", registerPack.getPhone());
//        map.put("read_phone_permission_granted", 0);
        map.put("device_ram", androidRegisterEnv.getDeviceRam());
        map.put("token", androidRegisterEnv.getToken());
        map.put("hasinrc", 1);
        map.put("network_radio_type", 1);
//        map.put("network_operator_name", "");
//        map.put("sim_operator_name", "");
        map.put("airplane_mode_type", 0);
        if (registerPack.isNoSimCard()) {
            map.put("roaming_type", 0);
            map.put("sim_type", 0);
        } else {
            map.put("sim_type", 1);
            map.put("roaming_type", 1);
            map.put("cellular_strength", "5");
        }
        map.put("authkey", authKeyPub);
        map.put("e_regid", regId);
        map.put("e_ident", ident);
        map.put("e_skey_id", sKeyId);
        map.put("e_skey_val", sKeyVal);
        map.put("e_skey_sig", sKeySig);
        map.put("rc", 0);
        map.put("lg", androidRegisterEnv.getIso639());
        map.put("lc", androidRegisterEnv.getIso3166());
        map.put("e_keytype", "BQ");
        map.put("fdid", androidRegisterEnv.getFdId());
        String exPid = Base64.getUrlEncoder().withoutPadding().encodeToString(androidRegisterEnv.getExpId());
        map.put("expid", WhatsAppUtils.urlEncode(exPid));
        map.put("id", androidRegisterEnv.getId());
        map.put("backup_token", androidRegisterEnv.getBackupToken());
        map.put("pid", androidRegisterEnv.getPid());
    }

    @SneakyThrows
    private AndroidRegisterEnv generateEnv() {
        AndroidRegisterEnv androidRegisterEnv = new AndroidRegisterEnv();
        IdentityKeyPair identityKeyPair = KeyHelper.generateIdentityKeyPair();
        androidRegisterEnv.setIdentityKeyPair(identityKeyPair.serialize());
        androidRegisterEnv.setRegistrationId(KeyHelper.generateRegistrationId(true));
        androidRegisterEnv.setSignedPreKey(KeyHelper.generateSignedPreKey(identityKeyPair, 0).serialize());
        ECKeyPair ecKeyPair = Curve.generateKeyPair();
        byte[] pubSerialize = ByteString.copyFrom(ecKeyPair.getPublicKey().serialize(), 1, 32).toByteArray();
        byte[] priSerialize = ecKeyPair.getPrivateKey().serialize();
        androidRegisterEnv.setPublicKey(pubSerialize);
        androidRegisterEnv.setPrivateKey(priSerialize);
        androidRegisterEnv.setFullPhone(username);
        androidRegisterEnv.setDeviceRam(DEVICE_RAM_LIST[RandomUtil.randomInt(0, DEVICE_RAM_LIST.length)]);
        androidRegisterEnv.setPid(RandomUtil.randomInt(15042, 35042));
        String countryCode = DeviceUtil.getCountryCode(registerPack.getPhoneAreaCode());
        if (!StringUtils.hasLength(countryCode)) {
            return null;
        }
        com.alibaba.fastjson.JSONObject jsonObject = DeviceUtil.getCountryInfo(countryCode);
        String mcc = jsonObject.getString("mcc");
        String mnc = jsonObject.getString("mnc");
        String iso639 = jsonObject.getString("iso639");
        String iso3166 = jsonObject.getString("iso3166");
        androidRegisterEnv.setIso639(iso639);
        androidRegisterEnv.setIso3166(iso3166);
        androidRegisterEnv.setMcc(mcc);
        androidRegisterEnv.setMnc(mnc);
        String fdId = IdUtil.randomUUID();
        androidRegisterEnv.setFdId(fdId);
        UUID fromString = UUID.fromString(fdId);
        ByteBuffer allocate = ByteBuffer.allocate(16);
        allocate.putLong(fromString.getMostSignificantBits());
        allocate.putLong(fromString.getLeastSignificantBits());
        androidRegisterEnv.setExpId(allocate.array());
        String content = "{\"exposure\":[],\"metrics\":{}}";
        String offlineAb = WhatsAppUtils.urlEncode(content);
        androidRegisterEnv.setOfflineAb(offlineAb);
        String id = WhatsAppUtils.urlEncode(generateBackupTokenOrId());
        androidRegisterEnv.setId(id);
        String backupToken = WhatsAppUtils.urlEncode(generateBackupTokenOrId());
        androidRegisterEnv.setBackupToken(backupToken);
        RegisterInfoConstant registerInfoConstant = SpringUtils.getBean(RegisterInfoConstant.class);
        RegisterInfoConstant.RegisterInfo registerInfo = registerPack.getRegisterInfo();
        if (registerInfo == null) {
            if (registerPack.isBusinessVersion()) {
                if (registerPack.isIos()) {
                    registerInfo = registerInfoConstant.getBusinessIosVersionRegisterInfo();
                } else {
                    registerInfo = registerInfoConstant.getBusinessVersionRegisterInfo();
                }
            } else {
                if (registerPack.isIos()) {
                    registerInfo = registerInfoConstant.getNormalIosVersionRegisterInfo();
                } else {
                    registerInfo = registerInfoConstant.getNormalVersionRegisterInfo();
                }
            }
        }
        if (registerPack.isIos()) {
            androidRegisterEnv.setToken(new GenerateRegisterToken(registerInfo.getClassesMd5Base64(), registerInfo.getKey(), registerPack.getPhone()).getIosToken());
            androidRegisterEnv.setDeviceInfo(DeviceUtil.randomGetOneIosDevice());
        } else {
            androidRegisterEnv.setToken(WhatsAppUtils.urlEncode(new GenerateRegisterToken(registerInfo.getClassesMd5Base64(), registerInfo.getKey(), registerPack.getPhone()).getToken()));
            androidRegisterEnv.setDeviceInfo(DeviceUtil.randomGetOneAndroidDevice());
        }
        androidRegisterEnv.setVersion(registerInfo.getVersion());
        androidRegisterEnv.setMistyped(7);
        return androidRegisterEnv;
    }

    private HttpClientUtil.ResponseResult sendRequest(String url) {
        return HttpClientUtil.builder().proxyInfo(registerPack.getProxyInfo()).headers(getDefaultHttpHeaders()).url(url).connectTimeoutMillis(6000).responseTimeout(Duration.ofSeconds(40)).build().get();
    }

    private HttpClientUtil.ResponseResult sendRequest(String url, String body) {
        return HttpClientUtil.builder().proxyInfo(registerPack.getProxyInfo()).headers(getDefaultHttpHeaders()).url(url).data(body.getBytes()).connectTimeoutMillis(6000).responseTimeout(Duration.ofSeconds(40)).build().post();
    }

    private DefaultHttpHeaders getDefaultHttpHeaders() {
        DeviceInfo deviceInfo = androidRegisterEnv.getDeviceInfo();
        String device = deviceInfo.getDevice();
        String osVersion = deviceInfo.getVersion();
        DefaultHttpHeaders entries;
        if (registerPack.isIos()) {
            entries = WhatsAppUtils.applyIosRegisterHeaders(androidRegisterEnv.getVersion(), osVersion, StrUtil.replace(device, " ", "_"), registerPack.isBusinessVersion());
        } else {
            entries = WhatsAppUtils.applyAndroidRegisterHeaders(androidRegisterEnv.getVersion(), osVersion, StrUtil.replace(device, " ", "_"), registerPack.isBusinessVersion());
        }
        return entries;
    }

    private void applySimCardInfo(Map<String, Object> map) {
        if (registerPack.isNoSimCard()) {
            map.put("sim_mcc", "000");
            map.put("mcc", "000");
            map.put("sim_mnc", "000");
            map.put("mnc", "000");
            map.put("simnum", "0");

        } else {
            map.put("sim_mcc", androidRegisterEnv.getMcc());
            map.put("mcc", androidRegisterEnv.getMcc());
            map.put("sim_mnc", androidRegisterEnv.getMnc());
            map.put("mnc", androidRegisterEnv.getMnc());
            map.put("simnum", "1");
        }
    }

    public static byte[] generateBackupTokenOrId() {
        try {
            KeyGenerator keyGenerator0 = KeyGenerator.getInstance("AES");
            keyGenerator0.init(0xA0, new SecureRandom());
            return keyGenerator0.generateKey().getEncoded();
        } catch (Exception exception) {
            return null;
        }

    }

    /**
     * 生成enc加密
     *
     * @param content 内容
     * @return 加密内容
     */
    public static String generateEnc(String content) {
        ECKeyPair ecKeyPair = Curve.generateKeyPair();
        byte[] key = Curve25519.getInstance(BEST)
                .calculateAgreement(REGISTRATION_PUBLIC_KEY, ((DjbECPrivateKey) ecKeyPair.getPrivateKey()).getPrivateKey());
        byte[] encrypt = AesGcm.encrypt(new byte[12], content.getBytes(StandardCharsets.UTF_8), key);
        byte[] publicKey = ((DjbECPublicKey) ecKeyPair.getPublicKey()).getPublicKey();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(ByteUtil.combine(publicKey, encrypt));
    }


}
