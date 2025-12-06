package com.whatsapp.android.entity;

import lombok.Data;

/**
 * 粉丝信息
 *
 * @author Rocky
 */
@Data
public class ProfileInfo {
    private String fansId;
    private String verifiedName;
    private String updateStatusTime;
    private String lid;
}
