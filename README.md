# Digital Wallet System — Microservices Architecture Backend

A production-oriented, microservices-based **Digital Wallet System** built with **Java 21**, **Spring Boot 3.2.x**, **Spring Cloud 2023.x**, **Spring Security + JJWT**, **PostgreSQL 16**, and **Docker**.

The system enables secure financial wallet operations including balance initialization, top-ups, high-concurrency fund transfers, role-based administration, and detailed transaction auditing.

---

## 📋 Table of Contents
1. [Project Overview](#1-project-overview)
2. [Tech Stack](#2-tech-stack)
3. [Architecture](#3-architecture)
4. [Project Structure](#4-project-structure)
5. [Database Structure](#5-database-structure)
6. [Authentication Flow](#6-authentication-flow)
7. [Wallet Transfer Workflow](#7-wallet-transfer-workflow)
8. [Setup Instructions](#8-setup-instructions)
9. [API Documentation](#9-api-documentation)
10. [Testing](#10-testing)
11. [Configuration](#11-configuration)
12. [Project Workflow](#12-project-workflow)
13. [Git Development Workflow](#13-git-development-workflow)
14. [Deployment](#14-deployment)

---

## 1. Project Overview

The **Digital Wallet System** is designed to demonstrate high-concurrency, fault-tolerant financial transactions across independently deployable microservices.

### Main Features
- **User Authentication**: Sign up and login with BCrypt password hashing and 24-hour stateless HS256 JWT issuance.
- **Wallet Balance Management**: Real-time balance updates backed by `@Version` **optimistic locking** to prevent race conditions or double-spending under concurrent transfer requests.
- **Fund Transfers**: Atomic transfer orchestration checking sender balance, validating accounts, and performing sequential debits and credits.
- **Transaction Auditing**: Complete historical tracking of transfers (`SUCCESS` / `FAILED`).
- **Role-Based Access Control (RBAC)**: Enforced centrally at the API Gateway (`USER` for personal wallet access, `ADMIN` for freezing wallets and viewing system-wide transaction logs).
- **Service Discovery & Routing**: Dynamic discovery via Eureka and client-side load balancing via Spring Cloud Gateway.

### Why Microservices?
- **Independent Scalability**: Wallet and Transaction services experience higher traffic than User Profile or Auth services and can be scaled independently.
- **Fault Isolation**: Outages in non-critical components do not impact authentication or core balance safety.
- **Decoupled Data Storage**: Each microservice owns its dedicated PostgreSQL database schema, preventing shared database coupling.

---

## 2. Tech Stack

- **Core Language & Framework**: Java 21, Spring Boot 3.2.3
- **Service Discovery**: Spring Cloud Netflix Eureka Server (`2023.0.0`)
- **API Gateway & Routing**: Spring Cloud Gateway, Reactive WebFlux
- **Inter-Service Communication**: Spring Cloud OpenFeign, Spring Cloud LoadBalancer
- **Security & Authorization**: Spring Security, JJWT (Java JWT `0.11.5`), BCrypt Password Hashing
- **Data Access & Persistence**: Spring Data JPA, Hibernate, PostgreSQL 16
- **Developer Tooling**: Spring Boot DevTools
- **Build Tool**: Apache Maven (Multi-Module Project)
- **Containerization & Orchestration**: Docker, Docker Compose
- **Testing Frameworks**: JUnit 5, Mockito, Spring Boot Test, Postman E2E Test Suite

---

## 3. Architecture

The system follows a decoupled microservice architecture where all client requests route through the **API Gateway**:

```text
                               ┌─────────────────┐
                               │   Client / UI   │
                               └────────┬────────┘
                                        │
                                        ▼ (Port 8080)
                               ┌─────────────────┐
                               │   API Gateway   │  (JWT Filter & RBAC)
                               └────────┬────────┘
                                        │
                 ┌──────────────────────┼──────────────────────┐
                 │                      │                      │
                 ▼                      ▼                      ▼
        ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
        │  Auth Service   │    │ Wallet Service  │    │Transaction Serv.│
        │   (Port 8081)   │    │   (Port 8082)   │    │   (Port 8083)   │
        └────────┬────────┘    └────────┬────────┘    └────────┬────────┘
                 │                      │                      │
                 ▼                      ▼                      ▼
          ┌─────────────┐        ┌─────────────┐        ┌─────────────┐
          │  auth_db    │        │  wallet_db  │        │transaction_db│
          │ (Port 5433) │        │ (Port 5434) │        │ (Port 5435) │
          └─────────────┘        └─────────────┘        └─────────────┘

                                ┌─────────────────┐
                                │  User Service   │
                                │   (Port 8084)   │
                                └────────┬────────┘
                                         │
                                         ▼
                                  ┌─────────────┐
                                  │   user_db   │
                                  │ (Port 5436) │
                                  └─────────────┘

                                ┌─────────────────┐
                                │  Eureka Server  │  (Service Discovery)
                                │   (Port 8761)   │
                                └─────────────────┘
```

### Inter-Service Request Pipeline
**Client** ➡️ **API Gateway** ➡️ **Transaction Service** ➡️ **Wallet Service** ➡️ **User Service**

---

## 4. Project Structure

```text
digital-wallet-system/
├── pom.xml                     # Parent POM managing Java 21, Spring Boot & Spring Cloud BOM
├── docker-compose.yml          # Container orchestration for 4 Postgres DBs + 6 microservices
├── .gitignore                  # Git exclusion rules
├── eureka-server/              # Service Discovery Registry (Port 8761)
├── api-gateway/                # Central Gateway, JWT Filter, RBAC (Port 8080)
├── auth-service/               # Signup, Login, Password Hashing, JWT issuance (Port 8081)
├── wallet-service/             # Wallet balance, Optimistic Locking, Topup/Debit/Credit (Port 8082)
├── transaction-service/        # Fund transfers, Feign client orchestration, History (Port 8083)
├── user-service/               # User profile management (Port 8084)
└── postman/                    # Automated Postman E2E testing collection & README
```

---

## 5. Database Structure

The platform uses **Database-per-Service** architecture with 4 separate PostgreSQL 16 databases:

### 1. User Table (`auth_db` & `user_db`)
- `id` (BIGINT, Primary Key)
- `name` (VARCHAR, Not Null)
- `email` (VARCHAR, Unique, Not Null)
- `password_hash` (VARCHAR, Not Null — Auth Service only)
- `role` (VARCHAR, Not Null — `USER` or `ADMIN`)
- `created_at` (TIMESTAMP)

### 2. Wallet Table (`wallet_db`)
- `id` (BIGINT, Primary Key)
- `user_id` (BIGINT, Unique, Not Null)
- `balance` (NUMERIC/DECIMAL, Not Null, Default `0.00`)
- `status` (VARCHAR, Not Null — `ACTIVE` or `FROZEN`)
- `version` (BIGINT, Not Null — `@Version` Optimistic Lock counter)
- `updated_at` (TIMESTAMP)

### 3. Transaction Table (`transaction_db`)
- `id` (BIGINT, Primary Key)
- `sender_id` (BIGINT, Not Null)
- `receiver_id` (BIGINT, Not Null)
- `amount` (NUMERIC/DECIMAL, Not Null)
- `status` (VARCHAR, Not Null — `SUCCESS` or `FAILED`)
- `created_at` (TIMESTAMP)

---

## 6. Authentication Flow

```text
   Client                  Auth Service               API Gateway              Microservices
     │                          │                          │                         │
     │── 1. POST /auth/login ──>│                          │                         │
     │   (email, password)      │                          │                         │
     │                          │── 2. Verify BCrypt ────>│                          │
     │                          │   Generate HS256 JWT     │                         │
     │<── 3. Return JWT Token ──│                          │                         │
     │                          │                          │                         │
     │── 4. Request with Header Authorization: Bearer <JWT> ─>│                         │
     │                                                     │── 5. Validate Signature │
     │                                                     │   & Check RBAC Role    │
     │                                                     │── 6. Forward Request ──>│
     │<───────────────────── 7. Return Protected Response ───────────────────────────│
```

---

## 7. Wallet Transfer Workflow

```text
 Client            Gateway         Transaction Service         Wallet Service         User Service
   │                  │                     │                        │                     │
   │── POST /transfer>│                     │                        │                     │
   │   (sender, rcvr) │── Validate JWT ────>│                        │                     │
   │                  │   & Forward Request │── 1. Verify Sender ───>│                     │
   │                  │                     │      & Receiver        │                     │
   │                  │                     │      Profiles          │────────────────────>│
   │                  │                     │<── Profiles OK ────────│<────────────────────│
   │                  │                     │                        │                     │
   │                  │                     │── 2. Fetch Balance ───>│                     │
   │                  │                     │<── Sender Balance ─────│                     │
   │                  │                     │                        │                     │
   │                  │                     │── 3. Debit Sender ────>│ (Optimistic Lock)   │
   │                  │                     │<── Debit OK ───────────│                     │
   │                  │                     │                        │                     │
   │                  │                     │── 4. Credit Receiver ─>│                     │
   │                  │                     │<── Credit OK ──────────│                     │
   │                  │                     │                        │                     │
   │                  │                     │── 5. Record SUCCESS ───│                     │
   │<── Return 200 OK ┴─────────────────────│   in transaction_db    │                     │
```

---

## 8. Setup Instructions

### Prerequisites
- **Java 21 JDK** installed
- **Apache Maven 3.9+** installed
- **Docker & Docker Compose** installed
- **Postman** (optional, for testing)

### Step-by-Step Local Startup

1. **Clone Repository**:
   ```bash
   git clone https://github.com/your-username/digital-wallet-system.git
   cd digital-wallet-system
   ```

2. **Build Maven Multi-Module Project**:
   ```bash
   mvn clean package -DskipTests
   ```

3. **Start Infrastructure & Microservices via Docker Compose**:
   ```bash
   docker-compose up --build
   ```

4. **Verify Eureka Dashboard**:
   Open browser at [http://localhost:8761](http://localhost:8761) and verify the following services are registered:
   - `EUREKA-SERVER`
   - `AUTH-SERVICE`
   - `USER-SERVICE`
   - `WALLET-SERVICE`
   - `TRANSACTION-SERVICE`
   - `API-GATEWAY`

---

## 9. API Documentation

All APIs are accessed through the **API Gateway** on `http://localhost:8080`.

| Microservice | Method | Endpoint | Auth Required | Description |
| :--- | :--- | :--- | :---: | :--- |
| **Auth** | `POST` | `/auth/signup` | No | Registers user & initializes wallet with balance 0 |
| **Auth** | `POST` | `/auth/login` | No | Authenticates credentials & returns HS256 JWT |
| **User** | `GET` | `/users/{id}` | Yes (`USER`) | Retrieves user profile |
| **User** | `PUT` | `/users/{id}` | Yes (`USER`) | Updates user profile name |
| **Wallet** | `GET` | `/wallets/{userId}` | Yes (`USER`) | Returns current wallet balance and status |
| **Wallet** | `POST` | `/wallets/{userId}/topup` | Yes (`USER`) | Tops up wallet balance (body: `{"amount": 500}`) |
| **Wallet** | `PUT` | `/wallets/{userId}/freeze` | Yes (`ADMIN`) | Blocks transactions on specified wallet |
| **Wallet** | `PUT` | `/wallets/{userId}/unfreeze` | Yes (`ADMIN`) | Restores wallet to `ACTIVE` status |
| **Transaction** | `POST` | `/transactions/transfer` | Yes (`USER`) | Transfers money (`{"senderId":1,"receiverId":2,"amount":200}`) |
| **Transaction** | `GET` | `/transactions/user/{userId}` | Yes (`USER`) | Returns user transaction history (newest first) |
| **Transaction** | `GET` | `/transactions/all` | Yes (`ADMIN`) | Returns all system transactions |

---

## 10. Testing

### Running Unit & Integration Tests
Execute the Maven test runner across all modules:
```bash
mvn test
```

### Test Coverage Highlights
- **`WalletServiceTest`**: Validates top-ups, debits, credits, insufficient balance rejection, and frozen wallet protection.
- **`AuthServiceTest`**: Tests BCrypt password verification, duplicate email rejection, and JWT generation.
- **`TransactionServiceTest`**: Mocks Feign clients to test successful transfers, self-transfer rejection, and insufficient balance failure recording.
- **`UserServiceTest`**: Validates profile lookup and updates.

### Postman Automated E2E Test Suite
Import `postman/digital-wallet.postman_collection.json` into Postman and execute the 12 sequential tests covering:
1. Signup Alice & Bob
2. Login Alice & Bob -> capture JWTs
3. Verify initial balance (0)
4. Topup Alice balance (500)
5. Transfer 200 from Alice to Bob -> verify status `SUCCESS`
6. Verify updated balances (Alice: 300, Bob: 200)
7. Check transaction history
8. Attempt transfer exceeding balance (10000) -> expect `400 Bad Request`
9. Attempt non-admin access to `/transactions/all` -> expect `403 Forbidden`

---

## 11. Configuration

Sensitive values and environment settings are configurable via environment variables:

| Environment Variable | Description | Default Fallback |
| :--- | :--- | :--- |
| `JWT_SECRET` | Secret key for HS256 JWT signing/validation | `SecretKeyForDigitalWalletSystem...` |
| `SPRING_DATASOURCE_URL` | JDBC database connection string | `jdbc:postgresql://localhost:<port>/<db>` |
| `SPRING_DATASOURCE_USERNAME` | Database username | `wallet` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | `wallet` |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | Eureka server registry URL | `http://localhost:8761/eureka/` |

---

## 12. Project Workflow

1. User registers via `POST /auth/signup`. Auth Service saves credentials and invokes `Wallet Service` to auto-initialize a wallet with balance 0.
2. User authenticates via `POST /auth/login` and receives a JWT token containing claims `userId` and `role`.
3. User tops up balance via `POST /wallets/{userId}/topup`.
4. User initiates a fund transfer via `POST /transactions/transfer`.
5. Transaction Service validates sender/receiver profiles (`User Service`), checks balance & status (`Wallet Service`), debits sender, credits receiver, and logs transaction state.
6. Admin users with `ADMIN` role access system-wide auditing (`GET /transactions/all`) or freeze wallets (`PUT /wallets/{userId}/freeze`).

---

## 13. Git Development Workflow

The repository follows **Conventional Commits** (`feat`, `fix`, `config`, `test`, `docs`, `chore`).

Commit history is maintained incrementally:
- Small, atomic commits per component/layer.
- Logical separation of model, repository, service, controller, and configuration.
- Clean working directory with strict `.gitignore` enforcement.

---

## 14. Deployment

### Production Docker Deployment
Deploy all microservices and databases with a single command:
```bash
docker-compose up -d --build
```

### Shutdown Stack
```bash
docker-compose down -v
```
