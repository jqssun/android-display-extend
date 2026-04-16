package io.github.jqssun.displayextend.job;

import io.github.jqssun.displayextend.Pref;
import io.github.jqssun.displayextend.R;
import io.github.jqssun.displayextend.State;
import io.github.jqssun.displayextend.shizuku.ServiceUtils;
import io.github.jqssun.displayextend.shizuku.ShizukuUtils;

import android.hardware.input.IInputManager;
import android.hardware.input.InputManager;
import android.hardware.usb.UsbDevice;
import android.os.RemoteException;
import android.view.DisplayAddress;
import android.view.DisplayInfo;
import android.view.InputDevice;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

public class InputRouting {
    public static Map<String, String> getInputDeviceDescriptorToPortMap() {
        if (State.userService == null) {
            State.log("user service not running, cannot get input device descriptor->port mapping");
            return new HashMap<>();
        }
        Map<String, String> map = new HashMap<>();
        try {
            String inputDump = State.userService.dumpInput();
            String[] lines = inputDump.split("\n");
            String lastDescriptor = "";
            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("Descriptor:")) {
                    lastDescriptor = line.substring("Descriptor:".length()).trim();
                }
                if (line.startsWith("Location:")) {
                    String inputPort = line.substring("Location:".length()).trim();
                    map.put(lastDescriptor, inputPort);
                }
            }
        } catch (RemoteException ex) {
            throw new RuntimeException(ex);
        }
        return map;
    }

    public static void bindInputToDisplay(DisplayInfo displayInfo, InputDevice inputDevice, IInputManager inputManager, Map<String, String> inputDeviceDescriptorToPortMap) {
        if (!inputDevice.isExternal()) {
            return;
        }
        State.log("binding device " + inputDevice.getId());
        try {
            inputManager.removeUniqueIdAssociationByDescriptor(inputDevice.getDescriptor());
            inputManager.addUniqueIdAssociationByDescriptor(inputDevice.getDescriptor(), String.valueOf(displayInfo.uniqueId));
            State.log("input routing updated: " + inputDevice.getName() + ", " + inputDevice.getDescriptor());
        } catch(Throwable e) {
            String inputPort = inputDeviceDescriptorToPortMap.get(inputDevice.getDescriptor());
            if (inputPort == null) {
                State.log("failed to update input routing: " + inputDevice + ", " + e.getMessage());
            } else {
                try {
                    inputManager.removeUniqueIdAssociation(inputPort);
                    inputManager.addUniqueIdAssociation(inputPort, String.valueOf(displayInfo.uniqueId));
                    State.log("input routing updated: " + inputDevice.getName() + ", " + inputPort + " => " + displayInfo.uniqueId);
                } catch(Throwable e2) {
                    try {
                        inputManager.removePortAssociation(inputPort);
                        int displayPort = ((DisplayAddress.Physical) displayInfo.address).getPort();
                        inputManager.addPortAssociation(inputPort, displayPort);
                        State.log("input routing updated: " + inputDevice.getName() + ", " + inputPort + " => " + displayPort);
                    } catch(Throwable e3) {
                        State.log("input port fallback also failed: " + inputDevice.getName() + ", " + e3.getMessage());
                        if (ShizukuUtils.hasPermission() && State.currentActivity.get() != null) {
                            Toast.makeText(State.currentActivity.get(), State.currentActivity.get().getString(R.string.input_routing_limitation), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        }
    }

    public static InputDevice findInputDevice(InputManager inputManager, UsbDevice usbDevice) {
        for(int inputDeviceId : inputManager.getInputDeviceIds()) {
            InputDevice inputDevice = inputManager.getInputDevice(inputDeviceId);
            if (inputDevice.isExternal() && inputDevice.getVendorId() == usbDevice.getVendorId() && inputDevice.getProductId() == usbDevice.getProductId()) {
                return inputDevice;
            }
        }
        return null;
    }

    public static void bindAllExternalInputToDisplay(int displayId) {
        if (!Pref.getAutoBindInput()) {
            State.log("skipping input binding (disabled in settings)");
            return;
        }
        DisplayInfo displayInfo = ServiceUtils.getDisplayManager().getDisplayInfo(displayId);
        IInputManager inputManager = ServiceUtils.getInputManager();
        Map<String, String> inputDeviceDescriptorToPortMap = InputRouting.getInputDeviceDescriptorToPortMap();
        for (int deviceId : inputManager.getInputDeviceIds()) {
            InputDevice inputDevice = inputManager.getInputDevice(deviceId);
            InputRouting.bindInputToDisplay(displayInfo, inputDevice, inputManager, inputDeviceDescriptorToPortMap);
        }
    }

}
