package io.github.jqssun.displayextend;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.hardware.input.InputManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Display;
import android.view.InputDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.materialswitch.MaterialSwitch;

import io.github.jqssun.displayextend.job.BindAllExternalInputToDisplay;
import io.github.jqssun.displayextend.shizuku.PermissionManager;
import io.github.jqssun.displayextend.shizuku.ShizukuUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SettingsFragment extends Fragment {
    private List<Display> displayList;
    private Spinner displaySpinner;
    private View bindBtn;
    private RecyclerView externalDevicesRecycler;
    private RecyclerView internalDevicesRecycler;
    private MaterialSwitch forceDesktopCheckbox;
    private MaterialSwitch forceResizableCheckbox;
    private MaterialSwitch enableFreeformCheckbox;
    private MaterialSwitch enableNonResizableCheckbox;
    private MaterialSwitch disableScreenShareProtectionCheckbox;
    private MaterialSwitch disableUsbAudioCheckbox;
    private MaterialSwitch useRealScreenOffCheckbox;
    private MaterialSwitch stayOnWhilePluggedCheckbox;
    private View externalDeviceContainer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        forceDesktopCheckbox = view.findViewById(R.id.forceDesktopCheckbox);
        forceResizableCheckbox = view.findViewById(R.id.forceResizableCheckbox);
        enableFreeformCheckbox = view.findViewById(R.id.enableFreeformCheckbox);
        enableNonResizableCheckbox = view.findViewById(R.id.enableNonResizableCheckbox);
        disableScreenShareProtectionCheckbox = view.findViewById(R.id.disableScreenShareProtectionCheckbox);
        displaySpinner = view.findViewById(R.id.displaySpinner);
        bindBtn = view.findViewById(R.id.bindBtn);
        externalDevicesRecycler = view.findViewById(R.id.externalDevicesRecycler);
        internalDevicesRecycler = view.findViewById(R.id.internalDevicesRecycler);
        disableUsbAudioCheckbox = view.findViewById(R.id.disableUsbAudioCheckbox);
        useRealScreenOffCheckbox = view.findViewById(R.id.useRealScreenOffCheckbox);
        stayOnWhilePluggedCheckbox = view.findViewById(R.id.stayOnWhilePluggedCheckbox);
        externalDeviceContainer = view.findViewById(R.id.externalDeviceContainer);

        _initDisplaySpinner();
        _setupBindButton();
        _setupDeviceLists();

        boolean granted = PermissionManager.grant("android.permission.WRITE_SECURE_SETTINGS");
        _setupDisableScreenShareProtectionCheckbox();
        _setupForceDesktopCheckbox();
        _setupForceResizableCheckbox();
        _setupEnableFreeformCheckbox();
        _setupEnableNonResizableCheckbox();
        _setupDisableUsbAudioCheckbox();
        _setupUseRealScreenOffCheckbox();
        _setupStayOnWhilePluggedCheckbox();
        _setupAutoScreenOffCheckbox(view);
        _setupAutoRouteInputCheckbox(view);
        if (!granted) {
            disableScreenShareProtectionCheckbox.setEnabled(false);
            forceDesktopCheckbox.setEnabled(false);
            forceResizableCheckbox.setEnabled(false);
            enableFreeformCheckbox.setEnabled(false);
            enableNonResizableCheckbox.setEnabled(false);
            disableUsbAudioCheckbox.setEnabled(false);
            stayOnWhilePluggedCheckbox.setEnabled(false);
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
                android.net.Uri.parse("https://github.com/jqssun/android-screen-extend"))));
        view.findViewById(R.id.shizukuBtn).setOnClickListener(v ->
            startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse("https://github.com/rikkaapps/shizuku"))));
        view.findViewById(R.id.exitBtn).setOnClickListener(v ->
            io.github.jqssun.displayextend.job.ExitAll.execute(requireActivity()));

        return view;
    }

    private void _initDisplaySpinner() {
        DisplayManager displayManager = (DisplayManager) requireContext().getSystemService(Context.DISPLAY_SERVICE);
        Display[] displays = displayManager.getDisplays();
        displayList = Arrays.asList(displays);

        List<String> displayNames = new ArrayList<>();
        for (Display display : displays) {
            displayNames.add(getString(R.string.display_spinner_format, display.getDisplayId(), display.getName()));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            displayNames
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        displaySpinner.setAdapter(adapter);
    }

    private void _setupBindButton() {
        bindBtn.setOnClickListener(v -> {
            if (!ShizukuUtils.hasShizukuStarted()) {
                Toast.makeText(requireContext(), getString(R.string.shizuku_required), Toast.LENGTH_SHORT).show();
                return;
            }
            int selectedPosition = displaySpinner.getSelectedItemPosition();
            if (selectedPosition != -1 && selectedPosition < displayList.size()) {
                Display selectedDisplay = displayList.get(selectedPosition);
                State.startNewJob(new BindAllExternalInputToDisplay(selectedDisplay.getDisplayId()));
            }
        });
    }

    private void _setupAutoScreenOffCheckbox(View root) {
        MaterialSwitch cb = root.findViewById(R.id.autoScreenOffCheckbox);
        cb.setChecked(Pref.getAutoScreenOff());
        cb.setOnCheckedChangeListener((b, c) -> Pref.setAutoScreenOff(c));
    }

    private void _setupAutoRouteInputCheckbox(View root) {
        com.google.android.material.materialswitch.MaterialSwitch cb = root.findViewById(R.id.autoRouteInputCheckbox);
        cb.setChecked(Pref.getAutoRouteInput());
        cb.setOnCheckedChangeListener((b, c) -> Pref.setAutoRouteInput(c));
    }

    private void _setupDeviceLists() {
        InputManager inputManager = (InputManager) requireContext().getSystemService(Context.INPUT_SERVICE);
        int[] deviceIds = inputManager.getInputDeviceIds();

        List<InputDevice> externalDevices = new ArrayList<>();
        List<InputDevice> internalDevices = new ArrayList<>();

        for (int deviceId : deviceIds) {
            InputDevice device = InputDevice.getDevice(deviceId);
            if (device != null) {
                if (device.isExternal()) {
                    externalDevices.add(device);
                } else {
                    internalDevices.add(device);
                }
            }
        }

        externalDeviceContainer.setVisibility(externalDevices.isEmpty() ? View.GONE : View.VISIBLE);

        externalDevicesRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        internalDevicesRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));

        DeviceAdapter externalAdapter = new DeviceAdapter(externalDevices, this::_showDeviceDetails);
        DeviceAdapter internalAdapter = new DeviceAdapter(internalDevices, this::_showDeviceDetails);

        externalDevicesRecycler.setAdapter(externalAdapter);
        internalDevicesRecycler.setAdapter(internalAdapter);
    }

    private void _showDeviceDetails(InputDevice device) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).navigateToInputDeviceDetail(device.getId());
        }
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
}
