# 04_API_SPECIFICATION.md

## שם הפרויקט

**ShareCutter**

## נתיב הפרויקט המקומי

```text
C:\Users\אורטל\Desktop\ShareCutter
```

---

## 1. מטרת המסמך

מסמך זה מגדיר את חוזה ה-REST API בין Angular לבין Spring Boot.

המסמך כולל:

- Base URL.
- Endpoints.
- Request DTOs.
- Response DTOs.
- הרשאות.
- קודי HTTP.
- מבנה שגיאות.
- תהליכי הרשמה והתחברות.
- ניהול תיקים.
- ניהול איתותים.
- חישוב Preview.
- ביצוע איזון.
- היסטוריית עסקאות.
- היסטוריית Snapshots.

---

## 2. כללים כלליים

### Base URL

```text
/api/v1
```

### Content Type

```http
Content-Type: application/json
```

### Authentication Header

בכל בקשה פרטית:

```http
Authorization: Bearer <jwt-token>
```

### Date and Time

תאריך ושעה יישלחו בפורמט ISO 8601.

דוגמה:

```text
2026-07-24T14:30:00Z
```

תאריך עסקי בלבד:

```text
2026-07-24
```

### UUID

מזהים יישלחו כמחרוזת UUID.

דוגמה:

```text
1094042f-de24-40f7-b806-898393142650
```

### מספרים עשרוניים

כסף, כמויות, משקלים ותשואות יישלחו כמספרים עשרוניים.

דוגמה:

```json
{
  "price": 183.2500,
  "quantity": 12.34567890,
  "targetWeight": 20.0000
}
```

---

## 3. מבנה תגובת שגיאה

כל שגיאת API תחזור במבנה אחיד.

```json
{
  "timestamp": "2026-07-24T14:30:00Z",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Portfolio weights must total 100%",
  "path": "/api/v1/portfolios",
  "fieldErrors": [
    {
      "field": "allocations",
      "message": "Total weight must equal 100%"
    }
  ]
}
```

### Error Codes מוצעים

```text
VALIDATION_ERROR
INVALID_CREDENTIALS
EMAIL_ALREADY_EXISTS
UNAUTHORIZED
FORBIDDEN
RESOURCE_NOT_FOUND
PORTFOLIO_NOT_FOUND
SIGNAL_NOT_FOUND
DUPLICATE_PORTFOLIO_NAME
DUPLICATE_SYMBOL
INVALID_STOCK_COUNT
INVALID_TOTAL_WEIGHT
INVALID_SIGNAL_STATUS
SIGNAL_ALREADY_EXECUTED
INSUFFICIENT_HOLDING_QUANTITY
CONFLICT
INTERNAL_SERVER_ERROR
```

---

## 4. Authentication API

---

## 4.1 הרשמה

### Endpoint

```http
POST /api/v1/auth/register
```

### הרשאה

```text
Public
```

### Request

```json
{
  "fullName": "Israel Israeli",
  "email": "israel@example.com",
  "password": "StrongPassword123"
}
```

### RegisterRequest

```text
fullName:
- חובה
- 2 עד 120 תווים

email:
- חובה
- פורמט אימייל תקין
- עד 255 תווים

password:
- חובה
- לפחות 8 תווים
```

### Response

```http
201 Created
```

```json
{
  "id": "1094042f-de24-40f7-b806-898393142650",
  "fullName": "Israel Israeli",
  "email": "israel@example.com",
  "createdAt": "2026-07-24T14:30:00Z"
}
```

### שגיאות

```text
400 VALIDATION_ERROR
409 EMAIL_ALREADY_EXISTS
```

---

## 4.2 התחברות

### Endpoint

```http
POST /api/v1/auth/login
```

### הרשאה

```text
Public
```

### Request

```json
{
  "email": "israel@example.com",
  "password": "StrongPassword123"
}
```

### Response

```http
200 OK
```

```json
{
  "accessToken": "jwt-token-value",
  "tokenType": "Bearer",
  "expiresInSeconds": 86400,
  "user": {
    "id": "1094042f-de24-40f7-b806-898393142650",
    "fullName": "Israel Israeli",
    "email": "israel@example.com"
  }
}
```

### שגיאות

```text
400 VALIDATION_ERROR
401 INVALID_CREDENTIALS
```

---

## 4.3 פרטי המשתמש המחובר

### Endpoint

```http
GET /api/v1/users/me
```

### הרשאה

```text
Authenticated
```

### Response

```http
200 OK
```

```json
{
  "id": "1094042f-de24-40f7-b806-898393142650",
  "fullName": "Israel Israeli",
  "email": "israel@example.com",
  "createdAt": "2026-07-24T14:30:00Z",
  "lastLoginAt": "2026-07-24T15:00:00Z"
}
```

---

## 5. Portfolio API

---

## 5.1 רשימת תיקים

### Endpoint

```http
GET /api/v1/portfolios
```

### הרשאה

```text
Authenticated
```

### Response

```http
200 OK
```

```json
[
  {
    "id": "3a1d8727-cf35-4dcb-ae11-12d42f884a76",
    "name": "Technology Portfolio",
    "creationMethod": "BY_AMOUNT",
    "initialValue": 10000.0000,
    "currentValue": 11250.5000,
    "totalProfit": 1250.5000,
    "totalReturnPercent": 12.505000,
    "holdingsCount": 10,
    "lastUpdatedAt": "2026-07-24T14:30:00Z"
  }
]
```

### כללים

- מוחזרים רק תיקים השייכים למשתמש המחובר.
- תיקים שנמחקו לוגית לא מוחזרים.
- ברירת מחדל: מיון לפי `updatedAt` מהחדש לישן.

---

## 5.2 יצירת תיק לפי סכום

### Endpoint

```http
POST /api/v1/portfolios/by-amount
```

### הרשאה

```text
Authenticated
```

### Request

```json
{
  "name": "Technology Portfolio",
  "investmentAmount": 10000.0000,
  "allocations": [
    {
      "symbol": "NVDA",
      "companyName": "NVIDIA",
      "price": 125.0000,
      "targetWeight": 20.0000
    },
    {
      "symbol": "AAPL",
      "companyName": "Apple",
      "price": 210.0000,
      "targetWeight": 15.0000
    }
  ]
}
```

המערך חייב להכיל בדיוק 10 מניות.

### Validation

```text
name:
- חובה
- עד 120 תווים
- ייחודי למשתמש

investmentAmount:
- חובה
- גדול מ-0

allocations:
- בדיוק 10 פריטים
- כל סימול ייחודי
- כל מחיר גדול מ-0
- כל משקל בין 0 ל-100
- סכום המשקלים 100%
```

### Response

```http
201 Created
```

```json
{
  "id": "3a1d8727-cf35-4dcb-ae11-12d42f884a76",
  "name": "Technology Portfolio",
  "creationMethod": "BY_AMOUNT",
  "initialValue": 10000.0000,
  "currentValue": 10000.0000,
  "totalRealizedProfit": 0.0000,
  "totalUnrealizedProfit": 0.0000,
  "totalReturnPercent": 0.000000,
  "holdings": [
    {
      "symbol": "NVDA",
      "companyName": "NVIDIA",
      "quantity": 16.00000000,
      "averageBuyPrice": 125.0000,
      "currentPrice": 125.0000,
      "currentValue": 2000.0000,
      "currentWeight": 20.0000,
      "unrealizedProfit": 0.0000,
      "unrealizedReturnPercent": 0.000000
    }
  ],
  "createdAt": "2026-07-24T14:30:00Z",
  "updatedAt": "2026-07-24T14:30:00Z"
}
```

### שגיאות

```text
400 VALIDATION_ERROR
400 INVALID_STOCK_COUNT
400 INVALID_TOTAL_WEIGHT
400 DUPLICATE_SYMBOL
409 DUPLICATE_PORTFOLIO_NAME
```

---

## 5.3 יצירת תיק לפי אחזקות קיימות

### Endpoint

```http
POST /api/v1/portfolios/by-holdings
```

### הרשאה

```text
Authenticated
```

### Request

```json
{
  "name": "Existing Portfolio",
  "holdings": [
    {
      "symbol": "NVDA",
      "companyName": "NVIDIA",
      "quantity": 10.50000000,
      "currentPrice": 125.0000,
      "averageBuyPrice": 110.0000
    }
  ]
}
```

המערך חייב להכיל בדיוק 10 מניות.

### Validation

```text
name:
- חובה
- ייחודי למשתמש

holdings:
- בדיוק 10 פריטים
- כל סימול ייחודי
- quantity גדול מ-0
- currentPrice גדול מ-0
- averageBuyPrice גדול מ-0
```

### Response

```http
201 Created
```

מבנה התגובה זהה ל-PortfolioDetailResponse.

---

## 5.4 קבלת תיק

### Endpoint

```http
GET /api/v1/portfolios/{portfolioId}
```

### הרשאה

```text
Authenticated
Owner only
```

### Response

```http
200 OK
```

```json
{
  "id": "3a1d8727-cf35-4dcb-ae11-12d42f884a76",
  "name": "Technology Portfolio",
  "creationMethod": "BY_AMOUNT",
  "initialValue": 10000.0000,
  "currentValue": 11250.5000,
  "totalRealizedProfit": 200.0000,
  "totalUnrealizedProfit": 1050.5000,
  "totalProfit": 1250.5000,
  "totalReturnPercent": 12.505000,
  "holdings": [
    {
      "symbol": "NVDA",
      "companyName": "NVIDIA",
      "quantity": 12.50000000,
      "averageBuyPrice": 115.0000,
      "currentPrice": 140.0000,
      "currentValue": 1750.0000,
      "currentWeight": 15.5543,
      "unrealizedProfit": 312.5000,
      "unrealizedReturnPercent": 21.739130
    }
  ],
  "createdAt": "2026-07-24T14:30:00Z",
  "updatedAt": "2026-07-24T15:30:00Z"
}
```

### שגיאות

```text
404 PORTFOLIO_NOT_FOUND
```

אין להבדיל בתגובה בין תיק שלא קיים לבין תיק שלא שייך למשתמש.

---

## 5.5 שינוי שם תיק

### Endpoint

```http
PATCH /api/v1/portfolios/{portfolioId}
```

### הרשאה

```text
Authenticated
Owner only
```

### Request

```json
{
  "name": "Updated Portfolio Name"
}
```

### Response

```http
200 OK
```

```json
{
  "id": "3a1d8727-cf35-4dcb-ae11-12d42f884a76",
  "name": "Updated Portfolio Name",
  "updatedAt": "2026-07-24T15:30:00Z"
}
```

### שגיאות

```text
400 VALIDATION_ERROR
404 PORTFOLIO_NOT_FOUND
409 DUPLICATE_PORTFOLIO_NAME
```

---

## 5.6 מחיקת תיק

### Endpoint

```http
DELETE /api/v1/portfolios/{portfolioId}
```

### הרשאה

```text
Authenticated
Owner only
```

### פעולה

מחיקה לוגית באמצעות `deleted_at`.

### Response

```http
204 No Content
```

### שגיאות

```text
404 PORTFOLIO_NOT_FOUND
```

---

## 6. Signal API

---

## 6.1 רשימת איתותים של תיק

### Endpoint

```http
GET /api/v1/portfolios/{portfolioId}/signals
```

### הרשאה

```text
Authenticated
Owner only
```

### Query Parameters אופציונליים

```text
status=DRAFT|CALCULATED|EXECUTED
page=0
size=20
```

### Response

```http
200 OK
```

```json
{
  "content": [
    {
      "id": "6bcab2bb-4754-455e-9167-31d0f1f10b42",
      "signalDate": "2026-07-24",
      "title": "Week 30 Signal",
      "status": "EXECUTED",
      "portfolioValueAtCalculation": 11250.5000,
      "createdAt": "2026-07-24T14:30:00Z",
      "executedAt": "2026-07-24T15:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

---

## 6.2 יצירת איתות

### Endpoint

```http
POST /api/v1/portfolios/{portfolioId}/signals
```

### הרשאה

```text
Authenticated
Owner only
```

### Request

```json
{
  "signalDate": "2026-07-24",
  "title": "Week 30 Signal",
  "description": "Weekly model rebalance",
  "allocations": [
    {
      "symbol": "NVDA",
      "companyName": "NVIDIA",
      "targetWeight": 5.0000,
      "marketPrice": 140.0000
    },
    {
      "symbol": "AAPL",
      "companyName": "Apple",
      "targetWeight": 25.0000,
      "marketPrice": 215.0000
    }
  ]
}
```

### Validation

```text
signalDate:
- חובה

allocations:
- בדיוק 10 פריטים
- סימולים ייחודיים
- כל מחיר גדול מ-0
- כל משקל בין 0 ל-100
- סכום המשקלים 100%
```

### Response

```http
201 Created
```

```json
{
  "id": "6bcab2bb-4754-455e-9167-31d0f1f10b42",
  "portfolioId": "3a1d8727-cf35-4dcb-ae11-12d42f884a76",
  "signalDate": "2026-07-24",
  "title": "Week 30 Signal",
  "description": "Weekly model rebalance",
  "status": "DRAFT",
  "allocations": [
    {
      "symbol": "NVDA",
      "companyName": "NVIDIA",
      "targetWeight": 5.0000,
      "marketPrice": 140.0000
    }
  ],
  "createdAt": "2026-07-24T14:30:00Z",
  "updatedAt": "2026-07-24T14:30:00Z"
}
```

---

## 6.3 קבלת איתות

### Endpoint

```http
GET /api/v1/portfolios/{portfolioId}/signals/{signalId}
```

### הרשאה

```text
Authenticated
Owner only
```

### Response

```http
200 OK
```

מבנה התגובה זהה ל-SignalDetailResponse.

### שגיאות

```text
404 PORTFOLIO_NOT_FOUND
404 SIGNAL_NOT_FOUND
```

---

## 6.4 עדכון איתות טיוטה

### Endpoint

```http
PUT /api/v1/portfolios/{portfolioId}/signals/{signalId}
```

### הרשאה

```text
Authenticated
Owner only
```

### Request

זהה ל-CreateSignalRequest.

### כללים

ניתן לעדכן רק איתות במצב:

```text
DRAFT
```

### Response

```http
200 OK
```

### שגיאות

```text
400 VALIDATION_ERROR
409 INVALID_SIGNAL_STATUS
404 SIGNAL_NOT_FOUND
```

---

## 6.5 מחיקת איתות טיוטה

### Endpoint

```http
DELETE /api/v1/portfolios/{portfolioId}/signals/{signalId}
```

### הרשאה

```text
Authenticated
Owner only
```

### כללים

ניתן למחוק רק איתות במצב DRAFT.

### Response

```http
204 No Content
```

### שגיאות

```text
409 INVALID_SIGNAL_STATUS
404 SIGNAL_NOT_FOUND
```

---

## 7. Rebalancing API

---

## 7.1 חישוב Preview

### Endpoint

```http
POST /api/v1/portfolios/{portfolioId}/signals/{signalId}/preview
```

### הרשאה

```text
Authenticated
Owner only
```

### Request

אין Body.

המערכת משתמשת בנתוני האיתות השמורים.

### פעולה

- בדיקת בעלות.
- בדיקת סטטוס DRAFT או CALCULATED.
- חישוב שווי התיק לפי מחירי האיתות.
- חישוב BUY ו-SELL.
- אין שינוי באחזקות.
- אין יצירת עסקאות.

### Response

```http
200 OK
```

```json
{
  "portfolioId": "3a1d8727-cf35-4dcb-ae11-12d42f884a76",
  "signalId": "6bcab2bb-4754-455e-9167-31d0f1f10b42",
  "portfolioValueBefore": 11250.5000,
  "totalBuyAmount": 3200.2500,
  "totalSellAmount": 3200.2500,
  "projectedPortfolioValueAfter": 11250.5000,
  "items": [
    {
      "symbol": "NVDA",
      "currentQuantity": 12.50000000,
      "currentPrice": 140.0000,
      "currentValue": 1750.0000,
      "currentWeight": 15.5543,
      "targetWeight": 5.0000,
      "targetValue": 562.5250,
      "action": "SELL",
      "tradeQuantity": 8.48232143,
      "tradeAmount": 1187.4750,
      "projectedQuantity": 4.01767857,
      "projectedValue": 562.4750
    }
  ],
  "warnings": [],
  "calculatedAt": "2026-07-24T14:45:00Z"
}
```

### הערה חשובה

בגלל עיגול כמויות, ייתכן פער זעיר בין:

```text
totalBuyAmount
totalSellAmount
```

המערכת תשתמש בסבילות מוגדרת ולא תיצור יתרת מזומן כישות נפרדת.

### שינוי סטטוס

לאחר Preview מוצלח:

```text
DRAFT → CALCULATED
```

---

## 7.2 ביצוע איזון

### Endpoint

```http
POST /api/v1/portfolios/{portfolioId}/signals/{signalId}/execute
```

### הרשאה

```text
Authenticated
Owner only
```

### Request

אין Body.

### תנאים

- האיתות במצב CALCULATED.
- האיתות לא בוצע בעבר.
- התיק עדיין קיים ואינו מחוק.
- כל המחירים תקינים.
- Preview ניתן לחישוב מחדש.

### פעולה

בתוך Transaction אחת:

1. יצירת Snapshot לפני איזון.
2. יצירת עסקאות BUY ו-SELL.
3. עדכון Holdings.
4. יצירת Snapshot אחרי איזון.
5. עדכון Portfolio.
6. שינוי Signal ל-EXECUTED.

### Response

```http
200 OK
```

```json
{
  "signalId": "6bcab2bb-4754-455e-9167-31d0f1f10b42",
  "portfolioId": "3a1d8727-cf35-4dcb-ae11-12d42f884a76",
  "status": "EXECUTED",
  "executedAt": "2026-07-24T15:00:00Z",
  "portfolio": {
    "currentValue": 11250.5000,
    "totalRealizedProfit": 450.2500,
    "totalUnrealizedProfit": 800.2500,
    "totalProfit": 1250.5000,
    "totalReturnPercent": 12.505000
  },
  "transactionsCreated": 8,
  "holdings": [
    {
      "symbol": "NVDA",
      "quantity": 4.01767857,
      "averageBuyPrice": 115.0000,
      "currentPrice": 140.0000,
      "currentValue": 562.4750,
      "currentWeight": 5.0000
    }
  ]
}
```

### שגיאות

```text
404 SIGNAL_NOT_FOUND
409 INVALID_SIGNAL_STATUS
409 SIGNAL_ALREADY_EXECUTED
500 INTERNAL_SERVER_ERROR
```

---

## 8. Transaction API

---

## 8.1 רשימת עסקאות

### Endpoint

```http
GET /api/v1/portfolios/{portfolioId}/transactions
```

### הרשאה

```text
Authenticated
Owner only
```

### Query Parameters

```text
type=BUY|SELL
symbol=NVDA
from=2026-01-01
to=2026-12-31
page=0
size=20
```

כולם אופציונליים.

### Response

```http
200 OK
```

```json
{
  "content": [
    {
      "id": "c8ef463d-e6eb-4d72-a69a-6fd1173cc056",
      "signalId": "6bcab2bb-4754-455e-9167-31d0f1f10b42",
      "symbol": "NVDA",
      "transactionType": "SELL",
      "quantity": 8.48232143,
      "price": 140.0000,
      "totalAmount": 1187.5250,
      "averageBuyPriceBefore": 115.0000,
      "averageBuyPriceAfter": 115.0000,
      "realizedProfit": 212.0580,
      "executedAt": "2026-07-24T15:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

---

## 8.2 קבלת עסקה בודדת

### Endpoint

```http
GET /api/v1/portfolios/{portfolioId}/transactions/{transactionId}
```

### הרשאה

```text
Authenticated
Owner only
```

### Response

```http
200 OK
```

מבנה התגובה זהה ל-TransactionResponse.

---

## 9. Snapshot API

---

## 9.1 רשימת Snapshots

### Endpoint

```http
GET /api/v1/portfolios/{portfolioId}/snapshots
```

### הרשאה

```text
Authenticated
Owner only
```

### Query Parameters

```text
type=INITIAL|BEFORE_REBALANCE|AFTER_REBALANCE|MANUAL
page=0
size=20
```

### Response

```http
200 OK
```

```json
{
  "content": [
    {
      "id": "4f987ef2-b3c9-40c6-bb88-6e2be3fcf982",
      "signalId": "6bcab2bb-4754-455e-9167-31d0f1f10b42",
      "snapshotType": "AFTER_REBALANCE",
      "totalValue": 11250.5000,
      "realizedProfit": 450.2500,
      "unrealizedProfit": 800.2500,
      "totalReturnPercent": 12.505000,
      "snapshotAt": "2026-07-24T15:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

---

## 9.2 קבלת Snapshot מלא

### Endpoint

```http
GET /api/v1/portfolios/{portfolioId}/snapshots/{snapshotId}
```

### הרשאה

```text
Authenticated
Owner only
```

### Response

```http
200 OK
```

```json
{
  "id": "4f987ef2-b3c9-40c6-bb88-6e2be3fcf982",
  "portfolioId": "3a1d8727-cf35-4dcb-ae11-12d42f884a76",
  "signalId": "6bcab2bb-4754-455e-9167-31d0f1f10b42",
  "snapshotType": "AFTER_REBALANCE",
  "totalValue": 11250.5000,
  "realizedProfit": 450.2500,
  "unrealizedProfit": 800.2500,
  "totalReturnPercent": 12.505000,
  "snapshotAt": "2026-07-24T15:00:00Z",
  "items": [
    {
      "symbol": "NVDA",
      "quantity": 4.01767857,
      "averageBuyPrice": 115.0000,
      "marketPrice": 140.0000,
      "holdingValue": 562.4750,
      "weightPercent": 5.0000,
      "unrealizedProfit": 100.4420
    }
  ]
}
```

---

## 10. Dashboard API

---

## 10.1 סיכום Dashboard

### Endpoint

```http
GET /api/v1/dashboard
```

### הרשאה

```text
Authenticated
```

### Response

```http
200 OK
```

```json
{
  "user": {
    "fullName": "Israel Israeli"
  },
  "summary": {
    "portfolioCount": 3,
    "combinedInitialValue": 30000.0000,
    "combinedCurrentValue": 33750.5000,
    "combinedProfit": 3750.5000,
    "combinedReturnPercent": 12.501667
  },
  "portfolios": [
    {
      "id": "3a1d8727-cf35-4dcb-ae11-12d42f884a76",
      "name": "Technology Portfolio",
      "currentValue": 11250.5000,
      "totalProfit": 1250.5000,
      "totalReturnPercent": 12.505000,
      "lastSignalDate": "2026-07-24",
      "updatedAt": "2026-07-24T15:00:00Z"
    }
  ]
}
```

### הערה

Dashboard הוא Endpoint נוחות.

אפשר היה להרכיב אותו מכמה בקשות נפרדות, אבל עבור MVP עדיף להחזיר סיכום אחד מה-Backend.

---

## 11. DTOs מרכזיים

### Auth

```text
RegisterRequest
LoginRequest
AuthResponse
UserResponse
```

### Portfolio

```text
CreatePortfolioByAmountRequest
CreatePortfolioByHoldingsRequest
PortfolioAllocationRequest
ExistingHoldingRequest
PortfolioSummaryResponse
PortfolioDetailResponse
HoldingResponse
UpdatePortfolioRequest
```

### Signal

```text
CreateSignalRequest
UpdateSignalRequest
SignalAllocationRequest
SignalSummaryResponse
SignalDetailResponse
```

### Rebalancing

```text
RebalancePreviewResponse
RebalancePreviewItemResponse
ExecuteRebalanceResponse
```

### Transaction

```text
TransactionResponse
PagedTransactionResponse
```

### Snapshot

```text
SnapshotSummaryResponse
SnapshotDetailResponse
SnapshotItemResponse
```

### Common

```text
ApiErrorResponse
FieldErrorResponse
PageResponse<T>
```

---

## 12. Pagination

Endpoints של היסטוריה יחזירו Pagination.

### Query Parameters

```text
page
size
sort
```

### ברירות מחדל

```text
page=0
size=20
```

### גודל מקסימלי

```text
size <= 100
```

### Response

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

---

## 13. הרשאות לפי Endpoint

| Endpoint | Public | Authenticated | Owner Check |
|---|---:|---:|---:|
| POST /auth/register | Yes | No | No |
| POST /auth/login | Yes | No | No |
| GET /users/me | No | Yes | No |
| GET /dashboard | No | Yes | No |
| GET /portfolios | No | Yes | Automatic |
| POST /portfolios/by-amount | No | Yes | Automatic |
| POST /portfolios/by-holdings | No | Yes | Automatic |
| GET /portfolios/{id} | No | Yes | Yes |
| PATCH /portfolios/{id} | No | Yes | Yes |
| DELETE /portfolios/{id} | No | Yes | Yes |
| Signal endpoints | No | Yes | Yes |
| Rebalancing endpoints | No | Yes | Yes |
| Transaction endpoints | No | Yes | Yes |
| Snapshot endpoints | No | Yes | Yes |

---

## 14. HTTP Status Standards

### 200 OK

- GET הצליח.
- PATCH הצליח.
- Preview הצליח.
- Execute הצליח.

### 201 Created

- משתמש נוצר.
- תיק נוצר.
- איתות נוצר.

### 204 No Content

- מחיקה הצליחה.

### 400 Bad Request

- נתונים לא תקינים.
- משקל אינו 100%.
- לא בדיוק 10 מניות.

### 401 Unauthorized

- Token חסר.
- Token לא תקין.
- Login נכשל.

### 403 Forbidden

יש להשתמש בזה רק כאשר המשתמש מזוהה אך אסור לו לבצע פעולה כללית.

בבדיקת בעלות על תיק עדיף להחזיר:

```text
404 Not Found
```

כדי לא לחשוף שהמשאב קיים.

### 404 Not Found

- תיק לא קיים או לא שייך למשתמש.
- איתות לא קיים או לא שייך למשתמש.
- עסקה לא קיימת.
- Snapshot לא קיים.

### 409 Conflict

- אימייל קיים.
- שם תיק קיים.
- איתות כבר בוצע.
- סטטוס אינו מאפשר פעולה.

---

## 15. Validation מרכזי

### תיק

```text
שם תיק אינו ריק
סכום גדול מ-0
בדיוק 10 מניות
אין סימולים כפולים
מחיר גדול מ-0
כמות גדולה מ-0
משקל בין 0 ל-100
סכום משקלים 100%
```

### איתות

```text
תאריך חובה
בדיוק 10 מניות
אין סימולים כפולים
מחיר גדול מ-0
משקל בין 0 ל-100
סכום משקלים 100%
```

### Auth

```text
שם מלא חובה
אימייל תקין
סיסמה לפחות 8 תווים
```

---

## 16. נרמול קלט

### Email

```text
trim
lowercase
```

### Symbol

```text
trim
uppercase
```

### Name

```text
trim
```

### Decimal

ה-Backend יקבע Scale בהתאם לסוג הערך.

אין לסמוך על עיגול שמגיע מה-Frontend.

---

## 17. Idempotency

### Execute Signal

ביצוע איתות חייב להיות מוגן מביצוע כפול.

אם מתקבלת בקשה נוספת לאחר שהאיתות בוצע:

```http
409 Conflict
```

```json
{
  "error": "SIGNAL_ALREADY_EXECUTED",
  "message": "The signal has already been executed"
}
```

### Preview

Preview יכול להתבצע כמה פעמים.

---

## 18. סדר פיתוח ה-API

### Phase 1

```text
POST /auth/register
POST /auth/login
GET /users/me
GET /health
```

### Phase 2

```text
GET /portfolios
POST /portfolios/by-amount
POST /portfolios/by-holdings
GET /portfolios/{id}
PATCH /portfolios/{id}
DELETE /portfolios/{id}
```

### Phase 3

```text
GET /portfolios/{id}/signals
POST /portfolios/{id}/signals
GET /portfolios/{id}/signals/{signalId}
PUT /portfolios/{id}/signals/{signalId}
DELETE /portfolios/{id}/signals/{signalId}
```

### Phase 4

```text
POST /portfolios/{id}/signals/{signalId}/preview
POST /portfolios/{id}/signals/{signalId}/execute
```

### Phase 5

```text
GET /portfolios/{id}/transactions
GET /portfolios/{id}/snapshots
GET /dashboard
```

---

## 19. Health Endpoint

### Endpoint

```http
GET /api/v1/health
```

### הרשאה

```text
Public
```

### Response

```http
200 OK
```

```json
{
  "status": "UP",
  "service": "sharecutter-backend",
  "timestamp": "2026-07-24T14:30:00Z"
}
```

---

## 20. הרחבה עתידית למחירי מניות

בעתיד ניתן להוסיף:

```http
GET /api/v1/stocks/{symbol}/price
POST /api/v1/portfolios/{portfolioId}/refresh-prices
```

### דוגמה

```json
{
  "symbol": "NVDA",
  "price": 140.2500,
  "source": "EXTERNAL_API",
  "priceDate": "2026-07-24"
}
```

Endpoints אלו אינם חלק מה-MVP.

---

## 21. קריטריוני קבלה ל-API

ה-API ייחשב תקין כאשר:

- משתמש יכול להירשם.
- משתמש יכול להתחבר ולקבל JWT.
- Token נדרש למסלולים פרטיים.
- שני משתמשים לא יכולים לגשת לאותם תיקים.
- ניתן ליצור כמה תיקים.
- ניתן ליצור תיק בשתי השיטות.
- לא ניתן ליצור תיק עם פחות או יותר מ-10 מניות.
- לא ניתן ליצור תיק שמשקליו אינם 100%.
- ניתן ליצור איתות אישי.
- ניתן לקבל Preview ללא שינוי נתונים.
- ניתן לבצע איתות פעם אחת בלבד.
- עסקאות נשמרות.
- Snapshots נשמרים.
- תגובות שגיאה אחידות.
- כל Endpoint מחזיר HTTP Status מתאים.

---

## 22. הצעד הבא

המסמך הבא:

```text
05_FRONTEND_SCREENS.md
```

יכלול:

- מסכי Angular.
- מבנה כל מסך.
- שדות וטפסים.
- ניווט.
- טבלאות.
- הודעות שגיאה.
- זרימת משתמש.
- סדר בניית הרכיבים.
