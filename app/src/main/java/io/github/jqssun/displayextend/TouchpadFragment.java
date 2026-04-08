package io.github.jqssun.displayextend;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

public class TouchpadFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_touchpad, container, false);

        View noProjectionText = view.findViewById(R.id.noProjectionCard);
        TextView targetDisplayText = view.findViewById(R.id.targetDisplayText);
        View startBtn = view.findViewById(R.id.startTouchpadBtn);

        int displayId = State.lastSingleAppDisplay;
        if (displayId <= 0) {
            noProjectionText.setVisibility(View.VISIBLE);
            targetDisplayText.setVisibility(View.GONE);
            startBtn.setVisibility(View.GONE);
        } else {
            noProjectionText.setVisibility(View.GONE);
            targetDisplayText.setVisibility(View.VISIBLE);
            startBtn.setVisibility(View.VISIBLE);

            DisplayManager dm = (DisplayManager) requireContext().getSystemService(Context.DISPLAY_SERVICE);
            Display display = dm.getDisplay(displayId);
            String name = display != null ? display.getName() : String.valueOf(displayId);
            targetDisplayText.setText(getString(R.string.target_display_format, displayId, name));
        }

        startBtn.setOnClickListener(v -> {
            int id = State.lastSingleAppDisplay;
            if (id <= 0) {
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.no_projection_title))
                    .setMessage(getString(R.string.no_projection_message))
                    .setPositiveButton(getString(R.string.got_it), null)
                    .show();
            } else {
                TouchpadActivity.startTouchpad(getContext(), id, false);
            }
        });

        return view;
    }
}
