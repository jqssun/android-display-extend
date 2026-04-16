package io.github.jqssun.displayextend;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.content.Intent;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class DisplayListFragment extends Fragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setExitTransition(new com.google.android.material.transition.MaterialSharedAxis(com.google.android.material.transition.MaterialSharedAxis.X, true));
        setReenterTransition(new com.google.android.material.transition.MaterialSharedAxis(com.google.android.material.transition.MaterialSharedAxis.X, false));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_display_list, container, false);

        DisplayManager displayManager = (DisplayManager) requireContext().getSystemService(Context.DISPLAY_SERVICE);
        Display[] displays = displayManager.getDisplays();

        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        List<Display> displayList = new ArrayList<>();
        for (Display display : displays) {
            if (display.getDisplayId() != State.bridgeDisplayId && display.getDisplayId() != State.mirrorDisplayId) {
                displayList.add(display);
            }
        }
        recyclerView.setAdapter(new DisplayAdapter(displayList, this::_onDisplayItemClick));

        view.findViewById(R.id.openCastBtn).setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setAction(android.provider.Settings.ACTION_CAST_SETTINGS);
            startActivity(intent);
        });

        view.findViewById(R.id.screenOffBtn).setOnClickListener(v -> {
            if (State.lastSingleAppDisplay <= 0) {
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.no_cast_title))
                    .setMessage(getString(R.string.no_cast_message))
                    .setPositiveButton(getString(R.string.got_it), null)
                    .show();
            } else {
                Intent intent = new Intent(getActivity(), PureBlackActivity.class);
                android.app.ActivityOptions options = android.app.ActivityOptions.makeBasic();
                startActivity(intent, options.toBundle());
            }
        });

        return view;
    }

    private void _onDisplayItemClick(Display display) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).navigateToDisplayDetail(display.getDisplayId());
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
                btnViewDetail = view.findViewById(R.id.viewDetailBtn);
            }
        }
    }
}
