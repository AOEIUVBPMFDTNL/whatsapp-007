package com.whatsapp;

import java.io.Serializable;

public class Me implements Serializable {
    public static final long serialVersionUID = -1952119322404952662L;
    public final String cc;
    public final String jabber_id;
    public final String number;

    public Me(String str, String str2, String str3) {
        this.cc = str;
        this.number = str2;
        this.jabber_id = str3;
    }
}