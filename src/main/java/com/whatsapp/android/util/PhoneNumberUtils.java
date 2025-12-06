package com.whatsapp.android.util;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

public class PhoneNumberUtils {
    private static final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();

    public static String parse(String phoneNumberStr) {
        try {
            Phonenumber.PhoneNumber phoneNumber = phoneNumberUtil.parse(handlerPhoneNumber(phoneNumberStr), null);
            return String.valueOf(phoneNumber.getCountryCode());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 校验号码是否正常
     */
    public static boolean isValidPhoneNumber(String phoneNumber, String defaultRegion) {
        try {
            Phonenumber.PhoneNumber number = phoneNumberUtil.parse(handlerPhoneNumber(phoneNumber), defaultRegion);
            String regionCodeForNumber = phoneNumberUtil.getRegionCodeForNumber(number);
            System.out.println(regionCodeForNumber);
            return phoneNumberUtil.isValidNumber(number);
        } catch (NumberParseException e) {
            // 如果解析失败，说明不是有效的手机号
            return false;
        }
    }

    /**
     * 获取国家代码
     */
    public static String getCountry(String phoneNumber, String defaultRegion) {
        try {
            Phonenumber.PhoneNumber number = phoneNumberUtil.parse(handlerPhoneNumber(phoneNumber), defaultRegion);
            return phoneNumberUtil.getRegionCodeForNumber(number);
        } catch (NumberParseException e) {
            return "";
        }
    }

    private static String handlerPhoneNumber(String phoneNumberStr) {
        char firstChar = phoneNumberStr.charAt(0);
        if (firstChar != '+') {
            // 如果不是加号，则在前面添加一个加号
            return "+" + phoneNumberStr;
        }
        return phoneNumberStr;
    }

    public static void main(String[] args) {
        String phone = "4917687383258";
        System.out.println(getCountry(phone, "US"));
    }

}
