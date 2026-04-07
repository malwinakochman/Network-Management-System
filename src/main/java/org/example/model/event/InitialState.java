package org.example.model.event;

import lombok.Data;

import java.util.Set;

@Data
public class InitialState extends Event {
    private Set<Long> deviceIds;
}
