package com.whatsapp.android.entity.response.contact;

import lombok.Data;

/**
 * @author sunnoc
 * @date 2021-03-17 16:43
 */
@Data
public class ContactResult {
    /**
     * 用户id
     */
    private String userId;
    /**
     * 原始手机号
     */
    private String phone;
    /**
     * 是否存在
     */
    private boolean exist;
    /**
     * 是否是商业号
     */
    private boolean business;
    /**
     * 头像地址
     */
    private String picture;
    /**
     * 修改头像时间
     */
    private String modifyPictureTime;
    /**
     * 描述
     */
    private String describe;
    /**
     * 修改描述时间
     */
    private String modifyDescribeTime;
}
