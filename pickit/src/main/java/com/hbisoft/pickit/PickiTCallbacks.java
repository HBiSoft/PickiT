package com.hbisoft.pickit;

import androidx.annotation.Nullable;

public interface PickiTCallbacks {
    void PickiTonUriReturned();
    void PickiTonStartListener();
    void PickiTonProgressUpdate(int progress);
    void PickiTonCompleteListener(@Nullable String path, boolean wasDriveFile, boolean wasUnknownProvider, boolean wasSuccessful, String Reason);
}
