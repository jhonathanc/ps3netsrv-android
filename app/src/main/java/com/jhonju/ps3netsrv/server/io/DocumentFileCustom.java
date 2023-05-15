package com.jhonju.ps3netsrv.server.io;

import static com.jhonju.ps3netsrv.server.utils.Utils.DKEY_EXT;
import static com.jhonju.ps3netsrv.server.utils.Utils.DOT_STR;
import static com.jhonju.ps3netsrv.server.utils.Utils.ISO_EXTENSION;
import static com.jhonju.ps3netsrv.server.utils.Utils.PS3ISO_FOLDER_NAME;
import static com.jhonju.ps3netsrv.server.utils.Utils.REDKEY_FOLDER_NAME;

import android.content.ContentResolver;
import android.os.ParcelFileDescriptor;

import androidx.documentfile.provider.DocumentFile;

import com.jhonju.ps3netsrv.app.PS3NetSrvApp;
import com.jhonju.ps3netsrv.server.enums.EEncryptionType;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class DocumentFileCustom implements IFile {

    public final DocumentFile documentFile;
    private final String decryptionKey;
    private final EEncryptionType encryptionType;
    private static final ContentResolver contentResolver = PS3NetSrvApp.getAppContext().getContentResolver();

    public DocumentFileCustom(DocumentFile documentFile) {
        this.documentFile = documentFile;
        String decryptionKey = null;
        EEncryptionType encryptionType = EEncryptionType.NONE;
        if (documentFile != null && documentFile.isFile()) {
            DocumentFile parent = documentFile.getParentFile();
            if (parent != null) {
                String parentName = parent.getName();
                if (parentName != null && parentName.equalsIgnoreCase(PS3ISO_FOLDER_NAME)) {
                    String fileName = documentFile.getName();
                    int pos = fileName == null ? -1 : fileName.lastIndexOf(DOT_STR);
                    if (pos >= 0 && fileName.substring(pos).equalsIgnoreCase(ISO_EXTENSION)) {
                        DocumentFile documentFileAux = parent.findFile(fileName.substring(0, pos) + DKEY_EXT);
                        if (documentFileAux == null || documentFileAux.isDirectory()) {
                            documentFileAux = parent.getParentFile();
                            if (documentFileAux != null) {
                                documentFileAux = documentFileAux.findFile(REDKEY_FOLDER_NAME);
                                if (documentFileAux != null && documentFileAux.isDirectory()) {
                                    documentFileAux = documentFileAux.findFile(fileName.substring(0, fileName.lastIndexOf(DOT_STR)) + DKEY_EXT);
                                }
                            }
                        }
                        if (documentFileAux != null && documentFileAux.isFile()) {
                            decryptionKey = getStringFromDocumentFile(documentFileAux);
                            encryptionType = EEncryptionType.REDUMP;
                        }
                    }
                }
            }
        }
        this.decryptionKey = decryptionKey;
        this.encryptionType = encryptionType;
    }

    private static String getStringFromDocumentFile(DocumentFile file) {
        try {
            InputStream is = contentResolver.openInputStream(file.getUri());
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            try {
                return reader.readLine().trim();
            } finally {
                reader.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
    public IFile findFile(String fileName) {
        return new DocumentFileCustom(documentFile.findFile(fileName));
    }

    public int read(byte[] buffer, long position) throws IOException {
        ParcelFileDescriptor pfd = contentResolver.openFileDescriptor(documentFile.getUri(), "r");
        FileInputStream fis = new FileInputStream(pfd.getFileDescriptor());
        FileChannel fileChannel = fis.getChannel();
        try {
            fileChannel.position(position);
            return fileChannel.read(ByteBuffer.wrap(buffer));
        } finally {
            fileChannel.close();
            fis.close();
            pfd.close();
        }
    }

    @Override
    public void close() throws IOException {
        //TODO: check what classes can be closed here
    }
}
