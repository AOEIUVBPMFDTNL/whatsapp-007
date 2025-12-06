package com.whatsapp.android;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * @author sunnoc
 * @date 2023-03-06 14:10
 */
@Slf4j
public class AddTerminalAllocationLimitTest {
    public static void main(String[] args) {
        List<String> ips = Arrays.asList("45.77.169.141", "207.148.77.20", "45.76.146.241", "45.32.112.99", "66.42.58.213", "45.77.36.29", "149.28.159.18", "45.76.184.143", "45.77.45.164", "139.180.190.173", "139.180.137.195", "45.76.159.227", "149.28.151.224", "139.180.146.221", "45.32.122.164", "45.32.104.107", "45.76.151.143", "139.180.211.27", "45.32.113.27", "139.180.139.172", "139.180.155.209", "207.148.67.74", "45.77.240.62", "45.76.187.154", "207.148.76.5", "139.180.214.240", "139.180.140.93", "45.32.127.237", "66.42.62.99", "45.32.106.210", "149.28.154.140", "66.42.49.191", "139.180.215.16", "45.77.47.11", "45.76.182.195", "139.180.189.42", "139.180.187.163", "45.77.36.146", "207.148.121.251", "45.32.105.129", "149.28.156.165", "139.180.191.59", "45.76.146.60", "139.180.191.74", "149.28.147.112", "139.180.129.108", "139.180.218.228", "207.148.76.154", "207.148.126.45", "149.28.142.191", "139.180.188.191", "45.77.255.14", "45.32.110.89", "139.180.219.151", "45.32.99.252", "207.148.121.247", "45.32.118.20", "45.77.44.47", "139.180.145.196", "45.32.108.82", "149.28.152.102", "139.180.215.86", "45.76.163.131", "45.77.43.214", "207.148.74.106", "45.32.101.70", "207.148.127.205", "45.77.43.250", "139.180.129.187", "207.148.73.18", "139.180.140.120", "45.32.126.97", "207.148.127.241", "139.180.136.189", "149.28.130.52", "45.32.102.236", "139.180.152.31", "207.148.116.214", "45.32.110.124");
        for (String ip : ips) {
            try {
                if (!limit(ip)) {
                    log.info("执行失败：{}", ip);
                    FileUtil.appendString(ip + "\n", "/Users/sunnoc/IdeaProjects/whatsapp-android/src/test/java/com/whatsapp/android/fail.txt", StandardCharsets.UTF_8);
                } else {
                    log.info("执行成功：{}", ip);
                }
            } catch (Exception e) {
                log.info("执行失败：{}", ip);

                FileUtil.appendString(ip, "/Users/sunnoc/IdeaProjects/whatsapp-android/src/test/java/com/whatsapp/android/fail.txt", StandardCharsets.UTF_8);
            }
        }
        log.info("执行完毕");

    }

    private static boolean limit(String ip) {
        String url = "http://8.214.160.42:83/task/terminal/record/addTerminalAllocationLimit?ip=" + ip;
        String body = HttpUtil.createPost(url).timeout(5 * 1000).execute().body();
        JSONObject jsonObject = JSONObject.parseObject(body);
        int code = jsonObject.getIntValue("code");
        if (code == 200) {
            return true;
        }
        return false;
    }
}
