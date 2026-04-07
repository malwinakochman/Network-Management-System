package org.example;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/devices")
@RequiredArgsConstructor
public class DeviceController {
    private final TopologyService topologyService;
    private final Map<Long, List<SseEmitter>> emitters = new HashMap<>();

    @PatchMapping("/{id}")
    public DeviceResponse updateDevice(@PathVariable Long id, @RequestBody DeviceRequest request) {
        Map<Long, Set<Long>> beforeState = new HashMap<>();
        for (Long subscriberId : emitters.keySet()) {
            beforeState.put(subscriberId, new HashSet<>(topologyService.getReachableDevices(subscriberId)));
        }
        Device device = topologyService.updateDevice(id, request);
        emitReachabilityChanges(beforeState);
        return DeviceResponse.toResponse(device);
    }

    @GetMapping("/{id}/reachable-devices")
    public SseEmitter streamReachableDevices(@PathVariable Long id) {
        SseEmitter emitter = new SseEmitter(300000L);
        emitters.computeIfAbsent(id, k -> new CopyOnWriteArrayList<>()).add(emitter);

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
        List<SseEmitter> list = emitters.get(id);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                emitters.remove(id);
            }
        }
    }

    private void emitReachabilityChanges(Map<Long, Set<Long>> beforeState) {
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
