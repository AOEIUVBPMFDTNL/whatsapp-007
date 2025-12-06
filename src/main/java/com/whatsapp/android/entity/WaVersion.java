package com.whatsapp.android.entity;

import lombok.Data;

/**
 * @author sunnoc
 * @date 2022-03-28 19:44
 */
@Data
public class WaVersion {
    private String version;
    private Integer releaseVersion;

    public WaVersion() {
    }

    public WaVersion(String version, Integer releaseVersion) {
        this.version = version;
        this.releaseVersion = releaseVersion;
    }
}
