package jni;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.entity.GcmTokenResult;
import com.whatsapp.android.entity.ProxyInfo;
import com.whatsapp.android.util.HttpClientUtil;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaderNames;
import lombok.extern.slf4j.Slf4j;
import org.microg.gms.checkin.CheckinProto;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;

import static com.whatsapp.android.util.WhatsAppUtils.getUrlParamsByMap;

/**
 * @author Rocky
 */
@Slf4j
public class GCMService {

    private static final String checkInUrl = "https://android.googleapis.com/checkin";

    private static final String gcmRegister = "https://android.apis.google.com/c2dm/register3";

    private static final String SENDER_W4B = "293955441834";

    private static final String SENDER = "293955441834";

    private static final String appIdW4b = "duK7K-eX0IY";

    private static final String appId = "ebwTzqDzCxk";

    private static final String gcmVer = "242113022";


    private final ProxyInfo proxyInfo;

    private final boolean isBusiness;

    private GcmTokenResult gcmTokenResult;

    private final String username;
    private final String appVerW4b;

    private final String appVer;

    private final String appVerNameW4b;

    private final String appVerName;

    public GCMService(ProxyInfo proxyInfo, boolean isBusiness, String username, String appVer, String appVerName) {
        this.proxyInfo = proxyInfo;
        this.isBusiness = isBusiness;
        this.username = username;
        this.appVer = appVer;
        this.appVerW4b = appVer;
        this.appVerName = appVerName;
        this.appVerNameW4b = appVerName;
    }

    public boolean checkIn() {
        CheckinProto.CheckinRequest.Builder checkInRequest = CheckinProto.CheckinRequest.newBuilder();
        CheckinProto.CheckinRequest.Checkin.Builder checkIn = CheckinProto.CheckinRequest.Checkin.newBuilder();
        CheckinProto.CheckinRequest.Checkin.Build.Builder checkInBuild = CheckinProto.CheckinRequest.Checkin.Build.newBuilder();
        CheckinProto.CheckinRequest.DeviceConfig.Builder config = CheckinProto.CheckinRequest.DeviceConfig.newBuilder();
        // 以pixel, android10为例子
        checkInBuild.setFingerprint("google/sailfish/sailfish:10/QP1A.190711.020/5800535:user/release-keys");
        checkInBuild.setBrand("google");
        checkInBuild.setRadio("8996-130361-1905270421");
        checkInBuild.setBootloader("8996-012001-1907011432");
        checkInBuild.setClientId("android-verizon");
        checkInBuild.setTime(1565745950);
        checkInBuild.setDevice("sailfish");
        checkInBuild.setSdkVersion(29);
        checkInBuild.setOtaInstalled(false);
        checkInBuild.setModel("Pixel");
        checkInBuild.setManufacturer("Google");
        checkInBuild.setProduct("sailfish");
        checkInRequest.setAndroidId(0);
        checkInRequest.setLocale("en-US");
        checkInRequest.setTimeZone("America/New_York");
        checkInRequest.setVersion(3);
        checkInRequest.setFragment(0);
        checkInRequest.setUserSerialNumber(0);
        checkInRequest.setSerial("FA" + RandomUtil.randomNumbers(10));
        config.setTouchScreen(3);
        config.setKeyboardType(1);
        config.setNavigation(1);
        config.setScreenLayout(2);
        config.setHasHardKeyboard(false);
        config.setHasFiveWayNavigation(false);
        config.setDensityDpi(420);
        config.setGlEsVersion(196610);
        config.setWidthPixels(1080);
        config.setHeightPixels(1920);
        config.setDeviceClass(0);
        config.setMaxApkDownloadSizeMb(0);
        checkIn.setLastCheckinMs(0);
        checkIn.setBuild(checkInBuild);
        checkInRequest.setCheckin(checkIn);
        checkInRequest.setDeviceConfiguration(config);
        DefaultHttpHeaders entries = new DefaultHttpHeaders();
        entries.set(HttpHeaderNames.CONTENT_TYPE, "application/x-protobuffer");
        entries.set(HttpHeaderNames.ACCEPT_LANGUAGE, "gzip");
        entries.set(HttpHeaderNames.USER_AGENT, "Dalvik/2.1.0 (Linux; U; Android 10; Pixel Build/QP1A.190711.020)");
        HttpClientUtil.ResponseResult post = HttpClientUtil.builder().proxyInfo(proxyInfo).headers(entries).url(checkInUrl).data(checkInRequest.build().toByteArray()).connectTimeoutMillis(6000).responseTimeout(Duration.ofSeconds(40)).build().post();
        CheckinProto.CheckinResponse checkinResponse = null;
        try {
            checkinResponse = CheckinProto.CheckinResponse.parseFrom(post.getBytes());
        } catch (Exception ignore) {
        }
        if (!ObjectUtil.isNotNull(checkinResponse) || !checkinResponse.getStatsOk()) {
            log.error("用户: {}, 获取gcm失败!", username);
            return false;
        }
        GcmTokenResult gcmTokenResult = new GcmTokenResult();
        gcmTokenResult.setAndroidId(String.valueOf(checkinResponse.getAndroidId()));
        gcmTokenResult.setSecurityToken(String.valueOf(checkinResponse.getSecurityToken()));
        this.gcmTokenResult = gcmTokenResult;
        return ObjectUtil.isNotNull(this.gcmTokenResult) && (StrUtil.isNotEmpty(this.gcmTokenResult.getAndroidId()) || !StrUtil.isNotEmpty(this.gcmTokenResult.getSecurityToken()));
    }

    public boolean gcmRegister() {
        DefaultHttpHeaders entries = new DefaultHttpHeaders();
        entries.set(HttpHeaderNames.CONTENT_TYPE, "application/x-www-form-urlencoded");
        entries.set(HttpHeaderNames.ACCEPT_LANGUAGE, "gzip");
        entries.set(HttpHeaderNames.USER_AGENT, "com.google.android.gms/242013022 (Linux; U; Android 10; en_US; Pixel; Build/QP1A.190711.020; Cronet/125.0.6383.0)");
        entries.set("authorization", "AidLogin " + gcmTokenResult.getAndroidId() + ":" + gcmTokenResult.getSecurityToken());
        entries.set("app", "com.whatsapp" + (isBusiness ? ".w4b" : ""));
        entries.set("gcm_ver", gcmVer);
        entries.set("app_ver", isBusiness ? appVerW4b : appVer);
        String uuid = IdUtil.fastUUID();
        // 从 UUID 中获取16字节的数据
        String androidId = uuid.replaceAll("-", "");
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("X-subtype", isBusiness ? SENDER_W4B : SENDER);
        map.put("sender", isBusiness ? SENDER_W4B : SENDER);
        map.put("X-app_ver", isBusiness ? appVerW4b : appVer);
        map.put("X-osv", "29");
        map.put("X-cliv", "fiid-20.0.0");
        map.put("X-gmsv", gcmVer);
        map.put("X-appid", isBusiness ? appIdW4b : appId);
        map.put("X-scope", "*");
        map.put("X-gmp_app_id", "1:" + (isBusiness ? SENDER_W4B : SENDER) + ":android:" + androidId);
        map.put("X-Firebase-Client", "fire-core/19.0.0 fire-android/ fire-iid/20.0.0");
        map.put("X-app_ver_name", isBusiness ? appVerNameW4b : appVerName);
        map.put("app", "com.whatsapp" + (isBusiness ? ".w4b" : ""));
        map.put("device", gcmTokenResult.getAndroidId());
        map.put("app_ver", isBusiness ? appVerW4b : appVer);
        String urlParamsByMap = getUrlParamsByMap(map);
        HttpClientUtil.ResponseResult post = HttpClientUtil.builder().proxyInfo(proxyInfo).headers(entries).url(gcmRegister).data(urlParamsByMap.getBytes(StandardCharsets.UTF_8)).connectTimeoutMillis(6000).responseTimeout(Duration.ofSeconds(40)).build().post();
        String resultString = post.getResultString();
        if (!post.isSuccess()) {
            return false;
        }
        String token = StrUtil.subAfter(resultString, "token=", false);
        this.gcmTokenResult.setToken(token);
        return true;
    }

    public String getGcmToken() {
        try {
            if (!checkIn()) {
                log.error("用户: {}, 获取gcm失败!", username);
                return null;
            }
            if (!gcmRegister() || StrUtil.isEmpty(this.gcmTokenResult.getToken())) {
                log.error("用户: {}, 获取gcm Token失败!", username);
                return null;
            }
            JSONObject jsonObject = new JSONObject();
            JSONObject gcm = new JSONObject();
            gcm.put("androidId", this.gcmTokenResult.getAndroidId());
            gcm.put("securityToken", this.gcmTokenResult.getSecurityToken());
            gcm.put("token", this.gcmTokenResult.getToken());
            jsonObject.put("gcm", gcm);
            String jsonString = jsonObject.toJSONString();
            log.info("用户:{}, 获取gcm结果: {}", username, jsonString);
            return jsonString;
        } catch (Exception e) {
            log.info("用户:{}, 获取gcm异常", username, e);
            return null;
        }

    }

    public static void main(String[] args) {
        String username = "6285936156737";
        ProxyInfo proxyInfo = new ProxyInfo();
        proxyInfo.setType(0);
        proxyInfo.setProxyHost("127.0.0.1");
        proxyInfo.setProxyPort(1080);
        GCMService gcmService = new GCMService(proxyInfo, false, username, "232378003", "2.24.7.79");
        gcmService.getGcmToken();
    }

}
