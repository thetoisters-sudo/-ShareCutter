# 11_SECURITY_GUIDELINES.md

# ShareCutter Security Guidelines

## מטרה

להגדיר את עקרונות האבטחה של המערכת בהתאם לשיטות עבודה מודרניות.

---

# 1. Authentication

- JWT Access Token
- BCrypt לסיסמאות
- אין שמירת סיסמאות גלויות
- JWT יכיל מזהה משתמש בלבד ומידע מינימלי

---

# 2. Authorization

כל Endpoint פרטי יבדוק:

- Token תקין
- המשתמש מחובר
- Ownership של המשאב

לעולם אין לסמוך על userId המגיע מה-Frontend.

---

# 3. Input Validation

בכל קלט:

- Bean Validation
- Validation עסקי
- Sanitization
- Trim
- Normalization

---

# 4. Password Policy

- לפחות 8 תווים
- Hash באמצעות BCrypt
- אין החזרת סיסמה ב-API
- אין רישום סיסמאות ללוג

---

# 5. SQL Injection

הגישה למסד תהיה דרך Spring Data JPA בלבד.

אין בניית SQL באמצעות שרשור מחרוזות.

---

# 6. XSS

Angular מצמצם סיכונים כברירת מחדל.

אין שימוש ב-innerHTML ללא Sanitization.

---

# 7. CORS

ב-MVP:

לאפשר רק את כתובת ה-Frontend.

---

# 8. Error Handling

למשתמש:

הודעות כלליות.

ללוג:

פירוט מלא.

אין לחשוף Stack Trace ללקוח.

---

# 9. Logging

אין לרשום:

- Password
- JWT
- Authorization Header

ניתן לרשום:

- Request ID
- User ID
- Endpoint
- Status Code
- Execution Time

---

# 10. Rate Limiting

לא נדרש ב-MVP.

מתוכנן להרחבה עתידית.

---

# 11. Dependency Management

- עדכון ספריות באופן קבוע
- תיקון פרצות אבטחה
- בדיקת CVEs לפני Release

---

# 12. Headers

מומלץ להגדיר:

- X-Content-Type-Options
- X-Frame-Options
- Referrer-Policy
- Content-Security-Policy
- Strict-Transport-Security (בסביבת HTTPS)

---

# 13. HTTPS

Production:

HTTPS בלבד.

Development:

HTTP מקומי.

---

# 14. Audit

יש לתעד:

- Login
- Logout (עתידי)
- Create Portfolio
- Execute Signal
- שגיאות אבטחה

---

# 15. OWASP Checklist

- Broken Access Control
- Cryptographic Failures
- Injection
- Insecure Design
- Security Misconfiguration
- Vulnerable Components
- Authentication Failures
- Data Integrity
- Logging & Monitoring

כל סעיף ייבדק לפני Release.

---

# 16. Security Review Checklist

- DTO בלבד
- Ownership Check
- Validation
- Transactions
- Logging בטוח
- JWT תקין
- אין מידע רגיש בתגובה

---

# Anti-Patterns

אין לבצע:

- Trust ל-userId מהלקוח
- חשיפת Entity ישירות
- שימוש ב-double לכסף
- החזרת Stack Trace ללקוח
- SQL דינמי משרשור מחרוזות

---

# הצעד הבא

12_LOGGING_AND_MONITORING.md
