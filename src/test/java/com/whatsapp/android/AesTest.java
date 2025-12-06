package com.whatsapp.android;


import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.util.DevUtil;


/**
 * @author sunnoc
 * @date 2020-07-31 16:06
 */
public class AesTest {
    public static void main(String[] args) {
        encrypted();
    }

    public static void encrypted() {
        String uuid = IdUtil.simpleUUID();
        System.out.println(uuid);
        String url = "http://terminal-update.oss-ap-southeast-1.aliyuncs.com/xxxxx/3.5.1-" + uuid;
        AES aes = SecureUtil.aes(Constant.UPDATE_TERMINAL_URL_KEY.getBytes());
        String encryptBase64 = aes.encryptBase64(url);
        System.out.println("加密后：" + encryptBase64);
        System.out.println("解密后：" + aes.decryptStr(encryptBase64));
    }


    public static void machine() {
        System.out.println(DevUtil.getMachineCode());
        System.out.println(DevUtil.getMachineCode());
        System.out.println(DevUtil.getMachineCode());
    }

}
