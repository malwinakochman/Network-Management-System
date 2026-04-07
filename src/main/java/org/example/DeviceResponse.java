package org.example;

import lombok.AllArgsConstructor;
import lombok.Data;

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
