import { supabase } from './supabaseClient';

// Base URL of the Spring Boot backend. Override with VITE_API_URL in production.
export const API_BASE =
  import.meta.env.VITE_API_URL || 'http://localhost:8080';

/**
 * Fetch wrapper that attaches the current Supabase access token as a
 * Bearer header and talks to the Spring Boot REST API.
 *
 * Auth still lives in Supabase (supabase-js); this only carries the JWT the
 * backend validates. Throws an Error (with .status) on non-2xx responses.
 */
export async function apiFetch(path, { method = 'GET', body, headers, signal } = {}) {
  const {
    data: { session },
  } = await supabase.auth.getSession();
  const token = session?.access_token;

  const res = await fetch(`${API_BASE}${path}`, {
    method,
    signal,
    headers: {
      ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...headers,
    },
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  if (!res.ok) {
    let detail = '';
    try {
      detail = (await res.json())?.message || '';
    } catch {
      /* response had no JSON body */
    }
    const err = new Error(detail || `Request failed (${res.status})`);
    err.status = res.status;
    throw err;
  }

  // 204 No Content
  if (res.status === 204) return null;
  return res.json();
}

export const api = {
  get: (path, opts) => apiFetch(path, { ...opts, method: 'GET' }),
  post: (path, body, opts) => apiFetch(path, { ...opts, method: 'POST', body }),
  put: (path, body, opts) => apiFetch(path, { ...opts, method: 'PUT', body }),
  patch: (path, body, opts) => apiFetch(path, { ...opts, method: 'PATCH', body }),
  del: (path, opts) => apiFetch(path, { ...opts, method: 'DELETE' }),
};
