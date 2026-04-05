# API Gateway

The API Gateway serves as the single entry point for the InnerView platform, routing all incoming frontend HTTP traffic to the appropriate backend microservices.

## Port Configuration
* **Local Development Port:** `8080`

## POM Contents (`pom.xml`)
This module inherits from the `innerview-spring-parent`. Its specific dependencies include:
* **Spring Cloud Starter Gateway:** Uses Spring WebFlux (Netty) under the hood to handle non-blocking, asynchronous routing.
* **Shared Module:** Imports `com.innerview.shared` to access common security configurations, allowing the gateway to perform initial JWT token validation before routing requests.
* **Note:** This service intentionally excludes `spring-boot-starter-web` (MVC) and database dependencies to prevent conflicts with WebFlux.