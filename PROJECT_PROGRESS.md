# Website Project Progress

## Project Overview

### Project Goal

Build a personal website for mainland China access with the following capabilities:

- Personal blog publishing
- Algorithm study workspace
- Daily check-ins and learning records
- Study note upload, categorization, preview, and annotation
- Editable learning roadmap
- Self-hosted deployment with room for long-term iteration

### Workspace

`F:\website`

### Current Overall Status

- Current phase: frontend core-feature version complete + backend file persistence in place
- Current focus: finish blog content flow, stabilize production build, and prepare for cloud deployment
- Deployment direction: self-hosted cloud server + domain + ICP filing + Caddy
- Target region: mainland China

## Status Definitions

- `Not started`: no execution yet
- `In progress`: actively being worked on
- `Completed`: implemented and verified
- `Blocked`: cannot continue until another issue or external dependency is resolved

## Current Snapshot

| Area | Status | Priority | Current Assessment | Notes |
| --- | --- | --- | --- | --- |
| Planning and structure | Completed | P0 | Stable | Stack, repo layout, and baseline docs are defined |
| Frontend initialization | Completed | P0 | Stable | Nuxt app, dependencies, and local dev flow are working |
| Frontend core pages | Completed | P0 | Usable | Home, Blog, Algorithms, About, Notes, and Roadmap pages are implemented |
| Study note feature set | Completed | P0 | Usable | Upload, list, tag filtering, detail, Markdown/PDF preview, and annotations are working |
| Markdown / PDF annotations | Completed | P0 | First usable version complete | Markdown highlights, PDF text highlights, anchor annotations, edit, and delete are wired |
| Algorithm roadmap | Completed | P1 | Editable | Mind-map style roadmap now supports editing, drag and drop, and persistence |
| Backend initialization | Completed | P0 | Stable | Spring Boot Maven project is in place |
| Backend first API slice | Completed | P0 | Shaped up | Health, check-ins, notes, annotations, and roadmap APIs exist |
| Backend persistence and storage | In progress | P0 | Partially landed | Notes, annotations, check-ins, and roadmap use file persistence; SQLite is still pending |
| Deployment templates | Completed | P1 | Ready as templates | Caddy example and deploy scripts exist |
| Frontend production build | Blocked | P0 | Still needs work | `npm run build` is still blocked in Nitro packaging |
| Cloud deployment / domain / ICP | Not started | P0 | Still pending | Server is rented, but real rollout and filing have not started |

## Completed Work

### Planning and architecture

- [x] Separate the new website from the old blog repository
- [x] Create an independent workspace
- [x] Confirm the main stack: Nuxt + Spring Boot + Caddy
- [x] Add architecture and stack decision documents

### Frontend

- [x] Initialize the Nuxt frontend project
- [x] Install dependencies and run local development
- [x] Build the main layout, navigation, and shared visual style
- [x] Implement Home, Blog, Algorithms, and About first versions
- [x] Add a real check-in panel and heatmap direction for algorithms
- [x] Add a dedicated study notes page at `/notes`
- [x] Add note listing, tag filtering, summary cards, and upload heatmap
- [x] Add study note detail pages with Markdown, code, and PDF preview
- [x] Add Markdown rendering improvements including code and formula support
- [x] Add Markdown annotations with highlight, linking, edit, and delete
- [x] Add PDF original preview, PDF.js annotation mode, text-highlight annotations, and anchor annotations
- [x] Add a dedicated roadmap page for algorithm learning
- [x] Add roadmap editing and drag-and-drop node movement

### Backend

- [x] Initialize the Spring Boot backend project
- [x] Add `GET /api/health`
- [x] Add `GET /api/checkins` and `POST /api/checkins`
- [x] Add study note upload API
- [x] Add note list and note detail APIs
- [x] Add note annotation create / read / update / delete APIs
- [x] Add roadmap read / save APIs
- [x] Add `website.storage` configuration structure
- [x] Verify backend tests with `mvn test`

### Storage and persistence

- [x] Persist study note metadata to file storage
- [x] Persist note annotations to file storage
- [x] Persist check-ins to file storage
- [x] Persist algorithm roadmap to file storage
- [x] Confirm current note files are stored under `F:\website\backend\uploads\notes`

## Work In Progress

- [ ] Finish the real blog content flow, especially article detail and richer content APIs
- [ ] Complete deploy-ready frontend polish, including footer filing placeholder and final responsive cleanup
- [ ] Upgrade core data persistence from file-based storage to SQLite
- [ ] Fix the remaining Nuxt production build blocker
- [ ] Normalize storage paths before cloud deployment

## Blockers and Risks

- [ ] Nuxt production build is still blocked
  - `npm run build` is still failing in Nitro packaging
- [ ] Storage path is still relative to backend startup directory
  - Current note data is landing in `backend/uploads`, which is fine locally but should be stabilized before deployment
- [ ] SQLite is still not wired in
  - File persistence works, but a database-backed setup will be more stable for later growth
- [ ] Real cloud rollout has not started yet
  - Domain binding, ICP, and deployment wiring are still pending

## Main Phases

### Phase 1. Planning and Architecture

- [x] Separate the new website from the old blog repository
- [x] Create an independent workspace
- [x] Confirm tech stack
- [x] Add baseline documentation
- [ ] Tighten the V1 production boundary even further if needed

### Phase 2. Local Development Environment

- [x] Initialize frontend in `F:\website\frontend`
- [x] Initialize backend in `F:\website\backend`
- [x] Initialize deployment files in `F:\website\deploy`
- [x] Initialize working documentation

### Phase 3. Frontend Development

- [x] Design homepage
- [x] Design blog page
- [x] Design algorithm study page
- [x] Design about page
- [x] Build dedicated study notes page and detail page
- [x] Build study note filtering, previews, and annotation UX
- [x] Build dedicated algorithm roadmap page
- [ ] Add real article detail page
- [ ] Finish footer filing area
- [ ] Finish remaining responsive polish

### Phase 4. Backend Development

- [x] Create backend skeleton
- [x] Implement health check endpoint
- [x] Implement check-in endpoints
- [x] Implement note upload / list / detail APIs
- [x] Implement note annotation create / read / update / delete APIs
- [x] Implement roadmap persistence APIs
- [ ] Introduce SQLite-backed persistence
- [ ] Add blog content APIs

### Phase 5. Storage and Data

- [x] Add file-based persistence for notes
- [x] Add file-based persistence for annotations
- [x] Add file-based persistence for check-ins
- [x] Add file-based persistence for roadmap
- [ ] Normalize all storage paths for deployment
- [ ] Define SQLite migration path
- [ ] Define backup strategy for deployment

### Phase 6. Deployment and Launch

- [ ] Fix frontend production build
- [ ] Freeze deployable frontend output
- [ ] Package and upload backend service
- [ ] Configure Caddy
- [ ] Configure HTTPS
- [ ] Configure service auto-start
- [ ] Bind domain and verify public access
- [ ] Start ICP filing process

## Next Step Plan

### Priority 0: unblock deployment

1. Fix the Nuxt production build issue so the frontend can actually be deployed.
2. Normalize backend storage paths so note and annotation data stop depending on the launch directory.
3. Add the footer filing placeholder and finish the remaining responsive polish.

### Priority 0: strengthen data layer

1. Move check-ins and future core data toward SQLite.
2. Decide the exact SQLite file location and migration path.
3. Keep current APIs stable while changing the persistence layer behind them.

### Priority 1: finish the blog content line

1. Add the article detail page.
2. Add backend content/detail endpoints for blog entries.
3. Turn the blog section from a showcase page into a real readable flow.

### Priority 1: continue polishing study notes

1. Improve compatibility for old PDF annotations that were saved before selection rectangles existed.
2. Continue refining annotation positioning and microcopy.
3. Add search / file-type filters / management polish if needed.

## Recommended Immediate Actions

1. Fix the Nitro production build failure in `npm run build`.
2. Convert note storage to a fixed absolute path before deployment.
3. Add blog article detail page and content API.
4. Move check-ins to SQLite-backed persistence.
5. Start organizing server directories, domain setup, and filing preparation.

## Decision Notes

| Item | Current Value | Notes |
| --- | --- | --- |
| Cloud server | Purchased | Ubuntu 22.04, 2 vCPU / 2 GiB / 40 GiB |
| Domain | TBD | `.com` or `.cn` recommended |
| Filing subject | TBD | Personal filing preferred |
| Frontend framework | Nuxt | Local dev is working |
| Backend framework | Spring Boot | Core APIs and file persistence are working |
| Database | SQLite preferred | Not implemented yet |
| Current note storage | File persistence | Currently under `backend/uploads/notes` |
| Current annotation storage | File persistence | Currently under `backend/uploads/notes/annotations.json` |
| Deployment method | Caddy + frontend/backend split | Templates exist, live rollout not started |

## Verification Notes

### 2026-04-21

- Verified backend `mvn test` passes
- Verified frontend `nuxi prepare` passes multiple times during recent UI changes
- Verified uploaded notes, metadata, and annotations currently land under `F:\website\backend\uploads\notes`
- Verified first usable Markdown and PDF annotation flow exists
- Verified roadmap editing and persistence are in place

### 2026-04-20

- Confirmed first-version frontend pages already existed in the codebase
- Confirmed backend health and check-in APIs already existed in the codebase
- Confirmed backend `mvn test` passed in a writable environment
- Confirmed frontend `npm run build` was still blocked in Nitro packaging

## Change Log

### 2026-04-21

- Completed study note upload, listing, tag filtering, detail page, and upload heatmap
- Completed Markdown rendering improvements, code highlighting, and formula rendering
- Completed Markdown annotation creation, linking, editing, and deletion
- Completed PDF original preview, PDF.js annotation mode, text-highlight annotations, and anchor annotations
- Completed algorithm roadmap standalone page, editing, and drag-and-drop movement
- Completed file persistence for notes and annotations
- Audited real storage paths and updated project progress documentation

### 2026-04-20

- Created new workspace at `F:\website`
- Added initial Chinese and English progress docs
- Initialized Nuxt frontend skeleton and installed dependencies
- Initialized Spring Boot backend skeleton
- Added deployment templates
- Implemented Home, Blog, Algorithms, and About first versions
- Added first backend APIs for health and check-ins
- Verified backend tests and identified the current frontend production build blocker
