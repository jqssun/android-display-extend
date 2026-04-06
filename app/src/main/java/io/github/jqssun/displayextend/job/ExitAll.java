package io.github.jqssun.displayextend.job;

import android.content.Context;
import android.content.Intent;

import io.github.jqssun.displayextend.BridgeActivity;
import io.github.jqssun.displayextend.FloatingButtonService;
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

        if (BridgeActivity.getInstance() != null) {
            BridgeActivity.getInstance().finish();
        }
        if (State.bridgeVirtualDisplay != null) {
            State.bridgeVirtualDisplay.release();
            State.bridgeVirtualDisplay = null;
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
