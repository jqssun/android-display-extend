package io.github.jqssun.displayextend;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import java.util.List;

import android.content.res.Resources;
import android.content.res.Configuration;
import android.util.DisplayMetrics;
import android.widget.Toast;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.view.Menu;

import io.github.jqssun.displayextend.shizuku.ShizukuUtils;

import java.util.stream.Collectors;
import java.util.ArrayList;

public class LauncherActivity extends AppCompatActivity {
    public static final String EXTRA_TARGET_DISPLAY_ID = "target_display_id";
    
    private AppListAdapter adapter;
    private Button floatingButtonToggle;

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
            SharedPreferences appPreferences = this.getSharedPreferences("app_preferences", MODE_PRIVATE);
            updateFloatingBackButtonText(appPreferences.getBoolean("FLOATING_BUTTON_" + display.getName(), false));
            floatingButtonToggle.setOnClickListener(v -> {
                boolean isEnabled = appPreferences.getBoolean("FLOATING_BUTTON_" + display.getName(), false);
                if (isEnabled) {
                    Intent serviceIntent = new Intent(this, FloatingButtonService.class);
                    this.stopService(serviceIntent);
                    isEnabled = false;
                } else {
                    if (FloatingButtonService.startFloating(this, displayId, false)) {
                        isEnabled = true;
                    }
                }
                appPreferences.edit().putBoolean("FLOATING_BUTTON_" + display.getName(), isEnabled).apply();
                updateFloatingBackButtonText(isEnabled);
            });
        }
        Button touchpadButton = findViewById(R.id.btn_touchpad);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R || ShizukuUtils.hasPermission()) {
            touchpadButton.setVisibility(View.VISIBLE);
        }
        touchpadButton.setOnClickListener(v -> {
            TouchpadActivity.startTouchpad(this, displayId, false);
        });
        
        RecyclerView recyclerView = findViewById(R.id.app_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> packages = new ArrayList<>();
        try {
            packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        } catch (SecurityException e) {
            Toast.makeText(this, getString(R.string.query_apps_permission_required), Toast.LENGTH_LONG).show();
            State.log("failed to query app list: " + e);
        }

        ImageButton menuButton = findViewById(R.id.menu_button);
        menuButton.setOnClickListener(v -> showMenu(v));
        
        boolean showAllApps = getSharedPreferences("app_preferences", MODE_PRIVATE)
            .getBoolean("show_all_apps", false);
            
        List<ApplicationInfo> userApps = packages.stream()
            .filter(app -> showAllApps || (app.flags & ApplicationInfo.FLAG_SYSTEM) == 0)
            .collect(Collectors.toList());
        
        EditText searchBox = findViewById(R.id.search_box);
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
            userApps, 
            pm, 
            displayId,
            getSharedPreferences("app_preferences", MODE_PRIVATE)
        );
        recyclerView.setAdapter(adapter);
        
        Resources resources = getResources();
        Configuration configuration = resources.getConfiguration();
        DisplayMetrics displayMetrics = resources.getDisplayMetrics();
        
        displayMetrics.densityDpi = 320; 
        configuration.densityDpi = 320;
        
        resources.updateConfiguration(configuration, displayMetrics);

        findViewById(R.id.btn_screen_off).setOnClickListener(v -> {
            Intent intent = new Intent(this, PureBlackActivity.class);
            ActivityOptions options = ActivityOptions.makeBasic();
            startActivity(intent, options.toBundle());
        });
    }
    private void updateFloatingBackButtonText(boolean isEnabled) {
        floatingButtonToggle.setText(isEnabled ? getString(R.string.floating_on) : getString(R.string.floating_off));
    }
    
    private void showMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(Menu.NONE, 1, Menu.NONE, getString(R.string.show_all_apps))
            .setCheckable(true)
            .setChecked(getSharedPreferences("app_preferences", MODE_PRIVATE)
                .getBoolean("show_all_apps", false));
                
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                item.setChecked(!item.isChecked());
                getSharedPreferences("app_preferences", MODE_PRIVATE)
                    .edit()
                    .putBoolean("show_all_apps", item.isChecked())
                    .apply();
                    
                PackageManager pm = getPackageManager();
                List<ApplicationInfo> packages = new ArrayList<>();
                try {
                    packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
                } catch (SecurityException e) {
                    Toast.makeText(this, getString(R.string.query_apps_permission_required), Toast.LENGTH_LONG).show();
                    State.log("failed to query app list: " + e);
                    return true;
                }
                
                List<ApplicationInfo> filteredApps = packages.stream()
                    .filter(app -> item.isChecked() || (app.flags & ApplicationInfo.FLAG_SYSTEM) == 0)
                    .collect(Collectors.toList());
                    
                adapter.updateAppList(filteredApps);
                return true;
            }
            return false;
        });
        
        popup.show();
    }

    public static void start(Context context, int displayId) {
        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> packages = new ArrayList<>();
        try {
            packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
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