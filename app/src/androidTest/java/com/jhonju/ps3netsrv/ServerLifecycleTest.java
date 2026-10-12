package com.jhonju.ps3netsrv;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.filters.SdkSuppress;

import com.jhonju.ps3netsrv.app.MainActivity;
import com.jhonju.ps3netsrv.app.PS3NetService;
import com.jhonju.ps3netsrv.app.ServerState;
import com.jhonju.ps3netsrv.app.SettingsService;
import com.jhonju.ps3netsrv.server.enums.ENetIsoCommand;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.DataInputStream;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/** Device tests: recreation, background execution, duplicate starts, failures and restart. */
@RunWith(AndroidJUnit4.class)
public class ServerLifecycleTest {
  private Context context;
  private ActivityScenario<MainActivity> activity;
  private int previousPort, previousListType, previousMaxConnections;
  private int port;
  private List<String> previousFolders;

  private void onMain(Runnable action) {
    InstrumentationRegistry.getInstrumentation().runOnMainSync(action);
  }

  private void awaitState(ServerState.Status status) throws Exception {
    long deadline = System.currentTimeMillis() + 5000;
    while (System.currentTimeMillis() < deadline) {
      if (PS3NetService.getState().getValue().status == status) return;
      Thread.sleep(20);
    }
    assertEquals(status, PS3NetService.getState().getValue().status);
  }

  @Before
  public void prepare() throws Exception {
    context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    onMain(() -> PS3NetService.stop(context));
    awaitState(ServerState.Status.STOPPED);
    previousPort = SettingsService.getPort();
    previousListType = SettingsService.getListType();
    previousMaxConnections = SettingsService.getMaxConnections();
    previousFolders = SettingsService.getFolders();
    try (ServerSocket available = new ServerSocket(0)) { port = available.getLocalPort(); }
    SettingsService.setPort(port);
    SettingsService.setListType(R.id.rbNone);
    SettingsService.setMaxConnections(0);
    SettingsService.setFolders(Collections.singletonList(context.getCacheDir().getAbsolutePath()));
    activity = ActivityScenario.launch(MainActivity.class);
  }

  @After
  public void cleanup() throws Exception {
    onMain(() -> PS3NetService.stop(context));
    awaitState(ServerState.Status.STOPPED);
    SettingsService.setPort(previousPort);
    SettingsService.setListType(previousListType);
    SettingsService.setMaxConnections(previousMaxConnections);
    SettingsService.setFolders(previousFolders);
    if (activity != null) activity.close();
  }

  private void start() throws Exception {
    onMain(() -> PS3NetService.start(context));
    awaitState(ServerState.Status.RUNNING);
  }

  private void assertRunningScreen() {
    activity.onActivity(screen -> {
      Button button = screen.findViewById(R.id.button_start_stop_server);
      TextView label = screen.findViewById(R.id.tvServerStopped);
      assertEquals(screen.getString(R.string.stop_server), button.getText().toString());
      assertTrue(button.isEnabled());
      assertTrue(label.getText().toString().contains(Integer.toString(port)));
    });
  }

  private void assertConnectionResponds(Socket client) throws Exception {
    byte[] path = "/missing-lifecycle-test".getBytes("UTF-8");
    byte[] command = ByteBuffer.allocate(16)
        .putShort((short) ENetIsoCommand.NETISO_CMD_STAT_FILE.value).putShort((short) path.length).array();
    client.getOutputStream().write(command);
    client.getOutputStream().write(path);
    client.getOutputStream().flush();
    new DataInputStream(client.getInputStream()).readFully(new byte[33]);
  }

  @Test
  public void recreationAndBackgroundPreserveServerAndExistingConnection() throws Exception {
    start();
    try (Socket client = new Socket("127.0.0.1", port)) {
      client.setSoTimeout(3000);
      assertConnectionResponds(client);
      activity.recreate();
      assertRunningScreen();
      assertConnectionResponds(client);
      activity.moveToState(Lifecycle.State.CREATED);
      assertConnectionResponds(client);
      activity.moveToState(Lifecycle.State.RESUMED);
      assertRunningScreen();
      assertConnectionResponds(client);
    }
  }

  @Test
  public void repeatedStartKeepsListenerAndRestartClosesOldConnection() throws Exception {
    start();
    try (Socket client = new Socket("127.0.0.1", port)) {
      client.setSoTimeout(3000);
      assertConnectionResponds(client);
      onMain(() -> {
        ContextCompat.startForegroundService(context, new Intent(context, PS3NetService.class));
        ContextCompat.startForegroundService(context, new Intent(context, PS3NetService.class));
      });
      InstrumentationRegistry.getInstrumentation().waitForIdleSync();
      assertConnectionResponds(client);
      onMain(() -> PS3NetService.restart(context));
      // The old accepted socket must be closed even if the client stays idle.
      assertEquals(-1, client.getInputStream().read());
      awaitState(ServerState.Status.RUNNING);
      try (Socket replacement = new Socket("127.0.0.1", port)) {
        replacement.setSoTimeout(3000);
        assertConnectionResponds(replacement);
      }
      assertRunningScreen();
    }
  }

  @Test
  public void occupiedPortReturnsToStoppedAndCanStartAgain() throws Exception {
    try (ServerSocket occupied = new ServerSocket(port)) {
      onMain(() -> PS3NetService.start(context));
      awaitState(ServerState.Status.STOPPED);
      activity.onActivity(screen -> {
        Button button = screen.findViewById(R.id.button_start_stop_server);
        TextView label = screen.findViewById(R.id.tvServerStopped);
        assertEquals(screen.getString(R.string.start_server), button.getText().toString());
        assertEquals(screen.getString(R.string.server_stopped), label.getText().toString());
        assertTrue(button.isEnabled());
      });
    }
    start();
    assertRunningScreen();
  }

  @RequiresApi(Build.VERSION_CODES.M)
  @SdkSuppress(minSdkVersion = 23)
  @Test
  public void exemptServerKeepsExistingAndNewConnectionsDuringDoze() throws Exception {
    PowerManager power = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
    boolean alreadyExempt = power.isIgnoringBatteryOptimizations(context.getPackageName());
    try {
      if (!alreadyExempt) shell("dumpsys deviceidle whitelist +" + context.getPackageName());
      assertTrue(power.isIgnoringBatteryOptimizations(context.getPackageName()));
      start();
      try (Socket existing = new Socket("127.0.0.1", port)) {
        existing.setSoTimeout(3000);
        assertConnectionResponds(existing);
        activity.moveToState(Lifecycle.State.CREATED);
        shell("dumpsys battery unplug");
        shell("dumpsys deviceidle force-idle");
        // PowerManager observes the transition asynchronously after the shell returns.
        long idleDeadline = System.currentTimeMillis() + 5000;
        while (!power.isDeviceIdleMode() && System.currentTimeMillis() < idleDeadline) {
          Thread.sleep(25);
        }
        assertTrue("Device must enter Doze for this test", power.isDeviceIdleMode());
        for (int i = 0; i < 12; i++) {
          assertConnectionResponds(existing);
          try (Socket incoming = new Socket("127.0.0.1", port)) {
            incoming.setSoTimeout(3000);
            assertConnectionResponds(incoming);
          }
          assertEquals(ServerState.Status.RUNNING, PS3NetService.getState().getValue().status);
          Thread.sleep(250);
        }
      }
    } finally {
      try {
        shell("dumpsys deviceidle unforce");
      } finally {
        try {
          shell("dumpsys battery reset");
        } finally {
          if (!alreadyExempt) shell("dumpsys deviceidle whitelist -" + context.getPackageName());
        }
      }
    }
  }

  @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
  private void shell(String command) throws Exception {
    ParcelFileDescriptor descriptor = InstrumentationRegistry.getInstrumentation()
        .getUiAutomation().executeShellCommand(command);
    try (InputStream output = new ParcelFileDescriptor.AutoCloseInputStream(descriptor)) {
      byte[] bytes = new byte[1024];
      while (output.read(bytes) != -1) { /* Wait for the shell operation to complete. */ }
    }
  }
}
