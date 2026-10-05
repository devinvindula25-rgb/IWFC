# 🗂️ IWFC Project File Categories & Complete Inventory

> **Intelligent Wellness and Fitness Center (IWFC)**  
> **Total Source Files:** 94 Java Files • 13 CSS Files • 5 Test Suites (26 Unit Tests) • 4 Image Assets  
> **Runtime Environment:** Java 21 (LTS) • JavaFX 21.0.2 • AtlantaFX 2.0.1 • Apache PDFBox 2.0.30 • Maven  

---

## 📊 Summary by Category

| # | Category | Package / Directory | File Count | Primary Responsibility |
|---|---|---|:---:|---|
| **1** | **Application Core & Module** | `com.iwfc`, root `src/main/java` | **2** | Bootstrap, JavaFX lifecycle, JPMS module descriptor |
| **2** | **Facade Architecture** | `com.iwfc.facade` | **1** | Subsystem orchestration, single entry point for UI |
| **3** | **Domain Models & Entities** | `com.iwfc.model` | **35** | Core business entities, enums, value objects, and states |
| **4** | **Business Services Layer** | `com.iwfc.service` | **10** | Business rules, scheduling validation, analytics, payment gateway |
| **5** | **Design Pattern Implementations** | `com.iwfc.pattern` | **14** | GoF patterns: Strategy, Observer, Singleton, Factory, Builder |
| **6** | **Repository Data Access** | `com.iwfc.repository` | **1** | Generic in-memory CRUD data access contract |
| **7** | **Domain Exceptions** | `com.iwfc.exception` | **6** | Custom domain exception hierarchy with user-facing messages |
| **8** | **Utilities & Document Generation** | `com.iwfc.util` | **1** | Apache PDFBox automated A4 billing & receipt generator |
| **9** | **User Interface (Views & Controllers)** | `com.iwfc.ui` | **18** | JavaFX glassmorphic screens, dashboards, and modal controllers |
| **10**| **Reusable UI Components** | `com.iwfc.ui.components` | **5** | Custom widgets: Toast notifications, badges, rings, dialog styler |
| **11**| **Design System & CSS Stylesheets** | `src/main/resources/com/iwfc/css` | **13** | Design tokens, Cupertino dark/light overrides, module stylesheets |
| **12**| **Static Media Assets** | `src/main/resources/com/iwfc/images` | **4+** | Anatomy charts, backgrounds, avatars, and exercise illustrations |
| **13**| **Automated Unit Test Suite** | `src/test/java/com/iwfc` | **5** | JUnit 5 automated regression and verification suite (26 tests) |
| **14**| **Build, Config & Output** | Project Root, `bills/` | **3** | `pom.xml`, `README.md`, generated PDF receipt storage |

---

## 1. 🚀 Application Core & Module Definition

| File Path | Type | Description |
|---|---|---|
| `src/main/java/module-info.java` | Java Module | JPMS module declaration (`com.iwfc`), exports packages, requires JavaFX, AtlantaFX, and PDFBox. |
| `src/main/java/com/iwfc/IWFCApp.java` | Application | JavaFX entry point; sets initial window parameters, scene size (1200×750), and transitions between Login and MainLayout. |

---

## 2. 🏛️ Facade Architecture

| File Path | Pattern | Description |
|---|---|---|
| `src/main/java/com/iwfc/facade/IWFCSystemFacade.java` | Facade | Unified API layer mediating between all UI views and backend services (Session, Maintenance, Equipment, Users, Payments). |

---

## 3. 📦 Domain Models & Entities (`com.iwfc.model`)

### 3.1. Users, Staff & Roles
| File Path | Type | Description |
|---|---|---|
| `User.java` | Abstract Class | Base class for system actors (`userId`, `name`, `email`, `role`, `phone`). |
| `Member.java` | Class | Enrolled gym member with assigned trainer, plan, hours spent, and loyalty metrics. |
| `Instructor.java` | Class | Fitness trainer with specialties, assigned client list, and certification details. |
| `Admin.java` | Class | System administrator with full management and audit privileges. |
| `Role.java` | Enum | System roles: `ADMIN`, `INSTRUCTOR`, `MEMBER`. |

### 3.2. Sessions, Classes & Fitness Plans
| File Path | Type | Description |
|---|---|---|
| `FitnessSession.java` | Class | Scheduled class entity (name, instructor, day, time slot, capacity, attendees). |
| `FitnessPlan.java` | Abstract Class | Base fitness regimen plan (`id`, `name`, `price`, `durationWeeks`, `description`). |
| `GroupClassPlan.java` | Class | Tiered group class membership regimen. |
| `PersonalTrainingPlan.java` | Class | 1-on-1 dedicated instructor regimen with personalized tracking. |
| `SelfGuidedPlan.java` | Class | Floor-access independent workout plan. |
| `Enrollment.java` | Class | Member subscription record linking a Member, Plan, and Assigned Instructor. |
| `TimeSlot.java` | Class | Scheduled session time boundaries (`startTime`, `endTime`). |
| `InstructorChange.java` | Class | Member request to switch assigned personal trainer. |
| `ChangeRequestStatus.java` | Enum | Instructor change states: `PENDING`, `APPROVED`, `REJECTED`. |

### 3.3. Bookings & Attendance
| File Path | Type | Description |
|---|---|---|
| `Booking.java` | Class | Member reservation record for a specific session and date. |
| `BookingStatus.java` | Enum | Booking states: `CONFIRMED`, `ATTENDED` (live check-in), `CANCELLED`. |

### 3.4. Equipment & Facility Asset Telemetry
| File Path | Type | Description |
|---|---|---|
| `Equipment.java` | Class | Physical machine entity (id, category, status, location, usage hours, threshold). |
| `EquipmentStatus.java` | Enum | Machinery conditions: `OPERATIONAL`, `UNDER_MAINTENANCE`, `OUT_OF_ORDER`. |
| `Location.java` | Enum | Gym zones: `CARDIO_ZONE`, `FREE_WEIGHTS`, `RESISTANCE_MACHINES`, `STUDIO`, `RECOVERY_ZONE`. |
| `UsageLogEntry.java` | Class | Telemetry log capturing equipment runtime duration and user. |

### 3.5. Maintenance & Fault Dispatch
| File Path | Type | Description |
|---|---|---|
| `MaintenanceRequest.java` | Class | Incident ticket linking faulty equipment, reporting trainer, description, and status. |
| `RequestStatus.java` | Enum | Work order lifecycle: `PENDING`, `ASSIGNED`, `COMPLETED`. |
| `Urgency.java` | Enum | Incident priority triage: `LOW`, `MEDIUM`, `HIGH`. |

### 3.6. Financials & POS
| File Path | Type | Description |
|---|---|---|
| `Payment.java` | Class | Financial transaction record (id, member, amount, tax, method, purpose, status, timestamp). |
| `PaymentMethod.java` | Enum | Payment types: `CARD`, `DIGITAL_WALLET`, `BANK_TRANSFER`, `CASH`. |
| `PaymentPurpose.java` | Enum | Payment category: `MEMBERSHIP_FEE`, `SESSION_BOOKING`, `PERSONAL_TRAINING`, `MERCHANDISE`. |
| `PaymentStatus.java` | Enum | Transaction states: `COMPLETED`, `PENDING`, `FAILED`, `REFUNDED`. |

### 3.7. Biometrics & CRM
| File Path | Type | Description |
|---|---|---|
| `BodyMeasurementLog.java` | Class | Biometric log (weight, body fat, chest, waist, hips, arms, thighs, calves, date). |
| `FollowUp.java` | Class | CRM member interaction note and retention record. |
| `FollowUpType.java` | Enum | Lead categories: `TRIAL`, `ANNIVERSARY`, `ENQUIRY`, `FEEDBACK`, `PARTY`, `MEMBERSHIP_RENEWAL`, `BIRTHDAY`, `BALANCE_DUE`. |
| `Convertibility.java` | Enum | Lead conversion rating: `HIGH`, `MEDIUM`, `LOW`. |

### 3.8. Notifications & Contracts
| File Path | Type | Description |
|---|---|---|
| `Notification.java` | Class | In-app notification alert with category, target role, title, read status, and timestamp. |
| `NotificationCategory.java` | Enum | Alert categories: `ALL`, `BOOKINGS`, `MAINTENANCE`, `PAYMENTS`, `ANNOUNCEMENTS`, `SYSTEM`. |
| `Identifiable.java` | Interface | Generic identification contract exposing `getId()`. |
| `AuditEntry.java` | Class | System audit log tracking operator, action, and timestamp. |

---

## 4. ⚙️ Business Services Layer (`com.iwfc.service`)

| File Path | Core Functionality |
|---|---|
| `EquipmentService.java` | Fleet telemetry management, runtime accumulation, threshold alert generation. |
| `SessionService.java` | Class scheduling, booking reservations, calendar mapping, capacity checking. |
| `MaintenanceService.java` | Work order generation, priority triage, technician assignment, status transitions. |
| `PaymentService.java` | Transaction orchestration, revenue calculation, strategy invocation, refund handling. |
| `UserService.java` | User directory lookup, authentication verification, role permission gating. |
| `EnrollmentService.java` | Fitness plan subscriptions, expiration dates, trainer reassignment requests. |
| `MeasurementAnalyzer.java` | Biometric analysis, BMI calculation, historical delta reporting. |
| `ScheduleMaker.java` | Timetable generation, recurring session pattern creation. |
| `SchedulingValidator.java` | Conflict resolution engine; prevents double-booking, overcapacity, and trainer clashes. |
| `MockPaymentGateway.java` | Simulated external payment gateway providing instant authorization or failure responses. |

---

## 5. 🧩 Design Patterns Layer (`com.iwfc.pattern`)

### 5.1. Singleton Pattern
| File Path | Description |
|---|---|
| `DataStore.java` | Central thread-safe in-memory database instance pre-seeded with realistic demonstration records. |

### 5.2. Observer Pattern
| File Path | Description |
|---|---|
| `NotificationService.java` | Event broker managing subscriber event listeners and dispatching real-time notifications. |
| `EventListener.java` | Generic observer callback interface (`onEvent(T event)`). |

### 5.3. Strategy Pattern (Payment Processing)
| File Path | Description |
|---|---|
| `payment/PaymentStrategy.java` | Algorithm strategy interface declaring `processPayment(PaymentContext context)`. |
| `payment/PaymentContext.java` | Encapsulates payment input data (card number, CVV, expiry, wallet ID, account, cash tendered). |
| `payment/PaymentResult.java` | Encapsulates payment outcome, approval code, and receipt metadata. |
| `payment/CardPayment.java` | Credit/Debit card strategy with Luhn and date validation. |
| `payment/DigitalWalletPayment.java` | Digital wallet strategy (Apple Pay, Google Pay, PayPal). |
| `payment/BankTransferPayment.java` | Electronic funds transfer strategy with account verification. |
| `payment/CashPayment.java` | Front-desk cash transaction strategy with change calculation. |

### 5.4. Factory & Builder Patterns (Fitness Plans & Sessions)
| File Path | Description |
|---|---|
| `plan/PlanCatalogueFactory.java` | Static factory instantiating the standard catalogue of training plans. |
| `plan/FitnessPlanBuilder.java` | Fluent builder assembling customized fitness regimens. |
| `SessionFactory.java` | Factory creating recurring and single-slot fitness session instances. |
| `RecurrenceRule.java` | Helper defining daily, weekly, or monthly session repetition. |

---

## 6. 🗄️ Repository Layer (`com.iwfc.repository`)

| File Path | Description |
|---|---|
| `Repository.java` | Generic in-memory CRUD repository implementation providing `save`, `findById`, `findAll`, `update`, and `delete`. |

---

## 7. ⚠️ Domain Exceptions (`com.iwfc.exception`)

| File Path | Base Class | Description |
|---|---|---|
| `IWFCException.java` | `Exception` | Base checked exception with error codes and user-friendly messages. |
| `UnauthorizedAccessException.java` | `IWFCException` | Thrown when a user attempts an action outside their granted role permissions. |
| `DuplicateDataException.java` | `IWFCException` | Thrown when attempting to insert an entity with an existing identifier. |
| `InvalidBookingException.java` | `IWFCException` | Thrown when booking exceeds capacity, overlaps an existing schedule, or has invalid dates. |
| `PaymentFailedException.java` | `IWFCException` | Thrown when a transaction is declined or exceeds credit parameters. |
| `InsufficientFundsException.java` | `IWFCException` | Thrown when balance or account amount is inadequate. |

---

## 8. 📄 Utilities & Document Generation (`com.iwfc.util`)

| File Path | Technology | Description |
|---|---|---|
| `BillGenerator.java` | Apache PDFBox 2.0.30 | Generates branded, styled A4 PDF invoices and payment receipts saved into the `bills/` folder. |

---

## 9. 🖥️ User Interface Views & Controllers (`com.iwfc.ui`)

| File Path | Layout Pattern | Primary Responsibility |
|---|---|---|
| `LoginView.java` | Centered Dual-Pane Card | Authentication portal with quick-login profile chips (`Admin`, `Trainer`, `Member`) and theme switcher. |
| `MainLayout.java` | BorderPane & Sidebar | Application shell with collapsible categorized sidebar navigation and swappable center content area. |
| `ThemeManager.java` | Singleton Manager | Real-time theme orchestrator applying AtlantaFX Cupertino Dark and Light CSS overlays. |
| `DashboardView.java` | Responsive Grid | Executive KPI cards, interactive multi-timeframe analytics charts, equipment duty meters, live check-in ledger, and maintenance triage. |
| `EquipmentView.java` | 2-Tier Category & Map | Equipment inventory, Donut Hub, and interactive Gym Floor Blueprint with live status telemetry. |
| `SessionView.java` | Calendar & Grid | Interactive class schedule timetable, capacity meters, and booking workflows. |
| `PaymentView.java` | Tabbed Financial Hub | Financial metrics, point-of-sale counter collection, automated PDF billing, and refund management. |
| `MembersManagementView.java`| Card Grid & Report Card | Member directory, membership tiers, profile inspector, and longitudinal report cards. |
| `UserManagementView.java` | Table & Detail Panel | Staff, instructor, and administrator management with permission assignment and specialty tagging. |
| `MaintenanceView.java` | Kanban & Ticket List | Work order tracking (`PENDING` ➔ `ASSIGNED` ➔ `COMPLETED`) with technician dispatch. |
| `FollowUpView.java` | Pipeline View | CRM member retention and sales follow-up pipeline with convertibility prioritization. |
| `NotificationView.java` | Feed & Filter Toolbar | Central alert feed with category filtering (`Bookings`, `Maintenance`, `Payments`, `System`). |
| `WorkoutPlansView.java` | Plan Card Grid | Fitness plan catalogue with enrollment triggers and pricing tables. |
| `WorkoutPlanDetailView.java`| Detail View | In-depth regimen breakdown with workout routine lists and instructor assignment. |
| `AdminReportsView.java` | Analytics Report Hub | Business intelligence summaries, financial trends, equipment downtime, and trainer workloads. |
| `MeasurementAnalyticsView.java`| Metric Trends | Longitudinal body measurement graphs, BMI calculation, and historical delta comparisons. |
| `AnalysisView.java` | Member Analytics View | Simplified member-facing progress charts and biometric comparison summary. |
| `InstructorProfileView.java`| Trainer Profile | Dedicated instructor portfolio showing assigned clients and active schedules. |

---

## 10. 🎨 Custom UI Components (`com.iwfc.ui.components`)

| File Path | Base Class | Description |
|---|---|---|
| `DialogUtils.java` | Utility | Removes native OS borders and applies glassmorphic styling to JavaFX `Dialog` and `Alert` dialogs. |
| `StatCard.java` | `VBox` | Glassmorphic KPI card with title, large numerical metric, trend chip, and icon badge. |
| `StatusBadge.java` | `Label` | Color-coded status badge for equipment conditions, bookings, and incident urgencies. |
| `ToastNotification.java`| Observer Listener | Animated slide-in notification toast appearing in top-right corner with auto-dismiss. |
| `UsageRing.java` | `StackPane` | Circular progress ring visualizing equipment usage and capacity percentage. |

---

## 11. 💅 Design System & CSS Stylesheets (`src/main/resources/com/iwfc/css`)

| Stylesheet File | Scope | Key Rules & Classes |
|---|---|---|
| `common.css` | Global | Core typography, base glassmorphic card tokens, button styling, login styles, toast notifications. |
| `dark-overrides.css` | Dark Theme | AtlantaFX `CupertinoDark` overrides: deep dark glass backgrounds, neon accents, white text fills. |
| `light-overrides.css` | Light Theme | AtlantaFX `CupertinoLight` overrides: clean white surfaces, subtle borders, high-contrast dark text fills. |
| `dashboard-management.css` | Dashboard | KPI card grids, telemetry chart styling, ledger table rows, duty meter bars, quick-action modals. |
| `equipment-management.css` | Equipment | Blueprint map zones, live pulsing status dots, Donut Hub cards, category chips. |
| `payments-management.css` | Payments | POS modal inputs, revenue KPI tiles, payment method selector buttons, invoice preview cards. |
| `members-management.css` | Members | Member cards, active plan tags, attendance streak counters, report card layouts. |
| `team-management.css` | Team / Staff | Staff directory tables, instructor cards, specialty badge pills, credential chips. |
| `notification-center.css` | Notifications | Unread notification highlighting, category filter pill buttons, action icons. |
| `followup-pipeline.css` | Follow-Ups | Kanban stages, convertibility badge colors (`HIGH`, `MEDIUM`, `LOW`), interaction timelines. |
| `member-reports.css` | Reports | Performance graphs, measurement cards, BMI gauge widgets, progress delta pills. |
| `workout-plans.css` | Regimens | Plan card grid, tier badges, pricing tags, routine bullet items. |
| `session-schedule.css` | Sessions | Timetable slots, day-of-week headers, capacity indicators. |

---

## 12. 🖼️ Static Media Assets (`src/main/resources/com/iwfc/images`)

| Asset Path | Type | Description |
|---|---|---|
| `images/anatomy.jpg` | Image | High-resolution anatomical diagram for biometric muscle targeting. |
| `images/male_anatomy.jpg` | Image | Male muscle group diagram for measurement tracking views. |
| `images/female_anatomy.jpg` | Image | Female muscle group diagram for measurement tracking views. |
| `images/login-bg.jpg` | Image | High-resolution branding illustration asset. |
| `images/avatars/` | Directory | Member and trainer profile avatars. |
| `images/exercises/` | Directory | Exercise routine thumbnails and technique illustrations. |

---

## 13. 🧪 Automated Unit Test Suite (`src/test/java/com/iwfc`)

| Test Suite File | Test Target | Test Count | Key Scenarios Tested |
|---|---|:---:|---|
| `exception/ExceptionVerificationTest.java` | Exception Hierarchy | **3** | Validates exception inheritance, status codes, and friendly message generation. |
| `pattern/RepositoryTest.java` | `Repository<T>` | **3** | Verifies entity save, findById, findAll, update, and delete semantics. |
| `pattern/payment/PaymentStrategyTest.java`| Strategy Pattern | **7** | Tests Card, Wallet, Bank Transfer, and Cash processing strategies with mock gateway. |
| `service/MaintenanceWorkflowTest.java` | `MaintenanceService` | **3** | Verifies work order reporting, urgency triage, status transitions, and equipment flag. |
| `service/SchedulingValidatorTest.java` | `SchedulingValidator` | **10** | Tests double-booking prevention, room capacity checks, trainer overlap, and valid hours. |
| **Total Automated Tests** | — | **26** | **100% Passing (`mvn test`)** |

---

## 14. 🛠️ Build, Config & Generated Artifacts

| File / Folder Path | Type | Description |
|---|---|---|
| `pom.xml` | Maven Configuration | Project dependencies (JavaFX 21, AtlantaFX 2.0.1, PDFBox 2.0.30, JUnit 5) and build plugins. |
| `README.md` | Markdown Documentation | Complete project documentation, architecture diagram, user guide, and installation steps. |
| `FILE_CATEGORIES.md` | Markdown Inventory | This comprehensive file inventory and category mapping guide. |
| `bills/` | Directory | Default destination for generated PDF invoices and receipts (`Receipt_PAY-XXXXXXXX.pdf`). |
