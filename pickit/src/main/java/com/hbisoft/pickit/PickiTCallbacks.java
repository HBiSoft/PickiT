package com.hbisoft.pickit;

public interface PickiTCallbacks {
    void PickiTonStartListener();
    void PickiTonProgressUpdate(int progress);
    void PickiTonCompleteListener(String path, boolean wasDriveFile);
}
