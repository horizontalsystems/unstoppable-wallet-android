package bitcoin.walllet.kit.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class NetworkUtils {

    private static InetAddress local = null;

    public static InetAddress getLocalInetAddress() {
        if (local == null) {
        }
        try {
            local = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        return local;
    }

    public static byte[] getIPv6(InetAddress inetAddr) {
        byte[] ip = inetAddr.getAddress();
        if (ip.length == 16) {
            return ip;
        }
        if (ip.length == 4) {
            byte[] ipv6 = new byte[16];
            ipv6[10] = -1;
            ipv6[11] = -1;
            System.arraycopy(ip, 0, ipv6, 12, 4);
            return ipv6;
        }
        throw new RuntimeException("Bad IP: " + HashUtils.toHexString(ip));
    }
}
