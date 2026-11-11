# Global Accessibility Theme Management Implementation Plan

This implementation plan outlines the creation of a centralized state management system (`ThemeManager`) to control global accessibility toggles (Focus Mode and High Contrast Mode) and dynamically adapt the entire application's visual appearance and animation behavior accordingly. This is crucial for ensuring compliance with modern Android UX guidelines and providing an inclusive user experience across all major views.

## User Review Required

The following decisions require your review:

*   **Animation Policy:** Disabling complex physics animations (e.g., using `animateContentSize` or Spring effects) when Focus Mode is active is the most robust approach to meet accessibility standards, as suggested by Android guidelines for cognitive load reduction.
    > [!IMPORTANT]
    > **Action:** This plan assumes that all existing composables relying on expressive motion (e.g., morphing cards, animated expansions) will need to be wrapped or modified to respect the `isFocusModeActive` state by limiting them to basic fade transitions when required. Please review this assumption against any specific component behaviors I might have missed.
*   **Color Palette Definition:** The plan uses Material 3's core color system as a base, implementing dedicated logic for Focus Mode and High Contrast. We will use the standard M3 utilities but create explicit fallbacks/overrides for these modes.

## Open Questions

1.  **Animation Complexity:** Are there any *critical* components (e.g., certain custom drag gestures or animations) whose expressive motion must be preserved even in Focus Mode? If so, they will need to require a manual exclusion rule from the `ThemeManager` logic.
2.  **Theming Source of Truth:** Is the primary source for theme definition the system's dynamic color scheme (Material 3 Dynamic Color), or should we prioritize a developer-defined fallback palette for *all* modes (e.g., always using a specific deep blue/teal)?

## Proposed Changes

We will introduce a new package and core files to house the global state manager and theme logic, modifying existing views to consume this context.

### 1. State Management & Theme Context (`ui` package)

#### [NEW] `ThemeManagerViewModel.kt`(file:///app/src/main/java/com/example/neurocalendar/ui/ThemeManagerViewModel.kt)
*   **Purpose:** Holds the core application state (`StateFlow`) for the accessibility toggles (`isFocusModeActive`, `isHighContrastActive`). Exposes a function to update both states atomically and triggers UI recomposition upon change.

#### [MODIFY] `CompositionLocal.kt` (file:///app/src/main/java/com/example/neurocalendar/ui/theme/ThemeCompositionLocal.kt)
*   **Purpose:** Defines the `CompositionLocal` that will hold the current theme state and manager reference, making it globally accessible to any composable tree depth.

#### [MODIFY] `Theme.kt` (file:///app/src/main/java/com/example/neurocalendar/ui/theme/Theme.kt)
*   **Purpose:** The main entry point for the app's theme. It will accept the theme state from the CompositionLocal, calculate the final `ColorScheme`, and wrap all child composables with a custom `CompositionLocalProvider` that forces the application of our dynamic theme rules.

### 2. Core Views (Consumers)

#### [MODIFY] `MainActivity.kt` (file:///app/src/main/java/com/example/neurocalendar/MainActivity.kt)
*   **Purpose:** The main activity will be updated to initialize and observe the `ThemeManagerViewModel`, passing its state down to the primary navigation wrapper, thus ensuring the entire app receives the theme context from the root.

#### [MODIFY] `CalendarRootScreen.kt` (Assuming this file exists or is the entry point for the week/month view)
*   **Purpose:** This screen will be updated to read and pass the current accessibility state down through its children, ensuring consistency.

### 3. Utility / Dependencies

We may need to add dependencies to `app/build.gradle.kts` if we use complex flow operators or specialized ViewModel factories. I will check this during execution.

## Verification Plan

### Automated Tests
1.  **Unit Testing:** Write unit tests for the new `ThemeManagerViewModel` to ensure state toggles correctly update and that derived theme logic (e.g., calculating High Contrast colors) is correct given specific boolean inputs.
2.  **Integration Testing:** Run instrumented tests on the main activity/root screen (`MainActivity`) while simulating the toggle changes (Focus Mode $\to$ Off, High Contrast $\to$ On). We must verify that key UI components like buttons and backgrounds correctly reflect the calculated colors and that animations behave as expected.

### Manual Verification
1.  Run the app on an emulator configured with a system theme change capability to test the dynamic color flow (if applicable).
2.  Manually toggle Focus Mode and High Contrast Mode via the provided UI controls and verify:
    *   Color palette changes across *all* visible composables (buttons, backgrounds, text).
    *   Animations are disabled/simplified when Focus Mode is active.
    *   The state persists correctly through screen navigations (e.g., navigating from WeekView to DayView then back).

I will proceed with the implementation once these points are addressed and approved.