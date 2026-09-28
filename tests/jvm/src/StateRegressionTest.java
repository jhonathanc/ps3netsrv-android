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
  public static void main(String[]args) throws Exception {
    migration();filtering();
    race();admissionControls();
  }
}
