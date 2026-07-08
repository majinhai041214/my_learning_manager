# Website Workspace

This workspace contains the new version of the personal website project.

## Structure

```text
F:/website
├─ frontend   # Nuxt frontend
├─ backend    # Spring Boot backend
├─ deploy     # Caddy and deployment scripts
└─ docs       # Project documentation
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

## Runtime Data

The backend now stores notes, check-ins, roadmap data, and annotations in an independent data directory by default:

- Local/project default: `../uploads` relative to the backend working directory
- In this workspace, that means `F:\website\uploads`
- On the server, if the backend runs from `/opt/www/website/backend`, that means `/opt/www/website/uploads`

You can override it with:

```powershell
$env:WEBSITE_STORAGE_BASE_DIR = 'D:\website-data'
```

or on Linux:

```bash
export WEBSITE_STORAGE_BASE_DIR=/opt/website-data
```

On startup, if the new storage directory is empty and a legacy `backend/uploads` directory is found, the backend will automatically copy the existing data into the independent root-level `uploads` directory.

Blog content is stored in the same runtime data directory:

- Markdown posts: `uploads/blog/posts`
- Blog images: `uploads/blog/images`

On the server, with the default deployment layout, these become:

- `/opt/www/website/uploads/blog/posts`
- `/opt/www/website/uploads/blog/images`
