package com.whatsapp.android;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.whatsapp.android.constant.Constant;

/**
 * @author sunnoc
 * @date 2022-02-11 15:28
 */
public class GenerateId {
    public static void main(String[] args) {
        for (int i = 0; i < 5; i++) {
            String msgId = generateNtMsgId();
            System.out.println(msgId);
            String ntMsgId = StrUtil.sub(msgId, 11, 21);
            System.out.println(ntMsgId);
        }
    }

    /**
     * 生成南田消息id
     */
    public static String generateNtMsgId() {
        String sourceMsgId = IdUtil.simpleUUID().toUpperCase();
        //替换内容不要修改它
        String sub = StrUtil.sub(sourceMsgId, 11, 21);
        return StrUtil.replace(sourceMsgId, sub, Constant.NT_MSG_TAG_NEW);
    }
}
