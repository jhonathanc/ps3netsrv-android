package com.jhonju.ps3netsrv.server.io;

import java.io.IOException;

public class File implements IFile {

    private final java.io.File file;

    public File(java.io.File file) {
        this.file = file;
    }

    public java.io.File getFile() {
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
        java.io.File[] filesAux = file.listFiles();
        IFile[] files = new IFile[filesAux.length];
        int i = 0;
        for (java.io.File fileAux : filesAux) {
            files[i] = new File(fileAux);
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
        return new File(new java.io.File(file.getCanonicalPath() + "/" + fileName));
    }
}
