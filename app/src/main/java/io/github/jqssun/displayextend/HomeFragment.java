package io.github.jqssun.displayextend;

import android.app.ActivityOptions;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import io.github.jqssun.displayextend.job.AcquireShizuku;
import io.github.jqssun.displayextend.job.ExitAll;
import io.github.jqssun.displayextend.job.FetchLogAndShare;
import io.github.jqssun.displayextend.shizuku.ShizukuUtils;

public class HomeFragment extends Fragment {

    private TextView shizukuStatus;
    private MaterialButton shizukuPermissionBtn;
    private MaterialButton screenOffBtn;
    private TextView versionText;

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

        // About info
        versionText = view.findViewById(R.id.versionText);

        view.findViewById(R.id.websiteLink).setOnClickListener(v ->
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/jqssun/android-screen-extend")))
        );

        // Double-tap about card to export logs
        View aboutCard = view.findViewById(R.id.aboutCard);
        GestureDetector gestureDetector = new GestureDetector(requireContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (ShizukuUtils.hasPermission() && getContext() != null) {
                    State.startNewJob(new FetchLogAndShare(getContext()));
                }
                return true;
            }
            @Override
            public boolean onDown(MotionEvent e) { return true; }
        });
        aboutCard.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return true;
        });

        // Shizuku link
        view.findViewById(R.id.shizukuBtn).setOnClickListener(v ->
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/rikkaapps/shizuku")))
        );

        // Exit
        view.findViewById(R.id.exitBtn).setOnClickListener(v ->
            ExitAll.execute(requireContext())
        );

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

        if (state.versionText != null) {
            versionText.setText(state.versionText);
        }
    }
}
