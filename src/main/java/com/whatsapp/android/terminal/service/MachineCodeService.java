package com.whatsapp.android.terminal.service;


import com.whatsapp.android.terminal.entity.MachineCode;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author sunnoc
 * @since 2020-07-28
 */
public interface MachineCodeService {
    /**
     * 存储终端机器码
     *
     * @param uuid 机器码
     * @return insMachineCode
     */
    MachineCode saveMachineCode(String uuid);

}
