package com.hbisoft.pickit

internal interface CallBackTask {
    fun PickiTonUriReturned()
    fun PickiTonPreExecute()
    fun PickiTonProgressUpdate(progress: Int)
    fun PickiTonPostExecute(path: String?, wasDriveFile: Boolean, wasSuccessful: Boolean, reason: String="")
}