package QRcode;
import java.util.Arrays;
import java.util.Iterator;

public final class C67142xp implements Iterator {
    public int A00 = 0;
    public final int A01;
    public final Object[] A02;

    public C67142xp(Object[] objArr, int i) {
        this.A02 = objArr;
        this.A01 = i;
    }

    public boolean hasNext() {
        return this.A00 < this.A02.length;
    }

    @Override // java.util.Iterator
    public Object next() {
        if (hasNext()) {
            int i = this.A00;
            int i2 = this.A01;
            Object[] objArr = this.A02;
            int length = objArr.length;
            int min = Math.min(i + i2, length);
            if (length > i2 || i != 0) {
                objArr = Arrays.copyOfRange(objArr, i, min);
            }
            this.A00 += i2;
            return objArr;
        }
        return null;
    }
}