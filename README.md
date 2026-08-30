# ClubLedger ⚽ 🏸 🏀

**ClubLedger** is an Android application built with Jetpack Compose and Room Database designed for community sports clubs, recreation teams, and amateur leagues. It automates attendance tracking, member payments, treasury balances, and proportional budget cost allocations.

---

## 👥 Target User Personas

ClubLedger is architected around the real-world operational workflows of community athletic clubs, balancing administrative control with member transparency:

### 1. 🛡️ Marcus — The Club Treasurer & Admin
- **Role**: Volunteer Treasurer / Club Manager.
- **Background**: Manages the finances, facility rentals, coach fees, and tournament entries for an adult amateur sports league (e.g., 3 teams, 40+ active members).
- **Core Goals**:
  - Eliminate spreadsheet chaos and untracked Venmo/Zelle payments.
  - Fairly allocate monthly facility rental costs based on who actually showed up to training and match days.
  - Maintain an immutable, tamper-evident audit trail for annual general meetings and transparency reports.
- **Key App Touchpoints**:
  - Treasury Dashboard & Budget Utilization tracking.
  - One-tap Attendance & Payment Approval Hub.
  - Proportional Cost Allocation Engine (1-tap invoice distribution).
  - Manual Balance Adjustments with mandatory audit reasoning (e.g., uniform purchase, early-bird credit).
  - One-click CSV Export for bookkeeping.

### 2. ⚡ Alex — The Active Playing Member
- **Role**: Regular Team Player (Member).
- **Background**: Plays in 3 to 4 training sessions and matches per month; travels occasionally for work.
- **Core Goals**:
  - Ensure he is only billed for sessions he actually attended (Zero-Attendance Fair Share guarantee).
  - Clear visibility into his current dues balance (*Settled*, *Payment Due*, or *Credit*).
  - Fast, friction-free payment logging with reference notes (#Zelle, #Venmo, Cash receipt).
- **Key App Touchpoints**:
  - Personal Financial Status Card & Running Balance Ledger.
  - "How My Cost Was Calculated" mathematical breakdown dialog.
  - Attendance check-in submitter with date and session type selection.
  - Payment submission form with status tracking (*Pending Approval* ➔ *Approved*).

### 3. 📋 Coach Sarah — Team Captain & Field Coordinator
- **Role**: Team Captain / Attendance Verifier.
- **Background**: Coordinates weekly practice sessions and weekend league fixtures on the field.
- **Core Goals**:
  - Quickly verify which players attended training sessions immediately after practice.
  - Flag unexcused absences or casual drop-ins.
- **Key App Touchpoints**:
  - Attendance management list with bulk approval and rejection notes.
  - Real-time roster view and active team selector.

---

## 🎯 Real-World Use Case Scenarios

### Use Case 1: Proportional Fair-Share Facility Allocation
* **Context**: The club rents a court/pitch for \$600.00 for the month of August across Team Alpha.
* **Problem**: 
  - Player A attended 8 sessions.
  - Player B attended 2 sessions.
  - Player C was injured and attended 0 sessions.
  - Traditional flat-fee dues unfairly charge Player B and Player C the same amount as Player A.
* **ClubLedger Solution**:
  - Total team attendance = 10 sessions.
  - **Player A's share**: $(8 / 10) \times \$600.00 = \$480.00$.
  - **Player B's share**: $(2 / 10) \times \$600.00 = \$120.00$.
  - **Player C's share**: $(0 / 10) \times \$600.00 = \$0.00$ (Automatic Zero-Attendance exemption).
  - The Treasurer taps **"Generate & Apply Invoices"**; individual debit entries are automatically written to each member's running balance ledger with complete calculation transparency.

### Use Case 2: Multi-Method Payment Verification with Audit Log
* **Context**: Player B sends \$120.00 via Zelle with memo `#ZEL-8831`.
* **Workflow**:
  1. Player B opens the app, selects *Zelle*, enters `120.00`, types `#ZEL-8831`, and submits.
  2. The payment appears in the Admin's **Pending Approvals** badge counter.
  3. Marcus (Treasurer) cross-references his bank app and taps **Approve**.
  4. The system:
     - Marks the payment as `APPROVED`.
     - Credits Player B's balance, changing their badge from *Payment Due (\$120.00)* to *Settled (\$0.00)*.
     - Creates an immutable `AuditLog` entry timestamped with user IDs and reference notes.

### Use Case 3: Remote Multi-Device Field Operations (Cloud Sync)
* **Context**: Coach Sarah is on the sports field approving attendances from her phone, while Marcus the Treasurer is at home reviewing finances on his tablet.
* **Workflow**:
  - Both devices connect to the shared channel `thunder-sports-club`.
  - As Sarah approves player check-ins, `FirestoreSyncManager` pushes updates in real-time.
  - Marcus's dashboard automatically reflects the new session totals and adjusts budget burn rates with 0ms manual polling.

---

## 🌟 Key Features

### 1. Dual-Role Architecture with Instant Switcher
- **Member View**:
  - Personal financial dashboard displaying current balance status (*Settled*, *Payment Due*, or *Overpaid Credit*).
  - Monthly attendance history with status badges (*Approved*, *Pending*, *Rejected*).
  - Invoices and transparent cost breakdown dialog explaining exact formula calculations.
  - Payment submission form supporting Bank Transfer, Zelle, Venmo, Cash, Card, and Check.
- **Admin View**:
  - Club Treasury overview (Total Dues Invoiced, Total Collected, Pending Approvals, Net Club Balance).
  - Team-by-team budget utilization and financial progress bars.
  - Quick-action approval hub for member attendance and payment claims.
  - Member management roster with custom financial balance adjustments.
  - Comprehensive Audit Trail and CSV Export Center.
- **Role Switcher**: Seamlessly switch between preloaded admin & member accounts or register new members directly from the top bar.

---

### 2. Proportional Budget & Invoicing Engine
- **Fair-Share Formula**:
  $$\text{Member Invoice Share} = \left(\frac{\text{Member Approved Sessions}}{\text{Total Team Approved Sessions}}\right) \times \text{Total Monthly Budget}$$
- **Zero-Attendance Guarantee**: Members who attended 0 sessions in a billing period are automatically charged $\$0.00$.
- **Cost Transparency**: Members can inspect the "How My Cost Was Calculated" dialog to see total team sessions, individual attended sessions, proportional percentage, and itemized breakdown.
- **Automatic Re-allocation**: Admins can recalculate allocations if attendance records are updated after initial generation, updating balances and recording audit trail logs.

---

### 3. Attendance Tracking & Team Owner Quick-Marking
- **Member Self Check-In**: Players submit attendance entries for Training Sessions, League Matches, Tournaments, and Friendly games.
- **Team Owner / Coach Quick-Mark Tool**: Team owners can check off the full roster for a practice or match in 1 tap with date, session type, and notes, automatically creating verified and approved attendance records in bulk.
- **Verification Workflow**: Status flow `PENDING` ➔ `APPROVED` / `REJECTED` (with optional rejection notes).
- **Bulk 1-Tap Approvals**: Admins can approve all pending member claims with a single click.

---

### 4. Running Balance Ledger & Payment Requests
- **Double-Entry Balance Ledger**:
  - `INVOICE_DEBIT` (Proportional budget allocations)
  - `PAYMENT_REQUEST` (Ad-hoc requests for uniform/kit fees, tournament entries, league registration)
  - `PAYMENT` (Credit: increases member balance upon admin verification)
  - `ADJUSTMENT` / `PENALTY` / `CREDIT` / `REFUND` (Custom adjustments with required audit reason)
- **Team Owner Payment Request Tool**:
  - Issue dues requests to single players or entire team rosters with presets (Uniforms, Tournaments, League Fees, Custom).
  - 1-tap "Copy Group Chat Reminder" formatted for WhatsApp / SMS group chats with full payment details.
  - Automatically posts debit to the member's balance ledger with audit logging.
- **Real-Time Running Balance Calculation**: Tracks settled ($0), owing, and overpayment credits.

---

### 5. Audit Trail & CSV Export Hub
- Full immutable event audit logging for team creations, budget allocations, payment approvals, and manual adjustments.
- Instant copy-to-clipboard export for:
  - **Attendance Records CSV**
  - **Payments & Dues CSV**
  - **Full Running Balance Ledger CSV**

---

## 🗄️ Database & Data Model Architecture

ClubLedger utilizes **Android Jetpack Room (SQLite)** to maintain local persistence, integrity, and reactive real-time UI updates via Kotlin Flow.

### Entity Relationship & Schema

```
  ┌──────────┐         ┌───────────────────┐         ┌──────────┐
  │   User   │1───────*│  TeamMembership   │*───────1│   Team   │
  └────┬─────┘         └───────────────────┘         └────┬─────┘
       │                                                  │
       │1                                                 │1
       ├──────────────* ┌────────────────┐ *──────────────┤
       │                │   Attendance   │                │
       │                └────────────────┘                │
       │1                                                 │1
       ├──────────────* ┌────────────────┐ *──────────────┤
       │                │    Payment     │                │
       │                └────────────────┘                │
       │                                                  │1
       │                                                  │
       │                                     ┌────────────┴────┐
       │                                     │   TeamBudget    │
       │                                     └────────────┬────┘
       │                                                  │1
       │               ┌───────────────────┐              │
       │1             *│ InvoiceAllocation │*            1│
       ├───────────────┤   (Proportional)  ├──────────────┤
       │               └─────────┬─────────┘              │
       │                         │                        │
       │1                        │1                      1│
       │               ┌─────────┴─────────┐ ┌────────────┴────┐
       ├──────────────*│   BalanceLedger   │ │     Invoice     │
       │               │ (Running Balance) │ └─────────────────┘
       │               └───────────────────┘
       │1
       └──────────────* ┌────────────────┐
                        │    AuditLog    │
                        └────────────────┘
```

### Entity Definitions

| Entity | Table Name | Purpose & Key Attributes |
| :--- | :--- | :--- |
| **`User`** | `users` | Club members and administrators. Contains `role` (`"ADMIN"` / `"MEMBER"`), `status` (`"ACTIVE"` / `"INACTIVE"`), `avatarColorHex`, and contact details. |
| **`Team`** | `teams` | Sports teams and sub-clubs (Soccer, Badminton, Basketball, Running). Tracks `sportType`, `monthlyBudgetGoal`, and branding colors. |
| **`TeamMembership`** | `team_memberships` | Association between users and teams with `roleInTeam` (`"MEMBER"`, `"CAPTAIN"`, `"COACH"`). |
| **`Attendance`** | `attendances` | Event participation records. Tracks `sessionDate`, `sessionType` (`"TRAINING"`, `"MATCH"`, `"TOURNAMENT"`, `"FRIENDLY"`), `status` (`"PENDING"`, `"APPROVED"`, `"REJECTED"`), and admin review notes. |
| **`Payment`** | `payments` | Member dues payments. Includes `paymentMethod` (`"BANK_TRANSFER"`, `"VENMO_ZELLE"`, `"CASH"`, `"CARD"`, `"CHECK"`), `referenceNote`, `receiptNote`, and approval status. |
| **`TeamBudget`** | `team_budgets` | Periodic budget targets for pitch bookings, equipment, tournaments, and coaching fees for a specific `periodMonth` and `periodYear`. |
| **`Invoice`** | `invoices` | Master billing invoice generated from a budget period, storing `totalApprovedSessions` and calculated `costPerSession`. |
| **`InvoiceAllocation`** | `invoice_allocations` | Individual member dues breakdown. Stores `approvedSessionsCount`, `percentage` ($15.5\%$), and exact `allocatedAmount`. |
| **`BalanceLedger`** | `balance_ledger` | Immutable financial ledger. Records debits (`INVOICE_DEBIT`) and credits (`PAYMENT_CREDIT`, `MANUAL_ADJUSTMENT`) with a calculated `runningBalanceAfter`. |
| **`AuditLog`** | `audit_logs` | Immutable audit trail capturing timestamp, performed action, affected entity, and administrator ID. |

---

## 🏗️ System Architecture & End-to-End Data Flow

ClubLedger implements a unidirectional data flow (UDF) architecture adhering to modern Android development standards. Data seamlessly moves across 4 distinct tiers: from User Interaction down to SQLite Persistence, and back up through Reactive Streams.

```
 [ USER / TOUCH EVENT ]
          │
          ▼
 ┌────────────────────────────────────────────────────────┐
 │ 1. PRESENTATION LAYER (Jetpack Compose Frontend)       │
 │    • Composable Screens (Member & Admin Views)         │
 │    • Dynamic Dialogs, Sliders & Forms                  │
 │    • Event Dispatchers (e.g., onSubmitPayment)         │
 └────────────────────────┬───────────────────────────────┘
                          │ (User Intent / Method Invocation)
                          ▼
 ┌────────────────────────────────────────────────────────┐
 │ 2. CONTROLLER & STATE LAYER (ClubViewModel)            │
 │    • viewModelScope (Dispatchers.IO)                   │
 │    • Input Validation & Form Sanitization              │
 │    • StateFlow Holders & Snackbar Notifications        │
 └────────────────────────┬───────────────────────────────┘
                          │ (Domain Operations & Transactions)
                          ▼
 ┌────────────────────────────────────────────────────────┐
 │ 3. BUSINESS LOGIC & CALCULATION ENGINES (Backend)      │
 │    • Proportional Cost Allocation Formula Engine       │
 │    • Immutable Running Balance Ledger Calculator       │
 │    • Auto-Audit Trail Event Logger                     │
 └────────────────────────┬───────────────────────────────┘
                          │ (CRUD Operations & Queries)
                          ▼
 ┌────────────────────────────────────────────────────────┐
 │ 4. PERSISTENCE LAYER (Room Database / SQLite)          │
 │    • ClubDao Data Access Objects (@Transaction)        │
 │    • Typed SQLite Tables (Users, Teams, Ledgers, etc.) │
 │    • Foreign Key Integrity & Indexes                   │
 └────────────────────────┬───────────────────────────────┘
                          │
                          │ ◄── [ Reactive Flow<T> Streams ] ──┐
                          └────────────────────────────────────┘
                                           │
                                           ▼
 [ AUTOMATIC RECOMPOSITION ] ◄── StateFlow Emission ◄── Room Observer
```

### Detailed Step-by-Step Data Flow

#### Example Walkthrough: Member Submitting a Payment

1. **User Action (Frontend / Presentation Layer)**:
   - The user opens the "Submit Payment" dialog, chooses "Venmo/Zelle", enters `$50.00`, types reference memo `#TXN-9982`, and taps **"Submit Payment"**.
   - `MemberPaymentsScreen` invokes the callback `viewModel.submitPayment(...)`.

2. **ViewModel Processing (State Layer)**:
   - `ClubViewModel` intercepts the call, validates non-empty fields, extracts the active `currentUser.id`, and launches a coroutine in `viewModelScope` with `Dispatchers.IO`.

3. **Domain & Business Logic Processing (Backend Services Layer)**:
   - Constructs a typed `Payment` entity with `status = "PENDING"`.
   - Automatically generates an `AuditLog` entry detailing the payment submission for administrative tracking.

4. **Database Persistence (Persistence Layer)**:
   - `ClubDao.insertPayment(payment)` inserts the row into the `payments` SQLite table.
   - `ClubDao.insertAuditLog(auditLog)` logs the action into `audit_logs`.

5. **Reactive Loop & Recomposition**:
   - Room detects the table modification and automatically pushes updated query results through `Flow<List<Payment>>`.
   - The ViewModel's `StateFlow` emits the new list to the UI.
   - Compose observes the state via `collectAsState()` and instantly recomposes the payment list and admin approval badge counter without manual polling.

---

## ☁️ Firebase Cloud Persistence & Social Multi-User Authentication

ClubLedger includes **Firebase Cloud Firestore** and **Multi-Provider Cloud Authentication (Google, Apple, and Facebook)** to enable secure real-time multi-user synchronization across remote phones:

1. **Architecture & Topology**:
   - **Local Cache & Offline Mode**: Jetpack Room provides instantaneous offline resilience.
   - **Cloud Sync Engine (`FirestoreSyncManager`)**: Pushes local records to `/clubs/{clubId}/...` and attaches real-time snapshot listeners for `attendances` and `payments`.
   - **Multi-Provider Authentication (`FirebaseAuthManager`)**:
     - **Google Sign-In**: Powered by Jetpack `CredentialManager` and `GetSignInWithGoogleOption`.
     - **Sign in with Apple**: Powered by Firebase `OAuthProvider` with `apple.com` and custom scopes (`email`, `name`).
     - **Facebook Login**: Powered by Firebase `OAuthProvider` with `facebook.com` (`email`, `public_profile`).

2. **Connecting Remote Devices & Signing In**:
   - In the **Audit & Cloud Hub / Reports** tab, open the **Remote Multi-User & Cloud Sync** card.
   - Enter your team's unique **Shared Club Identifier** (e.g. `thunder-sports-club`).
   - Toggle **Firebase Real-Time Sync** ON.
   - Tap **Google**, **Apple**, or **Facebook** to link your verified social identity to audit records.
   - Tap **Push to Cloud** to broadcast local rosters and budgets, or listen for incoming updates from teammates in real-time.

---

## 🛠️ Architecture & Tech Stack

- **Language:** Kotlin 100%
- **UI Framework:** Jetpack Compose with Material Design 3 (M3)
- **Local Persistence:** Android Jetpack Room (SQLite) with Kotlin Coroutines & Flow
- **State Management:** MVVM (`ClubViewModel`, `StateFlow`, `collectAsStateWithLifecycle`)
- **Theme & Design:** Dynamic Material 3 theming with Emerald Green (`#00897B`), Navy (`#1E3A8A`), and Amber accents, edge-to-edge support, and responsive layouts.
- **Unit Testing:** JUnit 4 tests for mathematical cost allocations, running ledger calculations, and CSV generators.

---

## 📂 Project Structure

```
app/src/main/java/com/example/
├── MainActivity.kt                  # App navigation & top-level scaffold
├── data/
│   ├── AppDatabase.kt               # Room database setup & preloaded seed data
│   ├── dao/
│   │   └── ClubDao.kt               # Room DAOs for users, teams, attendances, budgets, invoices, ledger
│   └── entity/                      # Room data entities (User, Team, Attendance, Budget, Invoice, etc.)
├── ui/
│   ├── components/                  # Common dialogs, headers, and UI widgets
│   ├── screens/
│   │   ├── admin/                   # Admin Dashboard, Approvals, Budgets, Members, Reports
│   │   └── member/                  # Member Dashboard, Attendance, Payments, Invoices
│   ├── theme/                       # Color, Typography, and Theme definitions
│   └── viewmodel/
│       └── ClubViewModel.kt         # Reactive business logic & data operations
```

---

## 🚀 Building, Testing & Deployment

Comprehensive local setup, testing, and production deployment instructions are provided in [DEPLOYMENT.md](./DEPLOYMENT.md).

### Quick Commands:

```bash
# 1. Verify local development environment
./scripts/dev_setup.sh

# 2. Run unit & Robolectric tests
./scripts/run_tests.sh

# 3. Build debug APK
./gradlew assembleDebug

# 4. Build production Google Play App Bundle (AAB)
./scripts/build_release.sh aab

# 5. Build production Release APK
./scripts/build_release.sh apk
```

---

## 🤖 CI/CD Automation

A pre-configured **GitHub Actions CI/CD Pipeline** is located at `.github/workflows/android_ci_cd.yml` to automatically run tests, build debug APKs on PRs, and generate release bundles on main merges.
