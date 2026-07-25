# 10_CODING_STANDARDS.md

# ShareCutter Coding Standards

## מטרה

להגדיר סטנדרט אחיד לכתיבת הקוד ב-Backend וב-Frontend.

---

# Backend

## Java

- Java 21
- Spring Boot
- Maven Wrapper

## Package Structure

controller/
service/
repository/
entity/
dto/
mapper/
config/
security/
validation/
exception/

---

## Naming

Classes:
PascalCase

Methods:
camelCase

Fields:
camelCase

Constants:
UPPER_SNAKE_CASE

Packages:
lowercase

---

## Entities

- Entity אחת לכל טבלה.
- אין Business Logic בתוך Entity.
- Auditing דרך מחלקת בסיס.

---

## DTO

לעולם אין לחשוף Entity ישירות ל-API.

כל Endpoint יעבוד עם DTO.

---

## Mapping

MapStruct בלבד.

אין Mapping ידני בקוד אלא אם יש צורך מיוחד.

---

## Validation

Bean Validation:

@NotNull
@NotBlank
@Email
@Size
@Positive

ולידציה עסקית תתבצע ב-Service.

---

## Exceptions

GlobalExceptionHandler יחיד.

אין try/catch מיותר ב-Controller.

---

## Logging

SLF4J.

INFO:
אירועים עסקיים.

WARN:
קלט לא תקין.

ERROR:
חריגות.

אין לרשום סיסמאות או JWT.

---

## Transactions

רק Service יכיל @Transactional.

---

# Frontend

Angular Standalone Components.

Reactive Forms בלבד.

Services ללא UI Logic.

HTTP דרך ApiService.

---

# Folder Structure

core/
shared/
features/

---

# Components

Component קטן עם אחריות אחת.

---

# Styling

SCSS.

אין CSS Inline.

---

# TypeScript

strict=true

אין any ללא הצדקה.

---

# Git

Commit קטן.

Push לאחר כל שלב משמעותי.

Branch אחד לכל Feature.

---

# Code Review Checklist

- Naming תקין
- DTO בלבד
- Validation
- Tests
- Logging
- Security
- Formatting

---

# Definition of Clean Code

- קצר
- קריא
- ללא כפילות
- ללא Magic Numbers
- אחריות אחת למחלקה

---

# הצעד הבא

11_SECURITY_GUIDELINES.md
