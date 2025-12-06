package axolotl.store;

import axolotl.AxolotlManager;
import lombok.extern.slf4j.Slf4j;
import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.state.IdentityKeyStore;
import org.whispersystems.libsignal.util.KeyHelper;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

@Slf4j
public class SignalIdentityKeyStore implements IdentityKeyStore {
    AxolotlManager axolotlManager_;
    int localRegistrationId_ = -1;
    IdentityKeyPair identityKeyPair_;

    public SignalIdentityKeyStore(AxolotlManager axolotlManager) {
        axolotlManager_ = axolotlManager;
        {
            try (ResultSet rs = axolotlManager_.GetStatement().executeQuery("SELECT registration_id, public_key, private_key FROM identities WHERE recipient_id = -1")) {
                if (rs.next()) {
                    localRegistrationId_ = rs.getInt("registration_id");
                    byte[] pubKey = rs.getBytes("public_key");
                    byte[] priKey = rs.getBytes("private_key");
                    identityKeyPair_ = new IdentityKeyPair(new IdentityKey(pubKey, 0), KeyHelper.decodePrivateKey(priKey));
                }
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
    }

    @Override
    public IdentityKeyPair getIdentityKeyPair() {
        return identityKeyPair_;
    }

    public void clearIdentityKeyPair() {
        this.identityKeyPair_ = null;
    }

    @Override
    public int getLocalRegistrationId() {
        return localRegistrationId_;
    }

    @Override
    public boolean saveIdentity(SignalProtocolAddress address, IdentityKey identityKey) {
        PreparedStatement preparedStatement = null;
        DeleteIdentity(address);
        try {
            preparedStatement = axolotlManager_.GetPreparedStatement("INSERT INTO identities (recipient_id, device_id, public_key, timestamp) VALUES(?, ?, ?, ?)");
            preparedStatement.setString(1, address.getName());
            preparedStatement.setInt(2, address.getDeviceId());
            preparedStatement.setBytes(3, identityKey.serialize());
            preparedStatement.setLong(4, System.currentTimeMillis() / 1000);
            preparedStatement.execute();
            return true;
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            axolotlManager_.closePreparedStatement(preparedStatement);
        }
        return false;
    }

    @Override
    public boolean isTrustedIdentity(SignalProtocolAddress address, IdentityKey identityKey, Direction direction) {
        /*IdentityKey storeIdentityKey = getIdentity(address);
        if (null == storeIdentityKey) {
            return true;
        }
        return storeIdentityKey.equals(identityKey);*/
        IdentityKey storeIdentityKey = getIdentity(address);
        if (null == storeIdentityKey) {
            return true;
        }
        boolean trusted = storeIdentityKey.equals(identityKey);
        if (!trusted) {
            saveIdentity(address, identityKey);
        }
        return true;

    }

    @Override
    public IdentityKey getIdentity(SignalProtocolAddress address) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager_.GetPreparedStatement("SELECT public_key from identities where recipient_id =? and device_id=?");
            preparedStatement.setString(1, address.getName());
            preparedStatement.setInt(2, address.getDeviceId());
            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (rs.next()) {
                    return new IdentityKey(rs.getBytes("public_key"), 0);
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

    void DeleteIdentity(SignalProtocolAddress address) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager_.GetPreparedStatement("delete from identities where recipient_id =? and device_id=?");
            preparedStatement.setString(1, address.getName());
            preparedStatement.setInt(2, address.getDeviceId());
            preparedStatement.execute();
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            axolotlManager_.closePreparedStatement(preparedStatement);
        }
    }

    public void updateRegistrationId(int registrationId) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager_.GetPreparedStatement("update identities set registration_id = ? where recipient_id = -1");
            preparedStatement.setInt(1, registrationId);
            preparedStatement.execute();
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            axolotlManager_.closePreparedStatement(preparedStatement);
        }
    }
}
