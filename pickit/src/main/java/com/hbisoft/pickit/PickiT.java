package com.hbisoft.pickit;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.File;

public class PickiT implements CallBackTask{
    private Context context;
    private final PickiTCallbacks pickiTCallbacks;
    private boolean isDriveFile = false;
    private boolean isFromUnknownProvider = false;
    private DownloadAsyncTask asyntask;

    public PickiT (Context context, PickiTCallbacks listener){
        this.context = context;
        this.pickiTCallbacks = listener;
    }

    public void getPath(Uri uri, int APILevel){
        String returnedPath;
        if (APILevel>=19){
            // Drive file was selected
            if (isOneDrive(uri)||isDropBox(uri)||isGoogleDrive(uri)){
                isDriveFile = true;
                downloadFile(uri, "tempFile");
            }
            // Local file was selected
            else {
                returnedPath = Utils.getRealPathFromURI_API19(context, uri);

                final MimeTypeMap mime = MimeTypeMap.getSingleton();
                String subStringExtension = String.valueOf(returnedPath).substring(String.valueOf(returnedPath).lastIndexOf(".") + 1);
                String extensionFromMime = mime.getExtensionFromMimeType(context.getContentResolver().getType(uri));

                // Path is null - error occurred
                if (returnedPath == null || returnedPath.equals("")){
                    pickiTCallbacks.PickiTonCompleteListener(returnedPath, false, false, false, Utils.errorReason());
                }
                // Path is not null
                else {
                    // This can be caused by twe situations
                    // 1. The file was selected from an unknown provider
                    // 2. getExtensionFromMimeType returned an unknown mime type for example "audio/mp4"
                    //
                    // When this is case we will copy/write the file to the temp folder, same as when a file is selected from Google Drive etc.
                    // We provide a name by getting the text after the last "/"
                    // Remember if the extension can't be found, it will not be added, but you will still be able to use the file
                    //Todo: Add checks for unknown file extensions

                    if (!subStringExtension.equals(extensionFromMime)) {
                        String fileName = returnedPath.substring(returnedPath.lastIndexOf("/") + 1);
                        isFromUnknownProvider = true;
                        downloadFile(uri, fileName);
                        return;
                    }

                    // Path can be returned, no need to make a "copy"
                    pickiTCallbacks.PickiTonCompleteListener(returnedPath, false, false, true, "");
                }
            }
        }else{
            //Todo: Test API <19
            returnedPath = Utils.getRealPathFromURI_BelowAPI19(context, uri);
            pickiTCallbacks.PickiTonCompleteListener(returnedPath, false, false, true, "");
        }

    }

    // Create a new file from the Uri that was selected
    private void downloadFile(Uri uri, String fileName){
        asyntask = new DownloadAsyncTask(uri, context, this, fileName);
        asyntask.execute();
    }

    // End the "copying" of the file
    public void cancelTask(){
        if (asyntask!=null){
            asyntask.cancel(true);
            deleteTemporaryFile();
        }
    }

    // Check different providers
    private boolean isDropBox(Uri uri) {
        return String.valueOf(uri).toLowerCase().contains("content://com.dropbox.android");
    }
    private boolean isGoogleDrive(Uri uri) {
        return String.valueOf(uri).toLowerCase().contains("com.google.android.apps");
    }
    private boolean isOneDrive(Uri uri){
        return String.valueOf(uri).toLowerCase().contains("com.microsoft.skydrive.content");
    }

    // Listeners
    @Override
    public void PickiTonPreExecute() {
        pickiTCallbacks.PickiTonStartListener();
    }

    @Override
    public void PickiTonProgressUpdate(int progress) {
        pickiTCallbacks.PickiTonProgressUpdate(progress);
    }

    @Override
    public void PickiTonPostExecute(String path, boolean wasDriveFile, boolean wasSuccessful, String reason) {
        if (wasSuccessful) {
            if (isDriveFile) {
                pickiTCallbacks.PickiTonCompleteListener(path, true, false, true, "");
            } else if (isFromUnknownProvider) {
                pickiTCallbacks.PickiTonCompleteListener(path, false, true, true, "");
            }
        }else {
            if (isDriveFile) {
                pickiTCallbacks.PickiTonCompleteListener(path, true, false, false, reason);
            } else if (isFromUnknownProvider) {
                pickiTCallbacks.PickiTonCompleteListener(path, false, true, false, reason);
            }
        }
    }

    // Delete the temporary folder
    public void deleteTemporaryFile(){
        File folder = context.getExternalFilesDir("Temp");
        if (folder != null) {
            if (deleteDirectory(folder)){
                Log.i("PickiT "," deleteDirectory was called");
            }
        }
    }

    private boolean deleteDirectory(File path) {
        if(path.exists()) {
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
        return(path.delete());
    }

}
