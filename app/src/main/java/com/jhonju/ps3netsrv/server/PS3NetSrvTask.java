package com.jhonju.ps3netsrv.server;

import com.jhonju.ps3netsrv.server.enums.EListType;
import com.jhonju.ps3netsrv.server.exceptions.PS3NetSrvException;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;

public class PS3NetSrvTask implements Runnable {
    private final Thread.UncaughtExceptionHandler exceptionHandler;
    private final int port;
    private final String folderPath;
    private final int maxConnections;
    private final boolean readOnly;
    private final EListType listType;
    private final Set<String> filterAddresses;
    private ServerSocket serverSocket;
    private boolean isRunning = true;

    public PS3NetSrvTask(int port, String folderPath, int maxConnections, boolean readOnly, Set<String> filterAddresses, EListType listType, Thread.UncaughtExceptionHandler exceptionHandler) {
        this.port = port;
        this.folderPath = folderPath;
        this.maxConnections = maxConnections;
        this.readOnly = readOnly;
        this.filterAddresses = filterAddresses;
        this.listType = listType;

        this.exceptionHandler = exceptionHandler;
    }

    public PS3NetSrvTask(int port, String folderPath, Thread.UncaughtExceptionHandler exceptionHandler) {
        this.port = port;
        this.folderPath = folderPath;
        this.maxConnections = 0;
        this.readOnly = false;
        this.filterAddresses = null;
        this.listType = EListType.LIST_TYPE_NONE;

        this.exceptionHandler = exceptionHandler;
    }

    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            while (isRunning) {
                Socket clientSocket = serverSocket.accept();
                String hostAddress = clientSocket.getInetAddress().getHostAddress();
                if (!allowIncomingConnection(hostAddress)) {
                    exceptionHandler.uncaughtException(null, new PS3NetSrvException(String.format("Blocked connection: %s", hostAddress)));
                    try {
                        clientSocket.close();
                    } catch (IOException e) {
                        exceptionHandler.uncaughtException(null, e);
                    }
                    continue;
                }
                new ContextHandler(new Context(clientSocket, folderPath, readOnly), maxConnections, exceptionHandler).start();
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
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        } finally {
            serverSocket = null;
        }
    }
}
