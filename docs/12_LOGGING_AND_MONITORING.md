# 12_LOGGING_AND_MONITORING.md

# ShareCutter - Logging & Monitoring Strategy

## מטרת המסמך

להגדיר אסטרטגיה אחידה ללוגים, ניטור ואבחון תקלות.

---

# עקרונות

- כל בקשה תקבל Correlation ID.
- כל שגיאה תירשם פעם אחת בלבד.
- אין לרשום מידע רגיש.
- הלוגים חייבים לאפשר שחזור אירוע.

---

# רמות לוג

## TRACE
מידע מפורט מאוד (Development בלבד).

## DEBUG
מידע למפתחים.

## INFO
אירועים עסקיים:
- Login
- Create Portfolio
- Execute Rebalance

## WARN
קלט לא תקין או ניסיון פעולה חריג.

## ERROR
חריגות בלתי צפויות.

---

# מבנה לוג

Timestamp
Level
CorrelationId
UserId (אם קיים)
HTTP Method
Endpoint
Status Code
Execution Time
Message

דוגמה:

2026-08-01T10:15:22Z INFO [a8f1] user=42 POST /api/portfolios 201 184ms Portfolio created

---

# Correlation ID

- נוצר בתחילת כל Request.
- מוחזר גם ב-Response Header.
- מועבר בין שכבות המערכת.

---

# מדדי ביצועים

יש למדוד:

- זמן תגובה ממוצע
- זמן תגובה מקסימלי
- מספר בקשות לדקה
- שיעור שגיאות
- זמן Execute Rebalance

---

# Audit Events

יש לתעד:

- Login
- Register
- Create Portfolio
- Update Portfolio
- Execute Rebalance
- Access Denied

---

# מה לא לרשום

- Password
- JWT
- Authorization Header
- מידע אישי שאינו נדרש

---

# ניטור עתידי

MVP:
- לוגים מקומיים

עתידי:
- Grafana
- Prometheus
- OpenTelemetry
- ELK Stack

---

# Checklist

- Correlation ID קיים
- Logging עקבי
- אין מידע רגיש
- ERROR כולל Stack Trace בלוג בלבד
- INFO אינו רועש מדי

---

# הצעד הבא

13_SEQUENCE_DIAGRAMS.md
