package axolotl;

import Env.DeviceEnv;
import Message.WhatsMessage;
import QRcode.AnonymousClass33J;
import QRcode.SyncdKeyId;
import Util.StringUtil;
import axolotl.store.*;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.entity.DeviceInfo;
import com.whatsapp.android.service.impl.init.ContactBookManager;
import com.whatsapp.android.service.impl.init.TaskQueueManager;
import com.whatsapp.android.util.DeviceUtil;
import com.whatsapp.android.util.KeyLockUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.whispersystems.libsignal.*;
import org.whispersystems.libsignal.groups.GroupCipher;
import org.whispersystems.libsignal.groups.GroupSessionBuilder;
import org.whispersystems.libsignal.groups.SenderKeyName;
import org.whispersystems.libsignal.groups.state.SenderKeyRecord;
import org.whispersystems.libsignal.protocol.CiphertextMessage;
import org.whispersystems.libsignal.protocol.PreKeySignalMessage;
import org.whispersystems.libsignal.protocol.SenderKeyDistributionMessage;
import org.whispersystems.libsignal.protocol.SignalMessage;
import org.whispersystems.libsignal.state.PreKeyBundle;
import org.whispersystems.libsignal.state.PreKeyRecord;
import org.whispersystems.libsignal.state.SessionRecord;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;
import org.whispersystems.libsignal.util.KeyHelper;

import java.io.File;
import java.security.SecureRandom;
import java.sql.*;
import java.util.*;

@Slf4j
public class AxolotlManager {
    private java.sql.Connection axolotlManager_ = null;
    @Getter
    SignalIdentityKeyStore identityKeyStore_ = null;
    public SignalPreKeyStore preKeyStore_ = null;
    public SignalSessionStore sessionStore_ = null;
    SignalSignedPreKeyStore signedPreKeyStore_ = null;
    public SignalSenderKeyStore senderKeyStore_ = null;
    SignalFastRatchetSenderKeyStore fastRatchetSenderKeyStore_ = null;
    ConfigStore configStore_ = null;
    DeviceStore deviceStore_ = null;
    HashMap<StringUtil.JidInfo, SessionCipher> sessionCipherHashMap = new HashMap<>();
    HashMap<SenderKeyName, GroupCipher> groupCipherHashMap = new HashMap<>();
    GroupSessionBuilder groupSessionBuilder_;
    public TrustedContactStore trustedContactStore;
    public SyncMultipleDevicesStore syncMultipleDevicesStore;
    public EphemeralMessageStore ephemeralMessageStore;
    public ChatHistoryStore chatHistoryStore;
    public TaskQueueManager taskQueueManager;
    public ContactBookManager contactBookManager;
    public PkMsgSyncContactStore pkMsgSyncContactStore;
    public ContactSyncStore contactSyncStore;
    public JidMapStore jidMapStore;
    @Getter
    String userName_;
    static final int COUNT_GEN_PREKEYS = 812;
    int adv_raw_id_ = 0;

    public AxolotlManager(String dbPath, String username) {
        this.userName_ = username;
        try {
            boolean dbExist = new File(dbPath).exists();
            //连接数据库
            String connectionPath = "jdbc:sqlite:" + dbPath;
            axolotlManager_ = DriverManager.getConnection(connectionPath);

            if (!dbExist) {
                //如果第一次安装创建数据表
                CrateTables();
                //初始化数据
                InitInstallData();
            }
            identityKeyStore_ = new SignalIdentityKeyStore(this);
            preKeyStore_ = new SignalPreKeyStore(this);
            sessionStore_ = new SignalSessionStore(this);
            signedPreKeyStore_ = new SignalSignedPreKeyStore(this);
            senderKeyStore_ = new SignalSenderKeyStore(this);
            fastRatchetSenderKeyStore_ = new SignalFastRatchetSenderKeyStore(this);
            configStore_ = new ConfigStore(this);
            groupSessionBuilder_ = new GroupSessionBuilder(senderKeyStore_);
            deviceStore_ = new DeviceStore(this);
            trustedContactStore = new TrustedContactStore(this, userName_);
            syncMultipleDevicesStore = new SyncMultipleDevicesStore(this);
            ephemeralMessageStore = new EphemeralMessageStore(this);
            chatHistoryStore = new ChatHistoryStore(this);
            taskQueueManager = new TaskQueueManager(this, axolotlManager_);
            contactBookManager = new ContactBookManager(this, axolotlManager_);
            pkMsgSyncContactStore = new PkMsgSyncContactStore(this);
            contactSyncStore = new ContactSyncStore(this, userName_);
            groupSessionBuilder_.setUsername(userName_);
            jidMapStore = new JidMapStore(this);
            initGroupPreFansRecordTable();
            initTrustedContactRecordTable();
            InitWebWhatsappTables();
            initPlatformDeviceInfoRecordTable();
            initSyncMultipleDevicesTable();
            initEphemeralMessageTable();
            initChatHistoryTable();
            initPkMsgSyncContactStoreTable();
            initContactSyncStoreTable();
            initJidMapTable();
            boolean autoCommitStatus = getAutoCommitStatus();
            if (!autoCommitStatus) {
                SetAutoCommit(true);
            }
        } catch (Exception e) {
            log.error("连接sqlite异常：{}", e.getMessage());
        }
    }

    public AxolotlManager(String dbPath, byte[] newEnv) {
        try {
            boolean dbExist = new File(dbPath).exists();
            //连接数据库
            String connectionPath = "jdbc:sqlite:" + dbPath;
            axolotlManager_ = DriverManager.getConnection(connectionPath);
            if (!dbExist) {
                //如果第一次安装创建数据表
                CrateTables();
                //初始化数据
                InitInstallData();
            }
            identityKeyStore_ = new SignalIdentityKeyStore(this);
            preKeyStore_ = new SignalPreKeyStore(this);
            sessionStore_ = new SignalSessionStore(this);
            signedPreKeyStore_ = new SignalSignedPreKeyStore(this);
            senderKeyStore_ = new SignalSenderKeyStore(this);
            fastRatchetSenderKeyStore_ = new SignalFastRatchetSenderKeyStore(this);
            configStore_ = new ConfigStore(this);
            groupSessionBuilder_ = new GroupSessionBuilder(senderKeyStore_);
            if (newEnv != null) {
                SetBytesSetting("env", newEnv);
            }
            byte[] envBuffer = GetBytesSetting("env");
            DeviceEnv.AndroidEnv.Builder builder = DeviceEnv.AndroidEnv.parseFrom(envBuffer).toBuilder();
            userName_ = builder.getFullphone();
            if (StringUtils.isEmpty(userName_)) {
                userName_ = IdUtil.simpleUUID();
            }
            groupSessionBuilder_.setUsername(userName_);
        } catch (Exception e) {
            log.error("连接sqlite异常：{}", e.getMessage());
        }
    }

    public void forceAutoCommit() {
        if (!getAutoCommitStatus()) {
            SetAutoCommit(true);
        }
    }

    public boolean getAutoCommitStatus() {
        try {
            return axolotlManager_.getAutoCommit();
        } catch (Exception e) {
            log.error("用户: {}, 获取自动提交状态异常", userName_, e);
        }
        return false;
    }

    public void SetAutoCommit(boolean commit) {
        try {
            axolotlManager_.setAutoCommit(commit);
        } catch (SQLException e) {
            log.error("用户: {}, 提交异常", userName_, e);
        }
    }

    public void Commit() {
        try {
            axolotlManager_.commit();
        } catch (SQLException e) {
            log.error("用户: {}, 提交异常", userName_, e);
        }
    }

    public void Rollback() {
        try {
            axolotlManager_.rollback();
        } catch (Exception ignore) {
        }
    }

    public void Close() {
        try {
            if (axolotlManager_ != null) {
                axolotlManager_.close();
                axolotlManager_ = null;
            }
        } catch (Throwable throwable) {
            log.error("用户: {}, 关闭数据库连接异常", userName_, throwable);
        }
    }

    void InitWebWhatsappTables() {
        try (Statement statement = axolotlManager_.createStatement()) {
            //创建数据表
            statement.execute("CREATE TABLE IF NOT EXISTS devices (_id INTEGER PRIMARY KEY AUTOINCREMENT,device_id TEXT,device_os TEXT,platform_type INTEGER,last_active INTEGER,login_time INTEGER,logout_time INTEGER,adv_key_index INTEGER,full_sync_required INTEGER,place_name TEXT);");
            statement.execute("CREATE TABLE IF NOT EXISTS crypto_info (device_id INTEGER NOT NULL, epoch INTEGER NOT NULL, key_data BLOB NOT NULL, timestamp INTEGER NOT NULL, fingerprint BLOB NOT NULL, stale_timestamp INTEGER NOT NULL DEFAULT 0, PRIMARY KEY ( device_id , epoch ))");
            statement.execute("CREATE TABLE IF NOT EXISTS syncd_mutations(_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, mutation_index TEXT UNIQUE NOT NULL, mutation_value BLOB, mutation_version INTEGER NOT NULL, collection_name TEXT NOT NULL, are_dependencies_missing BOOLEAN NOT NULL, mutation_mac BLOB, device_id INTEGER NOT NULL, epoch INTEGER NOT NULL, chat_jid TEXT, mutation_name TEXT )");
            statement.execute("CREATE TABLE IF NOT EXISTS collection_versions (collection_name TEXT PRIMARY KEY, version INTEGER NOT NULL, lt_hash BLOB, dirty_version INTEGER NOT NULL DEFAULT -1 )");


            String str_adv_raw_id = configStore_.GetSetting("adv_raw_id");
            if (StringUtil.isEmpty(str_adv_raw_id)) {
                adv_raw_id_ = Math.abs(QRcode.AnonymousClass0AM.RandomInt());
                configStore_.SetSetting("adv_raw_id", String.valueOf(adv_raw_id_));
            } else {
                adv_raw_id_ = Integer.parseInt(str_adv_raw_id);
            }

        } catch (Exception e) {
            log.error("初始化webWhatsapp表异常", e);
        }
    }

    void initGroupPreFansRecordTable() {
        try (Statement statement = axolotlManager_.createStatement()) {
            //创建数据表
            statement.execute("CREATE TABLE IF NOT EXISTS group_pre_fans_record (_id INTEGER PRIMARY KEY AUTOINCREMENT, group_id TEXT NOT NULL, fans_id TEXT NOT NULL, timestamp INTEGER)");
            statement.execute("CREATE UNIQUE INDEX IF NOT EXISTS _idx ON group_pre_fans_record(group_id, fans_id)");
        } catch (Exception e) {
            log.error("初始化群记录表异常", e);
        }

    }

    void initTrustedContactRecordTable() {
        try (Statement statement = axolotlManager_.createStatement()) {
            //创建数据表
            statement.execute("CREATE TABLE IF NOT EXISTS trusted_contact_store (_id INTEGER PRIMARY KEY AUTOINCREMENT, jid TEXT UNIQUE NOT NULL, token TEXT NOT NULL)");
        } catch (Exception e) {
            log.error("初始化信任联系人记录表异常", e);
        }
    }

    void initPlatformDeviceInfoRecordTable() {
        try (Statement statement = axolotlManager_.createStatement()) {
            //创建数据表
            statement.execute("CREATE TABLE IF NOT EXISTS platform_device_info (_id INTEGER PRIMARY KEY AUTOINCREMENT, platform TEXT UNIQUE NOT NULL, device_info TEXT NOT NULL)");
        } catch (Exception e) {
            log.error("初始化平台设备信息记录表异常", e);
        }
    }

    void initSyncMultipleDevicesTable() {
        try (Statement statement = axolotlManager_.createStatement()) {
            //创建数据表
            statement.execute("CREATE TABLE IF NOT EXISTS sync_multiple_devices (_id INTEGER PRIMARY KEY AUTOINCREMENT, recipient_id INTEGER UNIQUE NOT NULL)");
        } catch (Exception e) {
            log.error("初始化多设备同步记录表异常", e);
        }
    }

    void initEphemeralMessageTable() {
        try (Statement statement = axolotlManager_.createStatement()) {
            //创建数据表
            statement.execute("CREATE TABLE IF NOT EXISTS ephemeral_message (_id INTEGER PRIMARY KEY AUTOINCREMENT, jid TEXT UNIQUE NOT NULL, expiration INTEGER NOT NULL)");
        } catch (Exception e) {
            log.error("初始化限时消息记录表异常", e);
        }
    }

    void initChatHistoryTable() {
        try (Statement statement = axolotlManager_.createStatement()) {
            //创建数据表
            statement.execute("CREATE TABLE IF NOT EXISTS chat_history (_id INTEGER PRIMARY KEY AUTOINCREMENT, jid TEXT NOT NULL, msg_id TEXT NOT NULL, host TEXT NOT NULL, timestamp INTEGER)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_msg_id_jid ON chat_history(msg_id, jid)");
        } catch (Exception e) {
            log.error("初始化聊天历史记录表异常", e);
        }
    }

    void initPkMsgSyncContactStoreTable() {
        try (Statement statement = axolotlManager_.createStatement()) {
            //创建数据表
            statement.execute("CREATE TABLE IF NOT EXISTS pk_msg_sync_contact_store (_id INTEGER PRIMARY KEY AUTOINCREMENT, recipient_id TEXT UNIQUE NOT NULL, status INTEGER NOT NULL)");
        } catch (Exception e) {
            log.error("初始化pkMsg同步联系人存储表异常", e);
        }
    }

    void initContactSyncStoreTable() {
        try (Statement statement = axolotlManager_.createStatement()) {
            //创建数据表
            statement.execute("CREATE TABLE IF NOT EXISTS contact_sync_store (_id INTEGER PRIMARY KEY AUTOINCREMENT, recipient_id TEXT NOT NULL, device_id INTEGER NOT NULL)");
            // 为recipient_id和device_id创建唯一索引
            statement.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_contact_sync_recipient_device ON contact_sync_store (recipient_id, device_id)");

        } catch (Exception e) {
            log.error("初始化pkMsg同步联系人存储表异常", e);
        }
    }

    void initJidMapTable() {
        try (Statement statement = axolotlManager_.createStatement()) {
            // 增加jid-lid表
            statement.execute("CREATE TABLE IF NOT EXISTS jid_map (_id INTEGER PRIMARY KEY AUTOINCREMENT, jid TEXT NOT NULL, lid TEXT NOT NULL)");

            statement.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_jid_map_jid_lid ON jid_map (jid, lid)");

            // 为jid字段创建索引，优化GetLid方法的查询
            statement.execute("CREATE INDEX IF NOT EXISTS idx_jid_map_jid ON jid_map(jid)");

            // 为lid字段创建索引，优化GetJid方法的查询
            statement.execute("CREATE INDEX IF NOT EXISTS idx_jid_map_lid ON jid_map(lid)");

        } catch (Exception e) {
            log.error("初始化jid_map表异常", e);
        }
    }

    public void InsertDevice(String deviceId, int adv_key_index) {
        deviceStore_.InsertDevice(deviceId, adv_key_index);
    }

    public void ClearDevice() {
        deviceStore_.ClearDevice();
    }

    void CrateTables() {
        try (Statement statement = axolotlManager_.createStatement()) {
            //创建数据表
            statement.execute("CREATE TABLE IF NOT EXISTS identities (_id INTEGER PRIMARY KEY AUTOINCREMENT, recipient_id INTEGER, device_id INTEGER, registration_id INTEGER, public_key BLOB, private_key BLOB, next_prekey_id INTEGER, timestamp INTEGER)");
            statement.execute("CREATE UNIQUE INDEX IF NOT EXISTS identities_idx ON identities(recipient_id, device_id)");
            statement.execute("CREATE TABLE IF NOT EXISTS prekeys (_id INTEGER PRIMARY KEY AUTOINCREMENT, prekey_id INTEGER UNIQUE, sent_to_server BOOLEAN, record BLOB, direct_distribution BOOLEAN, upload_timestamp INTEGER)");
            statement.execute("CREATE TABLE IF NOT EXISTS prekey_uploads (_id INTEGER PRIMARY KEY AUTOINCREMENT, upload_timestamp INTEGER)");
            statement.execute("CREATE TABLE IF NOT EXISTS sessions (_id INTEGER PRIMARY KEY AUTOINCREMENT, recipient_id INTEGER, device_id INTEGER, record BLOB, timestamp INTEGER)");
            statement.execute("CREATE UNIQUE INDEX IF NOT EXISTS sessions_idx ON sessions(recipient_id, device_id)");
            statement.execute("CREATE TABLE IF NOT EXISTS signed_prekeys (_id INTEGER PRIMARY KEY AUTOINCREMENT, prekey_id INTEGER UNIQUE, timestamp INTEGER, record BLOB)");
            statement.execute("CREATE TABLE IF NOT EXISTS message_base_key (_id INTEGER PRIMARY KEY AUTOINCREMENT, msg_key_remote_jid TEXT NOT NULL, msg_key_from_me BOOLEAN NOT NULL, msg_key_id TEXT NOT NULL, recipient_id INTEGER, device_id INTEGER NOT NULL DEFAULT 0, last_alice_base_key BLOB NOT NULL, timestamp INTEGER)");
            statement.execute("CREATE UNIQUE INDEX IF NOT EXISTS message_base_key_idx ON message_base_key (msg_key_remote_jid, msg_key_from_me, msg_key_id, recipient_id, device_id)");
            statement.execute("CREATE TABLE IF NOT EXISTS sender_keys (_id INTEGER PRIMARY KEY AUTOINCREMENT, group_id TEXT NOT NULL, sender_id INTEGER NOT NULL, device_id INTEGER NOT NULL DEFAULT 0, record BLOB NOT NULL, timestamp INTEGER)");
            statement.execute("CREATE UNIQUE INDEX IF NOT EXISTS sender_keys_idx ON sender_keys (group_id, sender_id, device_id)");
            statement.execute("CREATE TABLE IF NOT EXISTS fast_ratchet_sender_keys (_id INTEGER PRIMARY KEY AUTOINCREMENT, group_id TEXT NOT NULL, sender_id INTEGER NOT NULL, device_id INTEGER NOT NULL DEFAULT 0, record BLOB NOT NULL)");
            statement.execute("CREATE UNIQUE INDEX IF NOT EXISTS fast_ratchet_sender_keys_idx ON fast_ratchet_sender_keys (group_id, sender_id, device_id)");
            //创建环境表
            statement.execute("CREATE TABLE IF NOT EXISTS settings(key text PRIMARY KEY,value text)");
        } catch (Exception ignored) {

        }
    }

    void createGroupSendMsgRecordTable() {
        try {
            Statement statement = axolotlManager_.createStatement();
            //创建环境表
            statement.execute("CREATE TABLE IF NOT EXISTS \"group_send_msg_record\" (\n" +
                    "  \"msg_id\" text NOT NULL,\n" +
                    "  \"enc_data\" TEXT NOT NULL\n" +
                    ");");
            statement.execute("CREATE UNIQUE INDEX IF NOT EXISTS \"uni_msg_id\"\n" +
                    "ON \"group_send_msg_record\" (\n" +
                    "  \"msg_id\"\n" +
                    ");");
        } catch (Exception e) {

        }
    }

    public Statement GetStatement() {
        try {
            return axolotlManager_.createStatement();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        return null;
    }

    public PreparedStatement GetPreparedStatement(String preSql) {
        try {
            return axolotlManager_.prepareStatement(preSql);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        return null;
    }

    public void InitInstallData() {
        //初始化第一次安装的数据
        IdentityKeyPair identityKeyPair = InitIdentityData();
        if (null == identityKeyPair) {
            System.err.println("create identityKeyPair failed");
            return;
        }
        identityKeyStore_ = new SignalIdentityKeyStore(this);
        InitSignedPreKeyData(identityKeyPair);
    }

    //初始化 用户标识数据表
    IdentityKeyPair InitIdentityData() {
        PreparedStatement identityPreStatement = null;
        try {
            IdentityKeyPair identityKeyPair = KeyHelper.generateIdentityKeyPair();
            int registrationId = KeyHelper.generateRegistrationId(true);
            //保存identity key pair
            identityPreStatement = axolotlManager_.prepareStatement("INSERT OR REPLACE INTO identities(recipient_id, device_id, registration_id, public_key, private_key, next_prekey_id, timestamp) values(-1,0,?,?,?,?,?)");
            identityPreStatement.setInt(1, registrationId);
            identityPreStatement.setBytes(2, identityKeyPair.getPublicKey().serialize());
            identityPreStatement.setBytes(3, identityKeyPair.getPrivateKey().serialize());
            identityPreStatement.setInt(4, SecureRandom.getInstance("SHA1PRNG").nextInt(16777214) + 1);
            identityPreStatement.setLong(5, System.currentTimeMillis() / 1000);
            identityPreStatement.execute();
            return identityKeyPair;
        } catch (Exception ignored) {
            log.error("InitIdentityData异常");
        } finally {
            closePreparedStatement(identityPreStatement);
        }
        return null;
    }

    //初始化signed pre key
    void InitSignedPreKeyData(IdentityKeyPair identityKeyPair) {
        PreparedStatement preStatement = null;
        try {
            SignedPreKeyRecord signedPreKey = KeyHelper.generateSignedPreKey(identityKeyPair, 0);
            preStatement = axolotlManager_.prepareStatement("INSERT OR REPLACE INTO signed_prekeys (prekey_id, timestamp, record) VALUES(?,?,?)");
            preStatement.setInt(1, signedPreKey.getId());
            preStatement.setLong(2, System.currentTimeMillis() / 1000);
            preStatement.setBytes(3, signedPreKey.serialize());
            preStatement.execute();
        } catch (Exception e) {

        } finally {
            closePreparedStatement(preStatement);
        }
    }

    public void setRegisterSecretKey(IdentityKeyPair identityKeyPair, int registrationId, SignedPreKeyRecord signedPreKey) {
        PreparedStatement identityPreStatement = null;
        try {
            identityPreStatement = axolotlManager_.prepareStatement("INSERT OR REPLACE INTO identities(recipient_id, device_id, registration_id, public_key, private_key, next_prekey_id, timestamp) values(-1,0,?,?,?,?,?)");
            identityPreStatement.setInt(1, registrationId);
            identityPreStatement.setBytes(2, identityKeyPair.getPublicKey().serialize());
            identityPreStatement.setBytes(3, identityKeyPair.getPrivateKey().serialize());
            identityPreStatement.setInt(4, SecureRandom.getInstance("SHA1PRNG").nextInt(16777214) + 1);
            identityPreStatement.setLong(5, System.currentTimeMillis() / 1000);
            identityPreStatement.execute();
        } catch (Exception ignored) {
        } finally {
            closePreparedStatement(identityPreStatement);
        }
        PreparedStatement preStatement = null;
        try {
            preStatement = axolotlManager_.prepareStatement("INSERT OR REPLACE INTO signed_prekeys (prekey_id, timestamp, record) VALUES(?,?,?)");
            preStatement.setInt(1, signedPreKey.getId());
            preStatement.setLong(2, System.currentTimeMillis() / 1000);
            preStatement.setBytes(3, signedPreKey.serialize());
            preStatement.execute();
        } catch (Exception ignored) {
        } finally {
            closePreparedStatement(preStatement);
        }
    }

    /**
     * 判断是否为刚入库的新号
     */
    public boolean isPreKeysInit() {
        // 判断是否为初始化
        // 1. 刚入库, 上线时prekeys表中为空
        // 2. 刚入库, 生成好prekeys后掉线了, 导致没有走后面的逻辑, 此时库里面全是unsent
        int sent = preKeyStore_.getPendingPreKeysCount();
        int unsent = preKeyStore_.GetUnsentCount();
        boolean flag = false;
        if ((sent == 0 && unsent == 0) || (sent == 0 && unsent >= COUNT_GEN_PREKEYS)) {
            flag = true;
        }
        return flag;
    }

    public void LevelPreKeys(boolean force) {
        int count = preKeyStore_.getPendingPreKeysCount();
        if (count < 100 || force) {
            int unsent = preKeyStore_.GetUnsentCount();
            if (unsent > 0) {
                return;
            }
            int maxId = preKeyStore_.getMaxPreKeyId();
            preKeyStore_.generatePreKeyAndStore(maxId, COUNT_GEN_PREKEYS);
        }
    }

    public void SetBytesSetting(String key, byte[] value) {
        try {
            KeyLockUtil.lock(userName_);
            configStore_.SetBytes(key, value);
        } catch (Exception ignored) {
        } finally {
            KeyLockUtil.unlock(userName_);
        }
    }

    public void SetStringSetting(String key, String value) {
        configStore_.SetSetting(key, value);
    }

    public byte[] GetBytesSetting(String key) {
        return configStore_.GetBytes(key);
    }

    public LinkedList<PreKeyRecord> LoadUnSendPreKey() {
        return preKeyStore_.LoadUnSendPreKey();
    }


    public PreKeyRecord GetOnePreKeyRecord() {
        return preKeyStore_.GetOnePreKeyRecord();
    }

    public void SetAsSent(LinkedList<Integer> sentIds) {
        preKeyStore_.setAsSent(sentIds);
    }

    public void delPreKeysSent() {
        PreparedStatement preparedStatement = null;
        try {
            SetAutoCommit(false);
            try {
                preparedStatement = this.GetPreparedStatement("delete from prekeys");
                preparedStatement.execute();
            } finally {
                this.closePreparedStatement(preparedStatement);
            }
            Commit();
        } catch (Exception e) {
            log.error("删除一次性密钥异常", e);
            Rollback();
        } finally {
            SetAutoCommit(true);
        }
    }

    public void delSignPreKeys() {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = this.GetPreparedStatement("delete from signed_prekeys");
            preparedStatement.execute();
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            this.closePreparedStatement(preparedStatement);
        }
    }

    public void delIdentities() {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = this.GetPreparedStatement("delete from identities where recipient_id = -1");
            preparedStatement.execute();
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            this.closePreparedStatement(preparedStatement);
        }
    }

    public SignedPreKeyRecord LoadLatestSignedPreKey(boolean generate) {
        List<SignedPreKeyRecord> records = signedPreKeyStore_.loadSignedPreKeys();
        if ((null == records) || (records.isEmpty())) {
            if (generate) {
                return GenerateSignedPrekey();
            }
            return null;
        }
        return records.get(records.size() - 1);
    }


    public SignedPreKeyRecord GenerateSignedPrekey() {
        SignedPreKeyRecord preKeyRecord = LoadLatestSignedPreKey(false);
        int newSignedPrekeyId = 0;
        if (null != preKeyRecord) {
            newSignedPrekeyId = preKeyRecord.getId() + 1;
        }
        SignedPreKeyRecord result = null;
        try {
            result = KeyHelper.generateSignedPreKey(identityKeyStore_.getIdentityKeyPair(), newSignedPrekeyId);
            signedPreKeyStore_.storeSignedPreKey(result.getId(), result);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        return result;
    }

    public SessionCipher GetSessionCipher(StringUtil.JidInfo jidInfo) {
        //GorgeousLooper.Instance().CheckThread();
        SessionCipher sessionCipher = sessionCipherHashMap.get(jidInfo);
        if (sessionCipher == null) {
            SignalProtocolAddress address = new SignalProtocolAddress(jidInfo.recipientId, jidInfo.deviceId);
            if (StringUtils.isEmpty(userName_)) {
                userName_ = IdUtil.simpleUUID();
            }
            sessionCipher = new SessionCipher(sessionStore_, preKeyStore_, signedPreKeyStore_, identityKeyStore_, address, userName_);
            sessionCipherHashMap.put(jidInfo, sessionCipher);
        }
        return sessionCipher;
    }

    public GroupCipher GetGroupCipher(String groupId, String receiptId, int deviceId) {
        //group deviceid=0
        SignalProtocolAddress address = new SignalProtocolAddress(receiptId, deviceId);
        SenderKeyName senderKeyName = new SenderKeyName(groupId, address);
        GroupCipher groupCipher = groupCipherHashMap.get(senderKeyName);
        if (null == groupCipher) {
            if (StringUtils.isEmpty(userName_)) {
                userName_ = IdUtil.simpleUUID();
            }
            groupCipher = new GroupCipher(senderKeyStore_, senderKeyName, userName_);
            groupCipherHashMap.put(senderKeyName, groupCipher);
        }
        return groupCipher;
    }

    public List<Integer> GetSubDeviceSessions(String recipientId) {
        return sessionStore_.getSubDeviceSessions(recipientId);
    }

    /**
     * 删除某个多设备session
     *
     * @param recipientId
     * @param deviceId
     */
    public void DeleteSession(String recipientId, int deviceId) {
        sessionStore_.deleteSession(new SignalProtocolAddress(recipientId, deviceId));
    }

    public static final Random A02 = new Random();

    public static byte[] A05(byte[] bArr) {
        int nextInt = A02.nextInt(16) + 1;
        int length = bArr.length;
        int i = length + nextInt;
        byte[] bArr2 = new byte[i];
        System.arraycopy(bArr, 0, bArr2, 0, length);
        Arrays.fill(bArr2, length, i, (byte) nextInt);
        return bArr2;
    }

    public CiphertextMessage Encrypt(StringUtil.JidInfo jidInfo, byte[] message) throws Exception {
        //GorgeousLooper.Instance().CheckThread();
        SessionCipher cipher = GetSessionCipher(jidInfo);
        //固定加一个padding
        byte[] paddingData = A05(message);
        return cipher.encrypt(paddingData);
    }

    public CiphertextMessage fakeSessionEncrypt(StringUtil.JidInfo jidInfo, byte[] message) throws Exception {
        //GorgeousLooper.Instance().CheckThread();
        SessionCipher cipher = GetSessionCipher(jidInfo);
        //固定加一个padding
        byte[] paddingData = A05(message);
        return cipher.fakeSessionEncrypt(paddingData);
    }

    public byte[] DecryptPKMsg(StringUtil.JidInfo jidInfo, byte[] data) throws InvalidVersionException, InvalidMessageException, InvalidKeyException, DuplicateMessageException, InvalidKeyIdException, UntrustedIdentityException, LegacyMessageException {
        //GorgeousLooper.Instance().CheckThread();
        PreKeySignalMessage message = new PreKeySignalMessage(data);
        // 储存identity
        identityKeyStore_.saveIdentity(new SignalProtocolAddress(jidInfo.recipientId, jidInfo.deviceId), message.getIdentityKey());
        byte[] plaintext = GetSessionCipher(jidInfo).decrypt(message);
        int padding = plaintext[plaintext.length - 1];

        byte[] result = new byte[plaintext.length - padding];
        System.arraycopy(plaintext, 0, result, 0, result.length);
        return result;
    }

    public byte[] DecryptMsg(StringUtil.JidInfo jidInfo, byte[] data) throws LegacyMessageException, InvalidMessageException, DuplicateMessageException, NoSessionException, UntrustedIdentityException {
        //GorgeousLooper.Instance().CheckThread();
        SignalMessage message = new SignalMessage(data);
        byte[] plaintext = GetSessionCipher(jidInfo).decrypt(message);
        int padding = plaintext[plaintext.length - 1];

        byte[] result = new byte[plaintext.length - padding];
        System.arraycopy(plaintext, 0, result, 0, result.length);
        return result;
    }

    public byte[] GroupEncrypt(String groupId, int deviceId, byte[] message, SenderKeyRecord senderKeyRecord) throws NoSessionException, InvalidKeyException {
        //GorgeousLooper.Instance().CheckThread();
        GroupCipher cipher = GetGroupCipher(groupId, userName_, deviceId);
        //固定加一个padding
        byte[] paddingData = new byte[message.length + 1];
        System.arraycopy(message, 0, paddingData, 0, message.length);
        paddingData[paddingData.length - 1] = 1;
        return cipher.encrypt(paddingData, senderKeyRecord);
    }

    public byte[] GroupDecrypt(String groupId, String participantId, int deviceId, byte[] data) throws NoSessionException, DuplicateMessageException, InvalidMessageException, LegacyMessageException {
        //GorgeousLooper.Instance().CheckThread();
        GroupCipher cipher = GetGroupCipher(groupId, participantId, deviceId);
        byte[] plaintext = cipher.decrypt(data);
        int padding = plaintext[plaintext.length - 1];

        byte[] result = new byte[plaintext.length - padding];
        System.arraycopy(plaintext, 0, result, 0, result.length);
        return result;
    }

    public SenderKeyDistributionMessage GroupCreateSKMsg(String groupId) {
        //GorgeousLooper.Instance().CheckThread();
        SignalProtocolAddress address = new SignalProtocolAddress(userName_, 0);
        SenderKeyName senderKeyName = new SenderKeyName(groupId, address);
        return groupSessionBuilder_.create(senderKeyName);
    }

    public SenderKeyRecord GroupCreateSenderKeyRecord() {
        return groupSessionBuilder_.createSenderKeyRecord();
    }


    public void GroupCreateSession(String groupId, String participantId, int deviceId, byte[] data) throws InvalidMessageException, LegacyMessageException {
        //group device id =0
        SignalProtocolAddress address = new SignalProtocolAddress(participantId, deviceId);
        SenderKeyName senderKeyName = new SenderKeyName(groupId, address);
        groupSessionBuilder_.process(senderKeyName, new SenderKeyDistributionMessage(data));
    }

    public void CreateSession(String receiptId, int deviceId, PreKeyBundle bundle) throws UntrustedIdentityException, InvalidKeyException {
        SignalProtocolAddress address = new SignalProtocolAddress(receiptId, deviceId);
        if (StringUtils.isEmpty(userName_)) {
            userName_ = IdUtil.simpleUUID();
        }
        SessionBuilder sessionBuilder = new SessionBuilder(sessionStore_, preKeyStore_, signedPreKeyStore_, identityKeyStore_, address, userName_);
        sessionBuilder.process(bundle);
    }

    public SenderKeyRecord LoadSenderKey(String groupId) {
        SignalProtocolAddress address = new SignalProtocolAddress(userName_, 0);
        SenderKeyName senderKeyName = new SenderKeyName(groupId, address);
        return senderKeyStore_.loadSenderKey(senderKeyName);
    }

    public int getLocalRegistrationId() {
        return identityKeyStore_.getLocalRegistrationId();
    }

    public IdentityKeyPair GetIdentityKeyPair() {
        return identityKeyStore_.getIdentityKeyPair();
    }

    public boolean ContainsSession(StringUtil.JidInfo jidInfo) {
        return sessionStore_.containsSession(jidInfo);
    }

    public List<String> batchQuerySession(List<String> recipientIds) {
        return sessionStore_.batchQuerySession(recipientIds);
    }

    public int containsSessionNum(StringUtil.JidInfo jidInfo) {
        return sessionStore_.containsSessionNum(jidInfo);
    }

    public int containsSessionNum(List<String> recipientIds) {
        return sessionStore_.containsSessionNum(recipientIds);
    }

    // 创建假的session
    public boolean CreateFakeSession(String receiptId, int deviceId, String fakeRecord) {
        SignalProtocolAddress signalProtocolAddress = new SignalProtocolAddress(receiptId, deviceId);
        try {
            sessionStore_.storeSession(signalProtocolAddress, new SessionRecord(Base64.decode(fakeRecord)));
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public void closePreparedStatement(PreparedStatement preparedStatement) {
        if (preparedStatement != null) {
            try {
                preparedStatement.close();
            } catch (Exception ignored) {
            }
        }
    }


    public void SetAdvRawId(int rawid) {
        adv_raw_id_ = rawid;
        configStore_.SetSetting("adv_raw_id", String.valueOf(adv_raw_id_));
    }

    public int GetAdvRawId() {
        return adv_raw_id_;
    }

    void InsertAppStateSyncKey(WhatsMessage.AppStateSyncKey key) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = GetPreparedStatement("INSERT OR IGNORE INTO crypto_info (device_id, epoch, key_data, timestamp, fingerprint) VALUES (?, ?, ?, ?, ?)");
            QRcode.SyncdKeyId syncdKeyId = new QRcode.SyncdKeyId(key.getKeyId().getKeyId().toByteArray());
            preparedStatement.setInt(1, syncdKeyId.GetDeviceId());
            preparedStatement.setInt(2, syncdKeyId.GetEpoch());

            preparedStatement.setBytes(3, key.getKeyData().getKeyData().toByteArray());
            preparedStatement.setLong(4, key.getKeyData().getTimestamp());
            preparedStatement.setBytes(5, key.getKeyData().getFingerprint().toByteArray());
            preparedStatement.execute();
        } catch (Exception e) {
        } finally {
            closePreparedStatement(preparedStatement);
        }
    }


    int GetMaxEpoch() {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = GetPreparedStatement("SELECT MAX ( epoch ) FROM crypto_info");
            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            } catch (Exception ignored) {
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            closePreparedStatement(preparedStatement);
        }
        return 1;
    }

    public WhatsMessage.AppStateSyncKey CreateAppStateSyncKey() {
        int A01 = GetMaxEpoch();
        if (A01 == 0) {
            A01 = new SecureRandom().nextInt(65536);
        }
        SyncdKeyId syncdKeyId = new SyncdKeyId(0, A01 + 1);

        long A02 = System.currentTimeMillis();
        WhatsMessage.AppStateSyncKey.Builder appStateSyncKey = WhatsMessage.AppStateSyncKey.newBuilder();
        appStateSyncKey.setKeyId(WhatsMessage.AppStateSyncKeyId.newBuilder().setKeyId(ByteString.copyFrom(syncdKeyId.id_data)));


        //key data
        WhatsMessage.AppStateSyncKeyData.Builder syncData = appStateSyncKey.getKeyDataBuilder();

        byte[] RandomBytes = QRcode.AnonymousClass0AM.RandomBytes(32);
        syncData.setKeyData(ByteString.copyFrom(RandomBytes));

        WhatsMessage.AppStateSyncKeyFingerprint.Builder fingerprintBuild = syncData.getFingerprintBuilder();
        fingerprintBuild.setRawId(adv_raw_id_);
        fingerprintBuild.setCurrentIndex(1);
        fingerprintBuild.addDeviceIndexes(0);
        fingerprintBuild.addDeviceIndexes(1);

        syncData.setTimestamp(A02);

        WhatsMessage.AppStateSyncKey key = appStateSyncKey.build();
        InsertAppStateSyncKey(key);

        return key;
    }


    public byte[] GetItHash(String collection_name) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = GetPreparedStatement("SELECT lt_hash FROM collection_versions WHERE collection_name = ?");
            preparedStatement.setString(1, collection_name);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (rs.next()) {
                    return rs.getBytes(1);
                }
            } catch (Exception ignored) {
            }
        } catch (Exception ignored) {
        } finally {
            closePreparedStatement(preparedStatement);
        }
        return null;
    }

    public HashMap<String, byte[]> GetMutationMac(String collection_name, String[] index) {
        HashMap<String, byte[]> result = new HashMap<>();
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT mutation_index, mutation_mac FROM syncd_mutations WHERE collection_name = ? AND mutation_index IN ");
        sb.append(AnonymousClass33J.A01(index.length));
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = GetPreparedStatement(sb.toString());
            preparedStatement.setString(1, collection_name);
            for (int i = 0; i < index.length; i++) {
                preparedStatement.setString(i + 2, index[i]);
            }

            try (ResultSet rs = preparedStatement.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString(1), rs.getBytes(2));
                }
            } catch (Exception ignored) {
            }
        } catch (Exception ignored) {
        } finally {
            closePreparedStatement(preparedStatement);
        }
        return result;
    }

    public int GetCollectionVersion(String collection_name) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = GetPreparedStatement("SELECT version FROM collection_versions WHERE collection_name = ?");
            preparedStatement.setString(1, collection_name);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            } catch (Exception ignored) {
            }
        } catch (Exception ignored) {
        } finally {
            closePreparedStatement(preparedStatement);
        }
        return 0;
    }

    public void SaveLtHash(String collection_name, byte[] hash, int version) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = GetPreparedStatement("INSERT OR REPLACE into collection_versions(version  , lt_hash,  collection_name) values (?, ?, ?)");
            preparedStatement.setLong(1, version);
            preparedStatement.setBytes(2, hash);
            preparedStatement.setString(3, collection_name);
            preparedStatement.execute();
        } catch (Exception ignored) {
        } finally {
            closePreparedStatement(preparedStatement);
        }
    }

    /**
     * 删除扫码设备信息
     */
    public void delScanWebInfo() {
        try {
            SetAutoCommit(false);
            executeSql("DELETE FROM collection_versions");
            executeSql("DELETE FROM crypto_info");
            executeSql("DELETE FROM syncd_mutations");
            executeSql("DELETE FROM settings WHERE key = 'scan_web'");
            executeSql("DELETE FROM settings WHERE key = 'adv_raw_id'");
            Commit();
        } catch (Exception e) {
            log.error("删除扫码设备信息异常", e);
            Rollback();
        } finally {
            SetAutoCommit(true);
        }
    }

    public void executeSql(String sql) throws SQLException {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = GetPreparedStatement(sql);
            preparedStatement.execute();
        } finally {
            closePreparedStatement(preparedStatement);
        }
    }

    public WhatsMessage.AppStateSyncKey GetAppStateSynKey(int deviceId, int epoch) {
        WhatsMessage.AppStateSyncKey result = null;
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = GetPreparedStatement("SELECT key_data, timestamp, fingerprint FROM crypto_info WHERE device_id = ?  AND epoch = ? ");
            preparedStatement.setInt(1, deviceId);
            preparedStatement.setInt(2, epoch);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (rs.next()) {
                    SyncdKeyId syncdKeyId = new SyncdKeyId(deviceId, epoch);

                    WhatsMessage.AppStateSyncKey.Builder appStateSyncKey = WhatsMessage.AppStateSyncKey.newBuilder();
                    appStateSyncKey.setKeyId(WhatsMessage.AppStateSyncKeyId.newBuilder().setKeyId(ByteString.copyFrom(syncdKeyId.id_data)));

                    //key data
                    WhatsMessage.AppStateSyncKeyData.Builder syncData = appStateSyncKey.getKeyDataBuilder();
                    syncData.setKeyData(ByteString.copyFrom(rs.getBytes("key_data")));

                    syncData.setFingerprint(WhatsMessage.AppStateSyncKeyFingerprint.parseFrom(rs.getBytes("fingerprint")));
                    syncData.setTimestamp(rs.getLong("timestamp"));
                    result = appStateSyncKey.build();
                }
            } catch (Exception ignored) {
            }
        } catch (Exception ignored) {
        } finally {
            closePreparedStatement(preparedStatement);
        }
        return result;
    }

    public void SaveMutations(String mutation_index, byte[] mutation_value, int mutation_version, String collection_name, int device_id, int epoch, byte[] mutation_mac, String mutation_name) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = GetPreparedStatement("INSERT OR REPLACE INTO syncd_mutations (mutation_index, mutation_value, mutation_version, collection_name, are_dependencies_missing, device_id, epoch, mutation_mac, chat_jid, mutation_name) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            preparedStatement.setString(1, mutation_index);
            preparedStatement.setBytes(2, mutation_value);
            preparedStatement.setInt(3, mutation_version);
            preparedStatement.setString(4, collection_name);
            preparedStatement.setBoolean(5, false);
            preparedStatement.setInt(6, device_id);
            preparedStatement.setInt(7, epoch);
            preparedStatement.setBytes(8, mutation_mac);
            preparedStatement.setString(9, "");
            preparedStatement.setString(10, mutation_name);


            preparedStatement.execute();
        } catch (Exception ignored) {
        } finally {
            closePreparedStatement(preparedStatement);
        }

    }


    public WhatsMessage.AppStateSyncKey GetAppStateSynKeyByMutationName(String mutation_name) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = GetPreparedStatement("SELECT device_id, epoch FROM syncd_mutations WHERE mutation_name = ?");
            preparedStatement.setString(1, mutation_name);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (rs.next()) {
                    return GetAppStateSynKey(rs.getInt(1), rs.getInt(2));
                }
            } catch (Exception ignored) {
            }
        } catch (Exception ignored) {
        } finally {
            closePreparedStatement(preparedStatement);
        }
        return null;
    }


    public WhatsMessage.AppStateSyncKey GetLastAppstateSyncKey() {
        WhatsMessage.AppStateSyncKey result = null;
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = GetPreparedStatement("SELECT device_id, epoch, key_data, timestamp, fingerprint FROM crypto_info ORDER BY epoch DESC, device_id ASC LIMIT 1");
            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (rs.next()) {
                    SyncdKeyId syncdKeyId = new SyncdKeyId(rs.getInt("device_id"), rs.getInt("epoch"));

                    WhatsMessage.AppStateSyncKey.Builder appStateSyncKey = WhatsMessage.AppStateSyncKey.newBuilder();
                    appStateSyncKey.setKeyId(WhatsMessage.AppStateSyncKeyId.newBuilder().setKeyId(ByteString.copyFrom(syncdKeyId.id_data)));

                    //key data
                    WhatsMessage.AppStateSyncKeyData.Builder syncData = appStateSyncKey.getKeyDataBuilder();
                    syncData.setKeyData(ByteString.copyFrom(rs.getBytes("key_data")));

                    syncData.setFingerprint(WhatsMessage.AppStateSyncKeyFingerprint.parseFrom(rs.getBytes("fingerprint")));
                    syncData.setTimestamp(rs.getLong("timestamp"));
                    result = appStateSyncKey.build();
                }
            } catch (Exception ignored) {
            }
        } catch (Exception ignored) {
        } finally {
            closePreparedStatement(preparedStatement);
        }
        return result;
    }

    public WhatsMessage.AppStateSyncKeyShare GetAllSyncKey() {
        WhatsMessage.AppStateSyncKeyShare.Builder builder = WhatsMessage.AppStateSyncKeyShare.newBuilder();
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = GetPreparedStatement("SELECT device_id, epoch, key_data, timestamp, fingerprint FROM crypto_info");
            try (ResultSet rs = preparedStatement.executeQuery()) {
                while (rs.next()) {
                    SyncdKeyId syncdKeyId = new SyncdKeyId(rs.getInt("device_id"), rs.getInt("epoch"));

                    WhatsMessage.AppStateSyncKey.Builder appStateSyncKey = WhatsMessage.AppStateSyncKey.newBuilder();
                    appStateSyncKey.setKeyId(WhatsMessage.AppStateSyncKeyId.newBuilder().setKeyId(ByteString.copyFrom(syncdKeyId.id_data)));

                    //key data
                    WhatsMessage.AppStateSyncKeyData.Builder syncData = appStateSyncKey.getKeyDataBuilder();
                    syncData.setKeyData(ByteString.copyFrom(rs.getBytes("key_data")));

                    syncData.setFingerprint(WhatsMessage.AppStateSyncKeyFingerprint.parseFrom(rs.getBytes("fingerprint")));
                    syncData.setTimestamp(rs.getLong("timestamp"));
                    builder.addKeys(appStateSyncKey);
                }
            } catch (Exception ignored) {
            }
        } catch (Exception ignored) {
        } finally {
            closePreparedStatement(preparedStatement);
        }
        return builder.build();
    }

    /**
     * 标记已完成过web扫码
     */
    public void markCompletedScanWeb() {
        configStore_.SetSetting("scan_web", "1");
    }

    /**
     * 是否扫码过web
     */
    public boolean isScanWeb() {
        String scanWeb = configStore_.GetSetting("scan_web");
        return StringUtils.hasLength(scanWeb) && "1".equals(scanWeb);
    }

    /**
     * 是否初始化
     */
    public boolean isInit() {
        return configStore_.existInit();
    }

    /**
     * 标记已初始化sendXMLStreamEnd
     */
    public void markInit() {
        try {
            KeyLockUtil.lock(userName_);
            configStore_.SetSetting("is_init", "0");
        } catch (Exception ignored) {
        } finally {
            KeyLockUtil.unlock(userName_);
        }
    }

    /**
     * 标记初始化完成
     */
    public void initCompleted() {
        try {
            KeyLockUtil.lock(userName_);
            configStore_.SetSetting("is_init", "1");
        } catch (Exception ignored) {
        } finally {
            KeyLockUtil.unlock(userName_);
        }
    }

    /**
     * 初始化是否完成
     */
    public boolean isInitCompleted() {
        String init = configStore_.GetSetting("is_init");
        return StringUtils.hasLength(init) && "1".equals(init);
    }

    public void setLastPn(String pn) {
        configStore_.SetSetting("last_pn", pn);
    }

    public String getLastPn() {
        return configStore_.GetSetting("last_pn");
    }

    public void setAbtConfig(String abtConfig) {
        configStore_.SetSetting("abt_config", abtConfig);
    }

    public String getAbtConfig() {
        return configStore_.GetSetting("abt_config");
    }

    public void setTimeLockEndTime(String timeLockEndTime) {
        try {
            KeyLockUtil.lock(userName_);
            configStore_.SetSetting("time_lock_end_time", timeLockEndTime);
        } catch (Exception ignored) {
        } finally {
            KeyLockUtil.unlock(userName_);
        }
    }

    public String getTimeLockEndTime() {
        return configStore_.GetSetting("time_lock_end_time");
    }

    public long getRemainingLockTime(String timeLockEndTime) {
        if (!StringUtils.hasLength(timeLockEndTime)) {
            return 0L;
        }
        timeLockEndTime = (timeLockEndTime.length() == 13) ? timeLockEndTime.substring(0, 10) : timeLockEndTime;
        Long endTime = Convert.toLong(timeLockEndTime);
        long lockTime = endTime - DateUtil.currentSeconds();
        if (lockTime > 0) {
            return lockTime;
        }
        try {
            KeyLockUtil.lock(userName_);
            configStore_.delSetting("time_lock_end_time");
        } catch (Exception ignored) {
        } finally {
            KeyLockUtil.unlock(userName_);
        }
        return 0L;
    }


    public void saveGroupPreFansRecord(String groupId, List<String> fansIds) {
        try {
            SetAutoCommit(false);
            for (String fansId : fansIds) {
                PreparedStatement preparedStatement = null;
                try {
                    preparedStatement = GetPreparedStatement("INSERT OR REPLACE INTO group_pre_fans_record (group_id, fans_id, timestamp) VALUES(?, ?, ?)");
                    preparedStatement.setString(1, groupId);
                    preparedStatement.setString(2, fansId);
                    preparedStatement.setLong(3, System.currentTimeMillis() / 1000);
                    preparedStatement.execute();
                } finally {
                    closePreparedStatement(preparedStatement);
                }

            }
            Commit();
        } catch (Exception e) {
            log.error("存储群新进粉丝异常", e);
            Rollback();
        } finally {
            SetAutoCommit(true);
        }
    }

    public List<String> getPreGroupFans(String groupId) {
        PreparedStatement preparedStatement = null;
        LinkedList<String> list = new LinkedList<>();
        try {
            preparedStatement = GetPreparedStatement("SELECT fans_id FROM group_pre_fans_record WHERE group_id = ?");
            preparedStatement.setString(1, groupId);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getString(1));
                }
            } catch (Exception ignored) {
            }
        } catch (Exception ignore) {

        } finally {
            closePreparedStatement(preparedStatement);
        }
        return list;
    }

    public void deletePreGroupFans(String groupId) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = GetPreparedStatement("DELETE FROM group_pre_fans_record WHERE group_id = ?");
            preparedStatement.setString(1, groupId);
            preparedStatement.execute();
        } catch (Exception ignore) {
        } finally {
            closePreparedStatement(preparedStatement);
        }
    }

    public void deleteGcmSettings() {
        try {
            this.delSetting("gcm_params");
            this.delSetting("gcmPersistentId");
            this.delSetting("apns_params");
        } catch (Exception e) {
            log.error("删除gcm失败", e);
        }
    }

    public void deletePreGroupFans(String groupId, List<String> fansIds) {
        try {
            SetAutoCommit(false);
            for (String fansId : fansIds) {
                PreparedStatement preparedStatement = null;
                try {
                    preparedStatement = GetPreparedStatement("DELETE FROM group_pre_fans_record WHERE group_id = ? AND fans_id LIKE ?");
                    preparedStatement.setString(1, groupId);
                    preparedStatement.setString(2, StrUtil.replace(fansId, "@s.whatsapp.net", "") + "%");
                    preparedStatement.execute();
                } finally {
                    closePreparedStatement(preparedStatement);
                }
            }
            Commit();
        } catch (Exception e) {
            log.error("删除群新进粉丝异常", e);
            Rollback();
        } finally {
            SetAutoCommit(true);
        }
    }

    public void deleteGroupSenderKey(String groupId, List<String> fansIds) {
        try {
            SetAutoCommit(false);
            for (String fansId : fansIds) {
                PreparedStatement preparedStatement = null;
                try {
                    preparedStatement = GetPreparedStatement("DELETE FROM sender_keys WHERE group_id = ? AND sender_id = ?");
                    preparedStatement.setString(1, groupId);
                    preparedStatement.setString(2, StrUtil.replace(fansId, "@s.whatsapp.net", ""));
                    preparedStatement.execute();
                } finally {
                    closePreparedStatement(preparedStatement);
                }
            }
            Commit();
        } catch (Exception e) {
            log.error("删除粉丝senderKey异常", e);
            Rollback();
        } finally {
            SetAutoCommit(true);
        }
    }

    public DeviceInfo getPlatformDeviceInfo(String platform) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = GetPreparedStatement("SELECT device_info FROM platform_device_info WHERE platform = ?");
            preparedStatement.setString(1, platform);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (rs.next()) {
                    String content = rs.getString(1);
                    if (StringUtils.hasLength(content)) {
                        return JSONObject.parseObject(content, DeviceInfo.class);
                    } else {
                        return getDeviceInfo(platform);
                    }
                } else {
                    return getDeviceInfo(platform);
                }
            } catch (Exception ignored) {
            }
        } catch (Exception ignored) {
        } finally {
            closePreparedStatement(preparedStatement);
        }
        return null;
    }


    public void savePlatformDeviceInfo(String platform, DeviceInfo deviceInfo) {
        PreparedStatement preparedStatement = null;
        String content = JSONObject.toJSONString(deviceInfo);
        try {
            KeyLockUtil.lock(userName_);
            preparedStatement = GetPreparedStatement("INSERT OR REPLACE into platform_device_info(platform, device_info) values (?, ?)");
            preparedStatement.setString(1, platform);
            preparedStatement.setString(2, content);
            preparedStatement.execute();
        } catch (Exception ignored) {
        } finally {
            closePreparedStatement(preparedStatement);
            KeyLockUtil.unlock(userName_);
        }
    }

    public void setGcmCatValue(byte[] catValue) {
        configStore_.SetBytes("gcm_cat", catValue);
    }

    public byte[] getGcmCatValue() {
        return configStore_.GetBytes("gcm_cat");
    }

    public void setGcmParamsValue(String catValue) {
        configStore_.SetSetting("gcm_params", catValue);
    }

    public String getGcmParamsValue() {
        return configStore_.GetSetting("gcm_params");
    }

    public void setApnsParamsValue(String catValue) {
        configStore_.SetSetting("apns_params", catValue);
    }

    public String getApnsParamsValue() {
        return configStore_.GetSetting("apns_params");
    }

    public void setGcmPersistentIdValue(String catValue) {
        configStore_.SetSetting("gcmPersistentId", catValue);
    }

    public String getGcmPersistentIdValue() {
        return configStore_.GetSetting("gcmPersistentId");
    }

    public void delSetting(String key) {
        configStore_.delSetting(key);
    }

    private DeviceInfo getDeviceInfo(String platform) {
        DeviceInfo deviceInfo = null;
        if (Constant.ANDROID.equals(platform)) {
            deviceInfo = DeviceUtil.randomGetOneAndroidDevice();
        } else if (Constant.IOS.equals(platform)) {
            deviceInfo = DeviceUtil.randomGetOneIosDevice();
        }
        if (deviceInfo == null) {
            return null;
        }
        //保存设备信息
        savePlatformDeviceInfo(platform, deviceInfo);
        return deviceInfo;
    }

    public void updateRegistrationId(int registrationId) {
        identityKeyStore_.updateRegistrationId(registrationId);
        identityKeyStore_ = new SignalIdentityKeyStore(this);
    }
}
