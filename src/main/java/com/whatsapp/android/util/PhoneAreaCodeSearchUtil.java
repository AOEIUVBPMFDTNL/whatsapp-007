package com.whatsapp.android.util;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.CharsetUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * 手机号区号搜索
 *
 * @author sunnoc
 * @date 2021-11-04 17:48
 */
public class PhoneAreaCodeSearchUtil {
    private static JSONObject PHONE_AREA_SEARCH = null;

    public static boolean loadPhoneAreaCodeSearchFile() {
        InputStream in;
        try {
            ClassPathResource cpr = new ClassPathResource("PhoneAreaCodeSearch.txt");
            in = cpr.getInputStream();
        } catch (IOException e) {
            return false;
        }
        String content = IoUtil.read(in, CharsetUtil.CHARSET_UTF_8);
        PHONE_AREA_SEARCH = JSONObject.parseObject(content);
        return true;
    }

    public static String getPhoneAreaCode(String phone) {
        if (PHONE_AREA_SEARCH == null || StringUtils.isEmpty(phone) || phone.length() < 8) {
            return null;
        }
        String phoneLength = String.valueOf(phone.length());
        JSONObject areaCodeMap = PHONE_AREA_SEARCH.getJSONObject(phoneLength);
        if (areaCodeMap == null) {
            return null;
        }
        JSONArray areaCodeLengthList = areaCodeMap.getJSONArray("areaCodeLengthList");
        for (int i = 0; i < areaCodeLengthList.size(); i++) {
            String length = areaCodeLengthList.getString(i);
            String areaCode = phone.substring(0, Integer.parseInt(length));
            JSONObject areaCodeLengthMap = areaCodeMap.getJSONObject("areaCodeLengthMap");
            if (areaCodeLengthMap.containsKey(areaCode)) {
                return areaCode;
            }
        }
        return null;
    }
}
