package com.hbisoft.pickit

internal interface CallBackTask {
    open fun PickiTonUriReturned()
    open fun PickiTonPreExecute()
    open fun PickiTonProgressUpdate(progress: Int)
    open fun PickiTonPostExecute(path: String?, wasDriveFile: Boolean, wasSuccessful: Boolean, reason: String?)
}