package com.whatsapp.android.service.impl.terminal;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.annotation.ApiType;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.entity.UserRecord;
import com.whatsapp.android.entity.response.terminal.UpdateTerminalPack;
import com.whatsapp.android.service.ApiService;
import com.whatsapp.android.service.ApiStrategy;
import com.whatsapp.android.terminal.service.TerminalVersionService;
import com.whatsapp.android.util.DeviceUtil;
import com.whatsapp.android.util.KeyLockUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * 更新终端
 *
 * @author lcj
 * @date 2023-06-14 17:43
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ApiType(TypeConstant.TaskType.GET_TERMINAL_RECORD_KEY)
public class GetTerminalRecordKeyService implements ApiStrategy {

    @Override
    public Result execute(String type, JSONObject dataObject, String taskId, User user) {
        Set<String> usernames = UserRecord.getRecord().keySet();
        return Result.taskSuccess(type, taskId, null, usernames);
    }

}