package com.whatsapp.android.entity.response.contact;

import com.whatsapp.android.entity.StatusResult;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author sunnoc
 * @date 2021-03-12 18:07
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SyncContactResult extends StatusResult {
    private List<ContactResult> list;

    public SyncContactResult() {
    }

    public SyncContactResult(StatusResult statusResult) {
        super(statusResult.getStatus(), statusResult.getMessage());
    }
}
