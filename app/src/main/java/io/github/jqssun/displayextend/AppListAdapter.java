package io.github.jqssun.displayextend;

import android.app.ActivityOptions;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import io.github.jqssun.displayextend.shizuku.ServiceUtils;
import io.github.jqssun.displayextend.shizuku.ShizukuUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AppListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final class AppEntry {
        public final ApplicationInfo applicationInfo;
        public final long sortTime;
        public final String subtitle;

        public AppEntry(ApplicationInfo applicationInfo, long sortTime, String subtitle) {
            this.applicationInfo = applicationInfo;
            this.sortTime = sortTime;
            this.subtitle = subtitle;
        }
    }

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_APP = 1;

    private static final class ListItem {
        public final int viewType;
        public final String title;
        public final AppEntry appEntry;

        public ListItem(int viewType, String title, AppEntry appEntry) {
            this.viewType = viewType;
            this.title = title;
            this.appEntry = appEntry;
        }
    }

    private List<AppEntry> installedApps;
    private List<AppEntry> runningApps;
    private final List<ListItem> filteredItems = new ArrayList<>();
    private final int targetDisplayId;
    private final PackageManager packageManager;
    private final Resources resources;
    private String currentQuery = "";

    public AppListAdapter(List<AppEntry> installedApps, List<AppEntry> runningApps,
                          PackageManager packageManager, Resources resources, int targetDisplayId) {
        this.packageManager = packageManager;
        this.resources = resources;
        this.targetDisplayId = targetDisplayId;
        this.installedApps = installedApps != null ? installedApps : Collections.emptyList();
        this.runningApps = runningApps != null ? runningApps : Collections.emptyList();
        _sortAppList(this.installedApps);
        _sortAppList(this.runningApps);
        _rebuildItems();
    }

    private void _sortAppList(List<AppEntry> appList) {
        Collections.sort(appList, (app1, app2) -> {
            if (app1.sortTime == app2.sortTime) {
                return app1.applicationInfo.loadLabel(packageManager)
                    .toString()
                    .compareToIgnoreCase(app2.applicationInfo.loadLabel(packageManager).toString());
            }
            return Long.compare(app2.sortTime, app1.sortTime);
        });
    }

    public void filter(String query) {
        currentQuery = query != null ? query : "";
        _rebuildItems();
    }

    public void updateInstalledApps(List<AppEntry> newInstalledApps) {
        this.installedApps = newInstalledApps != null ? newInstalledApps : Collections.emptyList();
        _sortAppList(this.installedApps);
        _rebuildItems();
    }

    public void updateRunningApps(List<AppEntry> newRunningApps) {
        this.runningApps = newRunningApps != null ? newRunningApps : Collections.emptyList();
        _sortAppList(this.runningApps);
        _rebuildItems();
    }

    public Set<String> getRunningPackageNames() {
        Set<String> packageNames = new HashSet<>();
        for (AppEntry app : runningApps) {
            packageNames.add(app.applicationInfo.packageName);
        }
        return packageNames;
    }

    private void _rebuildItems() {
        filteredItems.clear();

        List<AppEntry> filteredRunningApps = _filterAppEntries(runningApps);
        List<AppEntry> filteredInstalledApps = _filterAppEntries(installedApps);

        if (!filteredRunningApps.isEmpty()) {
            filteredItems.add(new ListItem(VIEW_TYPE_HEADER, _getString(R.string.running_tasks_header), null));
            for (AppEntry app : filteredRunningApps) {
                filteredItems.add(new ListItem(VIEW_TYPE_APP, null, app));
            }
        }
        if (!filteredInstalledApps.isEmpty()) {
            filteredItems.add(new ListItem(VIEW_TYPE_HEADER, _getString(R.string.installed_apps_header), null));
            for (AppEntry app : filteredInstalledApps) {
                filteredItems.add(new ListItem(VIEW_TYPE_APP, null, app));
            }
        }

        notifyDataSetChanged();
    }

    private List<AppEntry> _filterAppEntries(List<AppEntry> source) {
        if (currentQuery.isEmpty()) {
            return new ArrayList<>(source);
        }
        String lowerQuery = currentQuery.toLowerCase();
        List<AppEntry> matches = new ArrayList<>();
        for (AppEntry app : source) {
            String appName = app.applicationInfo.loadLabel(packageManager).toString().toLowerCase();
            String packageName = app.applicationInfo.packageName.toLowerCase();
            if (appName.contains(lowerQuery) || packageName.contains(lowerQuery)) {
                matches.add(app);
            }
        }
        return matches;
    }

    private String _getString(int resId) {
        return resources.getString(resId);
    }

    @Override
    public int getItemViewType(int position) {
        return filteredItems.get(position).viewType;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_list_section_header, parent, false);
            return new HeaderViewHolder(view);
        }
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ListItem item = filteredItems.get(position);
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).title.setText(item.title);
            return;
        }

        AppEntry entry = item.appEntry;
        ApplicationInfo app = entry.applicationInfo;
        ViewHolder appHolder = (ViewHolder) holder;
        appHolder.text1.setText(app.loadLabel(packageManager));
        appHolder.text2.setText(entry.subtitle != null ? entry.subtitle : app.packageName);

        try {
            appHolder.appIcon.setImageDrawable(packageManager.getApplicationIcon(app.packageName));
        } catch (PackageManager.NameNotFoundException e) {
            appHolder.appIcon.setImageResource(android.R.drawable.sym_def_app_icon);
        }

        View.OnClickListener castAction = v -> _castApp(v, app);
        appHolder.btnLaunch.setOnClickListener(castAction);
        appHolder.itemView.setOnClickListener(castAction);
        appHolder.btnOpenOnPhone.setOnClickListener(v -> _openOnPhone(v, app));
    }

    private void _castApp(View v, ApplicationInfo app) {
        if (!ShizukuUtils.hasPermission() && Pref.getLaunchTime(app.packageName) == 0) {
            _openOnPhone(v, app);
            Toast.makeText(v.getContext(), v.getContext().getString(R.string.first_launch_hint), Toast.LENGTH_SHORT).show();
            return;
        }
        Pref.setLastPackageName(app.packageName);
        Pref.setLaunchTime(app.packageName, System.currentTimeMillis());
        ServiceUtils.launchPackage(v.getContext(), app.packageName, targetDisplayId);
        if (State.floatingButtonService != null) {
            State.floatingButtonService.onSingleAppLaunched();
        }
    }

    private void _openOnPhone(View v, ApplicationInfo app) {
        Intent launchIntent = packageManager.getLaunchIntentForPackage(app.packageName);
        if (launchIntent != null) {
            Pref.setLaunchTime(app.packageName, System.currentTimeMillis());
            ActivityOptions options = ActivityOptions.makeBasic();
            v.getContext().startActivity(launchIntent, options.toBundle());
        }
    }

    @Override
    public int getItemCount() {
        return filteredItems.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView title;

        HeaderViewHolder(View view) {
            super(view);
            title = view.findViewById(R.id.headerText);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView appIcon;
        TextView text1;
        TextView text2;
        MaterialButton btnOpenOnPhone;
        MaterialButton btnLaunch;

        ViewHolder(View view) {
            super(view);
            appIcon = view.findViewById(R.id.app_icon);
            text1 = view.findViewById(R.id.text1);
            text2 = view.findViewById(R.id.text2);
            btnOpenOnPhone = view.findViewById(R.id.openOnPhoneBtn);
            btnLaunch = view.findViewById(R.id.launchBtn);
        }
    }
}
