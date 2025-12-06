package com.whatsapp.android.util.keystore;

import org.bouncycastle.asn1.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.util.Calendar;
import java.util.Date;
import java.util.Vector;

import sun.security.util.DerOutputStream;
import sun.security.util.ObjectIdentifier;
import sun.security.x509.*;

/**
 * 一个使用Java原生API创建Keystore Attestation证书的工具类
 * 尽可能模拟真实的Android Keystore Attestation证书链结构
 */
public class SimpleAttestationGeneratorBak {

    private static final String ATTESTATION_EXTENSION_OID = "1.3.6.1.4.1.11129.2.1.17";
    private static final String PACKAGE_NAME = "com.whatsapp.w4b";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    static {
        // 注册BouncyCastle提供者（仅用于ASN.1操作）
        Security.addProvider(new BouncyCastleProvider());
    }

    public static void main(String[] args) {
        try {
            System.out.println("开始生成更真实的Android Keystore Attestation证书链...");

            // 生成密钥对
            KeyPair rootKeyPair = generateECKeyPair("ROOT");
            KeyPair intermediateKeyPair = generateECKeyPair("INTERMEDIATE");
            KeyPair endKeyPair = generateECKeyPair("END");

            // 创建证书
            X509Certificate rootCert = generateRootCertificate(rootKeyPair);
            System.out.println("根证书生成完成");

            X509Certificate intermediateCert = generateIntermediateCertificate(intermediateKeyPair, rootKeyPair, rootCert);
            System.out.println("中间证书生成完成");

            X509Certificate endCert = generateEndCertificateWithAttestation(endKeyPair, intermediateKeyPair, intermediateCert);
            System.out.println("终端证书生成完成");

            // 输出证书链
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(rootCert.getEncoded());
            outputStream.write(intermediateCert.getEncoded());
            outputStream.write(endCert.getEncoded());

            String base64Chain = Base64.toBase64String(outputStream.toByteArray());
            System.out.println("\n生成的证书链(Base64):");
            System.out.println(base64Chain);

            // 保存到文件
            try (FileOutputStream fos = new FileOutputStream("attestation_chain.pem")) {
                fos.write("-----BEGIN CERTIFICATE CHAIN-----\n".getBytes());
                fos.write(base64Chain.getBytes());
                fos.write("\n-----END CERTIFICATE CHAIN-----".getBytes());
            }
            System.out.println("\n证书链已保存到 attestation_chain.pem");

        } catch (Exception e) {
            System.out.println("生成证书时出错:");
            e.printStackTrace();
        }
    }

    private static KeyPair generateECKeyPair(String name) throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
        keyGen.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair keyPair = keyGen.generateKeyPair();
        System.out.println(name + " 密钥对生成完成");
        return keyPair;
    }

    private static X509Certificate generateRootCertificate(KeyPair keyPair)
            throws CertificateException, IOException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, SignatureException {

        // 设置证书有效期 - 与真实的Google根证书匹配
        Date startDate = new Date(1452499430000L); // 2016-01-11 00:43:50 UTC
        Date endDate = new Date(2082016430000L);   // 2036-01-06 00:43:50 UTC

        // 创建X500Name - 与真实的Google根证书匹配
        X500Name subjectName = new X500Name("CN=Android Keystore Software Attestation Root, OU=Android, O=Google\\, Inc., L=Mountain View, ST=California, C=US");

        // 创建证书信息
        X509CertInfo certInfo = new X509CertInfo();
        certInfo.set(X509CertInfo.VERSION, new CertificateVersion(CertificateVersion.V3));
        certInfo.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber(new BigInteger("11674912229752527703")));
        certInfo.set(X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId(AlgorithmId.get("SHA256withECDSA")));
        certInfo.set(X509CertInfo.SUBJECT, subjectName);
        certInfo.set(X509CertInfo.ISSUER, subjectName);
        certInfo.set(X509CertInfo.KEY, new CertificateX509Key(keyPair.getPublic()));
        certInfo.set(X509CertInfo.VALIDITY, new CertificateValidity(startDate, endDate));

        // 添加扩展
        CertificateExtensions extensions = new CertificateExtensions();

        // 基本约束
        BasicConstraintsExtension bce = new BasicConstraintsExtension(true, 2);
        extensions.set(BasicConstraintsExtension.NAME, bce);

        // 密钥用途
        KeyUsageExtension keyUsage = new KeyUsageExtension();
        keyUsage.set(KeyUsageExtension.KEY_CERTSIGN, true);
        keyUsage.set(KeyUsageExtension.CRL_SIGN, true);
        extensions.set(KeyUsageExtension.NAME, keyUsage);

        // 添加主题密钥标识符
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] keyIdentifier = md.digest(keyPair.getPublic().getEncoded());
            SubjectKeyIdentifierExtension subjectKeyId = new SubjectKeyIdentifierExtension(keyIdentifier);
            extensions.set(SubjectKeyIdentifierExtension.NAME, subjectKeyId);
        } catch (Exception e) {
            System.out.println("创建主题密钥标识符时出错: " + e.getMessage());
        }

        certInfo.set(X509CertInfo.EXTENSIONS, extensions);

        // 创建证书并签名
        X509CertImpl certificate = new X509CertImpl(certInfo);
        certificate.sign(keyPair.getPrivate(), "SHA256withECDSA");

        return certificate;
    }

    private static X509Certificate generateIntermediateCertificate(KeyPair keyPair, KeyPair issuerKeyPair, X509Certificate issuerCert)
            throws CertificateException, IOException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, SignatureException {

        // 设置证书有效期 - 与真实的中间证书匹配
        Date startDate = new Date(1452499569000L); // 2016-01-11 00:46:09 UTC
        Date endDate = new Date(1757239569000L);   // 2026-01-08 00:46:09 UTC

        // 创建X500Name - 与真实的中间证书匹配
        X500Name subjectName = new X500Name("CN=Android Keystore Software Attestation Intermediate, OU=Android, O=Google\\, Inc., ST=California, C=US");
        X500Name issuerName = X500Name.asX500Name(issuerCert.getSubjectX500Principal());

        // 创建证书信息
        X509CertInfo certInfo = new X509CertInfo();
        certInfo.set(X509CertInfo.VERSION, new CertificateVersion(CertificateVersion.V3));
        certInfo.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber(new BigInteger("4097")));
        certInfo.set(X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId(AlgorithmId.get("SHA256withECDSA")));
        certInfo.set(X509CertInfo.SUBJECT, subjectName);
        certInfo.set(X509CertInfo.ISSUER, issuerName);
        certInfo.set(X509CertInfo.KEY, new CertificateX509Key(keyPair.getPublic()));
        certInfo.set(X509CertInfo.VALIDITY, new CertificateValidity(startDate, endDate));

        // 添加扩展
        CertificateExtensions extensions = new CertificateExtensions();

        // 基本约束
        BasicConstraintsExtension bce = new BasicConstraintsExtension(true, 1);
        extensions.set(BasicConstraintsExtension.NAME, bce);

        // 密钥用途
        KeyUsageExtension keyUsage = new KeyUsageExtension();
        keyUsage.set(KeyUsageExtension.KEY_CERTSIGN, true);
        keyUsage.set(KeyUsageExtension.CRL_SIGN, true);
        extensions.set(KeyUsageExtension.NAME, keyUsage);

        // 添加主题密钥标识符
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] keyIdentifier = md.digest(keyPair.getPublic().getEncoded());
            SubjectKeyIdentifierExtension subjectKeyId = new SubjectKeyIdentifierExtension(keyIdentifier);
            extensions.set(SubjectKeyIdentifierExtension.NAME, subjectKeyId);
        } catch (Exception e) {
            System.out.println("创建主题密钥标识符时出错: " + e.getMessage());
        }

        // 添加颁发者密钥标识符
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(issuerKeyPair.getPublic().getEncoded());
            KeyIdentifier keyId = new KeyIdentifier(digest);
            AuthorityKeyIdentifierExtension authKeyId = new AuthorityKeyIdentifierExtension(keyId, null, null);
            extensions.set(AuthorityKeyIdentifierExtension.NAME, authKeyId);
        } catch (Exception e) {
            System.out.println("创建颁发者密钥标识符时出错: " + e.getMessage());
        }

        certInfo.set(X509CertInfo.EXTENSIONS, extensions);

        // 创建证书并签名
        X509CertImpl certificate = new X509CertImpl(certInfo);
        certificate.sign(issuerKeyPair.getPrivate(), "SHA256withECDSA");

        return certificate;
    }

    private static X509Certificate generateEndCertificateWithAttestation(KeyPair keyPair, KeyPair issuerKeyPair, X509Certificate issuerCert)
            throws CertificateException, IOException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, SignatureException {

        // 设置证书有效期 - 与真实的终端证书匹配
        Date startDate = new Date(0); // 1970-01-01 00:00:00 UTC
        Date endDate = new Date(4294967295000L); // 2106-02-07 06:28:15 UTC

        // 创建X500Name - 使用字符串构造函数
        X500Name subjectName = new X500Name("CN=Android Keystore Key");
        X500Name issuerName = X500Name.asX500Name(issuerCert.getSubjectX500Principal());

        // 创建随机序列号
        byte[] serialBytes = new byte[8];
        SECURE_RANDOM.nextBytes(serialBytes);
        serialBytes[0] = (byte)(serialBytes[0] & 0x7F); // 确保是正数
        BigInteger serialNumber = new BigInteger(1, serialBytes);

        // 创建证书信息
        X509CertInfo certInfo = new X509CertInfo();
        certInfo.set(X509CertInfo.VERSION, new CertificateVersion(CertificateVersion.V3));
        certInfo.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber(serialNumber));
        certInfo.set(X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId(AlgorithmId.get("SHA256withECDSA")));
        certInfo.set(X509CertInfo.SUBJECT, subjectName);
        certInfo.set(X509CertInfo.ISSUER, issuerName);
        certInfo.set(X509CertInfo.KEY, new CertificateX509Key(keyPair.getPublic()));
        certInfo.set(X509CertInfo.VALIDITY, new CertificateValidity(startDate, endDate));

        // 添加扩展
        CertificateExtensions extensions = new CertificateExtensions();

        // 密钥用途
        KeyUsageExtension keyUsage = new KeyUsageExtension();
        keyUsage.set(KeyUsageExtension.DIGITAL_SIGNATURE, true);
        extensions.set(KeyUsageExtension.NAME, keyUsage);

        // 添加扩展密钥用途
        try {
            Vector<ObjectIdentifier> extendedKeyUsages = new Vector<>();
            extendedKeyUsages.add(new ObjectIdentifier("1.3.6.1.5.5.7.3.1")); // serverAuth
            extendedKeyUsages.add(new ObjectIdentifier("1.3.6.1.5.5.7.3.2")); // clientAuth
            ExtendedKeyUsageExtension ekue = new ExtendedKeyUsageExtension(extendedKeyUsages);
            extensions.set(ExtendedKeyUsageExtension.NAME, ekue);
        } catch (Exception e) {
            System.out.println("创建扩展密钥用途时出错: " + e.getMessage());
        }

        // 添加颁发者密钥标识符
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(issuerKeyPair.getPublic().getEncoded());
            KeyIdentifier keyId = new KeyIdentifier(digest);
            AuthorityKeyIdentifierExtension authKeyId = new AuthorityKeyIdentifierExtension(keyId, null, null);
            extensions.set(AuthorityKeyIdentifierExtension.NAME, authKeyId);
        } catch (Exception e) {
            System.out.println("创建颁发者密钥标识符时出错: " + e.getMessage());
        }

        // 添加Attestation扩展
        try {
            byte[] attestationExtData = createAttestationExtension();

            // 包装成DER OCTET STRING
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DerOutputStream dos = new DerOutputStream();
            dos.putOctetString(attestationExtData);
            baos.write(dos.toByteArray());

            extensions.set(ATTESTATION_EXTENSION_OID, new Extension(
                    new ObjectIdentifier(ATTESTATION_EXTENSION_OID),
                    false,
                    baos.toByteArray()));
        } catch (Exception e) {
            System.out.println("创建attestation扩展时出错: " + e.getMessage());
            e.printStackTrace();
        }

        certInfo.set(X509CertInfo.EXTENSIONS, extensions);

        // 创建证书并签名
        X509CertImpl certificate = new X509CertImpl(certInfo);
        certificate.sign(issuerKeyPair.getPrivate(), "SHA256withECDSA");

        return certificate;
    }

    private static byte[] createAttestationExtension() throws Exception {
        // 创建版本2的attestation扩展结构
        ASN1EncodableVector vector = new ASN1EncodableVector();

        // 版本
        vector.add(new ASN1Integer(2));

        // 安全级别 (0 = Software)
        vector.add(new ASN1Enumerated(0));

        // 挑战值 - 32字节随机数
        byte[] challenge = new byte[32];
        SECURE_RANDOM.nextBytes(challenge);
        vector.add(new DEROctetString(challenge)); // 真实系统使用octet string

        // 软件强制
        vector.add(new ASN1Enumerated(1));

        // TEE强制
        vector.add(new DEROctetString(new byte[0]));

        // 认证数据 - 模拟Android系统信息
        byte[] attestData = {
                0x41, 0x6e, 0x64, 0x72, 0x6f, 0x69, 0x64, 0x20, 0x31, 0x33, // "Android 13"
                0x2c, 0x20, 0x73, 0x65, 0x63, 0x75, 0x72, 0x69, 0x74, 0x79, 0x20,
                0x70, 0x61, 0x74, 0x63, 0x68, 0x20, 0x6c, 0x65, 0x76, 0x65, 0x6c, 0x3a,
                0x20, 0x32, 0x30, 0x32, 0x33, 0x2d, 0x30, 0x33, 0x2d, 0x30, 0x35 // ", security patch level: 2023-03-05"
        };
        vector.add(new DEROctetString(attestData));

        // 创建应用ID部分
        ASN1EncodableVector appIdVector = new ASN1EncodableVector();

        // 包信息
        ASN1EncodableVector packageInfoVector = new ASN1EncodableVector();
        ASN1EncodableVector packageSeqVector = new ASN1EncodableVector();
        packageSeqVector.add(new DEROctetString(PACKAGE_NAME.getBytes()));
        // 添加签名信息 - 模拟APK签名哈希
        byte[] signatureHash = new byte[32]; // SHA-256哈希
        SECURE_RANDOM.nextBytes(signatureHash);
        packageSeqVector.add(new DEROctetString(signatureHash));
        packageInfoVector.add(new DERSequence(packageSeqVector));
        appIdVector.add(new DERSet(packageInfoVector));

        // 创建KeyDescription序列
        ASN1EncodableVector keyDescriptionVector = new ASN1EncodableVector();

        // 添加应用ID信息
        ASN1EncodableVector appIdPair = new ASN1EncodableVector();
        appIdPair.add(new ASN1Integer(709)); // KM_TAG_ATTESTATION_APPLICATION_ID
        appIdPair.add(new DEROctetString(new DERSequence(appIdVector).getEncoded()));
        keyDescriptionVector.add(new DERSequence(appIdPair));

        // 添加创建时间戳
        ASN1EncodableVector creationTimePair = new ASN1EncodableVector();
        creationTimePair.add(new ASN1Integer(701)); // KM_TAG_CREATION_DATETIME
        creationTimePair.add(new ASN1Integer(System.currentTimeMillis() / 1000));
        keyDescriptionVector.add(new DERSequence(creationTimePair));

        // 添加密钥用途
        ASN1EncodableVector purposesVector = new ASN1EncodableVector();
        purposesVector.add(new ASN1Integer(2)); // KEY_PURPOSE_VERIFY
        purposesVector.add(new ASN1Integer(3)); // KEY_PURPOSE_SIGN

        ASN1EncodableVector purposePair = new ASN1EncodableVector();
        purposePair.add(new ASN1Integer(1)); // KM_TAG_PURPOSE
        purposePair.add(new DERSet(purposesVector));
        keyDescriptionVector.add(new DERSequence(purposePair));

        // 添加密钥起源
        ASN1EncodableVector originPair = new ASN1EncodableVector();
        originPair.add(new ASN1Integer(702)); // KM_TAG_ORIGIN
        originPair.add(new ASN1Integer(0)); // Generated
        keyDescriptionVector.add(new DERSequence(originPair));

        // 添加设备标识信息
        ASN1EncodableVector brandPair = new ASN1EncodableVector();
        brandPair.add(new ASN1Integer(1100)); // KM_TAG_ATTESTATION_ID_BRAND
        brandPair.add(new DEROctetString("Google".getBytes()));
        keyDescriptionVector.add(new DERSequence(brandPair));

        ASN1EncodableVector devicePair = new ASN1EncodableVector();
        devicePair.add(new ASN1Integer(1101)); // KM_TAG_ATTESTATION_ID_DEVICE
        devicePair.add(new DEROctetString("Pixel".getBytes()));
        keyDescriptionVector.add(new DERSequence(devicePair));

        ASN1EncodableVector productPair = new ASN1EncodableVector();
        productPair.add(new ASN1Integer(1102)); // KM_TAG_ATTESTATION_ID_PRODUCT
        productPair.add(new DEROctetString("Pixel 7".getBytes()));
        keyDescriptionVector.add(new DERSequence(productPair));

        ASN1EncodableVector manufacturerPair = new ASN1EncodableVector();
        manufacturerPair.add(new ASN1Integer(1103)); // KM_TAG_ATTESTATION_ID_MANUFACTURER
        manufacturerPair.add(new DEROctetString("Google".getBytes()));
        keyDescriptionVector.add(new DERSequence(manufacturerPair));

        ASN1EncodableVector modelPair = new ASN1EncodableVector();
        modelPair.add(new ASN1Integer(1104)); // KM_TAG_ATTESTATION_ID_MODEL
        modelPair.add(new DEROctetString("GVU6C".getBytes())); // Pixel 7 model
        keyDescriptionVector.add(new DERSequence(modelPair));

        // 添加操作系统版本和安全补丁级别
        ASN1EncodableVector osVersionPair = new ASN1EncodableVector();
        osVersionPair.add(new ASN1Integer(1226)); // KM_TAG_OS_VERSION
        osVersionPair.add(new ASN1Integer(130000)); // Android 13
        keyDescriptionVector.add(new DERSequence(osVersionPair));

        ASN1EncodableVector osPatchLevelPair = new ASN1EncodableVector();
        osPatchLevelPair.add(new ASN1Integer(1227)); // KM_TAG_OS_PATCHLEVEL
        osPatchLevelPair.add(new ASN1Integer(202303)); // 2023-03
        keyDescriptionVector.add(new DERSequence(osPatchLevelPair));

        // 添加密钥属性
        ASN1EncodableVector noAuthRequiredPair = new ASN1EncodableVector();
        noAuthRequiredPair.add(new ASN1Integer(503)); // KM_TAG_NO_AUTH_REQUIRED
        noAuthRequiredPair.add(ASN1Boolean.getInstance(true));
        keyDescriptionVector.add(new DERSequence(noAuthRequiredPair));

        // 将KeyDescription添加到主向量
        vector.add(new DERSequence(keyDescriptionVector));

        // 添加一个空的键值对序列(软件相关信息)
        vector.add(new DERSequence());

        // 编码为DER
        DERSequence sequence = new DERSequence(vector);
        return sequence.getEncoded();
    }

    // 辅助方法: 获取昨天的日期
    private static Date yesterday() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        return cal.getTime();
    }

    // 辅助方法: 获取几年后的日期
    private static Date yearsFromNow(int years) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, years);
        return cal.getTime();
    }
}