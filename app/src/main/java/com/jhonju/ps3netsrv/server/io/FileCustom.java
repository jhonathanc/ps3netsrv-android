package com.jhonju.ps3netsrv.server.io;

import static com.jhonju.ps3netsrv.server.utils.Utils.DKEY_EXT;
import static com.jhonju.ps3netsrv.server.utils.Utils.DOT_STR;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class FileCustom implements IFile {

    private final File file;
    private final String decryptionKey;

    public FileCustom(File file) {
        this.file = file;
        String decryptionKeyAux = null;
        if (file != null && file.isFile()) {
            String path = file.getAbsolutePath();
            int pos = path.lastIndexOf(DOT_STR);
            if (pos >= 0) {
                File decriptionKeyFile = new File(path.substring(0, pos) + DKEY_EXT);
                if (decriptionKeyFile.exists()) {
                    if (decriptionKeyFile != null) {
                        try {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(decriptionKeyFile)));
                            try {
                                decryptionKeyAux = reader.readLine().trim();
                            } finally {
                                reader.close();
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }
        decryptionKey = decryptionKeyAux;
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
    public IFile[] listFiles() {
        File[] filesAux = file.listFiles();
        IFile[] files = new IFile[filesAux.length];
        int i = 0;
        for (File fileAux : filesAux) {
            files[i] = new FileCustom(fileAux);
            i++;
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
}
