package com.imx.examples;

import cn.hutool.core.util.HexUtil;
import com.alibaba.fastjson.JSONObject;
import com.imx.apns.activate.ActivationInfo;
import com.imx.apns.common.APNsState;
import com.imx.common.util.ByteUtil;
import com.imx.netty.core.APNsClientProcessor;
import com.imx.netty.core.FutureNotification;
import com.imx.netty.core.PayloadResult;
import com.whatsapp.android.entity.ProxyInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

@Slf4j
public class APNsExample2 {

    public static void main(String[] args) {
        List<String> topics = Collections.singletonList("net.whatsapp.WhatsApp");
        // 消息通知
        FutureNotification<PayloadResult> futureNotification = new FutureNotification<PayloadResult>() {
            @Override
            public void OnApnsConnectSuccess() {
                log.info("Apns连接成功");
            }

            @Override
            public void OnApnsNotify(PayloadResult payloadResult) {
                log.info("Received notification: {}", JSONObject.toJSONString(payloadResult));
            }

            @Override
            public void OnApnsState(APNsState apnsState) {

            }

            @Override
            public void OnApnsClose(String reason, boolean force) {
                log.info("Apns连接断开: {}", reason);
            }
        };
        /*ProxyInfo proxyInfo = new ProxyInfo();
        proxyInfo.setType(0);
        proxyInfo.setProxyHost("216.238.104.116");
        proxyInfo.setProxyPort(39920);
        proxyInfo.setProxyUser("fans007");
        proxyInfo.setProxyPwd("fans888");*/
        ProxyInfo proxyInfo = new ProxyInfo();
        proxyInfo.setType(1);
        proxyInfo.setProxyHost("net.elfproxy.com");
        proxyInfo.setProxyPort(12325);
        proxyInfo.setProxyUser("rKB4GVCMO");
        proxyInfo.setProxyPwd("12345678-0_country-us_city-_session-0pTmqNGw_lifetime-1440m");
        try {
            String state = "aced00057372001d636f6d2e696d782e61706e732e636f6d6d6f6e2e41504e735374617465cdd33b77c99ce54d0200034c0004706169727400154c636f6d2f696d782f636f6d6d6f6e2f506169723b5b0005746f6b656e7400025b424c0006746f706963737400104c6a6176612f7574696c2f4c6973743b787073720013636f6d2e696d782e636f6d6d6f6e2e506169725223bc45c9be458d0200024c00036b65797400124c6a6176612f6c616e672f4f626a6563743b4c000576616c756571007e00067870757200025b42acf317f8060854e002000078700000027930820275020100300d06092a864886f70d01010105000482025f3082025b0201000281810092ef880a6467125aec5a5340dc7bb73fa55e6dcc3e98fb0d3d930f47fbed292ae157e99359b3fddc42e39f8cf822229e43032deac1b1f63548ed6549ba2bc4dbbd9fa7c5a7985fd48a6a2766ee363fae91d944d650fa97a5ce1890ec228bd6e49eb2dd7183e7ff2ed06980752d221f54446c192bab3f6abe2577e5b297180329020301000102818046591a27e794158ca496463fca900cd7130497caeeb96d911446e14d1487a26b1e4269d91b5c7a91471115e5773358a1aef1c9bd18896c986c5704647d16d183a90bd57b9980ed8eb1aa906006de49e414ff377c8a21d0ab0cf476b12c8464691fca9d15205326776db04928f860763a91a499ddc5f1e4858642b40800c32f15024100f1ec9f9ba322fcebce8b2c6110c35c5c8dec035864e9cee2725fbab0dc362a8020b18480f9933dfeff4ce4bf115a8b0cf972d4f45bf7b3e0caa33b7582cdc1870241009b7c15ff7c487cdd9005f6924f3260c271f242ebf27de90e91cf9a5726dc83e349815828926c21a4e0ef1fba3d9c5d7368a56199e4ff63f9bfd5d87c3d1101cf02401b932b06246c22840640dac81d8f07020db32f166e3a3038a36cc1ecd2cce1bf44fab6edb484d1f634c760f35e5901ac72ea61d7907c0566c3f2231edbcff41502407cb0a53f8f1edeac1c4cfbf1577bd226fd9447e0ca45f939caeb4f1de7375eb94e8060ffa075010225b4fe9fafbb0f227760718626243dbbd3011eac7b9880f702404a5cc1ea4526b94b215423cf67b700261aae8c3d5dbb23b2c44207b7e276f72efb3dad5b0cb212a9d31d8de47135036859ca5dbc914ec18fbf26f19b5d114be77571007e0008000002f7308202f33082025ca003020102020a048bb00e4ef765a4e498300d06092a864886f70d0101050500305a310b300906035504061302555331133011060355040a130a4170706c6520496e632e31153013060355040b130c4170706c65206950686f6e65311f301d060355040313164170706c65206950686f6e6520446576696365204341301e170d3235303630343038343434395a170d3236303630343038343934395a308183310b3009060355040613025553310b300906035504080c0243413112301006035504070c09437570657274696e6f31133011060355040a0c0a4170706c6520496e632e310f300d060355040b0c066950686f6e65312d302b0603550403162445423836393435342d463043452d343934382d413446432d30394435453039334437333930819f300d06092a864886f70d010101050003818d003081890281810092ef880a6467125aec5a5340dc7bb73fa55e6dcc3e98fb0d3d930f47fbed292ae157e99359b3fddc42e39f8cf822229e43032deac1b1f63548ed6549ba2bc4dbbd9fa7c5a7985fd48a6a2766ee363fae91d944d650fa97a5ce1890ec228bd6e49eb2dd7183e7ff2ed06980752d221f54446c192bab3f6abe2577e5b2971803290203010001a38195308192301f0603551d23041830168014b2fe21234486956a79d581268e7310d8a74c8e74301d0603551d0e0416041405bc285c1713f5da606ee82a87508aedc7afde7e300c0603551d130101ff04023000300e0603551d0f0101ff0404030205a030200603551d250101ff0416301406082b0601050507030106082b060105050703023010060a2a864886f76364060a0404020500300d06092a864886f70d0101050500038181004f8ca7c23c86ec9817cf8ed732b9e3a992951e1446088b0ee0b5d038c0fdcb7ccc22b080edea4f868ec6680c117614bff75cf6b82f3d86a435ce6fcb743c385fc24c5f666e9af016c078f6d7469ce3be4ef4476f1572f95b8ce48a162d12ff067107ada6abb9510308a42a5719b39216cf99a713b487cd34be25fd0e41be99a87571007e0008000000209de119df0af7731ad0806170d4b0574b94ca4ea00de8ecc6dc6af6f090cf1ead737200236a6176612e7574696c2e436f6c6c656374696f6e732453696e676c65746f6e4c6973742aef29103ca79b970200014c0007656c656d656e7471007e000678707400156e65742e77686174736170702e5768617473417070";
            APNsState apnsState = ByteUtil.deserialize(ByteUtil.hexToBytes(state));
            byte[] token = apnsState.getToken();
            String s = HexUtil.encodeHexStr(token);
            System.out.println(s);
            APNsClientProcessor apNsClientProcessor = new APNsClientProcessor(apnsState, topics, proxyInfo, futureNotification);
            apNsClientProcessor.process2();
        } catch (Throwable t) {
            System.err.println("Process exception: " + t.getMessage());
        }
    }
}
