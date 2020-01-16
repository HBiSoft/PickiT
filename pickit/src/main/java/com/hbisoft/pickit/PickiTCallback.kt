package com.hbisoft.pickit

interface PickiTCallback {
    fun onStartListener(request: Int)
    fun onProgressUpdate(request: Int, progress: Int?)
    fun onCompleteListener(
        request: Int,
        path: String?,
        provider: PickiTProvider,
        status: PickiTStatus,
        reason: String? = null
    )

}