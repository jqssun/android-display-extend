package io.github.jqssun.displayextend;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Display;
import android.view.InputDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.hardware.input.InputManager;
import android.hardware.display.DisplayManager;

import io.github.jqssun.displayextend.job.BindAllExternalInputToDisplay;
import io.github.jqssun.displayextend.shizuku.PermissionManager;
import io.github.jqssun.displayextend.shizuku.ShizukuUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SettingsFragment extends Fragment {
    private List<Display> displayList;
    private Spinner spinnerDisplays;
    private Button btnBind;
    private RecyclerView rvExternalDevices;
    private RecyclerView rvInternalDevices;
    private CheckBox cbForceDesktop;
    private CheckBox cbForceResizable;
    private CheckBox cbEnableFreeform;
    private CheckBox cbEnableNonResizable;
    private CheckBox cbDisableScreenShareProtection;
    private CheckBox cbDisableUsbAudio;
    private CheckBox cbUseRealScreenOff;
    private CheckBox cbStayOnWhilePlugged;
    private View externalDeviceContainer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        
        cbForceDesktop = view.findViewById(R.id.cbForceDesktop);
        cbForceResizable = view.findViewById(R.id.cbForceResizable);
        cbEnableFreeform = view.findViewById(R.id.cbEnableFreeform);
        cbEnableNonResizable = view.findViewById(R.id.cbEnableNonResizable);
        cbDisableScreenShareProtection = view.findViewById(R.id.cbDisableScreenShareProtection);
        spinnerDisplays = view.findViewById(R.id.spinnerDisplays);
        btnBind = view.findViewById(R.id.btnBind);
        rvExternalDevices = view.findViewById(R.id.rvExternalDevices);
        rvInternalDevices = view.findViewById(R.id.rvInternalDevices);
        cbDisableUsbAudio = view.findViewById(R.id.cbDisableUsbAudio);
        cbUseRealScreenOff = view.findViewById(R.id.cbUseRealScreenOff);
        cbStayOnWhilePlugged = view.findViewById(R.id.cbStayOnWhilePlugged);
        externalDeviceContainer = view.findViewById(R.id.externalDeviceContainer);
        
        initializeDisplaySpinner();
        setupBindButton();
        setupDeviceLists();

        if (PermissionManager.grant("android.permission.WRITE_SECURE_SETTINGS")) {
            setupDisableScreenShareProtectionCheckbox();
            setupForceDesktopCheckbox();
            setupForceResizableCheckbox();
            setupEnableFreeformCheckbox();
            setupEnableNonResizableCheckbox();
            setupDisableUsbAudioCheckbox();
            setupUseRealScreenOffCheckbox();
            setupStayOnWhilePluggedCheckbox();
        } else {
            cbDisableScreenShareProtection.setVisibility(View.GONE);
            cbForceDesktop.setVisibility(View.GONE);
            cbForceResizable.setVisibility(View.GONE);
            cbEnableFreeform.setVisibility(View.GONE);
            cbEnableNonResizable.setVisibility(View.GONE);
            cbDisableUsbAudio.setVisibility(View.GONE);
            cbStayOnWhilePlugged.setVisibility(View.GONE);
        }
        
        return view;
    }

    private void initializeDisplaySpinner() {
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
        spinnerDisplays.setAdapter(adapter);
    }

    private void setupBindButton() {
        btnBind.setOnClickListener(v -> {
            if (!ShizukuUtils.hasShizukuStarted()) {
                Toast.makeText(requireContext(), getString(R.string.shizuku_required), Toast.LENGTH_SHORT).show();
                return;
            }
            int selectedPosition = spinnerDisplays.getSelectedItemPosition();
            if (selectedPosition != -1 && selectedPosition < displayList.size()) {
                Display selectedDisplay = displayList.get(selectedPosition);
                State.startNewJob(new BindAllExternalInputToDisplay(selectedDisplay.getDisplayId()));
            }
        });
    }

    private void setupDeviceLists() {
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
        
        rvExternalDevices.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvInternalDevices.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        DeviceAdapter externalAdapter = new DeviceAdapter(externalDevices, this::showDeviceDetails);
        DeviceAdapter internalAdapter = new DeviceAdapter(internalDevices, this::showDeviceDetails);
        
        rvExternalDevices.setAdapter(externalAdapter);
        rvInternalDevices.setAdapter(internalAdapter);
    }

    private void showDeviceDetails(InputDevice device) {
        State.breadcrumbManager.pushBreadcrumb(device.getName(), () ->
        InputDeviceDetailFragment.newInstance(device.getId())
        );
    }

    private void setupForceDesktopCheckbox() {
        boolean isForceDesktop = Settings.Global.getInt(requireContext().getContentResolver(),
                "force_desktop_mode_on_external_displays", 0) == 1;
        cbForceDesktop.setChecked(isForceDesktop);
        boolean isHuawei = Build.MANUFACTURER.toLowerCase().contains("huawei") ||
                          Build.BRAND.toLowerCase().contains("huawei") ||
                          Build.DEVICE.toLowerCase().contains("huawei");
        
        if (isHuawei) {
            // disabled on huawei devices
            cbForceDesktop.setVisibility(View.GONE);
        }

        cbForceDesktop.setOnCheckedChangeListener((buttonView, isChecked) -> {
            try {
                Settings.Global.putInt(requireContext().getContentResolver(),
                        "force_desktop_mode_on_external_displays", isChecked ? 1 : 0);
                        
                Settings.Global.putInt(requireContext().getContentResolver(),
                "force_desktop_mode_on_external_displays", isChecked ? 1 : 0);
            } catch (SecurityException e) {
                State.log("failed: " + e);
            }
        });
    }

    private void setupForceResizableCheckbox() {
        boolean isForceResizable = Settings.Global.getInt(requireContext().getContentResolver(),
                "force_resizable_activities", 0) == 1;
        cbForceResizable.setChecked(isForceResizable);

        cbForceResizable.setOnCheckedChangeListener((buttonView, isChecked) -> {
            try {
                Settings.Global.putInt(requireContext().getContentResolver(),
                        "force_resizable_activities", isChecked ? 1 : 0);
            } catch (SecurityException e) {
                State.log("failed: " + e);
            }
        });
    }

    private void setupEnableFreeformCheckbox() {
        boolean isEnableFreeform = Settings.Global.getInt(requireContext().getContentResolver(),
                "enable_freeform_support", 0) == 1;
        cbEnableFreeform.setChecked(isEnableFreeform);

        cbEnableFreeform.setOnCheckedChangeListener((buttonView, isChecked) -> {
            try {
                Settings.Global.putInt(requireContext().getContentResolver(),
                        "enable_freeform_support", isChecked ? 1 : 0);
            } catch (SecurityException e) {
                State.log("failed: " + e);
            }
        });
    }

    private void setupEnableNonResizableCheckbox() {
        boolean isEnableNonResizable = Settings.Global.getInt(requireContext().getContentResolver(),
                "enable_non_resizable_multi_window", 0) == 1;
        cbEnableNonResizable.setChecked(isEnableNonResizable);

        cbEnableNonResizable.setOnCheckedChangeListener((buttonView, isChecked) -> {
            try {
                Settings.Global.putInt(requireContext().getContentResolver(),
                        "enable_non_resizable_multi_window", isChecked ? 1 : 0);
            } catch (SecurityException e) {
                State.log("failed: " + e);
            }
        });
    }

    private void setupDisableScreenShareProtectionCheckbox() {
        boolean isDisabled = Settings.Global.getInt(requireContext().getContentResolver(),
                "disable_screen_share_protections_for_apps_and_notifications", 0) == 1;
        cbDisableScreenShareProtection.setChecked(isDisabled);

        cbDisableScreenShareProtection.setOnCheckedChangeListener((buttonView, isChecked) -> {
            try {
                Settings.Global.putInt(requireContext().getContentResolver(),
                        "disable_screen_share_protections_for_apps_and_notifications", isChecked ? 1 : 0);
            } catch (SecurityException e) {
                State.log("failed: " + e);
            }
        });
    }

    private void setupDisableUsbAudioCheckbox() {
        boolean isDisabled = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
                .getBoolean("usb_audio_disabled", false);
                
        cbDisableUsbAudio.setChecked(isDisabled);

        cbDisableUsbAudio.setOnCheckedChangeListener((buttonView, isChecked) -> {
            try {
                Settings.Secure.putInt(requireContext().getContentResolver(),
                        "usb_audio_automatic_routing_disabled", isChecked ? 1 : 0);

                requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
                        .edit()
                        .putBoolean("usb_audio_disabled", isChecked)
                        .apply();
            } catch (SecurityException e) {
                State.log("failed: " + e);
            }
        });
    }

    private void setupUseRealScreenOffCheckbox() {
        boolean useRealScreenOff = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
                .getBoolean("use_real_screen_off", false);
                
        cbUseRealScreenOff.setChecked(useRealScreenOff);

        cbUseRealScreenOff.setOnCheckedChangeListener((buttonView, isChecked) -> {
            requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("use_real_screen_off", isChecked)
                    .apply();
        });
    }

    private void setupStayOnWhilePluggedCheckbox() {
        boolean isStayOnWhilePlugged = Settings.Global.getInt(requireContext().getContentResolver(),
                "stay_on_while_plugged_in", 0) != 0;
        cbStayOnWhilePlugged.setChecked(isStayOnWhilePlugged);

        cbStayOnWhilePlugged.setOnCheckedChangeListener((buttonView, isChecked) -> {
            try {
                // set to 7 to always keep screen when charging
                // a/c: 1, usb: 2, wireless: 4
                Settings.Global.putInt(requireContext().getContentResolver(),
                        "stay_on_while_plugged_in", isChecked ? 7 : 0);
            } catch (SecurityException e) {
                State.log("failed: " + e);
            }
        });
    }
}