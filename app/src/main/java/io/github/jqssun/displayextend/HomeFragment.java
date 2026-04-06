package io.github.jqssun.displayextend;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import io.github.jqssun.displayextend.job.AcquireShizuku;
import io.github.jqssun.displayextend.job.ExitAll;
import io.github.jqssun.displayextend.shizuku.ShizukuUtils;

public class HomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        TextView shizukuStatusPrefix = view.findViewById(R.id.shizukuStatusPrefix);
        shizukuStatusPrefix.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
        shizukuStatusPrefix.setPaintFlags(shizukuStatusPrefix.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        shizukuStatusPrefix.setOnClickListener(v -> {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/rikkaapps/shizuku")));
        });

        Button shizukuPermissionBtn = view.findViewById(R.id.shizukuPermissionBtn);
        shizukuPermissionBtn.setOnClickListener(v -> {
            State.startNewJob(new AcquireShizuku());
        });

        TextView shizukuStatus = view.findViewById(R.id.shizukuStatus);
        updateShizukuStatus(shizukuStatus, shizukuPermissionBtn);

        Button displayDeviceBtn = view.findViewById(R.id.displayDeviceBtn);
        Button simulateScreenOffBtn = view.findViewById(R.id.simulateScreenOffBtn);
        Button touchpadBtn = view.findViewById(R.id.touchpadBtn);
        Button inputDeviceBtn = view.findViewById(R.id.inputDeviceBtn);
        Button shizukuBtn = view.findViewById(R.id.shizukuBtn);
        Button aboutBtn = view.findViewById(R.id.aboutBtn);
        Button exitBtn = view.findViewById(R.id.exitBtn);

        exitBtn.setOnClickListener(v -> {
            ExitAll.execute(requireContext());
        });

        displayDeviceBtn.setOnClickListener(v -> {
            State.breadcrumbManager.pushBreadcrumb(getString(R.string.screens), () -> new DisplayListFragment());
        });

        boolean useRealScreenOff = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
        .getBoolean("use_real_screen_off", false);
        if(useRealScreenOff) {
            simulateScreenOffBtn.setText(getString(R.string.real_screen_off));
        }
        simulateScreenOffBtn.setOnClickListener(v -> {
            if (State.lastSingleAppDisplay <= 0) {
                showHelp();
            } else {
                Intent intent = new Intent(getActivity(), PureBlackActivity.class);
                ActivityOptions options = ActivityOptions.makeBasic();
                startActivity(intent, options.toBundle());
            }
        });

        touchpadBtn.setOnClickListener(v -> {
            if (State.lastSingleAppDisplay <= 0) {
                showHelp();
            } else {
                TouchpadActivity.startTouchpad(getContext(), State.lastSingleAppDisplay, false);
            }
        });

        inputDeviceBtn.setOnClickListener(v -> {
            if (ShizukuUtils.hasPermission()) {
                State.breadcrumbManager.pushBreadcrumb(getString(R.string.settings), () -> new SettingsFragment());
            } else {
                Toast.makeText(requireContext(), getString(R.string.shizuku_required), Toast.LENGTH_SHORT).show();
            }
        });

        shizukuBtn.setOnClickListener(v -> {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/rikkaapps/shizuku")));
        });

        aboutBtn.setOnClickListener(v -> {
            State.breadcrumbManager.pushBreadcrumb(getString(R.string.about), () -> new AboutFragment());
        });

        return view;
    }

    private void showHelp() {
        new AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.no_projection_title))
            .setMessage(getString(R.string.no_projection_message))
            .setPositiveButton(getString(R.string.got_it), null)
            .show();
    }

    private void updateShizukuStatus(TextView statusView, Button permissionBtn) {
        boolean started = ShizukuUtils.hasShizukuStarted();
        boolean hasPermission = ShizukuUtils.hasPermission();
        
        String status;
        if (!started) {
            status = getString(R.string.shizuku_not_started);
            permissionBtn.setVisibility(View.GONE);
        } else if (!hasPermission) {
            status = getString(R.string.shizuku_status_denied);
            permissionBtn.setVisibility(View.VISIBLE);
        } else {
            status = getString(R.string.shizuku_status_granted);
            permissionBtn.setVisibility(View.GONE);
        }
        
        statusView.setText(status);
    }
}