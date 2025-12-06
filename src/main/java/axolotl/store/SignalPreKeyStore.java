package axolotl.store;

import axolotl.AxolotlManager;
import cn.hutool.core.util.RandomUtil;
import lombok.extern.slf4j.Slf4j;
import org.whispersystems.libsignal.state.PreKeyRecord;
import org.whispersystems.libsignal.state.PreKeyStore;
import org.whispersystems.libsignal.util.KeyHelper;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedList;
import java.util.List;

@Slf4j
public class SignalPreKeyStore implements PreKeyStore {
    AxolotlManager axolotlManager_;

    public SignalPreKeyStore(AxolotlManager axolotlManager) {
        axolotlManager_ = axolotlManager;
    }

    @Override
    public PreKeyRecord loadPreKey(int preKeyId) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager_.GetPreparedStatement("select record  from prekeys where prekey_id =?");
            preparedStatement.setInt(1, preKeyId);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (rs.next()) {
                    return new PreKeyRecord(rs.getBytes(1));
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

    public void setAsSent(LinkedList<Integer> sentIds) {
        axolotlManager_.SetAutoCommit(false);
        try {
            for (Integer sentId : sentIds) {
                PreparedStatement preparedStatement = null;
                try {
                    preparedStatement = axolotlManager_.GetPreparedStatement("UPDATE prekeys SET sent_to_server = ?, upload_timestamp=? WHERE prekey_id = ?");
                    preparedStatement.setInt(1, 1);
                    preparedStatement.setLong(2, System.currentTimeMillis() / 1000);
                    preparedStatement.setInt(3, sentId);
                    preparedStatement.execute();
                } catch (Exception e) {
                    log.error(e.getMessage());
                } finally {
                    axolotlManager_.closePreparedStatement(preparedStatement);
                }
            }
            axolotlManager_.Commit();
        } catch (Exception ignore) {
            axolotlManager_.Rollback();
        } finally {
            axolotlManager_.SetAutoCommit(true);
        }
    }

    public LinkedList<PreKeyRecord> LoadUnSendPreKey() {
        LinkedList<PreKeyRecord> results = new LinkedList<>();
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager_.GetPreparedStatement("SELECT record FROM prekeys WHERE sent_to_server is NULL or sent_to_server = 0");
            try (ResultSet rs = preparedStatement.executeQuery()) {
                while (rs.next()) {
                    results.add(new PreKeyRecord(rs.getBytes(1)));
                }
            } catch (Exception ignored) {
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            axolotlManager_.closePreparedStatement(preparedStatement);
        }
        return results;
    }

    public int getPendingPreKeysCount() {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager_.GetPreparedStatement("SELECT count(*) FROM prekeys WHERE sent_to_server = 1");
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

    public PreKeyRecord GetOnePreKeyRecord() {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager_.GetPreparedStatement("SELECT * FROM prekeys WHERE sent_to_server = 1 order by _id desc limit 1");
            try (ResultSet rs = preparedStatement.executeQuery()) {
                PreKeyRecord record = new PreKeyRecord(rs.getBytes("record"));
                return record;
            } catch (Exception ignored) {
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            axolotlManager_.closePreparedStatement(preparedStatement);
        }
        return null;
    }

    public int GetUnsentCount() {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager_.GetPreparedStatement("SELECT count(*) FROM prekeys WHERE sent_to_server is NULL or sent_to_server = 0");
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


    @Override
    public void storePreKey(int preKeyId, PreKeyRecord record) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager_.GetPreparedStatement("INSERT INTO prekeys (prekey_id, sent_to_server, record, direct_distribution) VALUES(?, ?, ?, ?)");
            preparedStatement.setInt(1, preKeyId);
            preparedStatement.setBoolean(2, false);
            preparedStatement.setBytes(3, record.serialize());
            preparedStatement.setLong(4, 0);
            preparedStatement.execute();
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            axolotlManager_.closePreparedStatement(preparedStatement);
        }
    }

    @Override
    public boolean containsPreKey(int preKeyId) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager_.GetPreparedStatement("select COUNT(*)  from prekeys where prekey_id =?");
            preparedStatement.setInt(1, preKeyId);
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
    public void removePreKey(int preKeyId) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager_.GetPreparedStatement("delete from prekeys where prekey_id =?");
            preparedStatement.setInt(1, preKeyId);
            preparedStatement.execute();
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            axolotlManager_.closePreparedStatement(preparedStatement);
        }
    }

    public int getMaxPreKeyId() {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager_.GetPreparedStatement("SELECT max(prekey_id) FROM prekeys");
            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    if (id == 0) {
                        id = RandomUtil.randomInt(0x100000, 0xffffff);
                    }
                    return id;
                }
            } catch (Exception ignored) {
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            axolotlManager_.closePreparedStatement(preparedStatement);
        }
        return RandomUtil.randomInt(0x100000, 0xffffff);
    }

    public List<PreKeyRecord> generatePreKeyAndStore(int startId, int count) {
        //生成812个
        List<PreKeyRecord> preKeyRecords = KeyHelper.generatePreKeys(startId + 1, count);
        //保存到数据库
        axolotlManager_.SetAutoCommit(false);
        try {
            for (PreKeyRecord record : preKeyRecords) {
                storePreKey(record.getId(), record);
            }
            axolotlManager_.Commit();
        } catch (Exception ignore) {
            axolotlManager_.Rollback();
        } finally {
            axolotlManager_.SetAutoCommit(true);
        }
        return preKeyRecords;
    }
}
