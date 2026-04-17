package io.github.jqssun.displayextend;

import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManagerHidden;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.Settings;
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
    private static final String MATCH_CONTENT_FRAME_RATE_KEY = "match_content_frame_rate";

    private MaterialSwitch forceDesktopCheckbox;
    private MaterialSwitch forceResizableCheckbox;
    private MaterialSwitch enableFreeformCheckbox;
    private MaterialSwitch enableNonResizableCheckbox;
    private MaterialSwitch disableScreenShareProtectionCheckbox;
    private MaterialSwitch disableUsbAudioCheckbox;
    private MaterialSwitch useRealScreenOffCheckbox;
    private MaterialSwitch stayOnWhilePluggedCheckbox;
    private MaterialSwitch autoScreenOffCheckbox;
    private View matchContentFrameRateRow;
    private TextView matchContentFrameRateValueText;
    private Slider trackingSpeedSlider;

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
        matchContentFrameRateRow = view.findViewById(R.id.matchContentFrameRateRow);
        matchContentFrameRateValueText = view.findViewById(R.id.matchContentFrameRateValueText);
        trackingSpeedSlider = view.findViewById(R.id.trackingSpeedSlider);

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
        _setupResetAllButton(view);
        if (!granted) {
            disableScreenShareProtectionCheckbox.setEnabled(false);
            forceDesktopCheckbox.setEnabled(false);
            forceResizableCheckbox.setEnabled(false);
            enableFreeformCheckbox.setEnabled(false);
            enableNonResizableCheckbox.setEnabled(false);
            disableUsbAudioCheckbox.setEnabled(false);
            stayOnWhilePluggedCheckbox.setEnabled(false);
            matchContentFrameRateRow.setEnabled(false);
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
        trackingSpeedSlider.setValue(Math.max(0.5f, Math.min(3.0f, Pref.getTouchpadSensitivity())));
        trackingSpeedSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                Pref.setTouchpadSensitivity(value);
            }
        });
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
        _bindGlobalSetting(forceDesktopCheckbox, "force_desktop_mode_on_external_displays");
        boolean isHuawei = Build.MANUFACTURER.toLowerCase().contains("huawei") ||
                          Build.BRAND.toLowerCase().contains("huawei") ||
                          Build.DEVICE.toLowerCase().contains("huawei");
        if (isHuawei) {
            _hideRow(forceDesktopCheckbox);
        }
    }

    private void _setupForceResizableCheckbox() {
        _bindGlobalSetting(forceResizableCheckbox, "force_resizable_activities");
    }

    private void _setupEnableFreeformCheckbox() {
        _bindGlobalSetting(enableFreeformCheckbox, "enable_freeform_support");
    }

    private void _setupEnableNonResizableCheckbox() {
        _bindGlobalSetting(enableNonResizableCheckbox, "enable_non_resizable_multi_window");
    }

    private void _setupDisableScreenShareProtectionCheckbox() {
        _bindGlobalSetting(disableScreenShareProtectionCheckbox, "disable_screen_share_protections_for_apps_and_notifications");
    }

    private void _setupDisableUsbAudioCheckbox() {
        disableUsbAudioCheckbox.setChecked(Pref.getUsbAudioDisabled());

        disableUsbAudioCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            try {
                Settings.Secure.putInt(requireContext().getContentResolver(),
                        "usb_audio_automatic_routing_disabled", isChecked ? 1 : 0);
                Pref.setUsbAudioDisabled(isChecked);
            } catch (SecurityException e) {
                State.log("failed: " + e);
            }
        });
    }

    private void _setupMatchContentFrameRateRow() {
        _updateMatchContentFrameRateSummary(_getMatchContentFrameRateValue());
        matchContentFrameRateRow.setOnClickListener(v -> {
            int currentValue = _getMatchContentFrameRateValue();
            CharSequence[] options = new CharSequence[] {
                    getString(R.string.match_content_frame_rate_never),
                    getString(R.string.match_content_frame_rate_seamless),
                    getString(R.string.match_content_frame_rate_always)
            };
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.match_content_frame_rate)
                    .setSingleChoiceItems(options, _coerceMatchContentFrameRateValue(currentValue),
                            (dialog, which) -> {
                                _setMatchContentFrameRateValue(which);
                                _updateMatchContentFrameRateSummary(which);
                                dialog.dismiss();
                            })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
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
                "stay_on_while_plugged_in", 0) != 0;
        stayOnWhilePluggedCheckbox.setChecked(isStayOnWhilePlugged);

        stayOnWhilePluggedCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            try {
                Settings.Global.putInt(requireContext().getContentResolver(),
                        "stay_on_while_plugged_in", isChecked ? 7 : 0);
            } catch (SecurityException e) {
                State.log("failed: " + e);
            }
        });
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

        _putGlobalInt("force_desktop_mode_on_external_displays", 0);
        _putGlobalInt("force_resizable_activities", 0);
        _putGlobalInt("enable_freeform_support", 0);
        _putGlobalInt("enable_non_resizable_multi_window", 0);
        _putGlobalInt("disable_screen_share_protections_for_apps_and_notifications", 0);
        _putGlobalInt("stay_on_while_plugged_in", 0);
        _setMatchContentFrameRateValue(0);
        _putSecureInt("usb_audio_automatic_routing_disabled", 0);

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

    private void _updateMatchContentFrameRateSummary(int value) {
        int labelRes;
        switch (_coerceMatchContentFrameRateValue(value)) {
            case 0:
                labelRes = R.string.match_content_frame_rate_never;
                break;
            case 2:
                labelRes = R.string.match_content_frame_rate_always;
                break;
            case 1:
            default:
                labelRes = R.string.match_content_frame_rate_seamless;
                break;
        }
        matchContentFrameRateValueText.setText(labelRes);
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
