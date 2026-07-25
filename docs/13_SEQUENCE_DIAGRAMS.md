# 13_SEQUENCE_DIAGRAMS.md

# ShareCutter - Sequence Diagrams

## מטרת המסמך

להציג את זרימת הבקשות המרכזיות במערכת בין ה-Frontend, שכבות ה-Backend ומסד הנתונים.

התרשימים נכתבו ב-Mermaid כדי שניתן יהיה להציג אותם ישירות ב-GitHub, ב-VS Code ובכל עורך Markdown תומך.

---

# רכיבים מרכזיים

- Angular Client
- REST Controller
- Service Layer
- Repository Layer
- PostgreSQL
- JWT Security Filter
- Password Encoder
- Mapper
- Transaction Manager

---

# 1. Register User

```mermaid
sequenceDiagram
    actor User
    participant UI as Angular Register Page
    participant API as AuthController
    participant Service as AuthService
    participant Repo as UserRepository
    participant Encoder as PasswordEncoder
    participant DB as PostgreSQL

    User->>UI: מזין פרטי הרשמה
    UI->>API: POST /api/auth/register
    API->>Service: register(request)

    Service->>Repo: existsByEmail(email)
    Repo->>DB: SELECT user by email
    DB-->>Repo: result
    Repo-->>Service: exists / not exists

    alt Email already exists
        Service-->>API: throw UserAlreadyExistsException
        API-->>UI: 409 Conflict
        UI-->>User: הודעת שגיאה
    else Email available
        Service->>Encoder: encode(password)
        Encoder-->>Service: passwordHash
        Service->>Repo: save(user)
        Repo->>DB: INSERT users
        DB-->>Repo: saved user
        Repo-->>Service: user entity
        Service-->>API: UserResponse
        API-->>UI: 201 Created
        UI-->>User: הרשמה הושלמה
    end
```

## כללי מימוש

- אימייל מנורמל לפני הבדיקה.
- אין לשמור סיסמה גולמית.
- אין להחזיר passwordHash בתגובה.
- כפילות אימייל מוחזרת כ-409.

---

# 2. Login

```mermaid
sequenceDiagram
    actor User
    participant UI as Angular Login Page
    participant API as AuthController
    participant Service as AuthService
    participant Repo as UserRepository
    participant Encoder as PasswordEncoder
    participant JWT as JwtService
    participant DB as PostgreSQL

    User->>UI: מזין אימייל וסיסמה
    UI->>API: POST /api/auth/login
    API->>Service: login(request)

    Service->>Repo: findByEmail(email)
    Repo->>DB: SELECT user
    DB-->>Repo: user / empty
    Repo-->>Service: Optional<User>

    alt User not found
        Service-->>API: InvalidCredentialsException
        API-->>UI: 401 Unauthorized
        UI-->>User: פרטי התחברות שגויים
    else User found
        Service->>Encoder: matches(rawPassword, hash)
        Encoder-->>Service: true / false

        alt Password invalid
            Service-->>API: InvalidCredentialsException
            API-->>UI: 401 Unauthorized
            UI-->>User: פרטי התחברות שגויים
        else Password valid
            Service->>JWT: generateAccessToken(userId)
            JWT-->>Service: accessToken
            Service-->>API: LoginResponse
            API-->>UI: 200 OK + token
            UI-->>User: מעבר ל-Dashboard
        end
    end
```

## כללי מימוש

- אותה הודעת שגיאה עבור אימייל או סיסמה שגויים.
- אין לחשוף אם משתמש קיים.
- ה-JWT כולל מזהה משתמש בלבד ומידע מינימלי.

---

# 3. Load Current User

```mermaid
sequenceDiagram
    actor User
    participant UI as Angular App
    participant Filter as JwtAuthenticationFilter
    participant API as UserController
    participant Service as UserService
    participant Repo as UserRepository
    participant DB as PostgreSQL

    User->>UI: פותח אזור מאובטח
    UI->>Filter: GET /api/users/me + Bearer Token
    Filter->>Filter: validate token

    alt Token invalid or expired
        Filter-->>UI: 401 Unauthorized
        UI-->>User: מעבר למסך התחברות
    else Token valid
        Filter->>API: request with authenticated user
        API->>Service: getCurrentUser(userId)
        Service->>Repo: findById(userId)
        Repo->>DB: SELECT user
        DB-->>Repo: user
        Repo-->>Service: user
        Service-->>API: UserResponse
        API-->>UI: 200 OK
        UI-->>User: מציג פרטי משתמש
    end
```

---

# 4. Create Portfolio by Investment Amount

```mermaid
sequenceDiagram
    actor User
    participant UI as Angular Portfolio Form
    participant Filter as JwtAuthenticationFilter
    participant API as PortfolioController
    participant Service as PortfolioService
    participant Calc as AllocationCalculator
    participant Repo as PortfolioRepository
    participant HoldingRepo as HoldingRepository
    participant DB as PostgreSQL

    User->>UI: שם תיק, סכום, 10 מניות ומשקלים
    UI->>UI: Client-side validation
    UI->>Filter: POST /api/portfolios/by-amount
    Filter->>API: authenticated request
    API->>Service: createByAmount(userId, request)

    Service->>Service: validate exactly 10 unique symbols
    Service->>Service: validate weights total 100%
    Service->>Calc: calculate allocations
    Calc-->>Service: quantities and values

    Service->>Repo: save(portfolio)
    Repo->>DB: INSERT portfolio
    DB-->>Repo: portfolio

    loop Each holding
        Service->>HoldingRepo: save(holding)
        HoldingRepo->>DB: INSERT holding
        DB-->>HoldingRepo: saved holding
    end

    Service-->>API: PortfolioDetailsResponse
    API-->>UI: 201 Created
    UI-->>User: מציג תיק חדש
```

## Transaction Boundary

כל הפעולה חייבת לרוץ בתוך `@Transactional`.

אם יצירת Holding אחת נכשלת, כל יצירת התיק מתבטלת.

---

# 5. Create Portfolio from Existing Holdings

```mermaid
sequenceDiagram
    actor User
    participant UI as Angular Existing Holdings Form
    participant API as PortfolioController
    participant Service as PortfolioService
    participant Calc as PortfolioCalculationService
    participant Repo as PortfolioRepository
    participant HoldingRepo as HoldingRepository
    participant DB as PostgreSQL

    User->>UI: מזין 10 אחזקות קיימות
    UI->>API: POST /api/portfolios/from-holdings
    API->>Service: createFromHoldings(userId, request)

    Service->>Service: validate symbols, quantity and prices
    Service->>Calc: calculate current values
    Calc-->>Service: values and portfolio total
    Service->>Calc: calculate weights
    Calc-->>Service: current weights

    Service->>Repo: save(portfolio)
    Repo->>DB: INSERT portfolio

    loop Each holding
        Service->>HoldingRepo: save(holding)
        HoldingRepo->>DB: INSERT holding
    end

    Service-->>API: PortfolioDetailsResponse
    API-->>UI: 201 Created
    UI-->>User: מציג תיק חדש
```

---

# 6. Create Weekly Signal

```mermaid
sequenceDiagram
    actor User
    participant UI as Angular Signal Form
    participant API as SignalController
    participant Service as SignalService
    participant PortfolioRepo as PortfolioRepository
    participant SignalRepo as SignalRepository
    participant DB as PostgreSQL

    User->>UI: מזין 10 משקלי יעד
    UI->>API: POST /api/portfolios/{id}/signals
    API->>Service: createSignal(userId, portfolioId, request)

    Service->>PortfolioRepo: findOwnedPortfolio(userId, portfolioId)
    PortfolioRepo->>DB: SELECT portfolio
    DB-->>PortfolioRepo: portfolio / empty

    alt Portfolio not owned or not found
        Service-->>API: PortfolioNotFoundException
        API-->>UI: 404 Not Found
    else Portfolio found
        Service->>Service: validate 10 allocations and 100%
        Service->>SignalRepo: save(signal + allocations)
        SignalRepo->>DB: INSERT signal and allocations
        DB-->>SignalRepo: saved signal
        Service-->>API: SignalResponse
        API-->>UI: 201 Created
    end
```

---

# 7. Preview Rebalance

```mermaid
sequenceDiagram
    actor User
    participant UI as Angular Rebalance Preview
    participant API as RebalanceController
    participant Service as RebalanceService
    participant PortfolioRepo as PortfolioRepository
    participant SignalRepo as SignalRepository
    participant Calc as RebalanceCalculator
    participant DB as PostgreSQL

    User->>UI: מבקש Preview
    UI->>API: POST /api/portfolios/{id}/signals/{signalId}/preview
    API->>Service: preview(userId, portfolioId, signalId)

    Service->>PortfolioRepo: load portfolio with holdings
    PortfolioRepo->>DB: SELECT portfolio + holdings
    DB-->>PortfolioRepo: portfolio data

    Service->>SignalRepo: load signal with allocations
    SignalRepo->>DB: SELECT signal + allocations
    DB-->>SignalRepo: signal data

    Service->>Service: validate ownership and signal status
    Service->>Calc: calculate rebalance trades
    Calc-->>Service: BUY / SELL / HOLD instructions

    Service-->>API: RebalancePreviewResponse
    API-->>UI: 200 OK
    UI-->>User: מציג עסקאות צפויות
```

## חשוב

Preview אינו משנה מידע במסד הנתונים.

הוא מבצע חישוב בלבד.

---

# 8. Execute Rebalance

```mermaid
sequenceDiagram
    actor User
    participant UI as Angular Rebalance Confirmation
    participant API as RebalanceController
    participant Service as RebalanceService
    participant Snapshot as SnapshotService
    participant Calc as RebalanceCalculator
    participant TradeRepo as TradeTransactionRepository
    participant HoldingRepo as HoldingRepository
    participant SignalRepo as SignalRepository
    participant DB as PostgreSQL

    User->>UI: מאשר Execute
    UI->>API: POST /api/portfolios/{id}/signals/{signalId}/execute
    API->>Service: execute(userId, portfolioId, signalId)

    Service->>SignalRepo: lock and load signal
    SignalRepo->>DB: SELECT signal FOR UPDATE
    DB-->>SignalRepo: signal

    alt Signal already executed
        Service-->>API: SignalAlreadyExecutedException
        API-->>UI: 409 Conflict
    else Signal available
        Service->>Snapshot: createBeforeSnapshot(portfolioId)
        Snapshot->>DB: INSERT snapshot and items

        Service->>Calc: calculate rebalance trades
        Calc-->>Service: transactions

        loop Each transaction
            Service->>TradeRepo: save(transaction)
            TradeRepo->>DB: INSERT trade_transaction
            Service->>HoldingRepo: update holding
            HoldingRepo->>DB: UPDATE holding
        end

        Service->>SignalRepo: markExecuted(signalId)
        SignalRepo->>DB: UPDATE weekly_signal

        Service->>Snapshot: createAfterSnapshot(portfolioId)
        Snapshot->>DB: INSERT snapshot and items

        Service-->>API: RebalanceExecutionResponse
        API-->>UI: 200 OK
        UI-->>User: מציג תוצאות ביצוע
    end
```

## Transaction Boundary

הפעולה כולה רצה בתוך Transaction אחת:

1. נעילת Signal.
2. Snapshot לפני.
3. יצירת Transactions.
4. עדכון Holdings.
5. סימון Signal כבוצע.
6. Snapshot אחרי.

כל כשל גורם ל-Rollback מלא.

---

# 9. Load Dashboard

```mermaid
sequenceDiagram
    actor User
    participant UI as Angular Dashboard
    participant API as DashboardController
    participant Service as DashboardService
    participant PortfolioRepo as PortfolioRepository
    participant SnapshotRepo as SnapshotRepository
    participant TransactionRepo as TradeTransactionRepository
    participant DB as PostgreSQL

    User->>UI: פותח Dashboard
    UI->>API: GET /api/dashboard
    API->>Service: getDashboard(userId)

    par Portfolios
        Service->>PortfolioRepo: findAllByOwner(userId)
        PortfolioRepo->>DB: SELECT portfolios
        DB-->>PortfolioRepo: portfolios
    and Recent Snapshots
        Service->>SnapshotRepo: findRecentByOwner(userId)
        SnapshotRepo->>DB: SELECT snapshots
        DB-->>SnapshotRepo: snapshots
    and Recent Transactions
        Service->>TransactionRepo: findRecentByOwner(userId)
        TransactionRepo->>DB: SELECT transactions
        DB-->>TransactionRepo: transactions
    end

    Service-->>API: DashboardResponse
    API-->>UI: 200 OK
    UI-->>User: מציג נתוני Dashboard
```

---

# 10. Ownership Validation

```mermaid
sequenceDiagram
    participant API as REST Controller
    participant Service as Domain Service
    participant Repo as Repository
    participant DB as PostgreSQL

    API->>Service: operation(authenticatedUserId, resourceId)
    Service->>Repo: findByIdAndOwnerId(resourceId, authenticatedUserId)
    Repo->>DB: SELECT ... WHERE id = ? AND owner_id = ?
    DB-->>Repo: resource / empty

    alt Resource found
        Repo-->>Service: resource
        Service-->>API: continue
    else Resource missing or belongs to another user
        Repo-->>Service: empty
        Service-->>API: 404 Not Found
    end
```

## אבטחה

בכוונה מוחזר `404` גם כאשר המשאב קיים אך שייך למשתמש אחר.

כך לא חושפים את עצם קיומו של המשאב.

---

# 11. Global Error Handling

```mermaid
sequenceDiagram
    actor Client
    participant API as Controller
    participant Service as Service
    participant Handler as GlobalExceptionHandler
    participant Log as Logger

    Client->>API: Request
    API->>Service: execute operation
    Service-->>API: throws exception
    API-->>Handler: propagate exception

    Handler->>Log: log error with correlationId
    Handler-->>Client: standardized ErrorResponse
```

## Error Response Example

```json
{
  "timestamp": "2026-07-24T15:30:00Z",
  "status": 409,
  "code": "SIGNAL_ALREADY_EXECUTED",
  "message": "The signal has already been executed.",
  "correlationId": "8f44e0c2-6bdf-4b10-b0f7-4e43b2a5b138"
}
```

---

# 12. Angular HTTP Error Flow

```mermaid
sequenceDiagram
    actor User
    participant Component as Angular Component
    participant Service as Angular API Service
    participant Interceptor as HTTP Interceptor
    participant API as Spring Boot API
    participant Router as Angular Router

    User->>Component: מבצע פעולה
    Component->>Service: call API
    Service->>Interceptor: HTTP request
    Interceptor->>API: request with JWT

    alt 200-299
        API-->>Interceptor: success response
        Interceptor-->>Service: response
        Service-->>Component: data
        Component-->>User: הצלחה
    else 401
        API-->>Interceptor: 401
        Interceptor->>Router: navigate('/login')
        Router-->>User: מסך התחברות
    else 400/409
        API-->>Interceptor: validation/business error
        Interceptor-->>Service: error response
        Service-->>Component: error
        Component-->>User: הודעה מותאמת
    else 500
        API-->>Interceptor: server error
        Interceptor-->>Component: generic error
        Component-->>User: הודעת תקלה כללית
    end
```

---

# כללי יישום כלליים

- Controllers דקים.
- Business Logic ב-Service בלבד.
- גישה למסד דרך Repository בלבד.
- Mapping דרך MapStruct.
- פעולות כתיבה מורכבות בתוך Transaction.
- Ownership נבדק בכל משאב פרטי.
- Response Errors אחידים.
- Correlation ID עובר בכל הבקשה.
- Preview אינו כותב למסד.
- Execute חייב להיות Idempotent ברמת Signal.

---

# Checklist למימוש

## Register

- [ ] אימייל מנורמל
- [ ] בדיקת כפילות
- [ ] BCrypt
- [ ] תגובת 201
- [ ] בדיקת Integration

## Login

- [ ] הודעת שגיאה אחידה
- [ ] JWT תקין
- [ ] Expiration
- [ ] בדיקת Token פגום

## Portfolio Creation

- [ ] בדיוק 10 מניות
- [ ] Symbols ייחודיים
- [ ] סכום משקלים 100%
- [ ] Transaction מלאה
- [ ] BigDecimal בלבד

## Rebalance

- [ ] Preview ללא כתיבה
- [ ] Execute עם נעילה
- [ ] מניעת Execute כפול
- [ ] Snapshot לפני ואחרי
- [ ] Rollback מלא בכשל

## Security

- [ ] Ownership בכל Resource
- [ ] 404 למשאב שאינו שייך למשתמש
- [ ] אין מידע רגיש בלוגים
- [ ] JWT Filter לכל Endpoint מוגן

---

# הצעד הבא

`14_CLASS_DIAGRAM.md`

המסמך הבא יגדיר:

- Entities
- Repositories
- Services
- Controllers
- DTOs
- יחסי תלות
- גבולות אחריות בין שכבות
- תרשים UML מלא ב-Mermaid
