# Problem Hosting Service

This service acts as the repository for coding and system design problems used during mock interviews, categorizing them by difficulty and topic.

## Port Configuration
* **Local Development Port:** `8083`

## POM Contents (`pom.xml`)
This module inherits from the `innerview-spring-parent`. Its specific dependencies include:
* **Spring Boot Starter Web:** For serving problem descriptions and test cases.
* **Spring Boot Starter Data JPA:** For querying the problem catalog.
* **PostgreSQL:** The database driver.
* **Shared Module:** Imports `com.innerview.shared` to ensure API responses adhere to the platform's common data contracts.