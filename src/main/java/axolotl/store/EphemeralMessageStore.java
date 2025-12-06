package axolotl.store;

import axolotl.AxolotlManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class EphemeralMessageStore {
    AxolotlManager axolotlManager_;

    public EphemeralMessageStore(AxolotlManager axolotlManager) {
        axolotlManager_ = axolotlManager;
    }

    public void insert(String jid, int expiration) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager_.GetPreparedStatement("INSERT OR REPLACE INTO ephemeral_message(jid, expiration) VALUES(?, ?)");
            preparedStatement.setString(1, jid);
            preparedStatement.setInt(2, expiration);
            preparedStatement.execute();
        } catch (Exception ignored) {
        } finally {
            axolotlManager_.closePreparedStatement(preparedStatement);
        }
    }

    public int getExpirationTime(String jid) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager_.GetPreparedStatement("SELECT expiration FROM ephemeral_message WHERE jid = ?");
            preparedStatement.setString(1, jid);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("expiration");
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
