package com.hbisoft.pickit;

interface CallBackTask {
    void PickiTonUriReturned();
    void PickiTonPreExecute();
    void PickiTonProgressUpdate(int progress);
    void PickiTonPostExecute(String path, boolean wasDriveFile, boolean wasSuccessful, String reason);
}
