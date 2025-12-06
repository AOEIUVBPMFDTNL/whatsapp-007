package axolotl.store;

import Util.StringUtil;
import axolotl.AxolotlManager;
import lombok.extern.slf4j.Slf4j;
import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.state.SessionRecord;
import org.whispersystems.libsignal.state.SessionStore;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class SignalSessionStore implements SessionStore {
    AxolotlManager axolotlManager_;


    public SignalSessionStore(AxolotlManager axolotlManager) {
        axolotlManager_ = axolotlManager;
    }

    @Override
    public SessionRecord loadSession(SignalProtocolAddress address) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager_.GetPreparedStatement("select record from sessions where recipient_id = ? AND device_id = ?");
            preparedStatement.setString(1, address.getName());
            preparedStatement.setInt(2, address.getDeviceId());
            try (ResultSet result = preparedStatement.executeQuery()) {
                if (result.next()) {
                    return new SessionRecord(result.getBytes(1));
                }
            } catch (Exception ignored) {
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            axolotlManager_.closePreparedStatement(preparedStatement);
        }
        return new SessionRecord();
    }

    @Override
    public List<Integer> getSubDeviceSessions(String name) {
        PreparedStatement preparedStatement = null;
        try {
            List<Integer> subDevice = new ArrayList<>();
            preparedStatement = axolotlManager_.GetPreparedStatement("SELECT device_id from sessions WHERE recipient_id = ?");
            preparedStatement.setString(1, name);
            try (ResultSet result = preparedStatement.executeQuery()) {
                while (result.next()) {
                    subDevice.add(result.getInt(1));
                }
            } catch (Exception ignored) {
            }
            return subDevice;
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            axolotlManager_.closePreparedStatement(preparedStatement);
        }
        return null;
    }

    @Override
    public void storeSession(SignalProtocolAddress address, SessionRecord record) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager_.GetPreparedStatement("INSERT OR REPLACE INTO sessions (recipient_id, device_id, record, timestamp) VALUES(?,?, ?,?)");
            preparedStatement.setString(1, address.getName());
            preparedStatement.setInt(2, address.getDeviceId());
            preparedStatement.setBytes(3, record.serialize());
            preparedStatement.setLong(4, System.currentTimeMillis() / 1000);
            preparedStatement.execute();
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            axolotlManager_.closePreparedStatement(preparedStatement);
        }
    }

    @Override
    public boolean containsSession(SignalProtocolAddress address) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager_.GetPreparedStatement("select COUNT(*) from sessions where recipient_id = ? AND device_id = ?");
            preparedStatement.setString(1, address.getName());
            preparedStatement.setInt(2, address.getDeviceId());
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

    /**
     * 获取session数量
     */
    public int containsSessionNum(StringUtil.JidInfo jidInfo) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager_.GetPreparedStatement("select COUNT(*) from sessions where recipient_id = ?");
            preparedStatement.setString(1, jidInfo.recipientId);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            } catch (Exception ignored) {

            }
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            axolotlManager_.closePreparedStatement(preparedStatement);
        }
        return 0;
    }

    public int containsSessionNum(List<String> recipientIds) {
        String ids = "'" + String.join("','", recipientIds).trim() + "'";
        String sql = "select COUNT(*) from sessions where recipient_id in (?)";
        String sqlQuery = String.format(sql.replace("?", "%s"), ids);
        try (ResultSet rs = axolotlManager_.GetStatement().executeQuery(sqlQuery)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception ignored) {

        }
        return 0;
    }

    public boolean containsSession(StringUtil.JidInfo jidInfo) {
        SignalProtocolAddress address = new SignalProtocolAddress(jidInfo.recipientId, jidInfo.deviceId);
        return containsSession(address);
    }

    public List<String> batchQuerySession(List<String> recipientIds) {
        List<String> list = new ArrayList<>();
        if (recipientIds == null || recipientIds.isEmpty()) {
            return list;
        }
        PreparedStatement preparedStatement = null;
        try {
            // 动态构建SQL查询语句
            StringBuilder sqlBuilder = new StringBuilder("SELECT recipient_id FROM sessions WHERE recipient_id IN (");
            for (int i = 0; i < recipientIds.size(); i++) {
                sqlBuilder.append("?");
                if (i < recipientIds.size() - 1) {
                    sqlBuilder.append(",");
                }
            }
            sqlBuilder.append(") AND device_id = 0");
            String sql = sqlBuilder.toString();
            preparedStatement = axolotlManager_.GetPreparedStatement(sql);
            // 设置查询参数
            for (int i = 0; i < recipientIds.size(); i++) {
                preparedStatement.setString(i + 1, recipientIds.get(i));
            }
            // 执行查询并获取结果
            ResultSet rs = preparedStatement.executeQuery();
            // 处理查询结果
            while (rs.next()) {
                list.add(rs.getString("recipient_id"));
            }
        } catch (Exception e) {
            log.error("用户: {}, 批量查询session异常", axolotlManager_.getUserName_(), e);
        } finally {
            axolotlManager_.closePreparedStatement(preparedStatement);
        }
        return list;
    }

    @Override
    public void deleteSession(SignalProtocolAddress address) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager_.GetPreparedStatement("delete from sessions where recipient_id = ? AND device_id = ?");
            preparedStatement.setString(1, address.getName());
            preparedStatement.setInt(2, address.getDeviceId());
            preparedStatement.execute();
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            axolotlManager_.closePreparedStatement(preparedStatement);
        }
    }

    @Override
    public void deleteAllSessions(String name) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager_.GetPreparedStatement("delete from sessions where recipient_id = ?");
            preparedStatement.setString(1, name);
            preparedStatement.execute();
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            axolotlManager_.closePreparedStatement(preparedStatement);
        }
    }
}
