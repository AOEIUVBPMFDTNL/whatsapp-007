package com.whatsapp.android.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpException;
import cn.hutool.http.HttpUtil;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.PutObjectResult;
import com.whatsapp.android.constant.OssConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;

/**
 * @Author Irelia
 * @Date 2023/7/4 12:13
 */
@Service
@Slf4j
public class OssService {
    private OSSClient ossClient;
    @PostConstruct
    public void init() {
        if (StringUtils.isNotBlank(OssConstant.ENDPOINT)) {
            // 创建OSSClient实例。
            log.info("Oss init");
            ossClient = new OSSClient(OssConstant.ENDPOINT, OssConstant.ACCESS_KEY_ID, OssConstant.ACCESS_KEY_SECRET);
        }
    }

    public boolean uploadOssEnvFile(String fileUrl, File file) {
        try {
            PutObjectResult putObjectResult = ossClient.putObject(OssConstant.ENV_BUCKET_NAME, fileUrl, file);
            return StringUtils.isNotBlank(putObjectResult.getETag());
        } catch (Exception ce) {
            log.error("Caught an ClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with OSS, "
                    + "such as not being able to access the network,Error Message:{}", ce.getMessage());
        }
        return false;
    }

    public static void main(String[] args) {
        String localFile = "C:\\YT\\test.txt";
        String envUrl ="https://tk-env.oss-ap-southeast-1.aliyuncs.com/env/6281378532728.db";
        try {
            long l = HttpUtil.downloadFile(envUrl, FileUtil.file(localFile), 60 * 1000);
        }catch (HttpException e){
            if (e.getMessage().contains("404")){
                System.out.println("阿里云不存在");
            }
        }

    }

}
