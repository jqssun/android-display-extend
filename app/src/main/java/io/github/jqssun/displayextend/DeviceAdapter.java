package io.github.jqssun.displayextend;

import android.view.InputDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;



import java.util.List;
import java.util.function.Consumer;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {
    private List<InputDevice> devices;
    private Consumer<InputDevice> onViewClick;

    public DeviceAdapter(List<InputDevice> devices, Consumer<InputDevice> onViewClick) {
        this.devices = devices;
        this.onViewClick = onViewClick;
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_input_device, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        InputDevice device = devices.get(position);
        holder.tvDeviceName.setText(device.getName());
        holder.btnView.setOnClickListener(v -> onViewClick.accept(device));
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    static class DeviceViewHolder extends RecyclerView.ViewHolder {
        TextView tvDeviceName;
        Button btnView;

        DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDeviceName = itemView.findViewById(R.id.tvDeviceName);
            btnView = itemView.findViewById(R.id.btnView);
        }
    }
} 