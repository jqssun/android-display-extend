package io.github.jqssun.displayextend.job;

import io.github.jqssun.displayextend.State;
import io.github.jqssun.displayextend.shizuku.ShizukuUtils;

import rikka.shizuku.Shizuku;

public class AcquireShizuku implements Job {
    public static final int SHIZUKU_PERMISSION_REQUEST_CODE = 1001;
    private boolean hasRequestedPermission;
    public boolean acquired = false;

    @Override
    public void start() throws YieldException {
        if (!ShizukuUtils.hasShizukuStarted()) {
            return;
        }
        if (ShizukuUtils.hasPermission()) {
            State.log("Shizuku permission already granted");
            acquired = true;
        } else {
            if (hasRequestedPermission) {
                State.log("failed to acquire Shizuku permission");
                return;
            }
            hasRequestedPermission = true;
            Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE);
            throw new YieldException("waiting for Shizuku permission");
        }
    }
}
