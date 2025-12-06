package Util;

import lombok.extern.slf4j.Slf4j;

import java.security.MessageDigest;
import java.util.*;

/**
 * @author sunnoc
 * @date 2021-11-17 12:20
 */
@Slf4j
public class CryptUtil {
    public static String PHash(Collection<String> collection) {
        ArrayList<String> arrayList = new ArrayList<>(collection.size());
        arrayList.addAll(collection);
        Collections.sort(arrayList);
        try {
            MessageDigest instance = MessageDigest.getInstance("SHA-256");
            for (String s : arrayList) {
                instance.update(s.getBytes());
            }
            byte[] digest = instance.digest();
            byte[] bArr = new byte[6];
            System.arraycopy(digest, 0, bArr, 0, 6);
            return "2:" + Base64.getEncoder().encodeToString(bArr);
        } catch (Exception e) {
            log.error("pHash加密异常", e);
        }
        return "";
    }

    public static void main(String[] args) {
        HashSet<String> hashSet = new HashSet<>();
        hashSet.add("8617748754950.0:0@s.whatsapp.net");
        hashSet.add("959401677350.0:0@s.whatsapp.net");
        hashSet.add("8615228092350.0:0@s.whatsapp.net");
        String phash = CryptUtil.PHash(hashSet);
        System.out.println(phash);
    }
}
