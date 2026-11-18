## New: optional MySQL support
For MySQL, read `MYSQL_SETUP_WINDOWS.txt` first. Default embedded HSQLDB mode still works normally. The MySQL JDBC driver must be downloaded separately and placed into `lib/`.

# FarmLink — Complete Java OOP Marketplace

This project upgrades the supplied **21 editable HTML/CSS UI screens** into a full local database-backed FarmLink marketplace. It includes a Java API, JDBC repositories, service interfaces, all four OOP concepts, JavaScript interactions, and responsive fixes.

**Run:** Open the `FarmLink` folder in a terminal; install JDK 17+; execute `bash run.sh` (macOS/Linux) or `run.bat` (Windows). **Open the exact URL printed by the server** (usually **http://127.0.0.1:8080/**; if busy, it automatically tries **8081–8090**). Keep the terminal open.

**Demo accounts:** Farmer `DEMOFARMER` / `farmer1234`; Buyer `DEMOBUYER` / `buyer1234`. Demo credentials are for local development only.

**Where to edit:** `web/*.html` (pages), `web/css/*.css` (design), `web/js/app.js` (browser logic), `src/main/java/com/farmlink/*.java` (business logic). `original-ui/` contains the untouched source UI files.

**Documents:**
- [`docs/IMPLEMENTATION_GUIDE.md`](docs/IMPLEMENTATION_GUIDE.md) — full build steps, progress, features, launch, edits, troubleshooting.
- [`docs/OOP_CONCEPTS.md`](docs/OOP_CONCEPTS.md) — encapsulation, abstraction, inheritance and polymorphism with project code.
- [`docs/API_REFERENCE.md`](docs/API_REFERENCE.md) — JSON API documentation.
- [`docs/DATABASE_AND_ARCHITECTURE.md`](docs/DATABASE_AND_ARCHITECTURE.md) — database schema and architecture.

**Integration test:** While server runs, execute `python3 tests/smoke.py` (requires Python 3, no third-party packages).

**Notice:** This is a working localhost academic project, not a complete hosted commercial platform. Online payments, notifications, identity checks and production hardening are intentionally outside the scope.

## If it does not start

1. Extract the ZIP fully before running anything. **Do not open** `web/index.html` directly or use VS Code Live Server.
2. Install **JDK 17+**. In a terminal, run `java -version` and `javac -version`. Both commands should work.
3. Run `run.bat` in **Windows Command Prompt** or `bash run.sh` on macOS/Linux. The Windows script now uses `CompileProject.java` to compile Java files without broken Windows backslash escaping and stays open on errors.
4. Open **the exact localhost URL printed in the server window**, not a hardcoded port. FarmLink tries ports 8080–8090 when its default is occupied.
5. If you get an error, copy the complete terminal error message.

**Windows:** Right-click `FarmLink_Windows_Compile_Fixed.zip` → **Extract All** → open the extracted `FarmLink` folder → double-click `run.bat`. Windows may block downloaded scripts; inspect/unblock only if you trust this archive.

**For a specific custom port:** Windows Command Prompt: `set FARMLINK_PORT=8085` then `run.bat`; macOS/Linux: `FARMLINK_PORT=8085 bash run.sh`.
