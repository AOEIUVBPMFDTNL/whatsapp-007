package com.whatsapp.android.entity.pack.message;

import lombok.Data;

/**
 * 接受voip语音邀请
 *
 * @author Rocky
 */
@Data
public class AcceptCallPack {
    /**
     * callId
     */
    private String callId;
    /**
     * 粉丝用户id
     */
    private String callCreator;
    /**
     * 播放url
     */
    private String playUrl;

}
