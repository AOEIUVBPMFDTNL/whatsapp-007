package com.whatsapp.android.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author sunnoc
 * @date 2021-08-19 11:06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendFailMsg {
    private String username;
    private String message;
}
