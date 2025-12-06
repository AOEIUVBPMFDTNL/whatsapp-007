package com.whatsapp.android.util;

import com.whatsapp.android.entity.ProxyInfo;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelOption;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.resolver.NoopAddressResolverGroup;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.transport.ProxyProvider;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Builder
@Slf4j
@Data
public class HttpClientUtil {
    /**
     * 代理信息
     */
    private ProxyInfo proxyInfo;
    /**
     * 请求地址
     */
    private String url;
    /**
     * 请求头
     */
    private HttpHeaders headers;
    /**
     * 请求数据
     */
    private byte[] data;

    /**
     * 自定义的ssl context
     */
    private SslContextBuilder sslContextBuilder;
    /**
     * 连接超时
     */
    @Builder.Default
    private long connectTimeoutMillis = 6000;
    /**
     * 响应超时
     */
    @Builder.Default
    private Duration responseTimeout = Duration.ofSeconds(40);
    private static final SslContextBuilder SSL_CONTEXT_BUILDER = SslContextBuilder.forClient()
            .trustManager(InsecureTrustManagerFactory.INSTANCE);

    public static final String READ_TIMEOUT_EXCEPTION = "读超时异常";
    public static final String REQUEST_EXCEPTION = "请求异常";

    public static void main(String[] args) {
        for (int i = 0; i < 10; i++) {
            ProxyInfo proxyInfo = new ProxyInfo();
            proxyInfo.setType(1);
            proxyInfo.setProxyHost("127.0.0.1");
            proxyInfo.setProxyPort(1080);
        /*proxyInfo.setType(0);
        proxyInfo.setProxyHost("127.0.0.1");
        proxyInfo.setProxyPort(8899);*/
            //String url = "https://v.whatsapp.net/v2/exist?ENC=PeNd6zmRGt-pgnb9pwgrG3RutAmg0dfYcSdYww7gDF1xmEbPIVXt_3f9QqElsv6u5Nj8743-YpUrvJzeN-BHBUgqaQWFj64pXDc5iLS_mWHk4adovVHIRqeJmG3uC6OzcWEdzxEBZEntz3e79hhwMjmnHklHarJvGGiyLYaY-OGeo6hnrLWwxiBM2R7mBBWmcjN8r_clpAZIQlQEAcxizX8KZoHMeAN4S8TaWQS0uwvhOaeebiVy4B3BXOJYgRV2vlcWKtiUxuVt_ZQCVIUVdmwloNihnuW1CcNINn7MOHeiWo4ewQypZBNCkCjjozoILLBdTef8hMnIJybIYBIACl-EBl3RiYnXdlRE-rt7fQc2avCupmkAFAU8qezur357ulWEohcxPTT0xIr37Su5PenoJP282kdhVzTn5aa5ZMLmPYjfvIIzOJ8QYJMmivzuF15fQawwxzqPPbhA6TnD5OE3Js-kTVELA_8TZeWpS8VMEw5xkUT7NqXVrgP49BEptZIuZPZUjnsH4HoaJw62Wfi7rnpKkYf6k3cTj5yJ71Br6Txprsx03vYv_bAnonxk8wi_8PxJs6LWAEesNiaUh2VsrlbR9Hr9I1XOIE5eywML-bYKt8bg1MtSSr2qLtcyTSF_jWy2eX-0_LQQhuypPYoeQ313JnmqKTh0K8FC-AiMu_ypTrd5JkmA3DVJjovwmbmdcY7PP5AqL4a46QRRrz6Kqa0-n1vycJTJdETWkxOH-t9ysdv6I3uLuDqfDccZGlE_ERwew4Mz9h2YakbR47N-FWzAcvy14eHMPY1Y3X4jVHys3w6aUxD2jpNgHIJzzTmUy4DuC9eEGcco1VuJl8hdIYpUKj5l6s-LMtxaKhHFDXiDM9irQ6Nl6ItuxcBTRefavLVwy1Jq0XVB-XTMMRHNg68WWTswOPrJACLRoHV8hbfB-HflZS9JgBU8ZhQrNJdt-43nnL8H_Wm3pg8xz-M4p8owy5rZavxaY8dNRs6lVV04OMcNr-_17nUaQzZ5hQveFMjsgXLqbwbhMfcjXhvUcb6O-9sEr0CI3xFDrnKs-2W4NWyDlbIHI1GkfJ67kvphTzHKyvwQBSsANm6k0iaDlVpRb_5eAqOSDb07naE0tpfMeyUNVyOZOqkJJPK8gSe0vX9M4GvRw2Ec0pQHLWak6xnht2ingXLQsNXBCz-WpqGY7uovPkD_rY6OWdcDhMw1zV3LXspl";
            String url = "https://v.whatsapp.net/v2/exist";
            DefaultHttpHeaders entries = new DefaultHttpHeaders();
            entries.set(HttpHeaderNames.USER_AGENT, "WhatsApp/2.23.16.76 SMBA/9 Device/ZTE_T620");
            entries.set("WaMsysRequest", "1");
            entries.set("accept-encoding", "gzip");
            /**
             * user-agent: WhatsApp/2.23.16.76 SMBA/9 Device/ZTE_T620
             * WaMsysRequest: 1
             * accept-encoding: gzip
             * connection: keep-alive
             * host: v.whatsapp.net
             */
            ResponseResult responseResult = HttpClientUtil
                    .builder()
                    .proxyInfo(proxyInfo)
                    .headers(entries)
                    .url(url)
                    .connectTimeoutMillis(5000)
                    .responseTimeout(Duration.ofSeconds(20))
                    .build()
                    .get();
            String s = responseResult.getResultString();
            log.info("请求结果：{}", s);
            log.info(responseResult.toString());
        }
    }

    /**
     * 执行post请求
     */
    public ResponseResult post() {
        try {
            ResponseResult responseResult = new ResponseResult();
            byte[] result = getHttpClient().responseTimeout(responseTimeout)
                    .headers(httpHeaders -> {
                        if (headers != null) {
                            httpHeaders.add(headers);
                        }
                    })
                    .post()
                    .uri(url)
                    .send(Mono.just(Unpooled.wrappedBuffer(data)))
                    .responseSingle((resp, bytes) -> {
                        responseResult.setSuccess(true);
                        responseResult.setResponseHeaders(resp.responseHeaders());
                        responseResult.setStatusCode(resp.status().code());
                        return bytes.asByteArray();
                    })
                    .block();
            responseResult.setBytes(result);
            return responseResult;
        } catch (ReadTimeoutException e) {
            return new ResponseResult(false, READ_TIMEOUT_EXCEPTION);
        } catch (Exception e) {
            return new ResponseResult(false, REQUEST_EXCEPTION);
        }
    }

    /**
     * 执行get请求
     */
    public ResponseResult get() {
        try {
            ResponseResult responseResult = new ResponseResult();
            byte[] result = getHttpClient().responseTimeout(responseTimeout)
                    .headers(httpHeaders -> httpHeaders.add(headers))
                    .get()
                    .uri(url)
                    .responseSingle((resp, bytes) -> {
                        responseResult.setSuccess(true);
                        responseResult.setResponseHeaders(resp.responseHeaders());
                        responseResult.setStatusCode(resp.status().code());
                        return bytes.asByteArray();
                    })
                    .block();
            responseResult.setBytes(result);
            return responseResult;
        } catch (ReadTimeoutException e) {
            return new ResponseResult(false, READ_TIMEOUT_EXCEPTION);
        } catch (Exception e) {
            return new ResponseResult(false, REQUEST_EXCEPTION);
        }
    }

    private HttpClient getHttpClient() {
        //不使用连接池
        HttpClient client = HttpClient.create(ConnectionProvider.newConnection())
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) connectTimeoutMillis);
        if (proxyInfo != null) {
            client = client.proxy(spec -> spec.type(proxyInfo.getType() == 0 ? ProxyProvider.Proxy.HTTP : ProxyProvider.Proxy.SOCKS5)
                            .host(proxyInfo.getProxyHost())
                            .port(proxyInfo.getProxyPort())
                            .username(StringUtils.hasText(proxyInfo.getProxyUser()) ? proxyInfo.getProxyUser() : "")
                            .password(p -> StringUtils.hasText(proxyInfo.getProxyPwd()) ? proxyInfo.getProxyPwd() : "")
                            .connectTimeoutMillis(connectTimeoutMillis))
                    // 使用代理的DNS解析
                    .resolver(NoopAddressResolverGroup.INSTANCE);
        }
        return client.secure(sslContextSpec -> {
            if (sslContextBuilder != null) {
                sslContextSpec.sslContext(sslContextBuilder);
            } else {
                sslContextSpec.sslContext(SSL_CONTEXT_BUILDER);
            }
        });
    }

    @Data
    public static class ResponseResult {
        /**
         * 请求是否成功
         */
        private boolean success;
        /**
         * 错误信息
         */
        private String errMsg;
        /**
         * 响应协议头
         */
        private HttpHeaders responseHeaders;
        /**
         * 状态码
         */
        private int statusCode;
        /**
         * 响应内容
         */
        private byte[] bytes;

        public ResponseResult() {
        }

        public ResponseResult(boolean success, String errMsg) {
            this.success = success;
            this.errMsg = errMsg;
        }

        public String getResultString() {
            if (bytes != null) {
                return new String(bytes, StandardCharsets.UTF_8);
            }
            return null;
        }
    }
}