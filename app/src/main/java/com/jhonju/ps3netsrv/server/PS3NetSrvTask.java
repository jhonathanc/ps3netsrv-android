package com.jhonju.ps3netsrv.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PS3NetSrvTask implements Runnable {
	private final ExecutorService pool;
    private final Thread.UncaughtExceptionHandler exceptionHandler;
    private final String folderPath;
    private final int port;
    private ServerSocket serverSocket;
    private boolean isRunning = true;

    public PS3NetSrvTask(int port, String folderPath, Thread.UncaughtExceptionHandler exceptionHandler) {
        this.folderPath = folderPath;
        this.port = port;
        this.exceptionHandler = exceptionHandler;
        this.pool = Executors.newFixedThreadPool(5);
    }

    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            while (isRunning) {
                pool.execute(new ContextHandler(new Context(serverSocket.accept(), folderPath), exceptionHandler));
            }
        } catch (IOException e) {
            exceptionHandler.uncaughtException(null, e);
        } finally {
            shutdown();
        }
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
