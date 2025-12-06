package axolotl.store;

import axolotl.AxolotlManager;
import com.whatsapp.android.entity.response.profile.TcToken;
import com.whatsapp.android.util.KeyLockUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Slf4j
public class TrustedContactStore {
    AxolotlManager axolotlManager_;
    private final String username;

    public TrustedContactStore(AxolotlManager axolotlManager, String username) {
        axolotlManager_ = axolotlManager;
        this.username = username;
    }

    public void setToken(String jid, byte[] token) {
        String b64Value = Base64.getEncoder().encodeToString(token);
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager_.GetPreparedStatement("INSERT OR REPLACE INTO trusted_contact_store(jid, token) VALUES(?, ?)");
            preparedStatement.setString(1, jid);
            preparedStatement.setString(2, b64Value);
            preparedStatement.execute();
        } catch (Exception ignored) {
        } finally {
            axolotlManager_.closePreparedStatement(preparedStatement);
        }
    }

    /**
     * 批量插入token
     */
    public void batchInsertToken(List<TcToken> tokens) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager_.GetPreparedStatement("INSERT OR REPLACE INTO trusted_contact_store(jid, token) VALUES(?, ?)");
            for (TcToken tcToken : tokens) {
                preparedStatement.setString(1, tcToken.getUserId());
                preparedStatement.setString(2, tcToken.getTcToken());
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
        } catch (Exception e) {
            log.error("批量插入tcToken异常", e);
        } finally {
            axolotlManager_.closePreparedStatement(preparedStatement);
        }
    }

    public byte[] getToken(String jid) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager_.GetPreparedStatement("SELECT token FROM trusted_contact_store WHERE jid = ?");
            preparedStatement.setString(1, jid);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (rs.next()) {
                    String value = rs.getString("token");
                    if (StringUtils.hasLength(value)) {
                        return Base64.getDecoder().decode(value);
                    }
                }
            } catch (Exception ignored) {
            }
        } catch (Exception ignored) {
        } finally {
            axolotlManager_.closePreparedStatement(preparedStatement);
        }
        return null;
    }

    public List<TcToken> getContactsTcToken() {
        List<TcToken> list = new ArrayList<>();
        PreparedStatement preparedStatement = null;
        try {
            KeyLockUtil.lock(username);
            try {
                int pageSize = 1000;
                int offset = 0;
                boolean hasMoreData = true;
                while (hasMoreData) {
                    preparedStatement = axolotlManager_.GetPreparedStatement("select jid, token from trusted_contact_store limit ? offset ?");
                    preparedStatement.setInt(1, pageSize);
                    preparedStatement.setInt(2, offset);

                    try (ResultSet result = preparedStatement.executeQuery()) {
                        int count = 0;
                        while (result.next()) {
                            String recipient = result.getString("jid");
                            String tcToken = result.getString("token");
                            list.add(new TcToken(recipient, tcToken));
                            count++;
                        }
                        // 如果查询结果少于pageSize，说明已经查完了
                        hasMoreData = (count == pageSize);
                        offset += pageSize;
                    } catch (Exception ignored) {
                        hasMoreData = false; // 出现异常时停止循环
                    } finally {
                        axolotlManager_.closePreparedStatement(preparedStatement);
                        preparedStatement = null; // 重置以便下次循环使用
                    }
                }
            } catch (Exception e) {
                log.error(e.getMessage());
            } finally {
                if (preparedStatement != null) {
                    axolotlManager_.closePreparedStatement(preparedStatement);
                }
            }
        } catch (Exception ignored) {
        } finally {
            KeyLockUtil.unlock(username);
        }
        return list;
    }
}
