package QRcode;

import java.util.Base64;

public class ScanWeb {
   public static void Scan(String qrcode) {
       String[] split = qrcode.split(",");
       byte[] bArr = new byte[64];
       AnonymousClass0AM.RandomBytes(bArr);
       C91044Eq r3 = new C91044Eq(split[0], split[1], split[2], bArr);
       C03420Dv A022 = C02770At.A02();
       XPublicKey A032 = C02770At.ParseXPublicKey(AnonymousClass3AI.A00(new byte[]{5}, Base64.getDecoder().decode(r3.pub_key)));
       AnonymousClass0E6 r8 = A022.A00;
       if (r8.A00 == 5) {
           byte[] A0P = C02770At.A0P(C67652yr.A00().A03(A032.key, r8.A01), null, 80);
           byte[] bArr3 = new byte[32];
           System.arraycopy(A0P, 0, bArr3, 0, 32);
           byte[] bArr4 = new byte[32];
           System.arraycopy(A0P, 32, bArr4, 0, 32);
           byte[] bArr5 = new byte[16];
           System.arraycopy(A0P, 64, bArr5, 0, 16);
           byte[] A023 = AnonymousClass3AI.A02(bArr3, bArr5, r3.random64bytes);
           byte[] bArr6 = A022.A01.key;
           byte[] A012 = AnonymousClass3AI.A01(bArr4, AnonymousClass3AI.A00(bArr6, A023));
           String encodeToString = Base64.getEncoder().encodeToString(AnonymousClass3AI.A00(bArr6, AnonymousClass3AI.A00(A012, A023)));
           if (encodeToString != null) {
                C55942el.A02(r3.ref, r3.base64random64bytes, r3.part_3, encodeToString, 0);
           }
       }

   }
}
