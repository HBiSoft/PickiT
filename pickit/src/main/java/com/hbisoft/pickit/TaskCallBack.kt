package com.hbisoft.pickit

interface TaskCallBack {
    fun onPreExecute()
    fun onProgressUpdate(progress: Int?)
    fun onPostExecute(
        path: String?,
        wasDriveFile: Boolean,
        wasSuccessful: Boolean,
        reason: String?
    )
}