# Verification Summary

Tested using Java 21 compiling with `--release 17`, embedded HSQLDB, and Chromium browser layout checks.

## Java build and backend HTTP tests

- Java source compiled successfully with all 24 `.java` files.
- Embedded database created schema and seeded 2 example accounts, 9 harvests, 2 saved items and one request.
- `tests/smoke.py` passed end-to-end checks for registration, login/logout, invalid password, roles, API authorization, search, harvest create/update/unlist, image storage, favourites, request creation, status changes, inventory reserve/release, private messaging, dashboards and profile editing.
- Private requests are unavailable to another authenticated buyer.
- Java server serves `index.html`, `explore.html`, CSS and JavaScript directly.

## Frontend JavaScript integration checks

The unmodified frontend JavaScript was exercised in Chromium using a bridge to the running Java API (the test harness only supplied page location and network transport).

- Homepage displayed **4 live harvest cards**.
- Explore displayed **9 listings**; filtering for Fruits returned **2**.
- Buyer dashboard loaded live greeting and `2 / 1 / 1 / 0` stat values from seeded records.
- Farmer dashboard loaded live greeting and `9 / 1 / 1 / 0` stat values.
- Saved items loaded **2** harvest cards.
- Farmer listings generated **9** database-backed table rows.
- Farmer and buyer request pages loaded the seeded request.
- Conversation displayed the correct participant and stored messages.
- Harvest detail page showed the requested listing's real title.
- No browser JavaScript exceptions occurred in these checks.

## Responsive layout checks

Chromium rendered the HTML/CSS with local SVG and styles at widths **320, 390, 768 and 1440 px**. Tested home, explore, farmer dashboard, farmer listings, harvest details, request modal and login page. At these widths, `document.documentElement.scrollWidth` matched the viewport width (no page-level horizontal overflow). Save buttons remained fully within their listing cards.

**Important:** Actual appearance still depends on installed fonts and browser; check the site on your own device after making visual edits. This QA is for a local student project, not a formal accessibility/security audit.
