-- ============================================================
-- Recreate admin account if it was accidentally deleted
-- Email: admin@ubbcluj.ro  Password: admin123
-- ============================================================

DO $$
DECLARE
  admin_uid uuid := 'ad000000-0000-0000-0000-000000000001';
  admin_role_id uuid;
BEGIN
  SELECT id INTO admin_role_id FROM public.roles WHERE name = 'administrator';

  -- Create auth user (skip if exists)
  INSERT INTO auth.users (
    instance_id, id, aud, role, email, encrypted_password,
    email_confirmed_at, created_at, updated_at,
    raw_app_meta_data, raw_user_meta_data,
    confirmation_token, recovery_token, email_change_token_new, email_change
  ) VALUES (
    '00000000-0000-0000-0000-000000000000', admin_uid, 'authenticated', 'authenticated',
    'admin@ubbcluj.ro', extensions.crypt('admin123', extensions.gen_salt('bf')),
    now(), now(), now(),
    '{"provider":"email","providers":["email"]}', '{}',
    '', '', '', ''
  ) ON CONFLICT (id) DO NOTHING;

  -- Identity
  INSERT INTO auth.identities (
    provider_id, user_id, identity_data, provider, last_sign_in_at, created_at, updated_at
  ) VALUES (
    admin_uid::text, admin_uid,
    jsonb_build_object('sub', admin_uid::text, 'email', 'admin@ubbcluj.ro'),
    'email', now(), now(), now()
  ) ON CONFLICT DO NOTHING;

  -- Profile
  INSERT INTO public.profiles (id, full_name, short_name, initials, email, faculty)
  VALUES (
    admin_uid, 'Administrator', 'Admin', 'AD',
    'admin@ubbcluj.ro', 'Matematică și Informatică'
  ) ON CONFLICT (id) DO UPDATE
    SET full_name = EXCLUDED.full_name;

  -- Role: administrator
  INSERT INTO public.user_roles (user_id, role_id, is_primary)
  VALUES (admin_uid, admin_role_id, true)
  ON CONFLICT (user_id, role_id) DO NOTHING;

END $$;
