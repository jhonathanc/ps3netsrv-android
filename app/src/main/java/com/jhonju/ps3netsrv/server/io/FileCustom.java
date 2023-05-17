package com.jhonju.ps3netsrv.server.io;

import static com.jhonju.ps3netsrv.server.utils.Utils.DKEY_EXT;
import static com.jhonju.ps3netsrv.server.utils.Utils.DOT_STR;
import static com.jhonju.ps3netsrv.server.utils.Utils.ISO_EXTENSION;
import static com.jhonju.ps3netsrv.server.utils.Utils.PS3ISO_FOLDER_NAME;
import static com.jhonju.ps3netsrv.server.utils.Utils.READ_ONLY_MODE;
import static com.jhonju.ps3netsrv.server.utils.Utils.REDKEY_FOLDER_NAME;
import static com.jhonju.ps3netsrv.server.utils.Utils.SECTOR_SIZE;

import com.jhonju.ps3netsrv.server.enums.EEncryptionType;
import com.jhonju.ps3netsrv.server.utils.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import javax.crypto.spec.SecretKeySpec;

public class FileCustom implements IFile {

    private final File file;
    private final SecretKeySpec decryptionKey;
    private final EEncryptionType encryptionType;
    private final RandomAccessFile randomAccessFile;

    private final PS3RegionInfo[] regionInfos;
    private final byte[] iv = new byte[16];

    public FileCustom(File file) throws IOException {
        this.file = file;
        String redumpKey = null;
        RandomAccessFile randomAccessFile = null;
        PS3RegionInfo[] regionInfos = null;
        if (file != null && file.isFile()) {
            randomAccessFile = new RandomAccessFile(file, READ_ONLY_MODE);
            redumpKey = getRedumpKey(file.getParentFile(), file.getAbsolutePath(), file.getName());
        }
        this.randomAccessFile = randomAccessFile;
        if (redumpKey != null) {
            this.decryptionKey = new SecretKeySpec(redumpKey.getBytes(), "AES");
            this.encryptionType = EEncryptionType.REDUMP;

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
        this.regionInfos = regionInfos;
    }

    private static String getRedumpKey(File parent, String path, String fileName) throws IOException {
        String decryptionKey = null;
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
                    decryptionKey = getStringFromFile(decryptionKeyFile);
                }
            }
        }
        return decryptionKey;
    }

    private static String getStringFromFile(File file) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        try {
            return reader.readLine().trim();
        } finally {
            reader.close();
        }
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
            }
        }
        return bytesRead;
    }

    @Override
    public void close() throws IOException {
        randomAccessFile.close();
    }
}
