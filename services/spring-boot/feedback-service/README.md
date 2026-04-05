# Feedback Service

The Feedback Service handles the submission, storage, and retrieval of structured performance feedback and peer ratings after a mock interview concludes.

## Port Configuration
* **Local Development Port:** `8084`

## POM Contents (`pom.xml`)
This module inherits from the `innerview-spring-parent`. Its specific dependencies include:
* **Spring Boot Starter Web:** For endpoints handling feedback submission and profile rating aggregation.
* **Spring Boot Starter Data JPA:** For persisting feedback records.
* **PostgreSQL:** The database driver.
* **Shared Module:** Imports `com.innerview.shared` to utilize common domain models (e.g., User IDs, Session IDs).