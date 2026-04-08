package io.github.jqssun.displayextend.dialog;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import io.github.jqssun.displayextend.R;
import io.github.jqssun.displayextend.State;
import io.github.jqssun.displayextend.job.ChangeDPI;

public class DpiDialog {
    public static void show(Context context, int displayId, int currentDpi) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_dpi, null);
        TextInputEditText dpiInput = dialogView.findViewById(R.id.dpi_input);
        
        dpiInput.setText(String.valueOf(currentDpi));

        new MaterialAlertDialogBuilder(context)
                .setTitle(context.getString(R.string.edit_dpi))
                .setView(dialogView)
                .setPositiveButton(context.getString(R.string.ok), (dialog, which) -> {
                    try {
                        int newDpi = Integer.parseInt(dpiInput.getText().toString());
                        
                        if (newDpi <= 0) {
                            Toast.makeText(context, context.getString(R.string.invalid_number), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        State.startNewJob(new ChangeDPI(displayId, newDpi, currentDpi));
                    } catch (NumberFormatException e) {
                        Toast.makeText(context, context.getString(R.string.invalid_number), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(context.getString(R.string.cancel), null)
                .show();
    }
} 