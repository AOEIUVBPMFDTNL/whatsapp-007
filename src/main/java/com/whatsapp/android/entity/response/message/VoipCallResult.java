package com.whatsapp.android.entity.response.message;

import com.whatsapp.android.entity.StatusResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class VoipCallResult extends StatusResult {
    /**
     * 拨打id
     */
    private String callId;
}
