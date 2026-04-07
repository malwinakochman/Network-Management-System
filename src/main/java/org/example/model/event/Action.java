package org.example.model.event;

import lombok.Data;

@Data
public class Action extends Event {
    private Long deviceId;
}
