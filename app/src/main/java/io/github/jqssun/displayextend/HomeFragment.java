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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.transition.MaterialSharedAxis;

import io.github.jqssun.displayextend.job.AcquireShizuku;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {
    private TextView shizukuStatus;
    private MaterialButton shizukuPermissionBtn;
    private TextView inputBindingStatus;
    private LinearLayout displaysContainer;

    private final DisplayManager.DisplayListener displayListener = new DisplayManager.DisplayListener() {
        @Override public void onDisplayAdded(int id) { _refreshDisplayList(); }
        @Override public void onDisplayRemoved(int id) { _refreshDisplayList(); }
        @Override public void onDisplayChanged(int id) { _refreshDisplayList(); }
    };

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
        inputBindingStatus = view.findViewById(R.id.inputBindingStatus);
        displaysContainer = view.findViewById(R.id.displaysContainer);

        view.findViewById(R.id.openCastBtn).setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setAction(android.provider.Settings.ACTION_CAST_SETTINGS);
            startActivity(intent);
        });
        view.findViewById(R.id.openMirrorOverviewBtn).setOnClickListener(v ->
                MirrorIntegrationHelper.openMirrorOverview(requireContext()));

        view.findViewById(R.id.screen_off_button).setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), PureBlackActivity.class);
            startActivity(intent);
        });

        view.findViewById(R.id.inputBindingRow).setOnClickListener(v ->
            Navigation.findNavController(v).navigate(R.id.action_overview_to_input_binding));

        State.uiState.observe(getViewLifecycleOwner(), this::_updateUI);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Context ctx = getContext();
        if (ctx != null) {
            ((DisplayManager) ctx.getSystemService(Context.DISPLAY_SERVICE))
                    .registerDisplayListener(displayListener, null);
        }
        State.refreshUI();
        _refreshDisplayList();
        _updateInputBindingStatus();
    }

    @Override
    public void onPause() {
        super.onPause();
        Context ctx = getContext();
        if (ctx != null) {
            ((DisplayManager) ctx.getSystemService(Context.DISPLAY_SERVICE))
                    .unregisterDisplayListener(displayListener);
        }
    }

    private void _updateUI(ExtendUiState state) {
        if (state == null) return;

        if (state.shizukuStatus != null) {
            shizukuStatus.setText(state.shizukuStatus);
        }
        shizukuPermissionBtn.setVisibility(state.shizukuPermissionVisible ? View.VISIBLE : View.GONE);

        _refreshDisplayList();
        _updateInputBindingStatus();
    }

    private void _refreshDisplayList() {
        if (displaysContainer == null || getContext() == null) return;
        DisplayManager dm = (DisplayManager) requireContext().getSystemService(Context.DISPLAY_SERVICE);
        Display[] displays = dm.getDisplays();
        List<Display> displayList = new ArrayList<>();
        for (Display d : displays) {
            if (d.getDisplayId() != State.managedVirtualDisplayHostDisplayId
                    && d.getDisplayId() != State.mirrorDisplayId) {
                displayList.add(d);
            }
        }
        displaysContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(displaysContainer.getContext());
        for (Display display : displayList) {
            displaysContainer.addView(_createDisplayRow(inflater, displaysContainer, display));
        }
    }

    private void _updateInputBindingStatus() {
        if (inputBindingStatus == null) return;
        inputBindingStatus.setText(Pref.getAutoBindInput()
                ? R.string.input_binding_status_auto
                : R.string.input_binding_status_manual);
    }

    private void _onDisplayItemClick(Display display) {
        if (getView() != null) {
            Bundle args = new Bundle();
            args.putInt("display_id", display.getDisplayId());
            Navigation.findNavController(getView()).navigate(R.id.action_overview_to_display_detail, args);
        }
    }

    private View _createDisplayRow(LayoutInflater inflater, ViewGroup parent, Display display) {
        View row = inflater.inflate(R.layout.item_display, parent, false);
        TextView displayId = row.findViewById(R.id.display_id);
        TextView displayName = row.findViewById(R.id.display_name);
        ImageView displayIcon = row.findViewById(R.id.display_icon);

        String displayInfo = String.format(requireContext().getString(R.string.display_id_format),
                display.getDisplayId(),
                display.getWidth(),
                display.getHeight());
        displayId.setText(displayInfo);
        displayName.setText(requireContext().getString(R.string.display_name_format, display.getName()));
        displayIcon.setImageResource(
                display.getDisplayId() == Display.DEFAULT_DISPLAY
                        ? R.drawable.ic_builtin_display
                        : R.drawable.ic_screens
        );
        row.setOnClickListener(v -> _onDisplayItemClick(display));
        return row;
    }
}
