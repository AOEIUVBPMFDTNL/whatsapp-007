package axolotl.store;

import axolotl.AxolotlManager;
import com.whatsapp.android.entity.JidMap;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

public class JidMapStore {
    AxolotlManager axolotlManager_;

    public JidMapStore(AxolotlManager axolotlManager) {
        this.axolotlManager_ = axolotlManager;
    }

    public void InsertJidLid(String rawJid, String rawLid) {
        PreparedStatement preparedStatement = null;
        DeleteJidLid(rawJid, rawLid);
        try {
            preparedStatement = axolotlManager_.GetPreparedStatement("INSERT OR REPLACE INTO jid_map (jid, lid) VALUES(?, ?)");
            preparedStatement.setString(1, rawJid);
            preparedStatement.setString(2, rawLid);
            preparedStatement.execute();
        } catch (Exception e) {
        } finally {
            axolotlManager_.closePreparedStatement(preparedStatement);
        }
    }

    public void batchInsert(List<JidMap> jidMaps) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager_.GetPreparedStatement("INSERT OR REPLACE INTO jid_map (jid, lid) VALUES(?, ?)");
            for (JidMap jidMap : jidMaps) {
                preparedStatement.setString(1, jidMap.getJid());
                preparedStatement.setString(2, jidMap.getLid());
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
        } catch (Exception ignored) {
        } finally {
            axolotlManager_.closePreparedStatement(preparedStatement);
        }
    }

    public void DeleteJidLid(String rawJid, String rawLid) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager_.GetPreparedStatement("delete from jid_map where jid =? and lid=?;");
            preparedStatement.setString(1, rawJid);
            preparedStatement.setString(2, rawLid);
            preparedStatement.execute();
        } catch (Exception e) {
        } finally {
            axolotlManager_.closePreparedStatement(preparedStatement);
        }
    }

    public void DeleteJidLid(String rawJid) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager_.GetPreparedStatement("delete from jid_map where jid =?;");
            preparedStatement.setString(1, rawJid);
            preparedStatement.execute();
        } catch (Exception e) {
        } finally {
            axolotlManager_.closePreparedStatement(preparedStatement);
        }
    }

    public String GetJid(String rawLid) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager_.GetPreparedStatement("select jid from jid_map where lid = ?");
            preparedStatement.setString(1, rawLid);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            } catch (Exception ignored) {
            }
        } catch (Exception ignored) {
        } finally {
            axolotlManager_.closePreparedStatement(preparedStatement);
        }
        return "";
    }

    public String GetLid(String rawJid) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager_.GetPreparedStatement("select lid from jid_map where jid = ?");
            preparedStatement.setString(1, rawJid);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            } catch (Exception ignored) {
            }
        } catch (Exception ignored) {
        } finally {
            axolotlManager_.closePreparedStatement(preparedStatement);
        }
        return "";
    }


}
