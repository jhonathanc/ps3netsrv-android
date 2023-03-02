package com.jhonju.ps3netsrv.utils;

import static android.content.Context.WIFI_SERVICE;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

import com.jhonju.ps3netsrv.PS3NetSrvApp;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Enumeration;

public class Utils {

    /**
     * Get IP address from first non-localhost interface
     *
     * @param useIPv4 true=return ipv4, false=return ipv6
     * @return address or empty string
     */
    public static String getIPAddress(boolean useIPv4) throws Exception {
        ConnectivityManager connManager = (ConnectivityManager) PS3NetSrvApp.getAppContext().getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connManager.getActiveNetworkInfo();

        if (activeNetwork != null && activeNetwork.isConnected()) {
            // Obtém a lista de interfaces de rede disponíveis
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();

            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();

                // Verifica se a interface de rede está conectada e é a mesma que a conexão ativa
                if (networkInterface.isUp() && !networkInterface.isLoopback() && networkInterface.getInterfaceAddresses().size() > 0) {
                    for (InterfaceAddress address : networkInterface.getInterfaceAddresses()) {
                        InetAddress inetAddress = address.getAddress();
                        if (useIPv4 && inetAddress instanceof Inet4Address) {
                            return inetAddress.getHostAddress();
                        } else if (!useIPv4 && inetAddress instanceof Inet6Address) {
                            return inetAddress.getHostAddress();
                        }
                    }
                }
            }
        }
        return "<NOT ABLE TO GET IP>";
    }

    public static boolean isConnectedToLocal() {
        ConnectivityManager connManager = (ConnectivityManager) PS3NetSrvApp.getAppContext().getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ethernetInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);
        if (ethernetInfo != null && ethernetInfo.isConnected()) {
            return true;
        } else {
            NetworkInfo wifiInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            return (wifiInfo != null && wifiInfo.isConnected());
        }
    }

}