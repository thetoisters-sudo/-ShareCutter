# ShareCutter

## Investment Portfolio Management & Simulation Platform

**ShareCutter** is a full-stack investment portfolio management and simulation platform built with Angular, Spring Boot and PostgreSQL.

It allows authenticated users to create and manage investment portfolios, work with live market data, buy and sell fractional shares, manage target allocations, track transactions and portfolio history, and view aggregated investment information through a central dashboard.

## Live Application

**Production:**  
https://sharecutter-frontend-production.up.railway.app/

**Source Code:**  
https://github.com/thetoisters-sudo/-ShareCutter

> The production application is deployed on Railway with separate frontend, backend and PostgreSQL services.

---

# Overview

ShareCutter was developed as a full-stack software engineering project.

The project evolved from basic portfolio CRUD functionality into a connected investment-management system in which financial operations affect holdings, cash balances, portfolio values, analytics and history.

One of the central architectural principles is:

> **Transactions are the source of truth for portfolio state.**

Instead of treating transactions, holdings and portfolio values as unrelated records, ShareCutter connects them into a single portfolio lifecycle.

```text
User Action
    ג”‚
    ג–¼
Angular Frontend
    ג”‚
    ג–¼
REST API
    ג”‚
    ג–¼
Spring Boot Backend
    ג”‚
    ג”ג”€ג”€ Authentication
    ג”ג”€ג”€ Portfolio Management
    ג”ג”€ג”€ Asset Management
    ג”ג”€ג”€ Transactions
    ג”ג”€ג”€ Allocation Engine
    ג”ג”€ג”€ Market Data
    ג””ג”€ג”€ Portfolio Analytics
    ג”‚
    ג–¼
PostgreSQL
```

---

# Main Features

## Authentication

- User registration
- Login
- JWT authentication
- Access and refresh tokens
- Protected frontend routes
- Spring Security backend protection
- User-specific portfolio data
- Persistent production accounts

## Dashboard

- Aggregated overview of the user's portfolios
- Current portfolio values
- Portfolio analytics
- Portfolio performance information
- Synchronization after portfolio changes
- Market-data refresh

## Portfolio Management

- Multiple portfolios per user
- Create portfolios from an investment amount
- Create portfolios from existing holdings
- Portfolio editing
- Portfolio deletion using soft delete
- Cash management
- Portfolio-specific allocations
- Portfolio transaction history
- Current holdings calculation

## Assets

- Portfolio-specific assets
- Stocks
- ETFs
- Cryptocurrency and supported asset types
- Asset creation and editing
- Symbol and company search
- Live market-price integration
- Cryptocurrency pairs such as BTC/USD
- Current-price refresh

## Transactions

The transaction system supports financial operations including:

- BUY
- SELL
- DIVIDEND
- FEE
- DEPOSIT
- WITHDRAWAL

Transactions update the financial state of the portfolio rather than existing only as bookkeeping records.

Transaction history can be filtered and inspected per portfolio.

---

# Fractional Shares

ShareCutter supports fractional-share investing with high numerical precision.

Users can work in both directions.

### Quantity to amount

```text
Total Amount = Quantity ֳ— Current Market Price
```

### Amount to quantity

```text
Quantity = Total Amount / Current Market Price
```

For example, an allocation does not need to result in exactly 5 or 10 shares.

A transaction can contain quantities such as:

```text
6.7692687 AAPL
```

or:

```text
0.39230895 AAPL
```

Fractional quantities remain part of the portfolio state and are persisted in PostgreSQL.

---

# Portfolio Allocation

ShareCutter contains an allocation workflow for adjusting a portfolio toward target asset weights.

A user can:

1. Select a portfolio.
2. Select an asset.
3. Define a target portfolio weight.
4. Retrieve the current market price.
5. Preview the required transaction.
6. Execute the allocation.

The system determines whether reaching the requested target requires a BUY or SELL transaction and calculates the required fractional quantity.

Example:

```text
Current Portfolio
        ג”‚
        ג–¼
Target Asset Weight
        ג”‚
        ג–¼
Current Market Price
        ג”‚
        ג–¼
Required Position Value
        ג”‚
        ג–¼
BUY / SELL Difference
        ג”‚
        ג–¼
Fractional Quantity
        ג”‚
        ג–¼
Transaction
        ג”‚
        ג–¼
Updated Portfolio
```

Allocation-generated operations are stored as normal portfolio transactions and therefore appear in transaction history.

---

# Live Market Data

ShareCutter integrates external market data through the backend.

Market-data functionality includes:

- Symbol search
- Company information
- Current market prices
- Stock data
- Supported cryptocurrency pairs
- Portfolio price refresh

The current production deployment uses a **Twelve Data API key stored as a backend environment variable**.

API credentials are not stored in the frontend application or committed to the repository.

---

# Portfolio History

Each portfolio contains a transaction-based history.

The history records financial operations such as:

- Initial cash
- Initial holdings
- Purchases
- Sales
- Allocation-generated purchases
- Allocation-generated sales

Users can inspect quantities, prices, total transaction amounts, dates and transaction identifiers.

Portfolio history can also be exported as CSV.

---

# Internationalization

ShareCutter supports:

- English
- Hebrew
- RTL layout for Hebrew
- Runtime language switching

The interface automatically adapts its layout direction to the selected language.

---

# Theme Support

The frontend supports light and dark interface modes.

Theme selection is available directly from the application navigation.

---

# Technology Stack

## Frontend

- Angular
- TypeScript
- HTML
- CSS
- Angular Router
- Reactive Forms
- HTTP Client
- Route Guards

## Backend

- Java
- Spring Boot
- Spring Web
- Spring Security
- Spring Data JPA
- Bean Validation
- JWT authentication
- Flyway
- Maven

## Database

- PostgreSQL
- UUID identifiers
- Foreign-key relationships
- Soft deletion
- Flyway schema migrations

## Infrastructure

- Docker
- Docker Compose
- Caddy
- Railway
- Git
- GitHub

## External Services

- Twelve Data market-data API

---

# Architecture

The production system consists of three primary Railway services:

```text
Internet
   ג”‚
   ג–¼
ShareCutter Frontend
Angular + Caddy
   ג”‚
   ג”‚ /api/*
   ג–¼
Railway Private Network
   ג”‚
   ג–¼
ShareCutter Backend
Spring Boot
   ג”‚
   ג–¼
PostgreSQL
```

The browser communicates with the public frontend.

API requests are proxied by Caddy from:

```text
/api/*
```

to the Spring Boot backend over Railway's private network.

This allows the backend and database to remain separated from direct frontend implementation details.

---

# Database Model

The main application entities include:

```text
User
 ג”‚
 ג””ג”€ג”€ Portfolio
      ג”‚
      ג”ג”€ג”€ Asset
      ג”‚
      ג”ג”€ג”€ Transaction
      ג”‚
      ג”ג”€ג”€ Portfolio Holding
      ג”‚
      ג”ג”€ג”€ Portfolio Snapshot
      ג”‚
      ג””ג”€ג”€ Weekly Portfolio Target
```

The database is versioned using Flyway migrations.

---

# Security

ShareCutter uses JWT-based authentication.

The security flow is:

```text
Register / Login
      ג”‚
      ג–¼
Spring Security
      ג”‚
      ג–¼
JWT issued
      ג”‚
      ג–¼
Angular stores authentication state
      ג”‚
      ג–¼
Protected API requests
```

Portfolio resources are associated with their owning users.

Production secrets such as database credentials, JWT secrets and market-data API keys are supplied through environment variables.

---

# Production Deployment

The application is deployed on Railway.

The production environment contains:

```text
ShareCutter-Frontend
ShareCutter-Backend
Postgres
```

The frontend and backend are built and deployed independently.

Production configuration includes environment variables for:

```text
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
JWT_SECRET
JWT_ISSUER
JWT_ACCESS_TOKEN_EXPIRATION
JWT_REFRESH_TOKEN_EXPIRATION
TWELVE_DATA_API_KEY
```

Sensitive values must never be committed to Git.

---

# Running Locally

## Requirements

Install:

- Git
- Docker Desktop
- JDK 21
- Node.js
- npm

Clone the repository:

```bash
https://github.com/thetoisters-sudo/-ShareCutter
cd ShareCutter
```

## Database

Start PostgreSQL using Docker Compose:

```bash
docker compose up -d
```

## Backend

From the backend directory:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

## Frontend

From another terminal:

```powershell
cd frontend
npm.cmd install
npm.cmd start
```

The Angular development application will normally be available at:

```text
http://localhost:4200
```

---

# Build Verification

## Backend

```powershell
cd backend
.\mvnw.cmd clean test
```

or:

```powershell
.\mvnw.cmd -DskipTests compile
```

## Frontend

```powershell
cd frontend
npm.cmd run build
```

---

# Git Workflow

Development was performed using Git feature branches and incremental integration into `main`.

Major development areas included:

```text
database foundation
authentication
portfolio management
asset management
transaction management
frontend portfolios
frontend assets
market data
allocation
production deployment
```

The final production version is maintained on the `main` branch.

---

# Production Verification

The deployed application has been tested through the complete production stack.

Verified flows include:

- Account registration
- Login
- Portfolio creation
- Initial cash creation
- Initial holdings creation
- Live market-price retrieval
- Portfolio editing
- Fractional-share calculations
- BUY transactions
- SELL transactions
- Allocation BUY
- Allocation SELL
- Cash recalculation
- Holdings recalculation
- Portfolio transaction history
- CSV history export
- Logout and login persistence

The verification was performed against the deployed frontend, deployed Spring Boot backend and production PostgreSQL database.

---

# Example Portfolio Lifecycle

A portfolio can begin with:

```text
Cash:        $10,000
AAPL:        10 shares
NVDA:        50 shares
```

The user may then:

```text
SELL AAPL
      ג†“
BUY fractional AAPL
      ג†“
Change target allocation
      ג†“
Execute automatic allocation
      ג†“
Update cash
      ג†“
Update holdings
      ג†“
Persist transaction history
```

All operations remain connected to the same portfolio state.

---

# Project Goals

The purpose of ShareCutter is not to provide financial advice or execute real brokerage transactions.

It is an educational portfolio-management and simulation platform designed to demonstrate:

- Full-stack application architecture
- REST API design
- Authentication and authorization
- Relational database design
- Transaction-driven domain logic
- External API integration
- Fractional-share calculations
- Portfolio allocation algorithms
- Frontend/backend integration
- Containerization
- Cloud deployment

---

# Disclaimer

ShareCutter is a portfolio simulation and educational software project.

It does not execute real securities transactions and does not provide investment, financial or trading advice.

Market information may be delayed, incomplete or unavailable depending on the external market-data provider.

---

# Repository

GitHub:

https://github.com/thetoisters-sudo/-ShareCutter

Production:

https://sharecutter-frontend-production.up.railway.app/

---

## ShareCutter

**Angular | Spring Boot | PostgreSQL | Docker | Railway**
