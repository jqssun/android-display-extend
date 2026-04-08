package io.github.jqssun.displayextend;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import io.github.jqssun.displayextend.job.FetchLogAndShare;
import io.github.jqssun.displayextend.shizuku.ShizukuUtils;

public class LogsFragment extends Fragment {
    private RecyclerView logRecyclerView;
    private LogAdapter logAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_logs, container, false);

        logRecyclerView = view.findViewById(R.id.logRecyclerView);
        logAdapter = new LogAdapter(State.logs);
        logRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        logRecyclerView.setAdapter(logAdapter);
        _scrollToBottom();

        view.findViewById(R.id.exportLogsBtn).setOnClickListener(v -> {
            if (!ShizukuUtils.hasPermission()) {
                Toast.makeText(getContext(), getString(R.string.export_log_requires_shizuku), Toast.LENGTH_SHORT).show();
                return;
            }
            State.startNewJob(new FetchLogAndShare(getContext()));
        });

        view.findViewById(R.id.clearLogsBtn).setOnClickListener(v -> {
            State.logs.clear();
            logAdapter.notifyDataSetChanged();
        });

        State.logVersion.observe(getViewLifecycleOwner(), version -> _refreshLogs());

        return view;
    }

    private void _refreshLogs() {
        if (logAdapter != null) {
            logAdapter.notifyDataSetChanged();
            _scrollToBottom();
        }
    }

    private void _scrollToBottom() {
        if (logAdapter != null && logAdapter.getItemCount() > 0) {
            logRecyclerView.scrollToPosition(logAdapter.getItemCount() - 1);
        }
    }
}
