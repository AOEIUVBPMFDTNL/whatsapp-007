package com.whatsapp.android.entity.response.contact;

import com.whatsapp.android.entity.StatusResult;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author sunnoc
 * @date 2021-12-08 13:17
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class GetUserDeviceInfoResult extends StatusResult {
    private List<String> userList;

    public GetUserDeviceInfoResult(List<String> userList) {
        this.userList = userList;
    }

    public GetUserDeviceInfoResult(StatusResult statusResult, List<String> userList) {
        super(statusResult.getStatus(), statusResult.getMessage());
        this.userList = userList;
    }
}
