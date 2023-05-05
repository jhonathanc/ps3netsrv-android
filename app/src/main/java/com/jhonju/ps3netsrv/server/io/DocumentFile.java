package com.jhonju.ps3netsrv.server.io;

import java.io.IOException;

public class DocumentFile implements IFile {

    public final androidx.documentfile.provider.DocumentFile documentFile;
    public DocumentFile(androidx.documentfile.provider.DocumentFile documentFile) {
        this.documentFile = documentFile;
    }

    public androidx.documentfile.provider.DocumentFile getDocumentFile() {
        return documentFile;
    }

    @Override
    public boolean exists() {
        return documentFile != null && documentFile.exists();
    }

    @Override
    public boolean isFile() {
        return documentFile.isFile();
    }

    @Override
    public boolean isDirectory() {
        return documentFile.isDirectory();
    }

    @Override
    public boolean delete() {
        return documentFile.delete();
    }

    @Override
    public long length() {
        return documentFile.length();
    }

    @Override
    public IFile[] listFiles() {
        androidx.documentfile.provider.DocumentFile[] filesAux = documentFile.listFiles();
        IFile[] files = new IFile[filesAux.length];
        int i = 0;
        for (androidx.documentfile.provider.DocumentFile fileAux : filesAux) {
            files[i] = new DocumentFile(fileAux);
            i++;
        }
        return files;
    }

    @Override
    public long lastModified() {
        return documentFile.length();
    }

    @Override
    public String getName() {
        return documentFile.getName();
    }

    @Override
    public String[] list() {
        androidx.documentfile.provider.DocumentFile[] filesAux = documentFile.listFiles();
        String[] files = new String[filesAux.length];
        int i = 0;
        for (androidx.documentfile.provider.DocumentFile fileAux : filesAux) {
            files[i] = fileAux.getName();
            i++;
        }
        return files;
    }

    @Override
    public IFile findFile(String fileName) throws IOException {
        return new DocumentFile(documentFile.findFile(fileName));
    }
}
