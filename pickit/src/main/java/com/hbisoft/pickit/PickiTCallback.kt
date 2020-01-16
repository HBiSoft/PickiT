package com.hbisoft.pickit

interface PickiTCallback {
    fun onStartListener()
    fun onProgressUpdate(progress: Int?)
    fun onCompleteListener(
        path: String?,
        wasDriveFile: Boolean,
        wasUnknownProvider: Boolean,
        wasSuccessful: Boolean,
        reason: String?
    )
}