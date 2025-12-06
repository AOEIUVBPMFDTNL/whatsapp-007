/**
 * Copyright (C) 2014-2016 Open Whisper Systems
 * <p>
 * Licensed according to the LICENSE file in this repository.
 */
package org.whispersystems.libsignal.protocol;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;
import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.InvalidMessageException;
import org.whispersystems.libsignal.LegacyMessageException;
import org.whispersystems.libsignal.ecc.Curve;
import org.whispersystems.libsignal.ecc.ECPublicKey;
import org.whispersystems.libsignal.util.ByteUtil;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class SignalMessage implements CiphertextMessage {

    private static final int MAC_LENGTH = 8;

    private final int messageVersion;
    private final ECPublicKey senderRatchetKey;
    private final int counter;
    private final int previousCounter;
    private final byte[] ciphertext;
    private final byte[] serialized;

    public SignalMessage(byte[] serialized) throws InvalidMessageException, LegacyMessageException {
        try {
            byte[][] messageParts = ByteUtil.split(serialized, 1, serialized.length - 1 - MAC_LENGTH, MAC_LENGTH);
            byte version = messageParts[0][0];
            byte[] message = messageParts[1];
            byte[] mac = messageParts[2];

            if (ByteUtil.highBitsToInt(version) < CURRENT_VERSION) {
                throw new LegacyMessageException("Legacy message: " + ByteUtil.highBitsToInt(version));
            }

            if (ByteUtil.highBitsToInt(version) > CURRENT_VERSION) {
                throw new InvalidMessageException("Unknown version: " + ByteUtil.highBitsToInt(version));
            }

            SignalProtos.SignalMessage whisperMessage = SignalProtos.SignalMessage.parseFrom(message);

            if (!whisperMessage.hasCiphertext() ||
                    !whisperMessage.hasCounter() ||
                    !whisperMessage.hasRatchetKey()) {
                throw new InvalidMessageException("Incomplete message.");
            }

            this.serialized = serialized;
            this.senderRatchetKey = Curve.decodePoint(whisperMessage.getRatchetKey().toByteArray(), 0);
            this.messageVersion = ByteUtil.highBitsToInt(version);
            this.counter = whisperMessage.getCounter();
            this.previousCounter = whisperMessage.getPreviousCounter();
            this.ciphertext = whisperMessage.getCiphertext().toByteArray();
        } catch (InvalidProtocolBufferException | InvalidKeyException | ParseException e) {
            throw new InvalidMessageException(e);
        }
    }

    public SignalMessage(int messageVersion, SecretKeySpec macKey, ECPublicKey senderRatchetKey,
                         int counter, int previousCounter, byte[] ciphertext,
                         IdentityKey senderIdentityKey,
                         IdentityKey receiverIdentityKey) {
        byte[] version = {ByteUtil.intsToByteHighAndLow(messageVersion, CURRENT_VERSION)};
        byte[] message = SignalProtos.SignalMessage.newBuilder()
                .setRatchetKey(ByteString.copyFrom(senderRatchetKey.serialize()))
                .setCounter(counter)
                .setPreviousCounter(previousCounter)
                .setCiphertext(ByteString.copyFrom(ciphertext))
                .build().toByteArray();

        byte[] mac = getMac(senderIdentityKey, receiverIdentityKey, macKey, ByteUtil.combine(version, message));

        this.serialized = ByteUtil.combine(version, message, mac);
        this.senderRatchetKey = senderRatchetKey;
        this.counter = counter;
        this.previousCounter = previousCounter;
        this.ciphertext = ciphertext;
        this.messageVersion = messageVersion;
    }

    public ECPublicKey getSenderRatchetKey() {
        return senderRatchetKey;
    }

    public int getMessageVersion() {
        return messageVersion;
    }

    public int getCounter() {
        return counter;
    }

    public byte[] getBody() {
        return ciphertext;
    }

    public void verifyMac(IdentityKey senderIdentityKey, IdentityKey receiverIdentityKey, SecretKeySpec macKey)
            throws InvalidMessageException {
        byte[][] parts = ByteUtil.split(serialized, serialized.length - MAC_LENGTH, MAC_LENGTH);
        byte[] ourMac = getMac(senderIdentityKey, receiverIdentityKey, macKey, parts[0]);
        byte[] theirMac = parts[1];

        if (!MessageDigest.isEqual(ourMac, theirMac)) {
            throw new InvalidMessageException("Bad Mac!");
        }
    }

    private byte[] getMac(IdentityKey senderIdentityKey,
                          IdentityKey receiverIdentityKey,
                          SecretKeySpec macKey, byte[] serialized) {
        try {
            byte[] sendPublicKey = senderIdentityKey.getPublicKey().serialize();
            byte[] recPublicKey = receiverIdentityKey.getPublicKey().serialize();
            HMac hMac = new HMac(new SHA256Digest());
            hMac.init(new KeyParameter(macKey.getEncoded()));
            hMac.update(sendPublicKey, 0, sendPublicKey.length);
            hMac.update(recPublicKey, 0, recPublicKey.length);
            hMac.update(serialized, 0, serialized.length);
            byte[] fullMac = new byte[hMac.getMacSize()];
            hMac.doFinal(fullMac, 0);
            return ByteUtil.trim(fullMac, MAC_LENGTH);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public byte[] serialize() {
        return serialized;
    }

    @Override
    public int getType() {
        return CiphertextMessage.WHISPER_TYPE;
    }

    public static boolean isLegacy(byte[] message) {
        return message != null && message.length >= 1 &&
                ByteUtil.highBitsToInt(message[0]) != CiphertextMessage.CURRENT_VERSION;
    }

}
