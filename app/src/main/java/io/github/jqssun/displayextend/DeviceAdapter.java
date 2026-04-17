package io.github.jqssun.displayextend;

import androidx.annotation.DrawableRes;
import android.view.InputDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.function.Consumer;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {
    private final List<InputDevice> devices;
    private final Consumer<InputDevice> onClick;
    @DrawableRes
    private final int deviceIconRes;

    public DeviceAdapter(List<InputDevice> devices, Consumer<InputDevice> onClick,
                         @DrawableRes int deviceIconRes) {
        this.devices = devices;
        this.onClick = onClick;
        this.deviceIconRes = deviceIconRes;
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
        holder.deviceId.setText(String.format("Device %d (%04X:%04X)",
                device.getId(), device.getVendorId(), device.getProductId()));
        holder.deviceName.setText(device.getName());
        holder.deviceIcon.setImageResource(deviceIconRes);
        holder.itemView.setOnClickListener(v -> onClick.accept(device));
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    static class DeviceViewHolder extends RecyclerView.ViewHolder {
        final TextView deviceId;
        final TextView deviceName;
        final ImageView deviceIcon;

        DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            deviceId = itemView.findViewById(R.id.deviceIdText);
            deviceName = itemView.findViewById(R.id.deviceNameText);
            deviceIcon = itemView.findViewById(R.id.deviceIcon);
        }
    }
}
