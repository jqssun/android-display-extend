package io.github.jqssun.displayextend;

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
import com.google.android.material.checkbox.MaterialCheckBox;
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
    private MaterialButton launchButton;
    private int displayId;
    private Display display;
    private LinearLayout modesTable;
    private MaterialButton setImePolicyButton;
    private MaterialButton floatingButtonToggle;
    private MaterialCheckBox forceLandscapeCheckbox;

    public static DisplayDetailFragment newInstance(int displayId) {
        DisplayDetailFragment fragment = new DisplayDetailFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_DISPLAY_ID, displayId);
        fragment.setArguments(args);
        return fragment;
    }

    private String _getDisplayFlags(Display display) {
        int flags = display.getFlags();
        StringBuilder flagsStr = new StringBuilder();

        if ((flags & Display.FLAG_SECURE) != 0) flagsStr.append("FLAG_SECURE, ");
        if ((flags & Display.FLAG_SUPPORTS_PROTECTED_BUFFERS) != 0) flagsStr.append("FLAG_SUPPORTS_PROTECTED_BUFFERS, ");
        if ((flags & Display.FLAG_PRIVATE) != 0) flagsStr.append("FLAG_PRIVATE, ");
        if ((flags & Display.FLAG_PRESENTATION) != 0) flagsStr.append("FLAG_PRESENTATION, ");
        if ((flags & Display.FLAG_ROUND) != 0) flagsStr.append("FLAG_ROUND, ");

        if (flagsStr.length() > 0) {
            flagsStr.setLength(flagsStr.length() - 2);
        }

        return flagsStr.length() > 0 ? flagsStr.toString() : getString(R.string.none);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_display_detail, container, false);
        setImePolicyButton = view.findViewById(R.id.set_ime_policy_button);
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

        DisplayCutout cutout = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            cutout = display.getCutout();
        }
        String cutoutInfo = getString(R.string.no_cutout);
        if (cutout != null) {
            StringBuilder cutoutDetails = new StringBuilder(getString(R.string.cutout_bounds));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                for (Rect rect : cutout.getBoundingRects()) {
                    cutoutDetails.append(String.format(getString(R.string.cutout_rect_format),
                        rect.left, rect.top, rect.right, rect.bottom));
                }
            }
            cutoutInfo = cutoutDetails.toString();
        }

        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);

        TextView resolutionText = view.findViewById(R.id.resolution_text);
        resolutionText.setText(display.getWidth() + " x " + display.getHeight());

        // Display info table
        infoTable.removeAllViews();
        _addInfoRow(infoTable, getString(R.string.info_display_id), String.valueOf(display.getDisplayId()));
        _addInfoRow(infoTable, getString(R.string.info_name), display.getName());
        _addInfoRow(infoTable, getString(R.string.info_refresh_rate), String.format("%.1f Hz", display.getRefreshRate()));
        _addInfoRow(infoTable, getString(R.string.info_state), display.getState() == Display.STATE_ON ? getString(R.string.on) : getString(R.string.off));
        _addInfoRow(infoTable, getString(R.string.info_hdr), display.isHdr() ? getString(R.string.yes) : getString(R.string.no));
        _addInfoRow(infoTable, getString(R.string.info_flags), _getDisplayFlags(display));
        _addInfoRow(infoTable, getString(R.string.info_cutout), cutoutInfo);

        _setupDisplayModes(display.getSupportedModes());

        launchButton = view.findViewById(R.id.start_launcher_button);
        if (displayId == 0) {
            launchButton.setVisibility(View.GONE);
        }
        launchButton.setOnClickListener(v -> {
            LauncherActivity.start(getContext(), displayId);
        });

        MaterialCheckBox autoOpenLastAppCheckbox = view.findViewById(R.id.autoOpenLastAppCheckbox);
        if (displayId != Display.DEFAULT_DISPLAY) {
            autoOpenLastAppCheckbox.setVisibility(View.VISIBLE);
            autoOpenLastAppCheckbox.setChecked(Pref.getAutoOpenLastApp(display.getName()));
            autoOpenLastAppCheckbox.setOnCheckedChangeListener((buttonView, isChecked) ->
                Pref.setAutoOpenLastApp(display.getName(), isChecked));
        }

        MaterialButton touchpadButton = view.findViewById(R.id.touchpad_button);
        if (displayId != Display.DEFAULT_DISPLAY) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R || ShizukuUtils.hasPermission()) {
                touchpadButton.setVisibility(View.VISIBLE);
            }
        }
        touchpadButton.setOnClickListener(v -> {
            TouchpadActivity.startTouchpad(getContext(), displayId, false);
        });

        MaterialButton editResolutionButton = view.findViewById(R.id.edit_resolution_button);
        if(ShizukuUtils.hasShizukuStarted()) {
            editResolutionButton.setVisibility(View.VISIBLE);
            editResolutionButton.setOnClickListener(v -> {
                ResolutionDialog.show(getContext(), displayId, display.getWidth(), display.getHeight());
            });
        }

        TextView dpiText = view.findViewById(R.id.dpi_text);
        dpiText.setText(String.valueOf(metrics.densityDpi));

        MaterialButton editDpiButton = view.findViewById(R.id.edit_dpi_button);
        if(ShizukuUtils.hasShizukuStarted()) {
            editDpiButton.setVisibility(View.VISIBLE);
            editDpiButton.setOnClickListener(v -> {
                DpiDialog.show(getContext(), displayId, metrics.densityDpi);
            });
        }

        TextView userRotationText = view.findViewById(R.id.user_rotation_text);
        MaterialButton editRotationButton = view.findViewById(R.id.edit_rotation_button);

        _updateUserRotationText(userRotationText);

        if(ShizukuUtils.hasShizukuStarted()) {
            if (displayId != Display.DEFAULT_DISPLAY) {
                editRotationButton.setVisibility(View.VISIBLE);
            }
            editRotationButton.setOnClickListener(v -> {
                _showRotationDialog();
            });
        }

        _updateShizukuStatus();

        MaterialButton bridgeButton = view.findViewById(R.id.bridge_button);

        if (displayId == State.getBridgeVirtualDisplayId() || displayId == State.bridgeDisplayId) {
            bridgeButton.setVisibility(View.VISIBLE);
            bridgeButton.setText(getString(R.string.exit_bridge));
            bridgeButton.setOnClickListener(v -> {
                BridgeActivity.stopVirtualDisplay();
                if (BridgeActivity.getInstance() != null) {
                    BridgeActivity.getInstance().finish();
                }
            });
        } else if(displayId != Display.DEFAULT_DISPLAY && ShizukuUtils.hasShizukuStarted()) {
            bridgeButton.setVisibility(View.VISIBLE);
            bridgeButton.setOnClickListener(v -> _showBridgeDialog());
        }

        floatingButtonToggle = view.findViewById(R.id.floating_button_toggle);
        forceLandscapeCheckbox = view.findViewById(R.id.force_landscape_checkbox);
        
        if (displayId != Display.DEFAULT_DISPLAY) {
            floatingButtonToggle.setVisibility(View.VISIBLE);
            forceLandscapeCheckbox.setVisibility(View.VISIBLE);
            Activity activity = getActivity();
            if (activity == null) return view;
            boolean isEnabled = Pref.getFloatingButton(display.getName());
            boolean forceLandscape = Pref.getForceLandscape();

            _updateFloatingBackButtonText(isEnabled);
            forceLandscapeCheckbox.setChecked(forceLandscape);

            floatingButtonToggle.setOnClickListener(v -> {
                boolean newIsEnabled = !Pref.getFloatingButton(display.getName());
                if (newIsEnabled) {
                    if (FloatingButtonService.startFloating(getContext(), displayId, false)) {
                        Pref.setFloatingButton(display.getName(), true);
                    }
                } else {
                    Intent serviceIntent = new Intent(getContext(), FloatingButtonService.class);
                    getContext().stopService(serviceIntent);
                    Pref.setFloatingButton(display.getName(), false);
                }
                _updateFloatingBackButtonText(newIsEnabled);
            });

            forceLandscapeCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Pref.setForceLandscape(isChecked);
            });
        }

        return view;
    }

    private void _updateShizukuStatus() {
        if (shizukuTable == null) return;
        shizukuTable.removeAllViews();

        if (!ShizukuUtils.hasShizukuStarted()) {
            _addInfoRow(shizukuTable, getString(R.string.info_shizuku), getString(R.string.shizuku_not_started));
            return;
        }
        try {
            boolean hasPermission = ShizukuUtils.hasPermission();
            _addInfoRow(shizukuTable, getString(R.string.info_shizuku), hasPermission ? getString(R.string.shizuku_status_granted) : getString(R.string.shizuku_status_denied));

            if (!hasPermission) return;

            IWindowManager windowManager = ServiceUtils.getWindowManager();

            Point baseSize = new Point();
            windowManager.getBaseDisplaySize(displayId, baseSize);
            _addInfoRow(shizukuTable, getString(R.string.info_override_size), baseSize.x + "x" + baseSize.y);

            Point initialSize = new Point();
            windowManager.getInitialDisplaySize(displayId, initialSize);
            _addInfoRow(shizukuTable, getString(R.string.info_physical_size), initialSize.x + "x" + initialSize.y);

            try {
                int imePolicy = windowManager.getDisplayImePolicy(displayId);
                String imePolicyStr;
                switch (imePolicy) {
                    case 0: imePolicyStr = "LOCAL"; break;
                    case 1: imePolicyStr = "FALLBACK_DISPLAY"; break;
                    case 2: imePolicyStr = "HIDE"; break;
                    default: imePolicyStr = String.valueOf(imePolicy);
                }
                _addInfoRow(shizukuTable, getString(R.string.info_keyboard_policy), imePolicyStr);

                if (displayId != Display.DEFAULT_DISPLAY) {
                    setImePolicyButton.setVisibility(View.VISIBLE);
                    if (imePolicy == 0) {
                        setImePolicyButton.setText(getString(R.string.ime_to_main_display));
                        setImePolicyButton.setOnClickListener(v -> {
                            windowManager.setDisplayImePolicy(Display.DEFAULT_DISPLAY, 0);
                            windowManager.setDisplayImePolicy(displayId, 1);
                            try { State.refreshUI(); } catch (Throwable e) { State.log("failed: " + e); }
                        });
                    } else {
                        setImePolicyButton.setText(getString(R.string.ime_to_this_display));
                        setImePolicyButton.setOnClickListener(v -> {
                            windowManager.setDisplayImePolicy(Display.DEFAULT_DISPLAY, 1);
                            try {
                                windowManager.setDisplayImePolicy(displayId, 0);
                                State.refreshUI();
                            } catch (Throwable e) {
                                windowManager.setDisplayImePolicy(Display.DEFAULT_DISPLAY, 0);
                                State.log("failed: " + e);
                            }
                        });
                    }
                }
            } catch (Throwable e) { /* ignore */ }

            DisplayInfo displayInfo = ServiceUtils.getDisplayManager().getDisplayInfo(displayId);
            _addInfoRow(shizukuTable, getString(R.string.info_default_mode_id), String.valueOf(displayInfo.defaultModeId));
            try { _addInfoRow(shizukuTable, getString(R.string.info_refresh_rate_override), String.format("%.1f Hz", displayInfo.refreshRateOverride)); } catch (Throwable e) { }
            try { _addInfoRow(shizukuTable, getString(R.string.info_install_orientation), String.valueOf(displayInfo.installOrientation)); } catch (Throwable e) { }
            try { _addInfoRow(shizukuTable, getString(R.string.info_windowing_mode), WindowingMode.getWindowingMode(displayId)); } catch (Throwable e) { }

        } catch (Exception e) {
            shizukuTable.removeAllViews();
            _addInfoRow(shizukuTable, getString(R.string.info_shizuku), getString(R.string.shizuku_status_denied));
            State.log("failed to get Shizuku permission: " + e.getMessage());
        }
    }

    private void _setupDisplayModes(Display.Mode[] supportedModes) {
        modesTable.removeAllViews();

        for (Display.Mode mode : supportedModes) {
            String title = getString(R.string.mode_col_id) + " " + mode.getModeId();
            String value = mode.getPhysicalWidth() + "x" + mode.getPhysicalHeight()
                    + "@" + String.format("%.1f Hz", mode.getRefreshRate());
            _addInfoRow(modesTable, title, value);
        }
    }

    private void _addInfoRow(LinearLayout table, String label, String value) {
        int dp16 = (int) (16 * getResources().getDisplayMetrics().density);
        int dp12 = (int) (12 * getResources().getDisplayMetrics().density);
        int dp2 = (int) (2 * getResources().getDisplayMetrics().density);
        int dp56 = (int) (56 * getResources().getDisplayMetrics().density);

        LinearLayout item = new LinearLayout(getContext());
        item.setOrientation(LinearLayout.VERTICAL);
        item.setPadding(dp16, dp12, dp16, dp12);
        item.setMinimumHeight(dp56);

        TextView labelTv = new TextView(getContext());
        labelTv.setText(label);
        labelTv.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge);
        item.addView(labelTv);

        TextView valueTv = new TextView(getContext());
        valueTv.setText(value);
        valueTv.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
        valueTv.setTextColor(getResources().getColor(android.R.color.darker_gray, null));
        valueTv.setPadding(0, dp2, 0, 0);
        item.addView(valueTv);

        table.addView(item);
    }


private void _updateUserRotationText(TextView rotationText) {
    int rotation = display.getRotation();
    String rotationStr;
    switch(rotation) {
        case Surface.ROTATION_0:
            rotationStr = "0°";
            break;
        case Surface.ROTATION_90:
            rotationStr = "90°";
            break;
        case Surface.ROTATION_180:
            rotationStr = "180°";
            break;
        case Surface.ROTATION_270:
            rotationStr = "270°";
            break;
        default:
            rotationStr = getString(R.string.unknown);
    }
    rotationText.setText(rotationStr);
}

private void _showRotationDialog() {
    RotationDialog.show(getContext(), displayId);
}

private void _showBridgeDialog() {
    if (android.os.Build.VERSION.SDK_INT >= 34) {
        Toast.makeText(getContext(), getString(R.string.android15_rotation_hint), Toast.LENGTH_SHORT).show();
    }
    BridgeDialog.show(getContext(), display, displayId);
}

private void _updateFloatingBackButtonText(boolean isEnabled) {
    floatingButtonToggle.setText(isEnabled ? getString(R.string.hide_floating_back_button) : getString(R.string.show_floating_back_button));
}
}