package com.whatsapp.android.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.region.Region;
import org.springframework.util.StringUtils;

import java.io.File;

/**
 * cos客户端上传
 *
 * @author sunnoc
 * @date 2021-03-16 11:55
 */
public class CosUploadUtil {
    public static CosUploadUtil cosUpload = new CosUploadUtil();
    private COSCredentials cred;
    private ClientConfig clientConfig;
    private String sessionToken;
    /**
     * bucket 名需包含 appid
     */
    private final String bucketName = "nanshan-whatsapp-1257892306";
    /**
     * 地区名
     */
    private final String regionName = "ap-singapore";

    /**
     * 上传whatsApp环境
     *
     * @param key       文件名称
     * @param localFile 上传文件
     * @return boolean
     */
    public boolean uploadCosEnvFile(String key, File localFile) {
        if (cred == null || clientConfig == null || StringUtils.isEmpty(sessionToken)) {
            return false;
        }
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, localFile);
        // 设置 x-cos-security-token header 字段
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setSecurityToken(sessionToken);
        putObjectRequest.setMetadata(objectMetadata);
        COSClient cosclient = new COSClient(cred, clientConfig);
        try {
            // 3 生成 cos 客户端
            PutObjectResult putObjectResult = cosclient.putObject(putObjectRequest);
            // 成功：putobjectResult 会返回文件的 etag
            String etag = putObjectResult.getETag();
            //String url = "https://" + bucketName + ".cos." + regionName + ".myqcloud.com/env/" + key;
            return StringUtils.hasLength(etag);
        } catch (Throwable ignored) {
        } finally {
            // 关闭客户端
            cosclient.shutdown();
        }
        return false;
    }

    /**
     * 校验对象是否存在
     *
     * @param key 文件名称
     * @return boolean
     */
    public boolean doesObjectExist(String key) {
        String secretId = "AKIDrZ3IHJu2Xlv2vYiBTWJqEfssd6RahBm8";
        String secretKey = "IK4aR3asufA5l47uRknmX4IF0Xsxv2uW";
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        // 2 设置 bucket 的地域, COS 地域的简称请参照 https://cloud.tencent.com/document/product/436/6224
        // clientConfig 中包含了设置 region, https(默认 http), 超时, 代理等 set 方法, 使用可参见源码或者常见问题 Java SDK 部分。
        Region region = new Region(regionName);
        ClientConfig clientConfig = new ClientConfig(region);
        // 这里建议设置使用 https 协议
        clientConfig.setHttpProtocol(HttpProtocol.https);
        // 3 生成 cos 客户端。
        COSClient cosclient = new COSClient(cred, clientConfig);
        try {
            // 3 生成 cos 客户端
            boolean b = cosclient.doesObjectExist(bucketName, key);
            System.out.println(b);
            return b;
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            // 关闭客户端
            cosclient.shutdown();
        }
        return false;
    }

    public boolean uploadGroupMsgFile(String key, String msgId, byte[] localFile, boolean del) {
        if (cred == null || clientConfig == null || StringUtils.isEmpty(sessionToken)) {
            return false;
        }
        String groupMsgDir = System.getProperty("user.dir") + "/out/groupMsg/";
        File file = FileUtil.writeBytes(localFile, groupMsgDir + msgId);
        try {
            return uploadCosEnvFile(key, file);
        } catch (Exception e) {
            return false;
        } finally {
            if (del) {
                FileUtil.del(file);
            }
        }
    }

    /**
     * 下载群消息
     *
     * @param msgId 消息id
     * @return 群消息
     */
    public String downloadCosGroupMsg(String msgId) {
        String groupMsgDir = System.getProperty("user.dir") + "/out/groupMsg/";
        String filePath = groupMsgDir + msgId;
        if (FileUtil.exist(filePath)) {
            return FileUtil.readString(filePath, CharsetUtil.UTF_8);
        }
        try (HttpResponse result = HttpRequest
                .get("https://nanshan-whatsapp-1257892306.cos.ap-singapore.myqcloud.com/env/groupMsg/" + msgId)
                .timeout(30 * 1000)
                .execute();) {
            if (result.isOk()) {
                String body = result.body();
                FileUtil.writeString(body, filePath, CharsetUtil.UTF_8);
                return body;
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 创建cos配置
     *
     * @param url 获取临时密钥地址
     * @return boolean
     */
    public boolean createCosConfig(String url) {
        try {
            String content = HttpUtil.get(url);
            JSONObject jsonObject = JSONObject.parseObject(content);
            int code = jsonObject.getIntValue("code");
            if (code == 200) {
                // 用户基本信息
                String tmpSecretId = jsonObject.getJSONObject("data").getJSONObject("credentials").getString("tmpSecretId");
                String tmpSecretKey = jsonObject.getJSONObject("data").getJSONObject("credentials").getString("tmpSecretKey");
                // 替换为 STS 接口返回给您的临时 Token
                sessionToken = jsonObject.getJSONObject("data").getJSONObject("credentials").getString("sessionToken");
                // 1 初始化用户身份信息(secretId, secretKey)
                cred = new BasicCOSCredentials(tmpSecretId, tmpSecretKey);
                // 2 设置 bucket 区域,详情请参阅 COS 地域 https://cloud.tencent.com/document/product/436/6224
                clientConfig = new ClientConfig(new Region(regionName));
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

}
