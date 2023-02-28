package com.jhonju.ps3netsrv.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import com.jhonju.ps3netsrv.server.commands.ICommand;
import com.jhonju.ps3netsrv.server.commands.OpenDirCommand;
import com.jhonju.ps3netsrv.server.commands.OpenFileCommand;
import com.jhonju.ps3netsrv.server.commands.ReadCD2048Command;
import com.jhonju.ps3netsrv.server.commands.ReadDirCommand;
import com.jhonju.ps3netsrv.server.commands.ReadFileCommand;
import com.jhonju.ps3netsrv.server.commands.ReadFileCriticalCommand;
import com.jhonju.ps3netsrv.server.commands.StatFileCommand;
import com.jhonju.ps3netsrv.server.enums.ENetIsoCommand;
import com.jhonju.ps3netsrv.server.utils.Utils;

public class PS3NetSrvTask implements Runnable {
    private final String folderPath;
    ServerSocket serverSocket;

    volatile boolean running = true;

    public PS3NetSrvTask(int port, String folderPath) throws Exception {
        this.folderPath = folderPath;
        serverSocket = new ServerSocket(port);
    }

    @Override
    public void run() {
        try {
            while (running) {
                Socket socket = serverSocket.accept();
                new ServerThread(new Context(socket, folderPath)).start();
            }
            System.out.println("Thread end");
        } catch (IOException e) {
            //just let it die alone - the serverSocket.close() throws this exception
        }
    }

    public void shutdown() throws IOException {
        serverSocket.close();
        running = false;
    }

    private static class ServerThread extends Thread {
        private static final byte CMD_DATA_SIZE = 16;
        private final Context context;

        public ServerThread(Context context) {
            this.context = context;
        }

        public void run() {
            try {
                while (context.isSocketConnected()) {
                    byte[] packet = new byte[CMD_DATA_SIZE];
                    if (!Utils.readCommandData(context.getInputStream(), packet))
                        break;
                    if (Utils.isByteArrayEmpty(packet))
                        continue;
                    context.setCommandData(packet);
                    handleContext(context);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void handleContext(Context ctx) throws Exception {
            ICommand command;
            ENetIsoCommand opCode = ctx.getCommandData().getOpCode();
            switch (opCode) {
                case NETISO_CMD_OPEN_DIR:
                    command = new OpenDirCommand(ctx);
                    break;
                case NETISO_CMD_READ_DIR:
                    command = new ReadDirCommand(ctx);
                    break;
                case NETISO_CMD_STAT_FILE:
                    command = new StatFileCommand(ctx);
                    break;
                case NETISO_CMD_OPEN_FILE:
                    command = new OpenFileCommand(ctx);
                    break;
                case NETISO_CMD_READ_FILE:
                    command = new ReadFileCommand(ctx);
                    break;
                case NETISO_CMD_READ_FILE_CRITICAL:
                    command = new ReadFileCriticalCommand(ctx);
                    break;
                case NETISO_CMD_READ_CD_2048_CRITICAL:
                    command = new ReadCD2048Command(ctx);
                    break;
                default:
                    throw new Exception("OpCode not implemented!");
            }
            command.executeTask();
        }
    }
}
