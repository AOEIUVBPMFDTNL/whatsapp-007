package com.whatsapp.android.util;

import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * @author sunnoc
 * @date 2019-08-13 9:47
 */
@Slf4j
public class HttpUtils {
    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/69.0.3497.100 Safari/537.36 QIHU 360EE";
    private static final int MILLISECONDS = 40 * 1000;

    public static String postToJson(String url, String json) {
        try (HttpResponse result = HttpRequest.post(url).timeout(MILLISECONDS)
                .header(Header.USER_AGENT, USER_AGENT)
                .header(Header.CONTENT_TYPE, ContentType.JSON.toString())
                .body(json)
                .execute()) {
            String body = result.body();
            if (result.isOk()) {
                return body;
            }
        } catch (Exception ignored) {

        }
        return null;
    }


    public static String get(String url) {
        try (HttpResponse result = HttpRequest
                .get(url)
                .timeout(MILLISECONDS)
                .header(Header.USER_AGENT, USER_AGENT)
                .execute()) {
            String body = result.body();
            if (result.isOk()) {
                return body;
            }
        } catch (Exception ignore) {

        }
        return null;
    }

    public static String get(String url, int timeOut) {
        try (HttpResponse result = HttpRequest
                .get(url)
                .timeout(timeOut)
                .header(Header.USER_AGENT, USER_AGENT)
                .execute()) {
            String body = result.body();
            if (result.isOk()) {
                return body;
            }
        } catch (Exception e) {
            //log.error("异常", e);
        }
        return null;
    }
}