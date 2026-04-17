package io.github.jqssun.displayextend.job;

import android.content.Context;
import android.content.Intent;

import io.github.jqssun.displayextend.FloatingButtonService;
import io.github.jqssun.displayextend.ManagedVirtualDisplayActivity;
import io.github.jqssun.displayextend.MediaProjectionService;
import io.github.jqssun.displayextend.State;
import io.github.jqssun.displayextend.TouchpadAccessibilityService;

public class ExitAll {
    public static void execute(Context context) {
        if (State.mediaProjectionInUse != null) {
            State.mediaProjectionInUse.stop();
            State.mediaProjectionInUse = null;
        }
        State.setMediaProjection(null);
        context.stopService(new Intent(context, MediaProjectionService.class));

        context.stopService(new Intent(context, FloatingButtonService.class));

        Intent touchpadIntent = new Intent(context, TouchpadAccessibilityService.class);
        touchpadIntent.setAction(TouchpadAccessibilityService.class.getName());
        context.stopService(touchpadIntent);

        if (ManagedVirtualDisplayActivity.getInstance() != null) {
            ManagedVirtualDisplayActivity.getInstance().finish();
        }
        if (State.managedVirtualDisplay != null) {
            State.managedVirtualDisplay.release();
            State.managedVirtualDisplay = null;
        }
        if (State.mirrorVirtualDisplay != null) {
            State.mirrorVirtualDisplay.release();
            State.mirrorVirtualDisplay = null;
        }
        State.currentActivity.get().finish();
        
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
    }
}
