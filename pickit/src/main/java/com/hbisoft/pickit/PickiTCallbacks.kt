package com.hbisoft.pickit

interface PickiTCallbacks {
    open fun PickiTonUriReturned()
    open fun PickiTonStartListener()
    open fun PickiTonProgressUpdate(progress: Int)
    open fun PickiTonCompleteListener(path: String?, wasDriveFile: Boolean, wasUnknownProvider: Boolean, wasSuccessful: Boolean, Reason: String?)
}