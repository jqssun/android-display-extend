package io.github.jqssun.displayextend;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.hardware.input.InputManager;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Build;

import org.lsposed.hiddenapibypass.HiddenApiBypass;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import io.github.jqssun.displayextend.job.AcquireShizuku;
import io.github.jqssun.displayextend.job.DisplayMonitor;
import io.github.jqssun.displayextend.job.InputDeviceMonitor;
import io.github.jqssun.displayextend.shizuku.ShizukuUtils;

import java.lang.ref.WeakReference;

import rikka.shizuku.Shizuku;

public class MainActivity extends AppCompatActivity implements IMainActivity {
    public static final String ACTION_USB_PERMISSION = "io.github.jqssun.displayextend.USB_PERMISSION";
    public static final int REQUEST_CODE_MEDIA_PROJECTION = 1001;

    private BottomNavigationView bottomNav;
    private int currentTabId = R.id.nav_overview;
    private LogsFragment logsFragment;

    private final BroadcastReceiver usbPermissionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            android.util.Log.d("MainActivity", "received action: " + intent.getAction());
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                State.resumeJob();
            }
        }
    };

    private void onRequestPermissionsResult(int requestCode, int grantResult) {
        if (requestCode == AcquireShizuku.SHIZUKU_PERMISSION_REQUEST_CODE) {
            State.log("Shizuku permission result: " + (grantResult == PackageManager.PERMISSION_GRANTED ? "granted" : "denied"));
            State.resumeJob();
        } else {
            State.log("unknown Shizuku request code: " + requestCode);
        }
    }

    private final Shizuku.OnRequestPermissionResultListener REQUEST_PERMISSION_RESULT_LISTENER =
        this::onRequestPermissionsResult;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                HiddenApiBypass.addHiddenApiExemptions("");
                android.util.Log.i("MainActivity", "hidden API exemption added");
            } catch (Exception e) {
                android.util.Log.e("MainActivity", "failed to add hidden API exemption: " + e.getMessage());
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Shizuku.addRequestPermissionResultListener(REQUEST_PERMISSION_RESULT_LISTENER);
        if (ShizukuUtils.hasPermission() && State.userService == null) {
            Shizuku.peekUserService(State.userServiceArgs, State.userServiceConnection);
            Shizuku.bindUserService(State.userServiceArgs, State.userServiceConnection);
        }

        setContentView(R.layout.activity_main);

        State.currentActivity = new WeakReference<>(this);

        bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(item -> {
            selectTab(item.getItemId());
            return true;
        });

        // Show overview tab initially
        selectTab(R.id.nav_overview);

        Intent intent = getIntent();
        String action = intent.getAction();
        State.log("MainActivity created with action: " + action);

        IntentFilter permissionFilter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(usbPermissionReceiver, permissionFilter, null, null, Context.RECEIVER_EXPORTED);

        DisplayManager displayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        DisplayMonitor.init(displayManager);
        InputManager inputManager = (InputManager) getSystemService(Context.INPUT_SERVICE);
        InputDeviceMonitor.init(inputManager);
    }

    private void selectTab(int tabId) {
        // Clear backstack when switching tabs
        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        currentTabId = tabId;
        Fragment fragment;

        if (tabId == R.id.nav_overview) {
            fragment = new HomeFragment();
        } else if (tabId == R.id.nav_screens) {
            fragment = new DisplayListFragment();
        } else if (tabId == R.id.nav_touchpad) {
            fragment = new TouchpadFragment();
        } else if (tabId == R.id.nav_logs) {
            logsFragment = new LogsFragment();
            fragment = logsFragment;
        } else if (tabId == R.id.nav_settings) {
            fragment = new SettingsFragment();
        } else {
            fragment = new HomeFragment();
        }

        getSupportFragmentManager().beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit();
    }

    @Override
    public void navigateToDetail(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit();
    }

    @Override
    public void refreshCurrentFragment() {
        try {
            if (isFinishing() || isDestroyed()) return;
            // Only refresh if on a top-level tab (no detail pages in backstack)
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                selectTab(currentTabId);
            }
        } catch (Exception e) {
            // ignore
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        State.currentActivity = new WeakReference<>(this);
        State.resumeJob();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        State.unbindUserService();
        Shizuku.removeRequestPermissionResultListener(REQUEST_PERMISSION_RESULT_LISTENER);
        State.currentActivity = null;
        unregisterReceiver(usbPermissionReceiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_MEDIA_PROJECTION) {
            if (resultCode == RESULT_OK && data != null) {
                State.log("user granted projection permission");
                if (MediaProjectionService.instance != null) {
                    MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                    State.setMediaProjection(mediaProjectionManager.getMediaProjection(RESULT_OK, data));
                    State.getMediaProjection().registerCallback(new MediaProjection.Callback() {
                        @Override
                        public void onStop() {
                            super.onStop();
                            State.log("MediaProjection onStop callback");
                        }
                    }, null);
                    State.resumeJob();
                } else {
                    Intent serviceIntent = new Intent(this, MediaProjectionService.class);
                    serviceIntent.putExtra("data", data);
                    startService(serviceIntent);
                }
            } else {
                MediaProjectionService.isStarting = false;
                State.log("user denied projection permission");
                State.resumeJob();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void updateLogs() {
        if (logsFragment != null && logsFragment.isAdded()) {
            logsFragment.refreshLogs();
        }
    }
}
