package io.github.jqssun.displayextend;

import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManagerHidden;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.Settings;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.view.Display;
import android.view.IWindowManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;

import io.github.jqssun.displayextend.shizuku.ServiceUtils;
import io.github.jqssun.displayextend.shizuku.PermissionManager;
import io.github.jqssun.displayextend.shizuku.ShizukuUtils;
import io.github.jqssun.displayextend.shizuku.SurfaceControl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dev.rikka.tools.refine.Refine;

public class SettingsFragment extends Fragment {
    private static final float TRACKING_SPEED_MIN = 0.5f;
    private static final float TRACKING_SPEED_MAX = 5.0f;
    private static final String MATCH_CONTENT_FRAME_RATE_KEY = "match_content_frame_rate";
    private static final String USB_AUDIO_AUTOMATIC_ROUTING_DISABLED_KEY =
            "usb_audio_automatic_routing_disabled";
    private static final String FORCE_DESKTOP_MODE_ON_EXTERNAL_DISPLAYS_KEY =
            "force_desktop_mode_on_external_displays";
    private static final String FORCE_RESIZABLE_ACTIVITIES_KEY = "force_resizable_activities";
    private static final String ENABLE_FREEFORM_SUPPORT_KEY = "enable_freeform_support";
    private static final String ENABLE_NON_RESIZABLE_MULTI_WINDOW_KEY =
            "enable_non_resizable_multi_window";
    private static final String DISABLE_SCREEN_SHARE_PROTECTIONS_KEY =
            "disable_screen_share_protections_for_apps_and_notifications";
    private static final String STAY_ON_WHILE_PLUGGED_IN_KEY = "stay_on_while_plugged_in";

    private MaterialSwitch forceDesktopCheckbox;
    private MaterialSwitch forceResizableCheckbox;
    private MaterialSwitch enableFreeformCheckbox;
    private MaterialSwitch enableNonResizableCheckbox;
    private MaterialSwitch disableScreenShareProtectionCheckbox;
    private MaterialSwitch disableUsbAudioCheckbox;
    private MaterialSwitch useRealScreenOffCheckbox;
    private MaterialSwitch stayOnWhilePluggedCheckbox;
    private MaterialSwitch autoScreenOffCheckbox;
    private MaterialSwitch showSystemSettingNamesSwitch;
    private View matchContentFrameRateRow;
    private Spinner matchContentFrameRateSpinner;
    private Slider trackingSpeedSlider;
    private TextView disableUsbAudioTitle;
    private TextView matchContentFrameRateTitle;
    private TextView forceDesktopTitle;
    private TextView forceResizableTitle;
    private TextView enableFreeformTitle;
    private TextView enableNonResizableTitle;
    private TextView disableScreenShareProtectionTitle;
    private TextView stayOnWhilePluggedTitle;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        forceDesktopCheckbox = view.findViewById(R.id.forceDesktopCheckbox);
        forceResizableCheckbox = view.findViewById(R.id.forceResizableCheckbox);
        enableFreeformCheckbox = view.findViewById(R.id.enableFreeformCheckbox);
        enableNonResizableCheckbox = view.findViewById(R.id.enableNonResizableCheckbox);
        disableScreenShareProtectionCheckbox = view.findViewById(R.id.disableScreenShareProtectionCheckbox);
        disableUsbAudioCheckbox = view.findViewById(R.id.disableUsbAudioCheckbox);
        useRealScreenOffCheckbox = view.findViewById(R.id.useRealScreenOffCheckbox);
        stayOnWhilePluggedCheckbox = view.findViewById(R.id.stayOnWhilePluggedCheckbox);
        autoScreenOffCheckbox = view.findViewById(R.id.autoScreenOffCheckbox);
        showSystemSettingNamesSwitch = view.findViewById(R.id.showSystemSettingNamesSwitch);
        matchContentFrameRateRow = view.findViewById(R.id.matchContentFrameRateRow);
        matchContentFrameRateSpinner = view.findViewById(R.id.matchContentFrameRateSpinner);
        trackingSpeedSlider = view.findViewById(R.id.trackingSpeedSlider);
        disableUsbAudioTitle = view.findViewById(R.id.disableUsbAudioTitle);
        matchContentFrameRateTitle = view.findViewById(R.id.matchContentFrameRateTitle);
        forceDesktopTitle = view.findViewById(R.id.forceDesktopTitle);
        forceResizableTitle = view.findViewById(R.id.forceResizableTitle);
        enableFreeformTitle = view.findViewById(R.id.enableFreeformTitle);
        enableNonResizableTitle = view.findViewById(R.id.enableNonResizableTitle);
        disableScreenShareProtectionTitle = view.findViewById(R.id.disableScreenShareProtectionTitle);
        stayOnWhilePluggedTitle = view.findViewById(R.id.stayOnWhilePluggedTitle);

        boolean granted = PermissionManager.grant("android.permission.WRITE_SECURE_SETTINGS");
        _setupDisableScreenShareProtectionCheckbox();
        _setupForceDesktopCheckbox();
        _setupForceResizableCheckbox();
        _setupEnableFreeformCheckbox();
        _setupEnableNonResizableCheckbox();
        _setupDisableUsbAudioCheckbox();
        _setupMatchContentFrameRateRow();
        _setupUseRealScreenOffCheckbox();
        _setupStayOnWhilePluggedCheckbox();
        _setupAutoScreenOffCheckbox();
        _setupTrackingSpeedSlider();
        _setupShowSystemSettingNamesSwitch();
        _setupResetAllButton(view);
        if (!granted) {
            disableScreenShareProtectionCheckbox.setEnabled(false);
            forceDesktopCheckbox.setEnabled(false);
            forceResizableCheckbox.setEnabled(false);
            enableFreeformCheckbox.setEnabled(false);
            enableNonResizableCheckbox.setEnabled(false);
            disableUsbAudioCheckbox.setEnabled(false);
            stayOnWhilePluggedCheckbox.setEnabled(false);
            matchContentFrameRateSpinner.setEnabled(false);
            matchContentFrameRateRow.setAlpha(0.5f);
        }

        // About
        TextView versionText = view.findViewById(R.id.versionText);
        try {
            String ver = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0).versionName;
            versionText.setText(getString(R.string.version_format, ver, android.os.Build.VERSION.RELEASE));
        } catch (Exception e) {
            versionText.setText(R.string.version_unknown);
        }
        view.findViewById(R.id.websiteLink).setOnClickListener(v ->
            startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse("https://github.com/jqssun/android-display-extend"))));
        view.findViewById(R.id.shizukuBtn).setOnClickListener(v ->
            startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse("https://github.com/rikkaapps/shizuku"))));
        view.findViewById(R.id.exitBtn).setOnClickListener(v ->
            io.github.jqssun.displayextend.job.ExitAll.execute(requireActivity()));

        return view;
    }

    private void _setupAutoScreenOffCheckbox() {
        autoScreenOffCheckbox.setChecked(Pref.getAutoScreenOff());
        autoScreenOffCheckbox.setOnCheckedChangeListener((b, c) -> Pref.setAutoScreenOff(c));
    }

    private void _setupTrackingSpeedSlider() {
        trackingSpeedSlider.setValue(Math.max(
                TRACKING_SPEED_MIN,
                Math.min(TRACKING_SPEED_MAX, Pref.getTouchpadSensitivity())));
        trackingSpeedSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                Pref.setTouchpadSensitivity(value);
            }
        });
    }

    private void _setupShowSystemSettingNamesSwitch() {
        showSystemSettingNamesSwitch.setChecked(Pref.getShowSystemSettingNames());
        showSystemSettingNamesSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Pref.setShowSystemSettingNames(isChecked);
            _updateSystemSettingTitleLabels();
        });
        _updateSystemSettingTitleLabels();
    }

    private void _hideRow(View v) {
        ((View) v.getParent()).setVisibility(View.GONE);
    }

    private void _bindGlobalSetting(MaterialSwitch toggle, String key) {
        toggle.setChecked(Settings.Global.getInt(requireContext().getContentResolver(), key, 0) == 1);
        toggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            try {
                Settings.Global.putInt(requireContext().getContentResolver(), key, isChecked ? 1 : 0);
            } catch (SecurityException e) {
                State.log("failed: " + e);
            }
        });
    }

    private void _setupForceDesktopCheckbox() {
        _bindGlobalSetting(forceDesktopCheckbox, FORCE_DESKTOP_MODE_ON_EXTERNAL_DISPLAYS_KEY);
        boolean isHuawei = Build.MANUFACTURER.toLowerCase().contains("huawei") ||
                          Build.BRAND.toLowerCase().contains("huawei") ||
                          Build.DEVICE.toLowerCase().contains("huawei");
        if (isHuawei) {
            _hideRow(forceDesktopCheckbox);
        }
    }

    private void _setupForceResizableCheckbox() {
        _bindGlobalSetting(forceResizableCheckbox, FORCE_RESIZABLE_ACTIVITIES_KEY);
    }

    private void _setupEnableFreeformCheckbox() {
        _bindGlobalSetting(enableFreeformCheckbox, ENABLE_FREEFORM_SUPPORT_KEY);
    }

    private void _setupEnableNonResizableCheckbox() {
        _bindGlobalSetting(enableNonResizableCheckbox, ENABLE_NON_RESIZABLE_MULTI_WINDOW_KEY);
    }

    private void _setupDisableScreenShareProtectionCheckbox() {
        _bindGlobalSetting(disableScreenShareProtectionCheckbox, DISABLE_SCREEN_SHARE_PROTECTIONS_KEY);
    }

    private void _setupDisableUsbAudioCheckbox() {
        disableUsbAudioCheckbox.setChecked(Pref.getUsbAudioDisabled());

        disableUsbAudioCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            try {
                Settings.Secure.putInt(requireContext().getContentResolver(),
                        USB_AUDIO_AUTOMATIC_ROUTING_DISABLED_KEY, isChecked ? 1 : 0);
                Pref.setUsbAudioDisabled(isChecked);
            } catch (SecurityException e) {
                State.log("failed: " + e);
            }
        });
    }

    private void _setupMatchContentFrameRateRow() {
        Context context = requireContext();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                context,
                android.R.layout.simple_spinner_item,
                new String[] {
                        getString(R.string.match_content_frame_rate_never),
                        getString(R.string.match_content_frame_rate_seamless),
                        getString(R.string.match_content_frame_rate_always)
                }
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        matchContentFrameRateSpinner.setAdapter(adapter);
        matchContentFrameRateSpinner.setOnItemSelectedListener(null);
        matchContentFrameRateSpinner.setSelection(
                _coerceMatchContentFrameRateValue(_getMatchContentFrameRateValue()),
                false
        );

        final boolean[] initialized = {false};
        matchContentFrameRateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!initialized[0]) {
                    initialized[0] = true;
                    return;
                }
                _setMatchContentFrameRateValue(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // no-op
            }
        });
    }

    private void _setupUseRealScreenOffCheckbox() {
        useRealScreenOffCheckbox.setChecked(Pref.getUseRealScreenOff());

        useRealScreenOffCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Pref.setUseRealScreenOff(isChecked);
        });
    }

    private void _setupStayOnWhilePluggedCheckbox() {
        boolean isStayOnWhilePlugged = Settings.Global.getInt(requireContext().getContentResolver(),
                STAY_ON_WHILE_PLUGGED_IN_KEY, 0) != 0;
        stayOnWhilePluggedCheckbox.setChecked(isStayOnWhilePlugged);

        stayOnWhilePluggedCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            try {
                Settings.Global.putInt(requireContext().getContentResolver(),
                        STAY_ON_WHILE_PLUGGED_IN_KEY, isChecked ? 7 : 0);
            } catch (SecurityException e) {
                State.log("failed: " + e);
            }
        });
    }

    private void _updateSystemSettingTitleLabels() {
        boolean showRaw = Pref.getShowSystemSettingNames();
        _setSystemSettingTitle(disableUsbAudioTitle, R.string.disable_usb_audio, showRaw,
                _secureSettingTitle(USB_AUDIO_AUTOMATIC_ROUTING_DISABLED_KEY));
        _setSystemSettingTitle(matchContentFrameRateTitle, R.string.match_content_frame_rate,
                showRaw, _secureSettingTitle(MATCH_CONTENT_FRAME_RATE_KEY));
        _setSystemSettingTitle(forceDesktopTitle, R.string.force_desktop_mode, showRaw,
                _globalSettingTitle(FORCE_DESKTOP_MODE_ON_EXTERNAL_DISPLAYS_KEY));
        _setSystemSettingTitle(forceResizableTitle, R.string.force_resizable, showRaw,
                _globalSettingTitle(FORCE_RESIZABLE_ACTIVITIES_KEY));
        _setSystemSettingTitle(enableFreeformTitle, R.string.enable_freeform, showRaw,
                _globalSettingTitle(ENABLE_FREEFORM_SUPPORT_KEY));
        _setSystemSettingTitle(enableNonResizableTitle, R.string.enable_non_resizable_multiwindow,
                showRaw, _globalSettingTitle(ENABLE_NON_RESIZABLE_MULTI_WINDOW_KEY));
        _setSystemSettingTitle(disableScreenShareProtectionTitle,
                R.string.disable_screen_share_protection, showRaw,
                _globalSettingTitle(DISABLE_SCREEN_SHARE_PROTECTIONS_KEY));
        _setSystemSettingTitle(stayOnWhilePluggedTitle, R.string.stay_on_while_plugged, showRaw,
                _globalSettingTitle(STAY_ON_WHILE_PLUGGED_IN_KEY));
    }

    private void _setSystemSettingTitle(TextView textView, int friendlyResId, boolean showRaw,
                                        String rawTitle) {
        textView.setText(showRaw ? rawTitle : getString(friendlyResId));
    }

    private String _secureSettingTitle(String key) {
        return "Settings.Secure." + key;
    }

    private String _globalSettingTitle(String key) {
        return "Settings.Global." + key;
    }

    private void _setupResetAllButton(View root) {
        root.findViewById(R.id.resetAllBtn).setOnClickListener(v ->
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.reset_all_settings)
                        .setMessage(R.string.reset_all_settings_confirm)
                        .setPositiveButton(R.string.reset, (dialog, which) -> _resetAllToStock())
                        .setNegativeButton(R.string.cancel, null)
                        .show());
    }

    private void _resetAllToStock() {
        Context context = requireContext();

        _putGlobalInt(FORCE_DESKTOP_MODE_ON_EXTERNAL_DISPLAYS_KEY, 0);
        _putGlobalInt(FORCE_RESIZABLE_ACTIVITIES_KEY, 0);
        _putGlobalInt(ENABLE_FREEFORM_SUPPORT_KEY, 0);
        _putGlobalInt(ENABLE_NON_RESIZABLE_MULTI_WINDOW_KEY, 0);
        _putGlobalInt(DISABLE_SCREEN_SHARE_PROTECTIONS_KEY, 0);
        _putGlobalInt(STAY_ON_WHILE_PLUGGED_IN_KEY, 0);
        _setMatchContentFrameRateValue(0);
        _putSecureInt(USB_AUDIO_AUTOMATIC_ROUTING_DISABLED_KEY, 0);

        Pref.clearAll();
        _stopActiveFeatures(context);
        _resetConnectedDisplayConfigs(context);
        _refreshControlsFromState();
        State.refreshUI();
        State.log("reset app settings and connected display overrides to stock state");
    }

    private void _stopActiveFeatures(Context context) {
        if (State.mediaProjectionInUse != null) {
            State.mediaProjectionInUse.stop();
            State.mediaProjectionInUse = null;
        }
        State.setMediaProjection(null);
        MediaProjectionService.isStarting = false;
        context.stopService(new Intent(context, MediaProjectionService.class));
        context.stopService(new Intent(context, FloatingButtonService.class));

        Intent touchpadIntent = new Intent(context, TouchpadAccessibilityService.class);
        touchpadIntent.setAction(TouchpadAccessibilityService.class.getName());
        context.stopService(touchpadIntent);

        if (ManagedVirtualDisplayActivity.getInstance() != null) {
            ManagedVirtualDisplayActivity.getInstance().finish();
        }
        ManagedVirtualDisplayActivity.stopVirtualDisplay();
        if (State.mirrorVirtualDisplay != null) {
            State.mirrorVirtualDisplay.release();
            State.mirrorVirtualDisplay = null;
        }
        State.managedVirtualDisplayHostDisplayId = -1;
        State.mirrorDisplayId = -1;
        State.lastSingleAppDisplay = 0;

        if (State.isInPureBlackActivity != null) {
            State.isInPureBlackActivity.finish();
        }
        if (State.userService != null) {
            try {
                State.userService.stopListenVolumeKey();
                State.userService.setScreenPower(SurfaceControl.POWER_MODE_NORMAL);
            } catch (RemoteException e) {
                State.log("failed to restore screen power: " + e.getMessage());
            }
        }
    }

    private void _resetConnectedDisplayConfigs(Context context) {
        if (!ShizukuUtils.hasPermission()) {
            return;
        }
        DisplayManager displayManager = context.getSystemService(DisplayManager.class);
        if (displayManager == null) {
            return;
        }
        IWindowManager windowManager = ServiceUtils.getWindowManager();
        for (Display display : displayManager.getDisplays()) {
            int displayId = display.getDisplayId();
            if (displayId == Display.DEFAULT_DISPLAY) {
                continue;
            }

            try {
                windowManager.clearForcedDisplaySize(displayId);
            } catch (Throwable e) {
                State.log("failed to clear size for display " + displayId + ": " + e.getMessage());
            }

            try {
                windowManager.clearForcedDisplayDensityForUser(displayId, 0);
            } catch (Throwable e) {
                State.log("failed to clear density for display " + displayId + ": " + e.getMessage());
            }

            try {
                ServiceUtils.resetUserPreferredDisplayMode(displayId);
            } catch (Throwable e) {
                State.log("failed to clear preferred mode for display " + displayId + ": " + e.getMessage());
            }

            try {
                windowManager.setIgnoreOrientationRequest(displayId, false);
            } catch (Throwable e) {
                State.log("failed to clear orientation request for display " + displayId + ": " + e.getMessage());
            }

            try {
                windowManager.setFixedToUserRotation(displayId, 0);
            } catch (Throwable e) {
                State.log("failed to reset fixed rotation for display " + displayId + ": " + e.getMessage());
            }

            try {
                windowManager.thawDisplayRotation(displayId, "WindowManagerShellCommand#free");
            } catch (Throwable e) {
                try {
                    windowManager.thawDisplayRotation(displayId);
                } catch (Throwable e2) {
                    State.log("failed to thaw rotation for display " + displayId + ": " + e2.getMessage());
                }
            }
        }
    }

    private void _refreshControlsFromState() {
        forceDesktopCheckbox.setOnCheckedChangeListener(null);
        forceResizableCheckbox.setOnCheckedChangeListener(null);
        enableFreeformCheckbox.setOnCheckedChangeListener(null);
        enableNonResizableCheckbox.setOnCheckedChangeListener(null);
        disableScreenShareProtectionCheckbox.setOnCheckedChangeListener(null);
        disableUsbAudioCheckbox.setOnCheckedChangeListener(null);
        useRealScreenOffCheckbox.setOnCheckedChangeListener(null);
        stayOnWhilePluggedCheckbox.setOnCheckedChangeListener(null);
        autoScreenOffCheckbox.setOnCheckedChangeListener(null);
        showSystemSettingNamesSwitch.setOnCheckedChangeListener(null);
        matchContentFrameRateSpinner.setOnItemSelectedListener(null);
        trackingSpeedSlider.clearOnChangeListeners();

        _setupDisableScreenShareProtectionCheckbox();
        _setupForceDesktopCheckbox();
        _setupForceResizableCheckbox();
        _setupEnableFreeformCheckbox();
        _setupEnableNonResizableCheckbox();
        _setupDisableUsbAudioCheckbox();
        _setupMatchContentFrameRateRow();
        _setupUseRealScreenOffCheckbox();
        _setupStayOnWhilePluggedCheckbox();
        _setupAutoScreenOffCheckbox();
        _setupTrackingSpeedSlider();
        _setupShowSystemSettingNamesSwitch();
    }

    private int _getMatchContentFrameRateValue() {
        if (ShizukuUtils.hasPermission()) {
            try {
                return ServiceUtils.getRefreshRateSwitchingType();
            } catch (Throwable e) {
                State.log("failed to read match content frame rate preference via Shizuku: "
                        + e.getMessage());
            }
        }
        DisplayManager displayManager = requireContext().getSystemService(DisplayManager.class);
        if (displayManager == null) {
            return 1;
        }
        try {
            DisplayManagerHidden displayManagerHidden = Refine.unsafeCast(displayManager);
            return displayManagerHidden.getMatchContentFrameRateUserPreference();
        } catch (Throwable e) {
            State.log("failed to read match content frame rate preference: " + e.getMessage());
        }
        return 1;
    }

    private void _setMatchContentFrameRateValue(int value) {
        if (ShizukuUtils.hasPermission()) {
            try {
                ServiceUtils.setRefreshRateSwitchingType(value);
                return;
            } catch (Throwable e) {
                State.log("failed to set match content frame rate preference via Shizuku: "
                        + e.getMessage());
            }
        }
        _putSecureInt(MATCH_CONTENT_FRAME_RATE_KEY, value);
    }

    private int _coerceMatchContentFrameRateValue(int value) {
        if (value < 0 || value > 2) {
            return 1;
        }
        return value;
    }

    private void _putGlobalInt(String key, int value) {
        try {
            Settings.Global.putInt(requireContext().getContentResolver(), key, value);
        } catch (SecurityException e) {
            State.log("failed to reset global setting " + key + ": " + e.getMessage());
        }
    }

    private void _putSecureInt(String key, int value) {
        try {
            Settings.Secure.putInt(requireContext().getContentResolver(), key, value);
        } catch (SecurityException e) {
            State.log("failed to reset secure setting " + key + ": " + e.getMessage());
        }
    }
}
