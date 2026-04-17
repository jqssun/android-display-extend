package io.github.jqssun.displayextend.job;

import android.view.Display;

import io.github.jqssun.displayextend.State;
import io.github.jqssun.displayextend.shizuku.ServiceUtils;

public class ApplyDisplayMode implements Job {
    private final AcquireShizuku acquireShizuku = new AcquireShizuku();
    private final int displayId;
    private final Display.Mode mode;

    public ApplyDisplayMode(int displayId, Display.Mode mode) {
        this.displayId = displayId;
        this.mode = mode;
    }

    @Override
    public void start() throws YieldException {
        acquireShizuku.start();
        if (!acquireShizuku.acquired) return;

        ServiceUtils.setUserPreferredDisplayMode(displayId, mode, false);
        State.log("applied display mode " + mode.getModeId() + " to display " + displayId);
        State.resumeJobLater(750);
    }
}
