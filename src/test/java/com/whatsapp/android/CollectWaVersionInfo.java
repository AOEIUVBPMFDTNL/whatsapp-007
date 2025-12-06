package com.whatsapp.android;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.PatternPool;
import cn.hutool.core.util.StrUtil;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author sunnoc
 * @date 2022-07-18 17:26
 */
public class CollectWaVersionInfo {
    public static void main(String[] args) {
        generateNormalVersionInfo();
    }

    public static void generateBusinessVersionInfo() {
        String content = FileUtil.readString("/Users/sunnoc/IdeaProjects/whatsapp-android/src/test/java/com/whatsapp/android/collectWaVersionInfo.txt", StandardCharsets.UTF_8);
        //<h5 title="WhatsApp Business (.*?)"
        Pattern pattern = PatternPool.get("<h5 title=\"WhatsApp Business (.*?)\"");
        Matcher matcher = pattern.matcher(content);
        StringBuilder stringBuilder = new StringBuilder();
        while (matcher.find()) {
            String matcherContent = matcher.group(1);
            if (StringUtils.hasLength(matcherContent)) {
                String releaseVersion;
                if (StrUtil.contains(matcherContent, " beta")) {
                    releaseVersion = "1";
                } else {
                    releaseVersion = "0";
                }
                String version = matcherContent.replace("beta", "").replace(" ", "");
                stringBuilder.append("\t").append(version).append("\t").append(releaseVersion).append("\t").append("1").append("\n");
            }
        }
        System.out.println(stringBuilder.toString());
    }

    public static void generateNormalVersionInfo() {
        String content = FileUtil.readString("/Users/sunnoc/IdeaProjects/whatsapp-android/src/test/java/com/whatsapp/android/collectWaVersionInfo.txt", StandardCharsets.UTF_8);
        //<h5 title="WhatsApp Messenger (.*?)"
        Pattern pattern = PatternPool.get("title=\"WhatsApp Messenger(.*?)\"");
        Matcher matcher = pattern.matcher(content);
        StringBuilder stringBuilder = new StringBuilder();
        while (matcher.find()) {
            String matcherContent = matcher.group(1);
            if (StringUtils.hasLength(matcherContent)) {
                String releaseVersion;
                if (StrUtil.contains(matcherContent, " beta")) {
                    releaseVersion = "1";
                } else {
                    releaseVersion = "0";
                }
                String version = matcherContent.replace("beta", "").replace(" ", "");
                stringBuilder.append("\t").append(version).append("\t").append(releaseVersion).append("\t").append("0").append("\n");
            }
        }
        System.out.println(stringBuilder.toString());
    }
}
