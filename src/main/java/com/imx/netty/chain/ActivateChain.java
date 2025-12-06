package com.imx.netty.chain;

import com.alibaba.fastjson.JSON;
import com.imx.apns.activate.ActivateTransport;
import com.imx.apns.activate.ActivationInfo;
import com.imx.apns.activate.ActivationRequest;
import com.imx.apns.common.APNsState;
import com.imx.apns.gen.PlistXMLGenerator;
import com.imx.apns.gen.RSAPrivateKeyGenerator;
import com.imx.common.Chain;
import com.imx.common.ChainExecution;
import com.imx.common.Pair;
import com.imx.common.util.CertificateUtil;
import com.imx.common.util.PatternUtil;
import com.imx.common.util.SignatureHelper;
import com.imx.common.util.StringUtil;
import com.imx.netty.ssl.SslContextProvider;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.stream.Collectors;

import static com.imx.apns.common.Constants.PLIST_XML_CERTIFICATE_PATTERN;


@Slf4j
public class ActivateChain extends Chain<ChainContext> implements ChainExecution<ChainContext> {
    private static final int MAX_ACTIVATION_RETRIES = 3;

    @Override
    public void process(ChainContext chainContext) {
        try {
            execute(chainContext);
        } catch (Exception e) {
            if (e instanceof NoSuchElementException) {
                log.error("Failed to no find certificate.");
            } else {
                log.error("Failed to execute activate chain.", e);
            }
            return;
        }
        if (nextChain == null) {
            return;
        }
        nextChain.process(chainContext);
    }

    @Override
    public void execute(@NonNull ChainContext chainContext) throws Exception {
        if (Objects.nonNull(chainContext.getApNsState())) {
            return;
        }
        ActivationInfo activationInfo = chainContext.getActivationInfo();
        String jsonString = JSON.toJSONString(activationInfo);
        Map<String, String> data = JSON.parseObject(jsonString, Map.class);
        data = data.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> StringUtil.capitalizeFirstLetter(entry.getKey()),
                        Map.Entry::getValue
                ));
        String deviceCertRequest = new String(activationInfo.getDeviceCertRequest());
        data.put("DeviceCertRequest", deviceCertRequest);

        PrivateKey privateKeyFromPEM = RSAPrivateKeyGenerator.getPrivateKeyFromPEM();
        String plistXML = PlistXMLGenerator.generate(data);
        byte[] signBytes = SignatureHelper.sign(privateKeyFromPEM, plistXML.getBytes());

        ActivationRequest activationRequest = new ActivationRequest()
                .setActivationInfoXML(plistXML.getBytes())
                .setFairPlayCertChain(SslContextProvider.FAIRPLAY_CERT).setFairPlaySignature(signBytes)
                .setDevice(activationInfo.getDeviceClass());
        String response = callActivationServiceWithRetry(activationRequest);
        Optional<String> certificate = PatternUtil.getData(response, PLIST_XML_CERTIFICATE_PATTERN);
        if (!certificate.isPresent()) {
            throw new NoSuchElementException("No value present");
        }
        byte[] decode = Base64.getDecoder().decode(certificate.get());
        X509Certificate x509Certificate = CertificateUtil.parse(decode);

        Pair<byte[], byte[]> pair = new Pair<>(chainContext.getKeyPair().getPrivate().getEncoded(), x509Certificate.getEncoded());
        APNsState apnsState = new APNsState();
        apnsState.setTopics(chainContext.getTopics());
        apnsState.setPair(pair);
        chainContext.setApNsState(apnsState);

    }

    private String callActivationServiceWithRetry(ActivationRequest activationRequest) {
        for (int i = 0; i < MAX_ACTIVATION_RETRIES; i++) {
            try {
                String response = ActivateTransport.callOnDeviceActivation(activationRequest);
                if (StringUtils.hasLength(response)) {
                    return response;
                }
            } catch (Exception e) {
                log.warn("APNS Activation attempt {}/{} failed.", i + 1, MAX_ACTIVATION_RETRIES, e);
            }
        }
        return null;
    }
}
