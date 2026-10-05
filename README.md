# distributed-social-feed

A minimal social feed (signup, post, follow, like, read a feed) built as a vehicle to demonstrate correct, defensible system-design patterns — idempotency, a transactional outbox, Kafka-based fan-out, Redis cache-aside with stampede protection, cache/DB reconciliation, and refresh-token rotation. Feature set is deliberately small; the backend is the point.

## Stack

- **Backend:** Java 21, Spring Boot 3, PostgreSQL, Redis-compatible Valkey, Apache Kafka, Flyway
- **Frontend:** React 18, Vite, TypeScript, Redux Toolkit, RTK Query
- **Infra:** Aiven (Kafka + Postgres + Valkey), Render (API), Vercel (frontend) — all free tier, no card required

## What each feature is actually demonstrating

- **Create post** — idempotency key (atomic Redis reservation) prevents duplicate posts on retry/double-click; a transactional outbox guarantees the post and its fan-out event commit together or not at all.
- **Feed fan-out** — a Kafka consumer pushes new posts into followers' cached timelines, decoupling the write path from fan-out cost.
- **Read feed** — Redis cache-aside with a short-lived lease on cache miss, so a cold cache under concurrent load triggers one rebuild instead of a stampede against the database.
- **Like/unlike** — symmetric, idempotent at the database layer; a scheduled job periodically reconciles the fast Redis counter against the `likes` table, the source of truth.
- **Signup** — a Bloom filter gives an instant "probably available" username check before the database is ever consulted.
- **Refresh tokens** — rotate on every use; presenting an already-rotated token revokes the entire token family (theft detection).

## Project structure

```
backend/   Spring Boot API
frontend/  React app
```

## Status

Backend (auth, posts/outbox/fan-out, feed, follows, likes) and frontend are built and tested. Observability and deployment are in progress.
