/**
 * Copyright (C) 2014-2016 Open Whisper Systems
 * <p>
 * Licensed according to the LICENSE file in this repository.
 */
package org.whispersystems.libsignal.groups.ratchet;

import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Each SenderKey is a "chain" of keys, each derived from the previous.
 *
 * At any given point in time, the state of a SenderKey can be represented
 * as the current chain key value, along with its iteration count.  From there,
 * subsequent iterations can be derived, as well as individual message keys from
 * each chain key.
 *
 * @author Moxie Marlinspike
 */
public class SenderChainKey {

    private static final byte[] MESSAGE_KEY_SEED = {0x01};
    private static final byte[] CHAIN_KEY_SEED = {0x02};

    private final int iteration;
    private final byte[] chainKey;

    public SenderChainKey(int iteration, byte[] chainKey) {
        this.iteration = iteration;
        this.chainKey = chainKey;
    }

    public int getIteration() {
        return iteration;
    }

    public SenderMessageKey getSenderMessageKey() {
        return new SenderMessageKey(iteration, getDerivative(MESSAGE_KEY_SEED, chainKey));
    }

    public SenderChainKey getNext() {
        return new SenderChainKey(iteration + 1, getDerivative(CHAIN_KEY_SEED, chainKey));
    }

    public byte[] getSeed() {
        return chainKey;
    }

    private byte[] getDerivative(byte[] seed, byte[] key) {
        try {
            HMac hMac = new HMac(new SHA256Digest());
            hMac.init(new KeyParameter(key));
            hMac.update(seed, 0, seed.length);
            byte[] out = new byte[hMac.getMacSize()];
            hMac.doFinal(out, 0);
            return out;
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

}
