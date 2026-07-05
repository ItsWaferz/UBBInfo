// Supabase Edge Function: create-user
// Creates a new auth user + profile + primary role. Admin-only.
//
// The service-role key is read from the runtime secret auto-injected by the
// Edge Functions platform — you do NOT need to set it manually, and it never
// reaches the client.
//
// Deploy via dashboard: Edge Functions -> Create a function -> name it
// "create-user" -> paste this file -> Deploy. Keep "Verify JWT" enabled.

import { createClient } from 'https://esm.sh/@supabase/supabase-js@2';

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers':
    'authorization, x-client-info, apikey, content-type',
  'Access-Control-Allow-Methods': 'POST, OPTIONS',
};

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, 'Content-Type': 'application/json' },
  });
}

// Derive short_name ("M. Corneliu") + initials ("MC") from a full name
function deriveNames(fullName: string) {
  const tokens = fullName.trim().split(/\s+/).filter(Boolean);
  if (tokens.length === 0) return { short_name: fullName, initials: '' };
  if (tokens.length === 1) {
    return { short_name: tokens[0], initials: tokens[0][0]?.toUpperCase() ?? '' };
  }
  const initials = (tokens[0][0] + tokens[1][0]).toUpperCase();
  const short_name = `${tokens[0][0]}. ${tokens.slice(1).join(' ')}`;
  return { short_name, initials };
}

Deno.serve(async (req) => {
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders });
  }
  if (req.method !== 'POST') {
    return json({ error: 'Metodă neacceptată' }, 405);
  }

  const SUPABASE_URL = Deno.env.get('SUPABASE_URL')!;
  // Server-side Deno edge function; the key comes from the platform env,
  // not from client code.
  const SERVICE_ROLE = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!; // ship-safe-ignore SUPABASE_SERVICE_KEY_CLIENT
  const ANON = Deno.env.get('SUPABASE_ANON_KEY')!;

  // Service-role client (bypasses RLS) for privileged operations
  const admin = createClient(SUPABASE_URL, SERVICE_ROLE, {
    auth: { autoRefreshToken: false, persistSession: false },
  });

  try {
    // 1. Identify + authorize the caller (must be an administrator)
    const authHeader = req.headers.get('Authorization') ?? '';
    const callerClient = createClient(SUPABASE_URL, ANON, {
      global: { headers: { Authorization: authHeader } },
      auth: { autoRefreshToken: false, persistSession: false },
    });
    const {
      data: { user: caller },
      error: callerErr,
    } = await callerClient.auth.getUser();
    if (callerErr || !caller) return json({ error: 'Neautentificat' }, 401);

    const { data: roleRows } = await admin
      .from('user_roles')
      .select('roles(name)')
      .eq('user_id', caller.id);
    const isAdmin = (roleRows ?? []).some(
      (r: { roles?: { name?: string } }) => r.roles?.name === 'administrator'
    );
    if (!isAdmin) {
      return json({ error: 'Acces interzis — doar administratorii pot crea conturi.' }, 403);
    }

    // 2. Validate input
    const body = await req.json().catch(() => ({}));
    const email = (body.email ?? '').trim();
    const password = body.password ?? '';
    const full_name = (body.full_name ?? '').trim();
    const role_name = (body.role_name ?? '').trim();
    const extra = body.profile ?? {}; // optional profile fields

    if (!email || !password || !full_name || !role_name) {
      return json({ error: 'Email, parolă, nume și rol sunt obligatorii.' }, 400);
    }
    if (password.length < 6) {
      return json({ error: 'Parola trebuie să aibă cel puțin 6 caractere.' }, 400);
    }

    // 3. Look up the requested role
    const { data: roleRow, error: roleErr } = await admin
      .from('roles')
      .select('id, name')
      .eq('name', role_name)
      .single();
    if (roleErr || !roleRow) return json({ error: 'Rol invalid.' }, 400);

    // 4. Create the auth user (email auto-confirmed so they can log in now)
    const { data: created, error: createErr } = await admin.auth.admin.createUser({
      email,
      password,
      email_confirm: true,
    });
    if (createErr || !created?.user) {
      return json({ error: createErr?.message ?? 'Nu am putut crea contul.' }, 400);
    }
    const newId = created.user.id;

    // 5. Insert profile (rollback the auth user on failure)
    const { short_name, initials } = deriveNames(full_name);
    const allowed = [
      'student_id', 'faculty', 'specialization', 'study_year',
      'transport_id', 'group_name', 'financing', 'phone',
      'personal_email', 'iban', 'cnp', 'id_series', 'address',
      'academic_rank', 'honorifics',
    ];
    const profileRow: Record<string, unknown> = {
      id: newId,
      full_name,
      short_name,
      initials,
      email,
    };
    for (const k of allowed) {
      if (extra[k] !== undefined && extra[k] !== '') profileRow[k] = extra[k];
    }

    const { error: pErr } = await admin.from('profiles').insert(profileRow);
    if (pErr) {
      await admin.auth.admin.deleteUser(newId);
      return json({ error: `Eroare la crearea profilului: ${pErr.message}` }, 400);
    }

    // 6. Assign the primary role (rollback on failure)
    const { error: rErr } = await admin
      .from('user_roles')
      .insert({ user_id: newId, role_id: roleRow.id, is_primary: true });
    if (rErr) {
      await admin.from('profiles').delete().eq('id', newId);
      await admin.auth.admin.deleteUser(newId);
      return json({ error: `Eroare la atribuirea rolului: ${rErr.message}` }, 400);
    }

    // 7. For professors: assign selected courses (professor_courses)
    const courseIds: string[] = Array.isArray(body.courses) ? body.courses : [];
    if (role_name === 'profesor' && courseIds.length) {
      const rows = courseIds.map((cid: string) => ({
        professor_id: newId,
        course_id: cid,
        type: 'CURS',
        student_count: 0,
        study_year_label: '',
      }));
      const { error: pcErr } = await admin.from('professor_courses').insert(rows);
      if (pcErr) console.error('professor_courses insert failed:', pcErr.message);
    }

    return json({ ok: true, id: newId, email }, 200);
  } catch (e) {
    return json({ error: String((e as Error)?.message ?? e) }, 500);
  }
});
