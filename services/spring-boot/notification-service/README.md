# Notification Service

The Notification Service manages outbound communications, including calendar invites, upcoming interview reminders, and
system alerts.

## Port Configuration

* **Local Development Port:** `8085`

## POM Contents (`pom.xml`)

This module inherits from the `innerview-spring-parent`. Its specific dependencies include:

* **Spring Boot Starter Web:** To accept internal webhook triggers from the Matching or Session services.
* **Spring Boot Starter Mail:** Provides the `JavaMailSender` integration for dispatching SMTP emails.
* **Shared Module:** Imports `com.innerview.shared` to map user data into communication templates.
* **Note:** This service currently does not connect to the database directly, relying on data passed from other services
  via its REST API.