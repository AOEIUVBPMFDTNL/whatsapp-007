package com.whatsapp.android;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.io.FileUtil;
import cn.hutool.crypto.digest.DigestUtil;

/**
 * @author sunnoc
 * @date 2022-08-19 10:28
 */
public class ClassesMd5 {
    public static void main(String[] args) {
        String path = "/Users/sunnoc/work/study/whatsapp安卓协议/whatsapp hook版本/com.whatsapp_2.22.17.71/classes.dex";
        byte[] bytes = FileUtil.readBytes(path);
        String encode = Base64.encode(DigestUtil.md5(bytes));
        System.out.println(encode);
    }
}
