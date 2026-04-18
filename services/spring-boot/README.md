# User Service

The User Service is a core domain service responsible for managing user profiles, authentication logic, and
professional/technical skills data.

## Port Configuration

* **Local Development Port:** `8081`

## POM Contents (`pom.xml`)

This module inherits from the `innerview-spring-parent`. Its specific dependencies include:

* **Spring Boot Starter Web:** For standard synchronous REST controllers (Spring MVC).
* **Spring Boot Starter Data JPA:** For ORM and database interactions using Hibernate.
* **PostgreSQL:** The JDBC driver for connecting to the primary PostgreSQL database.
* **Spring Boot Starter Security:** For handling secure endpoints and password hashing.
* **Shared Module:** Imports `com.innerview.shared` for common DTOs, exception handlers, and security utilities.