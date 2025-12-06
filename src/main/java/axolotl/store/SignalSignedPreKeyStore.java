package axolotl.store;

import axolotl.AxolotlManager;
import lombok.extern.slf4j.Slf4j;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;
import org.whispersystems.libsignal.state.SignedPreKeyStore;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class SignalSignedPreKeyStore implements SignedPreKeyStore {
    AxolotlManager axolotlManager_;

    public SignalSignedPreKeyStore(AxolotlManager axolotlManager) {
        axolotlManager_ = axolotlManager;
    }

    @Override
    public SignedPreKeyRecord loadSignedPreKey(int signedPreKeyId) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager_.GetPreparedStatement("select record  from signed_prekeys where prekey_id =?");
            preparedStatement.setInt(1, signedPreKeyId);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (rs.next()) {
                    return new SignedPreKeyRecord(rs.getBytes(1));
                }
            } catch (Exception ignored) {
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            axolotlManager_.closePreparedStatement(preparedStatement);
        }
        return null;
    }

    @Override
    public List<SignedPreKeyRecord> loadSignedPreKeys() {
        Statement statement = null;
        try {
            List<SignedPreKeyRecord> results = new ArrayList<>();
            statement = axolotlManager_.GetStatement();
            try (ResultSet rs = statement.executeQuery("select record  from signed_prekeys")) {
                while (rs.next()) {
                    results.add(new SignedPreKeyRecord(rs.getBytes(1)));
                }
            } catch (Exception ignored) {
            }
            return results;
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    @Override
    public void storeSignedPreKey(int signedPreKeyId, SignedPreKeyRecord record) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager_.GetPreparedStatement("INSERT OR REPLACE INTO signed_prekeys (prekey_id,  timestamp, record) VALUES(?,?,?)");
            preparedStatement.setInt(1, signedPreKeyId);
            preparedStatement.setLong(2, System.currentTimeMillis() / 1000);
            preparedStatement.setBytes(3, record.serialize());
            preparedStatement.execute();
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            axolotlManager_.closePreparedStatement(preparedStatement);
        }
    }

    @Override
    public boolean containsSignedPreKey(int signedPreKeyId) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager_.GetPreparedStatement("select COUNT(*) from signed_prekeys where prekey_id =?");
            preparedStatement.setInt(1, signedPreKeyId);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) != 0;
                }
            } catch (Exception ignored) {
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            axolotlManager_.closePreparedStatement(preparedStatement);
        }
        return false;
    }

    @Override
    public void removeSignedPreKey(int signedPreKeyId) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager_.GetPreparedStatement("delete from signed_prekeys where prekey_id = ?");
            preparedStatement.setInt(1, signedPreKeyId);
            preparedStatement.execute();
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            axolotlManager_.closePreparedStatement(preparedStatement);
        }
    }
}
