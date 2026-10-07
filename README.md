# 🏋️ Intelligent Wellness and Fitness Center (IWFC)
### *Next-Generation Enterprise Gym Operations & Health Telemetry Suite*

[![Java 21](https://img.shields.io/badge/Java-21%20LTS-007396?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![JavaFX 21](https://img.shields.io/badge/JavaFX-21.0.2-FF6600?logo=java&logoColor=white)](https://openjfx.io/)
[![AtlantaFX](https://img.shields.io/badge/AtlantaFX-2.0.1-6366F1?logoColor=white)](https://github.com/mkpaz/atlantafx)
[![Apache PDFBox](https://img.shields.io/badge/PDFBox-2.0.30-D22128?logo=apache&logoColor=white)](https://pdfbox.apache.org/)
[![JUnit 5](https://img.shields.io/badge/JUnit-5.10.2-25A162?logo=junit5&logoColor=white)](https://junit.org/junit5/)
[![Maven 3.8+](https://img.shields.io/badge/Maven-Build%20Passing-C71A36?logo=apachemaven&logoColor=white)](https://maven.apache.org/)

---

## 📌 Executive Overview

The **Intelligent Wellness and Fitness Center (IWFC)** is an enterprise-grade desktop management system developed for modern fitness clubs, wellness facilities, and athletic complexes. Built on **Java 21 LTS** and **JavaFX 21**, IWFC combines robust Object-Oriented Design (OOP) principles, rigorous Gang of Four (GoF) design patterns, and an ultra-premium **glassmorphic design system** powered by AtlantaFX Cupertino themes.

IWFC provides comprehensive role-based workflows for **Administrators**, **Fitness Instructors**, and **Gym Members**, integrating real-time telemetry, point-of-sale processing with automated PDF receipt generation, interactive calendar scheduling, asset health diagnostics, and longitudinal body measurement analytics.

---

## 🚀 Key Functional Modules & UI Views

### 1. 🔐 Dual-Pane Authentication Hub (`LoginView`)
- **Centered Glassmorphic Architecture**: Responsive layout anchored to screen center with zero clipping across all screen resolutions (800×600 up to 4K).
- **Enterprise Showcase Column**: Left-side brand panel highlighting core telemetry metrics, system uptime, and version telemetry.
- **One-Click Quick Login Chips**:
  - `👑 Admin` — Preset ID `ADM001` (System Administrator)
  - `🏋️ Trainer` — Preset ID `INS001` (Lead Fitness Instructor)
  - `🏃 Member` — Preset ID `MEM001` (Enrolled Gym Member)
- **Floating Live Theme Switcher**: Instant circular button (`🌙 / ☀️`) toggling between dark and light modes.

### 2. 📊 Executive Operations Command Center (`DashboardView`)
- **Executive Metric Cards**:
  - *Gross Cashflow & Revenue*: Financial intake, average order value, and completed transactions.
  - *Active Bookings & Attendance*: Live check-in attendance volume and capacity utilization.
  - *Fleet Operational Rate*: Real-time percentage of operable gym equipment.
  - *Member Registration Base*: Total enrolled active gym members.
- **Interactive Multi-Timeframe Telemetry Chart**:
  - Toggle between **Revenue ($)** and **Attendance (Count)** views.
  - Multi-window time filtering: `Today` (4-hour buckets), `7 Days`, `30 Days`, `90 Days`, and `All Time`.
- **Live Equipment Duty Matrix**: Visual progress meters tracking operational runtime against recommended maintenance cycles.
- **Operational Bookings & Attendance Ledger**:
  - Searchable booking stream with instant status filter (`ALL`, `CONFIRMED`, `ATTENDED`, `CANCELLED`).
  - **One-Click Check-In Action**: Instantly marks member attendance with automated toast notification.
  - **Admin Booking Cancellation**: Verified modal confirmation with reason tracking.
- **High-Priority Incident Triage Feed**:
  - Real-time maintenance queue highlighting `HIGH`, `MEDIUM`, and `LOW` urgency faults.
  - Quick-resolve action modal to assign technician or close work order.
- **Quick Operations Modals**:
  - *New Booking Modal*: Staff booking on behalf of any active member.
  - *Walk-In POS Settlement*: Instant counter payment collection with custom purpose and automated receipt generation.

### 3. 🏋️ Equipment Operations Center & Blueprint Hub (`EquipmentView`)
- **Interactive Gym Floor Map**: Visual floor blueprint divided into zones (*Cardio Zone*, *Free Weights*, *Resistance Machines*, *Studio*, *Recovery Zone*) with live color-coded telemetry status dots.
- **2-Tier Category Navigation**: Category Donut Overview and category drill-down navigation.
- **Fleet Inventory**: Real-time hours logged, maintenance thresholds, location tagging, and status filters (`OPERATIONAL`, `UNDER_MAINTENANCE`, `OUT_OF_ORDER`).
- **Asset Lifecycle Management**: Log faults, update status, and commission new machinery.

### 4. 📅 Smart Scheduling & Timetable Hub (`SessionView`)
- **Interactive Calendar Engine**: Weekly and monthly timetable views with session capacity meters.
- **Class Booking Stream**: Instant member sign-up, waitlist logic, and conflict validation via `SchedulingValidator`.
- **Session Administration**: Add, reschedule, and cancel group classes or individual coaching slots.

### 5. 💳 Financial Command & POS Gateway (`PaymentView`)
- **Multi-Method Payment Context**: Powered by the Strategy Pattern:
  - 💳 **Credit/Debit Card** (`CardPayment`) with Luhn and expiry validation.
  - 📱 **Digital Wallet** (`DigitalWalletPayment`) supporting Apple Pay, Google Pay, and PayPal.
  - 🏦 **Bank Transfer** (`BankTransferPayment`) with reference verification.
  - 💵 **Cash Payment** (`CashPayment`) for on-counter front-desk settlements.
- **Automated PDF Receipt & Invoicing Engine**:
  - Integrated with **Apache PDFBox 2.0.30** via `BillGenerator`.
  - Generates branded, styled A4 PDF receipts (`Receipt_PAY-XXXXXXXX.pdf`) saved to `bills/` directory.
  - Automatic prompt to open PDF directly in default OS viewer.
- **Revenue Analytics & Audit Ledger**: Search, filter by status (`COMPLETED`, `PENDING`, `FAILED`, `REFUNDED`), and process verified refunds.

### 6. 🙋‍♂️ Members Directory & Report Cards (`MembersManagementView`)
- **Member Directory**: Avatar cards, active workout plan badges, assigned personal trainers, and accumulated attendance hours.
- **Longitudinal Member Report Card**:
  - Dedicated drill-down view showing workout plan progression, attendance streaks, payment histories, and body composition changes.
  - Direct plan reassignment and trainer change requests.

### 7. 👥 Team & Staff Command (`UserManagementView`)
- **Personnel Directory**: Unified view of Administrators, Trainers, and Operations Staff.
- **Trainer Portfolios**: Assigned trainees count, specialty credentials, contact directory, and role-based permissions.
- **Staff Onboarding**: Modal to provision new instructors and administrators with immediate credentials.

### 8. 🔧 Maintenance Work Orders (`MaintenanceView`)
- **Ticketing Lifecycle**: `PENDING` ➔ `ASSIGNED` ➔ `COMPLETED`.
- **Urgency Matrix**: High, Medium, and Low urgency badges with technician assignment tracking.
- **Downtime Auditing**: Equipment auto-flagged as `UNDER_MAINTENANCE` upon ticket dispatch.

### 9. 📋 CRM Follow-Up Pipeline (`FollowUpView`)
- **Sales & Retention Kanban**: Tracks member interactions across categories: `TRIAL`, `ANNIVERSARY`, `ENQUIRY`, `FEEDBACK`, `PARTY`, `MEMBERSHIP_RENEWAL`, `BIRTHDAY`, `BALANCE_DUE`.
- **Convertibility Scoring**: Visual prioritization (`HIGH`, `MEDIUM`, `LOW`) with action logs.

### 10. 🔔 Real-Time Notification Center (`NotificationView`)
- **Observer-Driven Alert Center**: Categorized notifications for bookings, maintenance escalations, payments, and system announcements.
- **Floating Toast System (`ToastNotification`)**: Non-blocking animated slide-in toasts for instant UI feedback.
- **Notification Triage**: Mark individual/all as read, mark unread, and clear history.

### 11. 💪 Workout Plans & Custom Regimens (`WorkoutPlansView` & `WorkoutPlanDetailView`)
- **Tiered Fitness Plans**:
  - *Personal Training Plan* (1-on-1 dedicated coaching, biometric tracking).
  - *Group Class Plan* (High-energy community classes).
  - *Self-Guided Plan* (Independent gym floor access).
- **Detailed Plan Breakdown**: Pricing, duration, included workout routines, and instructor assignment.

### 12. 📈 Biometrics & Measurement Analytics (`MeasurementAnalyticsView` & `AdminReportsView`)
- **Body Composition Tracker**: Log weight, body fat %, chest, waist, hips, arms, thighs, and calves over time.
- **Analytics & Trend Engine**: Calculates BMI changes, muscle-to-fat trajectory, and visual comparison delta chips.
- **Admin Business Intelligence**: Fleet utilization rates, instructor workload distribution, and monthly financial summaries.

---

## 🏛️ Technical Architecture & Design Patterns

The codebase strictly adheres to clean OOP principles and utilizes canonical Gang of Four (GoF) design patterns:

```
                          ┌───────────────────────┐   ┌───────────────────────────┐
                          │   JavaFX UI Layer     │   │   Headless Terminal CLI   │
                          │ (Views & Components)  │   │     (ConsoleApp.java)     │
                          └──────────┬────────────┘   └─────────────┬─────────────┘
                                     │                              │
                                     └──────────────┬───────────────┘
                                                    ▼
                                      ┌───────────────────────────┐
                                      │     IWFCSystemFacade      │  ◄── [Facade Pattern]
                                      └─────────────┬─────────────┘
         ┌──────────────────┬───────────────────────┴────────┬──────────────────┐
         ▼                  ▼                                ▼                  ▼
┌─────────────────┐ ┌───────────────┐               ┌───────────────┐ ┌─────────────────┐
│EquipmentService │ │SessionService │               │UserService    │ │PaymentService   │
└────────┬────────┘ └───────┬───────┘               └───────┬───────┘ └────────┬────────┘
         │                  │                 │                  │
         │                  │                 │         ┌────────┴────────┐
         │                  │                 │         │ PaymentStrategy │  ◄── [Strategy]
         │                  │                 │         └────────┬────────┘
         │                  │                 │                  │
         ▼                  ▼                 ▼                  ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                     com.iwfc.pattern.DataStore                          │  ◄── [Singleton]
│               In-Memory Repository<T> Collections                       │  ◄── [Repository]
└────────────────────────────────────┬────────────────────────────────────┘
                                     │
                                     ▼
                    ┌─────────────────────────────────┐
                    │ NotificationService & Observers │  ◄── [Observer Pattern]
                    └─────────────────────────────────┘
```

| Design Pattern | Primary Classes | Description |
| :--- | :--- | :--- |
| **Facade** | [`IWFCSystemFacade`](file:///d:/Projects/IWFC/src/main/java/com/iwfc/facade/IWFCSystemFacade.java) | Unified API entry point for all UI components, encapsulating multi-service orchestration (Session, Maintenance, Equipment, Users, Payments). |
| **Singleton** | [`DataStore`](file:///d:/Projects/IWFC/src/main/java/com/iwfc/pattern/DataStore.java) | Thread-safe in-memory central data repository populated with comprehensive demonstration records upon startup. |
| **Strategy** | [`PaymentStrategy`](file:///d:/Projects/IWFC/src/main/java/com/iwfc/pattern/payment/PaymentStrategy.java), [`PaymentContext`](file:///d:/Projects/IWFC/src/main/java/com/iwfc/pattern/payment/PaymentContext.java) | Encapsulates payment algorithms (`CardPayment`, `DigitalWalletPayment`, `BankTransferPayment`, `CashPayment`), enabling dynamic runtime method selection. |
| **Observer** | [`NotificationService`](file:///d:/Projects/IWFC/src/main/java/com/iwfc/pattern/NotificationService.java), [`ToastNotification`](file:///d:/Projects/IWFC/src/main/java/com/iwfc/ui/components/ToastNotification.java) | Event broadcast system dispatching real-time domain events (`BOOKING_CONFIRMED`, `MAINTENANCE_ALERT`, `EQUIPMENT_THRESHOLD`) to active listeners. |
| **Factory** | [`PlanCatalogueFactory`](file:///d:/Projects/IWFC/src/main/java/com/iwfc/pattern/plan/PlanCatalogueFactory.java), [`SessionFactory`](file:///d:/Projects/IWFC/src/main/java/com/iwfc/pattern/SessionFactory.java) | Centralized, decoupled instantiation of fitness plans and recurring fitness session templates. |
| **Builder** | [`FitnessPlanBuilder`](file:///d:/Projects/IWFC/src/main/java/com/iwfc/pattern/plan/FitnessPlanBuilder.java) | Fluent builder interface for constructing complex `PersonalTrainingPlan`, `GroupClassPlan`, and `SelfGuidedPlan` models. |
| **Repository** | [`Repository<T>`](file:///d:/Projects/IWFC/src/main/java/com/iwfc/repository/Repository.java) | Generic in-memory CRUD repository providing `findById`, `findAll`, `save`, `update`, and `delete` semantics. |

---

## 📂 Project Directory Structure

```text
IWFC/
├── pom.xml                                      # Maven POM (Java 21, JavaFX 21, AtlantaFX, PDFBox, JUnit 5)
├── README.md                                    # Comprehensive Project Documentation
├── bills/                                       # Generated PDF Invoices & Payment Receipts
└── src/
    ├── main/
    │   ├── java/
    │   │   ├── module-info.java                 # JPMS Java Module Definition (com.iwfc)
    │   │   └── com/iwfc/
    │   │       ├── IWFCApp.java                 # JavaFX Enterprise GUI Entry Point & Main Stage
    │   │       ├── ConsoleApp.java              # Interactive Headless CLI Console (Terminal Mode)
    │   │       ├── exception/                   # Domain Exception Hierarchy
    │   │       │   ├── DuplicateDataException.java
    │   │       │   ├── InsufficientFundsException.java
    │   │       │   ├── InvalidBookingException.java
    │   │       │   ├── IWFCException.java
    │   │       │   ├── PaymentFailedException.java
    │   │       │   └── UnauthorizedAccessException.java
    │   │       ├── facade/
    │   │       │   └── IWFCSystemFacade.java    # Central Facade Controller
    │   │       ├── model/                       # Domain Entities, Enums & Value Objects (35 files)
    │   │       │   ├── Booking.java, BookingStatus.java
    │   │       │   ├── Equipment.java, EquipmentStatus.java, Location.java
    │   │       │   ├── FitnessPlan.java, FitnessSession.java
    │   │       │   ├── Member.java, Instructor.java, Admin.java, User.java, Role.java
    │   │       │   ├── Payment.java, PaymentMethod.java, PaymentStatus.java
    │   │       │   ├── MaintenanceRequest.java, Urgency.java, RequestStatus.java
    │   │       │   └── BodyMeasurementLog.java, FollowUp.java, Notification.java
    │   │       ├── pattern/                     # GoF Design Pattern Implementations
    │   │       │   ├── DataStore.java           # Central In-Memory Singleton Store
    │   │       │   ├── NotificationService.java # Event Distributor (Observer)
    │   │       │   ├── payment/                 # Strategy Pattern Payment Implementations
    │   │       │   │   ├── PaymentStrategy.java, PaymentContext.java, PaymentResult.java
    │   │       │   │   ├── CardPayment.java, DigitalWalletPayment.java
    │   │       │   │   ├── BankTransferPayment.java, CashPayment.java
    │   │       │   └── plan/                    # Factory & Builder Plan Assembly
    │   │       │       ├── FitnessPlanBuilder.java
    │   │       │       └── PlanCatalogueFactory.java
    │   │       ├── repository/
    │   │       │   └── Repository.java          # Generic In-Memory Repository
    │   │       ├── service/                     # Business Logic Services (10 files)
    │   │       │   ├── EquipmentService.java, SessionService.java, MaintenanceService.java
    │   │       │   ├── PaymentService.java, UserService.java, EnrollmentService.java
    │   │       │   ├── MeasurementAnalyzer.java, ScheduleMaker.java, SchedulingValidator.java
    │   │       │   └── MockPaymentGateway.java
    │   │       ├── ui/                          # JavaFX View Controllers (18 files)
    │   │       │   ├── LoginView.java, MainLayout.java, ThemeManager.java
    │   │       │   ├── DashboardView.java, EquipmentView.java, SessionView.java
    │   │       │   ├── PaymentView.java, MembersManagementView.java, UserManagementView.java
    │   │       │   ├── MaintenanceView.java, FollowUpView.java, NotificationView.java
    │   │       │   ├── WorkoutPlansView.java, WorkoutPlanDetailView.java
    │   │       │   └── AdminReportsView.java, MeasurementAnalyticsView.java
    │   │       │   └── components/              # Reusable Glassmorphism UI Controls
    │   │       │       ├── DialogUtils.java, StatCard.java, StatusBadge.java
    │   │       │       ├── ToastNotification.java, UsageRing.java
    │   │       └── util/
    │   │           └── BillGenerator.java       # Apache PDFBox Invoice & Receipt Generator
    │   └── resources/
    │       ├── com/iwfc/
    │       │   ├── css/                         # Curated Glassmorphism Style Engine (12 sheets)
    │       │   │   ├── common.css               # Core Typography, Tokens & Components
    │       │   │   ├── dark-overrides.css       # AtlantaFX Cupertino Dark Color Palette
    │       │   │   ├── light-overrides.css      # AtlantaFX Cupertino Light Color Palette
    │       │   │   ├── sidebar-navigation.css   # Side Menu, Filter Chips, Search, Accordions & Drawer
    │       │   │   ├── dashboard-management.css # Dashboard Grid, KPI, Ledger & Chart Styling
    │       │   │   ├── equipment-management.css # Blueprint Map, Donut Hub & Fleet Styling
    │       │   │   ├── payments-management.css  # Financial KPIs, POS Modals & Invoice Cards
    │       │   │   ├── members-management.css   # Member Cards, Report Card Styling
    │       │   │   ├── team-management.css      # Staff Rostering, Specialty Tags
    │       │   │   ├── notification-center.css  # Notification Streams & Filter Badges
    │       │   │   └── followup-pipeline.css    # CRM Lead & Retention Pipelines
    │       │   └── images/                      # Anatomy Diagrams, Backgrounds, Avatars
    └── test/
        └── java/com/iwfc/                       # JUnit 5 Automated Test Suite (27 Tests)
            ├── exception/
            │   └── ExceptionVerificationTest.java
            ├── pattern/
            │   ├── RepositoryTest.java
            │   └── payment/PaymentStrategyTest.java
            └── service/
                ├── MaintenanceWorkflowTest.java
                └── SchedulingValidatorTest.java
```

---

## 👥 Role-Based Access Control (RBAC) & Default Credentials

IWFC seeds realistic demonstration records on startup. Use any of the pre-configured accounts:

| User ID | Full Name | Role | Access Scope & Permissions |
| :--- | :--- | :--- | :--- |
| **`ADM001`** | **Admin User** | `ADMIN` | **Full Enterprise Access**: All operations, system KPIs, financial ledger, staff management, maintenance dispatch, and user administration. |
| **`INS001`** | **Alex Morgan** | `INSTRUCTOR` | **Trainer Access**: Class schedules, assigned client measurements, maintenance reporting, session creation, and workout planning. |
| **`MEM001`** | **John Doe** | `MEMBER` | **Member Portal**: Class booking, personal biometrics logs, invoice history, trainer change requests, and profile details. |
| **`MEM002`** | **Jane Smith** | `MEMBER` | Enrolled gym member with active training plan and booking history. |
| **`MEM003`** | **Robert Johnson** | `MEMBER` | Enrolled gym member with biometric measurement history. |
| **`INS002`** | **Sarah Connor** | `INSTRUCTOR` | Strength & Conditioning specialist with assigned trainees. |

---

## 🛠️ Getting Started & Installation

### Prerequisites
- **Java Development Kit (JDK)**: Version **21** or higher ([Eclipse Temurin](https://adoptium.net/) or [Oracle JDK](https://www.oracle.com/java/technologies/downloads/))
- **Apache Maven**: Version **3.8.0** or higher ([Maven Official Site](https://maven.apache.org/download.cgi))
- **Git** (optional, for version control)

Verify your environment:
```bash
java -version
mvn -version
```

### 1. Compile the Project
To compile all 94 modular Java source files and process stylesheet resources:
```bash
mvn clean compile
```

### 2. Run the Automated Unit Tests
To execute the complete **JUnit 5** test suite (27 automated test cases verifying all domain exceptions, repository persistence, payment strategies, and session validation):
```bash
mvn test
```
*Expected output: `BUILD SUCCESS` with 27 tests run, 0 failures, 0 errors.*

### 3. Launch the Application

#### Option A: JavaFX Enterprise Graphical Dashboard (Default GUI)
To launch the JavaFX desktop interface:
```bash
mvn javafx:run
```

#### Option B: Headless Terminal CLI (ConsoleApp)
For evaluation in terminal, headless, or SSH environments without JavaFX display libraries:
```bash
mvn exec:java
```
*Note: Both `IWFCApp` and `ConsoleApp` seamlessly share the exact same underlying architecture, models, and business logic via `IWFCSystemFacade`.*

---

## 🎨 Design System & Visual Tech

- **Design Philosophy**: High-contrast, sleek glassmorphism with subtle borders, vibrant gradients, and dynamic micro-animations.
- **Base Framework**: Integrated with **AtlantaFX 2.0.1** using `CupertinoDark` and `CupertinoLight`.
- **Dynamic Theming**: The `ThemeManager` synchronizes user preferences in real time across all open modals, charts, tables, and dialogs.
- **Custom CSS Engine**: Modular stylesheet architecture with dedicated CSS per domain (`dashboard-management.css`, `equipment-management.css`, `payments-management.css`, etc.) ensuring strict design token consistency without style pollution.
- **Dialog Aesthetics**: Windowless transparent glass alerts (`DialogUtils.stylePremiumDialog`) eliminating harsh operating-system window borders for a native macOS/iOS-like aesthetic.

---

## 📄 License & Attribution

Developed for the **Intelligent Wellness and Fitness Center (IWFC)**. All rights reserved.  
Built with modern Java 21, JavaFX, and clean software architecture.
