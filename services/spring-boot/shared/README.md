# Shared Library

The Shared module is a standard Java library that contains code reused across multiple Spring Boot microservices within the InnerView platform. 

## Port Configuration
* **N/A:** This is a library, not an executable application. It does not run on a server or bind to a port.

## POM Contents (`pom.xml`)
This module inherits from the `innerview-spring-parent`. Its specific dependencies include:
* **Spring Boot Starter Web:** Included so you can write common `@ControllerAdvice` global exception handlers.
* **Spring Boot Starter Data JPA:** Included so you can define base entity classes (like `BaseEntity` with audit fields `createdAt`, `updatedAt`).
* **Spring Boot Starter Security:** Included to write common JWT parsing logic or shared `SecurityFilterChain` configurations.
* **Note:** This POM intentionally omits the `spring-boot-maven-plugin`. Maven compiles this into a standard `.jar` file to be imported by the other microservices.