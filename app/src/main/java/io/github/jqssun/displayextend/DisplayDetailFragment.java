package io.github.jqssun.displayextend;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.hardware.display.IDisplayManager;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.DisplayCutout;
import android.view.DisplayInfo;
import android.view.IWindowManager;
import android.widget.LinearLayout;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import io.github.jqssun.displayextend.shizuku.ServiceUtils;
import io.github.jqssun.displayextend.shizuku.ShizukuUtils;
import io.github.jqssun.displayextend.dialog.RotationDialog;
import io.github.jqssun.displayextend.dialog.ResolutionDialog;
import io.github.jqssun.displayextend.dialog.BridgeDialog;
import io.github.jqssun.displayextend.dialog.DpiDialog;
import io.github.jqssun.displayextend.shizuku.WindowingMode;

public class DisplayDetailFragment extends Fragment {
    private static final String ARG_DISPLAY_ID = "display_id";

    private LinearLayout infoTable;
    private LinearLayout shizukuTable;
    private LinearLayout shizukuCard;
    private Button launchButton;
    private int displayId;
    private Display display;
    private android.widget.TableLayout modesTable;
    private Button setImePolicyButton;
    private CheckBox autoOpenLastAppCheckbox;
    private Button floatingButtonToggle;
    private CheckBox forceLandscapeCheckbox;

    public static DisplayDetailFragment newInstance(int displayId) {
        DisplayDetailFragment fragment = new DisplayDetailFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_DISPLAY_ID, displayId);
        fragment.setArguments(args);
        return fragment;
    }

    private String getDisplayFlags(Display display) {
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

    private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_display_detail, container, false);
        setImePolicyButton = view.findViewById(R.id.set_ime_policy_button);
        modesTable = view.findViewById(R.id.modes_table);
        infoTable = view.findViewById(R.id.info_table);
        shizukuTable = view.findViewById(R.id.shizuku_table);
        shizukuCard = view.findViewById(R.id.shizuku_card);
        autoOpenLastAppCheckbox = view.findViewById(R.id.autoOpenLastAppCheckbox);
        displayId = getArguments().getInt(ARG_DISPLAY_ID);
        DisplayManager displayManager = (DisplayManager) getContext().getSystemService(Context.DISPLAY_SERVICE);
        display = displayManager.getDisplay(displayId);

        if(display == null) {
            State.currentActivity.get().onBackPressed();
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
        _addInfoRow(infoTable, "Display ID", String.valueOf(display.getDisplayId()));
        _addInfoRow(infoTable, "Name", display.getName());
        _addInfoRow(infoTable, "Refresh rate", String.format("%.1f Hz", display.getRefreshRate()));
        _addInfoRow(infoTable, "State", display.getState() == Display.STATE_ON ? getString(R.string.on) : getString(R.string.off));
        _addInfoRow(infoTable, "HDR", display.isHdr() ? getString(R.string.yes) : getString(R.string.no));
        _addInfoRow(infoTable, "Flags", getDisplayFlags(display));
        _addInfoRow(infoTable, "Cutout", cutoutInfo);

        setupDisplayModes(display.getSupportedModes());

        launchButton = view.findViewById(R.id.start_launcher_button);
        if (displayId == 0) {
            launchButton.setVisibility(View.GONE);
        }
        launchButton.setOnClickListener(v -> {
            LauncherActivity.start(getContext(), displayId);
        });

        Button touchpadButton = view.findViewById(R.id.touchpad_button);
        if (displayId != Display.DEFAULT_DISPLAY) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R || ShizukuUtils.hasPermission()) {
                touchpadButton.setVisibility(View.VISIBLE);
            }
        }
        touchpadButton.setOnClickListener(v -> {
            TouchpadActivity.startTouchpad(getContext(), displayId, false);
        });

        Button editResolutionButton = view.findViewById(R.id.edit_resolution_button);
        if(ShizukuUtils.hasShizukuStarted()) {
            editResolutionButton.setVisibility(View.VISIBLE);
            editResolutionButton.setOnClickListener(v -> {
                ResolutionDialog.show(getContext(), displayId, display.getWidth(), display.getHeight());
            });
        }

        TextView dpiText = view.findViewById(R.id.dpi_text);
        dpiText.setText(String.valueOf(metrics.densityDpi));

        Button editDpiButton = view.findViewById(R.id.edit_dpi_button);
        if(ShizukuUtils.hasShizukuStarted()) {
            editDpiButton.setVisibility(View.VISIBLE);
            editDpiButton.setOnClickListener(v -> {
                DpiDialog.show(getContext(), displayId, metrics.densityDpi);
            });
        }

        TextView userRotationText = view.findViewById(R.id.user_rotation_text);
        Button editRotationButton = view.findViewById(R.id.edit_rotation_button);

        updateUserRotationText(userRotationText);

        if(ShizukuUtils.hasShizukuStarted()) {
            if (displayId != Display.DEFAULT_DISPLAY) {
                editRotationButton.setVisibility(View.VISIBLE);
            }
            editRotationButton.setOnClickListener(v -> {
                showRotationDialog();
            });
        }

        updateShizukuStatus();

        Button bridgeButton = view.findViewById(R.id.bridge_button);

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
            bridgeButton.setOnClickListener(v -> showBridgeDialog());
        }

        floatingButtonToggle = view.findViewById(R.id.floating_button_toggle);
        forceLandscapeCheckbox = view.findViewById(R.id.force_landscape_checkbox);
        
        if (displayId != Display.DEFAULT_DISPLAY) {
            floatingButtonToggle.setVisibility(View.VISIBLE);
            forceLandscapeCheckbox.setVisibility(View.VISIBLE);
            SharedPreferences appPreferences = getActivity().getSharedPreferences("app_preferences", MODE_PRIVATE);
            boolean isEnabled = appPreferences.getBoolean("FLOATING_BUTTON_" + display.getName(), false);
            boolean forceLandscape = appPreferences.getBoolean("FLOATING_BUTTON_FORCE_LANDSCAPE", false);
            
            updateFloatingBackButtonText(isEnabled);
            forceLandscapeCheckbox.setChecked(forceLandscape);
            
            floatingButtonToggle.setOnClickListener(v -> {
                boolean newIsEnabled = !appPreferences.getBoolean("FLOATING_BUTTON_" + display.getName(), false);
                if (newIsEnabled) {
                    if (FloatingButtonService.startFloating(getContext(), displayId, false)) {
                        appPreferences.edit().putBoolean("FLOATING_BUTTON_" + display.getName(), true).apply();
                    }
                } else {
                    Intent serviceIntent = new Intent(getContext(), FloatingButtonService.class);
                    getContext().stopService(serviceIntent);
                    appPreferences.edit().putBoolean("FLOATING_BUTTON_" + display.getName(), false).apply();
                }
                updateFloatingBackButtonText(newIsEnabled);
            });

            forceLandscapeCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                appPreferences.edit().putBoolean("FLOATING_BUTTON_FORCE_LANDSCAPE", isChecked).apply();
            });
        }

        return view;
    }

    private void updateShizukuStatus() {
        if (shizukuTable == null) return;
        shizukuTable.removeAllViews();

        if (!ShizukuUtils.hasShizukuStarted()) {
            _addInfoRow(shizukuTable, "Shizuku", getString(R.string.shizuku_not_started));
            return;
        }
        try {
            boolean hasPermission = ShizukuUtils.hasPermission();
            _addInfoRow(shizukuTable, "Shizuku", hasPermission ? getString(R.string.shizuku_status_granted) : getString(R.string.shizuku_status_denied));

            if (!hasPermission) return;

            IWindowManager windowManager = ServiceUtils.getWindowManager();

            Point baseSize = new Point();
            windowManager.getBaseDisplaySize(displayId, baseSize);
            _addInfoRow(shizukuTable, "Override size", baseSize.x + "x" + baseSize.y);

            Point initialSize = new Point();
            windowManager.getInitialDisplaySize(displayId, initialSize);
            _addInfoRow(shizukuTable, "Physical size", initialSize.x + "x" + initialSize.y);

            try {
                int imePolicy = windowManager.getDisplayImePolicy(displayId);
                String imePolicyStr;
                switch (imePolicy) {
                    case 0: imePolicyStr = "LOCAL"; break;
                    case 1: imePolicyStr = "FALLBACK_DISPLAY"; break;
                    case 2: imePolicyStr = "HIDE"; break;
                    default: imePolicyStr = String.valueOf(imePolicy);
                }
                _addInfoRow(shizukuTable, "Keyboard policy", imePolicyStr);

                if (displayId != Display.DEFAULT_DISPLAY) {
                    setImePolicyButton.setVisibility(View.VISIBLE);
                    if (imePolicy == 0) {
                        setImePolicyButton.setText(getString(R.string.ime_to_main_display));
                        setImePolicyButton.setOnClickListener(v -> {
                            windowManager.setDisplayImePolicy(Display.DEFAULT_DISPLAY, 0);
                            windowManager.setDisplayImePolicy(displayId, 1);
                            try { State._refreshUI(); } catch (Throwable e) { State.log("failed: " + e); }
                        });
                    } else {
                        setImePolicyButton.setText(getString(R.string.ime_to_this_display));
                        setImePolicyButton.setOnClickListener(v -> {
                            windowManager.setDisplayImePolicy(Display.DEFAULT_DISPLAY, 1);
                            try {
                                windowManager.setDisplayImePolicy(displayId, 0);
                                State._refreshUI();
                            } catch (Throwable e) {
                                windowManager.setDisplayImePolicy(Display.DEFAULT_DISPLAY, 0);
                                State.log("failed: " + e);
                            }
                        });
                    }
                }
            } catch (Throwable e) { /* ignore */ }

            DisplayInfo displayInfo = ServiceUtils.getDisplayManager().getDisplayInfo(displayId);
            _addInfoRow(shizukuTable, "Default mode ID", String.valueOf(displayInfo.defaultModeId));
            try { _addInfoRow(shizukuTable, "Refresh rate override", String.format("%.1f Hz", displayInfo.refreshRateOverride)); } catch (Throwable e) { }
            try { _addInfoRow(shizukuTable, "Install orientation", String.valueOf(displayInfo.installOrientation)); } catch (Throwable e) { }
            try { _addInfoRow(shizukuTable, "Windowing mode", WindowingMode.getWindowingMode(displayId)); } catch (Throwable e) { }

        } catch (Exception e) {
            shizukuTable.removeAllViews();
            _addInfoRow(shizukuTable, "Shizuku", getString(R.string.shizuku_status_denied));
            State.log("failed to get Shizuku permission: " + e.getMessage());
        }
    }

    private void setupDisplayModes(Display.Mode[] supportedModes) {
        modesTable.removeAllViews();

        // Header row
        android.widget.TableRow header = new android.widget.TableRow(getContext());
        header.setPadding(0, 0, 0, 8);
        header.addView(_modeCell(getString(R.string.mode_col_id), true));
        header.addView(_modeCell(getString(R.string.mode_col_resolution), true));
        header.addView(_modeCell(getString(R.string.mode_col_refresh), true));
        modesTable.addView(header);

        // Data rows
        for (Display.Mode mode : supportedModes) {
            android.widget.TableRow row = new android.widget.TableRow(getContext());
            row.addView(_modeCell(String.valueOf(mode.getModeId()), false));
            row.addView(_modeCell(mode.getPhysicalWidth() + "x" + mode.getPhysicalHeight(), false));
            row.addView(_modeCell(String.format("%.1f Hz", mode.getRefreshRate()), false));
            modesTable.addView(row);
        }

        // Double-tap table to select mode
        modesTable.setOnClickListener(new View.OnClickListener() {
            private long lastClickTime = 0;
            @Override
            public void onClick(View v) {
                long clickTime = System.currentTimeMillis();
                if (clickTime - lastClickTime < 300) {
                    showDisplayModeDialog(supportedModes);
                }
                lastClickTime = clickTime;
            }
        });
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

    private TextView _modeCell(String text, boolean isHeader) {
        TextView tv = new TextView(getContext());
        tv.setText(text);
        tv.setPadding(0, 4, 24, 4);
        if (isHeader) {
            tv.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelMedium);
            tv.setTextColor(getResources().getColor(android.R.color.darker_gray, null));
        } else {
            tv.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
        }
        return tv;
    }

    private void showDisplayModeDialog(Display.Mode[] supportedModes) {
        if (!ShizukuUtils.hasShizukuStarted()) {
            showToast(getString(R.string.shizuku_required));
            return;
        }

        String[] items = new String[supportedModes.length];
        for (int i = 0; i < supportedModes.length; i++) {
            Display.Mode mode = supportedModes[i];
            items[i] = String.format("ID:%d %dx%d %.1fHz",
                    mode.getModeId(),
                    mode.getPhysicalWidth(),
                    mode.getPhysicalHeight(),
                    mode.getRefreshRate());
        }

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(getContext())
            .setTitle(getString(R.string.select_display_mode))
            .setItems(items, (dialog, which) -> {
                Display.Mode selectedMode = supportedModes[which];
                try {
                    IDisplayManager displayManager = ServiceUtils.getDisplayManager();
                    displayManager.setUserPreferredDisplayMode(displayId, selectedMode);
                    showToast(getString(R.string.display_mode_set_unlikely));
                } catch (Exception e) {
                    State.log("failed to set display mode: " + e);
                }
            })
            .setNegativeButton(getString(R.string.cancel), null)
            .show();
    }


private void updateUserRotationText(TextView rotationText) {
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

private void showRotationDialog() {
    RotationDialog.show(getContext(), displayId);
}

private void showBridgeDialog() {
    if (android.os.Build.VERSION.SDK_INT >= 34) {
        Toast.makeText(getContext(), getString(R.string.android15_rotation_hint), Toast.LENGTH_SHORT).show();
    }
    BridgeDialog.show(getContext(), display, displayId);
}

private void updateFloatingBackButtonText(boolean isEnabled) {
    floatingButtonToggle.setText(isEnabled ? getString(R.string.hide_floating_back_button) : getString(R.string.show_floating_back_button));
}
}