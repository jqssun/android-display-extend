package io.github.jqssun.displayextend;

import android.app.ActivityOptions;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import io.github.jqssun.displayextend.shizuku.ServiceUtils;
import io.github.jqssun.displayextend.shizuku.ShizukuUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.ViewHolder> {
    private List<ApplicationInfo> appList;
    private List<ApplicationInfo> filteredList;
    private int targetDisplayId;
    private PackageManager packageManager;

    public AppListAdapter(List<ApplicationInfo> appList, PackageManager packageManager, int targetDisplayId) {
        this.packageManager = packageManager;
        this.targetDisplayId = targetDisplayId;
        this.appList = appList != null ? appList : Collections.emptyList();
        _sortAppList(this.appList);
        this.filteredList = new ArrayList<>(this.appList);
    }

    private void _sortAppList(List<ApplicationInfo> appList) {
        Collections.sort(appList, (app1, app2) -> {
            Long time1 = Pref.getLaunchTime(app1.packageName);
            Long time2 = Pref.getLaunchTime(app2.packageName);
            
            if (time1.equals(time2)) {
                return app1.loadLabel(packageManager)
                    .toString()
                    .compareToIgnoreCase(app2.loadLabel(packageManager).toString());
            }
            return time2.compareTo(time1);
        });
    }

    public void filter(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(appList);
        } else {
            String lowerQuery = query.toLowerCase();
            for (ApplicationInfo app : appList) {
                String appName = app.loadLabel(packageManager).toString().toLowerCase();
                String packageName = app.packageName.toLowerCase();
                if (appName.contains(lowerQuery) || packageName.contains(lowerQuery)) {
                    filteredList.add(app);
                }
            }
        }
        _sortAppList(filteredList);
        notifyDataSetChanged();
    }

    public void updateAppList(List<ApplicationInfo> newAppList) {
        this.appList = newAppList != null ? newAppList : Collections.emptyList();
        _sortAppList(this.appList);
        this.filteredList = new ArrayList<>(this.appList);
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ApplicationInfo app = filteredList.get(position);
        holder.text1.setText(app.loadLabel(packageManager));
        holder.text2.setText(app.packageName);
        
        try {
            holder.appIcon.setImageDrawable(packageManager.getApplicationIcon(app.packageName));
        } catch (PackageManager.NameNotFoundException e) {
            holder.appIcon.setImageResource(android.R.drawable.sym_def_app_icon);
        }

        holder.btnLaunch.setOnClickListener(v -> {
            if (!ShizukuUtils.hasPermission() && Pref.getLaunchTime(app.packageName) == 0) {
                Toast.makeText(v.getContext(), v.getContext().getString(R.string.first_launch_hint), Toast.LENGTH_SHORT).show();
                return;
            }
            Pref.setLastPackageName(app.packageName);
            Pref.setLaunchTime(app.packageName, System.currentTimeMillis());
            ServiceUtils.launchPackage(v.getContext(), app.packageName, targetDisplayId);
            if (State.floatingButtonService != null) {
                State.floatingButtonService.onSingleAppLaunched();
            }
        });
        holder.btnLaunchToDefaultDisplay.setOnClickListener(v -> {
            Intent launchIntent = packageManager.getLaunchIntentForPackage(app.packageName);
            if (launchIntent != null) {
                Pref.setLaunchTime(app.packageName, System.currentTimeMillis());
                ActivityOptions options = ActivityOptions.makeBasic();
                v.getContext().startActivity(launchIntent, options.toBundle());
            }
        });
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView appIcon;
        TextView text1;
        TextView text2;
        Button btnLaunch;
        Button btnLaunchToDefaultDisplay;

        ViewHolder(View view) {
            super(view);
            appIcon = view.findViewById(R.id.app_icon);
            text1 = view.findViewById(R.id.text1);
            text2 = view.findViewById(R.id.text2);
            btnLaunch = view.findViewById(R.id.launchBtn);
            btnLaunchToDefaultDisplay = view.findViewById(R.id.launchToDefaultBtn);
        }
    }
}