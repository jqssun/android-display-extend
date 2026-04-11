package io.github.jqssun.displayextend;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.view.Display;

import androidx.appcompat.app.AppCompatActivity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import java.util.List;

import android.widget.Toast;
import android.text.Editable;
import android.text.TextWatcher;
import com.google.android.material.chip.Chip;

import java.util.stream.Collectors;
import java.util.ArrayList;

public class LauncherActivity extends AppCompatActivity {
    public static final String EXTRA_TARGET_DISPLAY_ID = "target_display_id";

    private AppListAdapter adapter;

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
