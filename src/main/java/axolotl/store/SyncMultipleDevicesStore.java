package axolotl.store;

import axolotl.AxolotlManager;
import lombok.extern.slf4j.Slf4j;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class SyncMultipleDevicesStore {
    private final AxolotlManager axolotlManager_;

    public SyncMultipleDevicesStore(AxolotlManager axolotlManager) {
        axolotlManager_ = axolotlManager;
    }

    public void insert(String recipientId) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager_.GetPreparedStatement("INSERT OR REPLACE INTO sync_multiple_devices(recipient_id) VALUES(?)");
            preparedStatement.setString(1, recipientId);
            preparedStatement.execute();
        } catch (Exception ignored) {
        } finally {
            axolotlManager_.closePreparedStatement(preparedStatement);
        }
    }

    public void insert(List<String> recipientIds) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager_.GetPreparedStatement("INSERT OR REPLACE INTO sync_multiple_devices(recipient_id) VALUES(?)");
            for (String recipientId : recipientIds) {
                preparedStatement.setString(1, recipientId);
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
        } catch (Exception ignored) {
        } finally {
            axolotlManager_.closePreparedStatement(preparedStatement);
        }
    }

    public List<String> batchQueryList(List<String> recipientIds) {
        List<String> list = new ArrayList<>();
        if (recipientIds == null || recipientIds.isEmpty()) {
            return list;
        }
        PreparedStatement preparedStatement = null;
        try {
            // 动态构建SQL查询语句
            StringBuilder sqlBuilder = new StringBuilder("SELECT recipient_id FROM sync_multiple_devices WHERE recipient_id IN (");
            for (int i = 0; i < recipientIds.size(); i++) {
                sqlBuilder.append("?");
                if (i < recipientIds.size() - 1) {
                    sqlBuilder.append(",");
                }
            }
            sqlBuilder.append(")");
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
            log.error("用户: {}, 批量查询多设备列表异常", axolotlManager_.getUserName_(), e);
        } finally {
            axolotlManager_.closePreparedStatement(preparedStatement);
        }
        return list;
    }

    public int getCount(String recipientId) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager_.GetPreparedStatement("select COUNT(*) from sync_multiple_devices where recipient_id = ?");
            preparedStatement.setString(1, recipientId);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
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
