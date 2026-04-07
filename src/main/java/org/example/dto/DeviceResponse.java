package org.example.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.example.model.Device;

@Data
@AllArgsConstructor
public class DeviceResponse {
    private Long id;
    private String name;
    private boolean active;

    public static DeviceResponse toResponse(Device device) {
        return new DeviceResponse(device.getId(), device.getName(), device.isActive());
    }
}
