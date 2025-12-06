package axolotl.store;

import Util.StringUtil;
import axolotl.AxolotlManager;
import com.whatsapp.android.util.KeyLockUtil;
import lombok.extern.slf4j.Slf4j;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ContactSyncStore {
    private final AxolotlManager axolotlManager;
    private final String username;


    public ContactSyncStore(AxolotlManager axolotlManager, String username) {
        this.axolotlManager = axolotlManager;
        this.username = username;
    }

    public List<String> getContacts(String recipientId) {
        List<String> list = new ArrayList<>();
        PreparedStatement preparedStatement = null;
        try {
            KeyLockUtil.lock(username);
            try {
                preparedStatement = axolotlManager.GetPreparedStatement("select recipient_id,device_id from contact_sync_store where recipient_id = ?");
                preparedStatement.setString(1, recipientId);
                try (ResultSet result = preparedStatement.executeQuery()) {
                    while (result.next()) {
                        String recipient = result.getString("recipient_id");
                        int deviceId = result.getInt("device_id");
                        String userId = String.format("%s.0:%d@%s", recipient, deviceId, "s.whatsapp.net");
                        list.add(userId);
                    }
                } catch (Exception ignored) {
                }
            } catch (Exception e) {
                log.error(e.getMessage());
            } finally {
                axolotlManager.closePreparedStatement(preparedStatement);
            }
        } catch (Exception ignored) {
        } finally {
            KeyLockUtil.unlock(username);
        }
        return list;
    }

    public void batchInsertSyncContact(List<StringUtil.JidInfo> jidInfos) {
        try {
            KeyLockUtil.lock(username);
            PreparedStatement preparedStatement = null;
            try {
                preparedStatement = axolotlManager.GetPreparedStatement("INSERT OR REPLACE INTO contact_sync_store (recipient_id,device_id) VALUES (?,?)");
                for (StringUtil.JidInfo jidInfo : jidInfos) {
                    preparedStatement.setString(1, jidInfo.recipientId);
                    preparedStatement.setInt(2, jidInfo.deviceId);
                    preparedStatement.addBatch();
                }
                preparedStatement.executeBatch();
            } catch (Exception e) {
                log.error("批量插入同步联系人异常", e);
            } finally {
                axolotlManager.closePreparedStatement(preparedStatement);
            }
        } catch (Exception ignored) {

        } finally {
            KeyLockUtil.unlock(username);
        }
    }
}
