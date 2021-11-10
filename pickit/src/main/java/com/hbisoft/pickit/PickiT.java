package com.hbisoft.pickit;

import android.app.Activity;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.util.ArrayList;

import static com.hbisoft.pickit.Utils.getFilePath;

public class PickiT implements CallBackTask {
    private final Context context;
    private final PickiTCallbacks pickiTCallbacks;
    private boolean isDriveFile = false;
    private boolean isMsfDownload = false;
    private boolean isFromUnknownProvider = false;
    private DownloadAsyncTask asyntask;
    private boolean unknownProviderCalledBefore = false;
    private final Activity mActivity;
    ArrayList<String> multiplePaths = new ArrayList<>();
    private boolean wasMultipleFileSelected = false;
    private int countMultiple;
    private int driveCountRef;
    private boolean enableProc = true;

    public PickiT(Context context, PickiTCallbacks listener, Activity activity) {
        this.context = context;
        this.pickiTCallbacks = listener;
        mActivity = activity;
    }

    public void getMultiplePaths(ClipData clipData) {
        wasMultipleFileSelected = true;
        countMultiple = clipData.getItemCount();
        for (int i = 0; i < clipData.getItemCount(); i++) {
            Uri imageUri = clipData.getItemAt(i).getUri();
            getPath(imageUri, Build.VERSION.SDK_INT);
        }
        if (!isDriveFile) {
            pickiTCallbacks.PickiTonMultipleCompleteListener(multiplePaths, true, "");
            multiplePaths.clear();
            wasMultipleFileSelected = false;
            wasUriReturnedCalledBefore = false;
            wasPreExecuteCalledBefore = false;
        }

    }

    public void getPath(Uri uri, int APILevel) {
        String returnedPath;
        if (APILevel >= 19) {
            String docId = null;
            // This is only used when a file is selected from a sub-directory inside the Downloads folder
            // and when the Uri returned has the msf: prefix
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    docId = DocumentsContract.getDocumentId(uri);
                }
            } catch (Exception e){
                // Ignore
            }
            // Drive file was selected
            if (isOneDrive(uri) || isDropBox(uri) || isGoogleDrive(uri)) {
                isDriveFile = true;
                downloadFile(uri);
            }
            // File was selected from Downloads provider
            else if (docId !=null && docId.startsWith("msf")) {
                String fileName = getFilePath(context, uri);
                try {
                    File file = new File(Environment.getExternalStorageDirectory().toString() + "/Download/"+ fileName);
                    // If the file exists in the Downloads directory
                    // we can return the path directly
                    if (file.exists()){
                        pickiTCallbacks.PickiTonCompleteListener(file.getAbsolutePath(), false, false, true, "");
                    }
                    // The file is in a sub-directory in Downloads
                    // We will first try to make use of the /proc/ protocol
                    // if /proc/ doesn't work, or if there is any issue trying to get access to the file, it gets copied to the applications directory
                    else {
                        if (enableProc) {
                            ParcelFileDescriptor parcelFileDescriptor;
                            try {
                                parcelFileDescriptor = context.getContentResolver().openFileDescriptor(uri, "r");
                                int fd = parcelFileDescriptor.getFd();
                                int pid = android.os.Process.myPid();
                                String mediaFile = "/proc/" + pid + "/fd/" + fd;
                                File file1 = new File(mediaFile);
                                if (file1.exists() && file1.canRead() && file1.canWrite()) {
                                    pickiTCallbacks.PickiTonCompleteListener(file1.getAbsolutePath(), false, false, true, "");
                                } else {
                                    isMsfDownload = true;
                                    downloadFile(uri);
                                }
                            } catch (Exception e) {
                                isMsfDownload = true;
                                downloadFile(uri);
                            }
                        } else {
                            isMsfDownload = true;
                            downloadFile(uri);
                        }
                    }
                }catch (Exception e){
                    isMsfDownload = true;
                    downloadFile(uri);
                }

            }
            // Local file was selected
            else {
                returnedPath = Utils.getRealPathFromURI_API19(context, uri);
                //Get the file extension
                final MimeTypeMap mime = MimeTypeMap.getSingleton();
                String subStringExtension = String.valueOf(returnedPath).substring(String.valueOf(returnedPath).lastIndexOf(".") + 1);
                String extensionFromMime = mime.getExtensionFromMimeType(context.getContentResolver().getType(uri));

                // Path is null
                if (returnedPath == null || returnedPath.equals("")) {
                    // This can be caused by two situations
                    // 1. The file was selected from a third party app and the data column returned null (for example EZ File Explorer)
                    // Some file providers (like EZ File Explorer) will return a URI as shown below:
                    // content://es.fileexplorer.filebrowser.ezfilemanager.externalstorage.documents/document/primary%3AFolderName%2FNameOfFile.mp4
                    // When you try to read the _data column, it will return null, without trowing an exception
                    // In this case the file need to copied/created a new file in the temporary folder
                    // 2. There was an error
                    // In this case call PickiTonCompleteListener and get/provide the reason why it failed

                    //We first check if it was called before, avoiding multiple calls
                    if (!unknownProviderCalledBefore) {
                        unknownProviderCalledBefore = true;
                        if (uri.getScheme() != null && uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
                            //Then we check if the _data colomn returned null
                            if (Utils.errorReason() != null && Utils.errorReason().equals("dataReturnedNull")) {
                                isFromUnknownProvider = true;
                                //Copy the file to the temporary folder
                                downloadFile(uri);
                                return;
                            } else if (Utils.errorReason() != null && Utils.errorReason().contains("column '_data' does not exist")) {
                                isFromUnknownProvider = true;
                                //Copy the file to the temporary folder
                                downloadFile(uri);
                                return;
                            } else if (Utils.errorReason() != null && Utils.errorReason().equals("uri")) {
                                isFromUnknownProvider = true;
                                //Copy the file to the temporary folder
                                downloadFile(uri);
                                return;
                            }
                        }
                    }
                    //Else an error occurred, get/set the reason for the error
                    pickiTCallbacks.PickiTonCompleteListener(returnedPath, false, false, false, Utils.errorReason());
                }
                // Path is not null
                else {
                    // This can be caused by two situations
                    // 1. The file was selected from an unknown provider (for example a file that was downloaded from a third party app)
                    // 2. getExtensionFromMimeType returned an unknown mime type for example "audio/mp4"
                    //
                    // When this is case we will copy/write the file to the temp folder, same as when a file is selected from Google Drive etc.
                    // We provide a name by getting the text after the last "/"
                    // Remember if the extension can't be found, it will not be added, but you will still be able to use the file
                    //Todo: Add checks for unknown file extensions

                    if (!subStringExtension.equals("jpeg") && !subStringExtension.equals(extensionFromMime) && uri.getScheme() != null && uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
                        isFromUnknownProvider = true;
                        downloadFile(uri);
                        return;
                    }

                    // Path can be returned, no need to make a "copy"
                    if (wasMultipleFileSelected){
                        multiplePaths.add(returnedPath);
                    }else {
                        pickiTCallbacks.PickiTonCompleteListener(returnedPath, false, false, true, "");
                    }
                }
            }
        } else {
            //Todo: Test API <19
            returnedPath = Utils.getRealPathFromURI_BelowAPI19(context, uri);
            pickiTCallbacks.PickiTonCompleteListener(returnedPath, false, false, true, "");
        }

    }

    // When selecting a file inside a sub-directory, some devices (Android 10>) will use the msf: provider
    // which we can't convert to a path without copying it to the applications directory.
    // For that, we make use of the /proc/ protocol.
    // Since this has not been tested fully, I'm adding the option to disable it.
    // I don't think this is necessary, but I'm giving the option nevertheless.
    public void enableProcProtocol(boolean shouldEnable){
        enableProc = shouldEnable;
    }

    // Create a new file from the Uri that was selected
    private void downloadFile(Uri uri) {
        asyntask = new DownloadAsyncTask(uri, context, this, mActivity);
        asyntask.execute();
    }

    // End the "copying" of the file
    public void cancelTask() {
        if (asyntask != null) {
            asyntask.cancel(true);
            deleteTemporaryFile(context);
        }
    }

    public boolean wasLocalFileSelected(Uri uri) {
        return !isDropBox(uri) && !isGoogleDrive(uri) && !isOneDrive(uri);
    }

    // Check different providers
    private boolean isDropBox(Uri uri) {
        return String.valueOf(uri).toLowerCase().contains("content://com.dropbox.");
    }

    private boolean isGoogleDrive(Uri uri) {
        return String.valueOf(uri).toLowerCase().contains("com.google.android.apps");
    }

    private boolean isOneDrive(Uri uri) {
        return String.valueOf(uri).toLowerCase().contains("com.microsoft.skydrive.content");
    }

    // PickiT callback Listeners
    private boolean wasUriReturnedCalledBefore = false;
    @Override
    public void PickiTonUriReturned() {
        if (wasMultipleFileSelected) {
            if (!wasUriReturnedCalledBefore) {
                pickiTCallbacks.PickiTonUriReturned();
                wasUriReturnedCalledBefore = true;
            }
        }else{
            pickiTCallbacks.PickiTonUriReturned();
        }
    }
    private boolean wasPreExecuteCalledBefore = false;
    @Override
    public void PickiTonPreExecute() {
        if (wasMultipleFileSelected || isMsfDownload) {
            if (!wasPreExecuteCalledBefore) {
                wasPreExecuteCalledBefore = true;
                pickiTCallbacks.PickiTonStartListener();
            }
        }else{
            pickiTCallbacks.PickiTonUriReturned();
        }
    }

    @Override
    public void PickiTonProgressUpdate(int progress) {
        pickiTCallbacks.PickiTonProgressUpdate(progress);
    }

    @Override
    public void PickiTonPostExecute(String path, boolean wasDriveFile, boolean wasSuccessful, String reason) {
        unknownProviderCalledBefore = false;
        if (wasSuccessful) {
            if (wasMultipleFileSelected){
                driveCountRef++;
                multiplePaths.add(path);
                if (driveCountRef == countMultiple){
                    wasPreExecuteCalledBefore = false;
                    wasUriReturnedCalledBefore = false;
                    pickiTCallbacks.PickiTonMultipleCompleteListener(multiplePaths, true, "");
                    multiplePaths.clear();
                    wasUriReturnedCalledBefore = false;
                    wasPreExecuteCalledBefore = false;
                }
            }else {
                if (isDriveFile) {
                    pickiTCallbacks.PickiTonCompleteListener(path, true, false, true, "");
                } else if (isFromUnknownProvider) {
                    pickiTCallbacks.PickiTonCompleteListener(path, false, true, true, "");
                }else if (isMsfDownload){
                    pickiTCallbacks.PickiTonCompleteListener(path, false, true, true, "");
                }
            }
        } else {
            if (isDriveFile) {
                pickiTCallbacks.PickiTonCompleteListener(path, true, false, false, reason);
            } else if (isFromUnknownProvider) {
                pickiTCallbacks.PickiTonCompleteListener(path, false, true, false, reason);
            }
        }
    }

    // Delete the temporary folder
    public void deleteTemporaryFile(Context context) {
        File folder = context.getExternalFilesDir("Temp");
        if (folder != null) {
            if (deleteDirectory(folder)) {
                Log.i("PickiT ", " deleteDirectory was called");
            }
        }
    }

    private boolean deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            if (files == null) {
                return false;
            }
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    boolean wasSuccessful = file.delete();
                    if (wasSuccessful) {
                        Log.i("Deleted ", "successfully");
                    }
                }
            }
        }
        return (path.delete());
    }

}
