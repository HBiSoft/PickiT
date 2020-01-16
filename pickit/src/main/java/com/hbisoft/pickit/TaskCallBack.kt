package com.hbisoft.pickit

interface TaskCallBack {
    fun onPreExecute(taskId: Int)
    fun onProgressUpdate(taskId: Int, progress: Int?)
    fun onPostExecute(
        taskId: Int,
        path: String?,
        status: PickiTStatus,
        reason: String? = null
    )
}