package io.github.jqssun.displayextend;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Display;
import android.view.InputDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import io.github.jqssun.displayextend.job.BindInputToDisplay;
import io.github.jqssun.displayextend.shizuku.ShizukuUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InputDeviceDetailFragment extends Fragment {
    private static final String ARG_DEVICE_ID = "device_id";
    private InputDevice device;
    private List<Display> displayList;
    private Spinner displaySpinner;
    private com.google.android.material.button.MaterialButton bindBtn;

    public static InputDeviceDetailFragment newInstance(int deviceId) {
        InputDeviceDetailFragment fragment = new InputDeviceDetailFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_DEVICE_ID, deviceId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEnterTransition(new com.google.android.material.transition.MaterialSharedAxis(com.google.android.material.transition.MaterialSharedAxis.X, true));
        setReturnTransition(new com.google.android.material.transition.MaterialSharedAxis(com.google.android.material.transition.MaterialSharedAxis.X, false));
        if (getArguments() != null) {
            int deviceId = getArguments().getInt(ARG_DEVICE_ID);
            device = InputDevice.getDevice(deviceId);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_input_device_detail, container, false);

        LinearLayout infoTable = view.findViewById(R.id.infoTable);

        if (device != null) {
            Context ctx = requireContext();
            InfoRow.add(ctx, infoTable, getString(R.string.info_device_name), device.getName());
            InfoRow.add(ctx, infoTable, getString(R.string.info_device_id), String.valueOf(device.getId()));
            InfoRow.add(ctx, infoTable, getString(R.string.info_product_id), String.format("0x%04X", device.getProductId()));
            InfoRow.add(ctx, infoTable, getString(R.string.info_vendor_id), String.format("0x%04X", device.getVendorId()));
            InfoRow.add(ctx, infoTable, getString(R.string.info_descriptor), device.getDescriptor());
            InfoRow.add(ctx, infoTable, getString(R.string.info_external), PlatformCompat.isExternalInputDevice(device) ? getString(R.string.yes) : getString(R.string.no));
            InfoRow.add(ctx, infoTable, getString(R.string.info_input_sources), _getDeviceSources(device));
        }

        displaySpinner = view.findViewById(R.id.displaySpinner);
        bindBtn = view.findViewById(R.id.bindBtn);

        _initDisplaySpinner();
        _setupBindButton();

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
                State.startNewJob(new BindInputToDisplay(device, selectedDisplay));
            }
        });
    }

    private String _getDeviceSources(InputDevice device) {
        List<String> sources = new ArrayList<>();

        if ((device.getSources() & InputDevice.SOURCE_KEYBOARD) != 0) sources.add(getString(R.string.source_keyboard));
        if ((device.getSources() & InputDevice.SOURCE_DPAD) != 0) sources.add("D-pad");
        if ((device.getSources() & InputDevice.SOURCE_GAMEPAD) != 0) sources.add(getString(R.string.source_gamepad));
        if ((device.getSources() & InputDevice.SOURCE_TOUCHSCREEN) != 0) sources.add(getString(R.string.source_touchscreen));
        if ((device.getSources() & InputDevice.SOURCE_MOUSE) != 0) sources.add(getString(R.string.source_mouse));

        return TextUtils.join(", ", sources);
    }
}
