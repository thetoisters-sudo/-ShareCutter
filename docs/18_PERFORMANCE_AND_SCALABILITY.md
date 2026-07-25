# 18_PERFORMANCE_AND_SCALABILITY.md

# ShareCutter - Performance & Scalability

## מטרה

להגדיר יעדי ביצועים ויכולת הרחבה כבר בשלב התכנון.

---

# יעדי SLA

| פעולה | יעד |
|---|---:|
| Login | < 500ms |
| Dashboard | < 1s |
| Create Portfolio | < 2s |
| Preview Rebalance | < 2s |
| Execute Rebalance | < 3s |

---

# עומסי תכנון (MVP)

- עד 10,000 משתמשים רשומים
- עד 100,000 עסקאות
- עד 1,000,000 Snapshot Items
- 10 אחזקות לכל תיק

---

# צווארי בקבוק צפויים

- חישובי Rebalance
- טעינת Dashboard
- שאילתות Snapshot גדולות

פתרון:
- אינדקסים מתאימים
- Pagination
- Projection Queries

---

# אינדקסים קריטיים

- users(email)
- portfolios(owner_id)
- holdings(portfolio_id, symbol)
- weekly_signals(portfolio_id, signal_week)
- trade_transactions(portfolio_id, created_at)
- portfolio_snapshots(portfolio_id, created_at)

---

# Cache

MVP:
- ללא Cache

עתידי:
- Redis
- Cache לנתוני Dashboard
- Cache למחירי מניות

---

# Scaling Strategy

שלב 1:
- שרת יחיד
- PostgreSQL יחיד

שלב 2:
- Read Replicas
- Redis

שלב 3:
- Horizontal Scaling
- Load Balancer

---

# Monitoring KPIs

- Response Time
- Error Rate
- CPU
- Memory
- DB Connections
- Slow Queries

---

# Performance Checklist

- BigDecimal בלבד לכסף
- Pagination לכל רשימה
- N+1 Queries אסורות
- Lazy/Eager לפי צורך
- Batch Updates כאשר מתאים

---

# הצעד הבא

19_FINAL_ARCHITECTURE_SUMMARY.md
