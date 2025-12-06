package com.imx.apns.gen;

import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;

import java.io.StringWriter;
import java.security.KeyPair;
import java.util.UUID;

public class PEMGenerator {

    private static final String SIGNATURE_ALGORITHM = "SHA256WITHRSA";

    public static String generate(KeyPair keyPair) throws Exception {
        X500NameBuilder nameBuilder = new X500NameBuilder(BCStyle.INSTANCE);
        nameBuilder.addRDN(BCStyle.C, "US");
        nameBuilder.addRDN(BCStyle.ST, "CA");
        nameBuilder.addRDN(BCStyle.L, "Cupertino");
        nameBuilder.addRDN(BCStyle.O, "Apple Inc.");
        nameBuilder.addRDN(BCStyle.OU, "iPhone");
        nameBuilder.addRDN(BCStyle.CN, UUID.randomUUID().toString());

        PKCS10CertificationRequestBuilder p10Builder = new JcaPKCS10CertificationRequestBuilder(
                nameBuilder.build(), keyPair.getPublic());
        JcaContentSignerBuilder jcsBuilder = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM);
        ContentSigner signer = jcsBuilder.build(keyPair.getPrivate());
        PKCS10CertificationRequest pkcs10CertificationRequest = p10Builder.build(signer);

        StringWriter stringWriter = new StringWriter();
        try (JcaPEMWriter jcaPEMWriter = new JcaPEMWriter(stringWriter)) {
            jcaPEMWriter.writeObject(pkcs10CertificationRequest);
        }

        return stringWriter.getBuffer().toString();
    }

}
