package com.jhonju.ps3netsrv.server.io;

import static com.jhonju.ps3netsrv.server.utils.Utils.DKEY_EXT;
import static com.jhonju.ps3netsrv.server.utils.Utils.DOT_STR;
import static com.jhonju.ps3netsrv.server.utils.Utils.ISO_EXTENSION;
import static com.jhonju.ps3netsrv.server.utils.Utils.PS3ISO_FOLDER_NAME;
import static com.jhonju.ps3netsrv.server.utils.Utils.READ_ONLY_MODE;
import static com.jhonju.ps3netsrv.server.utils.Utils.REDKEY_FOLDER_NAME;
import static com.jhonju.ps3netsrv.server.utils.Utils.SECTOR_SIZE;
import static com.jhonju.ps3netsrv.server.utils.Utils._3K3Y_KEY_OFFSET;
import static com.jhonju.ps3netsrv.server.utils.Utils.ENCRYPTION_KEY_SIZE;

import com.jhonju.ps3netsrv.R;
import com.jhonju.ps3netsrv.app.PS3NetSrvApp;
import com.jhonju.ps3netsrv.server.enums.EEncryptionType;
import com.jhonju.ps3netsrv.server.utils.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import javax.crypto.spec.SecretKeySpec;

public class FileCustom implements IFile {

    private final File file;
    private final SecretKeySpec decryptionKey;
    private final EEncryptionType encryptionType;
    private final RandomAccessFile randomAccessFile;

    private final PS3RegionInfo[] regionInfos;
    private final byte[] iv = new byte[16];

    public File getRealFile() { return file; }

    public FileCustom(File file) throws IOException {
        this.file = file;
        byte[] encryptionKey = null;
        EEncryptionType detectedEncryptionType = EEncryptionType.NONE;
        RandomAccessFile randomAccessFile = null;
        PS3RegionInfo[] regionInfos = null;
        
        if (file != null && file.isFile()) {
            randomAccessFile = new RandomAccessFile(file, READ_ONLY_MODE);
            
            // First try to get Redump key from external .dkey file
            encryptionKey = getRedumpKey(file.getParentFile(), file.getAbsolutePath(), file.getName());
            if (encryptionKey != null) {
                detectedEncryptionType = EEncryptionType.REDUMP;
            } else {
                // If no Redump key, try to get embedded 3k3y key from ISO
                encryptionKey = get3K3YKey(randomAccessFile);
                if (encryptionKey != null) {
                    detectedEncryptionType = EEncryptionType._3K3Y;
                }
            }
        }
        
        this.randomAccessFile = randomAccessFile;
        
        if (encryptionKey != null) {
            this.decryptionKey = new SecretKeySpec(encryptionKey, "AES");
            this.encryptionType = detectedEncryptionType;

            int sec0Sec1Length = SECTOR_SIZE * 2;
            if (file.length() >= sec0Sec1Length) {
                byte[] sec0sec1 = new byte[sec0Sec1Length];
                randomAccessFile.seek(0);
                if (randomAccessFile.read(sec0sec1) == sec0Sec1Length) {
                    regionInfos = Utils.getRegionInfos(sec0sec1);
                }
            }
        } else {
            this.decryptionKey = null;
            this.encryptionType = EEncryptionType.NONE;
        }
        this.regionInfos = regionInfos != null ? regionInfos : new PS3RegionInfo[0];
    }

    private static byte[] getRedumpKey(File parent, String path, String fileName) throws IOException {
        byte[] decryptionKey = null;
        if (parent != null && parent.getName().equalsIgnoreCase(PS3ISO_FOLDER_NAME)) {
            int pos = path.lastIndexOf(DOT_STR);
            if (pos >= 0 && path.substring(pos).equalsIgnoreCase(ISO_EXTENSION)) {
                File decryptionKeyFile = new File(path.substring(0, pos) + DKEY_EXT);
                if (!decryptionKeyFile.exists() || decryptionKeyFile.isDirectory()) {
                    File redKeyFolder = new File(parent.getParentFile(), REDKEY_FOLDER_NAME);
                    if (redKeyFolder.exists() && redKeyFolder.isDirectory()) {
                        decryptionKeyFile = new File(redKeyFolder, fileName.substring(0, fileName.lastIndexOf(DOT_STR)) + DKEY_EXT);
                    }
                }
                if (decryptionKeyFile.exists() && decryptionKeyFile.isFile()) {
                    decryptionKey = getKeyFromDocumentFile(decryptionKeyFile);
                }
            }
        }
        return decryptionKey;
    }

    private static byte[] getKeyFromDocumentFile(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        try {
            byte[] buffer = new byte[64];
            int bytesRead = fis.read(buffer);
            if (bytesRead < 0) {
                throw new IOException(PS3NetSrvApp.getAppContext().getString(R.string.error_redump_key_invalid));
            }
            
            if (bytesRead == 16) {
                byte[] key = new byte[16];
                System.arraycopy(buffer, 0, key, 0, 16);
                return key;
            } else if (bytesRead >= 32) {
                String hexStr = new String(buffer, 0, bytesRead, com.jhonju.ps3netsrv.server.charset.StandardCharsets.US_ASCII).trim();
                if (hexStr.length() >= 32) {
                    return hexStringToBytes(hexStr.substring(0, 32));
                } else {
                    throw new IOException(PS3NetSrvApp.getAppContext().getString(R.string.error_redump_key_hex_short, hexStr.length()));
                }
            } else {
                throw new IOException(PS3NetSrvApp.getAppContext().getString(R.string.error_redump_key_size_invalid, bytesRead));
            }
        } finally {
            fis.close();
        }
    }
    
    private static byte[] hexStringToBytes(String hex) {
        byte[] bytes = new byte[16];
        for (int i = 0; i < 16; i++) {
            int index = i * 2;
            bytes[i] = (byte) ((Character.digit(hex.charAt(index), 16) << 4)
                             + Character.digit(hex.charAt(index + 1), 16));
        }
        return bytes;
    }

    private static byte[] get3K3YKey(RandomAccessFile raf) throws IOException {
        if (raf == null || raf.length() < _3K3Y_KEY_OFFSET + ENCRYPTION_KEY_SIZE) {
            return null;
        }
        
        byte[] key = new byte[ENCRYPTION_KEY_SIZE];
        raf.seek(_3K3Y_KEY_OFFSET);
        int bytesRead = raf.read(key);
        
        if (bytesRead != ENCRYPTION_KEY_SIZE || isKeyEmpty(key)) {
            return null;
        }
        
        return key;
    }

    private static boolean isKeyEmpty(byte[] key) {
        for (byte b : key) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }

    public boolean mkdir() {
        return file.mkdir();
    }

    @Override
    public boolean exists() {
        return this.file.exists();
    }

    @Override
    public boolean isFile() {
        return this.file.isFile();
    }

    @Override
    public boolean isDirectory() {
        return file.isDirectory();
    }

    @Override
    public boolean delete() {
        return file.delete();
    }

    @Override
    public long length() {
        return file.length();
    }

    @Override
    public IFile[] listFiles() throws IOException {
        File[] filesAux = file.listFiles();
        IFile[] files = null;
        if (filesAux != null) {
            files = new IFile[filesAux.length];
            int i = 0;
            for (File fileAux : filesAux) {
                files[i] = new FileCustom(fileAux);
                i++;
            }
        }
        return files;
    }

    @Override
    public long lastModified() {
        return file.lastModified();
    }

    @Override
    public String getName() {
        return file.getName();
    }

    @Override
    public String[] list() {
        return file.list();
    }

    @Override
    public IFile findFile(String fileName) throws IOException {
        return new FileCustom(new File(file.getCanonicalPath() + "/" + fileName));
    }

    @Override
    public int read(byte[] buffer, long position) throws IOException {
        randomAccessFile.seek(position);
        int bytesRead = randomAccessFile.read(buffer);

        if (encryptionType == EEncryptionType.NONE) {
            return bytesRead;
        }

        for (PS3RegionInfo regionInfo : regionInfos) {
            if ((position >= regionInfo.getFirstAddress()) && (position <= regionInfo.getLastAddress())) {
                if (!regionInfo.isEncrypted()) {
                    return bytesRead;
                }
                Utils.decryptData(decryptionKey, iv, buffer, bytesRead / SECTOR_SIZE, position / SECTOR_SIZE);
                return bytesRead;
            }
        }
        return bytesRead;
    }

    @Override
    public void close() throws IOException {
        if (randomAccessFile != null) {
            randomAccessFile.close();
        }
    }

    @Override
    public void write(byte[] buffer) throws IOException {
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file)) {
            fos.write(buffer);
        }
    }

    @Override
    public boolean createDirectory(String name) {
        return new File(file, name).mkdir();
    }

    @Override
    public boolean createFile(String name) {
        try {
            return new File(file, name).createNewFile();
        } catch (IOException e) {
            return false;
        }
    }
}
