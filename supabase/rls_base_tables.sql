-- ============================================================
-- Lock down the base tables + drop a dead SECURITY DEFINER view.
--
-- Context: the app is fully backend-mediated. The React frontend talks ONLY to
-- the Spring Boot API and Supabase Auth — it never reads these tables directly
-- through PostgREST. The backend connects over JDBC as the `postgres` role,
-- which has BYPASSRLS, so enabling RLS here does NOT affect the API.
--
-- The only thing RLS gates is the public `anon` key that ships in the frontend
-- bundle. Without RLS, anyone with that key can read every row via
-- https://<ref>.supabase.co/rest/v1/profiles — including CNP, IBAN, address,
-- phone. These tables had policies defined (grades_admin.sql) but RLS was never
-- enabled, so the policies were dormant and the tables were wide open.
--
-- Run once in the Supabase SQL editor. Idempotent — safe to re-run.
-- ============================================================

-- 1. Drop the unused `professors_public` view (fixes the CRITICAL
--    "Security Definer View" lint). Nothing in the codebase queries it — the
--    professor list for Evaluare is served by the backend API. It also granted
--    SELECT to `anon`, leaking professor names to unauthenticated callers.
drop view if exists public.professors_public;

-- 2. Enable RLS on every base table that was missing it. With RLS on and no
--    anon/authenticated policy, PostgREST denies all direct access via the
--    public key; the backend keeps working because `postgres` bypasses RLS.
--    (Section 4 then drops the leftover direct-read policies so these end up
--    deny-all, consistent with the feature tables.)
alter table public.profiles          enable row level security;
alter table public.enrollments       enable row level security;
alter table public.courses           enable row level security;
alter table public.roles             enable row level security;
alter table public.user_roles        enable row level security;
alter table public.professor_courses enable row level security;
alter table public.exams             enable row level security;
alter table public.useful_links      enable row level security;

-- NOTE: if the Supabase linter still lists any other public table under
-- "RLS Disabled in Public", enable it the same way:
--   alter table public.<name> enable row level security;

-- 3. Revoke public RPC access to the SECURITY DEFINER helper functions
--    (lints 0028 anon / 0029 authenticated "… can execute SECURITY DEFINER
--    function"). They exist ONLY to back RLS policies (is_admin / teaches_course
--    / can_view_student) and the anonymized admin-evaluations accessor. The
--    backend reimplements all of them in Java (CurrentUserService) and reads
--    evaluations via JPA; the frontend makes no .rpc() calls. So no legitimate
--    caller reaches them through PostgREST — exposing /rest/v1/rpc/is_admin etc.
--    to anon/authenticated is needless attack surface.
--
--    They MUST stay SECURITY DEFINER (they read user_roles, which now has RLS —
--    SECURITY INVOKER would recurse). Revoking the PUBLIC/anon/authenticated
--    EXECUTE grant closes the API endpoints without touching the definer
--    behaviour: the owning `postgres` role (the backend's JDBC connection, which
--    also bypasses RLS) keeps full access, so nothing breaks.
revoke execute on function public.is_admin()                     from public, anon, authenticated;
revoke execute on function public.teaches_course(uuid)           from public, anon, authenticated;
revoke execute on function public.can_view_student(uuid)         from public, anon, authenticated;
revoke execute on function public.admin_professor_evaluations()  from public, anon, authenticated;

-- 4. Drop the leftover direct-read RLS policies (performance lints 0003
--    auth_rls_initplan + 0006 multiple_permissive_policies). These policies
--    (profiles_read_own, admin_select_*, prof_*, *_select_all, etc.) are a
--    vestige of the pre-hybrid design where the frontend read Supabase directly.
--    It no longer does (only supabase.auth.* + functions.invoke — zero .from()/
--    .rpc()), and the backend bypasses RLS, so these policies never execute.
--    Removing them makes every base table deny-all (like the feature tables) —
--    which clears both performance lints AND tightens security (a permissive
--    policy is more surface than none). The definitions live in git history
--    (grades_admin.sql, v2_schema.sql, v2_fix.sql, …) if direct reads ever
--    return. The DO block drops whatever policies exist by name, so it's
--    complete and idempotent regardless of which migrations were run.
do $$
declare
  r record;
  targets text[] := array[
    'profiles','enrollments','courses','roles','user_roles','professor_courses',
    'exams','exam_registrations','professor_evaluations','useful_links',
    'buildings','rooms','orar','semester_config','vacations'
  ];
begin
  for r in
    select policyname, tablename
    from pg_policies
    where schemaname = 'public'
      and tablename = any (targets)
  loop
    execute format('drop policy if exists %I on public.%I', r.policyname, r.tablename);
  end loop;
end $$;
