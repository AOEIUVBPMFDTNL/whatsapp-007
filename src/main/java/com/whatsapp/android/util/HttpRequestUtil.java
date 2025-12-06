package com.whatsapp.android.util;


import com.whatsapp.android.config.SocksConnectionSocketFactory;
import com.whatsapp.android.config.SocksSSLConnectionSocketFactory;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.entity.ProxyInfo;
import com.whatsapp.android.entity.StatusResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.*;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.springframework.util.StringUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

/**
 * @author sunnoc
 * @date 2020-07-04 15:07
 */
@Slf4j
public class HttpRequestUtil {
    public SSLContext SSL_CONTEXT = ssl();
    private String proxyHost;
    private int proxyPort;
    private String proxyUser;
    private String proxyPwd;
    private CloseableHttpClient httpClient;
    private HttpClientContext context;
    private BasicHttpClientConnectionManager connManager;
    private HttpClientBuilder httpClientBuilder = HttpClients.custom();
    private CookieStore cookieStore;
    private CloseableHttpResponse response;
    private boolean makeHttpProxy;
    private HttpHost proxyHttpHost;
    private int connectTimeout = 6000;
    private int socketTimeout = 40000;

    public HttpRequestUtil connectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    public HttpRequestUtil socketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
        return this;
    }

    public HttpRequestUtil() {
    }

    public HttpRequestUtil(CookieStore cookieStore) {
        this.cookieStore = cookieStore;
    }

    private HttpRequestUtil(String proxyHost, int proxyPort) {
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
    }

    private HttpRequestUtil(String proxyHost, int proxyPort, CookieStore cookieStore) {
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.cookieStore = cookieStore;
    }

    private HttpRequestUtil(String proxyHost, int proxyPort, String proxyUser, String proxyPwd) {
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.proxyUser = proxyUser;
        this.proxyPwd = proxyPwd;
    }

    private HttpRequestUtil(String proxyHost, int proxyPort, String proxyUser, String proxyPwd, CookieStore cookieStore) {
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.proxyUser = proxyUser;
        this.proxyPwd = proxyPwd;
        this.cookieStore = cookieStore;
    }

    public static HttpRequestUtil init() {
        return new HttpRequestUtil();
    }

    public static HttpRequestUtil init(CookieStore cookieStore) {
        return new HttpRequestUtil(cookieStore);
    }

    public static HttpRequestUtil init(String proxyHost, int proxyPort, String proxyUser, String proxyPwd) {
        return new HttpRequestUtil(proxyHost, proxyPort, proxyUser, proxyPwd);
    }

    public static HttpRequestUtil init(String proxyHost, int proxyPort, String proxyUser, String proxyPwd, CookieStore cookieStore) {
        return new HttpRequestUtil(proxyHost, proxyPort, proxyUser, proxyPwd, cookieStore);
    }

    public static HttpRequestUtil init(String proxyHost, int proxyPort) {
        return new HttpRequestUtil(proxyHost, proxyPort);
    }

    public static HttpRequestUtil init(String proxyHost, int proxyPort, CookieStore cookieStore) {
        return new HttpRequestUtil(proxyHost, proxyPort, cookieStore);
    }

    private void setVerify() {
        if (makeHttpProxy) {
            if (proxyUser != null && proxyUser.length() > 0 && proxyPwd != null && proxyPwd.length() > 0) {
                CredentialsProvider provider = new BasicCredentialsProvider();
                provider.setCredentials(new AuthScope(proxyHost, proxyPort),
                        new UsernamePasswordCredentials(proxyUser, proxyPwd));
                httpClientBuilder.setDefaultCredentialsProvider(provider);
            }
        } else {
            if (proxyUser != null && proxyUser.length() > 0 && proxyPwd != null && proxyPwd.length() > 0) {
                Authenticator.setDefault(ThreadLocalAuthUtil.AUTHENTICATOR);
                ThreadLocalAuthUtil.AUTHENTICATOR.setPasswordAuthentication(new PasswordAuthentication(proxyUser, proxyPwd.toCharArray()));
            }
            InetSocketAddress socketAddress = new InetSocketAddress(proxyHost, proxyPort);
            context = HttpClientContext.create();
            context.setAttribute("socks.address", socketAddress);
        }
    }

    public HttpRequestUtil makeHttpProxyClient() {
        makeHttpProxy = true;
        proxyHttpHost = new HttpHost(proxyHost, proxyPort);
        setVerify();
        httpClientBuilder.setSSLContext(SSL_CONTEXT);
        return this;
    }

    public HttpRequestUtil makeSocksProxyClient() {
        setVerify();
        Registry<ConnectionSocketFactory> reg = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", new SocksConnectionSocketFactory())
                .register("https", new SocksSSLConnectionSocketFactory(SSL_CONTEXT))
                .build();
        // HTTP客户端连接管理池
        connManager = new BasicHttpClientConnectionManager(reg);
        return this;
    }

    public RequestConfig getDefaultConfig() {
        RequestConfig.Builder builder = RequestConfig.custom()
                //设置连接超时时间，单位毫秒
                .setConnectTimeout(connectTimeout)
                //请求获取数据的超时时间，单位毫秒。 如果访问一个接口，多少时间内无法返回数据，就直接放弃此次调用
                .setSocketTimeout(socketTimeout)
                //设置cookie规则
                .setCookieSpec(CookieSpecs.STANDARD)
                //禁止重定向请求自动跳转
                .setRedirectsEnabled(false);
        if (makeHttpProxy) {
            builder.setProxy(proxyHttpHost);
        }
        return builder.build();
    }

    private HttpPost httpPost(String url, String data) {
        // 请求目标
        HttpPost request = new HttpPost(url);
        request.setEntity(new StringEntity(data, "utf-8"));
        request.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/84.0.4147.89 Safari/537.36");
        return request;
    }

    private HttpGet httpGet(String url) {
        // 请求目标
        HttpGet request = new HttpGet(url);
        request.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/84.0.4147.89 Safari/537.36");
        return request;
    }

    public String execute(HttpUriRequest request) {
        if (connManager != null) {
            connManager.setSocketConfig(SocketConfig.custom().setSoTimeout(socketTimeout).build());
            httpClientBuilder.setConnectionManager(connManager);
        }
        if (cookieStore != null) {
            httpClientBuilder.setDefaultCookieStore(cookieStore);
        }
        //禁用自动重试
        httpClientBuilder.disableAutomaticRetries();
        httpClientBuilder.setDefaultSocketConfig(SocketConfig.custom().setSoTimeout(socketTimeout).build());
        httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
        httpClient = httpClientBuilder.build();
        try {
            response = httpClient.execute(request, context);

            HttpEntity entity = response.getEntity();
            return EntityUtils.toString(entity);
        } catch (Throwable ignored) {

        } finally {
            release();
        }
        return null;
    }

    public HttpResponse executeAndGetResponse(HttpUriRequest request) {
        if (connManager != null) {
            connManager.setSocketConfig(SocketConfig.custom().setSoTimeout(socketTimeout).build());
            httpClientBuilder.setConnectionManager(connManager);
        }
        if (cookieStore != null) {
            httpClientBuilder.setDefaultCookieStore(cookieStore);
        }
        //禁用自动重试
        httpClientBuilder.disableAutomaticRetries();
        httpClientBuilder.setDefaultSocketConfig(SocketConfig.custom().setSoTimeout(socketTimeout).build());
        httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
        httpClient = httpClientBuilder.build();
        try {
            response = httpClient.execute(request, context);
        } catch (Throwable e) {
            log.error("http请求异常", e);
            return new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("https", 1, 2),
                    Constant.HTTP_REQUEST_EXCEPTION, e.getClass().getName() + ":" + e.getMessage()));
        }
        return response;
    }

    public String get(String url) {
        HttpGet request = httpGet(url);
        request.setConfig(getDefaultConfig());
        return execute(request);
    }

    public String post(String url, String data) {
        HttpPost request = httpPost(url, data);
        request.setConfig(getDefaultConfig());
        return execute(request);
    }

    public String postJson(String url, String data) {
        HttpPost request = httpPost(url, data);
        request.setConfig(getDefaultConfig());
        request.addHeader("Content-Type", "application/json");
        return execute(request);
    }

    /*private static SSLContext ssl() {
        SSLContext sslContext = null;
        try {
            sslContext = SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build();
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            e.printStackTrace();
        }
        return sslContext;
    }*/
    /*private static SSLContext ssl() {
        TrustManager[] trustAllCerts = new TrustManager[1];
        TrustManager tm = new MyTM();
        trustAllCerts[0] = tm;
        SSLContext sc;
        try {
            sc = SSLContext.getInstance("TLSv1.2");
            sc.init(null, trustAllCerts, null);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            return null;
        }
        return sc;
    }*/

    private static SSLContext ssl() {
        SSLContext sslContext = null;
        try {
            sslContext = SSLContexts.custom().loadTrustMaterial(null, (TrustStrategy) (x509Certificates, s) -> true).build();
        } catch (Throwable ignored) {

        }
        return sslContext;
    }
    /*public static SSLContext ssl() {
        TrustManager[] trustAllCerts = new TrustManager[1];
        TrustManager tm = new MyTM();
        trustAllCerts[0] = tm;
        SSLContext sc;
        try {
            sc = SSLContext.getInstance("TLSv1.2");
            sc.init(null, trustAllCerts, null);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            return null;
        }
        return sc;
    }*/

    public void release() {
        try {
            if (response != null) {
                response.close();
            }
        } catch (Throwable ignored) {
        }
        try {
            httpClient.close();
        } catch (Throwable ignored) {
        }
        if (proxyUser != null && proxyUser.length() > 0 && !makeHttpProxy) {
            ThreadLocalAuthUtil.AUTHENTICATOR.clearPasswordAuthentication();
        }
    }

    /**
     * 获取一个httpClient
     *
     * @param proxyInfo proxyInfo
     * @return HttpRequestUtil
     */
    public static HttpRequestUtil getHttpClient(ProxyInfo proxyInfo) {
        HttpRequestUtil httpRequest;
        if (proxyInfo != null) {
            if (StringUtils.isEmpty(proxyInfo.getProxyUser())) {
                httpRequest = HttpRequestUtil.init(proxyInfo.getProxyHost(), proxyInfo.getProxyPort());
            } else {
                httpRequest = HttpRequestUtil.init(proxyInfo.getProxyHost(), proxyInfo.getProxyPort(), proxyInfo.getProxyUser(), proxyInfo.getProxyPwd());
            }
            if (proxyInfo.getType() == 0) {
                httpRequest.makeHttpProxyClient();
            } else {
                httpRequest.makeSocksProxyClient();
            }
        } else {
            httpRequest = HttpRequestUtil.init();
        }
        return httpRequest;
    }

    public static HttpRequestUtil getHttpClient(ProxyInfo proxyInfo, CookieStore cookieStore) {
        HttpRequestUtil httpRequest;
        if (proxyInfo != null) {
            if (StringUtils.isEmpty(proxyInfo.getProxyUser())) {
                httpRequest = HttpRequestUtil.init(proxyInfo.getProxyHost(), proxyInfo.getProxyPort(), cookieStore);
            } else {
                httpRequest = HttpRequestUtil.init(proxyInfo.getProxyHost(), proxyInfo.getProxyPort(), proxyInfo.getProxyUser(), proxyInfo.getProxyPwd(), cookieStore);
            }
            if (proxyInfo.getType() == 0) {
                httpRequest.makeHttpProxyClient();
            } else {
                httpRequest.makeSocksProxyClient();
            }
        } else {
            httpRequest = HttpRequestUtil.init();
        }
        return httpRequest;
    }

    /**
     * 执行请求
     *
     * @param request request
     * @return statusResult
     */
    public static StatusResult executeRequest(HttpRequestUtil httpRequest, HttpUriRequest request) {
        HttpResponse httpResponse = httpRequest.executeAndGetResponse(request);
        if (httpResponse == null) {
            httpRequest.release();
            return StatusResult.fail();
        }
        int resultCode = httpResponse.getStatusLine().getStatusCode();
        String content = null;
        if (resultCode == Constant.HTTP_REQUEST_EXCEPTION) {
            httpRequest.release();
            return StatusResult.fail(httpResponse.getStatusLine().getReasonPhrase());
        }
        try {
            content = EntityUtils.toString(httpResponse.getEntity());
        } catch (Throwable ignored) {

        }
        httpRequest.release();
        return StatusResult.ok(content);
    }
}
