# 08_TEST_PLAN.md

# ShareCutter - Test Plan

## מטרה

להגדיר אסטרטגיית בדיקות מלאה לפני כתיבת הקוד.

---

# פירמידת הבדיקות

- Unit Tests
- Integration Tests
- API Tests
- UI Tests
- Manual Acceptance Tests

---

# יעדי איכות

- Code Coverage מינימלי: 80%
- כל Endpoint נבדק
- כל Service נבדק
- כל Validation נבדק
- אין Regression לפני Merge

---

# Unit Tests

## Authentication

- Register
- Login
- JWT Generation
- JWT Validation
- BCrypt

## Portfolio

- Create By Amount
- Create By Holdings
- Weight = 100%
- Exactly 10 Stocks
- Duplicate Symbols

## Rebalancing

- BUY calculation
- SELL calculation
- Average Cost
- Portfolio Return
- Snapshot Creation
- Transaction Creation

---

# Integration Tests

- Spring Boot + PostgreSQL
- Flyway Migration
- Repository Layer
- Transaction Rollback

---

# API Tests

לכל Endpoint:

- Success
- Validation Error
- Unauthorized
- Forbidden / Ownership
- Not Found
- Conflict

---

# Frontend Tests

- Login Flow
- Register Flow
- Dashboard
- Portfolio Creation
- Signal Creation
- Preview
- Execute

---

# Manual Acceptance

## תרחיש 1

משתמש חדש

1. Register
2. Login
3. Create Portfolio
4. Preview
5. Execute
6. View Transactions

צפוי:
ללא שגיאות.

---

## תרחיש 2

יצירת תיק עם 9 מניות

צפוי:

400 Validation Error

---

## תרחיש 3

משקלים 95%

צפוי:

Validation Error

---

## תרחיש 4

Execute פעמיים

צפוי:

409 SIGNAL_ALREADY_EXECUTED

---

## תרחיש 5

גישה לתיק של משתמש אחר

צפוי:

404

---

# Security Tests

- SQL Injection
- JWT Tampering
- Missing Token
- Expired Token
- XSS בסיסי
- CSRF (עתידי)

---

# Performance Goals

- Login < 500ms
- Dashboard < 1s
- Preview < 2s
- Execute < 3s

---

# Definition of Ready

כל Feature מתחיל רק לאחר:

- API מוגדר
- DB מוגדר
- UI מוגדר

---

# Definition of Done

- Unit Tests עוברים
- Integration Tests עוברים
- Manual Test עבר
- PR אושר

---

# הצעד הבא

09_ARCHITECTURE_DECISIONS.md
