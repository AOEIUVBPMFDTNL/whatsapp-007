package com.whatsapp.android.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author sunnoc
 * @date 2023-05-23 15:34
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GcmTokenResult {
    /**
     * 令牌
     */
    private String token;
    /**
     * 完整参数
     */
    private String params;
    private String androidId;
    private String securityToken;
}
