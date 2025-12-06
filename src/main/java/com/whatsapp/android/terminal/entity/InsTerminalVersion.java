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
 * @since 2020-07-31
 */
@Data
public class InsTerminalVersion implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Long id;

    /**
     * 终端版本号
     */
    private String version;

    /**
     * 更新密匙
     */
    private String updateKey;

    /**
     * 创建时间
     */
    private Date gmtCreate;

    /**
     * 更新时间
     */
    private Date gmtModified;


}
