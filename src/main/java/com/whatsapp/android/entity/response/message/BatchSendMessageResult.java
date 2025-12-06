package com.whatsapp.android.entity.response.message;

import com.whatsapp.android.entity.StatusResult;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 批量发送消息结果
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class BatchSendMessageResult extends StatusResult {
    /**
     * 发送结果，key是粉丝id
     */
    private Map<String, MessageResult> result;

    public static BatchSendMessageResult fail(List<String> userIds, String reason) {
        Map<String, MessageResult> map = new HashMap<>();
        BatchSendMessageResult batchSendMessageResult = new BatchSendMessageResult();
        for (String userId : userIds) {
            map.put(userId, new MessageResult("", false, reason));
        }
        batchSendMessageResult.setResult(map);
        return batchSendMessageResult;
    }
}
