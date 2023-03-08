package com.jhonju.ps3netsrv.server;

public class ThreadExceptionHandler implements Thread.UncaughtExceptionHandler {
    @Override
    public void uncaughtException(Thread t, Throwable e) {
        System.err.println(e.getMessage());
    }
}