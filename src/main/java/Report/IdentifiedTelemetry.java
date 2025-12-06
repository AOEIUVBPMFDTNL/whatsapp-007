package Report;

import Env.DeviceEnv;
import Handshake.NoiseHandshake;
import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.entity.ProxyInfo;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.util.HttpRequestUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.UUID;

@Slf4j
public class IdentifiedTelemetry {
    private static final String CONTENT_TYPE = "multipart/form-data; boundary=boundaryWAMpsAAL123xyz";
    private static final String ACCESS_TOKEN = "245118376424571|3e7d275052f1522bf3200afcf53841a7";

    public static String GetUserAgent(DeviceEnv.AndroidEnv env) {
        return String.format("WhatsApp/%d.%d.%d.%d Android/%s Device/%s-%s",
                env.getUserAgent().getAppVersion().getPrimary(),
                env.getUserAgent().getAppVersion().getSecondary(),
                env.getUserAgent().getAppVersion().getTertiary(),
                env.getUserAgent().getAppVersion().getQuaternary(),
                env.getUserAgent().getOsVersion(),
                env.getUserAgent().getManufacturer().replace("-", ""),
                env.getUserAgent().getDevice().replace("-", ""));
    }

    public static byte[] GenerateDataToSend(DeviceEnv.AndroidEnv env) {
        /*57414d0501350002300b02800d0f417375732d415355535f5a30315144502fe08555623879060440052e0188a51313333130302c313737362c323734362c3232363240036202800f05352e312e31286b1848390adf0788b11a02434e488f02c0008875172430336633393433662d386230612d343136652d623334352d333137366338383262646665801109322e32322e332e3738101548b102de0729c00826028875172430336633393433662d386230612d343136652d623334352d33313736633838326264666529840b3202082601
        [87, 65, 77, 5]  [1, 53, 0, 2]
        Record{channel=0, tag=11,	value=2:java.lang.Byte}
        Record{channel=0, tag=13,	value=Asus-ASUS_Z01QD:java.lang.String}
        Record{channel=0, tag=47,	value=1649772000:java.lang.Integer}
        Record{channel=0, tag=1657,	value=4:java.lang.Byte}
        Record{channel=0, tag=5,	value=302:java.lang.Short}
        Record{channel=0, tag=5029,	value=3100,1776,2746,2262:java.lang.String}
        Record{channel=0, tag=3,	value=610:java.lang.Short}
        Record{channel=0, tag=15,	value=5.1.1:java.lang.String}
        Record{channel=0, tag=6251,	value=1:java.lang.Integer}
        Record{channel=0, tag=2617,	value=2015:java.lang.Short}
        Record{channel=0, tag=6833,	value=CN:java.lang.String}
        Record{channel=0, tag=655,	value=192:java.lang.Short}
        Record{channel=0, tag=6005,	value=03f3943f-8b0a-416e-b345-3176c882bdfe:java.lang.String}
        Record{channel=0, tag=17,	value=2.22.3.78:java.lang.String}
        Record{channel=0, tag=21,	value=0:java.lang.Integer}
        Record{channel=0, tag=689,	value=2014:java.lang.Short}
        Record{channel=1, tag=2240,	value=1:java.lang.Integer}
        Record{channel=2, tag=2,	value=1:java.lang.Integer}
        Record{channel=0, tag=6005,	value=03f3943f-8b0a-416e-b345-3176c882bdfe:java.lang.String}
        Record{channel=1, tag=2948,	value=1:java.lang.Integer}
        Record{channel=2, tag=2,	value=8:java.lang.Byte}
        Record{channel=2, tag=1,	value=1:java.lang.Integer}
*/
        WAMOutputstream wam = new WAMOutputstream();
        try {
            wam.waByteArrayOutputStream.write(new byte[]{87, 65, 77, 5});
            wam.waByteArrayOutputStream.write(new byte[]{1, (byte) env.getConnectionLc(), 0, 2});
            wam.serialize(0, 11, (byte) 2);
            wam.serialize(0, 13, env.getUserAgent().getManufacturer() + "-" + env.getUserAgent().getDevice());
            wam.serialize(0, 47, System.currentTimeMillis() / 1000);
            wam.serialize(0, 1657, (byte) 4);
            wam.serialize(0, 5, (short) 302);
            wam.serialize(0, 5029, "3100,1776,2746,2262");
            wam.serialize(0, 3, (short) 610);
            wam.serialize(0, 15, env.getUserAgent().getOsVersion());
            wam.serialize(0, 6251, 1);
            wam.serialize(0, 2617, (short) 2015);
            wam.serialize(0, 6833, env.getUserAgent().getLocaleCountryIso31661Alpha2());
            wam.serialize(0, 655, (short) 192);
            wam.serialize(0, 6005, UUID.randomUUID().toString());
            String waversion = String.format("%d.%d.%d.%d",
                    env.getUserAgent().getAppVersion().getPrimary(),
                    env.getUserAgent().getAppVersion().getSecondary(),
                    env.getUserAgent().getAppVersion().getTertiary(),
                    env.getUserAgent().getAppVersion().getQuaternary());
            wam.serialize(0, 17, waversion);
            wam.serialize(0, 21, 0);
            wam.serialize(0, 689, 2014);
            wam.serialize(1, 2240, 1);
            wam.serialize(1, 2948, 1);
            wam.serialize(2, 2, 8);
            wam.serialize(2, 1, 1);
        } catch (Exception e) {

        }


        ByteBuffer buffer = wam.waByteArrayOutputStream.getByteBuffer();
        byte[] result = new byte[wam.waByteArrayOutputStream.size()];
        System.arraycopy(buffer.array(), 0, result, 0, result.length);
        return result;
    }


    public static byte[] GenerateDataToSend2(DeviceEnv.AndroidEnv env) {
        /*
    * [87, 65, 77, 5] [1, 2, 0, 2]
    *
    * Record{channel=0, tag=11,	value=2:java.lang.Byte}
    Record{channel=0, tag=13,	value=Asus-ASUS_Z01QD:java.lang.String}
    Record{channel=0, tag=47,	value=1649768400:java.lang.Integer}
    Record{channel=0, tag=1657,	value=4:java.lang.Byte}
    Record{channel=0, tag=5,	value=302:java.lang.Short}
    Record{channel=0, tag=5029,	value=3100,1776,2746,2262:java.lang.String}
    Record{channel=0, tag=3,	value=610:java.lang.Short}
    Record{channel=0, tag=15,	value=5.1.1:java.lang.String}
    Record{channel=0, tag=6251,	value=1:java.lang.Integer}
    Record{channel=0, tag=2617,	value=2015:java.lang.Short}
    Record{channel=0, tag=6833,	value=CN:java.lang.String}
    Record{channel=0, tag=655,	value=192:java.lang.Short}
    Record{channel=0, tag=17,	value=2.22.3.78:java.lang.String}
    Record{channel=0, tag=21,	value=0:java.lang.Integer}
    Record{channel=0, tag=689,	value=2014:java.lang.Short}
    Record{channel=0, tag=6005,	value=96ad1afc-bb94-4a94-919d-a8cbb887f64c:java.lang.String}
    Record{channel=1, tag=2958,	value=1:java.lang.Integer}
    Record{channel=2, tag=1,	value=1:java.lang.Integer}
    Record{channel=2, tag=2,	value=1:java.lang.Integer}
    * */
        WAMOutputstream wam = new WAMOutputstream();
        try {
            wam.waByteArrayOutputStream.write(new byte[]{87, 65, 77, 5});
            wam.waByteArrayOutputStream.write(new byte[]{1, (byte) (env.getConnectionLc() + 10), 0, 2});
            wam.serialize(0, 11, (byte) 2);
            wam.serialize(0, 13, env.getUserAgent().getManufacturer() + "-" + env.getUserAgent().getDevice());
            wam.serialize(0, 47, System.currentTimeMillis() / 1000);
            wam.serialize(0, 1657, (byte) 4);
            wam.serialize(0, 5, (short) 302);
            wam.serialize(0, 5029, "3100,1776,2746,2262");
            wam.serialize(0, 3, (short) 610);
            wam.serialize(0, 15, env.getUserAgent().getOsVersion());
            wam.serialize(0, 6251, 1);
            wam.serialize(0, 2617, (short) 2015);
            wam.serialize(0, 6833, env.getUserAgent().getLocaleCountryIso31661Alpha2());
            wam.serialize(0, 655, (short) 192);
            wam.serialize(0, 6005, UUID.randomUUID().toString());
            String waversion = String.format("%d.%d.%d.%d",
                    env.getUserAgent().getAppVersion().getPrimary(),
                    env.getUserAgent().getAppVersion().getSecondary(),
                    env.getUserAgent().getAppVersion().getTertiary(),
                    env.getUserAgent().getAppVersion().getQuaternary());
            wam.serialize(0, 17, waversion);
            wam.serialize(0, 21, 0);
            wam.serialize(0, 689, 2014);
            wam.serialize(1, 2240, 1);
            wam.serialize(1, 2958, 1);
            wam.serialize(2, 1, 1);
            wam.serialize(2, 2, 1);
        } catch (Exception e) {

        }


        ByteBuffer buffer = wam.waByteArrayOutputStream.getByteBuffer();
        byte[] result = new byte[wam.waByteArrayOutputStream.size()];
        System.arraycopy(buffer.array(), 0, result, 0, result.length);
        return result;
    }

    public static void Report(DeviceEnv.AndroidEnv env, NoiseHandshake.Proxy proxy_) {
        ProxyInfo proxyInfo = null;
        if (proxy_ != null) {
            proxyInfo = new ProxyInfo();
            proxyInfo.setType(proxy_.type);
            proxyInfo.setProxyHost(proxy_.server);
            proxyInfo.setProxyPort(proxy_.port);
            proxyInfo.setProxyUser(proxy_.userName);
            proxyInfo.setProxyPwd(proxy_.password);

        }
        Report(env, GenerateDataToSend(env), proxyInfo);
        Report(env, GenerateDataToSend2(env), proxyInfo);
    }


    public static String Report(DeviceEnv.AndroidEnv env, byte[] data, ProxyInfo proxyInfo) {
        String username = env.getFullphone();
        try {
            String url = "https://dit.whatsapp.net/deidentified_telemetry";
            Mac instance = Mac.getInstance("HmacSHA256");
            instance.init(new SecretKeySpec(cn.hutool.core.codec.Base64.decode(env.getSharedSecret()), instance.getAlgorithm()));
            byte[] doFinal = instance.doFinal(data);
            String credit = env.getOrignalToken() + "+" + Base64.getUrlEncoder().encodeToString(doFinal);
            HttpRequestUtil httpClient = HttpRequestUtil.getHttpClient(proxyInfo);
            HttpPost request = new HttpPost(url);
            request.setHeader("User-Agent", GetUserAgent(env));
            request.setHeader("Connection", "Keep-Alive");
            request.setHeader("Accept", "text/plain");
            request.setHeader("Accept-Charset", "UTF-8");
            request.setConfig(httpClient.getDefaultConfig());
            ByteArrayBody byteArrayBody = new ByteArrayBody(data, "WAMEventBuffer.dat");
            StringBody accessTokenData = new StringBody(ACCESS_TOKEN, ContentType.create(
                    "text/plain", Consts.UTF_8));
            StringBody credentialData = new StringBody(credit, ContentType.create(
                    "text/plain", Consts.UTF_8));
            JSONObject timestamp = new JSONObject();
            timestamp.put("t", System.currentTimeMillis() / 1000);
            StringBody metaData = new StringBody(timestamp.toJSONString(), ContentType.create(
                    "application/json", Consts.UTF_8));
            HttpEntity httpEntity = MultipartEntityBuilder.create()
                    .setCharset(Consts.UTF_8)
                    .setContentType(ContentType.parse(CONTENT_TYPE))
                    .setMode(HttpMultipartMode.STRICT)
                    .addPart("access_token", accessTokenData)
                    .addPart("credential", credentialData)
                    .addPart("message", byteArrayBody)
                    .addPart("meta_data", metaData)
                    .build();
            request.setEntity(httpEntity);
            request.setHeader("Content-Type", CONTENT_TYPE);
            StatusResult statusResult = HttpRequestUtil.executeRequest(httpClient, request);
            log.info("用户：{}，上报返回结果：{}", username, statusResult.getMessage());
        } catch (Exception e) {
            log.error("用户：{}，上报异常", username, e);
        }
        return "report: unknown";
    }
}
