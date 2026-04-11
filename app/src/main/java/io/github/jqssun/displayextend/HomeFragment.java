package io.github.jqssun.displayextend;

import android.app.ActivityOptions;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import io.github.jqssun.displayextend.job.AcquireShizuku;

public class HomeFragment extends Fragment {

    private TextView shizukuStatus;
    private MaterialButton shizukuPermissionBtn;
    private MaterialButton screenOffBtn;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Shizuku status
        TextView shizukuStatusPrefix = view.findViewById(R.id.shizukuStatusPrefix);
        shizukuStatusPrefix.setOnClickListener(v ->
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/rikkaapps/shizuku")))
        );

        shizukuPermissionBtn = view.findViewById(R.id.shizukuPermissionBtn);
        shizukuPermissionBtn.setOnClickListener(v -> State.startNewJob(new AcquireShizuku()));

        shizukuStatus = view.findViewById(R.id.shizukuStatus);

        screenOffBtn = view.findViewById(R.id.screenOffBtn);
        screenOffBtn.setOnClickListener(v -> {
            if (State.lastSingleAppDisplay <= 0) {
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.no_projection_title))
                    .setMessage(getString(R.string.no_projection_message))
                    .setPositiveButton(getString(R.string.got_it), null)
                    .show();
            } else {
                Intent intent = new Intent(getActivity(), PureBlackActivity.class);
                ActivityOptions options = ActivityOptions.makeBasic();
                startActivity(intent, options.toBundle());
            }
        });

        State.uiState.observe(getViewLifecycleOwner(), this::_updateUI);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        State.refreshUI();
    }

    private void _updateUI(ExtendUiState state) {
        if (state == null) return;

        if (state.shizukuStatus != null) {
            shizukuStatus.setText(state.shizukuStatus);
        }
        shizukuPermissionBtn.setVisibility(state.shizukuPermissionVisible ? View.VISIBLE : View.GONE);

        if (state.useRealScreenOff) {
            screenOffBtn.setText(getString(R.string.screen_off_real));
        } else {
            screenOffBtn.setText(getString(R.string.screen_off));
        }

    }
}
