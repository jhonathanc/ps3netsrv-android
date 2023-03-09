package com.jhonju.ps3netsrv.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PS3NetSrvTask implements Runnable {
	private final ExecutorService pool;
    private final ThreadExceptionHandler exceptionHandler;
    private final String folderPath;
    private final int port;
    private ServerSocket serverSocket;
    private boolean isRunning = true;

    public PS3NetSrvTask(int port, String folderPath, ThreadExceptionHandler exceptionHandler) {
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
        } catch (SocketException e) {
            System.err.println(e.getMessage()); //just let it die
        } catch (Exception e) {
            throw new RuntimeException(e);
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
            throw new RuntimeException("Error on close serversocket", e);
        } finally {
            serverSocket = null;
        }
    }
}
