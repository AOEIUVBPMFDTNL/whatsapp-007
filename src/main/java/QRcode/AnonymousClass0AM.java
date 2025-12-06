package QRcode;

import java.util.Random;

public class AnonymousClass0AM {
    static Random random = new Random();
    public static byte[] RandomBytes(int i) {
        byte[] bArr = new byte[i];
        random.nextBytes(bArr);
        return bArr;
    }

    public static void RandomBytes(byte[] data) {
        random.nextBytes(data);
    }

    public static int RandomInt() {
        return random.nextInt();
    }
}
