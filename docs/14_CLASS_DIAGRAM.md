# 14_CLASS_DIAGRAM.md

# ShareCutter - Class Diagram

## מטרת המסמך

להגדיר את מבנה המחלקות המרכזיות במערכת, תחומי האחריות שלהן והתלויות ביניהן.

---

# שכבות המערכת

```text
Angular
    │
REST Controllers
    │
Services
    │
Repositories
    │
PostgreSQL
```

---

# UML (Mermaid)

```mermaid
classDiagram

class User {
  +UUID id
  +String email
  +String passwordHash
}

class Portfolio {
  +UUID id
  +String name
}

class Holding {
  +UUID id
  +String symbol
  +BigDecimal quantity
  +BigDecimal averagePrice
}

class WeeklySignal {
  +UUID id
  +LocalDate week
  +boolean executed
}

class SignalAllocation {
  +String symbol
  +BigDecimal targetWeight
}

class TradeTransaction {
  +UUID id
  +TradeType type
  +BigDecimal quantity
}

class PortfolioSnapshot {
  +UUID id
  +Instant createdAt
}

User "1" --> "*" Portfolio
Portfolio "1" --> "*" Holding
Portfolio "1" --> "*" WeeklySignal
WeeklySignal "1" --> "*" SignalAllocation
Portfolio "1" --> "*" TradeTransaction
Portfolio "1" --> "*" PortfolioSnapshot
```

---

# Controllers

- AuthController
- UserController
- PortfolioController
- SignalController
- RebalanceController
- DashboardController

אחריות:
- קבלת HTTP Requests
- החזרת Responses
- ללא Business Logic

---

# Services

- AuthService
- UserService
- PortfolioService
- SignalService
- RebalanceService
- DashboardService
- SnapshotService

אחריות:
- כל הלוגיקה העסקית
- Transactions
- Validation עסקי

---

# Repositories

- UserRepository
- PortfolioRepository
- HoldingRepository
- WeeklySignalRepository
- TradeTransactionRepository
- SnapshotRepository

אחריות:
- גישה למסד בלבד

---

# DTOs

Request:
- RegisterRequest
- LoginRequest
- CreatePortfolioRequest
- CreateSignalRequest

Response:
- UserResponse
- LoginResponse
- PortfolioResponse
- DashboardResponse
- ErrorResponse

---

# Utilities

- JwtService
- AllocationCalculator
- RebalanceCalculator
- MapStruct Mappers

---

# Dependency Rules

- Controller → Service בלבד
- Service → Repository בלבד
- Repository → Database בלבד
- Entity אינה מכירה Controller
- DTO אינו Entity

---

# Clean Architecture Rules

- אחריות אחת לכל מחלקה.
- אין לוגיקה עסקית ב-Controller.
- אין SQL מחוץ ל-Repository.
- אין Entity ב-API.

---

# Checklist

- [ ] Controllers דקים
- [ ] Services ללא UI
- [ ] Repository ללא Business Logic
- [ ] DTO לכל Endpoint
- [ ] MapStruct לכל Mapping

---

# הצעד הבא

15_ER_DIAGRAM.md
