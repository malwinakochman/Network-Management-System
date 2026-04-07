package org.example;

import lombok.Data;

import java.util.Set;

@Data
public class Action extends Event {
    private Long deviceId;
}
