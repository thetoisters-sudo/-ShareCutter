# 02_SYSTEM_ARCHITECTURE.md

## שם הפרויקט

**ShareCutter**

## נתיב הפרויקט המקומי

```text
C:\Users\אורטל\Desktop\ShareCutter
```

---

## 1. מטרת המסמך

מסמך זה מגדיר את הארכיטקטורה הטכנית של ShareCutter: מבנה הפרויקט, שכבות המערכת, גרסאות העבודה, אבטחה, מסד הנתונים, מנוע החישובים והכנה לחיבור עתידי ל-API של שוק ההון.

המערכת תיבנה כ-Monorepo אחד המכיל Backend, Frontend, מסמכים וקובצי תשתית.

---

## 2. טכנולוגיות וגרסאות

### Backend

```text
Java: 21 LTS
Spring Boot: 4.1.x
Build Tool: Maven Wrapper
API: REST + JSON
```

### Frontend

```text
Angular: 21.x
Node.js: 24.x
Package Manager: npm
Angular Architecture: Standalone Components
```

### Database and Infrastructure

```text
PostgreSQL
Docker Desktop
Docker Compose
Flyway
Git
GitHub
Visual Studio Code
```

### הסיבה לבחירה

- Java 21 היא גרסת LTS יציבה.
- Spring Boot 4.1 תומך ב-Java 17 עד Java 26, ולכן Java 21 מתאים.
- Angular 21 תומך ב-Node.js 24.
- לא נבחר Angular 22 כדי לא לחייב Node.js 24.15 ומעלה סמוך להגשה.
- Maven Wrapper ימנע תלות בהתקנת Maven גלובלית.

---

## 3. מבנה התיקיות הראשי

```text
ShareCutter/
├── backend/
├── frontend/
├── docs/
├── compose.yml
├── .env
├── .env.example
├── .gitignore
└── README.md
```

### docs

```text
docs/
├── 00_PROJECT_DEFINITION.md
├── 01_FUNCTIONAL_REQUIREMENTS.md
├── 02_SYSTEM_ARCHITECTURE.md
├── 03_DATABASE_DESIGN.md
├── 04_API_SPECIFICATION.md
├── 05_FRONTEND_SCREENS.md
└── 06_IMPLEMENTATION_PLAN.md
```

---

## 4. מבנה Backend

```text
backend/
├── pom.xml
├── mvnw
├── mvnw.cmd
└── src/
    ├── main/
    │   ├── java/com/sharecutter/
    │   │   ├── ShareCutterApplication.java
    │   │   ├── config/
    │   │   ├── controller/
    │   │   ├── dto/
    │   │   ├── entity/
    │   │   ├── exception/
    │   │   ├── mapper/
    │   │   ├── repository/
    │   │   ├── security/
    │   │   ├── service/
    │   │   └── validation/
    │   └── resources/
    │       ├── application.yml
    │       ├── application-local.yml
    │       └── db/migration/
    └── test/java/com/sharecutter/
```

### שכבות Backend

```text
HTTP Request
    ↓
Controller
    ↓
Service
    ↓
Repository
    ↓
PostgreSQL
```

#### Controller

- מקבל בקשות HTTP.
- מבצע Validation על DTO.
- קורא ל-Service.
- מחזיר Response DTO.
- אינו מבצע חישובים עסקיים.
- אינו ניגש ישירות ל-Repository.

#### Service

- מכיל לוגיקה עסקית.
- בודק בעלות על משאבים.
- מנהל Transactions.
- מפעיל את מנגנון האיזון.
- מחשב תשואות ורווחים.

#### Repository

- משתמש ב-Spring Data JPA.
- מבצע גישה למסד הנתונים.
- כולל שאילתות לפי מזהה משתמש ובעלות.

#### Entity

- מייצגת טבלה במסד הנתונים.
- לא מוחזרת ישירות ל-Frontend.

#### DTO

- Request DTO.
- Response DTO.
- Preview DTO.
- Summary DTO.

#### Mapper

המיפוי ייכתב ידנית ב-MVP כדי להימנע מספריות נוספות.

---

## 5. חבילות Backend מרכזיות

```text
config/
controller/
dto/auth/
dto/user/
dto/portfolio/
dto/signal/
dto/transaction/
entity/
exception/
mapper/
repository/
security/
service/
validation/
```

### שירותים עיקריים

```text
AuthService
UserService
PortfolioService
PortfolioCalculationService
SignalService
RebalancingService
TransactionService
StockPriceService
ManualStockPriceService
```

---

## 6. מבנה Frontend

```text
frontend/
├── angular.json
├── package.json
├── proxy.conf.json
└── src/
    ├── app/
    │   ├── core/
    │   │   ├── guards/
    │   │   ├── interceptors/
    │   │   ├── models/
    │   │   └── services/
    │   ├── shared/
    │   │   ├── components/
    │   │   ├── validators/
    │   │   └── utils/
    │   ├── features/
    │   │   ├── auth/
    │   │   ├── dashboard/
    │   │   ├── portfolios/
    │   │   ├── signals/
    │   │   └── transactions/
    │   ├── app.config.ts
    │   └── app.routes.ts
    ├── environments/
    ├── styles.css
    └── main.ts
```

### החלטות Angular

- Standalone Components.
- Reactive Forms.
- HttpClient.
- Route Guards.
- Functional Interceptors.
- Lazy Loading לפי Feature, אם הזמן יאפשר.
- CSS רגיל ב-MVP, ללא ספריית UI חובה.

---

## 7. מסלולי Frontend

```text
/login
/register
/dashboard
/portfolios/new
/portfolios/:portfolioId
/portfolios/:portfolioId/edit
/portfolios/:portfolioId/signals
/portfolios/:portfolioId/signals/new
/portfolios/:portfolioId/signals/:signalId
/portfolios/:portfolioId/transactions
/profile
```

כל המסלולים מלבד `/login` ו-`/register` יהיו מוגנים באמצעות AuthGuard.

---

## 8. כתובות מקומיות

```text
Angular:
http://localhost:4200
```

```text
Spring Boot:
http://localhost:8080
```

```text
PostgreSQL:
localhost:5432
```

### API Base Path

```text
/api/v1
```

### Angular Proxy

הקובץ:

```text
frontend/proxy.conf.json
```

יעביר כל בקשה ל-`/api` אל:

```text
http://localhost:8080
```

---

## 9. Authentication and Security

### הרשמה

```text
Angular
→ POST /api/v1/auth/register
→ BCrypt
→ PostgreSQL
```

### התחברות

```text
Angular
→ POST /api/v1/auth/login
→ אימות משתמש
→ יצירת JWT
→ החזרת Access Token
```

### שליחת Token

```http
Authorization: Bearer <token>
```

### כללי אבטחה

- המשתמש מזוהה מתוך ה-JWT.
- אין לקבל `userId` מה-Frontend לצורך בעלות.
- כל תיק, איתות ועסקה ייבדקו מול המשתמש המחובר.
- סיסמאות יישמרו עם BCrypt.
- Entity של משתמש לא תחזיר `passwordHash`.
- Stack Trace לא יוחזר ללקוח.

### JWT ב-MVP

```text
Access Token בלבד
תוקף: 24 שעות
שמירה ב-localStorage
```

Refresh Token ו-HttpOnly Cookie יישמרו כהרחבה עתידית.

---

## 10. PostgreSQL ו-Flyway

PostgreSQL ירוץ ב-Docker Compose.

Spring Boot ו-Angular ירוצו מקומית לצורך Debug פשוט.

### JPA

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```

### Migration

```text
backend/src/main/resources/db/migration/
└── V1__create_initial_schema.sql
```

לא נשתמש ב-`ddl-auto=update`, מפני שהוא משנה Schema ללא היסטוריה מסודרת.

---

## 11. ישויות ליבה

```text
User
Portfolio
Holding
WeeklySignal
SignalAllocation
TradeTransaction
PortfolioSnapshot
StockPriceEntry
```

הקשרים והשדות המלאים יוגדרו במסמך:

```text
03_DATABASE_DESIGN.md
```

---

## 12. דיוק כספי

ב-Backend יש להשתמש ב:

```java
BigDecimal
```

אין להשתמש ב:

```java
float
double
```

עבור כסף, כמויות, משקלים או תשואות.

### Scale מוצע

```text
מחירים וסכומים: 4 ספרות אחרי הנקודה
כמויות מניות: 8 ספרות אחרי הנקודה
משקלים: 4 ספרות אחרי הנקודה
תשואות: 4 ספרות אחרי הנקודה
```

### עיגול

```java
RoundingMode.HALF_UP
```

אין לעגל באמצע חישוב אלא רק כאשר נדרש לחלק או להציג תוצאה.

---

## 13. מנוע האיזון

השירות:

```text
RebalancingService
```

יקבל:

- תיק נוכחי.
- אחזקות נוכחיות.
- איתות חדש.
- משקלי יעד.
- מחירים שהוזנו ידנית.

ויחזיר:

```text
RebalancePreview
```

### תהליך דו-שלבי

```text
1. Preview
2. Execute
```

#### Preview

- אינו משנה מסד נתונים.
- מחשב קניות ומכירות.
- מציג כמויות לפני ואחרי.
- מציג משקל נוכחי ומשקל יעד.

#### Execute

- דורש אישור משתמש.
- ירוץ בתוך Transaction אחת.
- יוצר רשומות BUY ו-SELL.
- מעדכן אחזקות.
- שומר Snapshot.
- משנה את סטטוס האיתות ל-EXECUTED.
- מבצע Rollback מלא במקרה של כשל.

---

## 14. שירות חישובי התיק

```text
PortfolioCalculationService
```

יהיה אחראי על:

- שווי אחזקה.
- שווי תיק.
- משקל בפועל.
- מחיר קנייה ממוצע.
- רווח ממומש.
- רווח לא ממומש.
- תשואה למניה.
- תשואה לתיק.

החישובים לא יתפזרו בין Controllers או Entities.

---

## 15. מקור מחירי מניות

תוגדר הפשטה:

```java
public interface StockPriceService
```

### MVP

```text
ManualStockPriceService
```

המחירים מתקבלים מהמשתמש ונשמרים עם תאריך.

### הרחבה עתידית

```text
ExternalStockPriceService
```

השירות יוכל להתחבר לספק כגון Alpha Vantage, Finnhub, Twelve Data או ספק אחר.

שאר המערכת תעבוד מול `StockPriceService`, ולא תהיה תלויה בספק מסוים.

---

## 16. תגובות שגיאה

מבנה אחיד:

```json
{
  "timestamp": "2026-07-24T12:00:00Z",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Portfolio weights must total 100%",
  "path": "/api/v1/portfolios",
  "fieldErrors": []
}
```

### קודי HTTP

```text
200 OK
201 Created
204 No Content
400 Bad Request
401 Unauthorized
403 Forbidden
404 Not Found
409 Conflict
500 Internal Server Error
```

---

## 17. Docker Compose

ב-MVP `compose.yml` יכיל PostgreSQL בלבד:

```yaml
services:
  postgres:
    image: postgres:17
    container_name: sharecutter-postgres
    environment:
      POSTGRES_DB: ${POSTGRES_DB}
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    ports:
      - "${POSTGRES_PORT}:5432"
    volumes:
      - sharecutter-postgres-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER} -d ${POSTGRES_DB}"]
      interval: 5s
      timeout: 5s
      retries: 10

volumes:
  sharecutter-postgres-data:
```

---

## 18. משתני סביבה

### `.env.example`

```text
POSTGRES_DB=sharecutter
POSTGRES_USER=sharecutter
POSTGRES_PASSWORD=change-me
POSTGRES_PORT=5432

JWT_SECRET=replace-with-a-long-random-secret
JWT_EXPIRATION_HOURS=24
```

הקובץ `.env` לא ייכנס ל-Git.

---

## 19. Git ו-GitHub

שם Repository:

```text
sharecutter
```

Branch ראשי:

```text
main
```

Branches לפי שלבים:

```text
docs/project-foundation
feat/backend-foundation
feat/authentication
feat/portfolio-management
feat/weekly-signals
feat/rebalancing
feat/angular-frontend
```

בגלל לוח הזמנים, נשתמש ב-Branches קצרים וב-Commits קטנים לאחר כל שלב שעבר בדיקה.

---

## 20. בדיקות

### Backend

- JUnit 5.
- Spring Boot Test.
- MockMvc.
- Mockito.
- Testcontainers PostgreSQL רק אם הזמן יאפשר.

בדיקות חובה:

- הרשמה והתחברות.
- אימייל כפול.
- בידוד בין משתמשים.
- יצירת תיק בשתי השיטות.
- בדיוק 10 מניות.
- משקל כולל 100%.
- Preview של איזון.
- Execute של איזון.
- מניעת ביצוע כפול.

### Frontend

- בדיקות ידניות מתועדות לכל מסך.
- בדיקות אוטומטיות בעדיפות שנייה בשל זמן ההגשה.

---

## 21. עקרונות MVP

- לא מוסיפים Microservices.
- לא מוסיפים Redis.
- לא מוסיפים RabbitMQ.
- לא מוסיפים Kubernetes.
- לא מחברים API בורסה בשלב הראשון.
- לא משתמשים בספריית UI כבדה ללא צורך.
- כל Feature חייב לעבור בדיקה לפני מעבר לשלב הבא.
- המטרה היא פרויקט עובד ומוצג היטב, לא מוזיאון לטכנולוגיות שאיש לא הספיק לבדוק.

---

## 22. הצעד הבא

המסמך הבא:

```text
03_DATABASE_DESIGN.md
```

יכלול:

- טבלאות.
- שדות.
- טיפוסי PostgreSQL.
- Primary Keys.
- Foreign Keys.
- Indexes.
- Constraints.
- קשרים בין משתמשים, תיקים, אחזקות, איתותים ועסקאות.
