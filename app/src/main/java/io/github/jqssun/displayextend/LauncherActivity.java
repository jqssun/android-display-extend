package io.github.jqssun.displayextend;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.view.Display;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import java.util.List;

import android.util.DisplayMetrics;
import android.widget.Toast;
import android.text.Editable;
import android.text.TextWatcher;
import com.google.android.material.chip.Chip;

import io.github.jqssun.displayextend.shizuku.ShizukuUtils;

import java.util.stream.Collectors;
import java.util.ArrayList;

public class LauncherActivity extends AppCompatActivity {
    public static final String EXTRA_TARGET_DISPLAY_ID = "target_display_id";
    
    private AppListAdapter adapter;
    private Chip floatingButtonToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        int displayId = getIntent().getIntExtra(EXTRA_TARGET_DISPLAY_ID, Display.DEFAULT_DISPLAY);
        
        setContentView(R.layout.activity_launcher);

        DisplayManager displayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        Display display = displayManager.getDisplay(displayId);
        if (display == null) {
            finish();
            return;
        }
        floatingButtonToggle = findViewById(R.id.floating_button_toggle);
        if (displayId != Display.DEFAULT_DISPLAY) {
            floatingButtonToggle.setVisibility(View.VISIBLE);
            floatingButtonToggle.setChecked(Pref.getFloatingButton(display.getName()));
            floatingButtonToggle.setOnCheckedChangeListener((chip, isChecked) -> {
                if (isChecked) {
                    if (!FloatingButtonService.startFloating(this, displayId, false)) {
                        floatingButtonToggle.setChecked(false);
                        return;
                    }
                } else {
                    Intent serviceIntent = new Intent(this, FloatingButtonService.class);
                    this.stopService(serviceIntent);
                }
                Pref.setFloatingButton(display.getName(), isChecked);
            });
        }
        Chip touchpadButton = findViewById(R.id.touchpadBtn);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R || ShizukuUtils.hasPermission()) {
            touchpadButton.setVisibility(View.VISIBLE);
        }
        touchpadButton.setOnClickListener(v -> {
            TouchpadActivity.startTouchpad(this, displayId, false);
        });
        
        RecyclerView recyclerView = findViewById(R.id.app_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        Chip showAllChip = findViewById(R.id.menu_button);
        showAllChip.setChecked(Pref.getShowAllApps());
        showAllChip.setOnCheckedChangeListener((chip, isChecked) -> {
            Pref.setShowAllApps(isChecked);
            adapter.updateAppList(_getFilteredApps(isChecked));
        });

        List<ApplicationInfo> userApps = _getFilteredApps(Pref.getShowAllApps());
        
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
        
        adapter = new AppListAdapter(userApps, getPackageManager(), displayId);
        recyclerView.setAdapter(adapter);

        _applyDpi();

        Chip forceDpiBtn = findViewById(R.id.forceDpiBtn);
        _updateForceDpiText(forceDpiBtn);
        forceDpiBtn.setOnClickListener(v -> _showForceDpiDialog(forceDpiBtn));

        findViewById(R.id.screenOffBtn).setOnClickListener(v -> {
            Intent intent = new Intent(this, PureBlackActivity.class);
            ActivityOptions options = ActivityOptions.makeBasic();
            startActivity(intent, options.toBundle());
        });
    }
    private List<ApplicationInfo> _getFilteredApps(boolean showAll) {
        List<ApplicationInfo> packages = new ArrayList<>();
        try {
            packages = getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);
        } catch (SecurityException e) {
            Toast.makeText(this, getString(R.string.query_apps_permission_required), Toast.LENGTH_LONG).show();
            State.log("failed to query app list: " + e);
        }
        return packages.stream()
            .filter(app -> showAll || (app.flags & ApplicationInfo.FLAG_SYSTEM) == 0)
            .collect(Collectors.toList());
    }

    private int _builtInDpi() {
        return getResources().getDisplayMetrics().densityDpi;
    }

    private void _applyDpi() {
        int dpi = Pref.getForceDpi();
        if (dpi <= 0) dpi = _builtInDpi();
        getResources().getDisplayMetrics().densityDpi = dpi;
        getResources().getConfiguration().densityDpi = dpi;
    }

    private void _updateForceDpiText(Chip chip) {
        int dpi = Pref.getForceDpi();
        chip.setText(dpi > 0 ? getString(R.string.force_dpi_format, dpi) : getString(R.string.force_dpi_off));
    }

    private void _showForceDpiDialog(Chip chip) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_force_dpi, null);
        com.google.android.material.textfield.TextInputEditText input = dialogView.findViewById(R.id.force_dpi_input);
        int current = Pref.getForceDpi();
        input.setText(String.valueOf(current));

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.force_dpi))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.ok), (d, w) -> {
                try {
                    int dpi = Integer.parseInt(input.getText().toString().trim());
                    Pref.setForceDpi(Math.max(0, dpi));
                    _applyDpi();
                    _updateForceDpiText(chip);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, getString(R.string.invalid_number), Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton(getString(R.string.cancel), null)
            .show();
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