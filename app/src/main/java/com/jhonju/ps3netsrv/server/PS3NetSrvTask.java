package com.jhonju.ps3netsrv.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
	private static ExecutorService pool;
    private String folderPath;
    private ServerSocket serverSocket;
    private volatile boolean isRunning = true;

    public PS3NetSrvTask(int port, String folderPath) throws Exception {
        this.folderPath = folderPath;
        serverSocket = new ServerSocket(port);
        pool = Executors.newFixedThreadPool(5);
    }

    public void run() {
        try {
            while (isRunning) {
                try (Socket socket = serverSocket.accept();
                     Context context = new Context(socket, folderPath)) {
                    pool.execute(new Handler(context));
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage()); //just let it die
        } finally {
        	pool.shutdown();
        }
    }

    public void shutdown() throws IOException {
        isRunning = false;
        pool.shutdownNow();
        serverSocket.close();
    }

    private static class Handler implements Runnable {
        private static final byte CMD_DATA_SIZE = 16;
        private final Context context;

        public Handler(Context context) {
            this.context = context;
        }

        @Override
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
                new RuntimeException(e);
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
                    throw new Exception("OpCode not implemented: " + opCode.toString());
            }
            command.executeTask();
        }
    }
}
