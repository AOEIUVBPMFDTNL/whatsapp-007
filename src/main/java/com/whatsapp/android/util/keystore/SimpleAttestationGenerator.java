package com.whatsapp.android.util.keystore;

import org.bouncycastle.asn1.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;
import sun.security.util.ObjectIdentifier;
import sun.security.x509.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.util.Date;

/**
 * 一个使用Java原生API创建Keystore Attestation证书的工具类
 * 尽可能模拟真实的Android Keystore Attestation证书链结构
 */
public class SimpleAttestationGenerator {

    private static final String ATTESTATION_EXTENSION_OID = "1.3.6.1.4.1.11129.2.1.17";
    private static final String PACKAGE_NAME = "com.whatsapp.w4b";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // KeyMaster标签常量
    private static final int KM_TAG_PURPOSE = 1;
    private static final int KM_TAG_ALGORITHM = 2;
    private static final int KM_TAG_KEY_SIZE = 3;
    private static final int KM_TAG_DIGEST = 5;
    private static final int KM_TAG_PADDING = 6;
    private static final int KM_TAG_BLOB_USAGE_REQUIREMENTS = 10;
    private static final int KM_TAG_RSA_PUBLIC_EXPONENT = 200;
    private static final int KM_TAG_ACTIVE_DATETIME = 400;
    private static final int KM_TAG_ORIGINATION_EXPIRE_DATETIME = 401;
    private static final int KM_TAG_USAGE_EXPIRE_DATETIME = 402;
    private static final int KM_TAG_NO_AUTH_REQUIRED = 503;
    private static final int KM_TAG_USER_AUTH_TYPE = 504;
    private static final int KM_TAG_AUTH_TIMEOUT = 505;
    private static final int KM_TAG_CREATION_DATETIME = 701;
    private static final int KM_TAG_ORIGIN = 702;
    private static final int KM_TAG_ROLLBACK_RESISTANCE = 703;
    private static final int KM_TAG_ROOT_OF_TRUST = 704;
    private static final int KM_TAG_OS_VERSION = 705;
    private static final int KM_TAG_OS_PATCHLEVEL = 706;
    private static final int KM_TAG_ATTESTATION_APPLICATION_ID = 709;
    private static final int KM_TAG_ATTESTATION_ID_BRAND = 1100;
    private static final int KM_TAG_ATTESTATION_ID_DEVICE = 1101;
    private static final int KM_TAG_ATTESTATION_ID_PRODUCT = 1102;
    private static final int KM_TAG_ATTESTATION_ID_MANUFACTURER = 1103;
    private static final int KM_TAG_ATTESTATION_ID_MODEL = 1104;
    private static final int KM_TAG_VENDOR_PATCHLEVEL = 1150;
    private static final int KM_TAG_BOOT_PATCHLEVEL = 1151;
    private static final int KM_TAG_DEVICE_UNIQUE_ATTESTATION = 1152;
    private static final int KM_TAG_OS_VERSION_MAJOR = 1226;
    private static final int KM_TAG_OS_VERSION_MINOR = 1227;
    private static final int KM_TAG_OS_VERSION_SUBMINOR = 1228;

    // KeyMaster值常量
    private static final int KM_PURPOSE_SIGN = 2;
    private static final int KM_PURPOSE_VERIFY = 3;
    private static final int KM_ALGORITHM_EC = 3;
    private static final int KM_ORIGIN_GENERATED = 0;
    private static final int KM_DIGEST_SHA_2_256 = 4;

    static {
        // 注册BouncyCastle提供者（仅用于ASN.1操作）
        Security.addProvider(new BouncyCastleProvider());
    }

    public static void main(String[] args) {
        try {
            //String attestData = "Android 13, security patch level: 2023-03-05";
            //com.whatsapp.w4b都是优化点
            System.out.println("开始生成更真实的Android Keystore Attestation证书链...");
            String base64Chain = generateAndroidKeystoreAttestation();
            System.out.println("\n生成的证书链(Base64):");
            System.out.println(base64Chain);


        } catch (Exception e) {
            System.out.println("生成证书时出错:");
            e.printStackTrace();
        }
    }

    public static String generateAndroidKeystoreAttestation() {
        try {
            // 生成密钥对
            KeyPair rootKeyPair = generateECKeyPair("ROOT");
            KeyPair intermediateKeyPair = generateECKeyPair("INTERMEDIATE");
            KeyPair endKeyPair = generateECKeyPair("END");

            // 创建证书
            X509Certificate rootCert = generateRootCertificate(rootKeyPair);

            X509Certificate intermediateCert = generateIntermediateCertificate(intermediateKeyPair, rootKeyPair, rootCert);

            X509Certificate endCert = generateEndCertificateWithAttestation(endKeyPair, intermediateKeyPair, intermediateCert);

            // 输出证书链 - 根证书要先输出
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(rootCert.getEncoded());
            outputStream.write(intermediateCert.getEncoded());
            outputStream.write(endCert.getEncoded());

            return Base64.toBase64String(outputStream.toByteArray());
        } catch (Exception e) {
            return null;
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

        // 设置证书有效期 - 与真实的Google根证书精确匹配
        Date startDate = new Date(1452499430000L); // 2016-01-11 08:43:50 CST
        Date endDate = new Date(2082016430000L);   // 2036-01-06 08:43:50 CST

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

        // 基本约束 - 关键扩展
        BasicConstraintsExtension bce = new BasicConstraintsExtension(true, 2);
        extensions.set(BasicConstraintsExtension.NAME, bce);

        // 密钥用途 - 关键扩展
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

        // 添加颁发者密钥标识符 - 与真实根证书一致
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(keyPair.getPublic().getEncoded());
            KeyIdentifier keyId = new KeyIdentifier(digest);
            AuthorityKeyIdentifierExtension authKeyId = new AuthorityKeyIdentifierExtension(keyId, null, null);
            extensions.set(AuthorityKeyIdentifierExtension.NAME, authKeyId);
        } catch (Exception e) {
            System.out.println("创建颁发者密钥标识符时出错: " + e.getMessage());
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
        Date startDate = new Date(1452499569000L); // 2016-01-11 08:46:09 CST
        Date endDate = new Date(1757239569000L);   // 2026-01-08 08:46:09 CST

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

        // 基本约束 - 关键扩展
        BasicConstraintsExtension bce = new BasicConstraintsExtension(true, 1);
        extensions.set(BasicConstraintsExtension.NAME, bce);

        // 密钥用途 - 关键扩展
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

        // 创建固定序列号 - 与真实的终端证书匹配
        BigInteger serialNumber = BigInteger.ONE; // 序列号为1

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

        // 密钥用途 - 关键扩展（和真实证书一致）
        KeyUsageExtension keyUsage = new KeyUsageExtension();
        keyUsage.set(KeyUsageExtension.DIGITAL_SIGNATURE, true);
        extensions.set(KeyUsageExtension.NAME, keyUsage);

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

        // 添加Attestation扩展 - 使用BouncyCastle来确保编码正确
        try {
            // 使用BouncyCastle创建更真实的Attestation扩展内容
            byte[] attestationBytes = createEnhancedAttestationExtension();

            // 创建OCTET STRING包装的扩展
            Extension attestExtension = new Extension(
                    new ObjectIdentifier(ATTESTATION_EXTENSION_OID),
                    false,
                    new DEROctetString(attestationBytes).getEncoded());

            extensions.set(ATTESTATION_EXTENSION_OID, attestExtension);

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

    private static byte[] createEnhancedAttestationExtension() throws Exception {
        // 主序列向量
        ASN1EncodableVector mainVector = new ASN1EncodableVector();

        // 1. 版本 (2)
        mainVector.add(new ASN1Integer(2));

        // 2. 安全级别 (SOFTWARE = 0)
        mainVector.add(new ASN1Enumerated(0));

        // 3. 挑战值 - 32字节随机数
        byte[] challenge = new byte[32];
        SECURE_RANDOM.nextBytes(challenge);
        mainVector.add(new ASN1Integer(new BigInteger(1, challenge)));

        // 4. 软件强制
        mainVector.add(new ASN1Enumerated(0));

        // 5. TEE强制 - 空八位字节字符串
        mainVector.add(new DEROctetString(new byte[0]));

        // 6. 认证数据 - 模拟Android系统信息
        String attestData = "Android 13, security patch level: 2023-03-05";
        mainVector.add(new DEROctetString(attestData.getBytes()));

        // 7. 应用ID序列 - KeyMaster标签与值
        ASN1EncodableVector keyDescriptionVector = new ASN1EncodableVector();

        // 7.1 应用ID信息
        byte[] appIdInfoBytes = createAttestationApplicationId();
        ASN1EncodableVector appIdPair = new ASN1EncodableVector();
        appIdPair.add(new ASN1Integer(KM_TAG_ATTESTATION_APPLICATION_ID));
        appIdPair.add(new DEROctetString(appIdInfoBytes));
        keyDescriptionVector.add(new DERSequence(appIdPair));

        // 7.2 创建时间戳
        ASN1EncodableVector creationTimePair = new ASN1EncodableVector();
        creationTimePair.add(new ASN1Integer(KM_TAG_CREATION_DATETIME));
        creationTimePair.add(new ASN1Integer(System.currentTimeMillis() / 1000));
        keyDescriptionVector.add(new DERSequence(creationTimePair));

        // 7.3 起源
        ASN1EncodableVector originPair = new ASN1EncodableVector();
        originPair.add(new ASN1Integer(KM_TAG_ORIGIN));
        originPair.add(new ASN1Integer(KM_ORIGIN_GENERATED));
        keyDescriptionVector.add(new DERSequence(originPair));

        // 7.4 密钥用途
        ASN1EncodableVector purposeVector = new ASN1EncodableVector();
        purposeVector.add(new ASN1Integer(KM_PURPOSE_SIGN));
        purposeVector.add(new ASN1Integer(KM_PURPOSE_VERIFY));

        ASN1EncodableVector purposePair = new ASN1EncodableVector();
        purposePair.add(new ASN1Integer(KM_TAG_PURPOSE));
        purposePair.add(new DERSet(purposeVector));
        keyDescriptionVector.add(new DERSequence(purposePair));

        // 7.5 算法
        ASN1EncodableVector algorithmPair = new ASN1EncodableVector();
        algorithmPair.add(new ASN1Integer(KM_TAG_ALGORITHM));
        algorithmPair.add(new ASN1Integer(KM_ALGORITHM_EC));
        keyDescriptionVector.add(new DERSequence(algorithmPair));

        // 7.6 密钥大小
        ASN1EncodableVector keySizePair = new ASN1EncodableVector();
        keySizePair.add(new ASN1Integer(KM_TAG_KEY_SIZE));
        keySizePair.add(new ASN1Integer(256)); // EC P-256
        keyDescriptionVector.add(new DERSequence(keySizePair));

        // 7.7 摘要算法
        ASN1EncodableVector digestVector = new ASN1EncodableVector();
        digestVector.add(new ASN1Integer(KM_DIGEST_SHA_2_256));

        ASN1EncodableVector digestPair = new ASN1EncodableVector();
        digestPair.add(new ASN1Integer(KM_TAG_DIGEST));
        digestPair.add(new DERSet(digestVector));
        keyDescriptionVector.add(new DERSequence(digestPair));

        // 7.8 无认证要求
        ASN1EncodableVector noAuthRequiredPair = new ASN1EncodableVector();
        noAuthRequiredPair.add(new ASN1Integer(KM_TAG_NO_AUTH_REQUIRED));
        noAuthRequiredPair.add(ASN1Boolean.getInstance(true));
        keyDescriptionVector.add(new DERSequence(noAuthRequiredPair));

        // 7.9 OS版本
        ASN1EncodableVector osVersionPair = new ASN1EncodableVector();
        osVersionPair.add(new ASN1Integer(KM_TAG_OS_VERSION));
        osVersionPair.add(new ASN1Integer(130000)); // Android 13
        keyDescriptionVector.add(new DERSequence(osVersionPair));

        // 7.10 OS补丁级别
        ASN1EncodableVector osPatchLevelPair = new ASN1EncodableVector();
        osPatchLevelPair.add(new ASN1Integer(KM_TAG_OS_PATCHLEVEL));
        osPatchLevelPair.add(new ASN1Integer(202303)); // 2023-03
        keyDescriptionVector.add(new DERSequence(osPatchLevelPair));

        // 7.11 设备品牌
        ASN1EncodableVector brandPair = new ASN1EncodableVector();
        brandPair.add(new ASN1Integer(KM_TAG_ATTESTATION_ID_BRAND));
        brandPair.add(new DEROctetString("Google".getBytes()));
        keyDescriptionVector.add(new DERSequence(brandPair));

        // 7.12 设备型号
        ASN1EncodableVector devicePair = new ASN1EncodableVector();
        devicePair.add(new ASN1Integer(KM_TAG_ATTESTATION_ID_DEVICE));
        devicePair.add(new DEROctetString("Pixel".getBytes()));
        keyDescriptionVector.add(new DERSequence(devicePair));

        // 7.13 产品名称
        ASN1EncodableVector productPair = new ASN1EncodableVector();
        productPair.add(new ASN1Integer(KM_TAG_ATTESTATION_ID_PRODUCT));
        productPair.add(new DEROctetString("Pixel 7".getBytes()));
        keyDescriptionVector.add(new DERSequence(productPair));

        // 7.14 制造商
        ASN1EncodableVector manufacturerPair = new ASN1EncodableVector();
        manufacturerPair.add(new ASN1Integer(KM_TAG_ATTESTATION_ID_MANUFACTURER));
        manufacturerPair.add(new DEROctetString("Google".getBytes()));
        keyDescriptionVector.add(new DERSequence(manufacturerPair));

        // 7.15 设备型号代码
        ASN1EncodableVector modelPair = new ASN1EncodableVector();
        modelPair.add(new ASN1Integer(KM_TAG_ATTESTATION_ID_MODEL));
        modelPair.add(new DEROctetString("GVU6C".getBytes())); // Pixel 7 model
        keyDescriptionVector.add(new DERSequence(modelPair));

        // 将KeyDescription作为序列添加到主向量
        mainVector.add(new DERSequence(keyDescriptionVector));

        // 8. 软件密钥相关信息(空序列)
        mainVector.add(new DERSequence());

        // 将整个序列编码为DER
        DERSequence attestationSequence = new DERSequence(mainVector);
        return attestationSequence.getEncoded();
    }

    private static byte[] createAttestationApplicationId() throws Exception {
        // 创建应用ID的ASN.1结构
        ASN1EncodableVector appIdVector = new ASN1EncodableVector();

        // 包信息集合
        ASN1EncodableVector packageInfoVector = new ASN1EncodableVector();

        // 包1信息 - com.whatsapp.w4b（确保这是第一个包）
        ASN1EncodableVector packageVector1 = new ASN1EncodableVector();
        // 包名
        packageVector1.add(new DEROctetString(PACKAGE_NAME.getBytes()));
        // 签名哈希 - 随机生成以模拟真实签名
        byte[] signatureHash = new byte[32]; // SHA-256哈希长度
        SECURE_RANDOM.nextBytes(signatureHash);
        packageVector1.add(new DEROctetString(signatureHash));
        packageInfoVector.add(new DERSequence(packageVector1));

        // 如需添加第二个包信息（平台签名），可以选择性启用此代码
        // ASN1EncodableVector packageVector2 = new ASN1EncodableVector();
        // packageVector2.add(new DEROctetString("android".getBytes()));
        // byte[] platformSignatureHash = new byte[32];
        // SECURE_RANDOM.nextBytes(platformSignatureHash);
        // packageVector2.add(new DEROctetString(platformSignatureHash));
        // packageInfoVector.add(new DERSequence(packageVector2));

        // 将所有包信息添加到应用ID
        appIdVector.add(new DERSet(packageInfoVector));

        // 编码整个应用ID结构
        DERSequence appIdSequence = new DERSequence(appIdVector);
        return appIdSequence.getEncoded();
    }
}