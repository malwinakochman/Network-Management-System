package org.example;

import lombok.Data;

@Data
public class Device {
    private Long id;
    private String name;
    private boolean active;
}