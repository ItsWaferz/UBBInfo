-- Named proximity zones, manageable independently of buildings so the admin can
-- add a zone before assigning it. Buildings still store their zone by name
-- (public.buildings.zone); this table is the catalog behind the dropdown.

create table if not exists public.zones (
  id   uuid primary key default gen_random_uuid(),
  name text unique not null
);

-- Seed from whatever zones are already used by buildings.
insert into public.zones (name)
select distinct btrim(zone) from public.buildings
where zone is not null and btrim(zone) <> ''
on conflict (name) do nothing;

-- RLS mirrors buildings: everyone reads, only admins write.
alter table public.zones enable row level security;

drop policy if exists "zones_select_all" on public.zones;
create policy "zones_select_all" on public.zones
  for select to authenticated using (true);

drop policy if exists "zones_admin_write" on public.zones;
create policy "zones_admin_write" on public.zones
  for all to authenticated using (public.is_admin()) with check (public.is_admin());
