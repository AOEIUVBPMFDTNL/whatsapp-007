package com.whatsapp.android;

import axolotl.AxolotlManager;
import cn.hutool.core.codec.Base64;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.ecc.Curve;
import org.whispersystems.libsignal.ecc.ECKeyPair;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;
import org.whispersystems.libsignal.state.StorageProtos;
import org.whispersystems.libsignal.util.ByteUtil;
import org.whispersystems.libsignal.util.KeyHelper;

import java.sql.PreparedStatement;

/**
 * @author sunnoc
 * @date 2023-05-10 19:08
 */
@Slf4j
public class GenerateSignPreKeyTest {
    private static AxolotlManager axolotlManager;

    static {
        axolotlManager = new AxolotlManager("/Users/sunnoc/IdeaProjects/whatsapp-android/out/66958129618.db", "66958129618");

    }

    public static void main(String[] args) throws InvalidKeyException {
        IdentityKeyPair identityKeyPair = InitIdentityData(Base64.decode("Ti9sViz8HcYcsBwOS4Z6yOWH6XtrnJ98cIuu8c543w4="), Base64.decode("4HHa73FmilwBjqNhvhospl5PDO9nINShHJ8Y9TjnGnc="));
        InitSignedPreKeyData(identityKeyPair, Base64.decode("WK1jqBR+h4SiAHnRnrd3MGArIjLl3bekFGJbYqkPrzE="),
                Base64.decode("oKwK3bpLg0K3B4fsYoOQ6XuKnEx3WAMOxWoCB7vsL08="), Base64.decode("cHNEpbrsabVwRIP/oFFV5+mqFrui94u63Iu8c2dAyzXYEsytJGWXk3u84GwdR7eHvexeBw5Er5SzOdl9rB7/iw=="));

    }

    public static IdentityKeyPair InitIdentityData(byte[] publicKey, byte[] privateKey) {
        byte[] type = {Curve.DJB_TYPE};
        publicKey = ByteUtil.combine(type, publicKey);
        PreparedStatement identityPreStatement = null;
        try {
            int registrationId = KeyHelper.generateRegistrationId(true);
            //保存identity key pair
            identityPreStatement = axolotlManager.GetPreparedStatement("INSERT OR REPLACE INTO identities(recipient_id, device_id, registration_id, public_key, private_key, next_prekey_id, timestamp) values(-1,0,?,?,?,?,?)");
            identityPreStatement.setInt(1, registrationId);
            identityPreStatement.setBytes(2, publicKey);
            identityPreStatement.setBytes(3, privateKey);
            identityPreStatement.setInt(4, KeyHelper.getRandomSequence(16777214));
            identityPreStatement.setLong(5, System.currentTimeMillis() / 1000);
            identityPreStatement.execute();
            return new IdentityKeyPair(new IdentityKey(publicKey, 0), KeyHelper.decodePrivateKey(privateKey));
        } catch (Exception e) {
            log.error("InitIdentityData异常", e);
        } finally {
            axolotlManager.closePreparedStatement(identityPreStatement);
        }
        return null;
    }

    public static void InitSignedPreKeyData(IdentityKeyPair identityKeyPair, byte[] keyPairPublic, byte[] keyPairPrivateKey, byte[] signature) {
        PreparedStatement preStatement = null;
        try {
            byte[] type = {Curve.DJB_TYPE};
            keyPairPublic = ByteUtil.combine(type, keyPairPublic);
            /*byte[] signature = Curve.calculateSignature(identityKeyPair.getPrivateKey(), keyPairPublic);
            System.out.println(Base64.encode(signature));*/
            // SignedPreKeyRecord signedPreKey1 = KeyHelper.generateSignedPreKey(identityKeyPair, 0);
            // byte[] serialize1 = signedPreKey1.getKeyPair().getPublicKey().serialize();
            long currentTime = System.currentTimeMillis() / 1000;
            StorageProtos.SignedPreKeyRecordStructure build = StorageProtos.SignedPreKeyRecordStructure.newBuilder()
                    .setId(0)
                    .setPublicKey(ByteString.copyFrom(keyPairPublic))
                    .setPrivateKey(ByteString.copyFrom(keyPairPrivateKey))
                    .setSignature(ByteString.copyFrom(signature))
                    .setTimestamp(currentTime)
                    .build();
            byte[] bytes = build.toByteArray();
            SignedPreKeyRecord signedPreKey = new SignedPreKeyRecord(bytes);
            byte[] serialize = signedPreKey.serialize();
            preStatement = axolotlManager.GetPreparedStatement("INSERT OR REPLACE INTO signed_prekeys (prekey_id, timestamp, record) VALUES(?,?,?)");
            preStatement.setInt(1, signedPreKey.getId());
            preStatement.setLong(2, currentTime);
            preStatement.setBytes(3, serialize);
            preStatement.execute();
        } catch (Exception e) {

        } finally {
            axolotlManager.closePreparedStatement(preStatement);
        }
    }
}
