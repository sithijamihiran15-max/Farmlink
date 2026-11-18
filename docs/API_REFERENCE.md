# FarmLink REST API Reference

All endpoints begin with `/api`. Request bodies and responses use `application/json`. When signed in, the browser sends an **HttpOnly `FL_SESSION` cookie**, so client-side JavaScript does not read the token. Errors are `{ "error": "..." }` and return an appropriate HTTP status (`400`, `401`, `403`, `404`, `409`, `413` or `500`).

## Public endpoints

| Method | Path | Action |
|---|---|---|
| GET | `/health` | Basic server/database health |
| GET | `/harvests` | Browse active harvests |
| GET | `/harvests/{id}` | Harvest details (inactive visible to owner only) |
| POST | `/auth/register` | Register FARMER or BUYER; starts session |
| POST | `/auth/login` | Login using NIC and password; starts session |
| GET | `/auth/me` | Current signed-in user or `null` |
| POST | `/auth/logout` | Invalidates the session |

`GET /harvests` query filters: `search`, `category`, `district`, `min`, `max`. For a logged-in farmer, `farmerId=<own id>` includes both active and inactive personal listings.

## Authenticated endpoints

| Method | Path | Access | Action |
|---|---|---|---|
| GET | `/profile` | Any account | Current profile |
| PUT | `/profile` | Any account | Update profile |
| GET | `/dashboard` | Any account | Stats and recent data |
| POST | `/harvests` | Farmer only | Publish harvest |
| PUT | `/harvests/{id}` | Owner farmer | Edit harvest |
| DELETE | `/harvests/{id}` | Owner farmer | Soft-unlist harvest, preserve requests |
| GET | `/saved` | Buyer only | Saved harvests |
| POST | `/saved/{harvestId}` | Buyer only | Add favourite |
| DELETE | `/saved/{harvestId}` | Buyer only | Remove favourite |
| GET | `/requests` | Any account | Own requests; farmer or buyer perspective |
| POST | `/requests` | Buyer only | Send buyer request |
| GET | `/requests/{id}` | Request participants | View request |
| PATCH | `/requests/{id}` | Request participants | Update permitted status |
| GET | `/requests/{id}/messages` | Request participants | Read private conversation |
| POST | `/requests/{id}/messages` | Request participants | Send message |

## Key request bodies

**Register:**

```json
{"role":"BUYER", "firstName":"Amaya", "lastName":"Silva", "nic":"123456789V", "phone":"0771234567", "address":"Main Street", "district":"Colombo", "province":"Western", "email":"", "password":"securePassword1!", "confirmPassword":"securePassword1!"}
```

**Create / update harvest:**

```json
{"title":"Fresh Tomatoes", "category":"Vegetables", "district":"Kandy", "price":240, "quantity":350, "unit":"kg", "status":"active", "description":"Fresh tomatoes available for collection this week.", "image":""}
```

`image` is either `""` (built-in illustration) or a JPEG/PNG/WebP **base64 data URL**; images are resized client-side and limited by the API to roughly 225 KB.

**Send request:** `{"harvestId":0,"quantity":25,"message":"Can I collect on Friday?"}`

**Change status:** `{"status":"accepted"}`. Valid transitions are:

- Farmer: `pending` → `accepted` or `declined`; `accepted` → `completed`.
- Buyer: `pending` or `accepted` → `cancelled`.
- Accepting deducts reserved stock inside one database transaction. Cancelling an accepted request restores it. Completed requests remain historical records.

**Send message:** `{"text":"Yes, pickup on Friday is possible."}`

## Browser and curl examples

The frontend uses `fetch('/api/...', { credentials:'same-origin' })`. To inspect the API manually:

```bash
curl http://127.0.0.1:8080/api/harvests
curl http://127.0.0.1:8080/api/health
curl -c cookies.txt -H 'Content-Type: application/json' \
  -d '{"nic":"DEMOBUYER","password":"buyer1234"}' \
  http://127.0.0.1:8080/api/auth/login
curl -b cookies.txt http://127.0.0.1:8080/api/dashboard
```

The sample API is built for a local campus project, not an Internet-exposed production service. See security limitations in the implementation guide.
