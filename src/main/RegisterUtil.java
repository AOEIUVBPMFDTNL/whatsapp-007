package com.whatsapp.android.api.register;

import Env.DeviceEnv;
import Util.StringUtil;
import axolotl.AxolotlManager;
import com.google.protobuf.ByteString;
import jni.Register;
import org.whispersystems.libsignal.logging.Log;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.util.Base64;

public class RegisterUtil {
    public interface RegisterDelegate {
        public void OnCheckExist(int code, String desc);
        public void OnRegister(int code, String desc);
    }

    RegisterDelegate delegate_;
    Proxy proxy_;
    String userName_; String password_;

    public RegisterUtil(RegisterDelegate delegate, Proxy proxy, String userName, String password) {
        delegate_ = delegate;
        proxy_ = proxy;
        userName_ = userName;
        password_ = password;
    }

    Call httpCall_;

    private static final String TAG = RegisterUtil.class.getName();

    AxolotlManager axolotlManager;
    String cc_;
    String phone_;

    public void Cleanup() {
        if (null != axolotlManager) {
            axolotlManager.Close();
            axolotlManager = null;
        }
        if (null != httpCall_) {
            httpCall_.cancel();
        }
    }

    public boolean CheckAccountExist(String cc, String phone) {
        cc_ = cc;
        phone_ = phone;
        if (null == axolotlManager) {
            String dataDir = System.getProperty("user.dir") + "/register";
            new File(dataDir).mkdirs();

            String dbPath = new File(dataDir, cc + phone).getAbsolutePath();
            axolotlManager = new AxolotlManager(dbPath);
        }


        DeviceEnv.RegisterParams.Builder existParams = DeviceEnv.RegisterParams.newBuilder();
        existParams.setCc(cc);
        existParams.setPhone(phone);
        existParams.setRegid(ByteString.copyFrom(GorgeousEngine.AdjustId(axolotlManager.getLocalRegistrationId())));
        existParams.setIdentity(ByteString.copyFrom(axolotlManager.GetIdentityKeyPair().getPublicKey().serialize(), 1, 32));


        SignedPreKeyRecord signedPreKeyRecord = axolotlManager.LoadLatestSignedPreKey(false);
        existParams.setSkeyId(ByteString.copyFrom(GorgeousEngine.AdjustId(signedPreKeyRecord.getId())));
        existParams.setSkeyVal(ByteString.copyFrom(signedPreKeyRecord.getKeyPair().getPublicKey().serialize(), 1, 32));
        existParams.setSkeySig(ByteString.copyFrom(signedPreKeyRecord.getSignature()));



        String encodeParams = Register.EncodeExistRequest(existParams.build().toByteArray(), axolotlManager.GetBytesSetting("env"));
        JSONObject encodeJson = new JSONObject(encodeParams);
        if (encodeJson.getInt("code") != 0) {
            return false;
        }
        if (axolotlManager.GetBytesSetting("env") == null) {
            axolotlManager.SetStringSetting("env", encodeJson.getString("env"));
        }

        java.net.Authenticator.setDefault(new java.net.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(userName_, password_.toCharArray());
            }
        });


        //send http
        try {
            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(encodeJson.getString("request"))
                    .addHeader("User-Agent", encodeJson.getString("useragent"))
                    .addHeader("Accept-Charset", "UTF-8")
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .get().build();
            OkHttpClient okHttpClient = new OkHttpClient().newBuilder().hostnameVerifier((s, sslSession) -> true).proxy(proxy_).authenticator(new Authenticator() {
                @Nullable
                @Override
                public Request authenticate(@Nullable Route route, @NotNull Response response) throws IOException {
                    String credential = Credentials.basic(userName_, password_);
                    return response.request().newBuilder()
                            .header("Authorization", credential)
                            .build();
                }
            }).build();

            httpCall_ = okHttpClient.newCall(request);
            httpCall_.enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    httpCall_ = null;
                    Log.e(TAG, e.getLocalizedMessage());
                    delegate_.OnCheckExist(-1 , e.getLocalizedMessage());
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    httpCall_ = null;
                    if (!response.isSuccessful()) {
                        delegate_.OnCheckExist(-2, response.body().string());
                        return;
                    }

                    DeviceEnv.AndroidEnv.Builder envBuilder = DeviceEnv.AndroidEnv.parseFrom(axolotlManager.GetBytesSetting("env")).toBuilder();
                    JSONObject responseJson = new JSONObject(response.body().string());
                    String status = responseJson.getString("status");
                    Log.d(TAG, responseJson.toString());
                    if (status.equals("ok")) {
                        envBuilder.setEdgeRoutingInfo(ByteString.copyFrom(Base64.getDecoder().decode(responseJson.getString("edge_routing_info"))));
                        String chatDnsDomain = responseJson.getString("chat_dns_domain");
                        if (StringUtil.isEmpty(chatDnsDomain)) {
                            envBuilder.setChatDnsDomain("fb");
                        } else {
                            envBuilder.setChatDnsDomain(chatDnsDomain);
                        }
                        axolotlManager.SetBytesSetting("env", envBuilder.build().toByteArray());
                    }
                    RequestCode();
                }
            });
        }
        catch (Exception e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
        return true;
    }



    boolean RequestCode() {
        DeviceEnv.RegisterParams.Builder existParams = DeviceEnv.RegisterParams.newBuilder();
        existParams.setCc(cc_);
        existParams.setPhone(phone_);
        existParams.setRegid(ByteString.copyFrom(GorgeousEngine.AdjustId(axolotlManager.getLocalRegistrationId())));
        existParams.setIdentity(ByteString.copyFrom(axolotlManager.GetIdentityKeyPair().getPublicKey().serialize(), 1, 32));


        SignedPreKeyRecord signedPreKeyRecord = axolotlManager.LoadLatestSignedPreKey(false);
        existParams.setSkeyId(ByteString.copyFrom(GorgeousEngine.AdjustId(signedPreKeyRecord.getId())));
        existParams.setSkeyVal(ByteString.copyFrom(signedPreKeyRecord.getKeyPair().getPublicKey().serialize(), 1, 32));
        existParams.setSkeySig(ByteString.copyFrom(signedPreKeyRecord.getSignature()));


        String encodeParams = Register.CodeRequest(existParams.build().toByteArray(), axolotlManager.GetBytesSetting("env"));
        JSONObject encodeJosn = new JSONObject(encodeParams);
        if (encodeJosn.getInt("code") != 0) {
            return false;
        }

        //send http
        try {
            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(encodeJosn.getString("request"))
                    .addHeader("User-Agent", encodeJosn.getString("useragent"))
                    .addHeader("Accept-Charset", "UTF-8")
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .get().build();
            OkHttpClient okHttpClient = new OkHttpClient().newBuilder().hostnameVerifier((s, sslSession) -> true).proxy(proxy_).build();
            httpCall_ = okHttpClient.newCall(request);
            httpCall_.enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    httpCall_ = null;
                    Log.e(TAG, e.getLocalizedMessage());
                    delegate_.OnCheckExist(-3 , e.getLocalizedMessage());
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    httpCall_ = null;
                    if (!response.isSuccessful()) {
                        delegate_.OnCheckExist(-4, response.body().string());
                        return;
                    }

                    JSONObject responseJson = new JSONObject(response.body().string());
                    String status = responseJson.getString("status");
                    Log.d(TAG, responseJson.toString());
                    if (status.equals("sent")) {
                        delegate_.OnCheckExist(0, responseJson.toString());
                        return;
                    }
                    delegate_.OnCheckExist(-5, responseJson.toString());
                }
            });
        }
        catch (Exception e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
        return  true;
    }


    public boolean  Register(String smsCode) {
        if (null != httpCall_) {
            delegate_.OnRegister(-6, "request is still running");
            return false;
        }
        DeviceEnv.RegisterParams.Builder existParams = DeviceEnv.RegisterParams.newBuilder();
        existParams.setCc(cc_);
        existParams.setPhone(phone_);
        existParams.setRegid(ByteString.copyFrom(GorgeousEngine.AdjustId(axolotlManager.getLocalRegistrationId())));
        existParams.setIdentity(ByteString.copyFrom(axolotlManager.GetIdentityKeyPair().getPublicKey().serialize(), 1, 32));
        existParams.setCode(smsCode);

        SignedPreKeyRecord signedPreKeyRecord = axolotlManager.LoadLatestSignedPreKey(false);
        existParams.setSkeyId(ByteString.copyFrom(GorgeousEngine.AdjustId(signedPreKeyRecord.getId())));
        existParams.setSkeyVal(ByteString.copyFrom(signedPreKeyRecord.getKeyPair().getPublicKey().serialize(), 1, 32));
        existParams.setSkeySig(ByteString.copyFrom(signedPreKeyRecord.getSignature()));


        String encodeParams = Register.Register(existParams.build().toByteArray(), axolotlManager.GetBytesSetting("env"));
        JSONObject encodeJosn = new JSONObject(encodeParams);
        if (encodeJosn.getInt("code") != 0) {
            return false;
        }

        //send http
        try {
            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(encodeJosn.getString("request"))
                    .addHeader("User-Agent", encodeJosn.getString("useragent"))
                    .addHeader("Accept-Charset", "UTF-8")
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .get().build();
            OkHttpClient okHttpClient = new OkHttpClient().newBuilder().hostnameVerifier((s, sslSession) -> true).proxy(proxy_).build();;
            Call call = okHttpClient.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    httpCall_ = null;
                    delegate_.OnRegister(-7 , e.getLocalizedMessage());
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    httpCall_ = null;
                    if (!response.isSuccessful()) {
                        delegate_.OnRegister(-8, response.body().string());
                        return;
                    }


                    JSONObject responseJson = new JSONObject(response.body().string());
                    Log.d(TAG, responseJson.toString());
                    String status = responseJson.getString("status");
                    if (!status.equals("ok")) {
                        delegate_.OnRegister(-9 , responseJson.toString());
                        return;
                    }
                    DeviceEnv.AndroidEnv.Builder envBuilder = DeviceEnv.AndroidEnv.parseFrom(axolotlManager.GetBytesSetting("env")).toBuilder();
                    envBuilder.setEdgeRoutingInfo(ByteString.copyFrom(Base64.getDecoder().decode(responseJson.getString("edge_routing_info"))));

                    if (!responseJson.has("chat_dns_domain")) {
                        envBuilder.setChatDnsDomain("fb");
                    } else {
                        envBuilder.setChatDnsDomain(responseJson.getString("chat_dns_domain"));
                    }
                    axolotlManager.SetBytesSetting("env", envBuilder.build().toByteArray());
                    delegate_.OnRegister(0, responseJson.toString());
                }
            });
        }
        catch (Exception e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
        return true;
    }
}
