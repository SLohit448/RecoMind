# RecoMind - AI-Based Recommendation System

RecoMind is a Java web application that suggests products to users based on their behaviour and stated
preferences. A hybrid recommender (content-based + collaborative filtering + time-decayed popularity)
combines the three scores with weights that an administrator can tune at runtime. Every recommendation
comes with a short "why recommended" explanation.

Built with plain Servlets, JDBC and an embedded Jetty server. No Spring, no Hibernate, no external
database or application server to install.

## Run it

1. Install JDK 17 or newer (check with `java -version`).
2. In the project folder run:

        .\gradlew.bat run

3. Open http://localhost:8080 in a browser.

The database (an H2 file in the `data` folder) is created and filled with demo data on the first start.
To reset it, stop the app and delete the `data` folder.

| Role  | Email               | Password  |
|-------|---------------------|-----------|
| Admin | admin@recomind.com  | Admin@123 |
| User  | user@recomind.com   | User@123  |

Run the tests with `.\gradlew.bat test` (46 JUnit 5 and Mockito tests). A runnable fat jar can be built
with `.\gradlew.bat shadowJar` (output: `build\libs\recomind-all.jar`).

## Features

**User**
- Personalised recommendation feed with a score, the algorithm that produced it and a "why" text
- Like and buy actions are stored as interactions and change future recommendations
- Preference management (categories, price range) that refreshes the feed
- Register (with optional onboarding categories), login, logout, change password

**Admin**
- Overview and analytics: users, catalog size, interaction counts, activity per category and per interaction type
- AI model settings: weights of the three algorithms, number of recommendations, similarity threshold,
  minimum interactions, refresh interval and active algorithm (weights must sum to 1.0)
- User management and item management

## Architecture

Layered design, one package per layer:

| Package | Responsibility |
|---|---|
| `model` | Plain objects: User, Item, Category, Interaction, UserPreference, SystemSettings, AuditLog |
| `dao` | DAO interfaces and JDBC implementations (PreparedStatement, try-with-resources) |
| `service` | AuthService, RecommendationService (cache and background threads), TransactionManager |
| `recommender` | Strategy interface, three algorithms and the HybridRecommender |
| `servlet` | JSON API servlets, all extending ApiServlet (central exception to HTTP status mapping) |
| `filter` | Authentication, role authorization, CSRF and character-encoding filters |
| `server`, `util`, `config` | Embedded Jetty, database bootstrap and connection pool, configuration |

Design diagrams are in the `docs` folder: requirements and use cases, ER diagram, class diagram, sequence
diagrams and the recommendation flowchart (Mermaid, rendered by GitHub and most Markdown viewers).

### Recommendation engine

- **Content-based:** TF-IDF weighted vectors of category and tag terms, cosine similarity between the user
  profile (liked items plus saved preferences) and each item.
- **Collaborative filtering:** item-item cosine similarity on the user-item matrix.
- **Popularity:** interaction counts with a 30-day half-life time decay. Used alone for new users (cold start).
- **Hybrid:** weighted sum with the admin weights, removes items the user already interacted with, applies the
  price preference and keeps the top N with a `PriorityQueue`.

## API

All endpoints return `{ "success": ..., "message": ..., "data": ... }`. Every POST/PUT/DELETE needs the CSRF
token (from `GET /api/csrf` or the login response) in the `X-CSRF-Token` header.

- `/api/admin/analytics`
- `/api/admin/items`
- `/api/admin/settings`
- `/api/admin/users`
- `/api/auth/login`
- `/api/auth/logout`
- `/api/auth/me`
- `/api/auth/password`
- `/api/auth/register`
- `/api/categories`
- `/api/csrf`
- `/api/interactions`
- `/api/items`
- `/api/preferences`
- `/api/recommendations`

Pages served from `src/main/resources/static`:

- `/admin-dashboard.html`
- `/admin.html`
- `/dashboard.html`
- `/discover.html`
- `/interactions.html`
- `/login.html`
- `/preferences.html`
- `/profile.html`
- `/register.html`
- `/user-dashboard.html`

## How the project meets the grading rubric

| Rubric area | Where it is demonstrated |
|---|---|
| Problem understanding and solution design | `docs/requirements.md`, use case, class, ER and sequence diagrams, layered architecture |
| Core Java concepts | Strategy pattern (`RecommendationStrategy` with 3 implementations), DAO pattern, interfaces and polymorphism. Collections: `ConcurrentHashMap` cache, `PriorityQueue` top-N, `HashMap`/`TreeMap`, streams, comparators. Custom exceptions (DAOException, ValidationException, AuthenticationException, RecommendationException) mapped to HTTP status codes in one place. Threads: `ScheduledExecutorService` refreshes the recommendation cache, a single-thread `ExecutorService` writes interactions asynchronously, a `ThreadLocal` holds the transaction connection, and a shutdown hook stops everything cleanly |
| Database integration (JDBC) | H2 with HikariCP, 9 tables with primary keys, foreign keys and indexes, PreparedStatement only, `TransactionManager` with commit and rollback (register user + preferences, bulk settings save), rollback verified by a test |
| Servlets and web integration | JSON servlets, `HttpSession` with timeout, session invalidated and recreated at login (session fixation protection), CSRF token, role-based access to admin pages and APIs |
| Code quality and testing | 46 automated tests (DAO and transaction tests on in-memory H2, recommender unit tests, service tests, API integration tests, Mockito servlet and filter tests), layered packages, passwords stored as BCrypt hashes |
| Teamwork | Git history with one commit per feature, this README, module ownership table below |
| Innovation | Explainable recommendations ("Because you liked ..."), admin-tunable hybrid algorithm, cold-start handling, onboarding categories at registration |



## Known limitations

- Demo data is synthetic and generated by SQL in `seed.sql`.
- Collaborative filtering needs a minimum number of interactions per user (admin setting); other users fall back to
  content-based and popularity scores.
- The session cookie is HttpOnly but the SameSite attribute is not set explicitly. The CSRF token protects state-changing calls.
- H2 is the only database that has been tested.
