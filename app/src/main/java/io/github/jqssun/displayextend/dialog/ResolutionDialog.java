package io.github.jqssun.displayextend.dialog;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import io.github.jqssun.displayextend.R;
import io.github.jqssun.displayextend.State;
import io.github.jqssun.displayextend.job.ChangeResolution;

public class ResolutionDialog {
    public static void show(Context context, int displayId, int currentWidth, int currentHeight) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_resolution, null);
        EditText widthInput = dialogView.findViewById(R.id.width_input);
        EditText heightInput = dialogView.findViewById(R.id.height_input);
        
        widthInput.setText(String.valueOf(currentWidth));
        heightInput.setText(String.valueOf(currentHeight));

        new MaterialAlertDialogBuilder(context)
                .setTitle(context.getString(R.string.edit_resolution))
                .setView(dialogView)
                .setPositiveButton(context.getString(R.string.ok), (dialog, which) -> {
                    try {
                        int newWidth = Integer.parseInt(widthInput.getText().toString());
                        int newHeight = Integer.parseInt(heightInput.getText().toString());
                        
                        if (newWidth <= 0 || newHeight <= 0) {
                            Toast.makeText(context, context.getString(R.string.invalid_number), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        State.startNewJob(new ChangeResolution(displayId, newWidth, newHeight));
                    } catch (NumberFormatException e) {
                        Toast.makeText(context, context.getString(R.string.invalid_number), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(context.getString(R.string.cancel), null)
                .show();
    }
} 