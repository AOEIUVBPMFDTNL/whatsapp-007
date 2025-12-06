package com.whatsapp.android;

import org.json.JSONException;
import org.json.JSONObject;

import cn.hutool.core.util.ZipUtil;
import cn.hutool.crypto.digest.DigestAlgorithm;
import cn.hutool.crypto.digest.Digester;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;

import java.nio.charset.StandardCharsets;

public class TestWSAPIHttp {

    public static final String KEY = "9ba988be8416b5c51ebe974f0699704d";

    public static void main(String[] args) throws Exception {
        /*JSONObject request = new JSONObject();
        request.put("mid", "10090");
//    request.put("route", "GetPhoneExists");
        request.put("route", "scanNumber");
        JSONObject body = new JSONObject();
        request.put("body", body);
        body.put("wid", 86);
        body.put("toWid", "8615228092350");
        byte[] data = request.toString().getBytes();*/
        byte[] data = loginPack();
        //先加密
        data = XXTEA.encrypt(data, KEY.getBytes(StandardCharsets.UTF_8));
        //压缩数据
        data = ZipUtil.gzip(data);
        String sign = new Digester(DigestAlgorithm.MD5).digestHex(data);
        String url = "http://13.213.215.106:1688/wsapi/rv?mid=10090&action=wsapi&sign=" + sign;

        HttpResponse response = HttpUtil.createPost(url)
                .body(data)
                .contentType("application/json")
                .execute();
        System.out.println("code: " + response.getStatus());
        if (200 == response.getStatus()) {
            byte[] result = response.bodyBytes();
            result = ZipUtil.unGzip(result);
            result = XXTEA.decrypt(result, KEY.getBytes(StandardCharsets.UTF_8));
            System.out.println(new String(result));
        }
    }

    public static byte[] loginPack() throws JSONException {
        JSONObject request = new JSONObject();
        JSONObject token = new JSONObject();
        request.put("mid", "10090");
        //登录
        request.put("route", "WALogin");
        JSONObject body = new JSONObject();
        body.put("mcc", "250");
        body.put("mnc", "01");
        //Token数据部分
        token.put("pn", "910826");
        token.put("routing_info", "CAwIDQ");
        token.put("registration_jid", "992917109901");
        //真机需要提供me,平台上注册则无需提供
        //token.put("me", "base64(me:bytes,11)");
        //如果是通过本平台下发短信注册，无需提供wa_version
        token.put("wa_version", "2.22.5.71");
        token.put("manufacturer", "Fly");    //Build.MANUFACTURER
        token.put("phone_model", "FS407");//Build.MODEL
        token.put("display", "SW13_FLY_FS407_2017_03_21");        //Build.DISPLAY
        token.put("version", "6.0");           //Build.VERSION.RELEASE
        token.put("phoneid_id", "fb12bf7a-b995-4295-81b9-22a9bb4aebfe");
        token.put("language", "ru");
        token.put("region", "RU");
        //根据情况选择如下：
        //(1)如果是在平台注册，则无需提供keystore值
        //(2)如果在真机上注册，则需要提供keystore值，生成规则安卓Base64.encode(bytes:"keystore.xml",11)
        token.put("keystore", "PD94bWwgdmVyc2lvbj0nMS4wJyBlbmNvZGluZz0ndXRmLTgnIHN0YW5kYWxvbmU9J3llcycgPz4KPG1hcD4KICAgIDxsb25nIG5hbWU9ImNsaWVudF9zdGF0aWNfa2V5cGFpcl9lbmNfc3VjY2VzcyIgdmFsdWU9IjEwMyIgLz4KICAgIDxzdHJpbmcgbmFtZT0iY2xpZW50X3N0YXRpY19rZXlwYWlyX2VuYyI-WzAsJnF1b3Q7OGFNSmxsVkwrU0dqVTFDaUJHdDVUTnJwYklsTmJiUTZaRmgrdUJQUnZ3TGoxWDhzRkpkblphd2pFUFhlbUVTXC8wc2pmTUYwdzFYOWlpQmdvMm9VdnhwcStSTWh3K2sxaWRqcEN3UUlOTkhvJnF1b3Q7LCZxdW90O056dDVZUXk5XC9xMm56UDNvJnF1b3Q7XTwvc3RyaW5nPgogICAgPGludCBuYW1lPSJjbGllbnRfYW5kcm9pZF9rZXlfc3RvcmVfYXV0aF92ZXIiIHZhbHVlPSIxIiAvPgogICAgPHN0cmluZyBuYW1lPSJzZXJ2ZXJfc3RhdGljX3B1YmxpYyI-eERuNk1xQlBuM082cHREaFBRdC90cWNYcnYyZEs3YVIvL05RTEZJVmFsMDwvc3RyaW5nPgogICAgPHN0cmluZyBuYW1lPSJjbGllbnRfc3RhdGljX2tleXBhaXJfcHdkX2VuYyI-WzIsJnF1b3Q7V2lGS1JyQ1VOZlBIVG1GTnRxOTVNWks1a0xsZUVBR01DUmlaNVIwbFJ4d2IzeGhLUlZwY0hyUkFhUWtleGVrVG9HS1wvWUVFeGo0TFwvcHFKOG56anBhQSZxdW90OywmcXVvdDtGbEs5ZlwvUU9jQTRWVUo3d1ZORFV1USZxdW90OywmcXVvdDs1XC95ZXdnJnF1b3Q7LCZxdW90O3VpZmpSU0lRSndEUzJYM09MaFwvdnZRJnF1b3Q7XTwvc3RyaW5nPgogICAgPGJvb2xlYW4gbmFtZT0iY2FuX3VzZXJfYW5kcm9pZF9rZXlfc3RvcmUiIHZhbHVlPSJ0cnVlIiAvPgo8L21hcD4K");
        //根据情况选择如下
        //(1)如果是在平台注册，则提供平台返回的StaticPriKey
        //(2)如果是在真机上，则查询数据库
        // select hex(public_key),hex(private_key) from identities where recipient_id=-1;
        // 把16进制hex(public_key)的值转化成bytes,然后用安卓Base64(bytes,11),同样hex(private_key)进行类型转化
        token.put("privateKey", "oB6aaBfxm+6JkND2Vja640F9saUxXGFThekps/vXPFc=");
        token.put("publicKey", "BQfxsD93AIIr7uDxAIGxhQvsPinenC8OKN8cW2MjGiVI");
        token.put("android_id", "794e8628d1c7016e");
        body.put("token", token);
        request.put("body", body);
        System.out.println(request.toString());
        return request.toString().getBytes();
    }
}