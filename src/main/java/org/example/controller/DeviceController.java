package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.example.dto.DeviceRequest;
import org.example.dto.DeviceResponse;
import org.example.model.Device;
import org.example.model.event.Action;
import org.example.model.event.EventType;
import org.example.model.event.InitialState;
import org.example.service.TopologyService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/devices")
@RequiredArgsConstructor
public class DeviceController {
    private final TopologyService topologyService;
    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    @Operation(summary = "Update device (name or active status)")
    @PatchMapping("/{id}")
    public DeviceResponse updateDevice(@PathVariable Long id, @RequestBody DeviceRequest request) {
        Map<Long, Set<Long>> beforeState = new HashMap<>();
        synchronized (emitters) {
            for (Long subscriberId : emitters.keySet()) {
                beforeState.put(subscriberId, new HashSet<>(topologyService.getReachableDevices(subscriberId)));
            }
        }
        Device device = topologyService.updateDevice(id, request);
        emitReachabilityChanges(beforeState);
        return DeviceResponse.toResponse(device);
    }

    @Operation(
            summary = "Stream reachable devices",
            description = """
                Stream returning types:
                - INITIAL_STATE: full list of reachable devices
                - ADDED: device became reachable
                - REMOVED: device became unreachable
                """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Stream of device updates",
            content = @Content(
                    mediaType = "text/event-stream",
                    examples = @ExampleObject(
                            value = """
                        data:{"deviceIds":[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19],"type":"INITIAL_STATE"}
                        data:{"deviceId":9,"type":"REMOVED"}
                        data:{"deviceId":4,"type":"ADDED"}
                        """
                    )
            )
    )
    @GetMapping(path = "/{id}/reachable-devices", produces = "text/event-stream")
    public SseEmitter streamReachableDevices(@PathVariable Long id) {
        SseEmitter emitter = new SseEmitter(300000L);
        synchronized (emitters) {
            emitters.computeIfAbsent(id, k -> new CopyOnWriteArrayList<>()).add(emitter);
        }

        Set<Long> initialState = topologyService.getReachableDevices(id);

        try {
            InitialState state = new InitialState();
            state.setType(EventType.INITIAL_STATE);
            state.setDeviceIds(initialState);
            emitter.send(SseEmitter.event().data(state));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }

        emitter.onCompletion(() -> cleanup(emitter, id));
        emitter.onTimeout(() -> cleanup(emitter, id));

        return emitter;
    }

    private void cleanup(SseEmitter emitter, Long id) {
        synchronized (emitters) {
            List<SseEmitter> list = emitters.get(id);
            if (list != null) {
                list.remove(emitter);
                if (list.isEmpty()) {
                    emitters.remove(id);
                }
            }
        }
    }

    @Operation(summary = "Get all devices")
    @GetMapping
    public List<DeviceResponse> getAllDevices() {
        return new ArrayList<>(topologyService.getDevices());
    }

    @Operation(summary = "Get device connections")
    @ApiResponse(
            responseCode = "200",
            description = "List of devices which are connected",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            value = "[0,10,12]"
                    )
            )
    )
    @GetMapping("/{id}/connections")
    public Set<Long> getDeviceConnections(@PathVariable Long id) {
        return topologyService.getConnectionsForDevice(id);
    }

    private void emitReachabilityChanges(Map<Long, Set<Long>> beforeState) {
        synchronized (emitters) {
            for (Map.Entry<Long, List<SseEmitter>> entry : emitters.entrySet()) {
                Long subscriberId = entry.getKey();
                Set<Long> before = beforeState.getOrDefault(subscriberId, new HashSet<>());
                Set<Long> after = topologyService.getReachableDevices(subscriberId);
                
                Set<Long> removed = new HashSet<>(before);
                removed.removeAll(after);
                
                Set<Long> added = new HashSet<>(after);
                added.removeAll(before);
                
                if (removed.isEmpty() && added.isEmpty()) {
                    continue;
                }
                
                List<SseEmitter> emittersList = entry.getValue();
                for (SseEmitter emitter : emittersList) {
                    for (Long deviceId : removed) {
                        try {
                            Action event = new Action();
                            event.setType(EventType.REMOVED);
                            event.setDeviceId(deviceId);
                            emitter.send(SseEmitter.event().data(event));
                        } catch (IOException e) {
                            emitter.completeWithError(e);
                        }
                    }
                    
                    for (Long deviceId : added) {
                        try {
                            Action event = new Action();
                            event.setType(EventType.ADDED);
                            event.setDeviceId(deviceId);
                            emitter.send(SseEmitter.event().data(event));
                        } catch (IOException e) {
                            emitter.completeWithError(e);
                        }
                    }
                }
            }
        }
    }
}
