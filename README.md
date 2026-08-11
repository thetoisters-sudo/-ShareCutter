# ShareCutter

## Investment Portfolio Management & Simulation Platform

ShareCutter is a full-stack investment portfolio management and simulation application.

The system allows authenticated users to create and manage multiple investment portfolios, work with live market data, buy and sell fractional shares, manage portfolio allocations, track transactions and portfolio history, and view aggregated portfolio information through a central dashboard.

---

## Overview

ShareCutter was developed as a full-stack software engineering project using Angular, Spring Boot and PostgreSQL.

The project evolved from basic portfolio CRUD functionality into a connected investment-management system in which transactions drive the state of holdings, portfolio values, analytics and the dashboard.

One of the central architectural principles of the project is:

> **Transactions are the source of truth for portfolio state.**

Instead of treating transactions, holdings and portfolio values as unrelated data, ShareCutter derives portfolio state from the financial operations performed by the user.

---

# Main Features

## Authentication

- User registration
- Login
- JWT authentication
- Protected frontend routes
- Spring Security backend protection
- User-specific portfolio data

## Dashboard

- Aggregated portfolio overview
- Current portfolio values
- Portfolio analytics
- Automatic synchronization after portfolio changes
- Manual market-data refresh

## Portfolio Management

- Multiple portfolios per user
- Create portfolios from an investment amount
- Create portfolios from existing holdings
- Portfolio editing
- Portfolio deletion using soft delete
- Cash management
- Portfolio-specific allocation
- Portfolio history

## Assets

- Portfolio-specific assets
- Stocks
- ETFs
- Cryptocurrency and other supported asset types
- Asset creation and editing
- Market symbol search
- Live market-price integration
- Cryptocurrency pairs such as BTC/USD

## Transactions

Supported transaction types include:

- BUY
- SELL
- DIVIDEND
- FEE
- DEPOSIT
- WITHDRAWAL
- TRANSFER_IN
- TRANSFER_OUT

Transactions affect portfolio state rather than existing only as bookkeeping records.

## Fractional Shares

ShareCutter supports fractional holdings.

Users can work in both directions:

```text
Total Amount = Quantity × Current Market Price