package com.imx.netty.ssl;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.CharsetUtil;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.springframework.core.io.ClassPathResource;

import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.List;

import static com.imx.apns.common.Constants.CERT_ROOT_FILES;

public class SslContextProvider {
    public static SslContext SSL_CONTEXT;
    public static String FAIRPLAY_PEM;
    public static byte[] FAIRPLAY_CERT;
    public static SslContextBuilder SSL_CONTEXT_BUILDER;

    static {
        try {
            SSL_CONTEXT = getSslContext();
            FAIRPLAY_PEM = loadFairplayPEM();
            FAIRPLAY_CERT = loadFairplayCert();
            SSL_CONTEXT_BUILDER = SslContextBuilder.forClient()
                    .trustManager(getTrustManagerFactory());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static SslContext getSslContext() throws IOException {
        ClassPathResource cpr = new ClassPathResource("/certs/root/profileidentity.ess.apple.com.cert");
        InputStream certInput = cpr.getInputStream();
        return SslContextBuilder.forClient()
                .trustManager(certInput)
                .build();
    }

    private static String loadFairplayPEM() throws IOException {
        ClassPathResource cpr = new ClassPathResource("/certs/fairplay.pem");
        InputStream inputStream = cpr.getInputStream();
        return IoUtil.read(inputStream, CharsetUtil.CHARSET_UTF_8);
    }

    private static byte[] loadFairplayCert() throws IOException {
        ClassPathResource cpr = new ClassPathResource("/certs/fairplay.cert");
        InputStream inputStream = cpr.getInputStream();
        return IoUtil.readBytes(inputStream);
    }

    private static TrustManagerFactory getTrustManagerFactory() throws Exception {
        List<Certificate> certificates = new ArrayList<>();
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        for (String certFilePath : CERT_ROOT_FILES) {
            ClassPathResource cpr = new ClassPathResource(certFilePath);
            try (InputStream caInput = cpr.getInputStream();) {
                Certificate ca = cf.generateCertificate(caInput);
                certificates.add(ca);
            }
        }
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        int certIndex = 0;
        for (Certificate certificate : certificates) {
            keyStore.setCertificateEntry("ca" + certIndex++, certificate);
        }
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore);
        return tmf;
    }
}
