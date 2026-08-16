# Beginner's Guide: Understanding and Testing the Digital Wallet System

Welcome! This guide is written specifically to help beginners understand how this microservices project fits together, where everything is running, and exactly how to test the APIs step-by-step (including what values to change and why).

---

## 🏗️ 1. Project Map: What is Running Where?

Instead of one single application, this project is split into **6 mini-applications (microservices)** that talk to each other. They run on the following ports:

| Service Name | Port | Database Name | Database Port | Description |
| :--- | :--- | :--- | :--- | :--- |
| **Eureka Server** | `9000` | *None* | *None* | The "Registry Book". Every microservice registers itself here so they can locate each other. |
| **API Gateway** | `8080` | *None* | *None* | The "Front Door". You send **all** external API requests here. It checks security (JWT) and routes requests to the correct service. |
| **Auth Service** | `8081` | `auth_db` | `5433` | Handles signups, logins, and issues security passes (JWT tokens). |
| **Wallet Service** | `8082` | `wallet_db` | `5434` | Manages account balances, top-ups, and balance updates. |
| **Transaction Service** | `8083` | `transaction_db` | `5435` | Orchestrates money transfers and stores transaction histories. |
| **User Service** | `8084` | `user_db` | `5436` | Stores user profiles (names and emails). |

---

## 🚦 2. The Step-by-Step API Testing Journey

To test this system, we will simulate a real scenario: **Alice signs up, Bob signs up, Alice logs in, tops up her wallet, and transfers money to Bob.**

You should send all requests to the **API Gateway on port `8080`**.

### Step 1: Create Alice's Account
We need to register a user. Doing this will automatically tell the Wallet Service to initialize a wallet with `0.00` balance.

* **URL**: `http://localhost:8080/auth/signup`
* **HTTP Method**: `POST`
* **Headers**:
  - `Content-Type`: `application/json`
* **Body** (JSON):
  ```json
  {
    "name": "Alice",
    "email": "alice@example.com",
    "password": "password123"
  }
  ```
* **Why do we send this?** This tells the system to create a new user profile. The system will return a success message.

### Step 2: Create Bob's Account
We need another user to receive the transfer.

* **URL**: `http://localhost:8080/auth/signup`
* **HTTP Method**: `POST`
* **Headers**:
  - `Content-Type`: `application/json`
* **Body** (JSON):
  ```json
  {
    "name": "Bob",
    "email": "bob@example.com",
    "password": "password123"
  }
  ```
> 📝 **Beginner Note on IDs**: The database assigns unique numbers (IDs) to users sequentially. Alice will be User ID `1`, and Bob will be User ID `2`. If you run these steps multiple times, these numbers will change to `3`, `4`, etc. **Make sure to change the IDs in the subsequent steps to match whatever IDs are generated for your users.**

### Step 3: Login as Alice to get a Security Token (JWT)
Most endpoints in the system are private. You must login to get a JSON Web Token (JWT) which acts as a temporary entry pass.

* **URL**: `http://localhost:8080/auth/login`
* **HTTP Method**: `POST`
* **Headers**:
  - `Content-Type`: `application/json`
* **Body** (JSON):
  ```json
  {
    "email": "alice@example.com",
    "password": "password123"
  }
  ```
* **Response**:
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiJ9.ey..."
  }
  ```
* **What to do now?** Copy the entire string value of `"token"`.

---

### Step 4: Check Alice's Balance
Let's see if Alice has any money. Since this endpoint is protected, we must present the token.

* **URL**: `http://localhost:8080/wallets/1`  *(Change `1` to Alice's User ID if different)*
* **HTTP Method**: `GET`
* **Headers**:
  - `Authorization`: `Bearer <PASTE_COPIED_TOKEN_HERE>`
* **Why the "Bearer " prefix?** This tells the system what type of token is being presented. Notice the space between `Bearer` and the token.
* **Expected Response**:
  ```json
  {
    "userId": 1,
    "balance": 0.00,
    "status": "ACTIVE"
  }
  ```

---

### Step 5: Add Money (Top-Up) to Alice's Wallet
Let's add `500.00` to Alice's balance so she has enough money to make a transfer.

* **URL**: `http://localhost:8080/wallets/1/topup` *(Change `1` to Alice's User ID)*
* **HTTP Method**: `POST`
* **Headers**:
  - `Authorization`: `Bearer <PASTE_COPIED_TOKEN_HERE>`
  - `Content-Type`: `application/json`
* **Body** (JSON):
  ```json
  {
    "amount": 500.00
  }
  ```
* **Why do we do this?** To load money into the sender's account. The response will show Alice's new balance is `500.00`.

---

### Step 6: Transfer Money from Alice to Bob
Now we transfer `200.00` from Alice to Bob.

* **URL**: `http://localhost:8080/transactions/transfer`
* **HTTP Method**: `POST`
* **Headers**:
  - `Authorization`: `Bearer <PASTE_COPIED_TOKEN_HERE>`
  - `Content-Type`: `application/json`
* **Body** (JSON):
  ```json
  {
    "senderId": 1,
    "receiverId": 2,
    "amount": 200.00
  }
  ```
* **Values to change here**:
  - If Alice's ID is different, change `senderId`.
  - If Bob's ID is different, change `receiverId`.
  - Change `amount` to whatever value you want to transfer.
* **Expected Response**:
  ```json
  {
    "transactionId": 1,
    "senderId": 1,
    "receiverId": 2,
    "amount": 200.00,
    "status": "SUCCESS"
  }
  ```

---

### Step 7: Verify History and Balances
- **Bob's Balance**: Send a `GET` request to `http://localhost:8080/wallets/2` (with Alice's token in headers). His balance should now be `200.00`.
- **Alice's Balance**: Send a `GET` request to `http://localhost:8080/wallets/1`. Her balance should now be `300.00`.
- **History**: Send a `GET` request to `http://localhost:8080/transactions/user/1`. It will show a list of transactions involving Alice.

---

## ⚠️ 3. Common Beginner Issues (Gotchas)

### 1. `401 Unauthorized` Response
* **Why?**: Either you forgot to add the `Authorization` header, or you pasted the raw token without typing the word **`Bearer`** followed by a space first.
* **Correct Format**: `Bearer eyJhbGciOiJIUzI1NiJ9.ey...`

### 2. `400 Bad Request` with `"Insufficient funds"`
* **Why?**: You tried to transfer money before completing **Step 5 (Top-Up)**, or you tried to transfer an amount larger than your wallet balance.

### 3. Service registry not visible at `localhost:9000`
* **Why?**: The Eureka Server is the central lookup server. If it is stopped, the other services cannot locate each other. Make sure `eureka-server` is started first and is running on port `9000`.

### 4. `500 Internal Server Error` or Feign exception during transfer
* **Why?**: One of the downstream microservices is not running. In order to complete a transfer, the API Gateway, Transaction Service, Wallet Service, and User Service must all be active and registered with Eureka.
