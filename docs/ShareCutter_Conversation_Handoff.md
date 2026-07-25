# ShareCutter - Handoff Summary

## מטרת הקובץ
להמשיך את הפרויקט בשיחה חדשה בדיוק מהנקודה שבה נעצרנו.

## הפרויקט
ShareCutter

נתיב עבודה:
C:\Users\אורטל\Desktop\ShareCutter

## Stack

Backend:
- Java 21
- Spring Boot
- Spring Security
- Spring Data JPA
- Flyway

Frontend:
- Angular
- TypeScript
- SCSS

Database:
- PostgreSQL

DevOps:
- Docker
- Docker Compose

Authentication:
- JWT
- BCrypt

## החלטות עסקיות

- משתמש אחד יכול להחזיק מספר תיקים.
- כל תיק כולל בדיוק 10 מניות פעילות.
- USD בלבד.
- Fractional Shares.
- יצירת תיק לפי סכום השקעה או לפי אחזקות קיימות.
- Preview אינו כותב למסד.
- Execute מבצע איזון אמיתי פעם אחת בלבד לכל Signal.
- Snapshot לפני ואחרי Execute.

## עקרונות ארכיטקטורה

- Layered Architecture.
- Controller -> Service -> Repository -> Database.
- DTO בלבד ב-API.
- MapStruct למיפויים.
- BigDecimal לכל חישוב כספי.
- UUID לכל הישויות.
- Validation בכל שכבה.
- Global Exception Handler.
- Ownership Validation.

## אבטחה

- JWT
- BCrypt
- Bean Validation
- אין Password/JWT בלוגים
- אין Stack Trace ללקוח

## מסמכים שהושלמו

00_PROJECT_DEFINITION.md
01_FUNCTIONAL_REQUIREMENTS.md
02_SYSTEM_ARCHITECTURE.md
03_DATABASE_DESIGN.md
04_API_SPECIFICATION.md
05_FRONTEND_SCREENS.md
06_IMPLEMENTATION_PLAN.md
07_UI_UX_GUIDELINES.md
08_TEST_PLAN.md
09_ARCHITECTURE_DECISIONS.md
10_CODING_STANDARDS.md
11_SECURITY_GUIDELINES.md
12_LOGGING_AND_MONITORING.md
13_SEQUENCE_DIAGRAMS.md
14_CLASS_DIAGRAM.md
15_ER_DIAGRAM.md
16_DEPLOYMENT_GUIDE.md
17_GIT_WORKFLOW_GUIDE.md
18_PERFORMANCE_AND_SCALABILITY.md
19_FINAL_ARCHITECTURE_SUMMARY.md

סה״כ: 19 מסמכים.

## מצב הפרויקט

שלב התכנון הושלם במלואו.

אין צורך במסמכי תכנון נוספים.

## תוכנית המימוש

1. יצירת Repository.
2. Spring Boot Bootstrap.
3. Angular Bootstrap.
4. Docker Compose.
5. PostgreSQL.
6. Flyway.
7. JWT Authentication.
8. Users.
9. Portfolio Management.
10. Holdings.
11. Weekly Signals.
12. Preview.
13. Execute.
14. Dashboard.
15. Polish.
16. Release ראשון.

## דרך העבודה

- לעבוד שלב אחר שלב.
- להסביר כל פעולה.
- Commit קטן לאחר כל שלב משמעותי.
- Push לאחר כל שלב משמעותי.
- להסביר תמיד מה הצעד הבא.
- לא לשנות החלטות תכנון ללא הצדקה.

## הנחיה לשיחה הבאה

להתחיל מיד בבניית הפרויקט לפי המסמכים, ללא חזרה לשלב התכנון.
