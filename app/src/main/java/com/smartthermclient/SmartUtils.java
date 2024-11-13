package com.smartthermclient;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class SmartUtils {
    String myIPaddr;
    static int mask; //NetworkPrefixLength

    SmartUtils()
    {   mask = 0;

    }

    public static long ipToLong(InetAddress ip) {
        byte[] octets = ip.getAddress();
        long result = 0;
        for (byte octet : octets) {
            result <<= 8;
            result |= octet & 0xff;
        }
        return result;
    }

    int CheckIPis_inRage(String TestIP)
    {   int rc = 0;
        int netmask;
        long ipTest;
        long ipMy = 0;
        try {
            ipMy = ipToLong(InetAddress.getByName(myIPaddr));
            ipTest = ipToLong(InetAddress.getByName(TestIP));
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        netmask  = 0xffffffff << (32 - mask);
        long ipMyNetFirst = (ipMy & netmask);
        long ipMyNetLast  = (ipMy | (~netmask));
        if((ipMyNetFirst < ipTest) && (ipTest <ipMyNetLast ))
            rc = 1;

        return rc;
    }

    public void GetMyIPandMask()
    {   mask = 0;
        myIPaddr = getIPAddress(true);
    }

    public static String getIPAddress(boolean useIPv4) {
        int n;
        String str;
        InterfaceAddress addressI;
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                n = 0;
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress().toUpperCase();
                        boolean isIPv4 = addr instanceof Inet4Address;
                        if (useIPv4) {
                            if (isIPv4) {
                                addressI = intf.getInterfaceAddresses().get(n);
                                mask = addressI.getNetworkPrefixLength();
                                //str = String.format(Locale.ROOT, "/%d", mask);
                                //sAddr += str;
                                return sAddr;
                            }
                        } else {
                            if (!isIPv4) { //todo
                                int delim = sAddr.indexOf('%'); // drop ip6 port suffix
                                return delim<0 ? sAddr : sAddr.substring(0, delim);
                            }
                        }
                    }
                    n++;
                }
            }
        } catch (Exception ex) { } // for now eat exceptions
        return "";
    }

}
