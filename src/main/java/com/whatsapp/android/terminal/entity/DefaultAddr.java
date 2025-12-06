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
public class DefaultAddr implements Serializable {

    private static final long serialVersionUID=1L;

    /**
     * id
     */
    private Long id;

    /**
     * 链接ws地址
     */
    private String ws;

    /**
     * 默认为0,1代表默认连接地址
     */
    private Boolean defaultAddr;

    /**
     * 创建时间
     */
    private Date gmtCreate;

    /**
     * 更新时间
     */
    private Date gmtModified;


}
