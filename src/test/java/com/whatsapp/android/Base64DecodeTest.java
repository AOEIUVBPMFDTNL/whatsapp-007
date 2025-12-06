package com.whatsapp.android;

import cn.hutool.core.util.HexUtil;

/**
 * @author sunnoc
 * @date 2023-05-10 19:53
 */
public class Base64DecodeTest {
    public static void main(String[] args) {
        //48f08250afb7c75a87088c82c0e7a908f58eb4075d3869b97efcdb46f9b20f68e8292de70e869b897d86b8ca6a8fc2ac927a6d42abdd0f55bd8b7f8d19ef5e83
        //519c955fa83e2225241a02e4d80ac38d1095e00894fe595236c662c4017ad8224918364de6da978905b71530e00ad1fd603f17eb5362707f15d4b8a8bea3088c
        String content1 = "UZyVX6g+IiUkGgLk2ArDjRCV4AiU/llSNsZixAF62CJJGDZN5tqXiQW3FTDgCtH9YD8X61NicH8V1LiovqMIjA==";
        System.out.println(HexUtil.encodeHexStr(Base64.decode(content1)));
        String content2 = "cHNEpbrsabVwRIP/oFFV5+mqFrui94u63Iu8c2dAyzXYEsytJGWXk3u84GwdR7eHvexeBw5Er5SzOdl9rB7/iw==";
        System.out.println(HexUtil.encodeHexStr(Base64.decode(content2)));
    }
}
