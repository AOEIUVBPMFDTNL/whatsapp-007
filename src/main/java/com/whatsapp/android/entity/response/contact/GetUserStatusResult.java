package com.whatsapp.android.entity.response.contact;

import com.whatsapp.android.entity.StatusResult;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author sunnoc
 * @date 2022-02-14 15:31
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class GetUserStatusResult extends StatusResult {
    /**
     * 是否存在描述
     */
    private boolean existDescribe;
    /**
     * 描述
     */
    private String describe;
    /**
     * 修改描述时间
     */
    private String modifyDescribeTime;

    public GetUserStatusResult() {
    }

    public GetUserStatusResult(StatusResult statusResult) {
        super(statusResult.getStatus(), statusResult.getMessage());
    }


}
