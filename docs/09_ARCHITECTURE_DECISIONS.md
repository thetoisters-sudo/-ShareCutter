# 09_ARCHITECTURE_DECISIONS.md

# ShareCutter - Architecture Decision Records (ADR)

## מטרת המסמך

מסמך זה מרכז את ההחלטות הארכיטקטוניות המרכזיות של הפרויקט ואת הנימוקים לבחירתן.

---

# ADR-001 - Backend

## החלטה

Java 21 + Spring Boot

## חלופות

- ASP.NET
- Node.js
- Go

## נימוק

- מערכת בשכבות ברורה.
- אקוסיסטם עשיר.
- תמיכה מצוינת ב-JPA, Security ו-Validation.
- מתאים לפרויקט שניתן להרחיב בעתיד.

---

# ADR-002 - Frontend

## החלטה

Angular

## חלופות

- React
- Vue

## נימוק

- Framework מלא.
- Dependency Injection.
- Routing מובנה.
- Reactive Forms.
- ארגון קוד עקבי.

---

# ADR-003 - Database

## החלטה

PostgreSQL

## חלופות

- MySQL
- SQL Server

## נימוק

- יציב.
- חינמי.
- תמיכה מעולה ב-UUID, JSON ו-Numeric.
- מתאים לחישובים פיננסיים.

---

# ADR-004 - Database Migrations

## החלטה

Flyway

## לא נבחר

ddl-auto=create/update

## נימוק

- היסטוריית שינויים.
- גרסאות מסודרות.
- עקביות בין סביבות.

---

# ADR-005 - Identifiers

## החלטה

UUID

## לא נבחר

Long Auto Increment

## נימוק

- קשה לנחש מזהים.
- מתאים למיזוג נתונים עתידי.
- מאפשר יצירה בצד השרת ללא תלות במסד.

---

# ADR-006 - Money Calculations

## החלטה

BigDecimal + NUMERIC

## לא נבחר

double / float

## נימוק

דיוק פיננסי מלא ללא שגיאות עיגול מצטברות.

---

# ADR-007 - Authentication

## החלטה

JWT Access Token בלבד ב-MVP.

## עתידי

Refresh Token + HttpOnly Cookie.

## נימוק

פשטות מימוש תוך שמירה על אפשרות הרחבה.

---

# ADR-008 - Portfolio Rules

## החלטה

בדיוק 10 מניות פעילות בכל תיק.

## נימוק

זהו כלל עסקי מרכזי של ShareCutter ולכן נאכף ב-Service וב-API.

---

# ADR-009 - Rebalancing

## החלטה

Preview ו-Execute מופרדים.

## נימוק

המשתמש רואה את כל העסקאות לפני שהן נכתבות למסד.

---

# ADR-010 - History

## החלטה

Snapshots לפני ואחרי כל איזון.

## נימוק

מאפשר שחזור מלא של מצב התיק והשוואה לאורך זמן.

---

# ADR-011 - Architecture Style

Layered Architecture

Controller
↓
Service
↓
Repository
↓
Database

נבחרה בזכות פשטות, בדיקות קלות והפרדת אחריות.

---

# ADR-012 - Transactions

כל Execute ירוץ בתוך Transaction אחת.

במקרה של כשל:

Rollback מלא.

---

# ADR-013 - Security

בכל Endpoint תתבצע בדיקת Ownership.

אין לסמוך על מזהי משתמש המגיעים מה-Frontend.

---

# ADR-014 - API

REST בלבד.

JSON בלבד.

אין GraphQL ב-MVP.

---

# ADR-015 - UI

Material Design עם התאמות למערכת פיננסית.

---

# החלטות שנדחו

- Redis
- RabbitMQ
- Kafka
- Kubernetes
- Microservices
- Stock APIs בזמן אמת

הסיבה:

שמירה על MVP פשוט, יציב וברור.

---

# Review

כל ADR חדש יקבל מספר חדש ולא יערוך ADR קיים, אלא אם מדובר בתיקון טעות עובדתית.

---

# הצעד הבא

10_DEPLOYMENT_GUIDE.md

יכלול:

- Docker
- הפעלה מקומית
- משתני סביבה
- Build
- Packaging
- הפעלה על מחשב חדש
