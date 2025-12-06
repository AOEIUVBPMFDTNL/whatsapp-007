package QRcode;


import Util.StringUtil;

import java.util.Collections;

public class AnonymousClass33J {
    public static String A01(int count) {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        for (int i=0; i<count;i++) {
            sb.append("?");
            if (i != count -1) {
                sb.append(",");
            }
        }
        sb.append(")");
        return sb.toString();
    }

}
