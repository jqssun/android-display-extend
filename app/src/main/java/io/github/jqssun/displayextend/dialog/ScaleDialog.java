package io.github.jqssun.displayextend.dialog;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import io.github.jqssun.displayextend.R;
import io.github.jqssun.displayextend.State;
import io.github.jqssun.displayextend.job.ChangeResolution;

public class ScaleDialog {
    private static final int MIN_PERCENT = 50;
    private static final int MAX_PERCENT = 400;

    public static void show(Context context, int displayId, int nativeWidth, int nativeHeight, int currentPercent) {
        if (nativeWidth <= 0 || nativeHeight <= 0) return;

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_scale, null);
        TextInputEditText scaleInput = dialogView.findViewById(R.id.scale_input);
        scaleInput.setText(String.valueOf(currentPercent));

        new MaterialAlertDialogBuilder(context)
                .setTitle(context.getString(R.string.edit_scale))
                .setView(dialogView)
                .setPositiveButton(context.getString(R.string.ok), (dialog, which) -> {
                    try {
                        int percent = Integer.parseInt(scaleInput.getText().toString());
                        if (percent < MIN_PERCENT || percent > MAX_PERCENT) {
                            Toast.makeText(context, context.getString(R.string.invalid_number), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        int newWidth = Math.round(nativeWidth * 100f / percent);
                        int newHeight = Math.round(nativeHeight * 100f / percent);
                        State.startNewJob(new ChangeResolution(displayId, newWidth, newHeight));
                    } catch (NumberFormatException e) {
                        Toast.makeText(context, context.getString(R.string.invalid_number), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(context.getString(R.string.cancel), null)
                .show();
    }
}
