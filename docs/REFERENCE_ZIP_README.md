# Java / OOP Reference ZIP

This separate ZIP is intended for source-code reading and an academic OOP presentation. It contains **all 24 original Java `.java` files from the working backend**, along with the OOP and architecture explanations and SQL schema.

Start with these files in order:

1. `src/main/java/com/farmlink/Account.java` — abstract class + private fields.
2. `src/main/java/com/farmlink/Farmer.java` and `Buyer.java` — inheritance, method overriding and polymorphism.
3. `src/main/java/com/farmlink/MarketplaceService.java` — abstraction via interface.
4. `src/main/java/com/farmlink/DefaultMarketplaceService.java` — actual interface implementation.
5. `src/main/java/com/farmlink/HarvestRepository.java` and `JdbcHarvestRepository.java` — database repository contract and JDBC implementation.
6. `src/main/java/com/farmlink/Main.java` — dependency injection/composition root.

For the **entire runnable website** including HTML/CSS/JS and the bundled JDBC jar, use `FarmLink_Complete_Project.zip` instead. The reference ZIP alone is not a runnable web application.
