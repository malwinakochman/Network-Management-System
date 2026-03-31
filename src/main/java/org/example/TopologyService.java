package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class TopologyService {
    private List<Device> devices;

    @PostConstruct
    public void load() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Map data = mapper.readValue(
                new File("src/main/resources/topology.json"),
                Map.class
        );

        List<Device> devices = (List<Device>) data.get("devices");
        log.info("Załadowano {} urządzeń", devices.size());
    }

    public List<Device> getDevices() {
        return devices;
    }
}
