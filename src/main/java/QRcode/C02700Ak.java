package QRcode;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class C02700Ak {
    /* renamed from: A06 */
    public static final String AcceptCharset;
    public static final Charset A07;
    public static final byte[] A08 = {6, 0};
    public static final byte[] A09 = {6, 1};
    public static final byte[] A0A = {6, 2};
    public static final int[] A0B = {604800, 0};
    public static final int[] A0C = {604800, 86400, 3600, 35, 15, 5, 0};
    public static final int[] A0D = {86400, 604800, 7776000, 0};
    public static final Long[] A0E = new Long[0];
    public static final String[] A0F = new String[0];

    static {
        Charset forName = StandardCharsets.UTF_8;
        A07 = forName;
        AcceptCharset = forName.name();
    }

}
