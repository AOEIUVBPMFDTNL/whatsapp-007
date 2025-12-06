package com.whatsapp.android.entity.response.contact;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class ContactInfoResult {
    private String jid;
    private String lid;
    private String deviceHash;
    /**
     * verified_name
     */
    private String serial;
    /**
     * tcToken
     */
    private byte[] tcToken;
}
