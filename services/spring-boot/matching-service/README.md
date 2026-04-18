# Matching Service

The Matching Service is responsible for pairing interviewers and interviewees based on availability, technical skills,
and past performance ratings.

## Port Configuration

* **Local Development Port:** `8082`

## POM Contents (`pom.xml`)

This module inherits from the `innerview-spring-parent`. Its specific dependencies include:

* **Spring Boot Starter Web:** For exposing REST endpoints related to scheduling.
* **Spring Boot Starter Data JPA:** For storing and retrieving match histories and queued requests.
* **PostgreSQL:** The database driver.
* **Shared Module:** Imports `com.innerview.shared` for standardizing requests and accessing global error handling.