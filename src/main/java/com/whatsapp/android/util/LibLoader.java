package com.whatsapp.android.util;

import cn.hutool.core.io.FileUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

/**
 * @author sunnoc
 * @date 2021-03-16 16:39
 */
@Slf4j
public class LibLoader {
    /**
     * 加载lib
     *
     * @param libName 库名称
     */
    public static void loadLib(String libName) {
        String jniDir = System.getProperty("user.dir") + "/jni";
        if (!FileUtil.exist(jniDir)) {
            FileUtil.mkdir(jniDir);
        }
        String outDir = System.getProperty("user.dir") + "/out";
        if (!FileUtil.exist(outDir)) {
            FileUtil.mkdir(outDir);
        }
        String mediaDir = outDir + "/media";
        if (!FileUtil.exist(mediaDir)) {
            FileUtil.mkdir(mediaDir);
        }
        FileUtil.clean(mediaDir);
        String registerDir = outDir + "/register";
        if (!FileUtil.exist(registerDir)) {
            FileUtil.mkdir(registerDir);
        }
        FileUtil.clean(registerDir);
        String groupMsgDir = outDir + "/groupMsg";
        if (!FileUtil.exist(groupMsgDir)) {
            FileUtil.mkdir(groupMsgDir);
        }
        // FileUtil.clean(groupMsgDir);
        String libFilePath = jniDir + "/" + libName;
        boolean result = saveResource(jniDir);
        if (result) {
            System.load(libFilePath);
        } else {
            log.error("释放lib文件失败");
        }
    }

    /**
     * 将resources目录下的文件，写入到File中
     */
    private static boolean saveResource(String jniDir) {
        try {
            String[] libArray = {"libNoiseJni.dll", "libNoiseJni.dylib", "libNoiseJni.so", "whatsapp_c", "whatsapp_d", "whatsapp_n", "whatsapp_o", "config.json"};
            for (String name : libArray) {
                String libFilePath = jniDir + "/" + name;
                FileUtil.writeFromStream(LibLoader.class.getResourceAsStream("/lib/" + name), new File(libFilePath));
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
