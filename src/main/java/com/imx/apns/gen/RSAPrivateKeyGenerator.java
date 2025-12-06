package com.imx.apns.gen;

import com.imx.netty.ssl.SslContextProvider;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.pkcs.RSAPrivateKey;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.RSAPrivateKeySpec;
import java.util.Base64;

public class RSAPrivateKeyGenerator {

    public static PrivateKey getPrivateKeyFromPEM() throws Exception {
        String privateKeyPEM = SslContextProvider.FAIRPLAY_PEM.replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(privateKeyPEM);

        ASN1Sequence asn1Sequence = ASN1Sequence.getInstance(keyBytes);
        RSAPrivateKey rsaPrivateKey = RSAPrivateKey.getInstance(asn1Sequence);
        RSAPrivateKeySpec privateKeySpec = new RSAPrivateKeySpec(
                rsaPrivateKey.getModulus(),
                rsaPrivateKey.getPrivateExponent()
        );

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(privateKeySpec);
    }

}
