package com.jhonju.ps3netsrv.utils;

import static android.content.Context.WIFI_SERVICE;

import android.net.wifi.WifiManager;

import com.jhonju.ps3netsrv.PS3NetSrvApp;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Utils {

    /**
     * Get IP address from first non-localhost interface
     *
     * @param useIPv4 true=return ipv4, false=return ipv6
     * @return address or empty string
     */
    public static String getIPAddress(boolean useIPv4) throws Exception {
        WifiManager wm = (WifiManager) PS3NetSrvApp.getAppContext().getApplicationContext().getSystemService(WIFI_SERVICE);
        byte[] bytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(wm.getConnectionInfo().getIpAddress()).array();
        return InetAddress.getByAddress(bytes).getHostAddress();
    }

}