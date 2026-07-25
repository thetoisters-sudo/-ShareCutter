# 19_FINAL_ARCHITECTURE_SUMMARY.md

# ShareCutter
## Final Architecture Summary

> מסמך הכניסה הרשמי לפרויקט.

---

# חזון

ShareCutter היא מערכת לניהול תיקי השקעות בני 10 מניות עם יצירת איתותים שבועיים, Preview לפני איזון מלא, ותיעוד היסטורי של כל שינוי.

---

# מטרות

- MVP יציב ופשוט.
- קוד נקי וניתן להרחבה.
- ארכיטקטורת Enterprise.
- תיעוד מלא לפני פיתוח.

---

# מחסנית הטכנולוגיות

## Backend
- Java 21
- Spring Boot
- Spring Security
- Spring Data JPA
- Flyway

## Frontend
- Angular
- TypeScript
- SCSS

## Database
- PostgreSQL

## DevOps
- Docker
- Docker Compose

---

# ארכיטקטורה

Angular
↓
REST API
↓
Controllers
↓
Services
↓
Repositories
↓
PostgreSQL

---

# מודולים

- Authentication
- Users
- Portfolios
- Holdings
- Weekly Signals
- Rebalancing
- Transactions
- Snapshots
- Dashboard

---

# כללי מערכת

- בדיוק 10 מניות פעילות.
- Fractional Shares.
- USD בלבד.
- BigDecimal לכל חישוב כספי.
- Preview אינו משנה נתונים.
- Execute מתבצע פעם אחת לכל Signal.
- Snapshot לפני ואחרי Execute.

---

# אבטחה

- JWT
- BCrypt
- Ownership Validation
- DTO בלבד
- Validation בכל שכבה
- Global Exception Handler

---

# ביצועים

- Dashboard < 1s
- Preview < 2s
- Execute < 3s

---

# Git

- Branch לכל Feature
- Commit קטן
- Pull Request
- Code Review
- Merge ל-main

---

# Deployment

- Docker Compose
- Flyway
- .env
- Maven Wrapper
- Angular CLI

---

# רשימת המסמכים

00 Project Definition
01 Functional Requirements
02 System Architecture
03 Database Design
04 API Specification
05 Frontend Screens
06 Implementation Plan
07 UI/UX Guidelines
08 Test Plan
09 Architecture Decisions
10 Coding Standards
11 Security Guidelines
12 Logging & Monitoring
13 Sequence Diagrams
14 Class Diagram
15 ER Diagram
16 Deployment Guide
17 Git Workflow Guide
18 Performance & Scalability
19 Final Architecture Summary

---

# Definition of Ready

- כל המסמכים אושרו.
- סביבת עבודה מוכנה.
- Git Repository מוכן.

---

# Definition of Done

- קוד.
- בדיקות.
- תיעוד.
- Build.
- Review.
- Release.

---

# סדר המימוש

1. Bootstrap
2. Authentication
3. Users
4. Portfolios
5. Holdings
6. Signals
7. Preview
8. Execute
9. Dashboard
10. Polish

---

# הרחבות עתידיות

- מחירי מניות בזמן אמת
- Redis
- Notifications
- Mobile App
- Multi-Currency
- Multi-Broker
- AI Recommendations

---

# סיכום

שלב התכנון הושלם.

מכאן מתחיל שלב המימוש.

כל החלטה מרכזית מתועדת, כל שכבה הוגדרה, וכללי העבודה נקבעו מראש כדי לצמצם אי-ודאות במהלך הפיתוח.
