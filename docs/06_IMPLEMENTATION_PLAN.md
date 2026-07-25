# 06_IMPLEMENTATION_PLAN.md

# ShareCutter - Implementation Plan

## מטרה

מסמך זה מפרק את הפרויקט לשלבים קטנים הניתנים לביצוע, בדיקה ו-Commit.

---

# שלב 0 - הקמת הפרויקט

- יצירת Repository
- יצירת Monorepo
- יצירת backend
- יצירת frontend
- Docker Compose עם PostgreSQL
- Flyway
- קובצי .env
- בדיקת Health

Git Branch:
```text
feat/project-bootstrap
```

---

# שלב 1 - Authentication

Backend:
- User Entity
- User Repository
- User Service
- BCrypt
- JWT
- Spring Security
- Register API
- Login API
- Me API

Frontend:
- Login
- Register
- Auth Guard
- JWT Interceptor

בדיקות:
- הרשמה
- התחברות
- משתמש ללא Token
- Token לא תקין

Branch:

```text
feat/authentication
```

---

# שלב 2 - Portfolio Management

Backend

- Portfolio Entity
- Holding Entity
- CRUD
- יצירה לפי סכום
- יצירה לפי אחזקות

Frontend

- Dashboard
- Portfolio List
- Create Portfolio
- Portfolio Details

בדיקות

- 10 מניות
- 100%
- שם תיק ייחודי

Branch

```text
feat/portfolio-management
```

---

# שלב 3 - Weekly Signals

Backend

- WeeklySignal
- SignalAllocation
- CRUD

Frontend

- Signal List
- Signal Form

בדיקות

- בדיוק 10 מניות
- 100%
- DRAFT בלבד לעריכה

Branch

```text
feat/weekly-signals
```

---

# שלב 4 - Rebalancing

Backend

- PortfolioCalculationService
- RebalancingService
- Preview
- Execute
- Snapshot
- Transactions

Frontend

- Preview Screen
- Execute Dialog

בדיקות

- BUY
- SELL
- Rollback
- Execute פעם אחת בלבד

Branch

```text
feat/rebalancing
```

---

# שלב 5 - History

Backend

- Transactions API
- Snapshot API
- Dashboard API

Frontend

- Transactions
- Snapshots

Branch

```text
feat/history
```

---

# שלב 6 - Polish

- Validation
- Error Pages
- Loading
- Responsive
- UI Polish
- Manual Testing
- Bug Fixes

Branch

```text
feat/polish
```

---

# Git Workflow

לכל שלב:

1. Create Branch
2. Implement
3. Test
4. Commit
5. Push
6. Pull Request
7. Merge

Commit לדוגמה:

```text
feat(auth): implement JWT authentication
```

---

# Definition of Done

כל שלב נחשב גמור כאשר:

- הקוד נבנה.
- אין שגיאות קומפילציה.
- הבדיקות עוברות.
- אין Warning קריטיים.
- Commit בוצע.
- Push בוצע.
- Pull Request אושר.

---

# סדר עבודה מומלץ

1. Bootstrap
2. Auth
3. Portfolio
4. Signals
5. Preview
6. Execute
7. History
8. Polish
9. Presentation

---

# Checklist

## Bootstrap
- [ ] Spring Boot
- [ ] Angular
- [ ] PostgreSQL
- [ ] Docker
- [ ] Flyway

## Auth
- [ ] Register
- [ ] Login
- [ ] JWT

## Portfolio
- [ ] By Amount
- [ ] By Holdings
- [ ] CRUD

## Signals
- [ ] CRUD
- [ ] Validation

## Rebalance
- [ ] Preview
- [ ] Execute

## History
- [ ] Transactions
- [ ] Snapshots

## UI
- [ ] Dashboard
- [ ] Responsive
- [ ] Polish

---

# הצעד הבא

סיימנו את שלב התכנון.

השלב הבא הוא יצירת הפרויקט בפועל והקמת סביבת העבודה לפי סדר המסמך הזה.
