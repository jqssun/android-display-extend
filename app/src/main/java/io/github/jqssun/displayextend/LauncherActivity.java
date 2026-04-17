package io.github.jqssun.displayextend;

import android.app.ActivityManager;
import android.app.TaskStackListener;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.view.Display;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.widget.Toast;
import android.text.Editable;
import android.text.TextWatcher;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class LauncherActivity extends AppCompatActivity {
    public static final String EXTRA_TARGET_DISPLAY_ID = "target_display_id";
    private static final long TASK_REFRESH_DEBOUNCE_MS = 150;

    private AppListAdapter adapter;
    private int targetDisplayId;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable refreshAppSectionsRunnable = this::_refreshAppSections;
    private final TaskStackListener taskStackListener = new TaskStackListener() {
        @Override
        public void onTaskCreated(int taskId, android.content.ComponentName componentName) throws RemoteException {
            _scheduleTaskRefresh();
        }

        @Override
        public void onTaskRemovalStarted(ActivityManager.RunningTaskInfo taskInfo) throws RemoteException {
            _scheduleTaskRefresh();
        }

        @Override
        public void onTaskDisplayChanged(int taskId, int newDisplayId) throws RemoteException {
            _scheduleTaskRefresh();
        }

        @Override
        public void onTaskMovedToFront(ActivityManager.RunningTaskInfo taskInfo) throws RemoteException {
            _scheduleTaskRefresh();
        }
    };
    private boolean isTaskStackListenerRegistered;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        targetDisplayId = getIntent().getIntExtra(EXTRA_TARGET_DISPLAY_ID, Display.DEFAULT_DISPLAY);

        setContentView(R.layout.activity_launcher);

        DisplayManager displayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        Display display = displayManager.getDisplay(targetDisplayId);
        if (display == null) {
            finish();
            return;
        }

        RecyclerView recyclerView = findViewById(R.id.app_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        Chip showAllChip = findViewById(R.id.menu_button);
        showAllChip.setChecked(Pref.getShowAllApps());
        showAllChip.setOnCheckedChangeListener((chip, isChecked) -> {
            Pref.setShowAllApps(isChecked);
            _refreshAppSections();
        });

        com.google.android.material.textfield.TextInputEditText searchBox = findViewById(R.id.search_box);
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                adapter.filter(s.toString());
            }
        });

        adapter = new AppListAdapter(
                new ArrayList<>(),
                new ArrayList<>(),
                getPackageManager(),
                getResources(),
                targetDisplayId
        );
        recyclerView.setAdapter(adapter);
        _refreshAppSections();
    }

    @Override
    protected void onResume() {
        super.onResume();
        _refreshAppSections();
    }

    @Override
    protected void onStart() {
        super.onStart();
        _registerTaskStackListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        _unregisterTaskStackListener();
        mainHandler.removeCallbacks(refreshAppSectionsRunnable);
    }

    private void _scheduleTaskRefresh() {
        mainHandler.removeCallbacks(refreshAppSectionsRunnable);
        mainHandler.postDelayed(refreshAppSectionsRunnable, TASK_REFRESH_DEBOUNCE_MS);
    }

    private void _registerTaskStackListener() {
        if (isTaskStackListenerRegistered || !io.github.jqssun.displayextend.shizuku.ShizukuUtils.hasPermission()) {
            return;
        }
        try {
            io.github.jqssun.displayextend.shizuku.ServiceUtils.getActivityTaskManager()
                    .registerTaskStackListener(taskStackListener);
            isTaskStackListenerRegistered = true;
        } catch (Throwable e) {
            State.log("failed to register task stack listener: " + e.getMessage());
        }
    }

    private void _unregisterTaskStackListener() {
        if (!isTaskStackListenerRegistered) {
            return;
        }
        try {
            io.github.jqssun.displayextend.shizuku.ServiceUtils.getActivityTaskManager()
                    .unregisterTaskStackListener(taskStackListener);
        } catch (Throwable e) {
            State.log("failed to unregister task stack listener: " + e.getMessage());
        }
        isTaskStackListenerRegistered = false;
    }

    private void _refreshAppSections() {
        List<AppListAdapter.AppEntry> runningApps = _getRunningApps();
        Set<String> runningPackages = runningApps.stream()
                .map(entry -> entry.applicationInfo.packageName)
                .collect(Collectors.toSet());
        adapter.updateRunningApps(runningApps);
        adapter.updateInstalledApps(_getFilteredApps(Pref.getShowAllApps(), runningPackages));
    }

    private List<AppListAdapter.AppEntry> _getFilteredApps(boolean showAll, Set<String> excludedPackages) {
        List<ApplicationInfo> packages = new ArrayList<>();
        try {
            packages = getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);
        } catch (SecurityException e) {
            Toast.makeText(this, getString(R.string.query_apps_permission_required), Toast.LENGTH_LONG).show();
            State.log("failed to query app list: " + e);
        }
        return packages.stream()
            .filter(app -> showAll || (app.flags & ApplicationInfo.FLAG_SYSTEM) == 0)
            .filter(app -> excludedPackages == null || !excludedPackages.contains(app.packageName))
            .map(app -> new AppListAdapter.AppEntry(
                    app,
                    Pref.getLaunchTime(app.packageName),
                    app.packageName
            ))
            .collect(Collectors.toList());
    }

    private List<AppListAdapter.AppEntry> _getRunningApps() {
        Map<String, State.RunningTaskEntry> tasksByPackage = new LinkedHashMap<>();
        for (State.RunningTaskEntry task : State.getRunningTasksOnDisplay(this, targetDisplayId)) {
            if (!tasksByPackage.containsKey(task.packageName)) {
                tasksByPackage.put(task.packageName, task);
            }
        }

        List<AppListAdapter.AppEntry> runningApps = new ArrayList<>();
        for (State.RunningTaskEntry task : tasksByPackage.values()) {
            try {
                ApplicationInfo app = getPackageManager().getApplicationInfo(task.packageName, PackageManager.GET_META_DATA);
                runningApps.add(new AppListAdapter.AppEntry(
                        app,
                        task.lastActiveTime,
                        task.packageName
                ));
            } catch (PackageManager.NameNotFoundException e) {
                State.log("failed to resolve running task package: " + task.packageName);
            }
        }
        return runningApps;
    }

    public static void start(Context context, int displayId) {
        List<ApplicationInfo> packages = new ArrayList<>();
        try {
            packages = context.getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);
        } catch (SecurityException e) {
            Toast.makeText(context, context.getString(R.string.query_apps_permission_required), Toast.LENGTH_LONG).show();
            State.log("failed to query app list: " + e);
            return;
        }

        if (packages.size() <= 1) {
            Toast.makeText(context, context.getString(R.string.cannot_get_app_list), Toast.LENGTH_LONG).show();
            return;
        }

        Intent intent = new Intent(context, LauncherActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(EXTRA_TARGET_DISPLAY_ID, displayId);
        context.startActivity(intent);
    }
}
