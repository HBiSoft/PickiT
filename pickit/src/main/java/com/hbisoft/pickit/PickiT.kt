package com.hbisoft.pickit

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import java.io.File
import java.util.*

class PickiT(private val context: Context, private val pickiTCallbacks: PickiTCallbacks, private val mActivity: Activity) : CallBackTask {
    private var isDriveFile = false
    private var isFromUnknownProvider = false
    private lateinit var asyncTask: DownloadAsyncTask
    private var unknownProviderCalledBefore = false

    fun getPath(uri: Uri?, APILevel: Int) {
        val returnedPath: String?
        if (APILevel >= 19) {
            // Drive file was selected
            if (isOneDrive(uri) || isDropBox(uri) || isGoogleDrive(uri)) {
                isDriveFile = true
                downloadFile(uri)
            } else {
                returnedPath = Utils.getRealPathFromURI_API19(context, uri)

                //Get the file extension
                val mime = MimeTypeMap.getSingleton()
                val subStringExtension = returnedPath.toString().substring(returnedPath.toString().lastIndexOf(".") + 1)
                val extensionFromMime = mime.getExtensionFromMimeType(context.contentResolver.getType(uri!!))

                // Path is null
                if (returnedPath == null || returnedPath == "") {
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
                        unknownProviderCalledBefore = true
                        uri.scheme?.let {
                            if (it == ContentResolver.SCHEME_CONTENT) {
                                //Then we check if the _data colomn returned null
                                Utils.errorReason()?.let { error ->
                                    if (error == "dataReturnedNull") {
                                        isFromUnknownProvider = true
                                        //Copy the file to the temporary folder
                                        downloadFile(uri)
                                        return
                                    } else if (error.contains("column '_data' does not exist")) {
                                        isFromUnknownProvider = true
                                        //Copy the file to the temporary folder
                                        downloadFile(uri)
                                        return
                                    }
                                }
                            }
                        }
                    }
                    //Else an error occurred, get/set the reason for the error
                    pickiTCallbacks.PickiTonCompleteListener(
                            returnedPath,
                            wasDriveFile = false,
                            wasUnknownProvider = false,
                            wasSuccessful = false,
                            Reason = Utils.errorReason()
                                    ?: "")
                } else {
                    // This can be caused by two situations
                    // 1. The file was selected from an unknown provider (for example a file that was downloaded from a third party app)
                    // 2. getExtensionFromMimeType returned an unknown mime type for example "audio/mp4"
                    //
                    // When this is case we will copy/write the file to the temp folder, same as when a file is selected from Google Drive etc.
                    // We provide a name by getting the text after the last "/"
                    // Remember if the extension can't be found, it will not be added, but you will still be able to use the file
                    //Todo: Add checks for unknown file extensions
                    uri.scheme?.let {
                        if (subStringExtension != extensionFromMime && it == ContentResolver.SCHEME_CONTENT) {
                            isFromUnknownProvider = true
                            downloadFile(uri)
                            return
                        }
                    }
                    // Path can be returned, no need to make a "copy"
                    pickiTCallbacks.PickiTonCompleteListener(
                            returnedPath,
                            wasDriveFile = false,
                            wasUnknownProvider = false,
                            wasSuccessful = true)
                }
            }
        } else {
            //Todo: Test API <19
            returnedPath = Utils.getRealPathFromURI_BelowAPI19(context, uri)
            pickiTCallbacks.PickiTonCompleteListener(
                    returnedPath,
                    wasDriveFile = false,
                    wasUnknownProvider = false,
                    wasSuccessful = true)
        }
    }

    // Create a new file from the Uri that was selected
    private fun downloadFile(uri: Uri?) {
        asyncTask = DownloadAsyncTask(uri, context, this, mActivity)
        asyncTask.execute()
    }

    // End the "copying" of the file
    fun cancelTask() {
        asyncTask.cancel(true)
        deleteTemporaryFile()
    }

    fun wasLocalFileSelected(uri: Uri?): Boolean {
        return !isDropBox(uri) && !isGoogleDrive(uri) && !isOneDrive(uri)
    }

    // Check different providers
    private fun isDropBox(uri: Uri?): Boolean {
        return uri.toString().toLowerCase(Locale.ROOT).contains("content://com.dropbox.android")
    }

    private fun isGoogleDrive(uri: Uri?): Boolean {
        return uri.toString().toLowerCase(Locale.ROOT).contains("com.google.android.apps")
    }

    private fun isOneDrive(uri: Uri?): Boolean {
        return uri.toString().toLowerCase(Locale.ROOT).contains("com.microsoft.skydrive.content")
    }

    // PickiT callback Listeners
    override fun PickiTonUriReturned() {
        pickiTCallbacks.PickiTonUriReturned()
    }

    override fun PickiTonPreExecute() {
        pickiTCallbacks.PickiTonStartListener()
    }

    override fun PickiTonProgressUpdate(progress: Int) {
        pickiTCallbacks.PickiTonProgressUpdate(progress)
    }

    override fun PickiTonPostExecute(path: String?, wasDriveFile: Boolean, wasSuccessful: Boolean, reason: String) {
        unknownProviderCalledBefore = false
        when {
            wasSuccessful -> {
                when {
                    isDriveFile ->
                        pickiTCallbacks.PickiTonCompleteListener(path, wasDriveFile = true, wasUnknownProvider = false, wasSuccessful = true)

                    isFromUnknownProvider ->
                        pickiTCallbacks.PickiTonCompleteListener(path, wasDriveFile = false, wasUnknownProvider = true, wasSuccessful = true)

                }
            }
            else -> {
                when {
                    isDriveFile ->
                        pickiTCallbacks.PickiTonCompleteListener(path, wasDriveFile = true, wasUnknownProvider = false, wasSuccessful = false, Reason = reason)

                    isFromUnknownProvider ->
                        pickiTCallbacks.PickiTonCompleteListener(path, wasDriveFile = false, wasUnknownProvider = true, wasSuccessful = false, Reason = reason)
                }
            }
        }
    }

    // Delete the temporary folder
    fun deleteTemporaryFile() {
        val folder = context.getExternalFilesDir("Temp")
        folder?.let {
            if (deleteDirectory(it)) Log.i("PickiT ", " deleteDirectory was called")
        }
    }

    private fun deleteDirectory(path: File?): Boolean {
        path?.let {
            if (it.exists()) {
                val files = it.listFiles() ?: return false

                files.forEach { file ->
                    if (file.isDirectory) {
                        deleteDirectory(file)
                    } else {
                        val wasSuccessful = file.delete()
                        if (wasSuccessful) {
                            Log.i("Deleted ", "successfully")
                        }
                    }
                }
            }
            return it.delete()
        } ?: return false
    }
}