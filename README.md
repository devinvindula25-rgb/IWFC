# Intelligent Wellness and Fitness Center (IWFC)

Welcome to the Intelligent Wellness and Fitness Center management system. This application is designed to handle all core operations for a premium fitness center, offering a seamless experience for Administrators, Fitness Instructors, and Gym Members.

## Overview
IWFC is built with a strong emphasis on Object-Oriented Programming (OOP) principles, clean architecture, and a modern, premium user interface built with JavaFX.

## Features
- **Role-Based Access Control**: Secure login and distinct views for Admins, Instructors, and Members.
- **Dynamic Dashboard**: Interactive widgets for quick overview of gym metrics.
- **Advanced User Management**: Rich profiles, attendance tracking, and performance analytics with interactive charts.
- **Maintenance & Equipment Logs**: Track machinery health, log issues, and assign maintenance staff with urgency indicators.
- **Session Scheduling & Bookings**: A visually appealing calendar to browse, book, and manage fitness classes.
- **Fitness Plans & Enrolments**: Flexible fitness plan management for members and instructors.
- **Notifications Engine**: In-app notifications for system updates, bookings, and alerts.

## Technical Architecture
- **Language**: Java 21
- **UI Framework**: JavaFX
- **Build Tool**: Maven
- **Design Patterns**: 
  - **Facade**: `IWFCSystemFacade` acts as the single point of entry to the backend.
  - **Singleton**: Data stores and shared services.
  - **Strategy**: Pluggable payment processors.
  - **Observer**: Real-time notification updates.
  - **Factory & Builder**: Fitness plan instantiation.

## Getting Started

### Prerequisites
- JDK 21+
- Maven 3.8+

### Build & Run
To compile the project and run the tests:
```bash
mvn clean test
```

To run the application:
```bash
mvn javafx:run
```

## Documentation
- Additional documentation is located in the `docs` folder or provided in standard project deliverables (e.g., UML Diagrams, Reports).
