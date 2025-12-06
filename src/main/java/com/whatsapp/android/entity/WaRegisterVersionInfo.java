package com.whatsapp.android.entity;

import lombok.Data;

/**
 * @author sunnoc
 * @date 2022-08-19 15:55
 */
@Data
public class WaRegisterVersionInfo {
    /**
     * classes.dex文件的md5后base64值
     */
    private String classesMd5Base64;
    /**
     * wa版本号
     */
    private String version;
}
