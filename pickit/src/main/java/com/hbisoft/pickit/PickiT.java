package com.hbisoft.pickit;

import android.app.Activity;
import android.content.ContentResolver;
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
    private boolean unknownProviderCalledBefore = false;
    private Activity mActivity;

    public PickiT (Context context, PickiTCallbacks listener, Activity activity){
        this.context = context;
        this.pickiTCallbacks = listener;
        mActivity = activity;
    }

    public void getPath(Uri uri, int APILevel){
        String returnedPath;
        if (APILevel>=19){
            // Drive file was selected
            if (isOneDrive(uri)||isDropBox(uri)||isGoogleDrive(uri)){
                isDriveFile = true;
                downloadFile(uri);
            }
            // Local file was selected
            else {
                returnedPath = Utils.getRealPathFromURI_API19(context, uri);

                //Get the file extension
                final MimeTypeMap mime = MimeTypeMap.getSingleton();
                String subStringExtension = String.valueOf(returnedPath).substring(String.valueOf(returnedPath).lastIndexOf(".") + 1);
                String extensionFromMime = mime.getExtensionFromMimeType(context.getContentResolver().getType(uri));

                // Path is null
                if (returnedPath == null || returnedPath.equals("")){
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
                            }else if (Utils.errorReason() != null && Utils.errorReason().contains("column '_data' does not exist")){
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

                    if (!subStringExtension.equals(extensionFromMime) && uri.getScheme() != null && uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
                        isFromUnknownProvider = true;
                        downloadFile(uri);
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
    private void downloadFile(Uri uri){
        asyntask = new DownloadAsyncTask(uri, context, this, mActivity);
        asyntask.execute();
    }

    // End the "copying" of the file
    public void cancelTask(){
        if (asyntask!=null){
            asyntask.cancel(true);
            deleteTemporaryFile();
        }
    }

    public boolean wasLocalFileSelected(Uri uri){
        return !isDropBox(uri) && !isGoogleDrive(uri) && !isOneDrive(uri);
    }

    // Check different providers
    private boolean isDropBox(Uri uri) {
        return String.valueOf(uri).toLowerCase().contains("content://com.dropbox.");
    }
    private boolean isGoogleDrive(Uri uri) {
        return String.valueOf(uri).toLowerCase().contains("com.google.android.apps");
    }
    private boolean isOneDrive(Uri uri){
        return String.valueOf(uri).toLowerCase().contains("com.microsoft.skydrive.content");
    }

    // PickiT callback Listeners
    @Override
    public void PickiTonUriReturned() {
        pickiTCallbacks.PickiTonUriReturned();
    }

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
        unknownProviderCalledBefore = false;
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
