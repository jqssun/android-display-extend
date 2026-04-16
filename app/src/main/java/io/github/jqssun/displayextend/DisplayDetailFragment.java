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
import android.widget.LinearLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import io.github.jqssun.displayextend.dialog.BridgeDialog;
import io.github.jqssun.displayextend.dialog.DpiDialog;
import io.github.jqssun.displayextend.dialog.ResolutionDialog;
import io.github.jqssun.displayextend.dialog.RotationDialog;
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
    private View imePolicyRow;
    private MaterialSwitch imePolicySwitch;

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
        imePolicyRow = view.findViewById(R.id.ime_policy_row);
        imePolicySwitch = view.findViewById(R.id.ime_policy_switch);
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
        MaterialButton launchButton = view.findViewById(R.id.start_launcher_button);
        if (!isSecondary) {
            launchButton.setVisibility(View.GONE);
            view.findViewById(R.id.actions_header).setVisibility(View.GONE);
        }
        launchButton.setOnClickListener(v -> LauncherActivity.start(getContext(), displayId));

        MaterialButton touchpadButton = view.findViewById(R.id.touchpad_button);
        if (isSecondary && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R || ShizukuUtils.hasPermission())) {
            touchpadButton.setVisibility(View.VISIBLE);
        }
        touchpadButton.setOnClickListener(v -> TouchpadActivity.startTouchpad(getContext(), displayId, false));

        MaterialButton bridgeButton = view.findViewById(R.id.bridge_button);
        if (displayId == State.getBridgeVirtualDisplayId() || displayId == State.bridgeDisplayId) {
            bridgeButton.setVisibility(View.VISIBLE);
            bridgeButton.setText(getString(R.string.exit_bridge));
            bridgeButton.setOnClickListener(v -> {
                BridgeActivity.stopVirtualDisplay();
                if (BridgeActivity.getInstance() != null) BridgeActivity.getInstance().finish();
            });
        } else if (isSecondary && ShizukuUtils.hasShizukuStarted()) {
            bridgeButton.setVisibility(View.VISIBLE);
            bridgeButton.setOnClickListener(v -> _showBridgeDialog());
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

            MaterialSwitch landscapeSwitch = view.findViewById(R.id.force_landscape_switch);
            landscapeSwitch.setChecked(Pref.getForceLandscape());
            landscapeSwitch.setOnCheckedChangeListener((b, checked) -> Pref.setForceLandscape(checked));
        }

        // --- Display Configuration ---
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);

        TextView resolutionText = view.findViewById(R.id.resolution_text);
        resolutionText.setText(display.getWidth() + " x " + display.getHeight());

        TextView dpiText = view.findViewById(R.id.dpi_text);
        dpiText.setText(String.valueOf(metrics.densityDpi));

        TextView rotationText = view.findViewById(R.id.user_rotation_text);
        _updateRotationText(rotationText);

        MaterialButton editResolutionButton = view.findViewById(R.id.edit_resolution_button);
        MaterialButton editDpiButton = view.findViewById(R.id.edit_dpi_button);
        MaterialButton editRotationButton = view.findViewById(R.id.edit_rotation_button);

        if (ShizukuUtils.hasShizukuStarted()) {
            editResolutionButton.setVisibility(View.VISIBLE);
            editResolutionButton.setOnClickListener(v ->
                ResolutionDialog.show(getContext(), displayId, display.getWidth(), display.getHeight()));

            editDpiButton.setVisibility(View.VISIBLE);
            editDpiButton.setOnClickListener(v ->
                DpiDialog.show(getContext(), displayId, metrics.densityDpi));

            if (isSecondary) {
                editRotationButton.setVisibility(View.VISIBLE);
                editRotationButton.setOnClickListener(v -> RotationDialog.show(getContext(), displayId));
            }
        }

        // --- Display Info ---
        DisplayCutout cutout = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            cutout = display.getCutout();
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

        infoTable.removeAllViews();
        InfoRow.add(ctx, infoTable, getString(R.string.info_display_id), String.valueOf(display.getDisplayId()));
        InfoRow.add(ctx, infoTable, getString(R.string.info_name), display.getName());
        InfoRow.add(ctx, infoTable, getString(R.string.info_refresh_rate), String.format("%.1f Hz", display.getRefreshRate()));
        InfoRow.add(ctx, infoTable, getString(R.string.info_state), display.getState() == Display.STATE_ON ? getString(R.string.on) : getString(R.string.off));
        InfoRow.add(ctx, infoTable, getString(R.string.info_hdr), display.isHdr() ? getString(R.string.yes) : getString(R.string.no));
        InfoRow.add(ctx, infoTable, getString(R.string.info_flags), _getDisplayFlags(display));
        InfoRow.add(ctx, infoTable, getString(R.string.info_cutout), cutoutInfo);

        _setupDisplayModes(display.getSupportedModes());
        _updateShizukuStatus();

        return view;
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
                String imePolicyStr;
                switch (imePolicy) {
                    case 0: imePolicyStr = "LOCAL"; break;
                    case 1: imePolicyStr = "FALLBACK_DISPLAY"; break;
                    case 2: imePolicyStr = "HIDE"; break;
                    default: imePolicyStr = String.valueOf(imePolicy);
                }
                InfoRow.add(ctx, shizukuTable, getString(R.string.info_keyboard_policy), imePolicyStr);

                if (displayId != Display.DEFAULT_DISPLAY) {
                    imePolicyRow.setVisibility(View.VISIBLE);
                    imePolicySwitch.setOnCheckedChangeListener(null);
                    imePolicySwitch.setChecked(imePolicy == 0);
                    imePolicySwitch.setOnCheckedChangeListener((b, checked) -> {
                        if (checked) {
                            windowManager.setDisplayImePolicy(Display.DEFAULT_DISPLAY, 1);
                            try {
                                windowManager.setDisplayImePolicy(displayId, 0);
                                State.refreshUI();
                            } catch (Throwable e) {
                                windowManager.setDisplayImePolicy(Display.DEFAULT_DISPLAY, 0);
                                imePolicySwitch.setChecked(false);
                                State.log("failed to set IME policy: " + e);
                                if (getContext() != null) {
                                    Toast.makeText(getContext(), R.string.ime_policy_unsupported, Toast.LENGTH_SHORT).show();
                                }
                            }
                        } else {
                            windowManager.setDisplayImePolicy(Display.DEFAULT_DISPLAY, 0);
                            windowManager.setDisplayImePolicy(displayId, 1);
                            try { State.refreshUI(); } catch (Throwable e) { State.log("failed: " + e); }
                        }
                    });
                }
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

    private void _setupDisplayModes(Display.Mode[] supportedModes) {
        modesTable.removeAllViews();
        Context modeCtx = getContext();
        if (modeCtx == null) return;
        for (Display.Mode mode : supportedModes) {
            String title = getString(R.string.mode_col_id) + " " + mode.getModeId();
            String value = mode.getPhysicalWidth() + "x" + mode.getPhysicalHeight()
                    + "@" + String.format("%.1f Hz", mode.getRefreshRate());
            InfoRow.add(modeCtx, modesTable, title, value);
        }
    }

    private void _updateRotationText(TextView rotationText) {
        int rotation = display.getRotation();
        switch (rotation) {
            case Surface.ROTATION_0: rotationText.setText("0°"); break;
            case Surface.ROTATION_90: rotationText.setText("90°"); break;
            case Surface.ROTATION_180: rotationText.setText("180°"); break;
            case Surface.ROTATION_270: rotationText.setText("270°"); break;
            default: rotationText.setText(getString(R.string.unknown));
        }
    }

    private void _showBridgeDialog() {
        if (Build.VERSION.SDK_INT >= 34) {
            Toast.makeText(getContext(), getString(R.string.android15_rotation_hint), Toast.LENGTH_SHORT).show();
        }
        BridgeDialog.show(getContext(), display, displayId);
    }

}
