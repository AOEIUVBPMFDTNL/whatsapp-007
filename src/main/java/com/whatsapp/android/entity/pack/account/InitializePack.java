package com.whatsapp.android.entity.pack.account;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author sunnoc
 * @date 2020-07-29 18:14
 */

@NoArgsConstructor
@Data
public class InitializePack {
    private String uuid;
    private String ip;
    private String name;
    /**
     * 通讯密匙
     */
    private String key;
    private List<String> online;

    public InitializePack(String uuid, String ip, String name, String key, List<String> online) {
        this.uuid = uuid;
        this.ip = ip;
        this.name = name;
        this.key = key;
        this.online = online;
    }
}
