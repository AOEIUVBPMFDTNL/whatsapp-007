package axolotl.store;

import axolotl.AxolotlManager;
import lombok.extern.slf4j.Slf4j;
import org.whispersystems.libsignal.groups.SenderKeyName;
import org.whispersystems.libsignal.groups.state.FastRatchetSenderKeyRecord;
import org.whispersystems.libsignal.groups.state.FastRatchetSenderKeyStore;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

@Slf4j
public class SignalFastRatchetSenderKeyStore implements FastRatchetSenderKeyStore {
    AxolotlManager axolotlManager_;

    public SignalFastRatchetSenderKeyStore(AxolotlManager axolotlManager) {
        axolotlManager_ = axolotlManager;
    }

    @Override
    public void storeFastRatchetSenderKey(SenderKeyName senderKeyName, FastRatchetSenderKeyRecord record) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager_.GetPreparedStatement("INSERT OR REPLACE INTO fast_ratchet_sender_keys (group_id, sender_id, device_id, record, timestamp) VALUES(?,?, ?,?,?)");
            preparedStatement.setString(1, senderKeyName.getGroupId());
            preparedStatement.setString(2, senderKeyName.getSender().getName());
            preparedStatement.setInt(3, senderKeyName.getSender().getDeviceId());
            preparedStatement.setBytes(4, record.serialize());
            preparedStatement.setLong(5, System.currentTimeMillis() / 1000);
            preparedStatement.execute();
        } catch (Exception e) {
            log.debug(e.getMessage());
        } finally {
            axolotlManager_.closePreparedStatement(preparedStatement);
        }
    }

    @Override
    public FastRatchetSenderKeyRecord loadFastRatchetSenderKey(SenderKeyName senderKeyName) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager_.GetPreparedStatement("select record from fast_ratchet_sender_keys where group_id = ? AND sender_id = ? AND device_id = ?");
            preparedStatement.setString(1, senderKeyName.getGroupId());
            preparedStatement.setString(2, senderKeyName.getSender().getName());
            preparedStatement.setInt(3, senderKeyName.getSender().getDeviceId());
            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (rs.next()) {
                    return new FastRatchetSenderKeyRecord(rs.getBytes(1));
                }
            } catch (Exception ignored) {
            }
        } catch (Exception e) {
            log.debug(e.getMessage());
        } finally {
            axolotlManager_.closePreparedStatement(preparedStatement);
        }
        return null;
    }
}
