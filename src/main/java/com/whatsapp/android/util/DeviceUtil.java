package com.whatsapp.android.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.entity.DeviceInfo;
import com.whatsapp.android.entity.MccMnc;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 获取一条注册环境
 *
 * @author sunnoc
 * @date 2021-07-22 15:47
 */
public class DeviceUtil {
    private static final String[] ANDROID_VERSION = {"9", "10", "11"};
    private static String[] DEVICES = null;
    private static JSONArray ANDROID_DEVICES = null;
    private static JSONObject IOS_DEVICES = null;
    private static Map<String, String> CC_TO_COUNTRY = null;
    private static JSONObject COUNTRY_INFO = null;
    private static JSONObject MCC_MNC_INFO = null;

    public static boolean loadDeviceFile() {
        InputStream in = null;
        try {
            ClassPathResource cpr = new ClassPathResource("envJson.txt");
            in = cpr.getInputStream();
        } catch (IOException e) {
            return false;
        }
        String content = IoUtil.read(in, CharsetUtil.CHARSET_UTF_8);
        DEVICES = StrUtil.split(content, System.lineSeparator());
        return true;
    }

    public static boolean loadAndroidDeviceFile() {
        InputStream in = null;
        try {
            ClassPathResource cpr = new ClassPathResource("androidDeviceInfo.txt");
            in = cpr.getInputStream();
        } catch (IOException e) {
            return false;
        }
        String content = IoUtil.read(in, CharsetUtil.CHARSET_UTF_8);
        ANDROID_DEVICES = JSONArray.parseArray(content);
        return true;
    }

    public static boolean loadCountryCodeFile() {
        InputStream in = null;
        try {
            ClassPathResource cpr = new ClassPathResource("waPhoneToCountry.txt");
            in = cpr.getInputStream();
        } catch (IOException e) {
            return false;
        }
        String content = IoUtil.read(in, CharsetUtil.CHARSET_UTF_8);
        String[] split = StrUtil.split(content, "\n");
        Map<String, String> map = new HashMap<>();
        for (String s : split) {
            String[] array = StrUtil.split(s, "----");
            map.put(array[0], array[1]);
        }
        CC_TO_COUNTRY = map;
        return true;
    }

    public static boolean loadIosDeviceFile() {
        InputStream in;
        try {
            ClassPathResource cpr = new ClassPathResource("iphoneInfo.txt");
            in = cpr.getInputStream();
        } catch (IOException e) {
            return false;
        }
        String content = IoUtil.read(in, CharsetUtil.CHARSET_UTF_8);
        IOS_DEVICES = JSONObject.parseObject(content);
        return true;
    }

    public static boolean loadWaCountryInfoFile() {
        InputStream in;
        try {
            ClassPathResource cpr = new ClassPathResource("waCountryInfo.txt");
            in = cpr.getInputStream();
        } catch (IOException e) {
            return false;
        }
        String content = IoUtil.read(in, CharsetUtil.CHARSET_UTF_8);
        COUNTRY_INFO = JSONObject.parseObject(content);
        return true;
    }

    public static byte[] loadHistoryReactorCoreJar() {
        InputStream in;
        try {
            ClassPathResource cpr = new ClassPathResource("/jar/reactor-core-3.3.6.RELEASE.jar");
            in = cpr.getInputStream();
        } catch (IOException e) {
            return null;
        }
        return IoUtil.readBytes(in);
    }

    public static boolean loadMccMncInfoFile() {
        InputStream in;
        try {
            ClassPathResource cpr = new ClassPathResource("mcc-mnc.json");
            in = cpr.getInputStream();
        } catch (IOException e) {
            return false;
        }
        String content = IoUtil.read(in, CharsetUtil.CHARSET_UTF_8);
        MCC_MNC_INFO = JSONObject.parseObject(content);
        return true;
    }

    public static MccMnc getMccMnc(String country) {
        JSONObject jsonObject = MCC_MNC_INFO.getJSONObject(country.toUpperCase());
        JSONArray jsonArray = jsonObject.getJSONArray("mnc_mcc");
        int size = jsonArray.size();
        JSONObject mccMncObject;
        if (size == 1) {
            mccMncObject = jsonArray.getJSONObject(0);
        } else {
            mccMncObject = jsonArray.getJSONObject(RandomUtil.randomInt(0, size));
        }
        String mcc = mccMncObject.getString("mcc");
        String mnc = mccMncObject.getString("mnc");
        if (mnc.length() < 3) {
            StringBuilder sb = new StringBuilder(mnc);
            while (sb.length() < 3) {
                sb.insert(0, '0');
            }
            mnc = sb.toString();
        }
        return new MccMnc(mcc, mnc);
    }

    public static DeviceInfo randomGetOneIosDevice() {
        if (IOS_DEVICES == null) {
            return null;
        }
        String manufacturer = IOS_DEVICES.getString("manufacturer");
        JSONArray devicesJsonArray = IOS_DEVICES.getJSONArray("device");
        String device = devicesJsonArray.getString(RandomUtil.randomInt(0, devicesJsonArray.size()));
        JSONArray infoJsonArray = IOS_DEVICES.getJSONArray("info");
        JSONObject jsonObject = infoJsonArray.getJSONObject(RandomUtil.randomInt(0, infoJsonArray.size()));
        String version = jsonObject.getString("version");
        String build = jsonObject.getString("build");
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setManufacturer(manufacturer);
        deviceInfo.setDevice(device);
        deviceInfo.setVersion(version);
        deviceInfo.setBuild(build);
        return deviceInfo;
    }

    public static DeviceInfo randomGetOneAndroidDevice() {
        if (IOS_DEVICES == null) {
            return null;
        }
        int size = ANDROID_DEVICES.size();
        int index = RandomUtil.randomInt(0, size);
        String content = ANDROID_DEVICES.getString(index);
        String[] split = StrUtil.split(content, ";");
        if (split.length == 3) {
            String manufacturer = split[0];
            String device = split[1];
            String version = ANDROID_VERSION[RandomUtil.randomInt(0, ANDROID_VERSION.length)];
            String build = RandomUtil.randomString(RandomUtil.randomInt(10, 13));
            return new DeviceInfo(manufacturer, device, version, build);
        }
        return null;
    }

    public static boolean loadDeviceFile(String path) {
        String content = FileUtil.readString(path, StandardCharsets.UTF_8);
        DEVICES = StrUtil.split(content, System.lineSeparator());
        return true;
    }

    public static String randomGetOneDevice() {
        if (DEVICES == null) {
            return null;
        }
        int randomInt = RandomUtil.randomInt(0, DEVICES.length);
        return DEVICES[randomInt];
    }

    public static String getCountryCode(String cc) {
        return CC_TO_COUNTRY.get(cc);
    }

    public static JSONObject getCountryInfo(String country) {
        JSONObject jsonObject = COUNTRY_INFO.getJSONObject(country);
        if (jsonObject == null) {
            return COUNTRY_INFO.getJSONObject("US");
        }
        return jsonObject;
    }

    public static String getIOSDeviceModelType(String device) {
        JSONObject deviceModel = IOS_DEVICES.getJSONObject("deviceModel");
        return deviceModel.getString(device);
    }
}
