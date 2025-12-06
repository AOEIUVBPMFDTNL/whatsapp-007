package com.imx.apns.common;

import com.dd.plist.NSDictionary;
import com.dd.plist.NSObject;
import com.dd.plist.PropertyListParser;
import com.imx.common.util.PatternUtil;
import com.imx.netty.ssl.SslContextProvider;
import com.whatsapp.android.util.HttpClientUtil;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.imx.apns.common.Constants.BAG_URL;

@Slf4j
public class AppleBag {

    private static final Map<String, NSDictionary> BAG_CACHE = new ConcurrentHashMap<>();

    public static NSDictionary getBag(String bagUrl) throws Exception {
        if (BAG_CACHE.containsKey(bagUrl)) {
            return BAG_CACHE.get(bagUrl);
        }
        DefaultHttpHeaders entries = new DefaultHttpHeaders();
        entries.set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED);
        HttpClientUtil.ResponseResult responseResult = HttpClientUtil.builder()
                .url(bagUrl)
                .headers(entries)
                .sslContextBuilder(SslContextProvider.SSL_CONTEXT_BUILDER)
                .connectTimeoutMillis(5000)
                .responseTimeout(Duration.ofSeconds(20))
                .build()
                .get();
        byte[] content = responseResult.getBytes();
        NSDictionary dictionary = (NSDictionary) PropertyListParser.parse(content);
        HashMap<String, NSObject> hashMap = dictionary.getHashMap();
        NSObject bag = hashMap.get("bag");
        String xml = bag.toXMLPropertyList();
        Optional<String> data = PatternUtil.getData(xml, Constants.PLIST_XML_CERTIFICATE_PATTERN);
        if (!data.isPresent()) {
            throw new RuntimeException();
        }
        String certificate = data.get().trim();
        byte[] bytes = Base64.getDecoder().decode(certificate);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        NSDictionary nsDict = (NSDictionary) PropertyListParser.parse(byteArrayInputStream);
        BAG_CACHE.put(bagUrl, nsDict);
        return nsDict;
    }


    public static String getAPNsServerHost() {
        try {
            NSDictionary bag = getBag(BAG_URL);
            int hostCount = (int) bag.get("APNSCourierHostcount").toJavaObject();
            Random rand = new Random();
            int randomHostIndex = rand.nextInt(hostCount) + 1;

            String hostname = (String) bag.get("APNSCourierHostname").toJavaObject();
            //log.info("Hostname: {} hostCount: {}", hostname, hostCount);
            return String.format("%d-%s", randomHostIndex, hostname);
        } catch (Exception e) {
            Random rand = new Random();
            int randomHostIndex = rand.nextInt(50) + 1;
            return randomHostIndex + "-courier.push.apple.com";
        }

    }

}
