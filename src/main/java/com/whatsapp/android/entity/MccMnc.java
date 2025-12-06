package com.whatsapp.android.entity;

import lombok.Data;

@Data
public class MccMnc {
    private String country;
    private String mcc;
    private String mnc;

    public MccMnc() {
    }

    public MccMnc(String mcc, String mnc) {
        this.mcc = mcc;
        this.mnc = mnc;
    }

    public MccMnc(String country, String mcc, String mnc) {
        this.country = country;
        this.mcc = mcc;
        this.mnc = mnc;
    }
}
