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

  // No Content, or a 200 with an empty body (void endpoints) — return null
  // instead of throwing on res.json() of an empty string.
  if (res.status === 204) return null;
  const text = await res.text();
  return text ? JSON.parse(text) : null;
}

export const api = {
  get: (path, opts) => apiFetch(path, { ...opts, method: 'GET' }),
  post: (path, body, opts) => apiFetch(path, { ...opts, method: 'POST', body }),
  put: (path, body, opts) => apiFetch(path, { ...opts, method: 'PUT', body }),
  patch: (path, body, opts) => apiFetch(path, { ...opts, method: 'PATCH', body }),
  del: (path, opts) => apiFetch(path, { ...opts, method: 'DELETE' }),
  upload: (path, formData, opts) => apiUpload(path, formData, opts),
  download: (path, opts) => apiDownload(path, opts),
};

/**
 * Fetch a binary response (e.g. a generated PDF) and trigger a browser
 * download. Attaches the Supabase JWT; supports GET and JSON-body POST.
 * Returns the suggested filename.
 */
export async function apiDownload(path, { method = 'GET', body } = {}) {
  const {
    data: { session },
  } = await supabase.auth.getSession();
  const token = session?.access_token;

  const res = await fetch(`${API_BASE}${path}`, {
    method,
    headers: {
      ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  if (!res.ok) {
    let detail = '';
    try {
      detail = (await res.json())?.message || '';
    } catch {
      /* no JSON body */
    }
    const err = new Error(detail || `Download failed (${res.status})`);
    err.status = res.status;
    throw err;
  }

  const cd = res.headers.get('Content-Disposition') || '';
  const match = /filename="?([^"]+)"?/i.exec(cd);
  const filename = match ? match[1] : 'document.pdf';

  const blob = await res.blob();
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
  return filename;
}

/**
 * Multipart upload helper — attaches the Supabase JWT but lets the browser set
 * the multipart Content-Type/boundary (so do NOT set Content-Type here).
 */
export async function apiUpload(path, formData, { method = 'POST' } = {}) {
  const {
    data: { session },
  } = await supabase.auth.getSession();
  const token = session?.access_token;

  const res = await fetch(`${API_BASE}${path}`, {
    method,
    headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: formData,
  });

  if (!res.ok) {
    let detail = '';
    try {
      detail = (await res.json())?.message || '';
    } catch {
      /* no JSON body */
    }
    const err = new Error(detail || `Upload failed (${res.status})`);
    err.status = res.status;
    throw err;
  }
  if (res.status === 204) return null;
  return res.json();
}
