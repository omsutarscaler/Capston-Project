# Capston-Project
Capstone Project | Backend application developed using Java, Spring Boot, and REST APIs, focusing on scalable architecture, clean code, and efficient backend development.

The repository contains three independent Maven projects. Open each project's `pom.xml` in IntelliJ or run its Maven wrapper from its project directory. All three projects use Java 17.

| Project | Default port | MySQL schema | Swagger UI |
| --- | --- | --- | --- |
| `UserAuthService` | 8081 | `user_service` | [Auth API](http://localhost:8081/swagger-ui/index.html) |
| `ecommerce` | 8080 | `test` | [Product API](http://localhost:8080/swagger-ui/index.html) |
| `PaymentGatewayService` | 8082 | None | [Payment API setup](PaymentGatewayService/README.md) |

Create the MySQL schemas and set `SPRING_DATASOURCE_PASSWORD` for UserAuthService and Ecommerce. For UserAuthService, also set `AUTH_JWT_SECRET_BASE64` to a Base64-encoded key containing at least 32 random bytes.

Set the same `PAYMENT_SERVICE_KEY` in Ecommerce and PaymentGatewayService. Set `STRIPE_API_KEY` only in PaymentGatewayService, using a sandbox key. Start UserAuthService and PaymentGatewayService, then start Ecommerce.

From `storefront`, run `npm ci` and `npm run dev` with Node.js 20.9 or later. Open the [storefront](http://localhost:3000/) to register, sign in, browse, save products, or check out.

After Ecommerce starts and creates its tables, load the local demo catalog by running `node ecommerce/demo/catalog.mjs | mysql --default-character-set=utf8mb4 -u root -p test` from the repository root. The script fills the 50 demo categories to 20 products each while preserving existing records and stock.

Create a product with a positive INR price using Ecommerce Swagger. Products start with zero inventory. Assign `ADMIN` to a trusted account in UserAuthService's `user_roles` table, then use that account's JWT with `PUT /inventory/{id}` and a body such as `{"available":5}`. The cart holds up to 10 units. Inventory is reserved at checkout and released when Stripe reports expiration or confirms cancellation. Complete a demo checkout with [Stripe test card details](https://docs.stripe.com/testing#cards).

For local password reset and receipt emails, start an SMTP catcher such as Mailpit on port 1025 and view messages at [localhost:8025](http://localhost:8025/). For inbox delivery, configure `SMTP_HOST`, `SMTP_PORT`, `RECEIPT_FROM`, and the required Spring Mail authentication and TLS properties in both services. If you use a different storefront origin, set `STOREFRONT_URL` in Ecommerce and UserAuthService so checkout and reset links return there.

Inventory and reviews are separate service modules within Ecommerce. Inventory shares the checkout database so an order and its stock reservation can be committed in one transaction.
