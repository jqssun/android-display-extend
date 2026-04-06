package io.github.jqssun.displayextend.job;

import android.view.Display;
import android.view.DisplayInfo;
import android.view.InputDevice;

import io.github.jqssun.displayextend.shizuku.ServiceUtils;

import java.util.Map;

public class BindInputToDisplay implements Job {
    private final AcquireShizuku acquireShizuku = new AcquireShizuku();
    private final InputDevice inputDevice;
    private final Display display;

    public BindInputToDisplay(InputDevice inputDevice, Display display) {
        this.inputDevice = inputDevice;
        this.display = display;
    }

    @Override
    public void start() throws YieldException {
        acquireShizuku.start();
        if (!acquireShizuku.acquired) {
            return;
        }
        Map<String, String> inputDeviceDescriptorToPortMap = InputRouting.getInputDeviceDescriptorToPortMap();
        DisplayInfo displayInfo = ServiceUtils.getDisplayManager().getDisplayInfo(display.getDisplayId());
        InputRouting.bindInputToDisplay(displayInfo, inputDevice, ServiceUtils.getInputManager(), inputDeviceDescriptorToPortMap);
    }
}
