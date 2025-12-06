package axolotl.store;

import axolotl.AxolotlManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class PkMsgSyncContactStore {
    AxolotlManager axolotlManager_;

    public PkMsgSyncContactStore(AxolotlManager axolotlManager) {
        axolotlManager_ = axolotlManager;
    }

    public void insert(String recipientId, int status) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager_.GetPreparedStatement("INSERT OR REPLACE INTO pk_msg_sync_contact_store(recipient_id, status) VALUES(?, ?)");
            preparedStatement.setString(1, recipientId);
            preparedStatement.setInt(2, status);
            preparedStatement.execute();
        } catch (Exception ignored) {
        } finally {
            axolotlManager_.closePreparedStatement(preparedStatement);
        }
    }

    /**
     * 获取pkMsg同步状态
     *
     * @param recipientId 粉丝id
     * @return 0未同步，1已同步，2已发送
     */
    public int getPkMsgSyncStatus(String recipientId) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager_.GetPreparedStatement("SELECT status FROM pk_msg_sync_contact_store WHERE recipient_id = ?");
            preparedStatement.setString(1, recipientId);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("status");
                }
            } catch (Exception ignored) {
            }
        } catch (Exception ignored) {
        } finally {
            axolotlManager_.closePreparedStatement(preparedStatement);
        }
        return 0;
    }

}
