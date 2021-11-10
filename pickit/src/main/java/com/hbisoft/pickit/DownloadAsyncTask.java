package com.hbisoft.pickit;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.OpenableColumns;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

class DownloadAsyncTask extends AsyncTask<Uri, Integer, String> {
    private final Uri mUri;
    private final CallBackTask callback;
    private final WeakReference<Context> mContext;
    private String pathPlusName;
    private File folder;
    private Cursor returnCursor;
    private InputStream is = null;
    private String errorReason = "";
    private final WeakReference<Activity> activityReference;

    DownloadAsyncTask(Uri uri, Context context, CallBackTask callback, Activity activity) {
        this.mUri = uri;
        mContext = new WeakReference<>(context);
        this.callback = callback;
        activityReference = new WeakReference<>(activity);
    }

    @Override
    protected void onPreExecute() {
        callback.PickiTonUriReturned();
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        int post = values[0];
        callback.PickiTonProgressUpdate(post);
    }

    @Override
    protected String doInBackground(Uri... params) {
        File file = null;
        int size = -1;

        Context context = mContext.get();
        if (context != null) {
            folder = context.getExternalFilesDir("Temp");
            if (folder!= null && !folder.exists()) {
                if (folder.mkdirs()) {
                    Log.i("PickiT : ", "Temp folder createdd");
                }
            }
            returnCursor = context.getContentResolver().query(mUri, null, null, null, null);
            try {
                is = context.getContentResolver().openInputStream(mUri);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        // File is now available
        activityReference.get().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                callback.PickiTonPreExecute();
            }
        });

        try {
            try {
                if (returnCursor != null && returnCursor.moveToFirst()){
                    if (mUri.getScheme() != null)
                    if (mUri.getScheme().equals("content")) {
                        int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
                        size = (int) returnCursor.getLong(sizeIndex);
                    }else if (mUri.getScheme().equals("file")) {
                        File ff = new File(mUri.getPath());
                        size = (int) ff.length();
                    }
                }
            }
            finally {
                if (returnCursor != null)
                returnCursor.close();
            }

            pathPlusName = folder + "/" + getFileName(mUri, mContext.get());
            file = new File(folder + "/" + getFileName(mUri, mContext.get()));

            BufferedInputStream bis = new BufferedInputStream(is);
            FileOutputStream fos = new FileOutputStream(file);

            byte[] data = new byte[1024];
            long total = 0;
            int count;
            while ((count = bis.read(data)) != -1) {
                if (!isCancelled()) {
                    total += count;
                    if (size != -1) {
                        try {
                            publishProgress((int) ((total * 100) / size));
                        }catch(Exception e){
                            Log.i("PickiT -", "File size is less than 1");
                            publishProgress(0);
                        }
                    }
                    fos.write(data, 0, count);
                }
            }
            fos.flush();
            fos.close();

        } catch (IOException e) {
            Log.e("Pickit IOException = ", e.getMessage());
            errorReason = e.getMessage();
        }

        return file.getAbsolutePath();

    }

    private String getFileName(Uri uri, Context context) {
        String result = null;
        if (uri.getScheme() != null) {
            if (uri.getScheme().equals("content")) {
                Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            assert result != null;
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    protected void onPostExecute(String result) {
        if(result == null){
            callback.PickiTonPostExecute(pathPlusName, true, false, errorReason);
        }else {
            callback.PickiTonPostExecute(pathPlusName, true, true, "");
        }
    }
}