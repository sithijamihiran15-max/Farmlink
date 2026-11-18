# Java Source Reference — Every Class and Interface

Every Java file is under `src/main/java/com/farmlink/`. All are real project sources, not isolated lecture examples.

| Source file | Type | Responsibility |
|---|---|---|
| `Main.java` | Application | Starts HTTP server, builds dependencies, serves editable HTML/CSS/JS. |
| `ApiHandler.java` | HTTP controller | REST routing, cookie sessions, JSON responses, authorization integration. |
| `Database.java` | Infrastructure | Embedded JDBC connection, schema setup and shutdown. |
| `Json.java` | Utility | Serializes and parses JSON without third-party libraries. |
| `Inputs.java` | Utility | Validates text, numbers, IDs, enums and builds response maps. |
| `ApiException.java` | Exception | Typed HTTP errors for the controller. |
| `Account.java` | Abstract class | Shared user identity, encapsulation, abstract role method. |
| `Farmer.java` | Subclass | Farmer type inherits from `Account`. |
| `Buyer.java` | Subclass | Buyer type inherits from `Account`. |
| `Harvest.java` | Model | Immutable private harvest fields and safe public view. |
| `PasswordHasher.java` | Security | PBKDF2 password hash, verification, opaque session tokens. |
| `AccountRepository.java` | Interface | Contract for accounts, passwords and sessions. |
| `JdbcAccountRepository.java` | Implementation | JDBC queries for user profiles and sessions. |
| `HarvestRepository.java` | Interface | Contract for browse, detail, create, update and unlist. |
| `JdbcHarvestRepository.java` | Implementation | Prepared SQL statements for harvest records and filters. |
| `TradeRepository.java` | Interface | Requests, messages, saved items and status changes. |
| `JdbcTradeRepository.java` | Implementation | Request transactions, stock reservations and private messages. |
| `AuthService.java` | Interface | Signup/login/session/profile contract. |
| `DefaultAuthService.java` | Implementation | Validated registration, PBKDF2 login and profile changes. |
| `MarketplaceService.java` | Interface | Marketplace business-operations contract. |
| `DefaultMarketplaceService.java` | Implementation | Farmer ownership checks and harvest validation. |
| `TradeService.java` | Interface | Request/favourite/messaging business contract. |
| `DefaultTradeService.java` | Implementation | Buyer/farmer role checks and allowed trade transitions. |
| `DemoSeeder.java` | Startup helper | Adds local example users, harvests, saved items, and a request. |

For a classroom demonstration, start with `Account.java`, `Farmer.java`, `Buyer.java`, `MarketplaceService.java`, `DefaultMarketplaceService.java`, `HarvestRepository.java`, `JdbcHarvestRepository.java`, and `Main.java`. Together they show encapsulation, abstraction, inheritance, polymorphism, repository pattern and dependency injection.
