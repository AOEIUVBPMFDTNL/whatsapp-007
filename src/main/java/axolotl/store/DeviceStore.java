package axolotl.store;

import axolotl.AxolotlManager;
import org.whispersystems.libsignal.state.PreKeyRecord;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedList;
import java.util.List;

public class DeviceStore {
    AxolotlManager axolotlManager_;
    public DeviceStore(AxolotlManager axolotlManager) {
        axolotlManager_ = axolotlManager;
    }

    public List<Integer> GetAdvKeyIndex() {
        LinkedList<Integer> results = new LinkedList<>();
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager_.GetPreparedStatement("SELECT adv_key_index FROM devices");
            try (ResultSet rs = preparedStatement.executeQuery()) {
                while (rs.next()) {
                    results.add(rs.getInt(1));
                }
            } catch (Exception ignored) {
            }
        } catch (Exception e) {

        } finally {
            axolotlManager_.closePreparedStatement(preparedStatement);
        }
        return results;
    }

    public void InsertDevice(String deviceId, int adv_key_index) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager_.GetPreparedStatement("INSERT INTO devices (device_id, adv_key_index) VALUES(?, ?)");
            preparedStatement.setString(1, deviceId);
            preparedStatement.setInt(2, adv_key_index);
            preparedStatement.execute();
        } catch (Exception e) {
        } finally {
            axolotlManager_.closePreparedStatement(preparedStatement);
        }
    }

    public void ClearDevice() {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager_.GetPreparedStatement("delete from devices");
            preparedStatement.execute();
        } catch (Exception e) {
        } finally {
            axolotlManager_.closePreparedStatement(preparedStatement);
        }
    }
}
