package io.github.jqssun.displayextend.shizuku;

import android.os.ParcelFileDescriptor;

interface IUserService {

    void destroy() = 16777114; // destroy method defined by Shizuku server

    void exit() = 1; // exit method defined by user

    void fetchLogs(in ParcelFileDescriptor sink) = 2;

    String dumpInput() = 3;

    void setScreenPower(int powerMode) = 4;

    void startListenVolumeKey() = 5;

    void stopListenVolumeKey() = 6;
}
