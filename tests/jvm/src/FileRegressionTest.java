import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.commands.*;
import com.jhonju.ps3netsrv.server.io.*;
import androidx.documentfile.provider.DocumentFile;

public class FileRegressionTest extends TestSupport {
  static IFile document(Path path) throws Exception {
    android.content.Context app = new android.content.Context();
    return new DocumentFileCustom(DocumentFile.fromTreeUri(app, android.net.Uri.parse("content:" + path)),
        app.getContentResolver(), app);
  }
  static byte[] critical(IFile file, int length, long offset) throws Exception {
    MemorySocket socket = new MemorySocket("");
    try (Context ctx = context(socket)) {
      ctx.setFile(Collections.singleton(file));
      new ReadFileCriticalCommand(ctx, length, offset).executeTask();
      return socket.output.toByteArray();
    }
  }
  static Map<String,Long> listing(Context ctx, MemorySocket socket) throws Exception {
    new ReadDirCommand(ctx).executeTask();
    byte[] bytes = socket.output.toByteArray(); ByteBuffer buffer=ByteBuffer.wrap(bytes);
    long count=buffer.getLong(); check(bytes.length == 8 + count*529, "directory records are exactly 529 bytes");
    Map<String,Long> entries=new HashMap<>();
    for(int i=0;i<count;i++) {
      long size=buffer.getLong();buffer.getLong();buffer.get();byte[] name=new byte[512];buffer.get(name);
      int end=0;while(end<512&&name[end]!=0)end++;
      check(end<512, "directory name is null terminated");
      entries.put(new String(name,0,end,StandardCharsets.UTF_8),size);
    }
    return entries;
  }
  public static void main(String[] args) throws Exception {
    Path base=Files.createTempDirectory(Paths.get(args[0]),"files-");
    Path first=directory(base,"first"),second=directory(base,"first-backup");
    Files.write(first.resolve("same.iso"),new byte[]{1});Files.write(second.resolve("same.iso"),new byte[]{2,2});
    Files.write(first.resolve("ação.iso"),new byte[]{3});Files.write(second.resolve("游戏.iso"),new byte[]{4});
    char[] longName=new char[180];Arrays.fill(longName,'游');
    IFile oversized=new FileCustom(first.resolve("same.iso").toFile()) {
      public String getName() { return new String(longName); }
    };
    IFile normal=new FileCustom(first.resolve("same.iso").toFile());
    IFile syntheticDir=new FileCustom(first.toFile()) {
      public IFile[] listFiles() { return new IFile[]{oversized,normal}; }
    };
    MemorySocket namesSocket=new MemorySocket("");
    try(Context ctx=context(namesSocket)) {
      ctx.setFile(Collections.singleton(syntheticDir));
      Map<String,Long> entries=listing(ctx,namesSocket);
      check(entries.size()==1&&entries.containsKey("same.iso"),"unrepresentable UTF-8 names are skipped without truncating aliases");
      fails(IOException.class,()->oversized.read(new byte[1],0),"skipped directory child is closed");
      fails(IOException.class,()->normal.read(new byte[1],0),"listed directory child is closed");
    }
    for(int i=0;i<200;i++) {
      try(Context ctx=context(new MemorySocket("/same.iso"),first.toString(),second.toString())) {
        Set<IFile> files=new ProbeCommand(ctx,"/same.iso").resolve();ctx.setFile(files);
        if(files.iterator().next().length()!=1)throw new AssertionError("root priority");
      }
    }
    check(true,"first root wins 200 repeated lookups");
    MemorySocket socket=new MemorySocket("/");
    try(Context ctx=context(socket,first.toString(),second.toString())) {
      ctx.setFile(new ProbeCommand(ctx,"/").resolve());Map<String,Long> entries=listing(ctx,socket);
      check(entries.size()==3&&entries.get("same.iso")==1&&entries.containsKey("ação.iso")&&entries.containsKey("游戏.iso"),
          "merged listing preserves UTF-8 names and first-root metadata");
    }
    FileCustom a=new FileCustom(first.resolve("same.iso").toFile());
    FileCustom b=new FileCustom(second.resolve("same.iso").toFile());
    try(Context ctx=context(new MemorySocket(""))) {
      ctx.setFile(Collections.<IFile>singleton(a));ctx.setFile(Collections.<IFile>singleton(a));
      check(a.read(new byte[1],0)==1,"retained file remains readable");
      ctx.setFile(Collections.<IFile>singleton(b));
      fails(IOException.class,()->a.read(new byte[1],0),"replaced file is closed");
    }
    fails(IOException.class,()->b.read(new byte[1],0),"context closes current file");
    try(Context ctx=context(new MemorySocket("/../second/same.iso"),first.toString())) {
      fails(com.jhonju.ps3netsrv.server.exceptions.PS3NetSrvException.class,
          ()->new ProbeCommand(ctx,"/../second/same.iso").resolve(),"parent traversal rejected");
    }
    Path link=first.resolve("outside");
    try {
      if(!Files.exists(link))Files.createSymbolicLink(link,second.toAbsolutePath());
      try(Context ctx=context(new MemorySocket("/outside/same.iso"),first.toString())) {
        Set<IFile> files=new ProbeCommand(ctx,"/outside/same.iso").resolve();ctx.setFile(files);
        check(files.isEmpty(),"external symlink rejected");
      }
    } catch(FileSystemException | UnsupportedOperationException e) {
      System.out.println("SKIP symlinks: host does not permit creating them (Linux CI exercises this)");
    }
    Path multipart=directory(base,"multipart");
    Files.write(multipart.resolve("disc.iso.0"),new byte[]{1,2,3,4});
    Files.write(multipart.resolve("disc.iso.1"),new byte[]{5,6});
    for(boolean saf:new boolean[]{false,true}) {
      IFile root=saf?document(multipart):new FileCustom(multipart.toFile());
      IFile[] children=root.listFiles();IFile multi=null;
      for(IFile child:children) {
        if(child.getName().endsWith(".0"))multi=child;
        if(!saf)check(field(child,"randomAccessFile")==null,"filesystem listing does not open content handles");
      }
      check(android.os.ParcelFileDescriptor.openCount==0,"SAF listing does not open content handles");
      check(multi!=null&&multi.length()==6,"lazy multipart metadata retains aggregate size (SAF="+saf+")");
      check(Arrays.equals(critical(multi,4,2),new byte[]{3,4,5,6}),"critical multipart boundary (SAF="+saf+")");
      Context.closeFiles(Arrays.asList(children));
      IFile[] again=root.listFiles();check(again[0]!=children[0],"repeated listing returns independently owned wrappers");
      Context.closeFiles(Arrays.asList(again));root.close();
      check(android.os.ParcelFileDescriptor.openCount==0,"multipart descriptors released");
    }
    Path game=directory(base,"game");byte[] payload={20,21,22,23};Files.write(game.resolve("data.bin"),payload);
    for(int sdk:new int[]{14,21}) {
      android.os.Build.VERSION.SDK_INT=sdk;
      for(boolean saf:new boolean[]{false,true}) {
        if(saf&&sdk<21)continue;
        IFile root=saf?document(game):new FileCustom(game.toFile());
        VirtualIsoFile iso=new VirtualIsoFile(root,new android.content.Context());root.close();
        List<?> entries=(List<?>)field(iso,"allFiles");long offset=(Long)field(entries.get(0),"startOffset");
        check(Arrays.equals(critical(iso,4,offset),payload),"virtual ISO payload SDK="+sdk+", SAF="+saf);
        check(android.os.ParcelFileDescriptor.openCount==0,"virtual ISO closes payload handles");
      }
    }
    Path ps3iso=directory(base,"PS3ISO");byte[] key=new byte[16],plain=new byte[2048],iv=new byte[16],disc=new byte[8192];
    for(int i=0;i<key.length;i++)key[i]=(byte)(i+1);for(int i=0;i<plain.length;i++)plain[i]=(byte)(i%127);iv[15]=2;
    ByteBuffer.wrap(disc).putInt(0,2).putInt(12,1).putInt(16,3).putInt(20,3);
    javax.crypto.Cipher cipher=javax.crypto.Cipher.getInstance("AES/CBC/NoPadding");
    cipher.init(javax.crypto.Cipher.ENCRYPT_MODE,new javax.crypto.spec.SecretKeySpec(key,"AES"),new javax.crypto.spec.IvParameterSpec(iv));
    System.arraycopy(cipher.doFinal(plain),0,disc,4096,2048);
    Files.write(ps3iso.resolve("encrypted.iso"),disc);Files.write(ps3iso.resolve("encrypted.dkey"),key);
    for(boolean saf:new boolean[]{false,true}) {
      IFile encrypted=saf?document(ps3iso.resolve("encrypted.iso")):new FileCustom(ps3iso.resolve("encrypted.iso").toFile());
      check(Arrays.equals(critical(encrypted,2048,4096),plain),"Redump decryption remains correct (SAF="+saf+")");
    }
    check(android.os.ParcelFileDescriptor.openCount==0,"all provider descriptors released");
  }
}
