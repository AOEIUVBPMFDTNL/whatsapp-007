package ProtocolTree;

/**
 * @author Rocky
 */
public class StringTag {
    public final short A00;
    public final short A01;
    public final boolean A02;

    public StringTag(int v, int v1, boolean z) {
        this.A02 = z;
        this.A01 = (short) v;
        this.A00 = (short) v1;
    }
}
