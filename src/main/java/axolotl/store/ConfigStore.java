package axolotl.store;

import axolotl.AxolotlManager;
import lombok.extern.slf4j.Slf4j;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Base64;

@Slf4j
public class ConfigStore {
    AxolotlManager axolotlManager_;

    public ConfigStore(AxolotlManager axolotlManager) {
        axolotlManager_ = axolotlManager;
    }

    public void SetSetting(String key, String value) {
        PreparedStatement preparedStatement = null;
        try {
            axolotlManager_.SetAutoCommit(false);
            preparedStatement = axolotlManager_.GetPreparedStatement("INSERT OR REPLACE INTO settings(key, value) VALUES(?, ?)");
            preparedStatement.setString(1, key);
            preparedStatement.setString(2, value);
            preparedStatement.execute();
            axolotlManager_.Commit();
        } catch (Exception e) {
            log.error("SetSetting: {}, exception", key, e);
            axolotlManager_.Rollback();
        } finally {
            axolotlManager_.closePreparedStatement(preparedStatement);
            axolotlManager_.SetAutoCommit(true);
        }
    }

    public String GetSetting(String key) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager_.GetPreparedStatement("SELECT  value from settings where key = ?");
            preparedStatement.setString(1, key);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("value");
                }
            } catch (Exception ignored) {
            }
        } catch (Exception e) {
            log.error("GetSetting: {} ,exception", key, e);
        } finally {
            axolotlManager_.closePreparedStatement(preparedStatement);
        }
        return "";
    }

    public boolean existInit() {
        PreparedStatement preparedStatement = null;
        boolean exists = false;
        try {
            preparedStatement = axolotlManager_.GetPreparedStatement("SELECT EXISTS(SELECT 1 FROM settings WHERE `key` = 'is_init')");
            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (rs.next()) {
                    exists = rs.getInt(1) == 1;
                }
            } catch (Exception ignored) {
            }
        } catch (Exception e) {
            log.error("ExistInit exception", e);
        } finally {
            axolotlManager_.closePreparedStatement(preparedStatement);
        }
        return exists;
    }

    public void delSetting(String key) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager_.GetPreparedStatement("delete from settings where key = ?");
            preparedStatement.setString(1, key);
            preparedStatement.execute();
        } catch (Exception ignored) {
        } finally {
            axolotlManager_.closePreparedStatement(preparedStatement);
        }
    }

    public void SetBytes(String key, byte[] value) {
        String b64Value = Base64.getEncoder().encodeToString(value);
        SetSetting(key, b64Value);
    }

    public byte[] GetBytes(String key) {
        String value = GetSetting(key);
        if (!value.isEmpty()) {
            return Base64.getDecoder().decode(value);
        }
        return null;
    }
}
