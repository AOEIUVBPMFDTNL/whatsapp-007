package jni;

import ProtocolTree.ProtocolTreeNode;

public class ProtocolNodeJni {
    //0  安装  1 ios
    public static native ProtocolTreeNode Decode(int osType, String version, byte[] data);
    public static native byte[] Encode(int osType, String version, ProtocolTreeNode node);
}
