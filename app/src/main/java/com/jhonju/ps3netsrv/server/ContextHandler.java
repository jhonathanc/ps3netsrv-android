package com.jhonju.ps3netsrv.server;

import com.jhonju.ps3netsrv.server.commands.CreateFileCommand;
import com.jhonju.ps3netsrv.server.commands.DeleteFileCommand;
import com.jhonju.ps3netsrv.server.commands.GetDirSizeCommand;
import com.jhonju.ps3netsrv.server.commands.ICommand;
import com.jhonju.ps3netsrv.server.commands.MakeDirCommand;
import com.jhonju.ps3netsrv.server.commands.OpenDirCommand;
import com.jhonju.ps3netsrv.server.commands.OpenFileCommand;
import com.jhonju.ps3netsrv.server.commands.ReadCD2048Command;
import com.jhonju.ps3netsrv.server.commands.ReadDirCommand;
import com.jhonju.ps3netsrv.server.commands.ReadDirEntryCommand;
import com.jhonju.ps3netsrv.server.commands.ReadDirEntryCommandV2;
import com.jhonju.ps3netsrv.server.commands.ReadFileCommand;
import com.jhonju.ps3netsrv.server.commands.ReadFileCriticalCommand;
import com.jhonju.ps3netsrv.server.commands.StatFileCommand;
import com.jhonju.ps3netsrv.server.commands.WriteFileCommand;
import com.jhonju.ps3netsrv.server.enums.ENetIsoCommand;
import com.jhonju.ps3netsrv.server.exceptions.PS3NetSrvException;
import com.jhonju.ps3netsrv.server.utils.Utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;

public class ContextHandler extends Thread {
    private static final byte IDX_OP_CODE = 0;
    private static final byte IDX_CMD_DATA_1 = 2;
    private static final byte IDX_CMD_DATA_2 = 4;
    private static final byte IDX_CMD_DATA_3 = 8;
    private static final byte CMD_DATA_SIZE = 16;
    private final int maxConnections;
    private final Context context;
    private static volatile int simultaneousConnections;

    public synchronized void incrementSimultaneousConnections()
    {
        simultaneousConnections++;
    }

    public synchronized void decrementSimultaneousConnections()
    {
        simultaneousConnections--;
    }

    public ContextHandler(Context context, int maxConnections, Thread.UncaughtExceptionHandler exceptionHandler) {
        super();
        setUncaughtExceptionHandler(exceptionHandler);
        this.context = context;
        this.maxConnections = maxConnections;
    }

    @Override
    public void run() {
        incrementSimultaneousConnections();
        try (Context ctx = context) {
            if (maxConnections > 0 && simultaneousConnections > maxConnections) {
                getUncaughtExceptionHandler().uncaughtException(this, new PS3NetSrvException("Connection limit is reached"));
                return;
            }
            while (ctx.isSocketConnected()) {
                try {
                    ByteBuffer packet = Utils.readCommandData(ctx.getInputStream(), CMD_DATA_SIZE);
                    if (packet == null) break;
                    if (Utils.isByteArrayEmpty(packet.array()))
                        continue;
                    handleContext(ctx, packet);
                } catch (PS3NetSrvException e) {
                    getUncaughtExceptionHandler().uncaughtException(this, e);
                }
            }
        } catch (IOException e) {
            Objects.requireNonNull(getUncaughtExceptionHandler()).uncaughtException(this, e);
        } finally {
            decrementSimultaneousConnections();
        }
    }

    private void handleContext(Context ctx, ByteBuffer buffer) throws PS3NetSrvException, IOException {
        final ICommand command;
        ENetIsoCommand opCode = ENetIsoCommand.valueOf(buffer.getShort(IDX_OP_CODE));
        if (opCode == null) {
            throw new PS3NetSrvException("invalid opCode: " + buffer.getShort(IDX_OP_CODE));
        }
        switch (opCode) {
            case NETISO_CMD_OPEN_DIR:
                command = new OpenDirCommand(ctx, buffer.getShort(IDX_CMD_DATA_1));
                break;
            case NETISO_CMD_READ_DIR:
                command = new ReadDirCommand(ctx);
                break;
            case NETISO_CMD_STAT_FILE:
                command = new StatFileCommand(ctx, buffer.getShort(IDX_CMD_DATA_1));
                break;
            case NETISO_CMD_OPEN_FILE:
                command = new OpenFileCommand(ctx, buffer.getShort(IDX_CMD_DATA_1));
                break;
            case NETISO_CMD_READ_FILE:
                command = new ReadFileCommand(ctx, buffer.getInt(IDX_CMD_DATA_2), buffer.getLong(IDX_CMD_DATA_3));
                break;
            case NETISO_CMD_READ_FILE_CRITICAL:
                command = new ReadFileCriticalCommand(ctx, buffer.getInt(IDX_CMD_DATA_2), buffer.getLong(IDX_CMD_DATA_3));
                break;
            case NETISO_CMD_READ_CD_2048_CRITICAL:
                command = new ReadCD2048Command(ctx, buffer.getInt(IDX_CMD_DATA_2), buffer.getInt(IDX_CMD_DATA_3));
                break;
            case NETISO_CMD_CREATE_FILE:
                command = new CreateFileCommand(ctx, buffer.getShort(IDX_CMD_DATA_1));
                break;
            case NETISO_CMD_WRITE_FILE:
                command = new WriteFileCommand(ctx, buffer.getInt(IDX_CMD_DATA_2));
                break;
            case NETISO_CMD_MKDIR:
                command = new MakeDirCommand(ctx, buffer.getShort(IDX_CMD_DATA_1));
                break;
            case NETISO_CMD_RMDIR:
            case NETISO_CMD_DELETE_FILE:
                command = new DeleteFileCommand(ctx, buffer.getShort(IDX_CMD_DATA_1));
                break;
            case NETISO_CMD_GET_DIR_SIZE:
                command = new GetDirSizeCommand(ctx, buffer.getShort(IDX_CMD_DATA_1));
                break;
            case NETISO_CMD_READ_DIR_ENTRY:
                command = new ReadDirEntryCommand(ctx);
                break;
            case NETISO_CMD_READ_DIR_ENTRY_V2:
                command = new ReadDirEntryCommandV2(ctx);
                break;
            default:
                throw new PS3NetSrvException("OpCode not implemented: " + opCode.name());
        }
        command.executeTask();
    }
}
