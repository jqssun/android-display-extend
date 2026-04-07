package io.github.jqssun.displayextend;

import android.app.ActivityOptions;
import android.content.Context;
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

import io.github.jqssun.displayextend.job.AcquireShizuku;
import io.github.jqssun.displayextend.job.ExitAll;
import io.github.jqssun.displayextend.job.FetchLogAndShare;
import io.github.jqssun.displayextend.shizuku.ShizukuUtils;

public class HomeFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Shizuku status
        TextView shizukuStatusPrefix = view.findViewById(R.id.shizukuStatusPrefix);
        shizukuStatusPrefix.setOnClickListener(v ->
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/rikkaapps/shizuku")))
        );

        com.google.android.material.button.MaterialButton shizukuPermissionBtn = view.findViewById(R.id.shizukuPermissionBtn);
        shizukuPermissionBtn.setOnClickListener(v -> State.startNewJob(new AcquireShizuku()));

        TextView shizukuStatus = view.findViewById(R.id.shizukuStatus);
        updateShizukuStatus(shizukuStatus, shizukuPermissionBtn);

        // Simulate screen off
        com.google.android.material.button.MaterialButton simulateScreenOffBtn = view.findViewById(R.id.simulateScreenOffBtn);
        boolean useRealScreenOff = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getBoolean("use_real_screen_off", false);
        if (useRealScreenOff) {
            simulateScreenOffBtn.setText(getString(R.string.real_screen_off));
        }
        simulateScreenOffBtn.setOnClickListener(v -> {
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
        TextView versionText = view.findViewById(R.id.versionText);
        try {
            String versionName = requireContext().getPackageManager()
                .getPackageInfo(requireContext().getPackageName(), 0).versionName;
            String androidVersion = android.os.Build.VERSION.RELEASE;
            versionText.setText(getString(R.string.version_format, versionName, androidVersion));
        } catch (Exception e) {
            versionText.setText(getString(R.string.version_unknown));
        }

        view.findViewById(R.id.websiteLink).setOnClickListener(v ->
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/jqssun/android-screen-extend")))
        );

        // Double-tap about card to export logs
        View aboutCard = view.findViewById(R.id.aboutCard);
        GestureDetector gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (ShizukuUtils.hasPermission()) {
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

        return view;
    }

    private void updateShizukuStatus(TextView statusView, View permissionBtn) {
        boolean started = ShizukuUtils.hasShizukuStarted();
        boolean hasPermission = ShizukuUtils.hasPermission();

        if (!started) {
            statusView.setText(getString(R.string.shizuku_not_started));
            permissionBtn.setVisibility(View.GONE);
        } else if (!hasPermission) {
            statusView.setText(getString(R.string.shizuku_status_denied));
            permissionBtn.setVisibility(View.VISIBLE);
        } else {
            statusView.setText(getString(R.string.shizuku_status_granted));
            permissionBtn.setVisibility(View.GONE);
        }
    }
}
