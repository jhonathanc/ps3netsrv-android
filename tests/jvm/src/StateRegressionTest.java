import java.util.*;
import java.util.concurrent.*;
import java.lang.reflect.*;
import java.net.*;
import com.jhonju.ps3netsrv.app.*;
import com.jhonju.ps3netsrv.server.*;
import com.jhonju.ps3netsrv.server.enums.*;

public class StateRegressionTest {
  static void resetCache() throws Exception {
    Field f=SettingsService.class.getDeclaredField("cachedFolders");f.setAccessible(true);f.set(null,null);
  }
  static void expect(String label,Object actual,Object wanted) {
    if(!Objects.equals(actual,wanted))throw new AssertionError(label+": "+actual+" != "+wanted);
    System.out.println("PASS "+label+": "+actual);
  }
  static void migration() throws Exception {
    android.content.Context app=PS3NetSrvApp.getAppContext();
    app.clear();resetCache();
    app.getSharedPreferences("FOLDER",0).edit().putString("settings","/legacy").apply();
    expect("first migration",SettingsService.getFolders(),Arrays.asList("/legacy"));
    SettingsService.setFolders(Arrays.asList("/new-first","/new-second"));
    expect("saved in current process",SettingsService.getFolders(),Arrays.asList("/new-first","/new-second"));
    resetCache();
    expect("JSON retained after legacy migration",SettingsService.getFolders(),Arrays.asList("/new-first","/new-second"));
    app.clear();resetCache();
    SettingsService.setFolders(Arrays.asList("/new-first","/new-second"));resetCache();
    expect("control new install retains JSON order",SettingsService.getFolders(),Arrays.asList("/new-first","/new-second"));
    app.clear();resetCache();
    app.getSharedPreferences("FOLDERS",0).edit().putStringSet("settings",new LinkedHashSet<>(Arrays.asList("/set-folder"))).apply();
    expect("control old set migration",SettingsService.getFolders(),Arrays.asList("/set-folder"));
    SettingsService.setFolders(Arrays.asList("/new-first","/new-second"));resetCache();
    expect("control old set retains updated JSON",SettingsService.getFolders(),Arrays.asList("/new-first","/new-second"));
    app.clear();resetCache();
    SettingsService.setFolders(Collections.emptyList());resetCache();
    expect("control intentionally empty JSON retained",SettingsService.getFolders(),Collections.emptyList());
  }
  static void race() throws Exception {
    Field serverField=PS3NetSrvTask.class.getDeclaredField("serverSocket");serverField.setAccessible(true);
    ExecutorService clients=Executors.newFixedThreadPool(32);
    int worst=0,round=0;
    try {
      for(round=1;round<=100;round++) {
        PS3NetSrvTask task=new PS3NetSrvTask(0,Collections.emptyList(),1,Collections.emptySet(),EListType.LIST_TYPE_NONE,
          (t,e)->{},new android.content.ContentResolver(),new android.content.Context());
        Thread server=new Thread(task);server.start();
        ServerSocket listener=null;
        for(int i=0;i<500;i++) {listener=(ServerSocket)serverField.get(task);if(listener!=null&&listener.isBound())break;Thread.sleep(2);}
        if(listener==null||!listener.isBound())throw new AssertionError("server did not bind");
        final int port=listener.getLocalPort();
        CountDownLatch ready=new CountDownLatch(32),go=new CountDownLatch(1);
        List<Future<Socket>> futures=new ArrayList<>();
        List<Socket> sockets=new ArrayList<>();
        try {
          for(int i=0;i<32;i++)futures.add(clients.submit(()->{ready.countDown();go.await();return new Socket("127.0.0.1",port);}));
          ready.await();go.countDown();
          for(Future<Socket> f:futures)sockets.add(f.get(10,TimeUnit.SECONDS));
          Thread.sleep(100);
          worst=Math.max(worst,ContextHandler.getSimultaneousConnections());
        } finally {
          for(Socket s:sockets)s.close();task.shutdown();server.join(3000);
          for(int i=0;i<500&&ContextHandler.getSimultaneousConnections()!=0;i++)Thread.sleep(2);
          if(ContextHandler.getSimultaneousConnections()!=0)throw new AssertionError("handler cleanup failed");
        }
        if(worst>1)break;
      }
    } finally {clients.shutdownNow();}
    if(worst!=1)throw new AssertionError("Admission failed: " + worst);
    System.out.println("PASS connection limit=1, observed maximum="+worst+", rounds="+(round-1));
  }

  static void filtering() throws Exception {
    Method method=PS3NetSrvTask.class.getDeclaredMethod("allowIncomingConnection",String.class);method.setAccessible(true);
    for(EListType type:EListType.values()) {
      PS3NetSrvTask task=new PS3NetSrvTask(0,Collections.emptyList(),1,new HashSet<>(Arrays.asList("192.168.1.10")),type,
        (t,e)->{},new android.content.ContentResolver(),new android.content.Context());
      expect("filter "+type+" listed",method.invoke(task,"192.168.1.10"),type!=EListType.LIST_TYPE_BLOCKED);
      expect("filter "+type+" unlisted",method.invoke(task,"192.168.1.11"),type!=EListType.LIST_TYPE_ALLOWED);
    }
  }
  static void admissionControls() throws Exception {
    Field serverField=PS3NetSrvTask.class.getDeclaredField("serverSocket");serverField.setAccessible(true);
    for(int limit:new int[]{0,3}) {
      PS3NetSrvTask task=new PS3NetSrvTask(0,Collections.emptyList(),limit,Collections.emptySet(),EListType.LIST_TYPE_NONE,
        (t,e)->{},new android.content.ContentResolver(),new android.content.Context());
      Thread server=new Thread(task);server.start();ServerSocket listener=null;
      for(int i=0;i<500;i++){listener=(ServerSocket)serverField.get(task);if(listener!=null&&listener.isBound())break;Thread.sleep(2);}
      if(listener==null||!listener.isBound())throw new AssertionError("server did not bind");
      try {
        for(int batch=0;batch<2;batch++) {
          List<Socket>sockets=new ArrayList<>();
          try {
            for(int i=0;i<8;i++)sockets.add(new Socket("127.0.0.1",listener.getLocalPort()));
            Thread.sleep(100);
            expect("capacity "+limit+", batch="+batch,ContextHandler.getSimultaneousConnections(),limit==0?8:3);
          } finally {
            for(Socket socket:sockets)socket.close();
            for(int i=0;i<500&&ContextHandler.getSimultaneousConnections()!=0;i++)Thread.sleep(2);
            expect("capacity returned after client disconnect",ContextHandler.getSimultaneousConnections(),0);
          }
        }
      } finally {task.shutdown();server.join(3000);}
    }
  }

  static PS3NetSrvTask newTask(int port, Thread.UncaughtExceptionHandler errors) {
    return new PS3NetSrvTask(port, Collections.emptyList(), 0, Collections.emptySet(),
        EListType.LIST_TYPE_NONE, errors, new android.content.ContentResolver(), new android.content.Context());
  }

  static void serverLifecycle() throws Exception {
    java.util.concurrent.atomic.AtomicInteger started = new java.util.concurrent.atomic.AtomicInteger();
    java.util.concurrent.atomic.AtomicInteger errors = new java.util.concurrent.atomic.AtomicInteger();
    PS3NetSrvTask cancelled = newTask(0, (t, e) -> errors.incrementAndGet());
    cancelled.setOnStarted(started::incrementAndGet);
    cancelled.shutdown();
    Thread cancelledThread = new Thread(cancelled);
    cancelledThread.start(); cancelledThread.join(3000);
    expect("stop before executor starts never binds", started.get(), 0);
    expect("cancelled task exits", cancelledThread.isAlive(), false);

    try (ServerSocket occupied = new ServerSocket(0)) {
      PS3NetSrvTask failed = newTask(occupied.getLocalPort(), (t, e) -> errors.incrementAndGet());
      failed.setOnStarted(started::incrementAndGet);
      Thread failedThread = new Thread(failed);
      failedThread.start(); failedThread.join(3000);
      expect("bind failure never reports started", started.get(), 0);
      expect("bind failure exits", failedThread.isAlive(), false);
      expect("bind failure clears running flag", failed.isRunning(), false);
      expect("bind failure reported once", errors.get(), 1);
    }

    Field serverField = PS3NetSrvTask.class.getDeclaredField("serverSocket");
    serverField.setAccessible(true);
    PS3NetSrvTask live = newTask(0, (t, e) -> errors.incrementAndGet());
    CountDownLatch listening = new CountDownLatch(1);
    live.setOnStarted(listening::countDown);
    Thread server = new Thread(live);
    server.start();
    try {
      if (!listening.await(3, TimeUnit.SECONDS)) throw new AssertionError("listener did not start");
      int port = ((ServerSocket) serverField.get(live)).getLocalPort();
      try (Socket client = new Socket("127.0.0.1", port)) {
        for (int i = 0; i < 500 && ContextHandler.getSimultaneousConnections() == 0; i++) Thread.sleep(2);
        expect("live server owns client", ContextHandler.getSimultaneousConnections(), 1);
        live.shutdown(); server.join(3000);
        client.setSoTimeout(3000);
        expect("stop closes accepted client socket", client.getInputStream().read(), -1);
        for (int i = 0; i < 500 && ContextHandler.getSimultaneousConnections() != 0; i++) Thread.sleep(2);
        expect("stop releases client slot without client disconnect", ContextHandler.getSimultaneousConnections(), 0);
        expect("stop unblocks listener", server.isAlive(), false);
        expect("intentional stop reports no errors", errors.get(), 1);
        try (ServerSocket replacement = new ServerSocket()) {
          replacement.setReuseAddress(true);
          replacement.bind(new InetSocketAddress(port));
          expect("stopped port can restart immediately", replacement.isBound(), true);
        }
      }
    } finally { live.shutdown(); server.join(3000); }

    for (int i = 0; i < 100; i++) {
      PS3NetSrvTask racing = newTask(0, (t, e) -> errors.incrementAndGet());
      Thread worker = new Thread(racing);
      worker.start(); racing.shutdown(); worker.join(3000);
      if (worker.isAlive() || racing.isRunning() || serverField.get(racing) != null) {
        throw new AssertionError("start/stop race leaked listener at round " + i);
      }
    }
    expect("100 immediate start/stop races report no errors", errors.get(), 1);
  }

  public static void main(String[]args) throws Exception {
    migration();filtering();
    race();admissionControls();serverLifecycle();
  }
}
