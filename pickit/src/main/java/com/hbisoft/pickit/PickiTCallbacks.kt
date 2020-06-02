package com.hbisoft.pickit

interface PickiTCallbacks {
    fun PickiTonUriReturned()
    fun PickiTonStartListener()
    fun PickiTonProgressUpdate(progress: Int)
    fun PickiTonCompleteListener(path: String?, wasDriveFile: Boolean, wasUnknownProvider: Boolean, wasSuccessful: Boolean, Reason: String?)
}