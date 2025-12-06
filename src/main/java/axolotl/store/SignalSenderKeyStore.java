package axolotl.store;

import axolotl.AxolotlManager;
import cn.hutool.core.util.HexUtil;
import lombok.extern.slf4j.Slf4j;
import org.whispersystems.libsignal.InvalidKeyIdException;
import org.whispersystems.libsignal.groups.SenderKeyName;
import org.whispersystems.libsignal.groups.ratchet.SenderChainKey;
import org.whispersystems.libsignal.groups.state.SenderKeyRecord;
import org.whispersystems.libsignal.groups.state.SenderKeyState;
import org.whispersystems.libsignal.groups.state.SenderKeyStore;
import org.whispersystems.libsignal.state.StorageProtos;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@Slf4j
public class SignalSenderKeyStore implements SenderKeyStore {
    AxolotlManager axolotlManager_;

    public SignalSenderKeyStore(AxolotlManager axolotlManager) {
        axolotlManager_ = axolotlManager;
    }

    @Override
    public void storeSenderKey(SenderKeyName senderKeyName, SenderKeyRecord record) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager_.GetPreparedStatement("INSERT OR REPLACE INTO sender_keys (group_id, sender_id, device_id, record, timestamp) VALUES(?,?, ?,?,?)");
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
    public SenderKeyRecord loadSenderKey(SenderKeyName senderKeyName) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager_.GetPreparedStatement("select record from sender_keys where group_id = ? AND sender_id = ? AND device_id = ?");
            preparedStatement.setString(1, senderKeyName.getGroupId());
            preparedStatement.setString(2, senderKeyName.getSender().getName());
            preparedStatement.setInt(3, senderKeyName.getSender().getDeviceId());
            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (rs.next()) {
                    return new SenderKeyRecord(rs.getBytes(1));
                }
            } catch (Exception ignored) {
            }
        } catch (Exception e) {
            log.debug(e.getMessage());
        } finally {
            axolotlManager_.closePreparedStatement(preparedStatement);
        }
        return new SenderKeyRecord();
    }

    public void deleteGroupRecord(String groupId) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager_.GetPreparedStatement("delete from sender_keys where group_id = ?");
            preparedStatement.setString(1, groupId);
            preparedStatement.execute();
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            axolotlManager_.closePreparedStatement(preparedStatement);
        }
    }

    public void deleteGroupRecord(String groupId, String jid) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager_.GetPreparedStatement("delete from sender_keys where group_id = ? and sender_id = ?");
            preparedStatement.setString(1, groupId);
            preparedStatement.setString(2, jid);
            preparedStatement.execute();
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            axolotlManager_.closePreparedStatement(preparedStatement);
        }
    }

    public static void main(String[] args) throws IOException, InvalidKeyIdException {
        byte[] bytes = HexUtil.decodeHex("0a730886a0ce8a021224081212206e39a250a321d6538a327739498cc411b9991cb2741dd3b0d930eedbbdbb16f21a450a210561afdd86c392c372e90eea76e57589e4d3b7b8998642bf8c4298b5cd2246366b12200010263a60b30f15e5d4010587f33431923b9dc996fc68b20bbd1ed823a1ff4b");
        SenderKeyRecord senderKeyRecord = new SenderKeyRecord(bytes);
        /*SenderKeyState senderKeyState = senderKeyRecord.getSenderKeyState();
        SenderChainKey senderChainKey = senderKeyState.getSenderChainKey();
        byte[] seed = senderChainKey.getSeed();
        SenderChainKey senderChainKey1 = new SenderChainKey(19, seed);
        senderKeyState.setSenderChainKey(senderChainKey1);*/
        StorageProtos.SenderKeyRecordStructure senderKeyRecordStructure = senderKeyRecord.generateSenderKeyRecordStructure();
        log.info(senderKeyRecordStructure.toString());
        log.info(HexUtil.encodeHexStr(senderKeyRecord.serialize()));

    }
}
