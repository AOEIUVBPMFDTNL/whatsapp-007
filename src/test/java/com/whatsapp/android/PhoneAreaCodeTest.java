package com.whatsapp.android;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.entity.InternationalMobilePhoneNumbers;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author sunnoc
 * @date 2021-11-04 12:26
 */
public class PhoneAreaCodeTest {
    public static void main(String[] args) {
        String phoneAreaCode = getPhoneAreaCode("213671159192");
        System.out.println(phoneAreaCode);
        // generatePhoneMap();
    }

    public static void generatePhoneAreaCodeSearchTxt() {
        //长度分组，地区代码分组，完整信息
        HashMap<String, HashMap<String, InternationalMobilePhoneNumbers>> phoneLengthMap = new HashMap<>();
        String content = FileUtil.readString("/Users/sunnoc/IdeaProjects/whatsapp-android/src/main/resources/phone_area_code1.txt", StandardCharsets.UTF_8);
        String[] split = StrUtil.split(content, "\n");
        for (String s : split) {
            if (StringUtils.hasLength(s)) {
                String[] phoneArray = StrUtil.split(s, "----");
                String englishName = phoneArray[0];
                String chineseName = phoneArray[1];
                String areaCode = phoneArray[2];
                String length = phoneArray[3];
                List<String> areaCodeList = StrUtil.split(areaCode, '/');
                List<String> lengthList = StrUtil.split(length, '/');
                if (areaCodeList.size() > 1 && lengthList.size() > 1) {
                    System.out.println(1);
                    return;
                }
                for (String integer : lengthList) {
                    //地区代码对应的完整信息
                    HashMap<String, InternationalMobilePhoneNumbers> areaMap = phoneLengthMap.get(integer);
                    if (areaMap == null) {
                        areaMap = new HashMap<>();
                    }
                    for (String area : areaCodeList) {
                        InternationalMobilePhoneNumbers internationalMobilePhoneNumbers = new InternationalMobilePhoneNumbers();
                        internationalMobilePhoneNumbers.setChineseName(chineseName);
                        internationalMobilePhoneNumbers.setEnglishName(englishName);
                        internationalMobilePhoneNumbers.setAreaCodeList(areaCodeList);
                        internationalMobilePhoneNumbers.setLengthList(lengthList);
                        areaMap.put(area, internationalMobilePhoneNumbers);
                    }
                    phoneLengthMap.putIfAbsent(integer, areaMap);

                }
            }
        }
        System.out.println(JSONObject.toJSONString(phoneLengthMap));
    }

    public static void newLength() {
        String content = FileUtil.readString("/Users/sunnoc/IdeaProjects/whatsapp-android/src/main/resources/phone_area_code.txt", StandardCharsets.UTF_8);
        String[] split = StrUtil.split(content, "\n");
        StringBuilder stringBuilder = new StringBuilder();
        for (String s : split) {
            if (StringUtils.hasLength(s)) {
                String[] phoneArray = StrUtil.split(s, "----");
                String englishName = phoneArray[0];
                String chineseName = phoneArray[1];
                String areaCode = phoneArray[2];
                String length = phoneArray[3];
                List<String> areaCodeList = StrUtil.split(areaCode, '/');
                List<String> lengthList = StrUtil.split(length, '/');
                List<String> newLengthList = new ArrayList<>();
                HashSet<String> strings = new HashSet<>();
                for (String s1 : areaCodeList) {
                    int length1 = s1.length();
                    for (String s2 : lengthList) {
                        String s3 = String.valueOf(length1 + Integer.parseInt(s2));
                        if (!strings.contains(s3)) {
                            newLengthList.add(s3);
                            strings.add(s3);
                        }
                    }
                }
                String newLength = String.join("/", newLengthList);
                stringBuilder.append(englishName).append("----").append(chineseName).append("----").append(areaCode).append("----").append(newLength).append("\n");
            }
        }
        FileUtil.writeString(stringBuilder.toString(), "/Users/sunnoc/IdeaProjects/whatsapp-android/src/main/resources/phone_area_code1.txt", StandardCharsets.UTF_8);
    }

    public static void generatePhoneMap() {
        String content = FileUtil.readString("/Users/sunnoc/IdeaProjects/whatsapp-android/src/main/resources/PhoneAreaCodeSearchOld.txt", StandardCharsets.UTF_8);
        JSONObject jsonObject = JSONObject.parseObject(content);
        JSONObject phoneMap = new JSONObject();
        Set<String> lengthSet = jsonObject.keySet();
        for (String phoneLength : lengthSet) {
            JSONObject areaCodeObject = jsonObject.getJSONObject(phoneLength);
            //获取区号
            Set<String> areaCodeSet = areaCodeObject.keySet();
            JSONObject areaCodeMap;
            if (phoneMap.containsKey(phoneLength)) {
                areaCodeMap = phoneMap.getJSONObject(phoneLength);
            } else {
                areaCodeMap = new JSONObject();
                phoneMap.put(phoneLength, areaCodeMap);
            }
            //按区号长度分组
            Map<Integer, List<String>> collect = areaCodeSet.stream().collect(Collectors.groupingBy(String::length));
            HashMap<String, JSONObject> stringInternationalMobilePhoneNumbersHashMap = new HashMap<>();
            collect.forEach((integer, strings) -> {
                String areaCodeLengthStr = String.valueOf(integer);
                for (String areaCode : strings) {
                    stringInternationalMobilePhoneNumbersHashMap.put(areaCode, areaCodeObject.getJSONObject(areaCode));
                }
            });
            //添加区号长度列表
            List<String> areaCodeLengthList = collect.keySet().stream().sorted(Comparator.reverseOrder()).map(String::valueOf).collect(Collectors.toList());
            areaCodeMap.put("areaCodeLengthList", areaCodeLengthList);
            areaCodeMap.put("areaCodeLengthMap", stringInternationalMobilePhoneNumbersHashMap);
            // System.out.println(JSONObject.toJSONString(collect));
            System.out.println("长度：" + phoneLength + " 区号长度：" + areaCodeLengthList + " 区号：" + areaCodeSet);
        }
        System.out.println(JSONObject.toJSONString(phoneMap));

    }

    public static String getPhoneAreaCode(String phone) {
        if (StringUtils.isEmpty(phone) || phone.length() < 8) {
            return null;
        }
        String content = FileUtil.readString("/Users/sunnoc/IdeaProjects/whatsapp-android/src/main/resources/PhoneAreaCodeSearch.txt", StandardCharsets.UTF_8);
        JSONObject jsonObject = JSONObject.parseObject(content);
        String phoneLength = String.valueOf(phone.length());
        JSONObject areaCodeMap = jsonObject.getJSONObject(phoneLength);
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
