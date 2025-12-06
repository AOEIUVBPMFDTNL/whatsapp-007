package Report;

import Handshake.NoiseHandshake;
import Message.WhatsMessage;
import ProtocolTree.ProtocolTreeNode;
import ProtocolTree.StanzaAttribute;
import QRcode.AnonymousClass0DP;
import QRcode.C05350Mv;
import QRcode.QRCodeInfo;
import Util.StringUtil;
import axolotl.AxolotlManager;
import cn.hutool.core.util.HexUtil;
import com.whatsapp.android.GorgeousEngine;
import com.whatsapp.android.util.LibLoader;
import jni.ProtocolNodeJni;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class Test {
    public static void main(String[] args) throws Exception {


        String os = System.getProperty("os.name");
        if (os.startsWith("Linux")) {
            LibLoader.loadLib("libNoiseJni.so");
        } else if (os.startsWith("Windows")) {
            LibLoader.loadLib("libNoiseJni.dll");
        } else if (os.startsWith("Mac")) {
            LibLoader.loadLib("libNoiseJni.dylib");
        }
        /*
        *
        *
        *
        *  while (true) {
            try {
                BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
                while (true) {
                    String msg = console.readLine();
                    if (msg.isEmpty()) {
                        continue;
                    }
                    switch (msg) {
                        case "Web":{
                            String code = console.readLine();
                            client.engine.ScanWebWhatsapp(code);
                        }
                        break;
                        case "1" :{
                            client.engine.SendOnePacket();
                        }
                        break;
                        case "2" : {
                            client.engine.SendTwoPacket();
                        }
                        break;
                        case "3" : {
                            client.engine.SendThreePacket();
                        }
                        break;
                        case "4" : {
                            client.engine.SendFourPacket();
                        }
                        break;
                        case "5" : {
                            client.engine.SendFivePacket();
                        }
                        break;
                        case "6" : {
                            client.engine.SendSixPacket();
                        }
                        break;
                        case "7" :{
                            client.engine.PresenceWeb();
                        }
                        break;
                        case "8" :{
                            client.engine.Send8Packet();
                        }
                        break;
                    }
                }
            }
            catch (Exception e) {

            }
        }
        *
        * */

    }
}
