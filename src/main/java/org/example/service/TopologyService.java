package org.example.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.example.dto.DeviceResponse;
import org.example.model.Device;
import org.example.dto.DeviceRequest;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TopologyService {
    private Map<Long, Device> devices;
    private Map<Long, Set<Long>> connections;

    @PostConstruct
    public void load() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> data = mapper.readValue(
                new File("src/main/resources/topology.json"),
                new TypeReference<>() {}
        );

        List<Device> devicesList = mapper.convertValue(data.get("devices"), new TypeReference<>() {});
        devices = devicesList.stream().collect(Collectors.toMap(Device::getId, d -> d));

        connections = new HashMap<>();
        devices.keySet().forEach(id -> connections.put(id, new HashSet<>()));
        List<Map<String, Long>> connectionsList = mapper.convertValue(data.get("connections"), new TypeReference<>() {});
        connectionsList.forEach(connection -> {
            Long from = connection.get("from");
            Long to = connection.get("to");
            connections.get(from).add(to);
            connections.get(to).add(from);
        });
    }

    public Device updateDevice(Long id, DeviceRequest request) {
        Device device = devices.get(id);
        if (request.getName() != null && !request.getName().isEmpty()) {
            device.setName(request.getName());
        }
        device.setActive(request.isActive());
        return device;
    }

    public Set<Long> getReachableDevices(Long startDeviceId) {
        Set<Long> reachable = new HashSet<>();
        Set<Long> visited = new HashSet<>();
        Queue<Long> queue = new LinkedList<>();

        queue.add(startDeviceId);
        visited.add(startDeviceId);
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            for (Long connection : connections.getOrDefault(current, Collections.emptySet())) {
                if (visited.contains(connection)) {
                    continue;
                }
                visited.add(connection);
                Device device = devices.get(connection);

                if (device != null && device.isActive()) {
                    reachable.add(connection);
                    queue.add(connection);
                }
            }
        }
        return reachable;
    }

    public Set<Long> getConnectionsForDevice(Long id) {
        return new HashSet<>(connections.getOrDefault(id, Collections.emptySet()));
    }

    public List<DeviceResponse> getDevices() {
        return devices.values().stream()
                .map(DeviceResponse::toResponse)
                .collect(Collectors.toList());
    }
}
