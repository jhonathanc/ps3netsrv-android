package com.jhonju.ps3netsrv.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Process-local service state; a recreated view observes the current snapshot. */
public final class ServerState {
  public enum Status { STOPPED, STARTING, RUNNING, STOPPING }

  public final Status status;
  public final int port;
  public final List<String> folders;

  public ServerState(Status status, int port, List<String> folders) {
    this.status = status;
    this.port = port;
    this.folders = Collections.unmodifiableList(new ArrayList<>(folders));
  }

  public boolean isActive() {
    return status == Status.STARTING || status == Status.RUNNING;
  }

  public ServerState withStatus(Status status) {
    return new ServerState(status, port, folders);
  }
}
