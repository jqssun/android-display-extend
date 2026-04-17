package io.github.jqssun.displayextend;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.view.Display;
import android.view.IWindowManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;

import io.github.jqssun.displayextend.shizuku.ServiceUtils;
import io.github.jqssun.displayextend.shizuku.ShizukuUtils;

public class TouchpadFragment extends Fragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEnterTransition(new com.google.android.material.transition.MaterialSharedAxis(com.google.android.material.transition.MaterialSharedAxis.X, true));
        setReturnTransition(new com.google.android.material.transition.MaterialSharedAxis(com.google.android.material.transition.MaterialSharedAxis.X, false));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_touchpad, container, false);

        ImageView statusIcon = view.findViewById(R.id.statusIcon);
        TextView statusTitle = view.findViewById(R.id.statusTitle);
        TextView statusDetail = view.findViewById(R.id.statusDetail);
        View imePolicyRow = view.findViewById(R.id.ime_policy_row);
        MaterialSwitch imePolicySwitch = view.findViewById(R.id.ime_policy_switch);
        View sensitivityLabel = view.findViewById(R.id.sensitivityLabel);
        Slider sensitivitySlider = view.findViewById(R.id.sensitivitySlider);
        View startBtn = view.findViewById(R.id.startTouchpadBtn);

        sensitivitySlider.setValue(Math.max(0.5f, Math.min(3.0f, Pref.getTouchpadSensitivity())));
        sensitivitySlider.addOnChangeListener((s, value, fromUser) -> {
            if (fromUser) Pref.setTouchpadSensitivity(value);
        });

        int displayId = State.lastSingleAppDisplay;
        boolean hasCast = displayId > 0;

        if (hasCast) {
            DisplayManager dm = (DisplayManager) requireContext().getSystemService(Context.DISPLAY_SERVICE);
            Display display = dm.getDisplay(displayId);
            statusIcon.setImageResource(R.drawable.ic_check_circle);
            statusTitle.setText(getString(R.string.display_id_format, displayId,
                    display != null ? display.getWidth() : 0,
                    display != null ? display.getHeight() : 0));
            statusDetail.setText(display != null ? display.getName() : String.valueOf(displayId));
            _setupImePolicy(imePolicyRow, imePolicySwitch, displayId);
        } else {
            statusIcon.setImageResource(R.drawable.ic_error);
            statusTitle.setText(R.string.touchpad_no_cast);
            statusDetail.setText(R.string.touchpad_no_cast_message);
            imePolicyRow.setVisibility(View.VISIBLE);
            imePolicySwitch.setOnCheckedChangeListener(null);
            imePolicySwitch.setChecked(false);
        }

        float disabledAlpha = 0.38f;
        sensitivityLabel.setAlpha(hasCast ? 1f : disabledAlpha);
        sensitivitySlider.setEnabled(hasCast);
        imePolicyRow.setAlpha(hasCast ? 1f : disabledAlpha);
        imePolicySwitch.setEnabled(hasCast);
        startBtn.setEnabled(hasCast);

        startBtn.setOnClickListener(v -> TouchpadActivity.startTouchpad(getContext(), State.lastSingleAppDisplay, false));

        return view;
    }

    private void _setupImePolicy(View row, MaterialSwitch toggle, int displayId) {
        if (!ShizukuUtils.hasPermission()) {
            row.setVisibility(View.GONE);
            return;
        }
        try {
            IWindowManager wm = ServiceUtils.getWindowManager();
            int imePolicy = wm.getDisplayImePolicy(displayId);
            row.setVisibility(View.VISIBLE);
            toggle.setOnCheckedChangeListener(null);
            toggle.setChecked(imePolicy == 0);
            toggle.setOnCheckedChangeListener((b, checked) -> {
                if (checked) {
                    wm.setDisplayImePolicy(Display.DEFAULT_DISPLAY, 1);
                    try {
                        wm.setDisplayImePolicy(displayId, 0);
                        State.refreshUI();
                    } catch (Throwable e) {
                        try { wm.setDisplayImePolicy(Display.DEFAULT_DISPLAY, 0); } catch (Throwable ignored) {}
                        toggle.setChecked(false);
                        State.log("failed to set IME policy: " + e);
                        if (getContext() != null) {
                            Toast.makeText(getContext(), R.string.ime_policy_unsupported, Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    wm.setDisplayImePolicy(Display.DEFAULT_DISPLAY, 0);
                    wm.setDisplayImePolicy(displayId, 1);
                    try { State.refreshUI(); } catch (Throwable e) { State.log("failed: " + e); }
                }
            });
        } catch (Throwable e) {
            row.setVisibility(View.GONE);
        }
    }
}
