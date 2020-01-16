package com.hbisoft.pickit

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.AsyncTask
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import java.io.BufferedInputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.lang.ref.WeakReference

class DownloadAsyncTask(
    private val uri: Uri,
    context: Context,
    private val taskCallBack: TaskCallBack,
    private val filename: String
) : AsyncTask<Uri?, Int?, String?>() {

    private val weakReferenceContext: WeakReference<Context> = WeakReference(context)
    private var folder: File? = null
    private var returnCursor: Cursor? = null
    private var inputStream: InputStream? = null
    private var extension: String? = null
    private var errorReason: String? = ""

    override fun onPreExecute() {
        taskCallBack.onPreExecute()
        val context = weakReferenceContext.get()
        if (context != null) {
            folder = context.getExternalFilesDir("Temp")
            returnCursor = context.contentResolver.query(uri, null, null, null, null)
            val mime = MimeTypeMap.getSingleton()
            extension = mime.getExtensionFromMimeType(context.contentResolver.getType(uri))
            try {
                inputStream = context.contentResolver.openInputStream(uri)
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }
        }
    }

    override fun onProgressUpdate(vararg values: Int?) {
        super.onProgressUpdate(*values)
        val post = values.firstOrNull()
        taskCallBack.onProgressUpdate(post)
    }

    override fun doInBackground(vararg params: Uri?): String? {
        var file: File? = null
        var size = -1
        try {
            returnCursor.use { cursor ->
                if (cursor?.moveToFirst() == true) {
                    when (uri.scheme) {
                        "content" -> {
                            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                            size = cursor.getLong(sizeIndex).toInt()
                        }
                        "file" -> {
                            val ff = File(uri.path)
                            size = ff.length().toInt()
                        }
                    }
                }
            }
            file = if (extension == null) {
                File(folder.toString() + "/" + filename)
            } else {
                File(folder.toString() + "/" + filename + "." + extension)
            }
            val bis = BufferedInputStream(inputStream)
            val fos = FileOutputStream(file)
            val data = ByteArray(1024)
            var total: Long = 0
            var count: Int
            while (bis.read(data).also { count = it } != -1) {
                if (!isCancelled) {
                    total += count.toLong()
                    if (size != -1) {
                        publishProgress((total * 100 / size).toInt())
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

    override fun onPostExecute(result: String?) {
        if (result == null) {
            taskCallBack.onPostExecute(result, wasDriveFile = true, wasSuccessful = false, reason = errorReason)
        } else {
            taskCallBack.onPostExecute(result, wasDriveFile = true, wasSuccessful = true, reason = "")
        }
    }

}