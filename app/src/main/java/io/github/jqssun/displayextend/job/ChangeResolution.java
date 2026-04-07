package io.github.jqssun.displayextend.job;

import android.app.Activity;
import android.graphics.Point;
import android.view.IWindowManager;

import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import io.github.jqssun.displayextend.R;
import io.github.jqssun.displayextend.State;
import io.github.jqssun.displayextend.shizuku.ServiceUtils;

public class ChangeResolution implements Job {
    private final AcquireShizuku acquireShizuku = new AcquireShizuku();
    private final int displayId;
    private final int width;
    private final int height;
    private int oldWidth;
    private int oldHeight;
    private volatile boolean confirmed = false;
    private boolean sleep1 = false;
    private boolean requestedConfirmation = false;
    private Thread timeoutThread;
    private AlertDialog dialog;

    public ChangeResolution(int displayId, int width, int height) {
        this.displayId = displayId;
        this.width = width;
        this.height = height;
    }
    @Override
    public void start() throws YieldException {
        acquireShizuku.start();
        if (!acquireShizuku.acquired) {
            return;
        }
        if (!sleep1) {
            sleep1 = true;
            IWindowManager wm = ServiceUtils.getWindowManager();
            Point baseSize = new Point();
            wm.getBaseDisplaySize(displayId, baseSize);
            oldWidth = baseSize.x;
            oldHeight = baseSize.y;
            wm.setForcedDisplaySize(displayId, width, height);
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    Activity activity = State.currentActivity.get();
                    if (activity != null) {
                        activity.runOnUiThread(() -> {
                            State.resumeJob();
                        });
                    }
                } catch (InterruptedException e) {
                    // ignore
                }
            }).start();
            throw new YieldException("waiting for confirmation dialog");
        }
        if (!requestedConfirmation) {
            requestedConfirmation = true;
            dialog = new MaterialAlertDialogBuilder(State.currentActivity.get())
            .setTitle(State.currentActivity.get().getString(R.string.edit_resolution))
            .setMessage(State.currentActivity.get().getString(R.string.confirm_resolution_format, oldWidth, oldHeight, width, height))
            .setPositiveButton(State.currentActivity.get().getString(R.string.ok), (dialog, which) -> {
                ChangeResolution.this.confirmed = true;
                State.resumeJob();
            })
            .setNegativeButton(State.currentActivity.get().getString(R.string.cancel), (dialog, which) -> {
                State.resumeJob();
            })
            .show();
            timeoutThread = new Thread(() -> {
                try {
                    Thread.sleep(5000);
                    Activity activity = State.currentActivity.get();
                    if (activity != null) {
                        activity.runOnUiThread(() -> {
                            if (dialog != null && dialog.isShowing()) {
                                dialog.dismiss();
                            }
                            State.resumeJob();
                        });
                    }
                } catch (InterruptedException e) {
                    // ignore
                }
            });
            timeoutThread.start();
            throw new YieldException("waiting for resolution confirmation");
        }
        timeoutThread.interrupt();
        if (!confirmed) {
            State.log("restoring resolution");
            dialog.dismiss();
            ServiceUtils.getWindowManager().setForcedDisplaySize(displayId, oldWidth, oldHeight);
        }
    }

}
