package com.whatsapp.android.terminal.service;


import com.whatsapp.android.terminal.entity.Terminal;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author sunnoc
 * @since 2020-07-28
 */
public interface TerminalService {
    /**
     * 获取终端信息
     *
     * @param uuid uuid
     * @return insTerminal
     */
    String getTerminalInfo(String uuid);

    /**
     * 存储终端信息
     *
     * @param terminal insTerminal
     * @return bool
     */
    boolean saveTerminalInfo(Terminal terminal);

    /**
     * 更新终端信息
     *
     * @param terminal insTerminal
     * @return bool
     */
    boolean updateTerminalInfo(Terminal terminal);

    /**
     * 获取ip白名单列表
     */
    boolean getIpWhite();
}
