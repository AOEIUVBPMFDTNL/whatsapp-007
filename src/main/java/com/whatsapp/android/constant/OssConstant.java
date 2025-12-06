package com.whatsapp.android.constant;
/**
 * @Author Irelia
 * @Date 2023/7/4 12:08
 */
public class OssConstant {
    /**
     * 节点信息
     */
    public static final String ENDPOINT = "oss-ap-southeast-1-internal.aliyuncs.com";
    /**
     * key
     */
    public static final String ACCESS_KEY_ID = "LTAI5tJ46gpW1FTCaEmpHCCy";
    /**
     * Secret
     */
    public static final String ACCESS_KEY_SECRET = "MIHaxheUQZP9Yxpkme0v37BYig4r0c";
    /**
     * Oss桶信息
     */
    public static final String ENV_BUCKET_NAME = "tk-env";
    /**
     * 获取文件的前缀地址
     */
    public static final String PRE_BUCKET_URL = "https://" + OssConstant.ENV_BUCKET_NAME + "."+ ENDPOINT + "/";
}