package io.github.jqssun.displayextend.job;

import android.view.IWindowManager;

import io.github.jqssun.displayextend.State;
import io.github.jqssun.displayextend.shizuku.ServiceUtils;

public class ResetDisplayConfig implements Job {
    private final static int FIXED_TO_USER_ROTATION_DEFAULT = 0;
    private final AcquireShizuku acquireShizuku = new AcquireShizuku();
    private final int displayId;

    public ResetDisplayConfig(int displayId) {
        this.displayId = displayId;
    }

    @Override
    public void start() throws YieldException {
        acquireShizuku.start();
        if (!acquireShizuku.acquired) return;

        IWindowManager wm = ServiceUtils.getWindowManager();

        try {
            wm.clearForcedDisplaySize(displayId);
        } catch (Throwable e) {
            State.log("failed to clear forced display size: " + e.getMessage());
        }

        try {
            wm.clearForcedDisplayDensityForUser(displayId, 0);
        } catch (Throwable e) {
            State.log("failed to clear forced display density: " + e.getMessage());
        }

        try {
            wm.setIgnoreOrientationRequest(displayId, false);
        } catch (Throwable e) {
            State.log("failed to setIgnoreOrientationRequest: " + e.getMessage());
        }
        try {
            wm.setFixedToUserRotation(displayId, FIXED_TO_USER_ROTATION_DEFAULT);
        } catch (Throwable e) {
            State.log("failed to setFixedToUserRotation: " + e.getMessage());
        }
        try {
            wm.thawDisplayRotation(displayId, "WindowManagerShellCommand#free");
        } catch (Error e) {
            try {
                wm.thawDisplayRotation(displayId);
            } catch (Throwable e2) {
                State.log("failed to thaw rotation: " + e2.getMessage());
            }
        }

        State.log("reset display " + displayId + " config");
    }
}
