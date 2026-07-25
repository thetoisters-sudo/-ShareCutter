# 16_DEPLOYMENT_GUIDE.md

# ShareCutter - Deployment Guide

## מטרה

לאפשר הקמה מלאה של הפרויקט על מחשב חדש בצורה עקבית.

---

# דרישות מוקדמות

- Git
- Docker Desktop
- Java 21 (JDK)
- Maven Wrapper (כלול בפרויקט)
- Node.js LTS
- Angular CLI
- PostgreSQL (דרך Docker)
- VS Code או IntelliJ IDEA

---

# מבנה הפרויקט

```text
ShareCutter/
├── backend/
├── frontend/
├── docker/
├── docs/
├── .env.example
├── docker-compose.yml
└── README.md
```

---

# שלב 1 - שכפול הפרויקט

```bash
git clone <repository-url>
cd ShareCutter
```

---

# שלב 2 - קובץ סביבה

העתק:

```text
.env.example
```

אל:

```text
.env
```

ומלא את הערכים המתאימים.

---

# משתני סביבה עיקריים

```text
DB_HOST=postgres
DB_PORT=5432
DB_NAME=sharecutter
DB_USER=sharecutter
DB_PASSWORD=change-me

JWT_SECRET=replace-with-long-random-secret
JWT_EXPIRATION_MINUTES=60

SPRING_PROFILES_ACTIVE=local
```

---

# שלב 3 - הפעלת Docker

```bash
docker compose up -d
```

השירותים הצפויים:

- PostgreSQL
- pgAdmin (אופציונלי)

---

# שלב 4 - Backend

```bash
cd backend
./mvnw spring-boot:run
```

או ב-Windows:

```powershell
mvnw.cmd spring-boot:run
```

---

# שלב 5 - Frontend

```bash
cd frontend
npm install
ng serve
```

---

# Flyway

בעת עליית השרת:

- המיגרציות ירוצו אוטומטית.
- אין להשתמש ב-ddl-auto=create.

---

# בדיקות תקינות

Backend:

- GET /actuator/health
- GET /api/health

Frontend:

- Login Page נטענת
- Dashboard לאחר התחברות

Database:

- כל הטבלאות נוצרו
- Flyway Schema History קיימת

---

# Build

Backend:

```bash
./mvnw clean package
```

Frontend:

```bash
ng build
```

---

# Release Checklist

- כל הבדיקות עברו
- Flyway מעודכן
- אין Secrets בקוד
- README מעודכן
- Docker Compose תקין

---

# Troubleshooting

## Port in use

בדוק אילו תהליכים מאזינים לפורט ושנה את המיפוי ב-docker-compose.

## Flyway failed

בדוק את סדר המיגרציות ואת טבלת flyway_schema_history.

## Angular build failed

הרץ:

```bash
npm install
```

ולאחר מכן:

```bash
ng build
```

---

# הצעד הבא

17_GIT_WORKFLOW_GUIDE.md
