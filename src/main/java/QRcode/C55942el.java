package QRcode;

import java.util.Base64;

public class C55942el {
    public static void A02(String ref, String secret, String browserId, String encryptedSecret, int loginType) {
        String GetCountry = "CN";
        String GetLanguage = "zh";
        String locales = "zh-CN";
        boolean is24HourFormat = true;
        byte[] bArr = new byte[32];
        AnonymousClass0AM.RandomBytes(bArr);
        String encodeToString = Base64.getEncoder().encodeToString(bArr);

    }
}
