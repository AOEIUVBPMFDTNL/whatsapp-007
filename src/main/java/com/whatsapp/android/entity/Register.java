package com.whatsapp.android.entity;

import axolotl.AxolotlManager;
import io.netty.util.Timeout;
import lombok.Data;

import java.util.Map;

/**
 * 注册包
 *
 * @author sunnoc
 * @date 2021-04-22 17:20
 */
@Data
public class Register {
    /**
     * 用户名
     */
    private String username;
    /**
     * 数据库管理
     */
    private AxolotlManager axolotlManager;
    /**
     * 注册数据目录
     */
    private String registerDataDir;
    /**
     * 延时任务
     */
    private Timeout timeout;
    /**
     * 注册参数
     */
    private Map<String, Object> map;
}
