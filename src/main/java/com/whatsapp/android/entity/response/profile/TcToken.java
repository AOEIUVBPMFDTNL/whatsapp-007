package com.whatsapp.android.entity.response.profile;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TcToken {
    private String userId;
    private String tcToken;
}
