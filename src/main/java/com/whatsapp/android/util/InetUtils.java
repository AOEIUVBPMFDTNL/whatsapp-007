package com.whatsapp.android.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

public class InetUtils {

    public static String getSelfIP() {
        String ipCandidate = null;
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                // Check if the interface is up, not loopback, not virtual (based on name heuristics)
                if (networkInterface.isUp() && !networkInterface.isLoopback() && !isVirtualInterface(networkInterface)) {
                    Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress address = addresses.nextElement();
                        // Ensure the address is an IPv4 address, not loopback, link-local, or multicast
                        if (!address.getHostAddress().contains(":") && !address.isLoopbackAddress() && !address.isLinkLocalAddress() && !address.isMulticastAddress()) {
                            // Prefer the first non-virtual address found, but continue searching for an ethernet interface
                            if (ipCandidate == null) {
                                ipCandidate = address.getHostAddress();
                            }
                            if (networkInterface.getName().startsWith("eth")) {
                                return address.getHostAddress();
                            }
                        }
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return ipCandidate;
    }

    private static boolean isVirtualInterface(NetworkInterface networkInterface) {
        String name = networkInterface.getName();
        return name.startsWith("docker") || name.startsWith("lo") || name.startsWith("veth") || name.startsWith("br-") || name.contains("virtual");
    }

    public static boolean isPrivateIP(String ip) {
        if (ip == null) {
            return false;
        }
        try {
            InetAddress inetAddress = InetAddress.getByName(ip);
            byte[] addr = inetAddress.getAddress();
            int firstOctet = addr[0] & 0xFF;
            int secondOctet = addr[1] & 0xFF;

            return (firstOctet == 10) // 10.0.0.0 - 10.255.255.255
                    || (firstOctet == 172 && (secondOctet >= 16 && secondOctet <= 31)) // 172.16.0.0 - 172.31.255.255
                    || (firstOctet == 192 && secondOctet == 168); // 192.168.0.0 - 192.168.255.255
        } catch (UnknownHostException e) {
            return false;
        }
    }

    public static void main(String[] args) {
        System.out.println(getSelfIP());
    }
}
