package org.example.model;

import lombok.Data;

@Data
public class Device {
    private Long id;
    private String name;
    private boolean active;
}