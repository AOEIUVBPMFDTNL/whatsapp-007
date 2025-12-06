package com.whatsapp.android.entity.response.contact;

import com.whatsapp.android.entity.StatusResult;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author sunnoc
 * @date 2022-02-14 18:23
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class FilterFansInfoResult extends StatusResult {
    /**
     * 粉丝信息
     */
    private List<FansInfo> list;

    @Data
    public static class FansInfo {
        /**
         * 更换设备时间左右
         */
        private String time;
        /**
         * 用户id
         */
        private String userId;
        /**
         * 是否有活跃
         */
        private boolean active;
    }


    public FilterFansInfoResult() {
    }

    public FilterFansInfoResult(StatusResult statusResult) {
        super(statusResult.getStatus(), statusResult.getMessage());
    }
}
