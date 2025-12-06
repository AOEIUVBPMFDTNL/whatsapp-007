package com.whatsapp.android.entity.response.terminal;

import lombok.Data;

/**
 * @author sunnoc
 * @date 2020-07-31 15:46
 */
@Data
public class UpdateTerminalPack {
    /**
     * 更新的版本
     */
    private String version;
    /**
     * 密匙
     */
    private String key;
    /**
     * ws地址
     */
    private String ws;
}
