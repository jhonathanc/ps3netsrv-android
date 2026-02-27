package com.jhonju.ps3netsrv.server;

import android.content.ContentResolver;

import com.jhonju.ps3netsrv.R;
import com.jhonju.ps3netsrv.server.commands.GetDirSizeCommand;
import com.jhonju.ps3netsrv.server.commands.ICommand;
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
import com.jhonju.ps3netsrv.server.commands.CreateFileCommand;
import com.jhonju.ps3netsrv.server.commands.MakeDirCommand;
import com.jhonju.ps3netsrv.server.commands.DeleteFileCommand;
import com.jhonju.ps3netsrv.server.enums.ENetIsoCommand;
import com.jhonju.ps3netsrv.server.exceptions.PS3NetSrvException;
import com.jhonju.ps3netsrv.server.utils.BinaryUtils;
import com.jhonju.ps3netsrv.server.utils.FileLogger;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class ContextHandler extends Thread {
  private static final byte IDX_OP_CODE = 0;
  private static final byte IDX_CMD_DATA_1 = 2;
  private static final byte IDX_CMD_DATA_2 = 4;
  private static final byte IDX_CMD_DATA_3 = 8;
  private static final byte CMD_DATA_SIZE = 16;
  private final Socket socket;
  private final List<String> folderPaths;
  private final ContentResolver contentResolver;
  private final android.content.Context androidContext;
  private static final AtomicInteger simultaneousConnections = new AtomicInteger(0);

  public synchronized void incrementSimultaneousConnections() {
    simultaneousConnections.incrementAndGet();
  }

  public synchronized void decrementSimultaneousConnections() {
    simultaneousConnections.decrementAndGet();
  }

  public static int getSimultaneousConnections() {
    return simultaneousConnections.get();
  }

  public ContextHandler(Socket socket, List<String> folderPaths,
      ContentResolver contentResolver, Thread.UncaughtExceptionHandler exceptionHandler,
      android.content.Context androidContext) {
    super();
    setUncaughtExceptionHandler(exceptionHandler);
    this.socket = socket;
    this.folderPaths = folderPaths;
    this.contentResolver = contentResolver;
    this.androidContext = androidContext;
  }

  @Override
  public void run() {
    incrementSimultaneousConnections();
    com.jhonju.ps3netsrv.server.Context ctx = new com.jhonju.ps3netsrv.server.Context(socket, folderPaths,
        contentResolver, androidContext);
    try {
      while (ctx.isSocketConnected()) {
        try {
          ByteBuffer packet = BinaryUtils.readCommandData(ctx.getInputStream(), CMD_DATA_SIZE);
          if (packet == null)
            break;
          if (BinaryUtils.isByteArrayEmpty(packet.array()))
            continue;
          handleContext(ctx, packet);
        } catch (PS3NetSrvException e) {
          getUncaughtExceptionHandler().uncaughtException(this, e);
        }
      }
    } catch (IOException e) {
      Objects.requireNonNull(getUncaughtExceptionHandler()).uncaughtException(this, e);
    } finally {
      ctx.close();
      decrementSimultaneousConnections();
    }
  }

  private void handleContext(com.jhonju.ps3netsrv.server.Context ctx, ByteBuffer buffer)
      throws PS3NetSrvException, IOException {
    final ICommand command;
    ENetIsoCommand opCode = ENetIsoCommand.valueOf(buffer.getShort(IDX_OP_CODE));

    if (opCode == null) {
      throw new PS3NetSrvException(
          ctx.getAndroidContext().getString(R.string.error_invalid_opcode, buffer.getShort(IDX_OP_CODE)));
    }
    FileLogger.logCommand(opCode.name(), buffer.array());
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
        command = new WriteFileCommand(ctx, buffer.getShort(IDX_CMD_DATA_1), buffer.getInt(IDX_CMD_DATA_2));
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
        throw new PS3NetSrvException(
            ctx.getAndroidContext().getString(R.string.error_opcode_not_implemented, opCode.name()));
    }
    command.executeTask();
  }
}
