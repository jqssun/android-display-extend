package io.github.jqssun.displayextend.job;

import io.github.jqssun.displayextend.State;
import io.github.jqssun.displayextend.shizuku.PermissionManager;

import android.content.ContentResolver;
import android.provider.Settings;
import android.view.Display;

public class TermuxDisablePhantomProcess implements Job {
    private final AcquireShizuku acquireShizuku = new AcquireShizuku();
    private final int displayId;

    public TermuxDisablePhantomProcess(int displayId) {
        this.displayId = displayId;
    }

    @Override
    public void start() throws YieldException {
        acquireShizuku.start();
        if (!acquireShizuku.acquired) {
            return;
        }
        if (displayId != Display.DEFAULT_DISPLAY) {
            InputRouting.bindAllExternalInputToDisplay(displayId);
        }
        disablePhantomProcsMonitoring();
    }

    private void disablePhantomProcsMonitoring() {
        if (PermissionManager.grant("android.permission.WRITE_SECURE_SETTINGS")) {
            try {
                ContentResolver contentResolver = State.currentActivity.get().getContentResolver();
                if (Settings.Global.getInt(contentResolver, "settings_enable_monitor_phantom_procs", 0) == 0) {
                    return;
                }
                if (State.userService != null) {
                    State.userService.executeCommand("/system/bin/device_config set_sync_disabled_for_tests persistent");
                    State.userService.executeCommand("/system/bin/device_config put activity_manager max_phantom_processes 2147483647");
                }
                Settings.Global.putInt(contentResolver,
                        "settings_enable_monitor_phantom_procs", 0);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }
}
