package io.github.jqssun.displayextend.job;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Display;

import android.content.Intent;

import io.github.jqssun.displayextend.Pref;
import io.github.jqssun.displayextend.PureBlackActivity;
import io.github.jqssun.displayextend.R;
import io.github.jqssun.displayextend.State;
import io.github.jqssun.displayextend.shizuku.ServiceUtils;
import io.github.jqssun.displayextend.shizuku.ShizukuUtils;

public class DisplayMonitor {
    private static boolean registered = false;
    public static void init(DisplayManager displayManager) {
        if (registered) {
            return;
        }
        registered = true;
        for (Display display : displayManager.getDisplays()) {
            handleNewDisplay(display);
        }
        displayManager.registerDisplayListener(new DisplayManager.DisplayListener() {
            @Override
            public void onDisplayAdded(int displayId) {
                State.log("display added, displayId: " + displayId);
                Display display = displayManager.getDisplay(displayId);
                if (display != null) {
                    handleNewDisplay(display);
                }
            }

            @Override
            public void onDisplayRemoved(int displayId) {
                State.log("display removed, displayId: " + displayId);
                if (State.floatingButtonService != null) {
                    State.floatingButtonService.onDisplayRemoved(displayId);
                }
                Context context = State.currentActivity.get();
                if (context != null) {
                    State.reconcileLastSingleAppDisplay(context);
                    State.refreshUI();
                }
            }

            @Override
            public void onDisplayChanged(int displayId) {
                if (State.floatingButtonService != null) {
                    State.floatingButtonService.onDisplayChanged(displayId);
                }
            }
        }, null);
    }

    private static void handleNewDisplay(Display display) {
        if (display.getDisplayId() == Display.DEFAULT_DISPLAY) {
            return;
        }
        Context context = State.currentActivity.get();
        if (context == null) {
            return;
        }
        handleAutoScreenOff(context);
        handleAutoOpenLastApp(context, display);
    }

    private static void handleAutoScreenOff(Context context) {
        if (!Pref.getAutoScreenOff()) return;
        Intent intent = new Intent(context, PureBlackActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    private static void handleAutoOpenLastApp(Context context, Display display) {
        boolean autoManagedVirtualDisplay = Pref.getAutoManagedVirtualDisplay(display.getName());
        if (ShizukuUtils.hasPermission() && (autoManagedVirtualDisplay
                || display.getDisplayId() == State.managedVirtualDisplayHostDisplayId)) {
            new Handler().postDelayed(() -> {
                DisplayMetrics metrics = new DisplayMetrics();
                display.getMetrics(metrics);
                State.startNewJob(new PresentManagedVirtualDisplay(display,
                        new VirtualDisplayArgs(
                                context.getString(R.string.managed_virtual_display_name),
                                display.getWidth(),
                                display.getHeight(),
                                (int) display.getRefreshRate(),
                                metrics.densityDpi,
                                Pref.getFollowAppRotation())));
            }, 500);
            return;
        }
        boolean autoOpen = Pref.getAutoOpenLastApp(display.getName());
        if (!autoOpen) {
            return;
        }
        String lastPackageName = Pref.getLastPackageName();
        if (lastPackageName == null) {
            return;
        }
        State.log("auto-launching " + lastPackageName + " on display " + display.getName());
        new Handler().postDelayed(() -> {
            ServiceUtils.launchPackage(context, lastPackageName, display.getDisplayId());
        }, 500);
    }
}
