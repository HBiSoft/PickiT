package com.hbisoft.pickit

import android.app.Activity
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.AsyncTask
import android.provider.OpenableColumns
import android.util.Log
import java.io.*
import java.lang.ref.WeakReference
import java.util.*

internal class DownloadAsyncTask(private val mUri: Uri?, context: Context, private val callback: CallBackTask, activity: Activity) : AsyncTask<Uri?, Int?, String?>() {
    private val mContext = WeakReference(context)
    private var pathPlusName: String? = null
    private var folder: File? = null
    private var returnCursor: Cursor? = null
    private var `is`: InputStream? = null
    private var errorReason: String? = ""
    private val activityReference = WeakReference(activity)
    override fun onPreExecute() {
        callback.PickiTonUriReturned()
    }

    override fun onProgressUpdate(vararg values: Int?) {
        super.onProgressUpdate(*values)
        val post: Int? = values[0]
        callback.PickiTonProgressUpdate(post!!)
    }

    override fun doInBackground(vararg params: Uri?): String? {
        var file: File? = null
        var size = -1
        val context = mContext.get()
        if (context != null) {
            folder = context.getExternalFilesDir("Temp")
            returnCursor = context.contentResolver.query(mUri!!, null, null, null, null)
            try {
                `is` = context.contentResolver.openInputStream(mUri)
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }
        }

        // File is now available
        activityReference.get()?.runOnUiThread { callback.PickiTonPreExecute() }
        try {
                try {
                if (returnCursor != null && returnCursor!!.moveToFirst()) {
                    if (mUri?.scheme != null) if (mUri.scheme == "content") {
                        val sizeIndex = returnCursor?.getColumnIndex(OpenableColumns.SIZE)
                        size = returnCursor?.getLong(sizeIndex!!)?.toInt()!!
                    } else if (mUri.scheme == "file") {
                        val ff = File(mUri.path)
                        size = ff.length().toInt()
                    }
                }
            } finally {
                if (returnCursor != null) returnCursor?.close()
            }
            pathPlusName = folder.toString() + "/" + getFileName(mUri, mContext.get())
            file = File(folder.toString() + "/" + getFileName(mUri, mContext.get()))
            val bis = BufferedInputStream(`is`)
            val fos = FileOutputStream(file)
            val data = ByteArray(1024)
            var total: Long = 0
            var count: Int
            while (bis.read(data).also { count = it } != -1) {
                if (!isCancelled) {
                    total += count.toLong()
                    if (size != -1) {
                        try {
                            publishProgress((total * 100 / size).toInt())
                        } catch (e: Exception) {
                            Log.i("PickiT -", "File size is less than 1")
                            publishProgress(0)
                        }
                    }
                    fos.write(data, 0, count)
                }
            }
            fos.flush()
            fos.close()
        } catch (e: IOException) {
            Log.e("Pickit IOException = ", e.message)
            errorReason = e.message
        }
        return file?.absolutePath
    }

    private fun getFileName(uri: Uri?, context: Context?): String? {
        var result: String? = null
        if (uri?.scheme != null) {
            if (uri.scheme == "content") {
                val cursor = context?.contentResolver?.query(uri, null, null, null, null)
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri?.path
            assert(result != null)
            val cut = result?.lastIndexOf('/')
            if (cut != -1) {
                result = result?.substring(cut!!.plus(1))
            }
        }
        return result
    }

    override fun onPostExecute(result: String?) {
        if (result == null) {
            callback.PickiTonPostExecute(pathPlusName, wasDriveFile = true, wasSuccessful = false, reason = errorReason)
        } else {
            callback.PickiTonPostExecute(pathPlusName, wasDriveFile = true, wasSuccessful = true, reason = "")
        }
    }
}