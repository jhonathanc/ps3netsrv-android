package com.jhonju.ps3netsrv.server;

import com.jhonju.ps3netsrv.server.enums.EListType;
import com.jhonju.ps3netsrv.server.exceptions.PS3NetSrvException;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Set;

public class PS3NetSrvTask implements Runnable {
  private final Thread.UncaughtExceptionHandler exceptionHandler;
  private final int port;
  private final List<String> folderPaths;
  private final int maxConnections;
  private final EListType listType;
  private final Set<String> filterAddresses;
  private ServerSocket serverSocket;
  private boolean isRunning = true;

  public boolean isRunning() {
    return isRunning;
  }

  public PS3NetSrvTask(int port, List<String> folderPaths, int maxConnections, Set<String> filterAddresses,
      EListType listType, Thread.UncaughtExceptionHandler exceptionHandler) {
    this.port = port;
    this.folderPaths = folderPaths;
    this.maxConnections = maxConnections;
    this.filterAddresses = filterAddresses;
    this.listType = listType;

    this.exceptionHandler = exceptionHandler;
  }

  public void run() {
    try {
      serverSocket = new ServerSocket(port);
      while (isRunning) {
        Socket clientSocket = serverSocket.accept();
        String hostAddress = clientSocket.getInetAddress().getHostAddress();
        if (!allowIncomingConnection(hostAddress)) {
          exceptionHandler.uncaughtException(null, new PS3NetSrvException(com.jhonju.ps3netsrv.app.PS3NetSrvApp
              .getAppContext().getString(com.jhonju.ps3netsrv.R.string.error_blocked_connection, hostAddress)));
          try {
            clientSocket.close();
          } catch (IOException e) {
            exceptionHandler.uncaughtException(null, e);
          }
          continue;
        }
        new ContextHandler(new Context(clientSocket, folderPaths), maxConnections, exceptionHandler).start();
      }
    } catch (IOException e) {
      exceptionHandler.uncaughtException(null, e);
    } finally {
      shutdown();
    }
  }

  private boolean allowIncomingConnection(String hostAddress) {
    if (listType == EListType.LIST_TYPE_NONE) {
      return true;
    }
    if (filterAddresses == null) {
      return (listType == EListType.LIST_TYPE_BLOCKED);
    }
    boolean addressExists = filterAddresses.contains(hostAddress);
    if (listType == EListType.LIST_TYPE_ALLOWED) {
      return addressExists;
    }
    return !addressExists;
  }

  public void shutdown() {
    isRunning = false;
    try {
      if (serverSocket != null)
        serverSocket.close();
    } catch (IOException e) {
      System.err.println(e.getMessage());
    } finally {
      serverSocket = null;
    }
  }
}
