# WarrantyOS – Stable Separated Frontend + Backend

This release is intentionally a clean, self-contained project. Do not merge files from older Warranty Platform copies into it.

## Structure

- `backend/` – Spring Boot 3.2.5, Java 21, MySQL/JPA, JWT, WebSocket
- `frontend/` – React 18 + Vite
- `docker-compose.yml` – MySQL 8.4 on host port 3307
- `.vscode/tasks.json` – one-click startup tasks

## Demo accounts

- Admin: `admin@warrantyos.com` / `Admin@123`
- Technician: `tech@warrantyos.com` / `Tech@123`
- Technician 2: `tech2@warrantyos.com` / `Tech2@123`
- Customer: `customer@warrantyos.com` / `Customer@123`
- Customer 2: `priya.sharma@example.com` / `Customer@123`
- Customer 3: `arjun.rao@example.com` / `Customer@123`

## Run

Open the project root in VS Code and press `Ctrl+Shift+B`, then choose `WarrantyOS: Start all`.

Manual backend:

```cmd
D:\apache-maven-3.10.0\bin\mvn.cmd -f backend\pom.xml clean spring-boot:run
```

Manual frontend:

```cmd
cd frontend
npm install
npm run dev
```

URLs:

- Frontend: http://localhost:5173
- Backend: http://localhost:8080
- MySQL: localhost:3307

## Important

Use this project as a clean folder (recommended: `D:\WarrantyOS`). Do not run the old `D:\Warranty-platform` copy alongside it.

## Authentication troubleshooting
If a browser was used with an older WarrantyOS build, stale JWT data can remain in localStorage. The current build treats missing/invalid/expired JWTs as HTTP 401, clears the stale session, and returns to the login page. If needed, open the browser console once and run:

```js
localStorage.removeItem('warrantyos_user'); localStorage.removeItem('warrantyos_token'); location.href='/';
```

Then sign in again with the demo account.
