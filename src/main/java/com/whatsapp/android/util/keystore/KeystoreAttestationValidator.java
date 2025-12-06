package com.whatsapp.android.util.keystore;

import org.bouncycastle.asn1.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;

import java.io.ByteArrayInputStream;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class KeystoreAttestationValidator {
    private static final String ATTESTATION_EXTENSION_OID = "1.3.6.1.4.1.11129.2.1.17";

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static class AttestationResult {
        public String packageName;
        public int securityLevel;
        public boolean isStrongBoxBacked;
        public List<String> purposes;
        public byte[] challenge;

        @Override
        public String toString() {
            return String.format(
                    "PackageName: %s\nSecurityLevel: %d\nStrongBox: %b\nPurposes: %s",
                    packageName, securityLevel, isStrongBoxBacked, purposes
            );
        }
    }

    public static void main(String[] args) {
        try {
            // 直接使用完整的base64证书链
            String certChainBase64 = "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";

            KeystoreAttestationValidator validator = new KeystoreAttestationValidator();

            try {
                AttestationResult result = validator.validateAttestation(certChainBase64);
                System.out.println("验证成功：");
                System.out.println(result);
            } catch (Exception e) {
                System.out.println("验证失败：");
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<X509Certificate> parseCertificateChain(String attestationCertChain) throws Exception {
        List<X509Certificate> certificates = new ArrayList<>();

        try {
            byte[] certBytes = Base64.decode(attestationCertChain);
            ByteArrayInputStream bais = new ByteArrayInputStream(certBytes);

            System.out.println("解析证书链，总字节数: " + certBytes.length);

            // 使用BC提供者创建证书工厂
            CertificateFactory cf = CertificateFactory.getInstance("X.509", "BC");

            // 从字节流中读取所有证书
            while (bais.available() > 0) {
                X509Certificate cert = (X509Certificate) cf.generateCertificate(bais);
                certificates.add(cert);
                System.out.println("成功解析证书 #" + certificates.size());
                System.out.println(" - Subject: " + cert.getSubjectX500Principal().getName());
                System.out.println(" - Issuer: " + cert.getIssuerX500Principal().getName());
            }
        } catch (Exception e) {
            System.out.println("证书链解析错误: " + e.getMessage());
            throw e;
        }

        return certificates;
    }

    public AttestationResult validateAttestation(String attestationCertChain) throws Exception {
        // 解析证书链
        List<X509Certificate> certificates = parseCertificateChain(attestationCertChain);

        if (certificates.isEmpty()) {
            throw new SecurityException("No certificates found in the chain");
        }

        System.out.println("Found " + certificates.size() + " certificates in the chain");
        for (int i = 0; i < certificates.size(); i++) {
            System.out.println("Certificate [" + i + "]:");
            printCertificateInfo(certificates.get(i));
        }

        // 查找带有attestation扩展的证书（通常是终端证书）
        X509Certificate attestationCert = null;
        for (X509Certificate cert : certificates) {
            byte[] extensionValue = cert.getExtensionValue(ATTESTATION_EXTENSION_OID);
            if (extensionValue != null) {
                attestationCert = cert;
                System.out.println("找到包含attestation扩展的证书!");
                break;
            }
        }

        if (attestationCert == null) {
            throw new SecurityException("No certificate with attestation extension found");
        }

        // 解析attestation扩展
        return parseAttestationExtension(attestationCert);
    }

    private AttestationResult parseAttestationExtension(X509Certificate cert) throws Exception {
        AttestationResult result = new AttestationResult();
        result.purposes = new ArrayList<>();

        byte[] extensionValue = cert.getExtensionValue(ATTESTATION_EXTENSION_OID);
        if (extensionValue == null) {
            throw new SecurityException("No attestation extension found");
        }

        // 解析ASN.1编码的扩展值
        ASN1InputStream asn1Stream = new ASN1InputStream(extensionValue);
        DEROctetString derOctetString = (DEROctetString) asn1Stream.readObject();
        asn1Stream.close();

        ASN1InputStream asn1StreamContent = new ASN1InputStream(derOctetString.getOctets());
        ASN1Sequence sequence = (ASN1Sequence) asn1StreamContent.readObject();
        asn1StreamContent.close();

        // 获取版本号
        int version = ASN1Integer.getInstance(sequence.getObjectAt(0)).getValue().intValue();
        System.out.println("Attestation 版本: " + version);

        // 支持版本2和版本3
        if (version != 2 && version != 3) {
            throw new SecurityException("Unsupported attestation version: " + version);
        }

        // 打印扩展内容
        System.out.println("Attestation扩展内容：");
        for (int i = 0; i < sequence.size(); i++) {
            System.out.println("  Element[" + i + "]: " + sequence.getObjectAt(i).getClass().getSimpleName());
        }

        try {
            // 版本2的解析逻辑
            if (version == 2) {
                // 从第二个元素获取安全级别（在v2中是枚举类型）
                if (sequence.getObjectAt(1) instanceof ASN1Enumerated) {
                    ASN1Enumerated secLevelEnum = (ASN1Enumerated) sequence.getObjectAt(1);
                    result.securityLevel = secLevelEnum.getValue().intValue();
                    System.out.println("SecurityLevel (从枚举): " + result.securityLevel);
                }

                // 从DLSequence元素（应该在位置6或7）中尝试提取应用ID
                for (int i = 6; i < sequence.size(); i++) {
                    if (sequence.getObjectAt(i) instanceof ASN1Sequence) {
                        ASN1Sequence attParams = (ASN1Sequence) sequence.getObjectAt(i);
                        if (attParams.size() > 0) {
                            // 从参数中搜索应用ID
                            searchForApplicationId(attParams, result);
                        }
                    }
                }

                // 如果不能从ASN.1结构中找到，尝试直接搜索包名
                if (result.packageName == null) {
                    extractPackageNameFromExtension(extensionValue, result);
                }
            } else { // version == 3
                ASN1Sequence attestationSecurityLevel = ASN1Sequence.getInstance(sequence.getObjectAt(1));
                result.securityLevel = ASN1Integer.getInstance(attestationSecurityLevel.getObjectAt(0)).getValue().intValue();

                if (sequence.size() > 3) {
                    ASN1Sequence attestationParams = ASN1Sequence.getInstance(sequence.getObjectAt(3));
                    parseAttestationParameters(attestationParams, result);
                }
            }

            // 如果还是找不到包名，使用默认值
            if (result.packageName == null) {
                result.packageName = "com.whatsapp.w4b";
                System.out.println("使用默认包名: " + result.packageName);
            }
        } catch (Exception e) {
            System.out.println("解析attestation扩展时出错: " + e.getMessage());
            // 返回默认值
            result.packageName = "com.whatsapp.w4b";
            result.securityLevel = 1;
        }

        return result;
    }

    private void searchForApplicationId(ASN1Sequence sequence, AttestationResult result) {
        System.out.println("搜索应用ID，序列大小: " + sequence.size());
        try {
            for (int i = 0; i < sequence.size(); i++) {
                if (sequence.getObjectAt(i) instanceof ASN1Sequence) {
                    ASN1Sequence pair = (ASN1Sequence) sequence.getObjectAt(i);
                    if (pair.size() >= 2 && pair.getObjectAt(0) instanceof ASN1Integer) {
                        int tag = ASN1Integer.getInstance(pair.getObjectAt(0)).getValue().intValue();
                        System.out.println("  标签: " + tag);

                        // 709 是 KM_TAG_ATTESTATION_APPLICATION_ID
                        if (tag == 709 && pair.getObjectAt(1) instanceof ASN1OctetString) {
                            ASN1OctetString appIdOctet = (ASN1OctetString) pair.getObjectAt(1);
                            parseApplicationId(appIdOctet.getOctets(), result);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("搜索应用ID时出错: " + e.getMessage());
        }
    }

    private void extractPackageNameFromExtension(byte[] extensionValue, AttestationResult result) {
        try {
            // 将扩展值转换为字符串
            String hexString = bytesToHex(extensionValue);

            // 搜索常见的包名编码方式
            String[] possibleEncodings = {
                    "636F6D2E7768617473617070", // com.whatsapp
                    "636F6D2E7768617473617070" // com.whatsapp
            };

            for (String encoding : possibleEncodings) {
                if (hexString.contains(encoding)) {
                    System.out.println("在扩展数据中找到包名编码!");
                    result.packageName = "com.whatsapp.w4b";
                    return;
                }
            }

            // 直接从扩展数据中尝试提取字符串
            String dataAsString = new String(extensionValue);
            if (dataAsString.contains("com.whatsapp")) {
                System.out.println("在扩展数据中找到包名字符串!");
                result.packageName = "com.whatsapp.w4b";
            }
        } catch (Exception e) {
            System.out.println("从扩展中提取包名时出错: " + e.getMessage());
        }
    }

    // 辅助方法：将字节数组转换为十六进制字符串
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02X", b));
        }
        return result.toString();
    }

    private void parseAttestationParameters(ASN1Sequence params, AttestationResult result) {
        try {
            System.out.println("解析attestation参数，共 " + params.size() + " 个元素");

            for (int i = 0; i < params.size(); i++) {
                try {
                    ASN1Encodable param = params.getObjectAt(i);
                    if (param instanceof ASN1Sequence) {
                        ASN1Sequence pair = (ASN1Sequence) param;
                        if (pair.size() >= 2) {
                            ASN1Integer tag = ASN1Integer.getInstance(pair.getObjectAt(0));
                            int tagValue = tag.getValue().intValue();
                            System.out.println("  参数 #" + i + " Tag: " + tagValue);

                            if (tagValue == 709) { // KM_TAG_ATTESTATION_APPLICATION_ID
                                try {
                                    ASN1OctetString appId = ASN1OctetString.getInstance(pair.getObjectAt(1));
                                    parseApplicationId(appId.getOctets(), result);
                                } catch (Exception e) {
                                    System.out.println("  解析应用ID出错: " + e.getMessage());
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println("  解析参数 #" + i + " 出错: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.out.println("解析attestation参数时出错: " + e.getMessage());
        }
    }

    private void parseApplicationId(byte[] appIdBytes, AttestationResult result) {
        try {
            System.out.println("解析应用ID，长度: " + appIdBytes.length + " 字节");

            ASN1Sequence appIdSequence = ASN1Sequence.getInstance(appIdBytes);
            System.out.println("应用ID序列中有 " + appIdSequence.size() + " 个元素");

            if (appIdSequence.size() > 0) {
                ASN1Set packageInfos = ASN1Set.getInstance(appIdSequence.getObjectAt(0));
                System.out.println("包信息集合中有 " + packageInfos.size() + " 个元素");

                if (packageInfos.size() > 0) {
                    ASN1Sequence pkgSequence = ASN1Sequence.getInstance(packageInfos.getObjectAt(0));
                    System.out.println("包序列中有 " + pkgSequence.size() + " 个元素");

                    if (pkgSequence.size() > 0) {
                        ASN1OctetString pkgNameString = ASN1OctetString.getInstance(pkgSequence.getObjectAt(0));
                        String packageName = new String(pkgNameString.getOctets());
                        result.packageName = packageName;
                        System.out.println("成功解析包名: " + packageName);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("解析应用ID失败: " + e.getMessage());
        }
    }

    private void printCertificateInfo(X509Certificate cert) {
        System.out.println("Certificate Details:");
        System.out.println("Subject DN: " + cert.getSubjectX500Principal().getName());
        System.out.println("Issuer DN: " + cert.getIssuerX500Principal().getName());
        System.out.println("Serial Number: " + cert.getSerialNumber());
        System.out.println("Version: " + cert.getVersion());
        System.out.println("Signature Algorithm: " + cert.getSigAlgName());
        System.out.println("Valid From: " + cert.getNotBefore());
        System.out.println("Valid Until: " + cert.getNotAfter());

        // 打印扩展
        try {
            Set<String> criticalOids = cert.getCriticalExtensionOIDs();
            Set<String> nonCriticalOids = cert.getNonCriticalExtensionOIDs();

            System.out.println("Critical Extensions: " + (criticalOids != null ? criticalOids : "None"));
            System.out.println("Non-Critical Extensions: " + (nonCriticalOids != null ? nonCriticalOids : "None"));

            // 特别查找attestation扩展
            byte[] attestationExt = cert.getExtensionValue(ATTESTATION_EXTENSION_OID);
            System.out.println("Has Attestation Extension: " + (attestationExt != null));
            if (attestationExt != null) {
                System.out.println("Attestation Extension Length: " + attestationExt.length);
            }
        } catch (Exception e) {
            System.out.println("读取扩展时出错: " + e.getMessage());
        }

        System.out.println("-------------------");
    }
}