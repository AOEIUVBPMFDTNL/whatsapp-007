/**
 * Copyright (C) 2014-2016 Open Whisper Systems
 * <p>
 * Licensed according to the LICENSE file in this repository.
 */
package org.whispersystems.libsignal.groups;

import com.whatsapp.android.util.KeyLockUtil;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.InvalidKeyIdException;
import org.whispersystems.libsignal.groups.state.SenderKeyRecord;
import org.whispersystems.libsignal.groups.state.SenderKeyState;
import org.whispersystems.libsignal.groups.state.SenderKeyStore;
import org.whispersystems.libsignal.protocol.SenderKeyDistributionMessage;
import org.whispersystems.libsignal.util.KeyHelper;

/**
 * GroupSessionBuilder is responsible for setting up group SenderKey encrypted sessions.
 * <p>
 * Once a session has been established, {@link org.whispersystems.libsignal.groups.GroupCipher}
 * can be used to encrypt/decrypt messages in that session.
 * <p>
 * The built sessions are unidirectional: they can be used either for sending or for receiving,
 * but not both.
 * <p>
 * Sessions are constructed per (groupId + senderId + deviceId) tuple.  Remote logical users
 * are identified by their senderId, and each logical recipientId can have multiple physical
 * devices.
 *
 * @author Moxie Marlinspike
 */

public class GroupSessionBuilder {

    private final SenderKeyStore senderKeyStore;
    private String username;

    public GroupSessionBuilder(SenderKeyStore senderKeyStore) {
        this.senderKeyStore = senderKeyStore;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Construct a group session for receiving messages from senderKeyName.
     *
     * @param senderKeyName                The (groupId, senderId, deviceId) tuple associated with the SenderKeyDistributionMessage.
     * @param senderKeyDistributionMessage A received SenderKeyDistributionMessage.
     */
    public void process(SenderKeyName senderKeyName, SenderKeyDistributionMessage senderKeyDistributionMessage) {
        try {
            KeyLockUtil.lock(username);
            SenderKeyRecord senderKeyRecord = senderKeyStore.loadSenderKey(senderKeyName);
            senderKeyRecord.addSenderKeyState(senderKeyDistributionMessage.getId(),
                    senderKeyDistributionMessage.getIteration(),
                    senderKeyDistributionMessage.getChainKey(),
                    senderKeyDistributionMessage.getSignatureKey());
            senderKeyStore.storeSenderKey(senderKeyName, senderKeyRecord);
        } finally {
            KeyLockUtil.unlock(username);
        }
    }

    /**
     * 创建senderKey
     *
     * @return
     */
    public SenderKeyRecord createSenderKeyRecord() {
        SenderKeyRecord senderKeyRecord = new SenderKeyRecord();
        senderKeyRecord.setSenderKeyState(KeyHelper.generateSenderKeyId(),
                0,
                KeyHelper.generateSenderKey(),
                KeyHelper.generateSenderSigningKey());
        return senderKeyRecord;
    }

    /**
     * Construct a group session for sending messages.
     *
     * @param senderKeyName The (groupId, senderId, deviceId) tuple.  In this case, 'senderId' should be the caller.
     * @return A SenderKeyDistributionMessage that is individually distributed to each member of the group.
     */
    public SenderKeyDistributionMessage create(SenderKeyName senderKeyName) {
        try {
            KeyLockUtil.lock(username);
            SenderKeyRecord senderKeyRecord = senderKeyStore.loadSenderKey(senderKeyName);

            if (senderKeyRecord.isEmpty()) {
                senderKeyRecord.setSenderKeyState(KeyHelper.generateSenderKeyId(),
                        0,
                        KeyHelper.generateSenderKey(),
                        KeyHelper.generateSenderSigningKey());
                senderKeyStore.storeSenderKey(senderKeyName, senderKeyRecord);
            }

            SenderKeyState state = senderKeyRecord.getSenderKeyState();

            return new SenderKeyDistributionMessage(state.getKeyId(),
                    state.getSenderChainKey().getIteration(),
                    state.getSenderChainKey().getSeed(),
                    state.getSigningKeyPublic());

        } catch (InvalidKeyIdException | InvalidKeyException e) {
            throw new AssertionError(e);
        } finally {
            KeyLockUtil.unlock(username);
        }

    }
}
