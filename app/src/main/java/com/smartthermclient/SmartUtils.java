package com.smartthermclient;

import static java.lang.Math.abs;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;

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
    int InvertColor(int color)
    { int r,g,b, a, newcolor, l, l1, dl, dl1,dl0;
        r = (color & 0xff0000)>>16;
        g = (color & 0x00ff00)>>8;
        b = color & 0x0000ff;
        l = r + g*2 +b;
        a = color & 0xff000000;
        if(r > 10) r = 255 - r;
        if(g > 10) g = 255 - g;
        if(b > 10) b = 255 - b;
        l1 = r + g*2 +b;
        dl0 = l1 - l;
        dl = l1 - b - l;
        dl1 = l1 - b +255 - l;
        if(abs(dl0) < abs(dl))
        {   dl0 = dl; b = 0;
        }
        if(abs(dl0) < abs(dl1))
        {   dl0 = dl; b = 255;
        }

        a = 0xff000000;
        newcolor = (r<<16)|(g<<8)|b|a;
        return newcolor;

    }

}
