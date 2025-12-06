package com.whatsapp.android.entity;

import lombok.Data;

@Data
public class TurnIpData {
    private String turnToken;
    private byte[] ipv4Addr;
    private byte[] ipv6Addr;
}
