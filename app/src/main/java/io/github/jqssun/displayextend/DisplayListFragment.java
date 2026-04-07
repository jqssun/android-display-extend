package io.github.jqssun.displayextend;

import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Display;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class DisplayListFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_display_list, container, false);

        DisplayManager displayManager = (DisplayManager) getContext().getSystemService(Context.DISPLAY_SERVICE);
        Display[] displays = displayManager.getDisplays();

        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        List<Display> displayList = new ArrayList<>();
        for (Display display : displays) {
            if (display.getDisplayId() != State.bridgeDisplayId && display.getDisplayId() != State.mirrorDisplayId) {
                displayList.add(display);
            }
        }
        recyclerView.setAdapter(new DisplayAdapter(displayList, this::onDisplayItemClick));

        view.findViewById(R.id.btnOpenCast).setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setAction(android.provider.Settings.ACTION_CAST_SETTINGS);
            startActivity(intent);
        });

        return view;
    }

    private void onDisplayItemClick(Display display) {
        if (getActivity() instanceof IMainActivity) {
            ((IMainActivity) getActivity()).navigateToDetail(
                DisplayDetailFragment.newInstance(display.getDisplayId())
            );
        }
    }

    private static class DisplayAdapter extends RecyclerView.Adapter<DisplayAdapter.ViewHolder> {
        private final List<Display> displayList;
        private final OnDisplayClickListener clickListener;

        interface OnDisplayClickListener {
            void onDisplayClick(Display display);
        }

        public DisplayAdapter(List<Display> displayList, OnDisplayClickListener listener) {
            this.displayList = displayList;
            this.clickListener = listener;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_display, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Display display = displayList.get(position);
            Context ctx = holder.itemView.getContext();
            String displayInfo = String.format(ctx.getString(R.string.display_id_format),
                display.getDisplayId(),
                display.getWidth(),
                display.getHeight());
            holder.displayId.setText(displayInfo);
            holder.displayName.setText(ctx.getString(R.string.display_name_format, display.getName()));

            holder.itemView.setOnClickListener(v -> clickListener.onDisplayClick(display));
            holder.btnViewDetail.setOnClickListener(v -> clickListener.onDisplayClick(display));
        }

        @Override
        public int getItemCount() {
            return displayList.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            public final TextView displayId;
            public final TextView displayName;
            public final MaterialButton btnViewDetail;

            public ViewHolder(View view) {
                super(view);
                displayId = view.findViewById(R.id.display_id);
                displayName = view.findViewById(R.id.display_name);
                btnViewDetail = view.findViewById(R.id.btn_view_detail);
            }
        }
    }
}
