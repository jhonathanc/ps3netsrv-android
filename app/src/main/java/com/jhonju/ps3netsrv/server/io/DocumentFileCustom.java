package com.jhonju.ps3netsrv.server.io;

import static com.jhonju.ps3netsrv.server.utils.Utils.DKEY_EXT;
import static com.jhonju.ps3netsrv.server.utils.Utils.DOT_STR;
import static com.jhonju.ps3netsrv.server.utils.Utils.ISO_EXTENSION;
import static com.jhonju.ps3netsrv.server.utils.Utils.PS3ISO_FOLDER_NAME;
import static com.jhonju.ps3netsrv.server.utils.Utils.READ_ONLY_MODE;
import static com.jhonju.ps3netsrv.server.utils.Utils.REDKEY_FOLDER_NAME;
import static com.jhonju.ps3netsrv.server.utils.Utils.SECTOR_SIZE;

import android.content.ContentResolver;
import android.os.ParcelFileDescriptor;

import androidx.documentfile.provider.DocumentFile;

import com.jhonju.ps3netsrv.app.PS3NetSrvApp;
import com.jhonju.ps3netsrv.server.enums.EEncryptionType;
import com.jhonju.ps3netsrv.server.utils.Utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import javax.crypto.spec.SecretKeySpec;

public class DocumentFileCustom implements IFile {

    public final DocumentFile documentFile;
    private final SecretKeySpec decryptionKey;
    private final EEncryptionType encryptionType;
    private static final ContentResolver contentResolver = PS3NetSrvApp.getAppContext().getContentResolver();
    private ParcelFileDescriptor pfd;
    private FileInputStream fis;
    private FileChannel fileChannel;
    private final PS3RegionInfo[] regionInfos;
    private final byte[] iv = new byte[16];

    public DocumentFileCustom(DocumentFile documentFile) throws IOException {
        this.documentFile = documentFile;
        byte[] redumpKey = null;
        PS3RegionInfo[] regionInfos = null;
        if (documentFile != null && documentFile.isFile()) {
            this.pfd = contentResolver.openFileDescriptor(documentFile.getUri(), READ_ONLY_MODE);
            this.fis = new FileInputStream(pfd.getFileDescriptor());
            this.fileChannel = fis.getChannel();
            redumpKey = getRedumpKey(documentFile.getParentFile(), documentFile.getName());
        }
        if (redumpKey != null) {
            this.decryptionKey = new SecretKeySpec(redumpKey, "AES");
            this.encryptionType = EEncryptionType.REDUMP;

            int sec0Sec1Length = SECTOR_SIZE * 2;
            if (documentFile.length() >= sec0Sec1Length) {
                byte[] sec0sec1 = new byte[sec0Sec1Length];

                fileChannel.position(0);
                if (fileChannel.read(ByteBuffer.wrap(sec0sec1)) == sec0Sec1Length) {
                    regionInfos = Utils.getRegionInfos(sec0sec1);
                }
            }
        } else {
            this.decryptionKey = null;
            this.encryptionType = EEncryptionType.NONE;
        }
        this.regionInfos = regionInfos;
    }

    private static byte[] getRedumpKey(DocumentFile parent, String fileName) throws IOException {
        byte[] decryptionKey = null;
        if (parent != null) {
            String parentName = parent.getName();
            if (parentName != null && parentName.equalsIgnoreCase(PS3ISO_FOLDER_NAME)) {
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
                        decryptionKey = getKeyFromDocumentFile(documentFileAux);
                    }
                }
            }
        }
        return decryptionKey;
    }

    private static byte[] getKeyFromDocumentFile(DocumentFile file) throws IOException {
        InputStream is = contentResolver.openInputStream(file.getUri());
        try {
            byte[] key = new byte[16];
            if (is.read(key) < 0) {
                throw new IOException("Redump key is invalid");
            }
            return key;
        } finally {
            is.close();
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
    public IFile[] listFiles() throws IOException {
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

    public int read(byte[] buffer, long position) throws IOException {
        fileChannel.position(position);
        int bytesRead = fileChannel.read(ByteBuffer.wrap(buffer));
        if (encryptionType != EEncryptionType.NONE) {
            for (PS3RegionInfo regionInfo : regionInfos) {
                if ((position >= regionInfo.getFirstAddress()) && (position <= regionInfo.getLastAddress())) {
                    if (!regionInfo.isEncrypted()) {
                        return bytesRead;
                    }
                    Utils.decryptData(decryptionKey, iv, buffer, bytesRead / SECTOR_SIZE, position / SECTOR_SIZE);
                }
            }
        }
        return bytesRead;
    }

    @Override
    public void close() throws IOException {
        try {
            if (fileChannel != null) fileChannel.close();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        } finally {
            fileChannel = null;
        }

        try {
            if (fis != null) fis.close();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        } finally {
            fis = null;
        }

        try {
            if (pfd != null) pfd.close();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        } finally {
            pfd = null;
        }
    }
}
