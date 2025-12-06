package com.whatsapp.android;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.HexUtil;

/**
 * @author sunnoc
 * @date 2022-12-16 17:03
 */
public class HexToBin {
    public static void main(String[] args) {
        String content = "0ad2020803122105a9bcdab43761bb71c6c879c473dc1aeeb202950644b38a2ce4642dbba953bc771a210585915d346ef856a003f9ac579e888a3623e9c24a465b1fa4460f1f16fd817f7e222022c8b268e861c9be141ad072bdf2c0940528dd7cf2e16a0867aa26adc5c123662800326b0a2105976b85b269241c37482a4fec30d7870d426c1fb19646e6bcd0050553d06a45071220f0897e3d5681b80b41253273a05e17ff27412a137af1d5fbdc91879637ef74691a240800122011fbfd62ed699eefdd0a380c31859e250cfbcae00f250fbafb364a2dc57848023a490a21053a7181738efa830474be48138c8bdb0a8822eac28a72ba30295d42273ce6b43d1a2408011220688ba18f71fc694b197d95156ab413ff9b0e32c53323654b5e9fe124e1049d4050f4a2f66b58d3e7ee84016a2105b0eb6498b6d6cf1bbdde8d924ab8393fc1d67f321c1097db4d8fc0bc6fcc0c4b1200";
        byte[] bytes = HexUtil.decodeHex(content);
        String encode = Base64.encode(bytes);
        System.out.println(encode);
        System.out.println(new String(bytes));
        FileUtil.writeBytes(bytes, "/Users/sunnoc/IdeaProjects/whatsapp-android/src/test/java/com/whatsapp/android/1.bin");
    }
}
