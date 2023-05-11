package com.jhonju.ps3netsrv.server.io;

import static com.jhonju.ps3netsrv.server.utils.Utils.DKEY_EXT;
import static com.jhonju.ps3netsrv.server.utils.Utils.DOT_STR;
import static com.jhonju.ps3netsrv.server.utils.Utils.ISO_EXTENSION;
import static com.jhonju.ps3netsrv.server.utils.Utils.PS3ISO_FOLDER_NAME;
import static com.jhonju.ps3netsrv.server.utils.Utils.REDKEY_FOLDER_NAME;

import com.jhonju.ps3netsrv.server.enums.EEncryptionType;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;

public class FileCustom implements IFile {

    private final File file;
    private final String decryptionKey;
    private final EEncryptionType encryptionType;
    private final RandomAccessFile randomAccessFile;

    public FileCustom(File file) throws IOException {
        this.file = file;
        String decryptionKey = null;
        RandomAccessFile randomAccessFile = null;
        EEncryptionType encryptionType = EEncryptionType.NONE;
        if (file != null && file.isFile()) {
            randomAccessFile = new RandomAccessFile(file, "r");
            File parent = file.getParentFile();
            if (parent != null && parent.getName().equalsIgnoreCase(PS3ISO_FOLDER_NAME)) {
                String path = file.getAbsolutePath();
                int pos = path.lastIndexOf(DOT_STR);
                if (pos >= 0 && path.substring(pos).equalsIgnoreCase(ISO_EXTENSION)) {
                    File decryptionKeyFile = new File(path.substring(0, pos) + DKEY_EXT);
                    if (!decryptionKeyFile.exists() || decryptionKeyFile.isDirectory()) {
                        File redKeyFolder = new File(parent.getParentFile(), REDKEY_FOLDER_NAME);
                        if (redKeyFolder.exists() && redKeyFolder.isDirectory()) {
                            String fileName = file.getName();
                            decryptionKeyFile = new File(redKeyFolder, fileName.substring(0, fileName.lastIndexOf(DOT_STR)) + DKEY_EXT);
                        }
                    }
                    if (decryptionKeyFile.exists() && decryptionKeyFile.isFile()) {
                        decryptionKey = getStringFromFile(decryptionKeyFile);
                        encryptionType = EEncryptionType.REDUMP;
                    }
                }
            }
        }
        this.randomAccessFile = randomAccessFile;
        this.decryptionKey = decryptionKey;
        this.encryptionType = encryptionType;
    }

    private static String getStringFromFile(File file) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            try {
                return reader.readLine().trim();
            } finally {
                reader.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public File getFile() {
        return file;
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
    public String getDecryptionKey() {
        return decryptionKey;
    }

    @Override
    public int read(byte[] buffer, long position) throws IOException {
        randomAccessFile.seek(position);
        return randomAccessFile.read(buffer);
    }

    @Override
    public void close() throws IOException {
        randomAccessFile.close();
    }
}
