# Network Management System

A Spring Boot application for managing device network with real-time streaming.

## Project Overview

This project implements a system for managing device networks distributed across various locations (Warsaw, Kraków, Wrocław, etc.). The application enables:

- **Device Management** - update device details and status
- **Connection Tracking** - tracking connections between devices

## Technologies

- **Java 24**
- **Spring Boot 4.0.5**
- **Maven**
- **Lombok**
- **Jackson**
- **Server-Sent Events (SSE)**

## Quick Start

### Installation and Running

1. **Build the project**
```bash
mvn clean compile
```

3. **Run the application**
```bash
mvn spring-boot:run
```

The application will be available at `http://localhost:8080`

## Documentation

### Swagger
```http
http://localhost:8080/swagger-ui.html
```

## Configuration

Network topology is defined in `src/main/resources/topology.json`:

```json
{
  "devices": [
    {
      "id": 0,
      "name": "Warsaw",
      "active": true
    }
  ],
  "connections": [
    {
      "from": 0,
      "to": 1
    }
  ]
}
```

## Testing

To run tests:

```bash
mvn test
```

## Author

Malwina Kochman

---

**Last updated:** April 2026