# 05_FRONTEND_SCREENS.md

# ShareCutter

## מטרת המסמך

מסמך זה מגדיר את מסכי Angular של מערכת ShareCutter ואת זרימת המשתמש ביניהם.

---

# מפת ניווט

```text
Login
   │
Register
   │
Dashboard
   ├── Portfolio List
   ├── Create Portfolio
   ├── Portfolio Details
   │      ├── Holdings
   │      ├── Transactions
   │      ├── Snapshots
   │      └── Weekly Signals
   │              ├── Signal Details
   │              ├── Preview Rebalance
   │              └── Execute Rebalance
   └── Profile
```

---

# 1. Login

שדות:
- Email
- Password

כפתורים:
- Login
- Register

ולידציה:
- אימייל תקין
- סיסמה חובה

---

# 2. Register

שדות:
- Full Name
- Email
- Password
- Confirm Password

ולידציה:
- כל השדות חובה
- אימייל תקין
- סיסמה לפחות 8 תווים
- התאמת סיסמאות

---

# 3. Dashboard

יוצג:

- שם המשתמש
- מספר תיקים
- שווי כולל
- רווח כולל
- תשואה כוללת

טבלה:

- שם תיק
- שווי
- תשואה
- עדכון אחרון
- כניסה לתיק

כפתור:

Create Portfolio

---

# 4. Create Portfolio

בחירת סוג:

- By Amount
- By Holdings

שדות משותפים:

- Portfolio Name

אם By Amount:

- Investment Amount
- 10 מניות
- מחיר
- משקל

אם By Holdings:

- 10 מניות
- Quantity
- Average Buy Price
- Current Price

המערכת תציג Validation בזמן אמת.

---

# 5. Portfolio Details

כרטיס עליון:

- שם תיק
- שווי נוכחי
- שווי התחלתי
- רווח
- תשואה

Tabs:

- Holdings
- Signals
- Transactions
- Snapshots

---

# Holdings

טבלה:

- Symbol
- Quantity
- Average Buy Price
- Current Price
- Current Value
- Weight
- Unrealized Profit

---

# Weekly Signals

רשימת איתותים.

כפתורים:

- New Signal
- Open

---

# Create Signal

טופס:

- Date
- Title
- Description
- 10 מניות
- Target Weight
- Market Price

כפתור:

Calculate Preview

---

# Preview Rebalance

כרטיס סיכום:

- Portfolio Value
- Total Buy
- Total Sell

טבלה:

- Symbol
- Current Weight
- Target Weight
- Action
- Quantity
- Amount

כפתורים:

- Execute
- Cancel

---

# Transactions

טבלה:

- Date
- Symbol
- BUY/SELL
- Quantity
- Price
- Total
- Realized Profit

מסננים:

- Symbol
- BUY/SELL
- Date Range

---

# Snapshots

טבלה:

- Date
- Type
- Portfolio Value
- Return

כניסה לפרטי Snapshot.

---

# Profile

שדות:

- Name
- Email
- Created At
- Last Login

---

# רכיבי Angular

Shared Components:

- Navbar
- Sidebar
- PortfolioCard
- PortfolioTable
- HoldingTable
- SignalTable
- TransactionTable
- SnapshotTable
- LoadingSpinner
- ConfirmDialog
- ErrorBanner
- SuccessToast

---

# Guards

- AuthGuard

---

# Interceptors

- JwtInterceptor
- ErrorInterceptor

---

# צבעי MVP

Primary: כחול כהה

Success: ירוק

Warning: כתום

Danger: אדום

רקע: לבן

---

# Responsive

Desktop ראשון.

Tablet נתמך.

Mobile בסיסי בלבד.

---

# סדר מימוש

1. Login
2. Register
3. Dashboard
4. Portfolio List
5. Create Portfolio
6. Portfolio Details
7. Signals
8. Preview
9. Execute
10. Transactions
11. Snapshots
12. Profile

---

# הצעד הבא

06_IMPLEMENTATION_PLAN.md

יכלול את תוכנית העבודה המלאה, חלוקה למשימות Git, סדר הקמה, Docker, Spring Boot, Angular ובדיקות.
