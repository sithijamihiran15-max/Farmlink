# FarmLink — Complete Implementation & Progress Guide

## Project goal

Convert the supplied **21-page standalone FarmLink HTML/CSS template** into an operational farmer-to-buyer marketplace using Java, relational database storage, service/repository interfaces, all four OOP concepts, and ordinary JavaScript for browser interactions. Keep every original HTML and CSS page editable and available.

## What changed, in order

1. **Inspected the supplied RAR.** Verified the 21 HTML screens, two CSS files, three SVG assets, and original README. The supplied forms, filters, dashboard counters, favourites, messages, and buyer inquiries originally displayed sample data only.
2. **Preserved source material.** Copied the unmodified files to `original-ui/`. Copied an editable working version into `web/` with the same HTML filenames and original `css/style.css`, `css/static.css`, and SVG assets.
3. **Designed Java layers.** Created `Account`, `Farmer`, `Buyer`, `Harvest`, dedicated service interfaces and implementations, repository interfaces and JDBC implementations, API routing, input validation, and an application startup class.
4. **Implemented the database.** Bundled HSQLDB, set up schema migration on start, seeded realistic local demo data, and persisted users, harvests, requests, messages, favourites, and sessions.
5. **Implemented accounts and login.** Registration by role, NIC login, salted PBKDF2 password hashes, sessions, role permissions, protected page redirects, profile updates, and logout.
6. **Implemented marketplace features.** Live home and explore listings, keyword/location/category/price filtering, sorting, harvest details, photo upload/resizing, new listings, edits and unlisting.
7. **Implemented buyer and farmer workflows.** Save/unsave favourites, send buyer requests, accept/decline/cancel/complete requests, track quantity, reserve/return available stock, view dashboards, and exchange private messages.
8. **Fixed layout issues.** Positioned floating favourite icons relative to their cards; prevented grid items and table columns from spilling out; refined hero artwork, images, media breakpoints and mobile buttons. Added responsive CSS as a separate, editable stylesheet rather than destroying the existing design.
9. **Connected the frontend.** Added `web/js/app.js` and one stylesheet reference to each editable page, changing the original form action buttons to real submit buttons. JavaScript replaces placeholder cards, request tables, dashboard statistics and profile contents with data from `/api`.
10. **Verified the backend.** Compiled all Java classes with JDK 21 targeting Java 17 and ran `tests/smoke.py`. This covers registration, login, role permissions, harvest CRUD, filtering, favourites, requests, status transitions, inventory adjustments, messages, dashboards, profile edits and logout.

## How to run (Windows, macOS or Linux)

**Requirement:** Install JDK **17 or newer** and make `java` and `javac` available in your terminal. No Maven, Gradle, Node.js, MySQL, internet connection or external database server is needed for local execution.

1. Extract `FarmLink_Complete_Project.zip` to a normal folder.
2. Open a terminal **inside the `FarmLink` directory**.
3. On macOS/Linux run `bash run.sh` (or `./run.sh` if executable); on Windows double-click `run.bat` or run it in Command Prompt.
4. The script compiles the Java source and starts the server. Open **the exact URL it prints** (typically **http://127.0.0.1:8080/**). If 8080 is already in use, FarmLink automatically tries ports **8081–8090**.
5. Keep the terminal window running while using the site; press `Ctrl+C` to stop.

If ports 8080–8090 are all occupied, set the `FARMLINK_PORT` environment variable to an available port (e.g., `8091`). The default interface is local loopback for safer student demos; use `FARMLINK_HOST` to change binding only when you understand the network exposure.

### Demo logins

| Role | NIC | Password |
|---|---|---|
| Farmer | `DEMOFARMER` | `farmer1234` |
| Buyer | `DEMOBUYER` | `buyer1234` |

**These are intentionally public demo credentials; do not publish an Internet-facing deployment with demo seeding enabled.** You may register your own account from the signup pages.

## Walkthrough — what to click

**Buyer path:** Home → Explore → filter or search → click harvest → Save harvest or Request this harvest → choose quantity and send message → Buyer dashboard → My requests → Message farmer → optionally cancel the request. Saved items appear on the Saved harvests page.

**Farmer path:** Login → Farmer dashboard → Post harvest → enter name, category, price, quantity and description (optional photo) → Publish harvest → My harvests → Edit or Unlist → Buyer requests → Accept or Decline → open Message conversation → Mark completed after the physical trade is finished.

**Profiles:** Open My profile from either dashboard, edit the personal details and press Save changes. The new values are saved in the database.

**Important trading rule:** Payment, delivery and collection arrangements are agreed directly between farmer and buyer; there is no online checkout/payment gateway. Accepting a request reserves its quantity; completion is manually marked by the farmer.

## Editable file map

```text
FarmLink/
  run.sh, run.bat                    Start Java server / compile source
  web/
    index.html                      Homepage (edit in HTML)
    explore.html                    Marketplace filters and cards
    harvest-details.html            Product details
    login.html, register-*.html     Authentication forms
    buyer-*.html, farmer-*.html     Buyer and farmer dashboard pages
    post-harvest.html              Farmer listing creation
    edit-harvest.html              Farmer listing editing
    request-harvest.html           Buyer request dialog
    conversation.html              Direct messaging
    how-it-works.html, pages.html   Public information / index
    css/style.css                  Original base design (editable)
    css/static.css                 Original mobile navigation (editable)
    css/responsive-fixes.css       Responsive refinements (editable)
    js/app.js                      Frontend API calls, events and rendering
    assets/*.svg                   Original illustrations / favicon
  original-ui/                       **Untouched original HTML/CSS/screens**
  src/main/java/com/farmlink/        Java model, services, repositories, API
  database/schema.sql               Readable database schema reference
  data/                              Auto-created persistent database files
  lib/hsqldb.jar                     Bundled embedded database JDBC dependency
  tests/smoke.py                     Automated HTTP API integration test
  docs/IMPLEMENTATION_GUIDE.md      This guide
  docs/OOP_CONCEPTS.md               Four OOP concepts with code examples
  docs/API_REFERENCE.md              REST endpoints and usage
  docs/DATABASE_AND_ARCHITECTURE.md  Database diagrams / security notes
  LICENSES/                          Embedded DB license notices
```

## Changing the design without losing functionality

- **Colors, spacing, fonts, general media queries:** edit `web/css/style.css`.
- **Extra mobile layout fixes:** edit `web/css/responsive-fixes.css` (loaded after the original CSS; it may override earlier declarations).
- **Navigation:** edit the header in the relevant `web/*.html` screen; preserve expected URLs.
- **Text/sections and static page layouts:** edit `web/*.html` freely.
- **Dynamic listing card layout:** edit the `card(h)` function in `web/js/app.js` because the JavaScript renders marketplace cards dynamically.
- **Dynamic request card layout:** edit `requestCard(r)` in `web/js/app.js`.
- **API interactions:** edit functions such as `api`, `explore`, `editor`, `inbox`, and `conversation` in `web/js/app.js`.
- **Business rules:** edit `DefaultAuthService.java`, `DefaultMarketplaceService.java`, and `DefaultTradeService.java`.
- **Database SQL:** edit repository implementations, and update `Database.java` migration logic **carefully**; changing `database/schema.sql` alone will not modify a running database.

Keep element IDs used by the JavaScript (e.g. `#login-form`, `#listing-form`, `#filters-form`, `#inquiry-form`) if you customize page markup. Existing pages remain separate files, not generated from a framework.

## How to test

Start the server, open a separate terminal and run:

```bash
python3 tests/smoke.py
```

On Windows with Python installed: `py tests\smoke.py`. The test uses only the Python standard library and creates temporary test accounts and listings in the live database, so run it against a **local demo copy**, not your production data. You can also open `http://127.0.0.1:8080/api/health` to confirm the server responds. `docs/API_REFERENCE.md` contains curl examples.

The Java code was compiled and the HTTP integration test passed during preparation. **Responsive browser layout checks were completed** at widths of 320, 390, 768 and 1440 pixels using local resources; no page-level horizontal overflow was detected, and save-button placement was checked. The frontend JavaScript was also exercised against the Java API using a browser test harness. Please still verify the final UI in Chrome/Edge on your own machine after design edits.

## Database and persistence

Users, harvests, inquiries, messages, saved harvests and sessions are relational tables. After first run, `data/farmlink.*` appears. The **database is persistent**: ending and restarting the server preserves registrations and listings. To reset the demonstration, stop Java and delete the generated `data/farmlink.*` files, then restart (and re-seed if enabled).

## Known scope / next production steps

Included: local working Java marketplace, client-side image resize, search and filter, login, account roles, CRUD, saved items, request lifecycle, stock reservation, private messaging and dashboards. **Not included:** card payments, map delivery tracking, identity/NIC verification, mail/SMS notifications, password recovery, external image hosting, admin moderation, cloud hosting, WebSockets (messages use manual Refresh), pagination, or real-time push alerts. Before deploying publicly, add TLS/HTTPS, Secure cookies, rate limiting and abuse protection, monitoring, backup strategy and production-grade database upgrades.

## Common problems

| Issue | Solution |
|---|---|
| `java` or `javac` not found | Install JDK 17+ and reopen the terminal; check `java -version` and `javac -version`. |
| Site opens but buttons do not work | Do **not** use `file://` or VS Code Live Server; run the Java server and use `http://127.0.0.1:8080/`. |
| Port 8080 already used | The fixed launcher automatically tries 8081–8090. **Open the actual URL printed in the terminal.** If needed, set `FARMLINK_PORT` to another free port. |
| Existing demo account duplicates | Demo seeder only inserts on the first database initialization; don't register duplicate NICs. |
| Database file in use | Shut down the Java process before copying or deleting anything in `data/`. |
| Image rejected | Choose JPEG/PNG/WebP under 2 MB; the browser resizes the photo before upload. |
| A page redirects to login | That page requires the correct FARMER or BUYER role. |

**Quick checks:** Visit the homepage → Login using a demo account → View the matching dashboard → Browse listings. For a full end-to-end exercise, sign in as both roles in two browser profiles or incognito windows and send/accept a buyer request.
