package com.whatsapp.android.entity.pack.contact;

import lombok.Data;

/**
 * @author sunnoc
 * @date 2022-01-24 09:52
 */
@Data
public class TimeLimitedMessagePack {
    /**
     * 粉丝id
     */
    private String userId;
    /**
     * 过期时间，0则代表关闭
     */
    private int expireTime;
}
