package com.imx.apns.activate;

import com.imx.apns.gen.PlistXMLGenerator;
import com.imx.netty.ssl.SslContextProvider;
import com.whatsapp.android.GorgeousEngine;
import com.whatsapp.android.entity.ProxyInfo;
import com.whatsapp.android.util.HttpClientUtil;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import lombok.extern.slf4j.Slf4j;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;

import static com.imx.apns.common.Constants.DEVICE_ACTIVATION_URL;

@Slf4j
public class ActivateTransport {

    public static String callOnDeviceActivation(ActivationRequest request) throws Exception {
        String url = String.format(DEVICE_ACTIVATION_URL, request.getDevice());
        HashMap<String, String> requestData = new HashMap<>();
        requestData.put("ActivationInfoComplete", Boolean.TRUE.toString());
        requestData.put("ActivationInfoXML", Base64.getEncoder().encodeToString(request.getActivationInfoXML()));
        requestData.put("FairPlayCertChain", Base64.getEncoder().encodeToString(request.getFairPlayCertChain()));
        requestData.put("FairPlaySignature", Base64.getEncoder().encodeToString(request.getFairPlaySignature()));
        String activationInfo = PlistXMLGenerator.generate(requestData);
        String encodedActivationInfo = URLEncoder.encode(activationInfo, StandardCharsets.UTF_8.toString());
        String formData = "activation-info=" + encodedActivationInfo;
        ProxyInfo gcmProxy = null;
        try {
            gcmProxy = GorgeousEngine.getAPNsProxy();
        } catch (Exception ignored) {
            log.warn("APNS proxy info is null");
        }
        DefaultHttpHeaders entries = new DefaultHttpHeaders();
        entries.set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED);
        HttpClientUtil.ResponseResult responseResult = HttpClientUtil.builder()
                .url(url)
                .headers(entries)
                .proxyInfo(gcmProxy)
                .sslContextBuilder(SslContextProvider.SSL_CONTEXT_BUILDER)
                .data(formData.getBytes())
                .connectTimeoutMillis(5000)
                .responseTimeout(Duration.ofSeconds(20))
                .build()
                .post();
        return responseResult.getResultString();
    }


}
