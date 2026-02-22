package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.R;
import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.exceptions.PS3NetSrvException;
import com.jhonju.ps3netsrv.server.utils.BinaryUtils;

import java.io.IOException;
import java.io.OutputStream;

public abstract class AbstractCommand implements ICommand {
  protected final Context ctx;
  protected static final int ERROR_CODE = -1;
  protected byte[] ERROR_CODE_BYTEARRAY = BinaryUtils.intToBytesBE(ERROR_CODE);
  protected final byte[] SUCCESS_CODE_BYTEARRAY = BinaryUtils.intToBytesBE(0);
  protected static final int EMPTY_SIZE = 0;
  protected static final int BYTES_TO_SKIP = 24;
  protected static final short MILLISECONDS_IN_SECOND = 1000;

  public AbstractCommand(Context ctx) {
    this.ctx = ctx;
  }

  protected void send(IResult result) throws IOException, PS3NetSrvException {
    byte[] byteArray;
    try {
      byteArray = result.toByteArray();
    } catch (IOException e) {
      throw new PS3NetSrvException(ctx.getAndroidContext().getString(R.string.error_byte_array_conversion));
    }
    /*
     * the "send" is out from try-catch because toByteArray also throws IOException,
     * so
     * it's possible to handle it. The IOException on "send" must end the
     * thread and close the connection on ContextHandler
     */
    send(byteArray);
  }

  protected void send(byte[] result) throws IOException, PS3NetSrvException {
    OutputStream os = ctx.getOutputStream();
    if (result.length == EMPTY_SIZE) {
      throw new PS3NetSrvException(ctx.getAndroidContext().getString(R.string.error_empty_byte_array));
    }
    os.write(result);
    os.flush();
  }

}
