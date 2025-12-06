package axolotl.store;

import axolotl.AxolotlManager;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import lombok.extern.slf4j.Slf4j;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
public class ChatHistoryStore {
    AxolotlManager axolotlManager_;

    public ChatHistoryStore(AxolotlManager axolotlManager_) {
        this.axolotlManager_ = axolotlManager_;
    }

    /**
     * 插入聊天历史
     *
     * @param jid               用户id
     * @param msgId             消息id
     * @param host              远程地址
     * @param msgExpirationDays 消息过期天数
     */
    public void insert(String jid, String msgId, String host, int msgExpirationDays) {
        DateTime dateTime = DateUtil.offsetDay(new Date(), msgExpirationDays);
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager_.GetPreparedStatement("INSERT OR REPLACE INTO chat_history(jid, msg_id, host, timestamp) VALUES(?, ?, ?, ?)");
            preparedStatement.setString(1, jid);
            preparedStatement.setString(2, msgId);
            preparedStatement.setString(3, host);
            preparedStatement.setLong(4, dateTime.getTime() / 1000);
            preparedStatement.execute();
        } catch (Exception ignored) {
        } finally {
            axolotlManager_.closePreparedStatement(preparedStatement);
        }
    }

    public List<String> selectBeforeMessage(String jid) {
        PreparedStatement preparedStatement = null;
        List<String> msgIdList = new ArrayList<>();
        try {
            preparedStatement = axolotlManager_.GetPreparedStatement("SELECT msg_id FROM chat_history WHERE jid = ?");
            preparedStatement.setString(1, jid);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                while (rs.next()) {
                    msgIdList.add(rs.getString("msg_id"));
                }
            } catch (Exception ignored) {
            }
        } catch (Exception ignored) {
        } finally {
            axolotlManager_.closePreparedStatement(preparedStatement);
        }
        return msgIdList;
    }

    /**
     * 获取远程请求地址
     *
     * @param jid   用户id
     * @param msgId 消息id
     * @return 远程请求地址
     */
    public String getHost(String jid, String msgId) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager_.GetPreparedStatement("SELECT host FROM chat_history WHERE jid = ? AND msg_id = ?");
            preparedStatement.setString(1, jid);
            preparedStatement.setString(2, msgId);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("host");
                }
            } catch (Exception ignored) {
            }
        } catch (Exception ignored) {
        } finally {
            axolotlManager_.closePreparedStatement(preparedStatement);
        }
        return null;
    }

    public void deleteExpiredMsg(String jid, String msgId) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager_.GetPreparedStatement("DELETE FROM chat_history WHERE jid = ? AND msg_id = ?");
            preparedStatement.setString(1, jid);
            preparedStatement.setString(2, msgId);
            preparedStatement.execute();
        } catch (Exception e) {
            log.error("用户: {}, 删除指定聊天记录异常", axolotlManager_.getUserName_(), e);
        } finally {
            axolotlManager_.closePreparedStatement(preparedStatement);
        }
    }

    /**
     * 删除过期消息
     */
    public void deleteExpiredMsg(long cleanChatRecordTime) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager_.GetPreparedStatement("DELETE FROM chat_history WHERE timestamp <= ? ");
            preparedStatement.setLong(1, cleanChatRecordTime);
            preparedStatement.execute();
        } catch (Exception e) {
            log.error("用户: {}, 定时删除聊天记录异常", axolotlManager_.getUserName_(), e);
        } finally {
            axolotlManager_.closePreparedStatement(preparedStatement);
        }
    }


}
