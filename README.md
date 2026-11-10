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

Open the [storefront](http://localhost:8080/). Register an account through User Auth Swagger and add a product with a positive INR price through Ecommerce Swagger. Sign in to the storefront and select **Buy**. Complete the payment with [Stripe test card details](https://docs.stripe.com/testing#cards).

Ecommerce verifies the payment and sends a receipt to the email on the account. For a local email test, start an SMTP catcher such as Mailpit on port 1025 and view captured receipts at [localhost:8025](http://localhost:8025/). For delivery to an inbox, set `SMTP_HOST`, `SMTP_PORT`, `RECEIPT_FROM`, and any required Spring Mail authentication and TLS properties in Ecommerce's environment. Ecommerce retries pending receipts while it is running. If you use a different storefront origin, set `checkout.storefront-url` to that origin's root URL for Stripe redirects.
