# 17_GIT_WORKFLOW_GUIDE.md

# ShareCutter - Git Workflow Guide

## מטרה

להגדיר תהליך עבודה אחיד עם Git לכל אורך הפרויקט.

---

# Branch Strategy

## main

- תמיד יציב
- ניתן לפריסה

## develop (אופציונלי בעתיד)

- אינטגרציה בין פיצ'רים

## Feature Branches

דוגמאות:

```text
feature/authentication
feature/portfolio-management
feature/rebalance-engine
feature/dashboard
bugfix/login-validation
hotfix/jwt-expiration
docs/update-api
```

---

# Workflow

```text
main
  │
  ├── feature/*
  │        │
  │        ├── Commit קטן
  │        ├── Push
  │        └── Pull Request
  │
  └── Merge לאחר Review
```

---

# Commit Convention

פורמט:

```text
type(scope): description
```

דוגמאות:

```text
feat(auth): add JWT authentication
feat(portfolio): create portfolio service
fix(api): validate duplicate symbols
refactor(service): simplify allocation logic
docs(api): update API specification
test(auth): add login integration tests
build(docker): update compose file
```

---

# Pull Request Checklist

- קוד מתקמפל
- כל הבדיקות עוברות
- אין Secrets
- אין קבצים זמניים
- תיעוד עודכן במידת הצורך

---

# Code Review Checklist

- Naming ברור
- DTO בלבד ב-API
- Validation מלאה
- Logging מתאים
- Security נשמר
- Transactions במקומות הנכונים
- אין כפילות קוד

---

# Merge Policy

ניתן למזג רק כאשר:

- Review אושר
- CI עבר
- אין קונפליקטים

---

# Tags

```text
v0.1.0 MVP
v0.2.0 Signals
v0.3.0 Rebalancing
v1.0.0 First Production
```

---

# Release Flow

1. Feature Branch
2. Pull Request
3. Review
4. Merge
5. Tag
6. Release Notes

---

# Git Ignore

אין להעלות:

- .env
- target/
- node_modules/
- logs/
- *.log
- IDE files

---

# Recovery

## Undo last commit (לא נשלח)

```bash
git reset --soft HEAD~1
```

## ביטול שינויים בקובץ

```bash
git restore <file>
```

## עדכון מה-main

```bash
git fetch origin
git rebase origin/main
```

---

# Best Practices

- Commit קטן וממוקד.
- Push לעיתים קרובות.
- Branch אחד לכל משימה.
- Merge רק לאחר בדיקות.

---

# הצעד הבא

18_PERFORMANCE_AND_SCALABILITY.md
