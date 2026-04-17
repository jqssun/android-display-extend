package io.github.jqssun.displayextend;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.hardware.input.InputManager;
import android.os.Bundle;
import android.view.Display;
import android.view.InputDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.transition.MaterialSharedAxis;

import io.github.jqssun.displayextend.job.BindAllExternalInputToDisplay;
import io.github.jqssun.displayextend.shizuku.ShizukuUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InputBindingFragment extends Fragment {
    private List<Display> displayList;
    private Spinner displaySpinner;
    private View bindBtn;
    private RecyclerView externalDevicesRecycler;
    private RecyclerView internalDevicesRecycler;
    private View externalDeviceContainer;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEnterTransition(new MaterialSharedAxis(MaterialSharedAxis.X, true));
        setReturnTransition(new MaterialSharedAxis(MaterialSharedAxis.X, false));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_input_binding, container, false);

        displaySpinner = view.findViewById(R.id.displaySpinner);
        bindBtn = view.findViewById(R.id.bindBtn);
        externalDevicesRecycler = view.findViewById(R.id.externalDevicesRecycler);
        internalDevicesRecycler = view.findViewById(R.id.internalDevicesRecycler);
        externalDeviceContainer = view.findViewById(R.id.externalDeviceContainer);

        _setupAutoBindCheckbox(view);
        _initDisplaySpinner();
        _setupBindButton();
        _setupDeviceLists();

        return view;
    }

    private void _setupAutoBindCheckbox(View view) {
        MaterialSwitch cb = view.findViewById(R.id.autoBindInputCheckbox);
        cb.setChecked(Pref.getAutoBindInput());
        cb.setOnCheckedChangeListener((b, c) -> Pref.setAutoBindInput(c));
    }

    private void _initDisplaySpinner() {
        DisplayManager dm = (DisplayManager) requireContext().getSystemService(Context.DISPLAY_SERVICE);
        Display[] displays = dm.getDisplays();
        displayList = Arrays.asList(displays);

        List<String> names = new ArrayList<>();
        for (Display d : displays) {
            names.add(getString(R.string.display_spinner_format, d.getDisplayId(), d.getName()));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            requireContext(), android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        displaySpinner.setAdapter(adapter);
    }

    private void _setupBindButton() {
        bindBtn.setOnClickListener(v -> {
            if (!ShizukuUtils.hasShizukuStarted()) {
                Toast.makeText(requireContext(), R.string.shizuku_required, Toast.LENGTH_SHORT).show();
                return;
            }
            int pos = displaySpinner.getSelectedItemPosition();
            if (pos != -1 && pos < displayList.size()) {
                State.startNewJob(new BindAllExternalInputToDisplay(displayList.get(pos).getDisplayId()));
            }
        });
    }

    private void _setupDeviceLists() {
        InputManager im = (InputManager) requireContext().getSystemService(Context.INPUT_SERVICE);
        int[] ids = im.getInputDeviceIds();

        List<InputDevice> external = new ArrayList<>();
        List<InputDevice> internal = new ArrayList<>();

        for (int id : ids) {
            InputDevice device = InputDevice.getDevice(id);
                if (device != null) {
                if (PlatformCompat.isExternalInputDevice(device)) external.add(device);
                else internal.add(device);
            }
        }

        externalDeviceContainer.setVisibility(external.isEmpty() ? View.GONE : View.VISIBLE);

        externalDevicesRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        internalDevicesRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        externalDevicesRecycler.setAdapter(new DeviceAdapter(external, this::_showDeviceDetails));
        internalDevicesRecycler.setAdapter(new DeviceAdapter(internal, this::_showDeviceDetails));
    }

    private void _showDeviceDetails(InputDevice device) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).navigateToInputDeviceDetail(device.getId());
        }
    }
}
