package com.hbisoft.pickit;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.File;

public class PickiT implements CallBackTask{
    private Context context;
    private final PickiTCallbacks pickiTCallbacks;
    private String returnedPath = "";
    private boolean isDriveFile = false;
    private DownloadAsyncTask asyntask;

    public PickiT (Context context, PickiTCallbacks listener){
        this.context = context;
        this.pickiTCallbacks = listener;
    }

    public void getPath(Uri uri, int APILevel){
        if (APILevel>=19){
            if (isOneDrive(uri)||isDropBox(uri)||isGoogleDrive(uri)){
                isDriveFile = true;
                downloadFile(uri);
            }else {
                returnedPath = Utils.getRealPathFromURI_API19(context, uri);
                if (returnedPath == null || returnedPath.equals("")){
                    pickiTCallbacks.PickiTonCompleteListener(returnedPath, false, false, Utils.errorReason());
                }else {
                    pickiTCallbacks.PickiTonCompleteListener(returnedPath, false, true, "");
                }
            }
        }else{
            //Todo: Test API <19
            returnedPath = Utils.getRealPathFromURI_BelowAPI19(context, uri);
            pickiTCallbacks.PickiTonCompleteListener(returnedPath, false, true, "");
        }

    }

    private void downloadFile(Uri uri){
        asyntask = new DownloadAsyncTask(uri, context, this);
        asyntask.execute();
    }

    public void cancelTask(){
        if (asyntask!=null){
            asyntask.cancel(true);
            deleteTemporaryFile();
        }
    }

    private boolean isDropBox(Uri uri) {
        return String.valueOf(uri).toLowerCase().contains("content://com.dropbox.android");
    }

    private boolean isGoogleDrive(Uri uri) {
        return String.valueOf(uri).toLowerCase().contains("com.google.android.apps");
    }

    private boolean isOneDrive(Uri uri){
        return String.valueOf(uri).toLowerCase().contains("com.microsoft.skydrive.content");
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
    public void PickiTonPostExecute(String path, boolean wasDriveFile) {
        if (isDriveFile){
            pickiTCallbacks.PickiTonCompleteListener(path, true, true, "");
        }
    }

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
