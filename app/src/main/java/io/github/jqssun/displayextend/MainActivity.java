package io.github.jqssun.displayextend;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.view.Display;
import android.hardware.input.InputManager;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import org.lsposed.hiddenapibypass.HiddenApiBypass;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import io.github.jqssun.displayextend.job.AcquireShizuku;
import io.github.jqssun.displayextend.job.DisplayMonitor;
import io.github.jqssun.displayextend.job.InputDeviceMonitor;
import io.github.jqssun.displayextend.shizuku.ShizukuUtils;

import java.lang.ref.WeakReference;

import rikka.shizuku.Shizuku;

public class MainActivity extends AppCompatActivity {
    public static final String ACTION_USB_PERMISSION = "io.github.jqssun.displayextend.USB_PERMISSION";
    public static final String ACTION_OPEN_OVERVIEW = "io.github.jqssun.displayextend.action.OPEN_OVERVIEW";
    public static final String ACTION_OPEN_DISPLAY_DETAIL = "io.github.jqssun.displayextend.action.OPEN_DISPLAY_DETAIL";
    public static final String EXTRA_DISPLAY_ID = "display_id";
    public static final String EXTRA_SOURCE_SCREEN = "source_screen";
    public static final String MIRROR_PACKAGE_NAME = "io.github.jqssun.displaymirror";
    public static final String ACTION_OPEN_MIRROR_SCREEN = "io.github.jqssun.displaymirror.action.OPEN_SCREEN";
    public static final String EXTRA_SCREEN = "screen";
    private static final String MIRROR_SCREEN_MOONLIGHT = "moonlight";
    private static final String MIRROR_SCREEN_AIRPLAY = "airplay";
    private static final String MIRROR_SCREEN_DISPLAYLINK = "displaylink";

    private NavController navController;
    private BottomNavigationView bottomNav;
    private OnBackPressedCallback crossAppBackCallback;
    private String crossAppMirrorScreen;
    private boolean crossAppLandingOverview;
    private int crossAppLandingDisplayId = -1;

    private final ActivityResultLauncher<Intent> mediaProjectionLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    State.log("user granted projection permission");
                    if (MediaProjectionService.instance != null) {
                        MediaProjectionManager mpm = (MediaProjectionManager) getSystemService(
                                Context.MEDIA_PROJECTION_SERVICE);
                        State.setMediaProjection(mpm.getMediaProjection(RESULT_OK, data));
                        State.getMediaProjection().registerCallback(new MediaProjection.Callback() {
                            @Override
                            public void onStop() {
                                super.onStop();
                                State.log("MediaProjection onStop callback");
                            }
                        }, null);
                        State.resumeJob();
                    } else {
                        Intent svc = new Intent(this, MediaProjectionService.class);
                        svc.putExtra("data", data);
                        startService(svc);
                    }
                } else {
                    MediaProjectionService.isStarting = false;
                    State.log("user denied projection permission");
                    State.resumeJob();
                }
            });

    public ActivityResultLauncher<Intent> getMediaProjectionLauncher() {
        return mediaProjectionLauncher;
    }

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

    private void _onPermissionResult(int requestCode, int grantResult) {
        if (requestCode == AcquireShizuku.SHIZUKU_PERMISSION_REQUEST_CODE) {
            State.log("Shizuku permission result: "
                    + (grantResult == PackageManager.PERMISSION_GRANTED ? "granted" : "denied"));
            State.resumeJob();
        } else {
            State.log("unknown Shizuku request code: " + requestCode);
        }
    }

    private final Shizuku.OnRequestPermissionResultListener REQUEST_PERMISSION_RESULT_LISTENER = this::_onPermissionResult;

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
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        Shizuku.addRequestPermissionResultListener(REQUEST_PERMISSION_RESULT_LISTENER);
        if (ShizukuUtils.hasPermission() && State.userService == null) {
            Shizuku.peekUserService(State.userServiceArgs, State.userServiceConnection);
            Shizuku.bindUserService(State.userServiceArgs, State.userServiceConnection);
        }

        setContentView(R.layout.activity_main);

        State.currentActivity = new WeakReference<>(this);
        State.reconcileLastSingleAppDisplay(this);
        State.refreshUI();

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        navController = navHostFragment.getNavController();
        bottomNav = findViewById(R.id.bottomNav);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        AppBarConfiguration appBarConfig = new AppBarConfiguration.Builder(
                R.id.nav_overview, R.id.nav_logs, R.id.nav_settings).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfig);
        NavigationUI.setupWithNavController(bottomNav, navController);
        crossAppBackCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                _returnToMirrorScreen();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, crossAppBackCallback);
        navController
                .addOnDestinationChangedListener((controller, destination, arguments) -> _updateCrossAppBackState());
        _handleLaunchIntent(getIntent());

        IntentFilter permissionFilter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(usbPermissionReceiver, permissionFilter, null, null, Context.RECEIVER_EXPORTED);

        DisplayManager displayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        DisplayMonitor.init(displayManager);
        InputManager inputManager = (InputManager) getSystemService(Context.INPUT_SERVICE);
        InputDeviceMonitor.init(inputManager);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        _handleLaunchIntent(intent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController,
                new AppBarConfiguration.Builder(
                        R.id.nav_overview, R.id.nav_logs, R.id.nav_settings).build())
                || super.onSupportNavigateUp();
    }

    public void navigateToDisplayDetail(int displayId) {
        if (_isShowingDisplayDetail(displayId)) {
            return;
        }
        if (navController.getCurrentDestination() != null
                && navController.getCurrentDestination().getId() == R.id.nav_display_detail) {
            navController.popBackStack(R.id.nav_overview, false);
        }
        Bundle args = new Bundle();
        args.putInt("display_id", displayId);
        navController.navigate(R.id.nav_display_detail, args);
    }

    public void navigateToInputDeviceDetail(int deviceId) {
        Bundle args = new Bundle();
        args.putInt("device_id", deviceId);
        navController.navigate(R.id.nav_input_device_detail, args);
    }

    @Override
    protected void onResume() {
        super.onResume();
        State.currentActivity = new WeakReference<>(this);
        State.reconcileLastSingleAppDisplay(this);
        State.resumeJob();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        State.unbindUserService();
        Shizuku.removeRequestPermissionResultListener(REQUEST_PERMISSION_RESULT_LISTENER);
        State.currentActivity = new WeakReference<>(null);
        unregisterReceiver(usbPermissionReceiver);
    }

    private void _handleLaunchIntent(Intent intent) {
        if (intent == null) {
            return;
        }

        String action = intent.getAction();
        State.log("MainActivity created with action: " + action);
        String sourceScreen = intent.getStringExtra(EXTRA_SOURCE_SCREEN);
        if (ACTION_OPEN_OVERVIEW.equals(action)) {
            _navigateToOverview();
            if (_isValidMirrorScreen(sourceScreen)) {
                crossAppMirrorScreen = sourceScreen;
                crossAppLandingOverview = true;
                crossAppLandingDisplayId = -1;
            } else {
                _clearCrossAppBackTarget();
            }
            _updateCrossAppBackState();
            return;
        }
        if (!ACTION_OPEN_DISPLAY_DETAIL.equals(action)) {
            _clearCrossAppBackTarget();
            _updateCrossAppBackState();
            return;
        }

        int displayId = intent.getIntExtra(EXTRA_DISPLAY_ID, -1);
        DisplayManager displayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        Display display = displayManager != null ? displayManager.getDisplay(displayId) : null;
        if (display == null) {
            _navigateToOverview();
            Toast.makeText(this, R.string.display_unavailable_opened_overview, Toast.LENGTH_SHORT).show();
            if (_isValidMirrorScreen(sourceScreen)) {
                crossAppMirrorScreen = sourceScreen;
                crossAppLandingOverview = true;
                crossAppLandingDisplayId = -1;
            } else {
                _clearCrossAppBackTarget();
            }
            _updateCrossAppBackState();
            return;
        }

        if (_isValidMirrorScreen(sourceScreen)) {
            crossAppMirrorScreen = sourceScreen;
            crossAppLandingOverview = false;
            crossAppLandingDisplayId = displayId;
        } else {
            _clearCrossAppBackTarget();
        }
        if (_isShowingDisplayDetail(displayId)) {
            _updateCrossAppBackState();
            return;
        }

        _navigateToOverview();
        navigateToDisplayDetail(displayId);
        _updateCrossAppBackState();
    }

    private void _navigateToOverview() {
        if (bottomNav != null && bottomNav.getSelectedItemId() != R.id.nav_overview) {
            bottomNav.setSelectedItemId(R.id.nav_overview);
        }
        if (navController.getCurrentDestination() != null
                && navController.getCurrentDestination().getId() != R.id.nav_overview) {
            navController.popBackStack(R.id.nav_overview, false);
        }
    }

    private boolean _isShowingDisplayDetail(int displayId) {
        if (navController.getCurrentDestination() == null
                || navController.getCurrentDestination().getId() != R.id.nav_display_detail) {
            return false;
        }

        Bundle args = navController.getCurrentBackStackEntry() != null
                ? navController.getCurrentBackStackEntry().getArguments()
                : null;
        return args != null && args.getInt("display_id", -1) == displayId;
    }

    private boolean _isOnCrossAppLandingScreen() {
        if (crossAppMirrorScreen == null || navController == null || navController.getCurrentDestination() == null) {
            return false;
        }
        if (crossAppLandingOverview) {
            return navController.getCurrentDestination().getId() == R.id.nav_overview;
        }
        return _isShowingDisplayDetail(crossAppLandingDisplayId);
    }

    private void _updateCrossAppBackState() {
        if (crossAppBackCallback == null) {
            return;
        }
        crossAppBackCallback.setEnabled(_isOnCrossAppLandingScreen());
    }

    private void _clearCrossAppBackTarget() {
        crossAppMirrorScreen = null;
        crossAppLandingOverview = false;
        crossAppLandingDisplayId = -1;
    }

    private boolean _isValidMirrorScreen(String screen) {
        return MIRROR_SCREEN_MOONLIGHT.equals(screen)
                || MIRROR_SCREEN_AIRPLAY.equals(screen)
                || MIRROR_SCREEN_DISPLAYLINK.equals(screen);
    }

    private void _returnToMirrorScreen() {
        Intent intent = new Intent(ACTION_OPEN_MIRROR_SCREEN);
        intent.setPackage(MIRROR_PACKAGE_NAME);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.putExtra(EXTRA_SCREEN, crossAppMirrorScreen);

        _clearCrossAppBackTarget();
        _updateCrossAppBackState();

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
        moveTaskToBack(true);
    }
}
