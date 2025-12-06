package com.whatsapp.android.terminal.entity;


import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * <p>
 *
 * </p>
 *
 * @author sunnoc
 * @since 2020-07-28
 */
@Data
public class Terminal implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */

    private Long id;

    /**
     * 终端名称
     */
    private String name;

    /**
     * 终端小号数
     */
    private Integer num;

    /**
     * 终端版本
     */
    private String version;

    /**
     * 链接ws地址
     */
    private String ws;

    /**
     * 终端内存
     */
    private String memory;

    /**
     * 终端cpu
     */
    private String cpu;

    /**
     * 系统信息
     */
    private String systemInfo;

    /**
     * 业务逻辑客户端
     */
    private String uuid;

    /**
     * 终端私网ip
     */
    private String ip;
    /**
     * 终端公网ip
     */
    private String publicIp;
    /**
     * 创建时间
     */
    private Date gmtCreate;

    /**
     * 更新时间
     */
    private Date gmtModified;

    /**
     * 业务类型 1FB 2INS
     */
    private Integer platformType;
    /**
     * 国家代码
     */
    private String countryCode;
}
