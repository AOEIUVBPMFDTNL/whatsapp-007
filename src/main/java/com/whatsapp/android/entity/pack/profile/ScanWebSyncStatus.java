package com.whatsapp.android.entity.pack.profile;

import lombok.Data;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author sunnoc
 * @date 2022-07-20 16:25
 */
@Data
public class ScanWebSyncStatus {
    /**
     * 是否正在执行扫码步骤
     */
    private boolean enable;
    /**
     * 执行步骤
     */
    private AtomicInteger inc = new AtomicInteger(1);
}
