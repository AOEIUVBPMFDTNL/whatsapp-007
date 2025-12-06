package com.whatsapp.android.entity;

import lombok.Data;

import java.util.List;

/**
 * 国际移动号码
 *
 * @author sunnoc
 * @date 2021-11-04 12:37
 */
@Data
public class InternationalMobilePhoneNumbers {
    /**
     * 英文名
     */
    private String englishName;
    /**
     * 中文名
     */
    private String chineseName;
    /**
     * 地区代码列表
     */
    private List<String> areaCodeList;
    /**
     * 号码长度列表
     */
    private List<String> lengthList;
}
