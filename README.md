# Chamcham Backend (Spring Boot + PostgreSQL)

This module migrates the original Node.js + MongoDB backend to **Java 21**, **Spring Boot**, and **PostgreSQL** using a clean layered architecture.

## Tech Stack

- Java 21
- Spring Boot 3.3.x
- Spring Data JPA (Hibernate)
- Spring Security + JWT (cookie or Bearer)
- PostgreSQL
- Flyway
- Gradle
- Lombok
- Jakarta Validation

## Layered Structure

- `controller/` REST API layer
- `service/` business logic and orchestration
- `repository/` JPA repositories
- `entity/` relational model
- `dto/` request/response payloads
- `mapper/` entity-to-dto transformations
- `config/` app and security configuration
- `exception/` global error handling
- `util/` shared utilities (`PageResponse`)

## Why UUID Primary Keys

The original MongoDB models rely on globally unique identifiers and frontend-facing IDs. UUID keys preserve this behavior and simplify migration because clients can keep using opaque identifiers without sequence coupling.

## Mongo -> PostgreSQL Mapping

- `User` -> `users`
- `Gig` -> `gigs`
- `Review` -> `reviews` (unique `(gig_id, reviewer_id)`)
- `Order` -> `orders`
- `Conversation` -> `conversations` (unique `(seller_id, buyer_id)`)
- `Message` -> `messages`
- Embedded arrays (`images`, `features`) normalized into `gig_images` and `gig_features`
- Auditing fields (`created_at`, `updated_at`) added to all tables

Schema is managed by Flyway in `src/main/resources/db/migration/V1__init_schema.sql` and created under PostgreSQL schema `core`.

## Example Endpoints

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/auth/me`
- `POST /api/gigs`
- `GET /api/gigs/single/{id}`
- `GET /api/gigs?page=0&size=10&sort=createdAt`
- `POST /api/reviews`
- `GET /api/orders`
- `POST /api/orders/create-payment-intent/{gigId}`
- `PATCH /api/orders`
- `GET /api/conversations`
- `POST /api/messages`

## Configuration

Use environment variables (defaults in `application.yml`):

- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `JWT_SECRET_BASE64`, `JWT_EXPIRATION_MS`
- `COOKIE_SECURE`, `COOKIE_SAME_SITE`, `COOKIE_MAX_AGE`
- `CORS_ALLOWED_ORIGINS`

## Run

```bash
cd /Users/sajjadkhan/Documents/chamcham/clone/springboot
./gradlew bootRun
```

## Test

```bash
cd /Users/sajjadkhan/Documents/chamcham/clone/springboot
./gradlew test
```

## Migration Improvements Introduced

- Business logic moved out of controllers into dedicated services
- DTO boundaries introduced to avoid exposing entities
- Global exception handling with consistent API error payloads
- Input validation on request DTOs
- Pagination and sorting for gigs list
- JWT authentication integrated with stateless Spring Security

