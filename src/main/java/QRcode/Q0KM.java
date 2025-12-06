package QRcode;

import axolotl.AxolotlManager;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

public class Q0KM {
    public static final byte[] A07 = "WhatsApp Patch Integrity".getBytes(C02700Ak.A07);

    public static byte[] A01(List list, byte[] bArr, byte[] bArr2, boolean z) {
        int i;
        boolean z2 = false;
        if (bArr.length == 128) {
            z2 = true;
        }
        byte[] bArr3 = new byte[128];
        System.arraycopy(bArr, 0, bArr3, 0, 128);
        ByteBuffer wrap = ByteBuffer.wrap(bArr3);
        wrap.order(ByteOrder.LITTLE_ENDIAN);
        Iterator it = list.iterator();
        while (it.hasNext()) {
            byte[] bArr4 = (byte[]) it.next();
            ByteBuffer wrap2 = ByteBuffer.wrap(C02770At.A0P(bArr4, bArr2, 128));
            wrap2.order(ByteOrder.LITTLE_ENDIAN);
            wrap.mark();
            while (wrap.hasRemaining()) {
                int position = wrap.position();
                int i2 = wrap.getShort() & 65535;
                int i3 = wrap2.getShort() & 65535;
                if (z) {
                    i = i2 + i3;
                } else {
                    i = i2 - i3;
                }
                int position2 = wrap.position();
                wrap.position(position);
                wrap.putShort((short) (((short) i) & 65535));
                wrap.position(position2);
            }
            wrap.reset();
        }
        return wrap.array();
    }

    public static byte[] A03(String str, List list, String[] strArr, AxolotlManager axolotlManager) {
        byte[] it_hash = axolotlManager.GetItHash(str);
        if (it_hash == null) {
            it_hash = new byte[128];
        }
        HashMap hashMap = new HashMap();
        C67142xp r5 = new C67142xp(strArr, 975);
        while (r5.hasNext()) {
            String[] strArr3 = (String[]) r5.next();
            ArrayList arrayList = new ArrayList();
            arrayList.add(str);
            arrayList.addAll(Arrays.asList(strArr3));
            hashMap.putAll(axolotlManager.GetMutationMac(str, strArr3));
        }
        ArrayList arrayList2 = new ArrayList(hashMap.values());


        return A01(list, A01(arrayList2, it_hash , A07, false), A07, true);
    }
}
