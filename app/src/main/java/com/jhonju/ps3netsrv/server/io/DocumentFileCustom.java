package com.jhonju.ps3netsrv.server.io;

import static com.jhonju.ps3netsrv.server.utils.Utils.DKEY_EXT;
import static com.jhonju.ps3netsrv.server.utils.Utils.DOT_STR;

import androidx.documentfile.provider.DocumentFile;

import com.jhonju.ps3netsrv.app.PS3NetSrvApp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class DocumentFileCustom implements IFile {

    public final DocumentFile documentFile;
    private final String decryptionKey;

    public DocumentFileCustom(DocumentFile documentFile) {
        String decryptionKeyAux = null;
        this.documentFile = documentFile;
        if (documentFile != null && documentFile.isFile()) {
            DocumentFile parent = documentFile.getParentFile();

            String fileName = documentFile.getName();
            int pos = fileName.lastIndexOf(DOT_STR);
            if (pos >= 0) {
                DocumentFile decriptionKeyFile = parent.findFile(fileName.substring(0, pos) + DKEY_EXT);
                if (decriptionKeyFile != null) {
                    try {
                        InputStream is = PS3NetSrvApp.getAppContext().getContentResolver().openInputStream(decriptionKeyFile.getUri());
                        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
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
        decryptionKey = decryptionKeyAux;
    }

    public DocumentFile getDocumentFile() {
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
        DocumentFile[] filesAux = documentFile.listFiles();
        IFile[] files = new IFile[filesAux.length];
        int i = 0;
        for (DocumentFile fileAux : filesAux) {
            files[i] = new DocumentFileCustom(fileAux);
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
        DocumentFile[] filesAux = documentFile.listFiles();
        String[] files = new String[filesAux.length];
        int i = 0;
        for (DocumentFile fileAux : filesAux) {
            files[i] = fileAux.getName();
            i++;
        }
        return files;
    }

    @Override
    public IFile findFile(String fileName) throws IOException {
        return new DocumentFileCustom(documentFile.findFile(fileName));
    }

    @Override
    public String getDecryptionKey() {
        return decryptionKey;
    }
}
