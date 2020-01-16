package com.hbisoft.pickit

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import java.io.File

class PickiT(private val context: Context){
    private var isDriveFile = false
    private var isFromUnknownProvider = false
    private var downloadAsyncTask: DownloadAsyncTask? = null
    private var unknownProviderCalledBefore = false

    fun getPath(request: Int, uri: Uri?, apiLevel: Int, pickiTCallback: PickiTCallback) {
        if (uri == null) {
            pickiTCallback.onCompleteListener(null,
                wasDriveFile = false,
                wasUnknownProvider = false,
                wasSuccessful = false,
                reason = "input uri is null"
            )
            return
        }
        val returnedPath: String?
        if (apiLevel >= 19) { // Drive file was selected
            if (isOneDrive(uri) || isDropBox(uri) || isGoogleDrive(uri)) {
                isDriveFile = true
                downloadFile(uri, "tempFile", pickiTCallback)
            } else {
                returnedPath = Utils.getRealPathFromURI_API19(context, uri)
                //Get the file extension
                val mime = MimeTypeMap.getSingleton()
                val subStringExtension = returnedPath.toString().substring(returnedPath.toString().lastIndexOf(".") + 1)
                val extensionFromMime = mime.getExtensionFromMimeType(context.contentResolver.getType(uri))
                // Path is null
                if (returnedPath == null || returnedPath == "") { // This can be caused by two situations
                    // 1. The file was selected from a third party app and the data column returned null (for example EZ File Explorer)
                    // Some file providers (like EZ File Explorer) will return a URI as shown below:
                    // content://es.fileexplorer.filebrowser.ezfilemanager.externalstorage.documents/document/primary%3AFolderName%2FNameOfFile.mp4
                    // When you try to read the _data column, it will return null, without trowing an exception
                    // In this case the file need to copied/created a new file in the temporary folder
                    // 2. There was an error
                    // In this case call onCompleteListener and get/provide the reason why it failed
                    //We first check if it was called before, avoiding multiple calls
                    if (!unknownProviderCalledBefore) {
                        unknownProviderCalledBefore = true
                        if (uri.scheme != null && uri.scheme == ContentResolver.SCHEME_CONTENT) { //Then we check if the _data colomn returned null
                            if (Utils.errorReason() != null && Utils.errorReason() == "dataReturnedNull") {
                                isFromUnknownProvider = true
                                //Copy the file to the temporary folder
                                downloadFile(uri, getFileName(uri), pickiTCallback)
                                return
                            }
                        }
                    }
                    //Else an error occurred, get/set the reason for the error
                    pickiTCallback.onCompleteListener(returnedPath, false, false, false, Utils.errorReason())
                } else { // This can be caused by two situations
                    // 1. The file was selected from an unknown provider (for example a file that was downloaded from a third party app)
                    // 2. getExtensionFromMimeType returned an unknown mime type for example "audio/mp4"
                    //
                    // When this is case we will copy/write the file to the temp folder, same as when a file is selected from Google Drive etc.
                    // We provide a name by getting the text after the last "/"
                    // Remember if the extension can't be found, it will not be added, but you will still be able to use the file
                    //Todo: Add checks for unknown file extensions
                    if (subStringExtension != extensionFromMime && uri.scheme != null && uri.scheme == ContentResolver.SCHEME_CONTENT) {
                        val fileName = returnedPath.substring(returnedPath.lastIndexOf("/") + 1)
                        isFromUnknownProvider = true
                        downloadFile(uri, fileName, pickiTCallback)
                        return
                    }
                    // Path can be returned, no need to make a "copy"
                    pickiTCallback.onCompleteListener(
                        returnedPath,
                        wasDriveFile = false,
                        wasUnknownProvider = false,
                        wasSuccessful = true,
                        reason = ""
                    )
                }
            }
        } else { //Todo: Test API <19
            returnedPath = Utils.getRealPathFromURIBelowAPI19(context, uri)
            pickiTCallback.onCompleteListener(
                returnedPath,
                wasDriveFile = false,
                wasUnknownProvider = false,
                wasSuccessful = true,
                reason = ""
            )
        }
    }

    /**
     * Returns the file name as a [String].
     */
    private fun getFileName(uri: Uri): String {
        val replaced = uri.toString()
            .replace("%2F", "/")
            .replace("%20", " ")
            .replace("%3A", "/")
        var name = replaced.substring(replaced.lastIndexOf("/") + 1)
        if (name.indexOf(".") > 0) {
            name = name.substring(0, name.lastIndexOf("."))
        }
        return name
    }

    /**
     * Create a new file from the Uri that was selected.
     */
    private fun downloadFile(uri: Uri, fileName: String, pickiTCallback: PickiTCallback) {
        downloadAsyncTask = DownloadAsyncTask(uri, context, object : TaskCallBack{
            //region /**PickiT [TaskCallBack]**/
            /**PickiT [TaskCallBack]*/
            override fun onPreExecute() {
                pickiTCallback.onStartListener()
            }

            override fun onProgressUpdate(progress: Int?) {
                pickiTCallback.onProgressUpdate(progress)
            }

            override fun onPostExecute(
                path: String?,
                wasDriveFile: Boolean,
                wasSuccessful: Boolean,
                reason: String?
            ) {
                unknownProviderCalledBefore = false
                if (wasSuccessful) {
                    if (isDriveFile) {
                        pickiTCallback.onCompleteListener(path,
                            wasDriveFile = true,
                            wasUnknownProvider = false,
                            wasSuccessful = true,
                            reason = ""
                        )
                    } else if (isFromUnknownProvider) {
                        pickiTCallback.onCompleteListener(path,
                            wasDriveFile = false,
                            wasUnknownProvider = true,
                            wasSuccessful = true,
                            reason = ""
                        )
                    }
                } else {
                    if (isDriveFile) {
                        pickiTCallback.onCompleteListener(path,
                            wasDriveFile = true,
                            wasUnknownProvider = false,
                            wasSuccessful = false,
                            reason = reason
                        )
                    } else if (isFromUnknownProvider) {
                        pickiTCallback.onCompleteListener(path,
                            wasDriveFile = false,
                            wasUnknownProvider = true,
                            wasSuccessful = false,
                            reason = reason
                        )
                    }
                }
            }
            //endregion
        }, fileName)
        downloadAsyncTask?.execute()
    }

    /**
     * End the "copying" of the file
     */
    fun cancelTask() {
        downloadAsyncTask?.cancel(true)
        deleteTemporaryFile()
    }

    fun wasLocalFileSelected(uri: Uri): Boolean {
        return !isDropBox(uri) && !isGoogleDrive(uri) && !isOneDrive(uri)
    }

    //region Check different providers
    private fun isDropBox(uri: Uri): Boolean {
        return uri.toString().toLowerCase().contains("content://com.dropbox.android")
    }

    private fun isGoogleDrive(uri: Uri): Boolean {
        return uri.toString().toLowerCase().contains("com.google.android.apps")
    }

    private fun isOneDrive(uri: Uri): Boolean {
        return uri.toString().toLowerCase().contains("com.microsoft.skydrive.content")
    }
    //endregion

    //region Delete the temporary folder
    fun deleteTemporaryFile() {
        val folder = context.getExternalFilesDir("Temp")
        if (folder != null) {
            if (deleteDirectory(folder)) {
                Log.i("PickiT ", " deleteDirectory was called")
            }
        }
    }

    private fun deleteDirectory(path: File): Boolean {
        if (path.exists()) {
            val files = path.listFiles() ?: return false
            for (file in files) {
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
        return path.delete()
    }
    //endregion

}