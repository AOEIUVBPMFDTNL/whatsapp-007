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

/**
 * 更新终端
 *
 * @author sunnoc
 * @date 2020-10-22 9:43
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ApiType(TypeConstant.TaskType.UPDATE_TERMINAL)
public class UpdateTerminalService implements ApiStrategy {
    private final TerminalVersionService terminalVersionService;
    private final ApiService apiService;
    /**
     * 是否更新中
     */
    private static boolean updating;

    @Override
    public Result execute(String type, JSONObject dataObject, String taskId, User user) {
        UpdateTerminalPack updateTerminalPack = dataObject.getObject("data", UpdateTerminalPack.class);
        if (!Constant.UPDATE_TERMINAL_KEY.equals(updateTerminalPack.getKey())) {
            log.warn("更新密匙错误：{}", updateTerminalPack.getKey());
            return Result.taskFail(type, taskId, null, StatusResult.fail("更新密匙错误"));
        }
        try {
            KeyLockUtil.lock(TypeConstant.TaskType.UPDATE_TERMINAL);
            byte[] bytes = DeviceUtil.loadHistoryReactorCoreJar();
            if (bytes != null && bytes.length > 0) {
                String os = System.getProperty("os.name");
                if (os.startsWith("Linux")) {
                    String path = "/usr/javawork/whatsapp/lib/reactor-core-3.3.6.RELEASE.jar";
                    FileUtil.writeBytes(bytes, path);
                } else if (os.startsWith("Mac")) {
                    String path = "/Users/sunnoc/IdeaProjects/whatsapp-android/target/lib/reactor-core-3.3.6.RELEASE.jar";
                    FileUtil.writeBytes(bytes, path);
                }
            }
            if (updating) {
                return Result.taskFail(type, taskId, null, StatusResult.fail("终端正在更新中"));
            }
            updating = true;
        } catch (Exception ignored) {
        } finally {
            KeyLockUtil.unlock(TypeConstant.TaskType.UPDATE_TERMINAL);
        }
        //查询下载链接
        String updateKey = terminalVersionService.getUpdateKey(updateTerminalPack.getVersion());
        if (StringUtils.isEmpty(updateKey)) {
            updating = false;
            return Result.taskFail(type, taskId, null, StatusResult.fail("版本不存在"));
        }
        ThreadUtil.execute(() -> {
            ThreadUtil.sleep(3000);
            AES aes = SecureUtil.aes(Constant.UPDATE_TERMINAL_URL_KEY.getBytes());
            String url = aes.decryptStr(updateKey);
            byte[] bytes = apiService.downPhoto(url);
            if (bytes != null && bytes.length > 500) {
                FileUtil.writeBytes(bytes, Constant.JAR_PATH + Constant.JAR_PACK_NAME + ".update");
                ThreadUtil.sleep(1000);
                RuntimeUtil.execForStr("nohup java -jar " + Constant.JAR_PATH + Constant.UPDATE_JAR_PACK_NAME + " &");
            } else {
                log.error("下载更新文件失败");
            }
            updating = false;
        });

        return Result.taskSuccess(type, taskId, null, StatusResult.ok("更新中..."));
    }

}