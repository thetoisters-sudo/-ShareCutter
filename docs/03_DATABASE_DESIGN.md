# 03_DATABASE_DESIGN.md

## שם הפרויקט

**ShareCutter**

## נתיב הפרויקט המקומי

```text
C:\Users\אורטל\Desktop\ShareCutter
```

---

## 1. מטרת המסמך

מסמך זה מגדיר את מבנה מסד הנתונים של ShareCutter עבור PostgreSQL.

המסמך כולל:

- טבלאות.
- שדות.
- טיפוסי נתונים.
- Primary Keys.
- Foreign Keys.
- Constraints.
- Indexes.
- קשרים בין הישויות.
- כללי מחיקה.
- דיוק מספרי.
- שמירת היסטוריה.

---

## 2. עקרונות תכנון

### 2.1 מזהים

כל הטבלאות ישתמשו ב-UUID.

```sql
UUID
```

Spring Boot יפיק UUID לכל רשומה.

### 2.2 תאריכים

יש להשתמש ב:

```sql
TIMESTAMP WITH TIME ZONE
```

עבור תאריך ושעה.

יש להשתמש ב:

```sql
DATE
```

עבור תאריך עסקי בלבד, כגון תאריך איתות.

### 2.3 כסף וכמויות

אין להשתמש ב:

```sql
REAL
DOUBLE PRECISION
```

יש להשתמש ב:

```sql
NUMERIC
```

### 2.4 דיוק מומלץ

```text
מחירים וסכומים:
NUMERIC(19,4)

כמויות מניות:
NUMERIC(24,8)

משקלים באחוזים:
NUMERIC(9,4)

תשואות:
NUMERIC(19,6)
```

### 2.5 שמות טבלאות

שמות הטבלאות יהיו:

```text
snake_case
plural
```

### 2.6 שמות עמודות

שמות העמודות יהיו:

```text
snake_case
```

---

## 3. תרשים קשרים כללי

```text
users
  │
  └──< portfolios
          │
          ├──< holdings
          │
          ├──< weekly_signals
          │       │
          │       └──< signal_allocations
          │
          ├──< trade_transactions
          │
          ├──< portfolio_snapshots
          │       │
          │       └──< portfolio_snapshot_items
          │
          └──< stock_price_entries
```

---

## 4. טבלת users

### מטרה

שמירת משתמשים רשומים.

### מבנה

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_login_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_users_email UNIQUE (email)
);
```

### כללים

- האימייל יישמר באותיות קטנות.
- האימייל יעבור Trim.
- הסיסמה תישמר כ-Hash בלבד.
- אין לשמור סיסמה גלויה.

### Indexes

```sql
CREATE UNIQUE INDEX idx_users_email_lower
ON users (LOWER(email));
```

ה-Constraint הייחודי וה-Index חייבים להיות מתואמים כך שלא ייווצרו משתמשים כפולים בגלל הבדל באותיות גדולות וקטנות.

---

## 5. טבלת portfolios

### מטרה

שמירת תיקי השקעות של משתמשים.

### מבנה

```sql
CREATE TABLE portfolios (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    name VARCHAR(120) NOT NULL,
    creation_method VARCHAR(30) NOT NULL,
    initial_value NUMERIC(19,4) NOT NULL,
    current_value NUMERIC(19,4) NOT NULL,
    total_realized_profit NUMERIC(19,4) NOT NULL DEFAULT 0,
    total_unrealized_profit NUMERIC(19,4) NOT NULL DEFAULT 0,
    total_return_percent NUMERIC(19,6) NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_portfolios_user
        FOREIGN KEY (user_id)
        REFERENCES users(id),

    CONSTRAINT uq_portfolios_user_name
        UNIQUE (user_id, name),

    CONSTRAINT chk_portfolios_initial_value_positive
        CHECK (initial_value > 0),

    CONSTRAINT chk_portfolios_current_value_non_negative
        CHECK (current_value >= 0),

    CONSTRAINT chk_portfolios_creation_method
        CHECK (creation_method IN ('BY_AMOUNT', 'BY_HOLDINGS'))
);
```

### הערות

- משתמש יכול להחזיק כמה תיקים.
- שם תיק חייב להיות ייחודי עבור אותו משתמש.
- `deleted_at` מאפשר מחיקה לוגית.
- תיק מחוק לא יוצג בברירת מחדל.

### Indexes

```sql
CREATE INDEX idx_portfolios_user_id
ON portfolios(user_id);

CREATE INDEX idx_portfolios_user_active
ON portfolios(user_id)
WHERE deleted_at IS NULL;
```

---

## 6. טבלת holdings

### מטרה

שמירת האחזקות הפעילות בתיק.

### מבנה

```sql
CREATE TABLE holdings (
    id UUID PRIMARY KEY,
    portfolio_id UUID NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    company_name VARCHAR(160),
    quantity NUMERIC(24,8) NOT NULL,
    average_buy_price NUMERIC(19,4) NOT NULL,
    current_price NUMERIC(19,4) NOT NULL,
    current_value NUMERIC(19,4) NOT NULL,
    current_weight NUMERIC(9,4) NOT NULL,
    unrealized_profit NUMERIC(19,4) NOT NULL DEFAULT 0,
    unrealized_return_percent NUMERIC(19,6) NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_holdings_portfolio
        FOREIGN KEY (portfolio_id)
        REFERENCES portfolios(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_holdings_portfolio_symbol
        UNIQUE (portfolio_id, symbol),

    CONSTRAINT chk_holdings_quantity_positive
        CHECK (quantity > 0),

    CONSTRAINT chk_holdings_average_buy_price_positive
        CHECK (average_buy_price > 0),

    CONSTRAINT chk_holdings_current_price_positive
        CHECK (current_price > 0),

    CONSTRAINT chk_holdings_current_value_non_negative
        CHECK (current_value >= 0),

    CONSTRAINT chk_holdings_current_weight_range
        CHECK (current_weight >= 0 AND current_weight <= 100)
);
```

### כללים

- סימול יישמר באותיות גדולות.
- לא יכולה להיות אותה מניה פעמיים באותו תיק.
- לאחר איזון תקין יהיו בדיוק 10 רשומות פעילות.
- מניה עם כמות 0 תימחק מהטבלה.

### Indexes

```sql
CREATE INDEX idx_holdings_portfolio_id
ON holdings(portfolio_id);

CREATE INDEX idx_holdings_symbol
ON holdings(symbol);
```

---

## 7. טבלת weekly_signals

### מטרה

שמירת איתותים אישיים של משתמש עבור תיק מסוים.

### מבנה

```sql
CREATE TABLE weekly_signals (
    id UUID PRIMARY KEY,
    portfolio_id UUID NOT NULL,
    signal_date DATE NOT NULL,
    title VARCHAR(160),
    description TEXT,
    status VARCHAR(20) NOT NULL,
    portfolio_value_at_calculation NUMERIC(19,4),
    calculated_at TIMESTAMP WITH TIME ZONE,
    executed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_weekly_signals_portfolio
        FOREIGN KEY (portfolio_id)
        REFERENCES portfolios(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_weekly_signals_status
        CHECK (status IN ('DRAFT', 'CALCULATED', 'EXECUTED'))
);
```

### כללים

- איתות שייך לתיק אחד.
- איתות יכול להיות טיוטה, מחושב או מבוצע.
- איתות שבוצע לא ניתן לעריכה.
- איתות לא יכול להתבצע פעמיים.

### Indexes

```sql
CREATE INDEX idx_weekly_signals_portfolio_id
ON weekly_signals(portfolio_id);

CREATE INDEX idx_weekly_signals_portfolio_date
ON weekly_signals(portfolio_id, signal_date DESC);

CREATE INDEX idx_weekly_signals_status
ON weekly_signals(status);
```

---

## 8. טבלת signal_allocations

### מטרה

שמירת 10 המניות והמשקלים בכל איתות.

### מבנה

```sql
CREATE TABLE signal_allocations (
    id UUID PRIMARY KEY,
    signal_id UUID NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    company_name VARCHAR(160),
    target_weight NUMERIC(9,4) NOT NULL,
    market_price NUMERIC(19,4) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_signal_allocations_signal
        FOREIGN KEY (signal_id)
        REFERENCES weekly_signals(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_signal_allocations_signal_symbol
        UNIQUE (signal_id, symbol),

    CONSTRAINT chk_signal_allocations_weight_range
        CHECK (target_weight >= 0 AND target_weight <= 100),

    CONSTRAINT chk_signal_allocations_market_price_positive
        CHECK (market_price > 0)
);
```

### כללים

- לכל איתות יהיו בדיוק 10 הקצאות.
- לא ניתן להזין סימול פעמיים באותו איתות.
- סכום המשקלים חייב להיות 100%.
- סכום המשקלים ייבדק ב-Service, לא רק במסד הנתונים.
- מחיר השוק נשמר כחלק מהאיתות כדי לשמר היסטוריה.

### Indexes

```sql
CREATE INDEX idx_signal_allocations_signal_id
ON signal_allocations(signal_id);

CREATE INDEX idx_signal_allocations_symbol
ON signal_allocations(symbol);
```

---

## 9. טבלת trade_transactions

### מטרה

שמירת פעולות קנייה ומכירה שנוצרו בעת אישור איזון.

### מבנה

```sql
CREATE TABLE trade_transactions (
    id UUID PRIMARY KEY,
    portfolio_id UUID NOT NULL,
    signal_id UUID,
    symbol VARCHAR(20) NOT NULL,
    transaction_type VARCHAR(10) NOT NULL,
    quantity NUMERIC(24,8) NOT NULL,
    price NUMERIC(19,4) NOT NULL,
    total_amount NUMERIC(19,4) NOT NULL,
    average_buy_price_before NUMERIC(19,4),
    average_buy_price_after NUMERIC(19,4),
    realized_profit NUMERIC(19,4) NOT NULL DEFAULT 0,
    executed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_trade_transactions_portfolio
        FOREIGN KEY (portfolio_id)
        REFERENCES portfolios(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_trade_transactions_signal
        FOREIGN KEY (signal_id)
        REFERENCES weekly_signals(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_trade_transactions_type
        CHECK (transaction_type IN ('BUY', 'SELL')),

    CONSTRAINT chk_trade_transactions_quantity_positive
        CHECK (quantity > 0),

    CONSTRAINT chk_trade_transactions_price_positive
        CHECK (price > 0),

    CONSTRAINT chk_trade_transactions_total_amount_positive
        CHECK (total_amount > 0)
);
```

### כללים

- כל פעולה שייכת לתיק.
- פעולה שנוצרה מאיתות תישמר עם `signal_id`.
- במכירה יישמר רווח או הפסד ממומש.
- אין לערוך עסקה לאחר יצירתה.

### Indexes

```sql
CREATE INDEX idx_trade_transactions_portfolio_id
ON trade_transactions(portfolio_id);

CREATE INDEX idx_trade_transactions_signal_id
ON trade_transactions(signal_id);

CREATE INDEX idx_trade_transactions_portfolio_date
ON trade_transactions(portfolio_id, executed_at DESC);

CREATE INDEX idx_trade_transactions_symbol
ON trade_transactions(symbol);
```

---

## 10. טבלת portfolio_snapshots

### מטרה

שמירת מצב תיק בנקודות זמן חשובות.

### מבנה

```sql
CREATE TABLE portfolio_snapshots (
    id UUID PRIMARY KEY,
    portfolio_id UUID NOT NULL,
    signal_id UUID,
    snapshot_type VARCHAR(20) NOT NULL,
    total_value NUMERIC(19,4) NOT NULL,
    realized_profit NUMERIC(19,4) NOT NULL DEFAULT 0,
    unrealized_profit NUMERIC(19,4) NOT NULL DEFAULT 0,
    total_return_percent NUMERIC(19,6) NOT NULL DEFAULT 0,
    snapshot_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_portfolio_snapshots_portfolio
        FOREIGN KEY (portfolio_id)
        REFERENCES portfolios(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_portfolio_snapshots_signal
        FOREIGN KEY (signal_id)
        REFERENCES weekly_signals(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_portfolio_snapshots_type
        CHECK (snapshot_type IN (
            'INITIAL',
            'BEFORE_REBALANCE',
            'AFTER_REBALANCE',
            'MANUAL'
        )),

    CONSTRAINT chk_portfolio_snapshots_total_value_non_negative
        CHECK (total_value >= 0)
);
```

### שימוש

- `INITIAL` בעת יצירת תיק.
- `BEFORE_REBALANCE` לפני ביצוע איתות.
- `AFTER_REBALANCE` לאחר ביצוע איתות.
- `MANUAL` לעדכון עתידי אם יידרש.

### Indexes

```sql
CREATE INDEX idx_portfolio_snapshots_portfolio_id
ON portfolio_snapshots(portfolio_id);

CREATE INDEX idx_portfolio_snapshots_portfolio_date
ON portfolio_snapshots(portfolio_id, snapshot_at DESC);
```

---

## 11. טבלת portfolio_snapshot_items

### מטרה

שמירת פירוט האחזקות בכל Snapshot.

### מבנה

```sql
CREATE TABLE portfolio_snapshot_items (
    id UUID PRIMARY KEY,
    snapshot_id UUID NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    quantity NUMERIC(24,8) NOT NULL,
    average_buy_price NUMERIC(19,4) NOT NULL,
    market_price NUMERIC(19,4) NOT NULL,
    holding_value NUMERIC(19,4) NOT NULL,
    weight_percent NUMERIC(9,4) NOT NULL,
    unrealized_profit NUMERIC(19,4) NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_snapshot_items_snapshot
        FOREIGN KEY (snapshot_id)
        REFERENCES portfolio_snapshots(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_snapshot_items_snapshot_symbol
        UNIQUE (snapshot_id, symbol),

    CONSTRAINT chk_snapshot_items_quantity_positive
        CHECK (quantity > 0),

    CONSTRAINT chk_snapshot_items_average_buy_price_positive
        CHECK (average_buy_price > 0),

    CONSTRAINT chk_snapshot_items_market_price_positive
        CHECK (market_price > 0),

    CONSTRAINT chk_snapshot_items_weight_range
        CHECK (weight_percent >= 0 AND weight_percent <= 100)
);
```

### Indexes

```sql
CREATE INDEX idx_snapshot_items_snapshot_id
ON portfolio_snapshot_items(snapshot_id);
```

---

## 12. טבלת stock_price_entries

### מטרה

שמירת מחירים ידניים שהוזנו על ידי המשתמש.

### מבנה

```sql
CREATE TABLE stock_price_entries (
    id UUID PRIMARY KEY,
    portfolio_id UUID NOT NULL,
    signal_id UUID,
    symbol VARCHAR(20) NOT NULL,
    price NUMERIC(19,4) NOT NULL,
    source VARCHAR(20) NOT NULL,
    price_date DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_stock_price_entries_portfolio
        FOREIGN KEY (portfolio_id)
        REFERENCES portfolios(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_stock_price_entries_signal
        FOREIGN KEY (signal_id)
        REFERENCES weekly_signals(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_stock_price_entries_price_positive
        CHECK (price > 0),

    CONSTRAINT chk_stock_price_entries_source
        CHECK (source IN ('MANUAL', 'EXTERNAL_API'))
);
```

### הערות

ב-MVP:

```text
source = MANUAL
```

בעתיד:

```text
source = EXTERNAL_API
```

### Indexes

```sql
CREATE INDEX idx_stock_price_entries_portfolio_id
ON stock_price_entries(portfolio_id);

CREATE INDEX idx_stock_price_entries_symbol_date
ON stock_price_entries(symbol, price_date DESC);
```

---

## 13. קשרים בין הישויות

### User → Portfolio

```text
One-to-Many
```

משתמש אחד יכול להחזיק כמה תיקים.

### Portfolio → Holding

```text
One-to-Many
```

לתיק יש 10 אחזקות פעילות לאחר יצירה או איזון.

### Portfolio → WeeklySignal

```text
One-to-Many
```

לתיק יכולים להיות איתותים רבים.

### WeeklySignal → SignalAllocation

```text
One-to-Many
```

לכל איתות יש בדיוק 10 הקצאות.

### Portfolio → TradeTransaction

```text
One-to-Many
```

לתיק יש היסטוריית עסקאות.

### WeeklySignal → TradeTransaction

```text
One-to-Many
```

איתות שבוצע יכול ליצור כמה עסקאות.

### Portfolio → PortfolioSnapshot

```text
One-to-Many
```

לתיק יש היסטוריית מצבים.

### PortfolioSnapshot → PortfolioSnapshotItem

```text
One-to-Many
```

כל Snapshot מכיל את פירוט האחזקות באותה נקודת זמן.

---

## 14. כללי מחיקה

### User

ב-MVP לא נדרש מסך מחיקת משתמש.

### Portfolio

תיק יימחק לוגית:

```text
deleted_at != NULL
```

הנתונים ההיסטוריים יישמרו.

### Holding

אחזקה עם כמות 0 תימחק פיזית.

### WeeklySignal

- ניתן למחוק טיוטה.
- לא ניתן למחוק איתות שבוצע.
- אפשרות עתידית: מחיקה לוגית.

### TradeTransaction

אין למחוק עסקאות שבוצעו.

### PortfolioSnapshot

אין למחוק Snapshot שבוצע, אלא במחיקת תיק מלאה לצורכי פיתוח.

---

## 15. Constraints עסקיים שלא ייאכפו רק במסד הנתונים

הבדיקות הבאות יתבצעו ב-Service:

- בדיוק 10 מניות בתיק.
- בדיוק 10 מניות באיתות.
- סכום משקלים 100%.
- משתמש מחובר הוא בעל התיק.
- איתות לא בוצע קודם.
- מכירה לא גדולה מהכמות הקיימת.
- כל הסימולים ייחודיים.
- כל המחירים קיימים.
- לאחר איזון יש בדיוק 10 אחזקות.
- תיק מחוק לא ניתן לשינוי.
- איתות EXECUTED לא ניתן לעריכה.

---

## 16. בדיקת 100%

### סבילות

בגלל ערכים עשרוניים, הבדיקה תשתמש בסבילות:

```text
99.9999 <= totalWeight <= 100.0001
```

### Java

```java
BigDecimal minimum = new BigDecimal("99.9999");
BigDecimal maximum = new BigDecimal("100.0001");
```

אם הסכום מחוץ לטווח, הבקשה תידחה.

---

## 17. Auditing

כל ישות מרכזית תכיל:

```text
created_at
updated_at
```

Spring Data JPA Auditing ישמש לעדכון אוטומטי.

### מחלקת בסיס אפשרית

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableEntity {
    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
```

ישויות היסטוריות שאינן משתנות, כגון עסקה, יכולות להכיל רק `created_at`.

---

## 18. Enum Mapping

ב-Java יש להשתמש ב:

```java
@Enumerated(EnumType.STRING)
```

אין להשתמש ב-Ordinal.

### Enums

```text
PortfolioCreationMethod
- BY_AMOUNT
- BY_HOLDINGS

SignalStatus
- DRAFT
- CALCULATED
- EXECUTED

TransactionType
- BUY
- SELL

SnapshotType
- INITIAL
- BEFORE_REBALANCE
- AFTER_REBALANCE
- MANUAL

StockPriceSource
- MANUAL
- EXTERNAL_API
```

---

## 19. Flyway Migrations

מבנה מוצע:

```text
backend/src/main/resources/db/migration/
├── V1__create_users.sql
├── V2__create_portfolios_and_holdings.sql
├── V3__create_signals.sql
├── V4__create_transactions.sql
├── V5__create_snapshots.sql
└── V6__create_stock_price_entries.sql
```

בגלל שמדובר בפרויקט קטן, ניתן גם להשתמש ב-Migration אחד:

```text
V1__create_initial_schema.sql
```

המלצה:

```text
Migration אחד ל-MVP הראשוני
```

לאחר שהפרויקט עובד, כל שינוי נוסף יקבל Migration חדש.

---

## 20. סדר שמירה ביצירת תיק

### יצירה לפי סכום

1. שמירת Portfolio.
2. חישוב 10 אחזקות.
3. שמירת Holdings.
4. שמירת מחירים.
5. יצירת Snapshot מסוג INITIAL.
6. שמירת Snapshot Items.
7. Commit.

### יצירה לפי אחזקות קיימות

1. חישוב שווי כל אחזקה.
2. חישוב שווי תיק.
3. שמירת Portfolio.
4. שמירת Holdings.
5. שמירת מחירים.
6. יצירת Snapshot מסוג INITIAL.
7. שמירת Snapshot Items.
8. Commit.

כל התהליך ירוץ בתוך Transaction אחת.

---

## 21. סדר שמירה בביצוע איזון

1. טעינת Portfolio עם נעילה מתאימה אם נדרש.
2. בדיקת בעלות.
3. בדיקת Signal.
4. יצירת Snapshot מסוג BEFORE_REBALANCE.
5. שמירת Snapshot Items.
6. יצירת Trade Transactions.
7. עדכון או מחיקת Holdings קיימות.
8. יצירת Holdings חדשות.
9. חישוב מחדש של שווי ומשקלים.
10. יצירת Snapshot מסוג AFTER_REBALANCE.
11. שמירת Snapshot Items.
12. עדכון Signal ל-EXECUTED.
13. עדכון Portfolio.
14. Commit.

במקרה של כשל:

```text
Rollback מלא
```

---

## 22. שאילתות מרכזיות

### כל תיקי המשתמש

```sql
SELECT *
FROM portfolios
WHERE user_id = :userId
  AND deleted_at IS NULL
ORDER BY updated_at DESC;
```

### תיק לפי מזהה ובעלות

```sql
SELECT *
FROM portfolios
WHERE id = :portfolioId
  AND user_id = :userId
  AND deleted_at IS NULL;
```

### אחזקות תיק

```sql
SELECT *
FROM holdings
WHERE portfolio_id = :portfolioId
ORDER BY current_weight DESC;
```

### איתותים לפי תיק

```sql
SELECT *
FROM weekly_signals
WHERE portfolio_id = :portfolioId
ORDER BY signal_date DESC, created_at DESC;
```

### עסקאות לפי תיק

```sql
SELECT *
FROM trade_transactions
WHERE portfolio_id = :portfolioId
ORDER BY executed_at DESC;
```

### Snapshot אחרון

```sql
SELECT *
FROM portfolio_snapshots
WHERE portfolio_id = :portfolioId
ORDER BY snapshot_at DESC
LIMIT 1;
```

---

## 23. החלטות סופיות למסד הנתונים

- UUID לכל מזהה.
- PostgreSQL.
- Flyway.
- BigDecimal מול NUMERIC.
- כמה תיקים לכל משתמש.
- בדיוק 10 אחזקות פעילות לאחר איזון.
- בדיוק 10 הקצאות בכל איתות.
- מחירים ידניים נשמרים כהיסטוריה.
- עסקאות אינן נערכות לאחר ביצוע.
- Snapshot לפני ואחרי כל איזון.
- מחיקה לוגית לתיקים.
- בידוד נתונים לפי user_id.
- Enum נשמר כטקסט.
- אין כסף מזומן בתיק.
- אין עמלות או מסים.
- אין הפקדות או משיכות ב-MVP.

---

## 24. הצעד הבא

המסמך הבא:

```text
04_API_SPECIFICATION.md
```

יכלול:

- Endpoints.
- Request DTOs.
- Response DTOs.
- קודי HTTP.
- הרשאות.
- מבני שגיאה.
- תהליכי הרשמה, תיק, איתות ואיזון.
