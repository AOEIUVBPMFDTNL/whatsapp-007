package QRcode;

public class SyncdKeyId {

    /* renamed from: A00 */
    public final byte[] id_data;

    public SyncdKeyId(int i, int epoch) {
        if (i == 0 && epoch == 0) {
            this.id_data = new byte[0];
            return;
        }
        byte[] bArr = new byte[6];
        this.id_data = bArr;
        bArr[1] = (byte) i;
        bArr[0] = (byte) (i >> 8);
        bArr[5] = (byte) epoch;
        bArr[4] = (byte) (epoch >> 8);
        bArr[3] = (byte) (epoch >> 16);
        bArr[2] = (byte) (epoch >> 24);
    }

    public SyncdKeyId(byte[] bArr) {
        this.id_data = bArr;
    }

    /* renamed from: A00 */
    public int GetDeviceId() {
        byte[] bArr = this.id_data;
        if (bArr.length != 0) {
            return (bArr[1] & 255) | ((bArr[0] & 255) << 8);
        }
        return 0;
    }

    /* renamed from: A01 */
    public int GetEpoch() {
        byte[] bArr = this.id_data;
        if (bArr.length == 0) {
            return 0;
        }
        return AnonymousClass053.A06(bArr, 2);
    }

}
