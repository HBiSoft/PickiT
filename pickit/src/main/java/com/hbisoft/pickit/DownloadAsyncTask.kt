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

internal class DownloadAsyncTask(private val mUri: Uri?, context: Context, private val callback: CallBackTask, activity: Activity) : AsyncTask<Uri?, Int?, String?>() {
    private lateinit var pathPlusName: String
    private lateinit var inputStream: InputStream
    private var folder: File? = null
    private var returnCursor: Cursor? = null
    private var errorReason: String = ""
    private val mContext = WeakReference(context)
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
        mContext.get()?.let {
            folder = it.getExternalFilesDir("Temp")
            returnCursor = it.contentResolver.query(mUri!!, null, null, null, null)
            try {
                inputStream = it.contentResolver.openInputStream(mUri)!!
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }

        }
        // File is now available
        activityReference.get()?.runOnUiThread { callback.PickiTonPreExecute() }
        try {
            returnCursor?.use { cursor ->
                if (cursor.moveToFirst()) {
                    mUri?.scheme?.let {
                        when (it) {
                            "content" ->
                                with(cursor){size = getLong(getColumnIndex(OpenableColumns.SIZE)).toInt()}
                            "file" ->
                                size = File(mUri.path.orEmpty()).length().toInt()
                        }
                    }
                }
                cursor.close()
            }
            pathPlusName = "${folder.toString()}/${getFileName(mUri, mContext.get())}"
            file = File(pathPlusName)
            val bis = BufferedInputStream(inputStream)
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
            Log.e("Pickit IOException = ", e.message.toString())
            errorReason = e.message ?: ""
        }
        return file?.absolutePath
    }

    private fun getFileName(uri: Uri?, context: Context?): String? {
        var result: String? = null

        uri?.scheme?.let {
            if (it == "content") {
                val cursor = context?.contentResolver?.query(uri, null, null, null, null)
                cursor?.let {
                    if (cursor.moveToFirst()) {
                        result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                    }
                    cursor.close()
                }
            }
        }
        if (result == null) {
            result = uri?.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut.plus(1))
            }
        }
        return result
    }

    override fun onPostExecute(result: String?) {
        if (result == null) {
            callback.PickiTonPostExecute(
                    pathPlusName,
                    wasDriveFile = true,
                    wasSuccessful = false,
                    reason = errorReason)
        } else {
            callback.PickiTonPostExecute(
                    pathPlusName,
                    wasDriveFile = true,
                    wasSuccessful = true)
        }
    }
}