package com.jhonju.ps3netsrv.app.components;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Environment;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** Simple dialog to pick file. */
public class SimpleFileChooser {
    public interface FileSelectedListener {
        void onFileSelected(File file);
    }

    private final FileSelectedListener mFileListener;

    private final Activity mActivity;
    private static final String PARENT_DIR = "..";
    private String[] mFileList;
    private File mCurrentPath;
    private final boolean mOnlyDirectories;

    public SimpleFileChooser(Activity activity, File path, FileSelectedListener listener, boolean onlyDirectories) {
        this.mActivity = activity;
        this.mFileListener = listener;
        this.mOnlyDirectories = onlyDirectories;
        if (!path.exists())
            path = Environment.getExternalStorageDirectory();
        rebuildFileList(path);
    }

    public void showDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle(mCurrentPath.getPath());
        if (mOnlyDirectories) {
            builder.setPositiveButton("Ok", this::onPositiveButtonClick);
        }
        // populate dialog with list of files and directories.
        builder.setItems(mFileList, this::onDialogItemClicked);
        builder.show();
    }

    // Create list of files and directories.
    private void rebuildFileList(File path) {
        this.mCurrentPath = path;
        List<String> r = new ArrayList<>();

        if (path.getParentFile() != null)
            r.add(PARENT_DIR);

        File[] fileList = path.listFiles();
        if (fileList != null) {
            Arrays.sort(fileList, fileArrayComparator);
            for (File file : fileList) {
                if (mOnlyDirectories && !file.isDirectory())
                    break;
                r.add(file.getName());
            }
        }
        mFileList = r.toArray(new String[0]);
    }

    // Get selected file, dir, or parent dir.
    private File getSelectedFile(String selectedFileName) {
        if (selectedFileName.equals(PARENT_DIR))
            return mCurrentPath.getParentFile();
        else
            return new File(mCurrentPath, selectedFileName);
    }

    // Comparator for Arrays.sort(). Separate folders from files, order
    // alphabetically, ignore case.
    private static final Comparator<File> fileArrayComparator = Comparator
            .comparingInt((File file) -> file.isDirectory() ? 0 : 1)
            .thenComparing(File::getName, String.CASE_INSENSITIVE_ORDER);

    private void onPositiveButtonClick(DialogInterface dialog, int which) {
        File selectedFile = (which < 0) ? mCurrentPath : getSelectedFile(mFileList[which]);

        // always remove previous dlg first
        dialog.cancel();
        dialog.dismiss();

        if (mFileListener != null)
            mFileListener.onFileSelected(selectedFile);
    }

    // Event when user click item on dialog
    private void onDialogItemClicked(DialogInterface dialog, int which) {
        String selectedFileName = mFileList[which];
        File selectedFile = getSelectedFile(selectedFileName);

        // always remove previous dlg first
        dialog.cancel();
        dialog.dismiss();

        if (selectedFile.isDirectory()) {
            rebuildFileList(selectedFile);
            showDialog(); // create new dlg
        } else {
            if (mFileListener != null)
                mFileListener.onFileSelected(selectedFile);
        }
    }
}
