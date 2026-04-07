package io.github.jqssun.displayextend.dialog;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import io.github.jqssun.displayextend.R;
import io.github.jqssun.displayextend.State;
import io.github.jqssun.displayextend.job.ChangeRotation;

public class RotationDialog {
    public interface OnRotationSelectedListener {
        void onRotationSelected(int rotation);
    }

    public static void show(Context context, int displayId) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_rotation, null);
        Spinner rotationSpinner = dialogView.findViewById(R.id.rotation_spinner);
        
        String[] rotationOptions = new String[]{context.getString(R.string.rotation_none), "0°", "90°", "180°", "270°"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            context,
            android.R.layout.simple_spinner_item,
            rotationOptions
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        rotationSpinner.setAdapter(adapter);

        new MaterialAlertDialogBuilder(context)
                .setTitle(context.getString(R.string.edit_rotation))
                .setView(dialogView)
                .setPositiveButton(context.getString(R.string.ok), (dialog, which) -> {
                    int position = rotationSpinner.getSelectedItemPosition();
                    int rotation;
                    switch (position) {
                        case 0: rotation = -1; break;
                        case 1: rotation = Surface.ROTATION_0; break;
                        case 2: rotation = Surface.ROTATION_90; break;
                        case 3: rotation = Surface.ROTATION_180; break;
                        case 4: rotation = Surface.ROTATION_270; break;
                        default: rotation = -1;
                    }
                    State.startNewJob(new ChangeRotation(displayId, rotation));
                })
                .setNegativeButton(context.getString(R.string.cancel), null)
                .show();
    }
} 