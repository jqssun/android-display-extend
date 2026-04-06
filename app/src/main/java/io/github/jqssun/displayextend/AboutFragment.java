package io.github.jqssun.displayextend;

import static io.github.jqssun.displayextend.job.AcquireShizuku.SHIZUKU_PERMISSION_REQUEST_CODE;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import io.github.jqssun.displayextend.job.FetchLogAndShare;
import io.github.jqssun.displayextend.shizuku.ShizukuUtils;

import rikka.shizuku.Shizuku;

public class AboutFragment extends Fragment {

            
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_about, container, false);

        TextView websiteLink = view.findViewById(R.id.websiteLink);
        websiteLink.setOnClickListener(v -> openUrl("https://github.com/jqssun/android-screen-extend"));

        TextView versionText = view.findViewById(R.id.versionText);
        try {
            String versionName = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0).versionName;
            String androidVersion = android.os.Build.VERSION.RELEASE;
            versionText.setText(getString(R.string.version_format, versionName, androidVersion));
        } catch (Exception e) {
            versionText.setText(getString(R.string.version_unknown));
        }

        GestureDetector gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (!ShizukuUtils.hasShizukuStarted()) {
                    State.log("shizuku not started");
                    return false;
                }
                if (!ShizukuUtils.hasPermission()) {
                    State.log("ask shizuku permission");
                    Toast.makeText(getContext(), getString(R.string.export_log_requires_shizuku), Toast.LENGTH_SHORT).show();
                    Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE);
                    return false;
                }
                State.startNewJob(new FetchLogAndShare(getContext()));
                return true;
            }

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }
        });

        View header = view.findViewById(R.id.header);
        header.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return true;
        });
        
        return view;
    }

    private void openUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }

} 