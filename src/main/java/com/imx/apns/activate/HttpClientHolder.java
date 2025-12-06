package com.imx.apns.activate;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.imx.apns.common.Constants.CERT_ROOT_FILES;


public class HttpClientHolder {

    static volatile CloseableHttpClient httpClient;

    private HttpClientHolder() {
    }

    public static CloseableHttpClient getHttpClient() throws Exception {
        if (httpClient == null) {
            synchronized (HttpClientHolder.class) {
                if (httpClient == null) {
                    httpClient = new Builder()
                            .maxConnTotal(10)
                            .connectionTimeToLive(10)
                            .certFiles(CERT_ROOT_FILES)
                            .build();
                }
            }
        }
        return httpClient;
    }


    public static class Builder {

        String[] CERT_FILES;

        int maxConnTotal;

        int connectionTimeToLive;

        public Builder certFiles(String[] cerFiles) {
            this.CERT_FILES = cerFiles;
            return this;
        }

        public Builder maxConnTotal(int maxConnTotal) {
            this.maxConnTotal = maxConnTotal;
            return this;
        }

        public Builder connectionTimeToLive(int connectionTimeToLive) {
            this.connectionTimeToLive = connectionTimeToLive;
            return this;
        }

        public CloseableHttpClient build() throws Exception {
            return HttpClientBuilder.create()
                    .setSSLContext(SSLContextFactory.getSSLContext(CERT_FILES))
                    .setMaxConnTotal(maxConnTotal)
                    .setConnectionTimeToLive(connectionTimeToLive, TimeUnit.SECONDS)
                    .build();
        }
    }

    static class SSLContextFactory {

        static SSLContext getSSLContext(String[] certFiles) throws Exception {
            List<Certificate> certificates = new ArrayList<>();
            CertificateFactory cf = CertificateFactory.getInstance("X.509");

            for (String certFilePath : certFiles) {
                try (InputStream caInput = new FileInputStream(certFilePath)) {
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

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), new java.security.SecureRandom());
            return sslContext;
        }

    }

}
