# APVM - File-by-File Implementation Guide

This document explains what each file in APVM is responsible for and what kind of code should be written inside it.

The main rule is:

- `ui/` → UI and user interaction
- `data/` → Android API calls, processing, analysis, calculations, database operations
- `model/` → data classes and enums
- `navigation/` → screen-to-screen navigation
- `MainActivity.kt` → application startup

Do not put business logic directly inside UI screens unless it is simple UI state handling.

---

# 1. MainActivity.kt

### Purpose

Entry point of the Android application.

### Should contain

- `ComponentActivity`
- `onCreate()`
- `setContent {}`
- APVM theme
- `AppNavigation()`
- edge-to-edge configuration

### Example

```kotlin
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            AndroidManagerTheme {
                AppNavigation()
            }
        }
    }
}
```

### Should NOT contain

* APK analysis
* permission scanning
* risk calculations
* database code
* large UI implementations

---

# 2. navigation/Screen.kt

### Purpose

Defines every screen and its navigation route.

### Should contain

One entry for every screen.

Current:

```kotlin
sealed class Screen(val route: String) {

    data object Dashboard : Screen("dashboard")

    data object PermissionAuditor :
        Screen("permission_auditor")

    data object ProcessManager :
        Screen("process_manager")
}
```

When remaining screens are implemented, add:

```kotlin
data object ApkScanner :
    Screen("apk_scanner")

data object RiskAssessment :
    Screen("risk_assessment")

data object SecurityReport :
    Screen("security_report")

data object ScanHistory :
    Screen("scan_history")
```

### Should NOT contain

* UI
* repositories
* Android API calls
* analysis logic

---

# 3. navigation/AppNavigation.kt

### Purpose

Connects routes to actual Compose screens.

### Should contain

* `NavController`
* `NavHost`
* `composable()` destinations
* navigation callbacks

Example:

```kotlin
composable(Screen.Dashboard.route) {
    DashboardScreen(
        onNavigateToPermissions = {
            navController.navigate(
                Screen.PermissionAuditor.route
            )
        }
    )
}
```

### When adding a new screen

Add its route to `Screen.kt` and its destination to `AppNavigation.kt`.

### Should NOT contain

* APK analysis
* permission classification
* risk calculation
* database operations

---

# 4. model/AppInfo.kt

### Purpose

Represents an installed Android application.

### Should contain

```kotlin
data class AppInfo(
    val name: String,
    val packageName: String,
    val versionName: String?,
    val isSystemApp: Boolean
)
```

### Used by

* Dashboard
* Permission Auditor
* APK Scanner
* Risk Assessment
* Security Report
* Scan History

Do not put Android API calls here.

---

# 5. model/ProcessInfo.kt

### Purpose

Represents process/application activity information.

### Should contain

```kotlin
data class ProcessInfo(
    val processName: String,
    val pid: Int,
    val memoryMb: Long,
    val importance: Int,
    val isForeground: Boolean
)
```

### Important

If Android does not expose a value, do not invent one.

For the current UsageStats implementation:

```text
pid = -1
memoryMb = 0
```

represent unavailable information.

---

# 6. model/RiskLevel.kt

### Purpose

Defines APVM's security severity/risk levels.

### Should contain

```kotlin
enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
```

Used across:

* scanner
* risk assessment
* reports
* history

---

# 7. data/apps/AppRepository.kt

### Purpose

Retrieves installed applications from the Android device.

### Android API used

```text
PackageManager
```

### Should contain

Code for:

* getting installed applications
* getting application names
* getting package names
* getting version information
* identifying system applications

### Flow

```text
Android PackageManager
        ↓
AppRepository
        ↓
List<AppInfo>
        ↓
UI
```

### Current implementation

The repository calls:

```kotlin
packageManager.getInstalledApplications(0)
```

and converts each `ApplicationInfo` into an `AppInfo`.

### Should NOT contain

* Compose UI
* navigation
* risk calculations

---

# 8. data/permissions/PermissionRepository.kt

### Purpose

Retrieves and classifies permissions requested by installed applications.

### Android API used

```text
PackageManager
```

### Should contain

* requested permission retrieval
* permission descriptions
* permission classification
* dangerous permission detection
* special permission detection

### Main data structures

```kotlin
data class PermissionEntry(
    val name: String,
    val description: String,
    val category: String
)
```

and:

```kotlin
data class AppPermissionInfo(
    val app: AppInfo,
    val permissions: List<PermissionEntry>
)
```

### Classification

Permissions are classified into categories such as:

```text
NORMAL
DANGEROUS
SPECIAL
```

### Important

The repository only identifies permissions and security indicators.

A dangerous permission does NOT automatically mean that the application is malicious.

---

# 9. data/process/ProcessRepository.kt

### Purpose

Retrieves recently active application/process information.

### Android API used

```text
UsageStatsManager
```

### Should contain

* Usage Access checking
* usage event queries
* detection of recently foregrounded applications
* conversion into `ProcessInfo`

### Main function

```kotlin
fun hasUsageAccess(): Boolean
```

Checks whether the user has granted Usage Access.

### Current process detection

The repository checks events such as:

```text
ACTIVITY_RESUMED
MOVE_TO_FOREGROUND
```

and records the most recent activity for each package.

### Important Android limitation

Normal Android applications cannot freely inspect or terminate arbitrary processes.

Therefore this repository must only expose information actually available through Android's permitted APIs.

---

# 10. ui/theme/Color.kt

### Purpose

Contains APVM's color definitions.

### Should contain

```kotlin
val Background = Color(0xFF000000)
val Surface = Color(0xFF111111)
val SurfaceVariant = Color(0xFF151515)
val PrimaryText = Color(0xFFFFFFFF)
val SecondaryText = Color(0xFF888888)
val Border = Color(0xFF252525)
val Graphite = Color(0xFF6B6B6B)
```

No feature-specific logic should be placed here.

---

# 11. ui/theme/Theme.kt

### Purpose

Defines the Material 3 application theme.

### Should contain

* `darkColorScheme`
* Material theme configuration
* APVM colors
* system bar configuration

---

# 12. ui/theme/Type.kt

### Purpose

Defines typography.

### Should contain

* font sizes
* font weights
* typography styles
* Material typography configuration

---

# 13. ui/components/APVMTopBar.kt

### Purpose

Reusable APVM top bar.

### Should contain

* APVM title/logo
* top bar layout
* status bar spacing
* reusable top-level UI styling

### Should NOT contain

* feature-specific data
* repository calls
* navigation logic

---

# 14. ui/components/APVMBottomBar.kt

### Purpose

Reusable bottom navigation UI.

### Should contain

* navigation items
* icons
* selected/unselected state
* bottom navigation layout

Navigation actions should be provided through callbacks or the navigation layer.

---

# 15. ui/components/StatCard.kt

### Purpose

Reusable statistics card.

Example:

```text
142
APPS
```

or:

```text
84
PROCESSES
```

### Should contain

Only the UI for displaying:

* value
* label
* card styling

Example usage:

```kotlin
StatCard(
    value = "142",
    label = "APPS"
)
```

The data itself should come from the screen/repository.

---

# 16. ui/components/ModuleCard.kt

### Purpose

Reusable card for APVM modules.

Examples:

```text
PROCESS MANAGER
PERMISSION AUDITOR
APK SCANNER
RISK ASSESSMENT
SECURITY REPORT
SCAN HISTORY
```

### Should contain

* title
* description
* icon/visual
* clickable UI

The actual feature logic belongs to the relevant repository/screen.

---

# 17. ui/components/FindingCard.kt

### Purpose

Reusable security finding card.

Example:

```text
HIGH

Insecure WebView

com.example.app
```

### Should contain

* severity
* finding name
* affected application/component
* styling

It should receive data rather than calculate the finding itself.

---

# 18. ui/dashboard/DashboardScreen.kt

### Purpose

Main APVM dashboard.

### Should contain

Dashboard UI for:

* application count
* process count
* dangerous permission count
* vulnerability count
* module cards
* recent findings

### Current data sources

```text
AppRepository
ProcessRepository
```

### Current flow

```text
DashboardScreen
      ↓
AppRepository
      ↓
Installed application count

DashboardScreen
      ↓
ProcessRepository
      ↓
Recently active application count
```

### Navigation

The screen receives callbacks such as:

```kotlin
onNavigateToPermissions
onNavigateToProcessManager
```

and invokes them when the corresponding module card is clicked.

### Future changes

Currently some dashboard security values/findings may be placeholders.

Once APK Scanner and Risk Assessment are implemented, these should use real data.

---

# 19. ui/permissions/PermissionAuditorScreen.kt

### Purpose

UI for auditing installed application permissions.

### Should contain

* app list
* search
* dangerous permission display
* special permission display
* permission details
* risk indicator UI
* loading/empty states

### Data source

```text
PermissionRepository
```

### Flow

```text
PermissionAuditorScreen
        ↓
PermissionRepository
        ↓
PackageManager
        ↓
AppPermissionInfo
        ↓
Compose UI
```

### Important

The screen should display the classification produced by the repository.

Do not duplicate permission classification logic inside the screen.

---

# 20. ui/process/ProcessManagerScreen.kt

### Purpose

UI for the Process Manager.

### Should contain

* process/application list
* search
* refresh
* CPU/RAM/stat UI where data is actually available
* foreground/recent status
* Usage Access warning
* button to open Usage Access settings
* loading state
* empty state

### Data source

```text
ProcessRepository
```

### Usage Access flow

```text
Process Manager
      ↓
Usage Access unavailable
      ↓
GRANT USAGE ACCESS
      ↓
Android Settings
      ↓
Return to APVM
      ↓
Refresh data
```

### Important

Do not fabricate PID, RAM, CPU or process-control data.

Only show values actually available through Android APIs.

---

# 21. APK SCANNER FILES

The APK Scanner is the next major feature.

---

## ui/scanner/ApkScannerScreen.kt

### Purpose

Main UI for selecting and scanning an APK.

### Should contain

* APK file picker
* selected APK information
* Analyze button
* scan progress
* analysis stages
* scan result
* vulnerability findings

### Example UI flow

```text
SELECT APK
     ↓
APK SELECTED
     ↓
ANALYZE APK
     ↓
SCANNING
     ↓
ANALYSIS COMPLETE
     ↓
FINDINGS
```

### Should NOT contain

The actual APK parsing or vulnerability detection algorithms.

---

## ui/scanner/ApkFindingCard.kt

### Purpose

Displays one vulnerability found during APK analysis.

### Should contain

* severity
* finding name
* affected component
* evidence
* impact
* recommendation

Example:

```text
HIGH

INSECURE WEBVIEW

Affected component:
MainActivity

Evidence:
JavaScript enabled

Impact:
Potential WebView attack surface

Recommendation:
Review WebView configuration
```

---

## data/scanner/ApkScannerRepository.kt

### Purpose

Main coordinator for the APK scanning process.

### Should contain

* receiving selected APK
* invoking analyzer functions
* collecting analyzer results
* returning structured findings
* coordinating scan stages

### Flow

```text
APK Scanner Screen
        ↓
ApkScannerRepository
        ↓
ApkAnalyzer
        ↓
VulnerabilityFinding
```

---

## data/scanner/ApkAnalyzer.kt

### Purpose

Contains the actual static security analysis logic.

### Should contain

Checks such as:

* AndroidManifest analysis
* permission analysis
* exported components
* component protection
* WebView configuration
* JavaScript interfaces
* local file access
* sensitive/hardcoded information indicators
* other static security checks defined for APVM

### Important

This is the main security-analysis file.

Do not put Compose code here.

---

## model/ApkInfo.kt

### Purpose

Stores metadata extracted from the APK.

### Should contain

Possible fields:

```kotlin
data class ApkInfo(
    val packageName: String,
    val versionName: String?,
    val versionCode: Long?,
    val minSdk: Int?,
    val targetSdk: Int?
)
```

Additional metadata can be added if required by the scanner.

---

## model/VulnerabilityFinding.kt

### Purpose

Standard structure for a vulnerability/security finding.

### Should contain

```kotlin
data class VulnerabilityFinding(
    val name: String,
    val severity: RiskLevel,
    val affectedComponent: String?,
    val evidence: String,
    val impact: String,
    val recommendation: String
)
```

This model should be shared by:

```text
APK Scanner
Risk Assessment
Security Report
Scan History
```

---

# 22. RISK ASSESSMENT FILES

---

## ui/risk/RiskAssessmentScreen.kt

### Purpose

Displays the calculated security risk.

### Should contain

* overall score
* overall risk level
* permission risk
* vulnerability risk
* component exposure risk
* explanation of major contributors

Example:

```text
82 / 100

CRITICAL

PERMISSION RISK       MEDIUM
VULNERABILITY RISK    HIGH
COMPONENT EXPOSURE    CRITICAL
```

The screen displays the result.

It should NOT calculate the score itself.

---

## ui/risk/RiskScoreCard.kt

### Purpose

Reusable UI component for displaying a risk score.

### Should receive

* score
* risk level
* label

### Should contain

Only UI/presentation logic.

---

## data/risk/RiskAssessmentRepository.kt

### Purpose

Calculates the security risk.

### Should contain

The APVM risk calculation logic.

Inputs may include:

```text
Permission Risk
Vulnerability Risk
Component Exposure Risk
Severity weights
```

Output:

```text
RiskAssessment
```

### Flow

```text
VulnerabilityFinding
        +
Permission information
        +
Component exposure
        ↓
RiskAssessmentRepository
        ↓
RiskAssessment
```

The risk formula/weights should be implemented here, not in the UI.

---

## model/RiskAssessment.kt

### Purpose

Stores the final risk calculation.

### Should contain

```kotlin
data class RiskAssessment(
    val permissionRisk: Int,
    val vulnerabilityRisk: Int,
    val componentExposureRisk: Int,
    val overallScore: Int,
    val overallLevel: RiskLevel
)
```

---

# 23. SECURITY REPORT FILES

---

## ui/report/SecurityReportScreen.kt

### Purpose

Displays the complete security report.

### Should contain

* APK/application information
* overall security score
* overall risk
* vulnerabilities
* permissions
* exposed components
* evidence
* impact
* recommendations

---

## ui/report/ReportFindingCard.kt

### Purpose

Displays one finding inside the security report.

### Should contain

* severity
* vulnerability name
* affected component
* evidence
* impact
* recommendation

It should use the common:

```text
VulnerabilityFinding
```

model.

---

## ui/report/ReportSummaryCard.kt

### Purpose

Displays a high-level report summary.

Example:

```text
SECURITY SCORE

82 / 100

CRITICAL

7 vulnerabilities
12 dangerous permissions
3 exposed components
```

This is presentation only.

---

## data/report/SecurityReportRepository.kt

### Purpose

Builds a complete security report from scanner and risk data.

### Should contain

Logic to combine:

```text
AppInfo
ApkInfo
VulnerabilityFinding
RiskAssessment
Permission information
```

into:

```text
SecurityReport
```

### Flow

```text
APK Scanner
     ↓
Findings
     ↓
Risk Assessment
     ↓
SecurityReportRepository
     ↓
SecurityReport
     ↓
SecurityReportScreen
```

---

## model/SecurityReport.kt

### Purpose

Represents a complete security report.

### Should contain

```kotlin
data class SecurityReport(
    val app: AppInfo?,
    val apkInfo: ApkInfo?,
    val findings: List<VulnerabilityFinding>,
    val riskAssessment: RiskAssessment
)
```

---

# 24. SCAN HISTORY FILES

---

## ui/history/ScanHistoryScreen.kt

### Purpose

Displays previous APK/application security scans.

### Should contain

* scan history list
* application name
* package name
* scan date
* score
* risk level
* finding count
* opening previous scan results

The screen should retrieve history through:

```text
ScanHistoryRepository
```

---

## data/history/AppDatabase.kt

### Purpose

Defines the local Room database.

### Should contain

* Room `@Database`
* database entities
* database configuration
* DAO references

Possible entities:

```text
ScanHistory
```

Additional entities can be added if detailed findings need to be persisted separately.

---

## data/history/ScanHistoryRepository.kt

### Purpose

Interface between Scan History UI and Room database.

### Should contain

Functions such as:

```kotlin
saveScan(...)
getScanHistory(...)
getScanById(...)
deleteScan(...)
```

### Flow

```text
Scan completed
      ↓
ScanHistoryRepository
      ↓
Room Database
      ↓
ScanHistoryScreen
```

---

## model/ScanHistory.kt

### Purpose

Represents one stored scan.

### Should contain

```kotlin
data class ScanHistory(
    val id: Long,
    val appName: String,
    val packageName: String,
    val scanDate: Long,
    val score: Int,
    val riskLevel: RiskLevel,
    val findingCount: Int
)
```

---

# 25. COMPLETE FILE STRUCTURE

```text
com/example/android_manager/

├── MainActivity.kt
│
├── navigation/
│   ├── AppNavigation.kt
│   └── Screen.kt
│
├── model/
│   ├── AppInfo.kt
│   ├── ProcessInfo.kt
│   ├── RiskLevel.kt
│   ├── ApkInfo.kt
│   ├── VulnerabilityFinding.kt
│   ├── RiskAssessment.kt
│   ├── SecurityReport.kt
│   └── ScanHistory.kt
│
├── data/
│   ├── apps/
│   │   └── AppRepository.kt
│   │
│   ├── permissions/
│   │   └── PermissionRepository.kt
│   │
│   ├── process/
│   │   └── ProcessRepository.kt
│   │
│   ├── scanner/
│   │   ├── ApkScannerRepository.kt
│   │   └── ApkAnalyzer.kt
│   │
│   ├── risk/
│   │   └── RiskAssessmentRepository.kt
│   │
│   ├── report/
│   │   └── SecurityReportRepository.kt
│   │
│   └── history/
│       ├── AppDatabase.kt
│       └── ScanHistoryRepository.kt
│
└── ui/
    ├── theme/
    │   ├── Color.kt
    │   ├── Theme.kt
    │   └── Type.kt
    │
    ├── components/
    │   ├── APVMTopBar.kt
    │   ├── APVMBottomBar.kt
    │   ├── StatCard.kt
    │   ├── ModuleCard.kt
    │   └── FindingCard.kt
    │
    ├── dashboard/
    │   └── DashboardScreen.kt
    │
    ├── permissions/
    │   └── PermissionAuditorScreen.kt
    │
    ├── process/
    │   └── ProcessManagerScreen.kt
    │
    ├── scanner/
    │   ├── ApkScannerScreen.kt
    │   └── ApkFindingCard.kt
    │
    ├── risk/
    │   ├── RiskAssessmentScreen.kt
    │   └── RiskScoreCard.kt
    │
    ├── report/
    │   ├── SecurityReportScreen.kt
    │   ├── ReportFindingCard.kt
    │   └── ReportSummaryCard.kt
    │
    └── history/
        └── ScanHistoryScreen.kt
```

# 26. How Team Members Should Work

Each feature should follow this pattern:

```text
SCREEN
  ↓
REPOSITORY
  ↓
ANDROID API / ANALYZER / DATABASE
  ↓
MODEL
  ↓
SCREEN
```

For example, APK Scanner:

```text
ApkScannerScreen.kt
        ↓
ApkScannerRepository.kt
        ↓
ApkAnalyzer.kt
        ↓
ApkInfo.kt
VulnerabilityFinding.kt
        ↓
ApkScannerScreen.kt
```

Risk Assessment:

```text
RiskAssessmentScreen.kt
        ↓
RiskAssessmentRepository.kt
        ↓
RiskAssessment.kt
        ↓
RiskAssessmentScreen.kt
```

Scan History:

```text
ScanHistoryScreen.kt
        ↓
ScanHistoryRepository.kt
        ↓
AppDatabase.kt
        ↓
ScanHistory.kt
        ↓
ScanHistoryScreen.kt
```

# 27. Important Development Rule

Before creating a new file, check whether the functionality already belongs in an existing file.

Do not create multiple repositories or models for the same responsibility.

Each file should have one clear responsibility.

```text
UI problem
→ ui/

Android/data problem
→ data/

Data structure
→ model/

Navigation problem
→ navigation/
```
