package com.whatsapp.android.entity.response.profile;

import com.whatsapp.android.entity.StatusResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenerateMsgIdResult extends StatusResult {
    private String msgId;
    private boolean ios;
    private boolean businessVersion;
}
