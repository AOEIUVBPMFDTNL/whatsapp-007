package com.whatsapp.android;


/* renamed from: X.1lS */
public class C35611lS {
    public final int A00;
    public final String A01;
    public final byte[] A02;
    public final byte[] A03;
    public final byte[] A04;


    public C35611lS(String str, byte[] bArr, byte[] bArr2, byte[] bArr3, int i) {
        this.A00 = i;
        this.A03 = bArr2;
        this.A02 = bArr;
        this.A04 = bArr3;
        this.A01 = str;
    }

    /*public String A00() {
        JSONArray jSONArray = new JSONArray();
        jSONArray.put(this.A00);
        jSONArray.put(Base64.encodeToString(this.A02, 3));
        byte[] bArr = this.A03;
        if (bArr != null) {
            jSONArray.put(Base64.encodeToString(bArr, 3));
        }
        byte[] bArr2 = this.A04;
        if (bArr2 != null) {
            jSONArray.put(Base64.encodeToString(bArr2, 3));
        }
        String str = this.A01;
        if (str != null) {
            jSONArray.put(str);
        }
        return jSONArray.toString();
    }*/
}