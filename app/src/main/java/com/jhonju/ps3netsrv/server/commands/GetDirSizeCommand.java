package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.exceptions.PS3NetSrvException;
import com.jhonju.ps3netsrv.server.io.IFile;
import com.jhonju.ps3netsrv.server.utils.BinaryUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

public class GetDirSizeCommand extends FileCommand {

  public GetDirSizeCommand(Context ctx, short filePathLength) {
    super(ctx, filePathLength);
    ERROR_CODE_BYTEARRAY = BinaryUtils.longToBytesBE(ERROR_CODE);
  }

  @Override
  public void executeTask() throws IOException, PS3NetSrvException {
    Set<IFile> files = getFile();
    try {
      long size = 0;
      for (IFile file : files) {
        size += calculateFileSize(file);
      }
      send(BinaryUtils.longToBytesBE(size));
    } finally {
      Context.closeFiles(files);
    }
  }

  private static long calculateFileSize(IFile file) throws IOException {
    if (!file.isDirectory()) return file.length();
    IFile[] children = file.listFiles();
    if (children == null) return 0;
    try {
      long size = 0;
      for (IFile child : children) {
        size += calculateFileSize(child);
      }
      return size;
    } finally {
      Context.closeFiles(Arrays.asList(children));
    }
  }
}
