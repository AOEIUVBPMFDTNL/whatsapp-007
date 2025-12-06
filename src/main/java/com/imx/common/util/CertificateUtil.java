package com.imx.common.util;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class CertificateUtil {

    public static X509Certificate parse(byte[] certificateBytes) throws Exception {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(certificateBytes);
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        return (X509Certificate) factory.generateCertificate(inputStream);
    }

}
