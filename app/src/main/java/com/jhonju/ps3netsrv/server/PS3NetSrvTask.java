package com.jhonju.ps3netsrv.server;

import com.jhonju.ps3netsrv.server.enums.EListType;
import com.jhonju.ps3netsrv.server.exceptions.PS3NetSrvException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PS3NetSrvTask implements Runnable {
	private final ExecutorService pool;
    private final Thread.UncaughtExceptionHandler exceptionHandler;
    private final String folderPath;
    private final int port;
    private ServerSocket serverSocket;
    private boolean isRunning = true;

    private final EListType listType;

    private final List<String> filterAddresses;

    public PS3NetSrvTask(int port, String folderPath, List<String> filterAddresses, EListType listType, Thread.UncaughtExceptionHandler exceptionHandler) {
        this.folderPath = folderPath;
        this.port = port;
        this.exceptionHandler = exceptionHandler;
        this.pool = Executors.newFixedThreadPool(5);
        this.filterAddresses = filterAddresses;
        this.listType = listType;
    }

    public PS3NetSrvTask(int port, String folderPath, Thread.UncaughtExceptionHandler exceptionHandler) {
        this.folderPath = folderPath;
        this.port = port;
        this.exceptionHandler = exceptionHandler;
        this.pool = Executors.newFixedThreadPool(5);
        this.filterAddresses = null;
        this.listType = EListType.LIST_TYPE_NONE;
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
                pool.execute(new ContextHandler(new Context(clientSocket, folderPath), exceptionHandler));
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
        pool.shutdownNow();
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        } finally {
            serverSocket = null;
        }
    }
}
