package io.github.jqssun.displayextend.job;

import io.github.jqssun.displayextend.R;
import io.github.jqssun.displayextend.State;
import io.github.jqssun.displayextend.shizuku.ServiceUtils;
import android.app.Activity;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.view.IWindowManager;

public class ChangeDPI implements Job {
    private final AcquireShizuku acquireShizuku = new AcquireShizuku();
    public final int displayId;
    public final int dpi;
    private int oldDpi;
    private volatile boolean confirmed = false;
    private boolean sleep1 = false;
    private boolean requestedConfirmation = false;
    private Thread timeoutThread;
    private AlertDialog dialog;

    public ChangeDPI(int displayId, int dpi, int oldDpi) {
        this.displayId = displayId;
        this.dpi = dpi;
        this.oldDpi = oldDpi;
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
            wm.setForcedDisplayDensityForUser(displayId, dpi, 0);
            
            new Thread(() -> {
                try {
                    Thread.sleep(500);
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
                .setTitle(State.currentActivity.get().getString(R.string.edit_dpi))
                .setMessage(State.currentActivity.get().getString(R.string.confirm_dpi_format, oldDpi, dpi))
                .setPositiveButton(State.currentActivity.get().getString(R.string.ok), (dialog, which) -> {
                    ChangeDPI.this.confirmed = true;
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
            throw new YieldException("waiting for DPI confirmation");
        }

        timeoutThread.interrupt();
        if (!confirmed) {
            State.log("restoring DPI");
            dialog.dismiss();
            ServiceUtils.getWindowManager().setForcedDisplayDensityForUser(displayId, oldDpi, 0);
        }
    }
}
