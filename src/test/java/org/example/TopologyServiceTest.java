package org.example;

import org.example.dto.DeviceRequest;
import org.example.dto.DeviceResponse;
import org.example.exception.DeviceNotFoundException;
import org.example.model.Device;
import org.example.service.TopologyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class TopologyServiceTest {

    private TopologyService topologyService;

    @BeforeEach
    void setUp() throws Exception {
        topologyService = new TopologyService();

        Map<Long, Device> testDevices = new HashMap<>();

        Device device1 = new Device();
        device1.setId(1L);
        device1.setName("Kraków");
        device1.setActive(true);

        Device device2 = new Device();
        device2.setId(2L);
        device2.setName("Wrocław");
        device2.setActive(true);

        Device device3 = new Device();
        device3.setId(3L);
        device3.setName("Poznań");
        device3.setActive(false);

        Device device4 = new Device();
        device4.setId(4L);
        device4.setName("Gdańsk");
        device4.setActive(true);

        testDevices.put(1L, device1);
        testDevices.put(2L, device2);
        testDevices.put(3L, device3);
        testDevices.put(4L, device4);

        Map<Long, Set<Long>> testConnections = new HashMap<>();
        testConnections.put(1L, new HashSet<>(Arrays.asList(2L)));
        testConnections.put(2L, new HashSet<>(Arrays.asList(1L, 3L)));
        testConnections.put(3L, new HashSet<>(Arrays.asList(2L, 4L)));
        testConnections.put(4L, new HashSet<>(Arrays.asList(3L)));

        setField(topologyService, "devices", testDevices);
        setField(topologyService, "connections", testConnections);
    }

    private void setField(Object object, String fieldName, Object value) throws Exception {
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(object, value);
    }

    @Test
    void shouldReturnAllDevices() {
        // When
        List<DeviceResponse> devices = topologyService.getDevices();
        // Then
        assertEquals(4, devices.size());
    }

    @Test
    void shouldReturnCorrectDeviceData() {
        // When
        List<DeviceResponse> devices = topologyService.getDevices();
        // Then
        Optional<DeviceResponse> router = devices.stream()
                .filter(d -> d.getId().equals(1L))
                .findFirst();

        assertTrue(router.isPresent());
        assertEquals("Kraków", router.get().getName());
        assertTrue(router.get().isActive());
    }

    @Test
    void shouldUpdateDeviceName() {
        // Given
        DeviceRequest request = new DeviceRequest("Cracow", true);
        // When
        Device updated = topologyService.updateDevice(1L, request);
        // Then
        assertEquals("Cracow", updated.getName());
        assertEquals(1L, updated.getId());
    }

    @Test
    void shouldUpdateDeviceActiveStatus() {
        // Given
        DeviceRequest request = new DeviceRequest("Kraków", false);
        // When
        Device updated = topologyService.updateDevice(1L, request);
        // Then
        assertFalse(updated.isActive());
    }

    @Test
    void shouldReturnDeviceConnections() {
        // When
        Set<Long> connections = topologyService.getConnectionsForDevice(1L);
        // Then
        assertEquals(1, connections.size());
        assertTrue(connections.contains(2L));
    }

    @Test
    void shouldReturnMultipleConnections() {
        // When
        Set<Long> connections = topologyService.getConnectionsForDevice(2L);
        // Then
        assertEquals(2, connections.size());
        assertTrue(connections.contains(1L));
        assertTrue(connections.contains(3L));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonexistentDevice() {
        // Given
        DeviceRequest request = new DeviceRequest("Test", true);
        // When & Then
        assertThrows(DeviceNotFoundException.class, () -> {
            topologyService.updateDevice(999L, request);
        });
    }

    @Test
    void shouldThrowExceptionForNonexistentDevice() {
        assertThrows(DeviceNotFoundException.class, () -> {
            topologyService.getConnectionsForDevice(999L);
        });
    }

    @Test
    void shouldFindAllReachableActiveDevices() {
        // When
        Set<Long> reachable = topologyService.getReachableDevices(1L);
        // Then
        assertEquals(1, reachable.size());
        assertTrue(reachable.contains(2L));
        assertFalse(reachable.contains(3L));
    }

    @Test
    void shouldNotReachThroughInactiveDevice() {
        // When
        Set<Long> reachable = topologyService.getReachableDevices(2L);

        // Then
        assertEquals(1, reachable.size());
        assertTrue(reachable.contains(1L));
        assertFalse(reachable.contains(3L));
        assertFalse(reachable.contains(4L));
    }

    @Test
    void shouldNotIncludeStartDeviceInReachable() {
        // When
        Set<Long> reachable = topologyService.getReachableDevices(1L);
        // Then
        assertFalse(reachable.contains(1L));
    }

    @Test
    void shouldReachAllActiveDevicesWhenAllActive() throws Exception {
        // Given
        Map<Long, Device> devices = getPrivateField(topologyService, "devices");
        devices.get(3L).setActive(true);
        // When
        Set<Long> reachable = topologyService.getReachableDevices(1L);
        // Then
        assertEquals(3, reachable.size());
        assertTrue(reachable.contains(2L));
        assertTrue(reachable.contains(3L));
        assertTrue(reachable.contains(4L));
    }

    @Test
    void shouldReturnEmptyForDeviceWithoutConnections() throws Exception {
        // Given
        Map<Long, Device> devices = getPrivateField(topologyService, "devices");
        Device singleDevice = new Device();
        singleDevice.setId(5L);
        singleDevice.setName("Device without any connections");
        singleDevice.setActive(true);
        devices.put(5L, singleDevice);
        // When
        Set<Long> reachable = topologyService.getReachableDevices(5L);
        // Then
        assertTrue(reachable.isEmpty());
    }

    @SuppressWarnings("unchecked")
    private <T> T getPrivateField(Object object, String fieldName) throws Exception {
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T) field.get(object);
    }
}