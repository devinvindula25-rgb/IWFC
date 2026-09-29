# IWFC - Video Demo Script (10 Minutes)

## 0:00 - 1:00 | Introduction & Architecture
- **Visuals:** Show the splash screen or login screen.
- **Voiceover:** "Welcome to the Intelligent Wellness and Fitness Center (IWFC) demonstration. Our project is a comprehensive JavaFX application that manages modern gym operations. We'll start by looking at our system architecture, which employs a Facade pattern as the central entry point, supported by Singletons for repository management and the Observer pattern for a real-time notification engine."

## 1:00 - 2:30 | The Administrator Experience & Dashboard
- **Visuals:** Log in as Admin. Navigate through Dashboard.
- **Voiceover:** "Logging in as an Administrator, we immediately see the premium, glass-morphism inspired dashboard. Note the smooth fade-in animations as we navigate. The dashboard provides a high-level overview of active members, revenue via our Strategy-pattern-driven payment system, and system health."
- **Action:** Toggle Dark/Light mode using the top-left sun/moon icon.

## 2:30 - 4:30 | User & Instructor Management
- **Visuals:** Click on 'Users' in the sidebar. Select an Instructor from the list.
- **Voiceover:** "In the User Management module, we've implemented advanced UI features. Notice the Instructor Insights panel: here we track the 'Impact Score' using a dynamic radial gauge. We can also see an avatar stack displaying their assigned trainees. This demonstrates our complex data relationship where members enroll in fitness plans constructed via the Builder and Factory patterns, and are subsequently assigned to instructors."

## 4:30 - 6:30 | Equipment & Maintenance
- **Visuals:** Navigate to 'Equipment' and 'Maintenance'. Log some usage hours on a treadmill.
- **Voiceover:** "Equipment management is fully automated. When I log usage on this treadmill that pushes it past its maintenance threshold, the system automatically changes its status and generates a maintenance request. The Observer pattern then immediately dispatches a notification to the maintenance staff."
- **Action:** Resolve the generated maintenance request to show the loop closing.

## 6:30 - 8:30 | Session Booking & Validation
- **Visuals:** Navigate to 'Sessions'. Book a session for a member. Try to book a conflicting session.
- **Voiceover:** "Our session scheduler includes robust validation. It checks for capacity limits and time clashes. Watch what happens when I attempt to double-book a member or an instructor—the system instantly blocks the action and provides a descriptive error, handled securely by our custom exception hierarchy."

## 8:30 - 10:00 | Code Review & Conclusion
- **Visuals:** Switch to IDE. Show `IWFCSystemFacade.java` and a JUnit test class.
- **Voiceover:** "To ensure reliability, we wrote 25+ JUnit tests covering these core workflows, particularly focusing on edge cases and negative paths. As you can see, our code remains highly modular and strictly adheres to SOLID principles. Thank you for watching the IWFC demonstration."
