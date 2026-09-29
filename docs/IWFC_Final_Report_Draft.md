# Intelligent Wellness and Fitness Center (IWFC)
## Final Project Report

### 1. Introduction
The Intelligent Wellness and Fitness Center (IWFC) is a comprehensive, modern software solution built to manage the complex operations of a premium gym facility. Designed with Java 21 and JavaFX, the application serves three primary user roles: Administrators, Instructors, and Members. 

### 2. Objectives
- **Role-Based Workflows:** Distinct UI and capabilities for different user types.
- **Robust Architecture:** Implementing clean object-oriented design using GOF design patterns.
- **Advanced UI/UX:** Utilizing JavaFX for a modern, glass-morphism inspired, highly interactive user experience.
- **Integrated Sub-systems:** Handling equipment maintenance, fitness class scheduling, payment processing, and advanced instructor/trainee management.

### 3. System Architecture & Design Patterns
The system employs multiple architectural patterns to ensure scalability and maintainability:
- **Facade Pattern (`IWFCSystemFacade`):** Acts as the single entry point for the UI to interact with the backend business logic.
- **Observer Pattern (`NotificationEngine`):** Allows loose coupling between system events (like a machine breaking down) and users receiving alerts.
- **Strategy Pattern (`PaymentStrategy`):** Enables swapping out payment processors (Card, Bank Transfer, Digital Wallet, Cash) dynamically at runtime.
- **Factory & Builder Patterns (`FitnessPlanBuilder`, `PlanCatalogueFactory`):** Handles the complex instantiation of diverse fitness plans (Group, Personal Training, Self-Guided).
- **Singleton Pattern (`Repository` & `ThemeManager`):** Ensures single, globally accessible instances for in-memory data storage and UI themes.

### 4. Key Features Developed
- **Dashboard & Navigation:** A responsive sidebar with active states and a global theme toggle (Light/Dark mode).
- **Equipment & Maintenance Module:** Tracks machine usage hours, auto-triggers maintenance requests at thresholds, and manages repair workflows.
- **Session Booking:** A dynamic calendar view preventing time-clashes and managing capacity limits.
- **Advanced Instructor Management:** Instructors can view "Impact Scores (IIS)", track their specific trainees via visually appealing avatar stacks, and review recent feedback.
- **Member Insights:** Members can see their financial history, track gym hours, and view their currently active fitness plan with instructor assignment.

### 5. Testing & Quality Assurance
The backend logic is covered by a suite of JUnit tests focusing on:
- Exception handling and validation (`ExceptionVerificationTest`).
- Complex workflows like usage triggering maintenance (`MaintenanceWorkflowTest`).
- Custom time-clash validation (`SchedulingValidatorTest`).

### 6. Conclusion
The IWFC project successfully bridges complex backend scheduling, OOP design patterns, and premium UI aesthetics into a cohesive JavaFX application ready for modern gym environments.
