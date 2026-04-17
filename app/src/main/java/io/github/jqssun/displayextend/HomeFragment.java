package io.github.jqssun.displayextend;

import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.transition.MaterialSharedAxis;

import io.github.jqssun.displayextend.job.AcquireShizuku;

public class HomeFragment extends Fragment {

    private TextView shizukuStatus;
    private MaterialButton shizukuPermissionBtn;
    private TextView displaysStatus;
    private TextView touchpadStatus;
    private TextView inputBindingStatus;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setExitTransition(new MaterialSharedAxis(MaterialSharedAxis.X, true));
        setReenterTransition(new MaterialSharedAxis(MaterialSharedAxis.X, false));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        TextView shizukuStatusPrefix = view.findViewById(R.id.shizukuStatusPrefix);
        shizukuStatusPrefix.setOnClickListener(v ->
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/rikkaapps/shizuku")))
        );

        shizukuPermissionBtn = view.findViewById(R.id.shizukuPermissionBtn);
        shizukuPermissionBtn.setOnClickListener(v -> State.startNewJob(new AcquireShizuku()));

        shizukuStatus = view.findViewById(R.id.shizukuStatus);
        displaysStatus = view.findViewById(R.id.displaysStatus);
        touchpadStatus = view.findViewById(R.id.touchpadStatus);
        inputBindingStatus = view.findViewById(R.id.inputBindingStatus);

        view.findViewById(R.id.displaysRow).setOnClickListener(v ->
            Navigation.findNavController(v).navigate(R.id.action_overview_to_screens));
        view.findViewById(R.id.touchpadRow).setOnClickListener(v ->
            Navigation.findNavController(v).navigate(R.id.action_overview_to_touchpad));
        view.findViewById(R.id.inputBindingRow).setOnClickListener(v ->
            Navigation.findNavController(v).navigate(R.id.action_overview_to_input_binding));

        State.uiState.observe(getViewLifecycleOwner(), this::_updateUI);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        State.refreshUI();
        _updateDisplaysStatus();
        _updateTouchpadStatus();
        _updateInputBindingStatus();
    }

    private void _updateUI(ExtendUiState state) {
        if (state == null) return;

        if (state.shizukuStatus != null) {
            shizukuStatus.setText(state.shizukuStatus);
        }
        shizukuPermissionBtn.setVisibility(state.shizukuPermissionVisible ? View.VISIBLE : View.GONE);

        _updateDisplaysStatus();
        _updateTouchpadStatus();
        _updateInputBindingStatus();
    }

    private void _updateDisplaysStatus() {
        if (displaysStatus == null) return;
        DisplayManager dm = (DisplayManager) requireContext().getSystemService(Context.DISPLAY_SERVICE);
        Display[] displays = dm.getDisplays();
        int count = 0;
        for (Display d : displays) {
            if (d.getDisplayId() != State.managedVirtualDisplayHostDisplayId
                    && d.getDisplayId() != State.mirrorDisplayId) {
                count++;
            }
        }
        displaysStatus.setText(getString(R.string.displays_status_format, count));
    }

    private void _updateTouchpadStatus() {
        if (touchpadStatus == null) return;
        int displayId = State.lastSingleAppDisplay;
        if (displayId <= 0) {
            touchpadStatus.setText(R.string.touchpad_no_cast);
        } else {
            DisplayManager dm = (DisplayManager) requireContext().getSystemService(Context.DISPLAY_SERVICE);
            Display display = dm.getDisplay(displayId);
            String name = display != null ? display.getName() : String.valueOf(displayId);
            touchpadStatus.setText(getString(R.string.target_display_format, displayId, name));
        }
    }

    private void _updateInputBindingStatus() {
        if (inputBindingStatus == null) return;
        inputBindingStatus.setText(Pref.getAutoBindInput()
                ? R.string.input_binding_status_auto
                : R.string.input_binding_status_manual);
    }
}
