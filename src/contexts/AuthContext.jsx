import { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { supabase } from '../supabaseClient';
import { api } from '../api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [loading, setLoading] = useState(true); // checking session on mount
  const [user, setUser] = useState(null);
  const [profile, setProfile] = useState(null);
  const [roles, setRoles] = useState([]); // [{ name, label, icon, badge_class, home_page, is_primary }]
  const [currentRole, setCurrentRole] = useState(null); // active role name string

  // Load profile + roles for a given auth user.
  // Auth stays in Supabase; profile/roles now come from the Spring Boot API
  // (GET /api/me/profile) instead of direct Supabase DB queries.
  const loadUserData = useCallback(async (authUser) => {
    const me = await api.get('/api/me/profile');

    const flatRoles = me.roles || [];
    // Primary role first, then the rest
    flatRoles.sort((a, b) => (b.is_primary ? 1 : 0) - (a.is_primary ? 1 : 0));

    const primary = flatRoles.find((r) => r.is_primary) || flatRoles[0] || null;

    setProfile(me.profile);
    setRoles(flatRoles);
    setCurrentRole(primary ? primary.name : null);
    setUser(authUser);
  }, []);

  // Expose the current Supabase access token (JWT) for ad-hoc API calls.
  const getToken = useCallback(async () => {
    const {
      data: { session },
    } = await supabase.auth.getSession();
    return session?.access_token ?? null;
  }, []);

  // On mount: check for an existing session (loading screen stays up until done)
  useEffect(() => {
    let active = true;

    (async () => {
      try {
        const {
          data: { session },
        } = await supabase.auth.getSession();
        if (session?.user) {
          await loadUserData(session.user);
        }
      } catch (err) {
        console.error('Session check failed:', err);
      } finally {
        if (active) setLoading(false);
      }
    })();

    return () => {
      active = false;
    };
  }, [loadUserData]);

  const login = useCallback(
    async (email, password) => {
      const { data, error } = await supabase.auth.signInWithPassword({
        email,
        password,
      });
      if (error) throw error;
      await loadUserData(data.user);
      return data.user;
    },
    [loadUserData]
  );

  const logout = useCallback(async () => {
    await supabase.auth.signOut();
    setUser(null);
    setProfile(null);
    setRoles([]);
    setCurrentRole(null);
  }, []);

  const switchRole = useCallback(
    (roleName) => {
      if (roles.some((r) => r.name === roleName)) {
        setCurrentRole(roleName);
      }
    },
    [roles]
  );

  const activeRole = roles.find((r) => r.name === currentRole) || null;

  const value = {
    loading,
    user,
    profile,
    setProfile,
    roles,
    currentRole,
    activeRole,
    isAuthenticated: !!user,
    login,
    logout,
    switchRole,
    getToken,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
