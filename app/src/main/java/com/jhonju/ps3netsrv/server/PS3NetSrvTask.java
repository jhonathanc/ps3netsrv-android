package com.jhonju.ps3netsrv.server;

import com.jhonju.ps3netsrv.server.enums.EListType;
import com.jhonju.ps3netsrv.server.exceptions.PS3NetSrvException;
import com.jhonju.ps3netsrv.server.utils.FileLogger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Set;

/**
 * PS3NetSrvTask handles the main server loop for the ps3netsrv protocol.
 * 
 * This class is responsible for:
 * - Listening on a specified port for incoming connections
 * - Validating incoming connections based on IP whitelist/blacklist rules
 * - Creating a new ContextHandler for each accepted connection
 * - Properly shutting down the server socket
 * 
 * @author PS3NetSrv Android Contributors
 */
public class PS3NetSrvTask implements Runnable {
  private final Thread.UncaughtExceptionHandler exceptionHandler;
  private final int port;
  private final android.content.ContentResolver contentResolver;
  private final List<String> folderPaths;
  private final int maxConnections;
  private final EListType listType;
  private final Set<String> filterAddresses;
  private ServerSocket serverSocket;
  private volatile boolean isRunning = true;
  private final android.content.Context androidContext;

  /**
   * Checks if the server is currently running.
   * 
   * @return true if server is running, false otherwise
   */
  public boolean isRunning() {
    return isRunning;
  }

  /**
   * Constructs a PS3NetSrvTask with the specified configuration.
   * 
   * @param port The port to listen on
   * @param folderPaths List of folder paths to serve files from
   * @param maxConnections Maximum number of concurrent connections allowed
   * @param filterAddresses Set of IP addresses for whitelist/blacklist filtering
   * @param listType The type of filtering (NONE, ALLOWED, BLOCKED)
   * @param exceptionHandler Handler for uncaught exceptions
   * @param contentResolver Android ContentResolver for file access
   */
  public PS3NetSrvTask(int port, List<String> folderPaths, int maxConnections, Set<String> filterAddresses,
      EListType listType, Thread.UncaughtExceptionHandler exceptionHandler,
      android.content.ContentResolver contentResolver, android.content.Context androidContext) {
    this.port = port;
    this.contentResolver = contentResolver;
    this.folderPaths = folderPaths;
    this.maxConnections = maxConnections;
    this.filterAddresses = filterAddresses;
    this.listType = listType;
    this.exceptionHandler = exceptionHandler;
    this.androidContext = androidContext;
  }

  /**
   * Main server loop that accepts and handles incoming client connections.
   * 
   * This method:
   * 1. Creates a ServerSocket on the specified port
   * 2. Accepts incoming connections in a loop
   * 3. Validates each connection against whitelist/blacklist rules
   * 4. Creates a new ContextHandler thread for each valid connection
   * 5. Logs all significant events for debugging purposes
   */
  @Override
  public void run() {
    try {
      this.serverSocket = new ServerSocket();
      this.serverSocket.setReuseAddress(true);
      this.serverSocket.bind(new java.net.InetSocketAddress(port));
      FileLogger.logInfo("PS3NetSrv server started listening on port: " + port);

      while (isRunning) {
        try {
          Socket clientSocket = serverSocket.accept();
          String hostAddress = clientSocket.getInetAddress().getHostAddress();

          if (!allowIncomingConnection(hostAddress)) {
            FileLogger.logWarning("Incoming connection blocked from IP: " + hostAddress +
                " (Filter type: " + listType + ")");
            exceptionHandler.uncaughtException(null, new PS3NetSrvException(
                this.androidContext.getString(
                    com.jhonju.ps3netsrv.R.string.error_blocked_connection, hostAddress)));
            try (Context ignored = new Context(clientSocket, folderPaths, contentResolver, androidContext)) {
              // try-with-resources closes the socket automatically
            }
            continue;
          }

          FileLogger.logInfo("Client connected from IP: " + hostAddress);
          if (maxConnections > 0 && ContextHandler.getSimultaneousConnections() >= maxConnections) {
            FileLogger.logWarning("Connection limit reached (" + maxConnections + "). Rejecting " + hostAddress);
            try (Context ignored = new Context(clientSocket, folderPaths, contentResolver, androidContext)) {
              // try-with-resources closes the socket automatically
            }
            continue;
          }
          new ContextHandler(clientSocket, folderPaths, contentResolver,
              maxConnections, exceptionHandler, androidContext).start();
        } catch (IOException e) {
          if (isRunning) {
            FileLogger.logError("Error accepting client connection", e);
            exceptionHandler.uncaughtException(null, e);
          }
        }
      }
    } catch (IOException e) {
      FileLogger.logError("Failed to start server on port " + port, e);
      exceptionHandler.uncaughtException(null, e);
    } finally {
      shutdown();
    }
  }

  /**
   * Determines whether an incoming connection from the specified IP address should be accepted.
   * 
   * Logic:
   * - LIST_TYPE_NONE: Accept all connections
   * - LIST_TYPE_ALLOWED: Accept only if IP is in the whitelist
   * - LIST_TYPE_BLOCKED: Accept all except those in the blacklist
   * 
   * @param hostAddress The IP address of the incoming connection
   * @return true if connection should be accepted, false if it should be rejected
   */
  private boolean allowIncomingConnection(String hostAddress) {
    if (listType == EListType.LIST_TYPE_NONE) {
      return true;
    }
    
    if (filterAddresses == null || filterAddresses.isEmpty()) {
      return (listType == EListType.LIST_TYPE_BLOCKED);
    }
    
    boolean addressExists = filterAddresses.contains(hostAddress);
    
    if (listType == EListType.LIST_TYPE_ALLOWED) {
      return addressExists;
    }
    
    return !addressExists;
  }

  /**
   * Gracefully shuts down the server.
   * 
   * This method:
   * 1. Sets isRunning flag to false to stop accepting new connections
   * 2. Closes the server socket
   * 3. Logs the shutdown event
   * 4. Handles any IOException that occurs during socket closure
   */
  public void shutdown() {
    isRunning = false;
    try {
      if (serverSocket != null && !serverSocket.isClosed()) {
        serverSocket.close();
        FileLogger.logInfo("PS3NetSrv server socket closed");
      }
    } catch (IOException e) {
      FileLogger.logError("Error closing server socket", e);
    } finally {
      serverSocket = null;
    }
  }
}