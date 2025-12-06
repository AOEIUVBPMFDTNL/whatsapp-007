package ExportTool;


import Env.DeviceEnv;
import Util.StringUtil;
import cn.hutool.core.codec.Base64;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;
import com.whatsapp.Me;
import jni.NoiseJni;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;
import org.whispersystems.libsignal.util.KeyHelper;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.UUID;

@Slf4j
public class ExportTool {
    static void CrateTables(java.sql.Connection axolotlManager) {
        try {
            Statement statement = axolotlManager.createStatement();
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
        } catch (Exception e) {

        }
    }

    static class MeObjectInputStream extends ObjectInputStream {
        public MeObjectInputStream(InputStream inputStream) throws Exception {
            super(inputStream);
        }

        @Override // java.io.ObjectInputStream
        public ObjectStreamClass readClassDescriptor() {
            try {
                ObjectStreamClass readClassDescriptor = super.readClassDescriptor();
                return readClassDescriptor.getName().equals("com.whatsapp.App$Me") ? ObjectStreamClass.lookup(Me.class) : readClassDescriptor;

            } catch (Exception e) {
                System.out.println(e.getLocalizedMessage());
            }
            return null;
        }
    }

    public static void main(String[] args) throws Exception {
        String os = System.getProperty("os.name");
        if (os.startsWith("Linux")) {
            //sudo apt install libssl-dev
            //sudo apt-get install curl libcurl4-openssl-dev
            System.load("/Users/sunnoc/IdeaProjects/whatsapp-android/src/main/resources/lib/libNoiseJni.so");
        } else if (os.startsWith("Windows")) {
            System.load("/Users/sunnoc/IdeaProjects/whatsapp-android/src/main/resources/lib/libNoiseJni.dll");
        } else if (os.startsWith("Mac")) {
            System.load("/Users/sunnoc/IdeaProjects/whatsapp-android/src/main/resources/lib/libNoiseJni.dylib");
        }

        String json_str = "\n" +
                "{\"a\":\"rO0ABXVyAAJbQqzzF_gGCFTgAgAAeHAAAAAqAAJaB7tUzJrkvDWKb2EsLVJLa6loxAQig6A8DQurF4JPm4N-J3wl5uFs\",\"b\":\"rO0ABXNyAA9jb20ud2hhdHNhcHAuTWXk6K3RrOBlqgIAA0wAAmNjdAASTGphdmEvbGFuZy9TdHJpbmc7TAAJamFiYmVyX2lkcQB-AAFMAAZudW1iZXJxAH4AAXhwdAACOTF0AAw5MTg5ODkxMzk5Njl0AAo4OTg5MTM5OTY5\",\"c\":\"PD94bWwgdmVyc2lvbj0nMS4wJyBlbmNvZGluZz0ndXRmLTgnIHN0YW5kYWxvbmU9J3llcycgPz4KPG1hcD4KICAgIDxsb25nIG5hbWU9ImNsaWVudF9zdGF0aWNfa2V5cGFpcl9lbmNfc3VjY2VzcyIgdmFsdWU9IjIzMyIgLz4KICAgIDxzdHJpbmcgbmFtZT0iY2xpZW50X3N0YXRpY19rZXlwYWlyX2VuYyI-WzAsJnF1b3Q7QkNBRURqTlRXaWRiN0U1OFBLV3dcL1FtZFdhQ2FiR2wrdnRaQnVMU2plOERHalVNbnN0bWErTVNNSTM3bWZhT3Y3K21pS0toZ2lFVDNtRHY1cGFFVnl2VlRidmRFemU3d3h5RU81T2txaCtBJnF1b3Q7LCZxdW90O05BVEtORGU5Nm03bFpEMWsmcXVvdDtdPC9zdHJpbmc-CiAgICA8aW50IG5hbWU9ImNsaWVudF9hbmRyb2lkX2tleV9zdG9yZV9hdXRoX3ZlciIgdmFsdWU9IjEiIC8-CiAgICA8c3RyaW5nIG5hbWU9InNlcnZlcl9zdGF0aWNfcHVibGljIj54RG42TXFCUG4zTzZwdERoUFF0L3RxY1hydjJkSzdhUi8vTlFMRklWYWwwPC9zdHJpbmc-CiAgICA8Ym9vbGVhbiBuYW1lPSJjYW5fdXNlcl9hbmRyb2lkX2tleV9zdG9yZSIgdmFsdWU9InRydWUiIC8-CiAgICA8c3RyaW5nIG5hbWU9ImNsaWVudF9zdGF0aWNfa2V5cGFpcl9wd2RfZW5jIj5bMiwmcXVvdDtkeERPUXM3bGpBcHBVcGRLa0xURmVxUkk2QVppVHN3RHlsMlZHYjE2YXh0aG9kM0M1d0o3NGtKd2NxRDBJTkRkQlhMYll6YU1IMWZySUlCMzA3RVdOdyZxdW90OywmcXVvdDsxdGZnQkxHXC9cL0ZnWEFMR3pCWVFnQlEmcXVvdDssJnF1b3Q7dXZvZ3R3JnF1b3Q7LCZxdW90O3pRMVwvK0poODVJWmhWRmpzVVgzNVdRJnF1b3Q7XTwvc3RyaW5nPgo8L21hcD4K\",\"d\":\"MDUyNjFlMzFjYWY0MTBhMzhjODkyNDgwNDk3MDlhNzA4NTgxZTRhOGYwYTk4OWNjOTM4MTI2ZWUyZjJhNjhiZjcy\",\"e\":\"YThkMjU2ZmZhMTAzOWVlYWEzOTg4MGMxYTYyNDFlYzM3ZTc3ZWIyODdkMDg0ZmMyOTIzOGVmYjdkYjFjOWQ1YQ\",\"f\":\"626172163\",\"g\":\"MDgwMDEyMjEwNWFmY2ZmY2NhZGIzNWEwMWMyNmNiODdmOWIyZjI5MWE4OTdkMzU0MDkxMTBkYzYwZjZkYjY3MDVlMTRjYTJlMjMxYTIwNjBmM2YyMThjYjE1YmU3ODcyZDA1NDFlOGMzMWZhMTQ0MDg1ZTRlNDk5MTcxZGQzN2RlYzNlYmI2NDAxZWU0ZTIyNDA2ODk1MjAyZmI3ZGU1MWFhM2UzMDY3ODExODk0Mzk0YWYxYjgzZjUwNjgxZjI0M2I3MmEwY2Y3MzM4MzIzOTQwZjdiOTM3NmM0MzBlYmM2YjJhMzFiZmU4MGViYjFkZTc2NTU5ZmIxZjVmNDE4ZmM5NjEwMjIxOWExNDA4MmIwYzI5M2Q1YzYxN2Y3OTAxMDAwMA\",\"h\":\"LAVA\",\"i\":\"Z60\",\"j\":\"LAVA_Z60_1_16_V1.0_S129_20171113_ENG_IN\",\"k\":10820018176,\"l\":10820018176,\"m\":\"armeabi-v7a\",\"n\":\"ss7991680\",\"o\":\"2.21.9.13\",\"aa\":\"IN\",\"ac\":\"Z60\",\"ab\":\"LAVA\",\"ad\":\"703929f9d092cc19\",\"af\":\"7.0\"}";
        JSONObject json = JSONObject.parseObject(json_str);
        //创建 env数据
        DeviceEnv.AndroidEnv.Builder envBuild = DeviceEnv.AndroidEnv.newBuilder();
        String phone = ParseMe(Base64.decode(json.getString("b")), envBuild);
        //生成一个 db 文件
        //连接数据库
        if (StringUtil.isEmpty(phone)) {
            log.info("获取用户手机号失败");
            return;
        }
        String connectionPath = "jdbc:sqlite:" + "/Users/sunnoc/IdeaProjects/whatsapp-android/out/" + phone + ".db";
        java.sql.Connection axolotlManager = DriverManager.getConnection(connectionPath);
        //创建所有的表
        CrateTables(axolotlManager);

        envBuild.setChatDnsDomain("fb");
        DeviceEnv.UserAgent.Builder useragentBuild = DeviceEnv.UserAgent.newBuilder();
        useragentBuild.setPlatform(DeviceEnv.Platform.ANDROID);
        useragentBuild.setReleaseChannel(DeviceEnv.ReleaseChannel.RELEASE);
        useragentBuild.setOsVersion(json.getString("af"));
        useragentBuild.setManufacturer(json.getString("h"));
        useragentBuild.setDevice(json.getString("ab"));
        useragentBuild.setOsBuildNumber(json.getString("j"));
        envBuild.setUserAgent(useragentBuild);

        ParseKeyPair(Base64.decode(json.getString("c")), envBuild);
        ParsePref(json, envBuild);
        try {
            //插入部 env
            try {
                PreparedStatement preparedStatement = axolotlManager.prepareStatement("INSERT OR REPLACE INTO settings(key, value) VALUES(?, ?)");
                preparedStatement.setString(1, "env");
                preparedStatement.setString(2, Base64.encode(envBuild.build().toByteArray()));
                preparedStatement.execute();
            } catch (Exception e) {
                log.info("插入环境异常");
                return;
            }

            //identities 表添加数据
            {
                PreparedStatement identityPreStatement = axolotlManager.prepareStatement("INSERT OR REPLACE INTO identities(recipient_id, device_id, registration_id, public_key, private_key, next_prekey_id, timestamp) values(-1,0,?,?,?,?,?)");
                identityPreStatement.setInt(1, Integer.parseInt(json.getString("f")));
                identityPreStatement.setBytes(2, StringUtil.HexToBytes(new String(Base64.decode(json.getString("d")))));
                identityPreStatement.setBytes(3, StringUtil.HexToBytes(new String(Base64.decode(json.getString("e")))));
                identityPreStatement.setInt(4, KeyHelper.getRandomSequence(16777214));
                identityPreStatement.setLong(5, System.currentTimeMillis() / 1000);
                identityPreStatement.execute();
            }

            {
                //SignedPreKeyRecord
                SignedPreKeyRecord record = new SignedPreKeyRecord(StringUtil.HexToBytes(new String(Base64.decode(json.getString("g")))));
                try {
                    PreparedStatement preStatement = axolotlManager.prepareStatement("INSERT INTO signed_prekeys (prekey_id, timestamp, record) VALUES(?,?,?)");
                    preStatement.setInt(1, record.getId());
                    preStatement.setLong(2, System.currentTimeMillis() / 1000);
                    preStatement.setBytes(3, record.serialize());
                    preStatement.execute();
                } catch (Exception e) {
                    log.info("生成signedPreKeyRecord异常");

                }
            }
        } catch (Exception e) {
            log.error("生成环境异常", e);
        } finally {
            axolotlManager.close();
        }

    }

    static String ParseMe(byte[] me_data, DeviceEnv.AndroidEnv.Builder envBuild) {
        try {
            MeObjectInputStream objectInputStream = new MeObjectInputStream(new ByteArrayInputStream(me_data));
            Me me = (Me) objectInputStream.readObject();
            //手机号
            envBuild.setFullphone(me.jabber_id);
            return me.jabber_id;
        } catch (Exception e) {
            return null;
        }
    }

    static void ParsePref(JSONObject json, DeviceEnv.AndroidEnv.Builder envBuild) {
        //WA 版本
        String[] versions = json.getString("o").split("\\.");
        DeviceEnv.AppVersion.Builder appVersionOrBuilder = envBuild.getUserAgentBuilder().getAppVersionBuilder();
        appVersionOrBuilder.setPrimary(Integer.parseInt(versions[0]));
        appVersionOrBuilder.setSecondary(Integer.parseInt(versions[1]));
        appVersionOrBuilder.setTertiary(Integer.parseInt(versions[2]));
        if (versions.length >= 4) {
            appVersionOrBuilder.setQuaternary(Integer.parseInt(versions[3]));
        }
        //Country
        String countryInfo = NoiseJni.getCountryInfo2(json.getString("aa"));
        JSONObject jsonObject = JSONObject.parseObject(countryInfo);
        DeviceEnv.UserAgent.Builder userAgentBuilder = envBuild.getUserAgentBuilder();
        userAgentBuilder.setMcc(jsonObject.getString("mcc"));
        userAgentBuilder.setMnc(jsonObject.getString("mnc"));
        userAgentBuilder.setLocaleLanguageIso6391(jsonObject.getString("iso639"));
        userAgentBuilder.setLocaleCountryIso31661Alpha2(jsonObject.getString("iso3166"));

        envBuild.setPushname(json.getString("n"));

        envBuild.setEdgeRoutingInfo(ByteString.copyFrom(Base64.decode("CAIICA")));


        envBuild.getUserAgentBuilder().setPhoneId(UUID.randomUUID().toString());

        String id = UUID.randomUUID().toString();
        envBuild.setFdid(id);
        String exPid = id.substring(0, 20);
        try {
            envBuild.setExpid(ByteString.copyFrom(exPid, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    static void ParseKeyPair(byte[] data, DeviceEnv.AndroidEnv.Builder envBuild) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document dom = builder.parse(new ByteArrayInputStream(data));
            Element root = dom.getDocumentElement();
            NodeList items = root.getElementsByTagName("string");
            for (int i = 0; i < items.getLength(); i++) {
                Element personNode = (Element) items.item(i);
                String key = personNode.getAttribute("name");
                if (key.equals("client_static_keypair_pwd_enc")) {
                    String base64Decode = NoiseJni.stringFromJNI(personNode.getTextContent());
                    byte[] decrpyt = Base64.decode(base64Decode);
                    Env.DeviceEnv.KeyPair.Builder keyBuild = envBuild.getClientStaticKeyPairBuilder();
                    keyBuild.setStrPrivateKey(ByteString.copyFrom(decrpyt, 0, 32));
                    keyBuild.setStrPubKey(ByteString.copyFrom(decrpyt, 32, 32));
                } else if (key.equals("server_static_public")) {
                    envBuild.setServerStaticPublic(ByteString.copyFrom(Base64.decode(personNode.getTextContent())));
                } else if (key.equals("client_static_keypair")) {
                    byte[] b64Keypair = Base64.decode(personNode.getTextContent());
                    Env.DeviceEnv.KeyPair.Builder keyBuild = envBuild.getClientStaticKeyPairBuilder();
                    keyBuild.setStrPrivateKey(ByteString.copyFrom(b64Keypair, 0, 32));
                    keyBuild.setStrPubKey(ByteString.copyFrom(b64Keypair, 32, 32));
                }
            }
        } catch (Exception e) {

        }
    }
}