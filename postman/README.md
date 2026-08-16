# Digital Wallet System — Postman Testing Guide

This directory contains the Postman collection to test the Digital Wallet System microservices end-to-end through the API Gateway (`http://localhost:8080`).

## Instructions to Run

1. **Start the System**:
   Ensure all microservices and databases are up and running (e.g. via `docker-compose up --build` or starting each Spring Boot service).
   Verify Eureka Dashboard at [http://localhost:8761](http://localhost:8761) shows all services registered:
   - `AUTH-SERVICE`
   - `USER-SERVICE`
   - `WALLET-SERVICE`
   - `TRANSACTION-SERVICE`
   - `API-GATEWAY`

2. **Import Collection**:
   - Open Postman.
   - Click **Import** in the top left corner.
   - Select `digital-wallet.postman_collection.json`.

3. **Run Requests in Order**:
   The collection uses automated Postman test scripts to extract variables (`aliceUserId`, `bobUserId`, `aliceToken`, `bobToken`) from previous responses and pass them into subsequent requests.

   Execute the requests in numerical sequence:
   1. `POST /auth/signup` (Alice) -> Stores `aliceUserId`
   2. `POST /auth/signup` (Bob) -> Stores `bobUserId`
   3. `POST /auth/login` (Alice) -> Stores `aliceToken`
   4. `POST /auth/login` (Bob) -> Stores `bobToken`
   5. `GET /wallets/{aliceUserId}` -> Asserts initial balance is `0`
   6. `POST /wallets/{aliceUserId}/topup` (Amount: 500) -> Asserts updated balance is `500`
   7. `POST /transactions/transfer` (Alice -> Bob, Amount: 200) -> Asserts status is `SUCCESS`
   8. `GET /wallets/{aliceUserId}` -> Asserts balance is `300`
   9. `GET /wallets/{bobUserId}` -> Asserts balance is `200`
   10. `GET /transactions/user/{aliceUserId}` -> Asserts transaction history is present
   11. `POST /transactions/transfer` (Insufficient funds: 10000) -> Asserts `400 Bad Request`
   12. `GET /transactions/all` (Using non-admin token) -> Asserts `403 Forbidden`
