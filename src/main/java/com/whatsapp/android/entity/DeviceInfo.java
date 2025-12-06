package com.whatsapp.android.entity;

import lombok.Data;

/**
 * @author sunnoc
 * @date 2021-10-08 11:46
 */
@Data
public class DeviceInfo {
    /**
     * 制造商
     */
    private String manufacturer;
    /**
     * 设备名称
     */
    private String device;
    /**
     * 系统版本号
     */
    private String version;
    /**
     * 内部版本号
     */
    private String build;


    public DeviceInfo() {
    }

    public DeviceInfo(String manufacturer, String device, String version, String build) {
        this.manufacturer = manufacturer;
        this.device = device;
        this.version = version;
        this.build = build;
    }
}
