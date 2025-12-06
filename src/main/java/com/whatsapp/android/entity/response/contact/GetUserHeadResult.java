package com.whatsapp.android.entity.response.contact;

import com.whatsapp.android.entity.StatusResult;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author sunnoc
 * @date 2021-03-10 15:38
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class GetUserHeadResult extends StatusResult {
    private String picture;
    private String modifyPictureTime;
    /**
     * 是否没有头像
     */
    private Boolean noHead;

    public GetUserHeadResult() {
    }

    public GetUserHeadResult(StatusResult statusResult) {
        super(statusResult.getStatus(), statusResult.getMessage());
    }

    public GetUserHeadResult(StatusResult statusResult, Boolean noHead) {
        super(statusResult.getStatus(), statusResult.getMessage());
        this.noHead = noHead;
    }
}
