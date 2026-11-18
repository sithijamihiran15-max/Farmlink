# FarmLink administrator setup (MySQL and embedded HSQLDB)

## What this adds

- A new Java `Admin extends Account` type and explicit role decoding.
- `AdminRepository` + `JdbcAdminRepository`, and `AdminService` + `DefaultAdminService`.
- Admin-only backend APIs with server-side role authorization.
- Editable `web/admin-dashboard.html` and `web/css/admin.css`.
- Local-only admin creation command; public `/api/auth/register` is still FARMER/BUYER only.
- Existing MySQL tables and data are preserved; no schema migration required.

## Windows: create a NEW admin in MySQL

1. Back up the current FarmLink directory and database before copying the patch.
2. Stop `run.bat` and copy the patch files into your existing FarmLink directory.
3. Ensure `lib/mysql-connector-j-*.jar` exists and MySQL is running.
4. Open **Command Prompt (cmd.exe)** in the FarmLink directory. Run:

   ```cmd
   set "FARMLINK_DB=mysql"
   set "FARMLINK_DB_USER=farmlink_app"
   set "FARMLINK_DB_PASSWORD=your-existing-MySQL-password"
   create-admin.bat
   ```

5. Enter your first name, last name, a **new unique NIC/login ID**, email, and a strong password (12+ characters) when asked. The script hashes your password with PBKDF2 and writes the admin record to your selected database. It does not print or store your plaintext password in a project file.
6. Restart using `run.bat` in the same Command Prompt, then open the URL printed by the server. Log in with the admin NIC/password at `/login.html`. You should land on `/admin-dashboard.html`.

### If you prefer to promote an existing test account

With the **new Java files installed**, sign up as a new farmer/buyer and run this as your MySQL **database administrator** in Workbench (do not execute while using the old Java code):

```sql
USE farmlink_db;
UPDATE users SET role='ADMIN' WHERE nic='REPLACE_WITH_ACCOUNT_NIC' AND role IN ('FARMER','BUYER');
SELECT id,nic,role FROM users WHERE nic='REPLACE_WITH_ACCOUNT_NIC';
```

Log out and back in. This turns the existing account into an admin (it can no longer act as its old role). Do not update arbitrary real-user accounts. Do **not** put SQL admin promotion into a public API or website.

## Embedded database

Without `FARMLINK_DB=mysql`, running `create-admin.bat` creates the admin in the original `data/farmlink.*` HSQLDB database instead. HSQLDB and MySQL are **separate databases**; use the same environment variables for both `create-admin.bat` and `run.bat`.

## Admin features

- Summary counts of farmers, buyers, administrators, harvests, and requests.
- View registered accounts (excluding password hashes) and all harvest listings.
- Unlist active harvests (soft status update); request history stays intact.
- Server endpoints: GET `/api/admin/overview`, GET `/api/admin/users`, GET `/api/admin/harvests`, POST `/api/admin/harvests/:id/unlist`.
- All endpoints require a logged-in ADMIN; page URLs alone do not grant access.

## Notes

- The public registration form can never register an ADMIN. That is intentional.
- Do not share admin login details, put passwords in screenshots, or expose this locally developed server directly to the public Internet.
- The project is a local academic/demo application; production deployment would require HTTPS, CSRF protection, audit logging, account recovery, stronger deployment controls, and other security hardening.
- No changes to `database/mysql_schema.sql` are necessary.
