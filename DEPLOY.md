# Deploy — free hosting

Stack:

| Piece | Host | Notes |
|-------|------|-------|
| DB + Auth | **Supabase** | already hosted (free) — nothing to do |
| Backend (Spring Boot + Timefold) | **Google Cloud Run** | scale-to-zero, free tier, Docker build |
| Frontend (React/Vite) | **Vercel** | static, git-push auto-deploy |

Every `git push` to the connected branch redeploys (Vercel automatically; Cloud Run
via a redeploy command or a Cloud Build trigger).

---

## 1. Backend → Google Cloud Run

### One-time setup
```bash
# Install the gcloud CLI, then:
gcloud auth login
gcloud projects create ubbinfo-<something-unique>      # or reuse an existing project
gcloud config set project ubbinfo-<something-unique>
gcloud services enable run.googleapis.com cloudbuild.googleapis.com artifactregistry.googleapis.com
```

### Deploy (from the `backend/` folder — it has the Dockerfile)
```bash
cd backend
gcloud run deploy ubbinfo-api \
  --source . \
  --region europe-west1 \
  --allow-unauthenticated \
  --memory 512Mi \
  --cpu 1 \
  --timeout 300 \
  --min-instances 0 \
  --set-env-vars "SUPABASE_DB_PASSWORD=YOUR_DB_PASSWORD,SUPABASE_SERVICE_ROLE_KEY=YOUR_SERVICE_ROLE_KEY,APP_CORS_ORIGINS=http://localhost:5173"
```

- `--source .` makes Cloud Build build the image from the `Dockerfile`, then deploys it.
- The Supabase project URL / JWKS / DB host + user are already baked into
  `application.properties` (your project ref), so you only pass the **secrets**:
  - `SUPABASE_DB_PASSWORD` — Supabase → Project Settings → Database (pooler password).
  - `SUPABASE_SERVICE_ROLE_KEY` — Supabase → Project Settings → API (needed only for the
    admitted-students import; the app runs without it, import just stays disabled).
- Deploy prints a **Service URL** like `https://ubbinfo-api-xxxx.europe-west1.run.app`.
  Copy it — the frontend needs it.

> First request after idle has a cold start (~10–30s to boot the JVM). Normal for
> scale-to-zero. Set `--min-instances 1` to avoid it (leaves the free tier / small cost).

### More secure (optional): keep secrets in Secret Manager
```bash
echo -n "YOUR_DB_PASSWORD" | gcloud secrets create supabase-db-password --data-file=-
echo -n "YOUR_SERVICE_ROLE_KEY" | gcloud secrets create supabase-service-role-key --data-file=-
gcloud run deploy ubbinfo-api --source . --region europe-west1 --allow-unauthenticated \
  --memory 512Mi --timeout 300 \
  --set-secrets "SUPABASE_DB_PASSWORD=supabase-db-password:latest,SUPABASE_SERVICE_ROLE_KEY=supabase-service-role-key:latest" \
  --set-env-vars "APP_CORS_ORIGINS=https://YOUR-APP.vercel.app"
```

---

## 2. Frontend → Vercel

1. vercel.com → **Add New… → Project** → import this GitHub repo.
2. Framework preset: **Vite** (auto-detected). Build command `npm run build`, output `dist`.
   Root directory: repo root (leave default — the frontend lives at the root).
3. **Environment Variables** → add:
   - `VITE_API_URL` = the Cloud Run Service URL from step 1
     (e.g. `https://ubbinfo-api-xxxx.europe-west1.run.app`, **no trailing slash**).
4. **Deploy**. You get a URL like `https://ubbinfo.vercel.app`.

(HashRouter is used, so no SPA rewrite rules are needed.)

---

## 3. Close the loop (CORS)

The backend only accepts browser calls from origins in `APP_CORS_ORIGINS`. After the
Vercel URL exists, point the backend at it:

```bash
gcloud run services update ubbinfo-api --region europe-west1 \
  --set-env-vars "APP_CORS_ORIGINS=https://ubbinfo.vercel.app,SUPABASE_DB_PASSWORD=YOUR_DB_PASSWORD,SUPABASE_SERVICE_ROLE_KEY=YOUR_SERVICE_ROLE_KEY"
```

> `--set-env-vars` **replaces** all env vars, so pass them all together (or use
> `--update-env-vars APP_CORS_ORIGINS=...` to change just one).

Add your Supabase Auth redirect: Supabase → Authentication → URL Configuration → add
`https://ubbinfo.vercel.app` to the Site URL / redirect allow-list.

---

## Redeploying after code changes
- **Frontend**: `git push` → Vercel rebuilds automatically.
- **Backend**: `cd backend && gcloud run deploy ubbinfo-api --source . --region europe-west1`
  (or set up a Cloud Build trigger on the repo to auto-deploy on push).

## Tuning notes
- Timefold is CPU/RAM heavy. 512Mi + 1 CPU is fine for small/demo datasets. Bump
  `--memory 1Gi --cpu 2` if orar generation is slow or OOMs on large inputs.
- `ORAR_SOLVE_SECONDS` (default 8) is settable via `--set-env-vars`.
