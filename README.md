# 7-Eleven Testing Round

**Candidate:** Loi Nguyen Thang &nbsp;·&nbsp; **Role:** Fresher Java Engineer @ 7-Eleven Vietnam &nbsp;·&nbsp; **Stage:** Round 2 — technical test

A Spring Boot e-commerce web app implementing the three required screens:
admin product management, user product catalog with checkout, and admin
order management.

---

## Quick start

One command. Everything (Postgres + app + seed data) comes up via Docker.

```bash
cp .env.production.example .env       # set POSTGRES_PASSWORD inside
docker compose up -d --build
```

App is then at **http://localhost:7410**.

(Port 7410 is the host port set in `.env` via `APP_PORT`. The container
itself listens on 8080 internally — only the host-side port matters.)

The database boots fresh with 5 categories, 12 sample products, plus two
test accounts so you can start clicking around immediately.

---

## Test accounts

Both accounts are seeded automatically on first boot.

| Role | Email | Password | What you can do |
| :--- | :--- | :--- | :--- |
| **Admin** | `admin@example.com` | `admin123` | Manage products and orders |
  | **User**  | `user@example.com`  | `user123`  | Browse catalog and place orders |

---

## Use cases to test

### 1. Admin — manage products

```
 ┌─────────────┐   click    ┌──────────────┐   /admin/loadAddProduct
 │  /signin    │─────────►  │ admin/index  │──────────────────┐
 │  admin login│            └──────────────┘                  │
 └─────────────┘                   │                          ▼
                                   │ /admin/products    ┌──────────┐
                                   ▼                    │ Add form │
                            ┌──────────────┐            └────┬─────┘
                            │ Product LIST │◄───────────────┘ submit
                            │              │
                            │  ✏ /admin/editProduct/{id}   detail + edit
                            │  🗑 /admin/deleteProduct/{id}  delete
                            └──────────────┘
```

**Try this:**
1. Log in as **admin**.
2. Open `/admin/products` — you should see the 12 seeded products.
3. Click **Add New** → fill in title, description, pick a category, price, stock → **Save Product**.
4. Click ✏ on any row to edit (e.g. change discount to 15%) → **Save**.
5. Click 🗑 on a row to delete.

### 2. User — browse and place an order

```
 ┌──────────────┐    /signin     ┌────────────────┐
 │  /products   │   user login   │  Product card  │
 │  (public)    │  ────────────► │  click title   │
 └──────┬───────┘                └────────┬───────┘
        │ filter by category              │
        │ /products?category=Beverages    ▼
        ▼                          ┌────────────────┐
   filtered list                   │  /product/{id} │
                                   │   detail page  │
                                   └────────┬───────┘
                                            │ Add to cart
                                            ▼
                                   ┌────────────────┐    /user/orders   ┌──────────────────┐
                                   │   /user/cart   │ ───────────────►  │  Checkout form   │
                                   │  qty +/-       │                    │  address, COD    │
                                   └────────────────┘                    └────────┬─────────┘
                                                                                  │ submit
                                                                                  ▼
                                                                       /user/order-success
```

**Try this:**
1. Log in as **user**.
2. Open `/products` — see all 12 products.
3. Filter by **Beverages** in the sidebar (or open `/products?category=Beverages`).
4. Click on **Coca-Cola 330ml** → product detail page.
5. Click **Add to cart**. Click **+** in the cart to bump quantity to 2.
6. Open `/user/orders` → fill the checkout form → **Place order**.
7. You land on a success page. Visit `/user/user-orders` to see the order in your history.

### 3. Admin — view and ship orders

```
 ┌──────────────┐  /admin/orders  ┌──────────────┐   select status   ┌──────────────┐
 │  admin login │ ──────────────► │ Order LIST   │ ────────────────► │  DELIVERED   │
 │              │                 │              │                   │  CANCELLED   │
 └──────────────┘                 │  Browser     │                   │  IN_PROGRESS │
                                  │  Shopper     │                   │  ...         │
                                  │  Coca-Cola   │ ◄──────────────── │              │
                                  │  $24,000     │   status updated  │  submit      │
                                  └──────────────┘                   └──────────────┘
```

**Try this:**
1. Log in as **admin** (in another browser tab if you want to keep the user session).
2. Open `/admin/orders` — the order you just placed appears.
3. Pick **DELIVERED** in the **Change Status** dropdown → click the submit arrow.
4. A green "Status updated" toast appears; the order's STATUS column now reads `Delivered`.

---

## Local dev (without Docker)

```bash
# 1. A Postgres for the app
docker run -d --name ecom-db \
  -e POSTGRES_USER=ecom -e POSTGRES_PASSWORD=ecompass \
  -e POSTGRES_DB=ecommerce_db -p 5440:5432 postgres:16-alpine

# 2. .env points the app at it
cp .env.production.example .env
# SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5440/ecommerce_db?user=ecom&password=ecompass

# 3. Build + run
mvn -DskipTests clean package
java -jar target/Ecommerce-SpringBoot-0.0.1-SNAPSHOT.jar
```

---

## Tests

The test suite covers all three use cases at four layers:

```
mvn test                  # 51 tests (unit + web + integration),     ~11s
mvn test -Pfast           # 40 tests (unit + web only),               ~5s
mvn test -Pall-tests      # 52 tests including headless browser E2E, ~22s
```

Watch the browser E2E run live in Chromium:

```bash
mvn test -Dbrowser.e2e=true -Dbrowser.headless=false -Dbrowser.slowmo=400 \
         -Dtest=EcommerceBrowserE2ETest
```

Coverage report: `target/site/jacoco/index.html` after every `mvn test`.

---

## Tech stack

Java 17 · Spring Boot 3.3 · Spring Security 6 · Spring Data JPA · Hibernate ·
Thymeleaf · Bootstrap 5 · PostgreSQL 16 (H2 for tests) · Cloudinary ·
JUnit 5 · Mockito · Playwright · Docker · GitHub Actions.

---

## Project layout

```
src/main/java/com/ecom/Ecommerce_SpringBoot/
├── config/         Security, default admin & data seed, Cloudinary
├── controller/     AdminController, HomeController, UserController
├── entities/       Product, ProductOrder, Cart, UserDtls, Category, ...
├── persistence/    DAOs
├── repository/     Spring Data JPA repositories
├── service/        Service interfaces + implementations
└── util/           CommonUtil (mail), StatusOrder, AppConstant

src/test/java/com/ecom/Ecommerce_SpringBoot/
├── unit/           @Tag("unit")        pure Mockito
├── web/            @Tag("web")         @WebMvcTest slices
├── integration/    @Tag("integration") @SpringBootTest + H2
├── e2e/            @Tag("e2e")         Playwright + Chromium
└── support/        TestFixtures, IntegrationTestBase

.github/workflows/
├── ci.yml          test → build & push to GHCR
└── cd.yml          deploy on self-hosted runner (workflow_run from CI)
```

---

Submitted by **Loi Nguyen Thang** for the 7-Eleven Vietnam Fresher Java
Engineer 2nd-round technical test.
