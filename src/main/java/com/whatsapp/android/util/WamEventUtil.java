package com.whatsapp.android.util;

import Wam.proto.WamEvent;
import Wam.proto.WamEventTsNavigation;
import Wam.proto.WamRecord;
import cn.hutool.core.util.RandomUtil;

/**
 * 生成wam事件
 *
 * @author Rocky
 */
public class WamEventUtil {

    public static Long navigationMainChat(WamRecord.Builder recordBuilder, long recordTimeStampMs, int sessionId, String unifiedSession) {
        long eventTimeMs = recordTimeStampMs;
        // 主页面至聊天界面
        WamEventTsNavigation.Builder mainToChat = WamEventTsNavigation.newBuilder();
        mainToChat.setNavigationSourceNumber(6);
        mainToChat.setNavigationDestinationNumber(4);
        mainToChat.setTsSessionId(sessionId);
        // 随机增加
        eventTimeMs = eventTimeMs + RandomUtil.randomInt(1000, 2000);
        mainToChat.setTsTimestampMs(eventTimeMs);
        // 再source页面保留的时间
        mainToChat.setRelativeTimestampMs(RandomUtil.randomInt(30000, 120000));
        WamEvent.Builder mainToChatEventBuilder = WamEvent.newBuilder();
        mainToChat.setUnifiedSessionId(unifiedSession);
        mainToChatEventBuilder.setWamEventTsNavigation(mainToChat);
        recordBuilder.addEvents(mainToChatEventBuilder);
        // 聊天界面返回主页面
        WamEventTsNavigation.Builder chatToMain = WamEventTsNavigation.newBuilder();
        chatToMain.setNavigationSourceNumber(4);
        chatToMain.setNavigationDestinationNumber(6);
        chatToMain.setTsSessionId(sessionId);
        // 随机增加
        eventTimeMs = eventTimeMs + RandomUtil.randomInt(1000, 2000);
        chatToMain.setTsTimestampMs(eventTimeMs);
        chatToMain.setRelativeTimestampMs(RandomUtil.randomInt(30000, 120000));
        WamEvent.Builder chatToMainEventBuilder = WamEvent.newBuilder();
        chatToMain.setUnifiedSessionId(unifiedSession);
        chatToMainEventBuilder.setWamEventTsNavigation(chatToMain);
        recordBuilder.addEvents(chatToMainEventBuilder);
        return eventTimeMs;
    }

}
