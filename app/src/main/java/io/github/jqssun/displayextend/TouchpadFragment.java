package io.github.jqssun.displayextend;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.view.Display;
import android.view.IWindowManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

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
        Spinner imePolicySpinner = view.findViewById(R.id.ime_policy_spinner);
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
            _setupImePolicy(imePolicyRow, imePolicySpinner, displayId);
        } else {
            statusIcon.setImageResource(R.drawable.ic_error);
            statusTitle.setText(R.string.touchpad_no_cast);
            statusDetail.setText(R.string.touchpad_no_cast_message);
            imePolicyRow.setVisibility(View.VISIBLE);
            _setupImePolicySpinner(imePolicySpinner);
            imePolicySpinner.setSelection(DisplayImePolicyCompat.indexOf(DisplayImePolicyCompat.FALLBACK_DISPLAY), false);
        }

        float disabledAlpha = 0.38f;
        sensitivityLabel.setAlpha(hasCast ? 1f : disabledAlpha);
        sensitivitySlider.setEnabled(hasCast);
        imePolicyRow.setAlpha(hasCast ? 1f : disabledAlpha);
        imePolicySpinner.setEnabled(hasCast);
        startBtn.setEnabled(hasCast);

        startBtn.setOnClickListener(v -> TouchpadActivity.startTouchpad(getContext(), State.lastSingleAppDisplay, false));

        return view;
    }

    private void _setupImePolicy(View row, Spinner spinner, int displayId) {
        if (!ShizukuUtils.hasPermission()) {
            row.setVisibility(View.GONE);
            return;
        }
        try {
            IWindowManager wm = ServiceUtils.getWindowManager();
            row.setVisibility(View.VISIBLE);
            _syncImePolicySpinner(wm, spinner, displayId);
        } catch (Throwable e) {
            row.setVisibility(View.GONE);
        }
    }

    private void _setupImePolicySpinner(Spinner spinner) {
        Context context = spinner.getContext();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                context,
                android.R.layout.simple_spinner_item,
                DisplayImePolicyCompat.labels(context)
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void _syncImePolicySpinner(IWindowManager wm, Spinner spinner, int displayId) throws Throwable {
        _setupImePolicySpinner(spinner);
        int currentPolicy = wm.getDisplayImePolicy(displayId);
        spinner.setOnItemSelectedListener(null);
        spinner.setSelection(DisplayImePolicyCompat.indexOf(currentPolicy), false);
        final boolean[] initialized = {false};
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!initialized[0]) {
                    initialized[0] = true;
                    return;
                }
                _applyImePolicySelection(wm, spinner, displayId, DisplayImePolicyCompat.values()[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // no-op
            }
        });
    }

    private void _applyImePolicySelection(IWindowManager wm, Spinner spinner, int displayId, int requestedPolicy) {
        try {
            int currentPolicy = wm.getDisplayImePolicy(displayId);
            if (currentPolicy == requestedPolicy) {
                return;
            }

            wm.setDisplayImePolicy(Display.DEFAULT_DISPLAY, DisplayImePolicyCompat.LOCAL);
            wm.setDisplayImePolicy(displayId, requestedPolicy);
            State.refreshUI();
        } catch (Throwable e) {
            try {
                _syncImePolicySpinner(wm, spinner, displayId);
            } catch (Throwable ignored) {
                spinner.setOnItemSelectedListener(null);
            }
            State.log("failed to set IME policy: " + e);
            if (getContext() != null) {
                Toast.makeText(getContext(), R.string.ime_policy_unsupported, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
