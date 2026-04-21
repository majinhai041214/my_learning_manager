# Website Project Progress

## Project Goal

Build a personal website that supports:

- Personal blog publishing
- An algorithm study workspace
- Daily check-ins and learning records
- Future note upload, code upload, and annotations
- Self-hosted deployment with room for long-term iteration

Primary target:

- Visitors in mainland China

Workspace:

- `F:\website`

## Current Status

- Current phase: frontend first-version implementation + backend basic API prototype
- Current focus: refine the first frontend version, add real persistence/upload capabilities, and unblock production build/deployment
- Deployment direction: self-hosted cloud server with domain, ICP filing, HTTPS, and Caddy reverse proxy

## Status Snapshot

| Area | Status | Notes |
| --- | --- | --- |
| Workspace and docs | In progress | Workspace, progress docs, stack decision, and architecture docs already exist |
| Frontend initialization | Completed | Nuxt project created, dependencies installed, local development already runs |
| Frontend first-version pages | Completed | Home, Blog, Algorithms, and About pages are implemented |
| Frontend responsive/detail refinement | In progress | Basic responsive layout exists, but article detail and footer filing area are still missing |
| Backend initialization | Completed | Spring Boot project skeleton is ready |
| Backend basic API | In progress | `GET /api/health`, `GET /api/checkins`, and `POST /api/checkins` already exist |
| Backend persistence/storage design | In progress | Storage config exists, but check-ins still use in-memory repository and SQLite is not wired in yet |
| Deployment templates | Completed | Caddy example and frontend/backend deployment scripts already exist |
| Production build verification | Blocked | Backend tests pass, but Nuxt production build still fails at Nitro packaging on Windows |
| Cloud server / domain / ICP | Not started | Still requires purchase and filing preparation |

## Completed Work

### Planning and structure

- [x] Separate the new website from the old blog repository
- [x] Create independent workspace folders
- [x] Confirm the main stack: Nuxt + Spring Boot + Caddy
- [x] Add architecture and stack decision documents

### Frontend

- [x] Initialize the Nuxt frontend project
- [x] Install dependencies and run local development
- [x] Build the main layout, navigation, and footer
- [x] Complete first-version pages for Home, Blog, Algorithms, and About
- [x] Add a static algorithm check-in calendar component for product direction demo

### Backend

- [x] Initialize the Spring Boot backend project
- [x] Add health endpoint
- [x] Add check-in list/create endpoints
- [x] Add DTO / service / repository structure for the first backend slice
- [x] Add backend test scaffold and verify Maven tests can pass

### Deployment

- [x] Add Caddy example configuration
- [x] Add frontend deployment script template
- [x] Add backend deployment script template

## Work In Progress

- [ ] Refine the first frontend version into a deployable V1 experience
- [ ] Add article detail page and richer blog content structure
- [ ] Replace in-memory check-in storage with persistent storage
- [ ] Design note/upload/annotation data model
- [ ] Resolve Nuxt production build issue on Windows / current environment

## Blockers and Risks

- [ ] Nuxt production build is not fully passing yet
  - Client and server builds complete
  - Nitro packaging currently fails with a `readlink` / `EISDIR` issue around `vue-bundle-renderer`
- [ ] Deployment is not ready until production build is stable
- [ ] Mainland launch still depends on cloud server purchase, domain registration, and ICP filing

## Main Phases

### Phase 1. Planning and Architecture

- [x] Separate the new website from the old blog repository
- [x] Create independent workspace folders
- [x] Confirm version 1 core direction
- [x] Confirm tech stack
- [ ] Finalize a tighter V1 feature boundary

### Phase 2. Infrastructure Preparation

- [ ] Choose cloud vendor and server product
- [ ] Select region
- [ ] Purchase server
- [ ] Register domain
- [ ] Configure DNS resolution
- [ ] Prepare ICP filing materials
- [ ] Submit ICP filing
- [ ] Wait for filing approval

Recommended baseline:

- Server: low-cost mainland cloud server
- OS: Ubuntu 24.04 LTS
- Region: Guangzhou preferred
- Domain: `.com` or `.cn`

### Phase 3. Local Development Environment

- [x] Initialize frontend project in `F:\website\frontend`
- [x] Initialize backend project in `F:\website\backend`
- [x] Initialize deployment files in `F:\website\deploy`
- [x] Create documentation in `F:\website\docs`
- [x] Create upload test directory in `F:\website\uploads`

### Phase 4. Frontend Development

- [x] Design homepage
- [x] Design algorithm study page
- [x] Design blog page
- [x] Design about page
- [ ] Design article detail page
- [ ] Finish responsive/detail refinement
- [ ] Prepare ICP footer area for future filing number display
- [ ] Optimize static resource loading for low-bandwidth access

### Phase 5. Backend Development

- [x] Create backend framework skeleton
- [x] Implement health check endpoint
- [x] Implement basic check-in endpoints
- [ ] Define persistent storage model
- [ ] Implement upload API
- [ ] Implement note list/detail API
- [ ] Implement annotation create/read API

### Phase 6. Data and Storage Design

- [ ] Decide the first production-ready database setup
- [ ] Replace in-memory check-in storage with SQLite or another chosen V1 store
- [ ] Design file storage layout
- [ ] Design note metadata format
- [ ] Design annotation data structure
- [ ] Design backup strategy

### Phase 7. Deployment and Launch

- [ ] Fix frontend production build
- [ ] Verify deployable frontend output
- [ ] Upload frontend build output
- [ ] Upload backend service
- [ ] Configure Caddy reverse proxy
- [ ] Configure HTTPS
- [ ] Configure service auto-start
- [ ] Bind domain and test public access

## Next Step Plan

### Priority 0: unblock the development path

1. Fix the Nuxt production build issue so the frontend can actually be deployed.
2. Freeze the V1 frontend scope: keep Home / Blog / Algorithms / About, and add only one article detail template for now.
3. Add ICP filing footer placeholder and finish the remaining responsive cleanup.

### Priority 1: make backend data real

1. Replace the current in-memory check-in repository with persistent storage.
2. Finalize the V1 storage decision, with SQLite as the default target.
3. Keep the existing health and check-in API stable while adding storage migration.

### Priority 1: prepare the next backend slice

1. Design note metadata structure.
2. Design upload directory rules under `uploads/`.
3. Implement the first upload API, then note list/detail API.

### Priority 0 outside code

1. Decide cloud vendor, region, and baseline server spec.
2. Register the domain.
3. Start ICP filing preparation as early as possible because approval will take time.

## Recommended Immediate Actions

These are the next 5 concrete actions in order:

1. Investigate and fix the Nuxt production build failure.
2. Add the article detail page and filing footer placeholder.
3. Introduce SQLite-backed persistence for check-ins.
4. Define note/upload/annotation data models in the backend.
5. Finalize server + domain + filing decisions.

## Decision Notes

- Server vendor: TBD
- Server region: Guangzhou preferred
- Domain: TBD
- Filing subject: personal filing preferred
- Frontend framework: Nuxt
- Backend framework: Spring Boot
- Database: SQLite preferred for V1, not implemented yet
- Deployment method: Caddy + frontend/backend split

## Verification Notes

### 2026-04-20

- Confirmed that frontend first-version pages already exist in the codebase
- Confirmed backend health and check-in APIs already exist in the codebase
- Confirmed backend `mvn test` can pass in a writable environment
- Confirmed frontend `npm run build` is still blocked at Nitro packaging

## Change Log

### 2026-04-20

- Created new workspace at `F:\website`
- Added initial progress documentation
- Initialized Nuxt frontend skeleton and installed dependencies
- Initialized Spring Boot backend skeleton
- Added deployment templates
- Implemented first frontend pages: Home, Blog, Algorithms, About
- Added first backend APIs for health and check-ins
- Verified backend tests and identified the current frontend production build blocker
- Updated project progress to reflect actual code status and next execution plan
