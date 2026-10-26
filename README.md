# CampusConnect — Campus Event Management Platform & CI/CD Pipeline

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.9-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue.svg)](https://www.docker.com/)
[![Jenkins](https://img.shields.io/badge/Jenkins-CI%2FCD-red.svg)](https://www.jenkins.io/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg)](https://www.mysql.com/)
[![Tests](https://img.shields.io/badge/Tests-59%20Passing-success.svg)](#testing)

**CampusConnect** is a full-stack, enterprise-grade campus event management and digital ticketing web application. It automates college event discovery, real-time seat inventory booking, verified attendee pass issuance, and comprehensive administrative oversight.

This project implements an **End-to-End DevOps CI/CD Pipeline** covering version control (Git/GitHub), automated build/testing (Maven), continuous integration (Jenkins), and containerization (Docker).

> **Automated CI/CD Trigger Active**: Commits pushed to the `main` branch automatically trigger syntax validation, test execution, artifact packaging, and Docker container builds in Jenkins.

---

## Table of Contents
- [System Architecture & Features](#system-architecture--features)
  - [Student Portal](#student-portal)
  - [Admin Management Portal](#admin-management-portal)
  - [Security & Authentication](#security--authentication)
- [DevOps & CI/CD Toolchain](#devops--cicd-toolchain)
- [Tech Stack](#tech-stack)
- [Project Directory Structure](#project-directory-structure)
- [Local Setup & Getting Started](#local-setup--getting-started)
  - [Prerequisites](#prerequisites)
  - [Database Configuration](#database-configuration)
  - [Running the Application](#running-the-application)
  - [Default Credentials](#default-credentials)
- [Containerization with Docker](#containerization-with-docker)
  - [Multi-Stage Dockerfile](#multi-stage-dockerfile)
  - [One-Click Stack with Docker Compose](#one-click-stack-with-docker-compose)
  - [Standalone Docker Container](#standalone-docker-container)
- [Jenkins CI/CD Pipeline](#jenkins-cicd-pipeline)
- [Testing & Quality Assurance](#testing--quality-assurance)

---

## System Architecture & Features

### Student Portal
- **Interactive Event Catalog**: Filter events by category (*Technical, Workshop, Hackathon, Cultural*), keyword, or schedule.
- **Real-Time Seat Management**: Prevents overbooking using database transactions; displays dynamic remaining seat indicators.
- **Digital Pass Issuance**: Generates a verified digital event pass with a formatted reference code (`CC-REG-X`) upon registration.
- **My Registrations Dashboard**: View active, upcoming, and past passes with options to view digital passes or cancel registrations.
- **Email Confirmations**: Dispatches automated booking confirmation emails with event details and entrance pass codes.

### Admin Management Portal
- **Key Metrics Dashboard**: Instant KPI cards for total events, active registrations, verified students, and capacity alerts.
- **Event Lifecycle Workspace**: Create, edit, publish (`OPEN`), cancel (`CANCELLED`), close, or archive events.
- **Registrations & Attendee Rosters**: Search attendee lists by student name, roll number, or pass code; filter by confirmation status; export attendee data.
- **Student Account Management**: Manage student profiles, view pass histories, and toggle account activation.
- **Visual Analytics & Reports**: Graphical visual charts (Chart.js) depicting event popularity, registration status distribution, and capacity utilization.

### Security & Authentication
- **Role-Based Access Control (RBAC)**: Strict segregation between `ROLE_STUDENT` and `ROLE_ADMIN`.
- **Password Protection**: BCrypt one-way hashing for all stored credentials.
- **Session Security & CSRF**: Built with Spring Security 6, session fixation protection, and CSRF token verification.

---

## DevOps & CI/CD Toolchain

The project satisfies an end-to-end DevOps case study workflow:

```mermaid
flowchart LR
    A[Developer Workstation] -->|1. Git Commit & Push| B[GitHub Repository]
    B -->|2. Webhook / SCM Poll| C[Jenkins CI/CD Server]
    subgraph Jenkins Pipeline
        C1[Compile Code] --> C2[Run 59 JUnit Tests]
        C2 --> C3[Package JAR]
        C3 --> C4[Build Docker Image]
    end
    C --> Jenkins Pipeline
    Jenkins Pipeline -->|3. Container Image| D[Docker Engine]
    D -->|4. Deploy Container| E[CampusConnect Web App :8081]
```

1. **Version Control**: Git for branch management and GitHub for remote repository hosting.
2. **Build Tool**: Apache Maven (`mvnw`) for dependency resolution, compilation, and packaging.
3. **Continuous Integration**: Jenkins declarative pipeline automating compilation, test validation, and artifact generation.
4. **Containerization**: Multi-stage Dockerfile producing a secure, minimal Alpine JRE container image.
5. **Orchestration**: Docker Compose automating multi-container deployment with MySQL 8.0.

---

## Tech Stack

| Layer | Technologies |
| :--- | :--- |
| **Backend Framework** | Spring Boot 4.0.9 (Spring MVC, Spring Data JPA, Spring Security) |
| **Java Platform** | Java 21 (LTS) |
| **View Engine** | Thymeleaf 3.1+ & Thymeleaf Extras Spring Security |
| **Frontend Styling** | Bootstrap 5.3, Bootstrap Icons, Plus Jakarta Sans, Custom CSS |
| **Data Visualizations** | Chart.js 4.4 |
| **Persistence / ORM** | Hibernate 7, Spring Data JPA |
| **Database** | MySQL 8.0 (MySQL Connector/J) |
| **Testing** | JUnit 5, Spring Boot Test, Mockito, Spring Security Test |
| **Build Automation** | Maven Wrapper 3.9+ |
| **Containerization** | Docker, Docker Compose, Eclipse Temurin 21 Alpine |
| **CI/CD Automation** | Jenkins Declarative Pipeline |

---

## Project Directory Structure

```text
campusconnect/
├── .mvn/wrapper/                 # Maven wrapper binaries
├── src/
│   ├── main/
│   │   ├── java/com/campusconnect/campusconnect/
│   │   │   ├── config/           # Security, MVC, and Data Initializer configs
│   │   │   ├── controller/       # Admin, Auth, Event, Home, Registration controllers
│   │   │   ├── dto/              # Forms and transfer objects (EventForm, DashboardStatsDto)
│   │   │   ├── exception/        # Custom domain exceptions
│   │   │   ├── model/            # JPA Entities (User, Event, EventRegistration, Role, EventStatus)
│   │   │   ├── repository/       # Spring Data JPA repositories
│   │   │   ├── service/          # Business logic (EventService, RegistrationService, EmailService)
│   │   │   ├── CampusconnectApplication.java
│   │   │   └── Event.java
│   │   └── resources/
│   │       ├── static/css/       # Custom SaaS design system (style.css)
│   │       ├── templates/        # Thymeleaf HTML views
│   │       │   ├── admin/        # Dashboard, events-list, registrations, users, reports
│   │       │   ├── auth/         # Login and register pages
│   │       │   ├── events/       # Explore directory, details, booking modals
│   │       │   ├── fragments/    # Navbar, layout headers, footers
│   │       │   ├── home/         # Landing and featured events
│   │       │   └── registrations/# My registrations and digital passes
│   │       └── application.properties # Environment-driven application settings
│   └── test/java/com/campusconnect/campusconnect/
│       ├── controller/           # Web MVC controller tests
│       ├── security/             # Role authorization and access tests
│       └── service/              # Unit tests (Event, Registration, Email services)
├── Dockerfile                    # Multi-stage production container build
├── .dockerignore                 # Docker context exclusion rules
├── docker-compose.yml            # Multi-container stack (App + MySQL)
├── Jenkinsfile                   # Declarative CI/CD pipeline script
├── mvnw / mvnw.cmd               # Cross-platform Maven wrapper scripts
├── pom.xml                       # Maven dependency and plugin descriptors
└── README.md                     # Project documentation
```

---

## Local Setup & Getting Started

### Prerequisites
- **JDK 21** installed and configured on your `PATH`.
- **MySQL 8.0** server running locally (or via Docker).
- **Git** version control system.

### Database Configuration
Ensure a MySQL database named `campusconnect` exists:
```sql
CREATE DATABASE campusconnect CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Update your database credentials in `src/main/resources/application.properties` or set them via environment variables:
```properties
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:mysql://localhost:3306/campusconnect}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:root}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:}
server.port=${SERVER_PORT:8081}
```

### Running the Application

Using the bundled Maven wrapper:

* **Windows (PowerShell / CMD)**:
  ```powershell
  .\mvnw.cmd spring-boot:run
  ```
* **Linux / macOS**:
  ```bash
  chmod +x mvnw
  ./mvnw spring-boot:run
  ```

Access the application in your browser at: **`http://localhost:8081`**

### Default Credentials

The system automatically initializes default accounts on first run:

| Role | Username / Email | Password | Access Area |
| :--- | :--- | :--- | :--- |
| **Administrator** | `admin@campusconnect.edu` | `Admin@12345` | `/admin/**` (Full Admin Management) |
| **Student (Demo)** | `student@college.edu` | `Student@123` | `/explore`, `/registrations/**` |

---

## Containerization with Docker

### Multi-Stage Dockerfile
The project uses a two-stage Docker build to keep images minimal and secure:
1. **Build Stage (`eclipse-temurin:21-jdk-alpine`)**: Resolves dependencies and compiles the application JAR.
2. **Runtime Stage (`eclipse-temurin:21-jre-alpine`)**: Runs the standalone JAR under a dedicated non-root user (`appuser`).

### One-Click Stack with Docker Compose
To launch both the **CampusConnect Web App** and a dedicated **MySQL 8.0** database container:

```powershell
# Build image and start all containers in background
docker compose up --build -d

# Check running container status
docker compose ps

# View live container logs
docker compose logs -f app

# Tear down the stack
docker compose down
```

### Standalone Docker Container
To manually build and run the container:

```powershell
# 1. Build the Docker image
docker build -t campusconnect:latest .

# 2. Run container mapped to port 8081
docker run -d -p 8081:8081 --name campusconnect-app `
  -e SPRING_DATASOURCE_URL="jdbc:mysql://host.docker.internal:3306/campusconnect" `
  -e SPRING_DATASOURCE_USERNAME="root" `
  -e SPRING_DATASOURCE_PASSWORD="" `
  campusconnect:latest
```

---

## Jenkins CI/CD Pipeline

The project includes a ready-to-use [`Jenkinsfile`](./Jenkinsfile) that defines a multi-stage declarative pipeline:

| Stage | Command / Action | Description |
| :--- | :--- | :--- |
| **1. Checkout SCM** | `checkout scm` | Pulls latest commit from the Git repository. |
| **2. Compile** | `.\mvnw.cmd clean compile` | Validates syntax and compiles Java source classes. |
| **3. Automated Tests** | `.\mvnw.cmd test` | Runs the test suite; records XML reports in JUnit plugin. |
| **4. Package JAR** | `.\mvnw.cmd package -DskipTests` | Packages executable JAR and archives it as a build artifact. |
| **5. Docker Build** | `docker build -t campusconnect:latest .` | Assembles the production Docker container image. |

### Running the Pipeline in Jenkins Website:
1. Navigate to `http://localhost:8080`.
2. Click **New Item** ➔ Choose **Pipeline** ➔ Name it `CampusConnect-CI-CD`.
3. Under the **Pipeline** configuration, choose **Pipeline script from SCM** (point to your Git repo) or paste the script directly.
4. Click **Build Now** to execute the pipeline and view the visual stage progression.

---

## Testing & Quality Assurance

The application includes an automated test suite verifying business logic, authorization rules, and HTTP endpoints:

```powershell
.\mvnw.cmd test
```

### Test Coverage Highlights:
- **`EventServiceTest`**: Validates event creation, capacity tracking, seat decrementation, status transitions, and search filters.
- **`RegistrationServiceTest`**: Tests registration flow, duplicate prevention, capacity enforcement, and pass cancellation.
- **`EmailServiceTest`**: Validates simulated and SMTP notification dispatch logic.
- **`SecurityAccessTest`**: Verifies role permissions, anonymous restrictions, student boundaries, and admin-only endpoint protection.
- **`WebControllersTest`**: Integration tests across web views and form submissions.

**Total Tests**: `59 Tests Run, 0 Failures, 0 Errors, 0 Skipped`.

---

## License
This project is developed for educational and academic evaluation purposes.
