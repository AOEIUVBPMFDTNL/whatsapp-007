package axolotl.store;

import axolotl.AxolotlManager;
import lombok.extern.slf4j.Slf4j;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * @author sunnoc
 * @date 2021-04-15 16:23
 */
@Slf4j
public class GroupSendMsgStore {

    AxolotlManager axolotlManager_;

    public GroupSendMsgStore(AxolotlManager axolotlManager) {
        axolotlManager_ = axolotlManager;
    }

    public boolean insert(String msgId, String encData) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager_.GetPreparedStatement("INSERT OR REPLACE INTO group_send_msg_record (msg_id, enc_data) VALUES(?,?)");
            preparedStatement.setString(1, msgId);
            preparedStatement.setString(2, encData);
            return preparedStatement.execute();
        } catch (Exception e) {
            log.error("插入发送群消息异常", e);
            return false;
        } finally {
            axolotlManager_.closePreparedStatement(preparedStatement);
        }
    }

    public String getEncInfo(String msgId) {
        String sql = "SELECT enc_data AS total FROM group_send_msg_record WHERE msg_id = ?";
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager_.GetPreparedStatement(sql);
            preparedStatement.setString(1, msgId);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (rs.next()) {
                    String encData = rs.getString(1);
                    log.info("getEncInfo:{}", encData);
                    return encData;
                }
            } catch (Exception ignored) {
            }
            return null;
        } catch (Exception e) {
            return null;
        } finally {
            axolotlManager_.closePreparedStatement(preparedStatement);
        }
    }
}
