package com.whatsapp.android.entity.pack.contact;

import lombok.Data;

import java.util.List;

/**
 * @author sunnoc
 * @date 2021-03-12 18:15
 */
@Data
public class SyncContactPack {
    /**
     * 手机号必须为不带区号
     */
    private List<String> list;

    /**
     * 跳过联系人存储
     */
    private boolean skipContactStorage;
}
