# Capston-Project
Capstone Project | Backend application developed using Java, Spring Boot, and REST APIs, focusing on scalable architecture, clean code, and efficient backend development.

The repository contains two independent Maven projects. Open each project's `pom.xml` in IntelliJ or run its Maven wrapper from its project directory. Both projects use Java 17.

| Project | Default port | MySQL schema | Swagger UI |
| --- | --- | --- | --- |
| `UserAuthService` | 8081 | `user_service` | [Auth API](http://localhost:8081/swagger-ui/index.html) |
| `capston` | 8080 | `test` | [Product API](http://localhost:8080/swagger-ui/index.html) |

Create the MySQL schemas and set `SPRING_DATASOURCE_PASSWORD` in each run configuration. For UserAuthService, also set `AUTH_JWT_SECRET_BASE64` to a Base64-encoded key containing at least 32 random bytes. Start UserAuthService first, then start Capston. Capston calls the auth service on port 8081 by default.
