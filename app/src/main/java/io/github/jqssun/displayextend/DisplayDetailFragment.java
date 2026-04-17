package io.github.jqssun.displayextend;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.DisplayCutout;
import android.view.DisplayInfo;
import android.view.IWindowManager;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import io.github.jqssun.displayextend.dialog.ManagedVirtualDisplayDialog;
import io.github.jqssun.displayextend.dialog.DpiDialog;
import io.github.jqssun.displayextend.dialog.ResolutionDialog;
import io.github.jqssun.displayextend.dialog.RotationDialog;
import io.github.jqssun.displayextend.dialog.ScaleDialog;
import io.github.jqssun.displayextend.job.ApplyDisplayMode;
import io.github.jqssun.displayextend.job.ResetDisplayConfig;
import io.github.jqssun.displayextend.shizuku.ServiceUtils;
import io.github.jqssun.displayextend.shizuku.ShizukuUtils;
import io.github.jqssun.displayextend.shizuku.WindowingMode;

public class DisplayDetailFragment extends Fragment {
    private static final String ARG_DISPLAY_ID = "display_id";

    private LinearLayout infoTable;
    private LinearLayout shizukuTable;
    private LinearLayout shizukuCard;
    private int displayId;
    private Display display;
    private LinearLayout modesTable;
    private TextView rotationText;
    private TextView resolutionText;
    private TextView dpiText;
    private Slider dpiSlider;
    private TextView scaleText;
    private Slider scaleSlider;
    private View scaleRow;
    private int nativeWidth;
    private int nativeHeight;

    private final DisplayManager.DisplayListener displayListener = new DisplayManager.DisplayListener() {
        @Override public void onDisplayAdded(int id) {}
        @Override public void onDisplayRemoved(int id) {
            if (id == displayId) {
                _handleCurrentDisplayRemoved();
            } else if (id == State.getManagedVirtualDisplayId()
                    || id == State.managedVirtualDisplayHostDisplayId) {
                _refreshDisplayState();
            }
        }
        @Override public void onDisplayChanged(int id) {
            if (id == displayId) _refreshDisplayState();
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        Context ctx = getContext();
        if (ctx != null) {
            ((DisplayManager) ctx.getSystemService(Context.DISPLAY_SERVICE))
                    .registerDisplayListener(displayListener, null);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Context ctx = getContext();
        if (ctx != null) {
            ((DisplayManager) ctx.getSystemService(Context.DISPLAY_SERVICE))
                    .unregisterDisplayListener(displayListener);
        }
    }

    public static DisplayDetailFragment newInstance(int displayId) {
        DisplayDetailFragment fragment = new DisplayDetailFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_DISPLAY_ID, displayId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEnterTransition(new com.google.android.material.transition.MaterialSharedAxis(com.google.android.material.transition.MaterialSharedAxis.X, true));
        setReturnTransition(new com.google.android.material.transition.MaterialSharedAxis(com.google.android.material.transition.MaterialSharedAxis.X, false));
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_display_detail, container, false);
        modesTable = view.findViewById(R.id.modes_table);
        infoTable = view.findViewById(R.id.info_table);
        shizukuTable = view.findViewById(R.id.shizuku_table);
        shizukuCard = view.findViewById(R.id.shizuku_card);
        Bundle args = getArguments();
        if (args == null) return view;
        displayId = args.getInt(ARG_DISPLAY_ID);
        Context ctx = getContext();
        if (ctx == null) return view;
        DisplayManager displayManager = (DisplayManager) ctx.getSystemService(Context.DISPLAY_SERVICE);
        display = displayManager.getDisplay(displayId);

        if (display == null) {
            Activity activity = getActivity();
            if (activity != null) activity.onBackPressed();
            return view;
        }

        boolean isSecondary = displayId != Display.DEFAULT_DISPLAY;

        // --- Quick Actions ---
        View actionsHeader = view.findViewById(R.id.actions_header);
        MaterialButton launchButton = view.findViewById(R.id.start_launcher_button);
        if (!isSecondary) {
            launchButton.setVisibility(View.GONE);
        }
        launchButton.setOnClickListener(v -> LauncherActivity.start(getContext(), displayId));

        MaterialButton touchpadButton = view.findViewById(R.id.touchpad_button);
        if (isSecondary && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R || ShizukuUtils.hasPermission())) {
            touchpadButton.setVisibility(View.VISIBLE);
        }
        touchpadButton.setOnClickListener(v -> TouchpadActivity.startTouchpad(getContext(), displayId, false));

        MaterialButton resetConfigButton = view.findViewById(R.id.reset_config_button);
        if (ShizukuUtils.hasShizukuStarted()) {
            resetConfigButton.setVisibility(View.VISIBLE);
            resetConfigButton.setOnClickListener(v -> new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.reset_display_config)
                    .setMessage(R.string.reset_display_config_confirm)
                    .setPositiveButton(R.string.reset, (d, w) -> State.startNewJob(new ResetDisplayConfig(displayId)))
                    .setNegativeButton(R.string.cancel, null)
                    .show());
        }

        MaterialButton managedVirtualDisplayButton = view.findViewById(R.id.managed_virtual_display_button);
        if (displayId == State.getManagedVirtualDisplayId()
                || displayId == State.managedVirtualDisplayHostDisplayId) {
            managedVirtualDisplayButton.setVisibility(View.VISIBLE);
            managedVirtualDisplayButton.setText(getString(R.string.disable_managed_virtual_display_mode));
            managedVirtualDisplayButton.setOnClickListener(v -> {
                int managedDisplayId = State.getManagedVirtualDisplayId();
                int managedHostDisplayId = State.managedVirtualDisplayHostDisplayId;
                ManagedVirtualDisplayActivity.stopVirtualDisplay();
                if (ManagedVirtualDisplayActivity.getInstance() != null) {
                    ManagedVirtualDisplayActivity.getInstance().finish();
                }
                State.refreshUI();
                if (displayId == managedDisplayId) {
                    _handleCurrentDisplayRemoved();
                } else if (displayId == managedHostDisplayId) {
                    _refreshDisplayState();
                }
            });
        } else if (isSecondary && ShizukuUtils.hasShizukuStarted()) {
            managedVirtualDisplayButton.setVisibility(View.VISIBLE);
            managedVirtualDisplayButton.setOnClickListener(v -> _showManagedVirtualDisplayDialog());
        }

        if (actionsHeader != null) {
            boolean hasVisibleActions = launchButton.getVisibility() == View.VISIBLE
                    || touchpadButton.getVisibility() == View.VISIBLE
                    || resetConfigButton.getVisibility() == View.VISIBLE
                    || managedVirtualDisplayButton.getVisibility() == View.VISIBLE;
            actionsHeader.setVisibility(hasVisibleActions ? View.VISIBLE : View.GONE);
        }

        // --- Display Settings (only for secondary displays) ---
        View settingsHeader = view.findViewById(R.id.settings_header);
        LinearLayout settingsSection = view.findViewById(R.id.settings_section);
        if (isSecondary) {
            settingsHeader.setVisibility(View.VISIBLE);
            settingsSection.setVisibility(View.VISIBLE);

            MaterialSwitch autoOpenSwitch = view.findViewById(R.id.autoOpenLastAppSwitch);
            autoOpenSwitch.setChecked(Pref.getAutoOpenLastApp(display.getName()));
            autoOpenSwitch.setOnCheckedChangeListener((b, checked) -> Pref.setAutoOpenLastApp(display.getName(), checked));

            MaterialSwitch floatingSwitch = view.findViewById(R.id.floating_button_switch);
            floatingSwitch.setChecked(Pref.getFloatingButton(display.getName()));
            floatingSwitch.setOnCheckedChangeListener((b, checked) -> {
                if (checked) {
                    if (!FloatingButtonService.startFloating(getContext(), displayId, false)) {
                        floatingSwitch.setChecked(false);
                        return;
                    }
                } else {
                    getContext().stopService(new Intent(getContext(), FloatingButtonService.class));
                }
                Pref.setFloatingButton(display.getName(), checked);
            });

            MaterialSwitch landscapeSwitch = view.findViewById(R.id.floating_button_force_landscape_switch);
            landscapeSwitch.setChecked(Pref.getFloatingButtonForceLandscape());
            landscapeSwitch.setOnCheckedChangeListener((b, checked) -> Pref.setFloatingButtonForceLandscape(checked));

            View imePolicyRow = view.findViewById(R.id.ime_policy_row);
            Spinner imePolicySpinner = view.findViewById(R.id.ime_policy_spinner);
            _setupImePolicy(imePolicyRow, imePolicySpinner);
        }

        // --- Display Configuration ---
        resolutionText = view.findViewById(R.id.resolution_text);
        dpiText = view.findViewById(R.id.dpi_text);
        rotationText = view.findViewById(R.id.user_rotation_text);
        dpiSlider = view.findViewById(R.id.dpi_slider);
        scaleText = view.findViewById(R.id.scale_text);
        scaleSlider = view.findViewById(R.id.scale_slider);
        scaleRow = view.findViewById(R.id.scale_row);

        MaterialButton editResolutionButton = view.findViewById(R.id.edit_resolution_button);
        MaterialButton editDpiButton = view.findViewById(R.id.edit_dpi_button);
        MaterialButton editRotationButton = view.findViewById(R.id.edit_rotation_button);
        MaterialButton editScaleButton = view.findViewById(R.id.edit_scale_button);

        if (ShizukuUtils.hasShizukuStarted()) {
            _readNativeSize();

            editResolutionButton.setVisibility(View.VISIBLE);
            editResolutionButton.setOnClickListener(v -> {
                Display d = _currentDisplay();
                if (d != null) ResolutionDialog.show(getContext(), displayId, d.getWidth(), d.getHeight());
            });

            editDpiButton.setVisibility(View.VISIBLE);
            editDpiButton.setOnClickListener(v -> {
                Display d = _currentDisplay();
                if (d == null) return;
                DisplayMetrics m = new DisplayMetrics();
                d.getMetrics(m);
                DpiDialog.show(getContext(), displayId, m.densityDpi);
            });

            dpiSlider.setVisibility(View.VISIBLE);
            dpiSlider.addOnChangeListener((s, value, fromUser) -> {
                if (fromUser) dpiText.setText(String.valueOf((int) value));
            });
            dpiSlider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
                @Override public void onStartTrackingTouch(Slider slider) {}
                @Override public void onStopTrackingTouch(Slider slider) {
                    int newDpi = (int) slider.getValue();
                    ServiceUtils.getWindowManager().setForcedDisplayDensityForUser(displayId, newDpi, 0);
                }
            });

            if (nativeWidth > 0 && nativeHeight > 0) {
                scaleRow.setVisibility(View.VISIBLE);
                scaleSlider.setVisibility(View.VISIBLE);

                editScaleButton.setOnClickListener(v ->
                        ScaleDialog.show(getContext(), displayId, nativeWidth, nativeHeight, _currentScalePercent()));

                scaleSlider.addOnChangeListener((s, value, fromUser) -> {
                    if (fromUser) scaleText.setText(getString(R.string.scale_format, (int) value));
                });
                scaleSlider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
                    @Override public void onStartTrackingTouch(Slider slider) {}
                    @Override public void onStopTrackingTouch(Slider slider) {
                        int percent = (int) slider.getValue();
                        if (percent <= 0) return;
                        int w = Math.round(nativeWidth * 100f / percent);
                        int h = Math.round(nativeHeight * 100f / percent);
                        ServiceUtils.getWindowManager().setForcedDisplaySize(displayId, w, h);
                    }
                });
            }

            if (isSecondary) {
                editRotationButton.setVisibility(View.VISIBLE);
                editRotationButton.setOnClickListener(v -> RotationDialog.show(getContext(), displayId));
            }
        }

        _refreshDisplayState();

        return view;
    }

    private Display _currentDisplay() {
        Context ctx = getContext();
        if (ctx == null) return null;
        return ((DisplayManager) ctx.getSystemService(Context.DISPLAY_SERVICE)).getDisplay(displayId);
    }

    private void _readNativeSize() {
        try {
            Point p = new Point();
            ServiceUtils.getWindowManager().getInitialDisplaySize(displayId, p);
            nativeWidth = p.x;
            nativeHeight = p.y;
        } catch (Throwable e) {
            State.log("failed to read initial display size: " + e.getMessage());
            nativeWidth = 0;
            nativeHeight = 0;
        }
    }

    private void _handleCurrentDisplayRemoved() {
        Activity activity = getActivity();
        if (activity == null || !isAdded()) {
            return;
        }
        activity.runOnUiThread(() -> {
            activity.onBackPressed();
        });
    }

    private int _currentScalePercent() {
        if (nativeWidth <= 0) return 100;
        Display d = _currentDisplay();
        if (d == null || d.getWidth() <= 0) return 100;
        return Math.round(nativeWidth * 100f / d.getWidth());
    }

    private float _snapSliderValue(Slider slider, float value) {
        float clamped = Math.max(slider.getValueFrom(), Math.min(slider.getValueTo(), value));
        float stepSize = slider.getStepSize();
        if (stepSize <= 0f) return clamped;

        float steps = Math.round((clamped - slider.getValueFrom()) / stepSize);
        float snapped = slider.getValueFrom() + steps * stepSize;
        return Math.max(slider.getValueFrom(), Math.min(slider.getValueTo(), snapped));
    }

    private void _refreshDisplayState() {
        Context ctx = getContext();
        if (ctx == null) return;
        Display d = _currentDisplay();
        if (d == null) return;
        DisplayMetrics metrics = new DisplayMetrics();
        d.getMetrics(metrics);

        if (resolutionText != null) resolutionText.setText(d.getWidth() + " x " + d.getHeight());
        if (dpiText != null) dpiText.setText(String.valueOf(metrics.densityDpi));
        if (dpiSlider != null && dpiSlider.getVisibility() == View.VISIBLE) {
            float snapped = _snapSliderValue(dpiSlider, metrics.densityDpi);
            if (dpiSlider.getValue() != snapped) dpiSlider.setValue(snapped);
        }
        if (scaleText != null && scaleRow != null && scaleRow.getVisibility() == View.VISIBLE) {
            int percent = _currentScalePercent();
            scaleText.setText(getString(R.string.scale_format, percent));
            if (scaleSlider != null) {
                float snapped = _snapSliderValue(scaleSlider, percent);
                if (scaleSlider.getValue() != snapped) scaleSlider.setValue(snapped);
            }
        }
        if (rotationText != null) _updateRotationText(rotationText);

        _populateInfoTable(d, ctx);
        Display.Mode currentMode = d.getMode();
        _setupDisplayModes(d.getSupportedModes(), currentMode != null ? currentMode.getModeId() : -1);
        _updateShizukuStatus();
    }

    private void _populateInfoTable(Display d, Context ctx) {
        if (infoTable == null) return;
        infoTable.removeAllViews();
        DisplayCutout cutout = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            cutout = d.getCutout();
        }
        String cutoutInfo = getString(R.string.none);
        if (cutout != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            List<Rect> rects = cutout.getBoundingRects();
            if (!rects.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < rects.size(); i++) {
                    Rect r = rects.get(i);
                    if (i > 0) sb.append("\n");
                    sb.append(String.format("{'left': %d, 'top': %d, 'right': %d, 'bottom': %d}", r.left, r.top, r.right, r.bottom));
                }
                cutoutInfo = sb.toString();
            }
        }
        InfoRow.add(ctx, infoTable, getString(R.string.info_display_id), String.valueOf(d.getDisplayId()));
        InfoRow.add(ctx, infoTable, getString(R.string.info_name), d.getName());
        InfoRow.add(ctx, infoTable, getString(R.string.info_refresh_rate), String.format("%.1f Hz", d.getRefreshRate()));
        InfoRow.add(ctx, infoTable, getString(R.string.info_state), d.getState() == Display.STATE_ON ? getString(R.string.on) : getString(R.string.off));
        InfoRow.add(ctx, infoTable, getString(R.string.info_hdr), d.isHdr() ? getString(R.string.yes) : getString(R.string.no));
        InfoRow.add(ctx, infoTable, getString(R.string.info_flags), _getDisplayFlags(d));
        InfoRow.add(ctx, infoTable, getString(R.string.info_cutout), cutoutInfo);
    }

    private void _updateShizukuStatus() {
        if (shizukuTable == null) return;
        shizukuTable.removeAllViews();
        Context ctx = getContext();
        if (ctx == null) return;
        View header = getView() != null ? getView().findViewById(R.id.shizuku_header) : null;

        if (!ShizukuUtils.hasShizukuStarted() || !ShizukuUtils.hasPermission()) {
            shizukuCard.setVisibility(View.GONE);
            if (header != null) header.setVisibility(View.GONE);
            return;
        }
        shizukuCard.setVisibility(View.VISIBLE);
        if (header != null) header.setVisibility(View.VISIBLE);

        try {

            IWindowManager windowManager = ServiceUtils.getWindowManager();

            Point baseSize = new Point();
            windowManager.getBaseDisplaySize(displayId, baseSize);
            InfoRow.add(ctx, shizukuTable, getString(R.string.info_override_size), baseSize.x + "x" + baseSize.y);

            Point initialSize = new Point();
            windowManager.getInitialDisplaySize(displayId, initialSize);
            InfoRow.add(ctx, shizukuTable, getString(R.string.info_physical_size), initialSize.x + "x" + initialSize.y);

            try {
                int imePolicy = windowManager.getDisplayImePolicy(displayId);
                InfoRow.add(ctx, shizukuTable, getString(R.string.info_keyboard_policy),
                        DisplayImePolicyCompat.toDebugString(imePolicy));
            } catch (Throwable e) { /* ignore */ }

            DisplayInfo displayInfo = ServiceUtils.getDisplayManager().getDisplayInfo(displayId);
            InfoRow.add(ctx, shizukuTable, getString(R.string.info_default_mode_id), String.valueOf(displayInfo.defaultModeId));
            try { InfoRow.add(ctx, shizukuTable, getString(R.string.info_refresh_rate_override), String.format("%.1f Hz", displayInfo.refreshRateOverride)); } catch (Throwable e) { }
            try { InfoRow.add(ctx, shizukuTable, getString(R.string.info_install_orientation), String.valueOf(displayInfo.installOrientation)); } catch (Throwable e) { }
            try { InfoRow.add(ctx, shizukuTable, getString(R.string.info_windowing_mode), WindowingMode.getWindowingMode(displayId)); } catch (Throwable e) { }

        } catch (Exception e) {
            shizukuCard.setVisibility(View.GONE);
            if (header != null) header.setVisibility(View.GONE);
            State.log("failed to load advanced display info: " + e.getMessage());
        }
    }

    private String _getDisplayFlags(Display display) {
        int flags = display.getFlags();
        StringBuilder flagsStr = new StringBuilder();
        if ((flags & Display.FLAG_SECURE) != 0) flagsStr.append("FLAG_SECURE, ");
        if ((flags & Display.FLAG_SUPPORTS_PROTECTED_BUFFERS) != 0) flagsStr.append("FLAG_SUPPORTS_PROTECTED_BUFFERS, ");
        if ((flags & Display.FLAG_PRIVATE) != 0) flagsStr.append("FLAG_PRIVATE, ");
        if ((flags & Display.FLAG_PRESENTATION) != 0) flagsStr.append("FLAG_PRESENTATION, ");
        if ((flags & Display.FLAG_ROUND) != 0) flagsStr.append("FLAG_ROUND, ");
        if (flagsStr.length() > 0) flagsStr.setLength(flagsStr.length() - 2);
        return flagsStr.length() > 0 ? flagsStr.toString() : getString(R.string.none);
    }

    private void _setupDisplayModes(Display.Mode[] supportedModes, int currentModeId) {
        modesTable.removeAllViews();
        Context modeCtx = getContext();
        if (modeCtx == null) return;
        boolean canApplyModes = ServiceUtils.canSetUserPreferredDisplayMode();
        for (Display.Mode mode : supportedModes) {
            View row = LayoutInflater.from(modeCtx).inflate(R.layout.item_display_mode, modesTable, false);
            TextView labelView = row.findViewById(R.id.mode_label);
            TextView valueView = row.findViewById(R.id.mode_value);
            MaterialButton applyButton = row.findViewById(R.id.mode_apply_button);

            String title = getString(R.string.mode_col_id) + " " + mode.getModeId();
            String value = mode.getPhysicalWidth() + "x" + mode.getPhysicalHeight()
                    + "@" + String.format("%.1f Hz", mode.getRefreshRate());
            boolean isCurrentMode = mode.getModeId() == currentModeId;

            labelView.setText(title);
            valueView.setText(value);

            if (!canApplyModes) {
                applyButton.setVisibility(View.GONE);
            } else {
                applyButton.setEnabled(!isCurrentMode);
                applyButton.setText(isCurrentMode ? R.string.current : R.string.apply);
                applyButton.setOnClickListener(v -> State.startNewJob(new ApplyDisplayMode(displayId, mode)));
            }

            modesTable.addView(row);
        }
    }

    private void _updateRotationText(TextView rotationText) {
        Context ctx = getContext();
        if (ctx == null) return;
        Display fresh = ((DisplayManager) ctx.getSystemService(Context.DISPLAY_SERVICE)).getDisplay(displayId);
        int rotation = fresh != null ? fresh.getRotation() : -1;
        switch (rotation) {
            case Surface.ROTATION_0: rotationText.setText("0°"); break;
            case Surface.ROTATION_90: rotationText.setText("90°"); break;
            case Surface.ROTATION_180: rotationText.setText("180°"); break;
            case Surface.ROTATION_270: rotationText.setText("270°"); break;
            default: rotationText.setText(getString(R.string.unknown));
        }
    }

    private void _showManagedVirtualDisplayDialog() {
        if (Build.VERSION.SDK_INT >= 34) {
            Toast.makeText(getContext(), getString(R.string.android15_rotation_hint), Toast.LENGTH_SHORT).show();
        }
        ManagedVirtualDisplayDialog.show(getContext(), display, displayId);
    }

    private void _setupImePolicy(View row, Spinner spinner) {
        if (row == null || spinner == null) {
            return;
        }
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

    private void _syncImePolicySpinner(IWindowManager wm, Spinner spinner, int targetDisplayId) throws Throwable {
        _setupImePolicySpinner(spinner);
        int currentPolicy = wm.getDisplayImePolicy(targetDisplayId);
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
                _applyImePolicySelection(wm, spinner, targetDisplayId,
                        DisplayImePolicyCompat.values()[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // no-op
            }
        });
    }

    private void _applyImePolicySelection(IWindowManager wm, Spinner spinner,
                                          int targetDisplayId, int requestedPolicy) {
        try {
            int currentPolicy = wm.getDisplayImePolicy(targetDisplayId);
            if (currentPolicy == requestedPolicy) {
                return;
            }

            wm.setDisplayImePolicy(Display.DEFAULT_DISPLAY, DisplayImePolicyCompat.LOCAL);
            wm.setDisplayImePolicy(targetDisplayId, requestedPolicy);
            State.refreshUI();
        } catch (Throwable e) {
            try {
                _syncImePolicySpinner(wm, spinner, targetDisplayId);
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
