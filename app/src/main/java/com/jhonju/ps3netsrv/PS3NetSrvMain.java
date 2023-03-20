package com.jhonju.ps3netsrv;

import androidx.annotation.NonNull;

import com.jhonju.ps3netsrv.server.PS3NetSrvTask;

public class PS3NetSrvMain {
    private static PS3NetSrvTask server;

    public static void main(String[] args) throws Exception {
        int port = 38008;
        String folderPath;
        switch (args.length) {
            case 2:
                port = Integer.parseInt(args[1]);
            case 1:
                folderPath = args[0];
                break;
            default:
                folderPath = System.getProperty("user.dir");
                break;
        }
        System.out.println("Server is running at " + port);
        System.out.println("Server is running at " + folderPath);
        server = new PS3NetSrvTask(port, folderPath, new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(@NonNull Thread thread, @NonNull Throwable throwable) {
                System.err.println(thread.getId() + " " + throwable.getMessage());
            }
        });

        server.run();
        System.out.println("Server end");
    }
}
