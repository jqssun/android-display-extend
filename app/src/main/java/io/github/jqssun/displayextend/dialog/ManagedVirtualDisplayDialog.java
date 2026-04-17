package io.github.jqssun.displayextend.dialog;

import android.content.Context;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import io.github.jqssun.displayextend.Pref;
import io.github.jqssun.displayextend.R;
import io.github.jqssun.displayextend.State;
import io.github.jqssun.displayextend.job.PresentManagedVirtualDisplay;
import io.github.jqssun.displayextend.job.VirtualDisplayArgs;
import io.github.jqssun.displayextend.shizuku.ServiceUtils;

public class ManagedVirtualDisplayDialog {
    public static void show(Context context, Display display, int displayId) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_managed_virtual_display, null);
        
        MaterialCheckBox followAppRotationCheckbox = dialogView.findViewById(R.id.followAppRotationCheckbox);
        MaterialCheckBox skipScreenCaptureCheckbox = dialogView.findViewById(R.id.skipScreenCaptureCheckbox);
        MaterialCheckBox autoManagedVirtualDisplayCheckbox =
                dialogView.findViewById(R.id.autoManagedVirtualDisplayCheckbox);
        
        followAppRotationCheckbox.setChecked(Pref.getFollowAppRotation());
        skipScreenCaptureCheckbox.setChecked(Pref.getSkipScreenCapturePermission());
        autoManagedVirtualDisplayCheckbox.setChecked(Pref.getAutoManagedVirtualDisplay(display.getName()));

        Point initialSize = new Point();
        ServiceUtils.getWindowManager().getInitialDisplaySize(displayId, initialSize);
        new MaterialAlertDialogBuilder(context)
                .setTitle(context.getString(R.string.managed_virtual_display_settings))
                .setView(dialogView)
                .setPositiveButton(context.getString(R.string.ok), (dialog, which) -> {
                    Pref.setFollowAppRotation(followAppRotationCheckbox.isChecked());
                    Pref.setSkipScreenCapturePermission(skipScreenCaptureCheckbox.isChecked());
                    boolean autoManagedVirtualDisplay = autoManagedVirtualDisplayCheckbox.isChecked();
                    Pref.setAutoManagedVirtualDisplay(display.getName(), autoManagedVirtualDisplay);
                    if (autoManagedVirtualDisplay) {
                        Pref.setAutoOpenLastApp(display.getName(), false);
                    }

                    DisplayMetrics metrics = new DisplayMetrics();
                    display.getMetrics(metrics);
                    State.startNewJob(new PresentManagedVirtualDisplay(display, new VirtualDisplayArgs(
                            context.getString(R.string.managed_virtual_display_name),
                            initialSize.x,
                            initialSize.y,
                            (int) display.getRefreshRate(),
                            metrics.densityDpi,
                            Pref.getFollowAppRotation())));
                })
                .setNegativeButton(context.getString(R.string.cancel), null)
                .show();
    }
}
