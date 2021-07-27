package com.jhonju.ps3netsrv.server;

import android.os.AsyncTask;

import com.jhonju.ps3netsrv.server.commands.ICommand;
import com.jhonju.ps3netsrv.server.commands.OpenDirCommand;
import com.jhonju.ps3netsrv.server.commands.OpenFileCommand;
import com.jhonju.ps3netsrv.server.commands.ReadCD2048Command;
import com.jhonju.ps3netsrv.server.commands.ReadDirCommand;
import com.jhonju.ps3netsrv.server.commands.ReadFileCommand;
import com.jhonju.ps3netsrv.server.commands.ReadFileCriticalCommand;
import com.jhonju.ps3netsrv.server.commands.StatFileCommand;
import com.jhonju.ps3netsrv.server.utils.Utils;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class PS3NetSrvTask extends AsyncTask<String, Void, Void> {
    private int port;
    private String folderPath;

    public PS3NetSrvTask(int port, String folderPath) {
        this.port = port;
        this.folderPath = folderPath;
    }

    @Override
    protected Void doInBackground(String... params) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (!isCancelled()) {
                Socket socket = serverSocket.accept();
                new ServerThread(new Context(socket, folderPath)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private class ServerThread extends Thread {
        private static final byte CMD_DATA_SIZE = 16;
        private Context ctx;

        public ServerThread(Context ctx) {
            this.ctx = ctx;
        }

        public void run() {
            try {
                while (ctx.isSocketConnected()) {
                    byte[] pct = new byte[CMD_DATA_SIZE];
                    if (!Utils.readCommandData(ctx.getInputStream(), pct))
                        break;
                    if (Utils.isByteArrayEmpty(pct))
                        continue;
                    ctx.setCommandData(pct);
                    handleContext(ctx);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void handleContext(Context ctx) throws Exception {
            ICommand cmd = null;
            switch (ctx.getCommandData().getOpCode()) {
                case NETISO_CMD_OPEN_DIR:
                    cmd = new OpenDirCommand(ctx);
                    break;
                case NETISO_CMD_READ_DIR:
                    cmd = new ReadDirCommand(ctx);
                    break;
                case NETISO_CMD_STAT_FILE:
                    cmd = new StatFileCommand(ctx);
                    break;
                case NETISO_CMD_OPEN_FILE:
                    cmd = new OpenFileCommand(ctx);
                    break;
                case NETISO_CMD_READ_FILE:
                    cmd = new ReadFileCommand(ctx);
                    break;
                case NETISO_CMD_READ_FILE_CRITICAL:
                    cmd = new ReadFileCriticalCommand(ctx);
                    break;
                case NETISO_CMD_READ_CD_2048_CRITICAL:
                    cmd = new ReadCD2048Command(ctx);
                    break;
                default:
                    throw new Exception("Deu pau");
            }
            if (cmd == null) {
                throw new Exception("Not implemented yet");
            }
            cmd.executeTask();
        }
    }
}
