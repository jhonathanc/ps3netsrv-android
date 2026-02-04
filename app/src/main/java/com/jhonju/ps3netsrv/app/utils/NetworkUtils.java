package com.jhonju.ps3netsrv.app.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.jhonju.ps3netsrv.app.PS3NetSrvApp;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class NetworkUtils {

  public static String getIPAddress(boolean useIPv4) throws Exception {
    ConnectivityManager connManager = (ConnectivityManager) PS3NetSrvApp.getAppContext().getApplicationContext()
        .getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo activeNetwork = connManager.getActiveNetworkInfo();

    if (activeNetwork != null && activeNetwork.isConnected()) {
      Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();

      while (networkInterfaces.hasMoreElements()) {
        NetworkInterface networkInterface = networkInterfaces.nextElement();
        if (networkInterface.isUp() && !networkInterface.isLoopback()
            && networkInterface.getInterfaceAddresses().size() > 0) {
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
    ConnectivityManager connManager = (ConnectivityManager) PS3NetSrvApp.getAppContext().getApplicationContext()
        .getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo ethernetInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);
    if (ethernetInfo != null && ethernetInfo.isConnected()) {
      return true;
    } else {
      NetworkInfo wifiInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
      return (wifiInfo != null && wifiInfo.isConnected());
    }
  }
}