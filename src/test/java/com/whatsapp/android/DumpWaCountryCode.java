package com.whatsapp.android;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.PatternPool;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author sunnoc
 * @date 2021-12-15 15:38
 */
public class DumpWaCountryCode {
    public static void main(String[] args) {
        String regex = "<node index=\"\\d+\" text=\"([\\u4e00-\\u9fa5]+)\" resource-id=\"com.whatsapp:id/country_first_name\"[\\s\\S]*?<node index=\"\\d+\" text=\"\\+*(\\d+)\" resource-id=\"com.whatsapp:id/country_code\"";
        String content = FileUtil.readString("/Users/sunnoc/Downloads/dump.xml", StandardCharsets.UTF_8);
        Pattern pattern = PatternPool.get(regex);
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            String countryCn = matcher.group(1);
            String phoneCode = matcher.group(2);
            System.out.println(countryCn + "----" + phoneCode);
        }
    }
}
