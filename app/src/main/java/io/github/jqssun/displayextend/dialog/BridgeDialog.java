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
import io.github.jqssun.displayextend.job.ProjectViaBridge;
import io.github.jqssun.displayextend.job.VirtualDisplayArgs;
import io.github.jqssun.displayextend.shizuku.ServiceUtils;

public class BridgeDialog {
    public static void show(Context context, Display display, int displayId) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_bridge, null);
        
        MaterialCheckBox rotatesWithContentCheckbox = dialogView.findViewById(R.id.rotatesWithContentCheckbox);
        MaterialCheckBox skipScreenCaptureCheckbox = dialogView.findViewById(R.id.skipScreenCaptureCheckbox);
        MaterialCheckBox autoBridgeCheckbox = dialogView.findViewById(R.id.autoBridgeCheckbox);
        
        rotatesWithContentCheckbox.setChecked(Pref.getRotatesWithContent());
        skipScreenCaptureCheckbox.setChecked(Pref.getSkipScreenCapturePermission());
        autoBridgeCheckbox.setChecked(Pref.getAutoBridge(display.getName()));

        Point initialSize = new Point();
        ServiceUtils.getWindowManager().getInitialDisplaySize(displayId, initialSize);
        new MaterialAlertDialogBuilder(context)
                .setTitle(context.getString(R.string.bridge_settings))
                .setView(dialogView)
                .setPositiveButton(context.getString(R.string.ok), (dialog, which) -> {
                    Pref.setRotatesWithContent(rotatesWithContentCheckbox.isChecked());
                    Pref.setSkipScreenCapturePermission(skipScreenCaptureCheckbox.isChecked());
                    boolean autoBridge = autoBridgeCheckbox.isChecked();
                    Pref.setAutoBridge(display.getName(), autoBridge);
                    if (autoBridge) {
                        Pref.setAutoOpenLastApp(display.getName(), false);
                    }

                    DisplayMetrics metrics = new DisplayMetrics();
                    display.getMetrics(metrics);
                    State.startNewJob(new ProjectViaBridge(display, new VirtualDisplayArgs(
                            context.getString(R.string.bridge_display),
                            initialSize.x,
                            initialSize.y,
                            (int) display.getRefreshRate(),
                            metrics.densityDpi,
                            Pref.getRotatesWithContent())));
                })
                .setNegativeButton(context.getString(R.string.cancel), null)
                .show();
    }
} 