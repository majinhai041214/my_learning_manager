# Website Workspace

This workspace contains the new version of the personal website project.

## Structure

```text
F:/website
├─ frontend   # Nuxt frontend
├─ backend    # Spring Boot backend
├─ deploy     # Caddy and deployment scripts
├─ docs       # Project documentation
└─ uploads    # Uploads and local runtime data
```

## Stack

- Frontend: Nuxt
- Backend: Spring Boot
- Deploy: Caddy + shell scripts

## Quick Start

Frontend:

```powershell
cd F:\website\frontend
npm.cmd install
npm.cmd run dev
```

Backend:

```powershell
cd F:\website\backend
mvn spring-boot:run
```

