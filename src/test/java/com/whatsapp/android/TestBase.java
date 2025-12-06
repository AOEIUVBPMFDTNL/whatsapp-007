package com.whatsapp.android;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.HexUtil;

/**
 * @author sunnoc
 * @date 2022-03-10 20:11
 */
public class TestBase {
    public static void main(String[] args) {
        String content = "QTAxRTlBNjgxN0YxOUJFRTg5OTBEMEY2NTYzNkJBRTM0MTdEQjFBNTMxNUM2MTUzODVFOTI5QjNGQkQ3M0M1Nw";
        byte[] bytes = HexUtil.decodeHex(Base64.decodeStr(content));
        String encode = Base64.encode(bytes);
        System.out.println(encode);
    }
}

