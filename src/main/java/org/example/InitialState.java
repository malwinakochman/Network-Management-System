package org.example;

import lombok.Data;

import java.util.Set;

@Data
public class InitialState extends Event {
    private Set<Long> deviceIds;
}
