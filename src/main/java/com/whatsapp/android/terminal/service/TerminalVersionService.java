package com.whatsapp.android.terminal.service;


import com.whatsapp.android.terminal.entity.InsTerminalVersion;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author sunnoc
 * @since 2020-07-31
 */
public interface TerminalVersionService {
    /**
     * 获取终端更新url
     *
     * @param version 版本号
     * @return url
     */
    String getUpdateKey(String version);

    /**
     * 获取最新的终端版本
     *
     * @return InsTerminalVersion
     */
    InsTerminalVersion getNewestVersion();
}
