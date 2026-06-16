import { useEffect, useMemo, useState } from 'react';
import { supabase } from '../../supabaseClient';
import Icon from '../../components/Icon';
import Toast from '../../components/Toast';
import CreateUserModal from '../../components/CreateUserModal';
import EditUserModal from '../../components/EditUserModal';

export default function Users() {
  const [roles, setRoles] = useState([]); // {id, name, label, badge_class}
  const [users, setUsers] = useState([]); // {id, full_name, email, student_id, roleIds:Set}
  const [loading, setLoading] = useState(true);
  const [busyKey, setBusyKey] = useState(null); // `${userId}:${roleId}` while toggling
  const [toast, setToast] = useState(null);
  const [createOpen, setCreateOpen] = useState(false);
  const [editUser, setEditUser] = useState(null);
  const [roleFilter, setRoleFilter] = useState('all');
  const [specFilter, setSpecFilter] = useState('all');
  const [search, setSearch] = useState('');

  const load = async () => {
    setLoading(true);
    const [profilesRes, rolesRes, userRolesRes] = await Promise.all([
      supabase.from('profiles').select('*'),
      supabase.from('roles').select('id, name, label, badge_class').order('name'),
      supabase.from('user_roles').select('user_id, role_id, is_primary'),
    ]);

    if (profilesRes.error || rolesRes.error || userRolesRes.error) {
      console.error('Load users failed:', profilesRes.error || rolesRes.error || userRolesRes.error);
      setLoading(false);
      return;
    }

    const rolesByUser = new Map();
    const primaryByUser = new Map();
    for (const ur of userRolesRes.data || []) {
      if (!rolesByUser.has(ur.user_id)) rolesByUser.set(ur.user_id, new Set());
      rolesByUser.get(ur.user_id).add(ur.role_id);
      if (ur.is_primary) primaryByUser.set(ur.user_id, ur.role_id);
    }

    setRoles(rolesRes.data || []);
    setUsers(
      (profilesRes.data || [])
        .map((p) => ({
          ...p,
          roleIds: rolesByUser.get(p.id) || new Set(),
          primaryRoleId: primaryByUser.get(p.id) || null,
        }))
        .sort((a, b) => (a.full_name || '').localeCompare(b.full_name || ''))
    );
    setLoading(false);
  };

  useEffect(() => {
    load();
  }, []);

  const flashToast = (variant, message) => {
    setToast({ variant, message });
    setTimeout(() => setToast(null), 3000);
  };

  const toggleRole = async (user, role) => {
    const has = user.roleIds.has(role.id);

    if (has && user.roleIds.size === 1) {
      flashToast('error', 'Un utilizator trebuie să aibă cel puțin un rol.');
      return;
    }
    if (has && user.primaryRoleId === role.id) {
      flashToast('error', 'Nu poți elimina rolul principal.');
      return;
    }

    setBusyKey(`${user.id}:${role.id}`);
    let error;
    if (has) {
      ({ error } = await supabase
        .from('user_roles')
        .delete()
        .eq('user_id', user.id)
        .eq('role_id', role.id));
    } else {
      ({ error } = await supabase
        .from('user_roles')
        .insert({ user_id: user.id, role_id: role.id, is_primary: false }));
    }
    setBusyKey(null);

    if (error) {
      console.error('Toggle role failed:', error);
      flashToast('error', 'Eroare la actualizarea rolului.');
      return;
    }

    setUsers((prev) =>
      prev.map((u) => {
        if (u.id !== user.id) return u;
        const next = new Set(u.roleIds);
        if (has) next.delete(role.id);
        else next.add(role.id);
        return { ...u, roleIds: next };
      })
    );
    flashToast('success', has ? 'Rol eliminat.' : 'Rol adăugat.');
  };

  const roleNameById = useMemo(
    () => Object.fromEntries(roles.map((r) => [r.id, r.name])),
    [roles]
  );
  const specializations = useMemo(
    () => [...new Set(users.map((u) => u.specialization).filter(Boolean))].sort(),
    [users]
  );
  const filteredUsers = useMemo(() => {
    const q = search.trim().toLowerCase();
    return users.filter((u) => {
      if (roleFilter !== 'all') {
        const names = [...u.roleIds].map((id) => roleNameById[id]);
        if (!names.includes(roleFilter)) return false;
      }
      if (specFilter !== 'all' && u.specialization !== specFilter) return false;
      if (q) {
        const hay = `${u.full_name || ''} ${u.email || ''} ${u.student_id || ''}`.toLowerCase();
        if (!hay.includes(q)) return false;
      }
      return true;
    });
  }, [users, roleFilter, specFilter, search, roleNameById]);

  if (loading) {
    return (
      <section className="card">
        <div className="card-body muted center">Se încarcă utilizatorii…</div>
      </section>
    );
  }

  return (
    <section className="card">
      <div className="card-header">
        <h2 className="card-title">
          <Icon name="manage_accounts" />
          Utilizatori ({filteredUsers.length}/{users.length})
        </h2>
        <button type="button" className="btn btn-primary btn-sm" onClick={() => setCreateOpen(true)}>
          <Icon name="person_add" />
          Adaugă utilizator
        </button>
      </div>

      <div className="filter-bar">
        <div className="input-wrap filter-search">
          <Icon name="search" className="input-icon" />
          <input
            type="text"
            placeholder="Caută nume, email, matricol…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
        <div className="input-wrap filter-select">
          <Icon name="badge" className="input-icon" />
          <select className="select-bare" value={roleFilter} onChange={(e) => setRoleFilter(e.target.value)}>
            <option value="all">Toate rolurile</option>
            {roles.map((r) => (
              <option key={r.id} value={r.name}>
                {r.label}
              </option>
            ))}
          </select>
        </div>
        <div className="input-wrap filter-select">
          <Icon name="school" className="input-icon" />
          <select className="select-bare" value={specFilter} onChange={(e) => setSpecFilter(e.target.value)}>
            <option value="all">Toate specializările</option>
            {specializations.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
        </div>
      </div>

      <div className="table-wrap">
        <table className="data-table">
          <thead>
            <tr>
              <th>Nume</th>
              <th>Email</th>
              <th>Specializare</th>
              <th>Roluri</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {filteredUsers.map((u) => (
              <tr key={u.id}>
                <td>{u.full_name}</td>
                <td className="mono">{u.email}</td>
                <td>{u.specialization || '—'}</td>
                <td>
                  <div className="role-chips">
                    {roles.map((role) => {
                      const active = u.roleIds.has(role.id);
                      const isPrimary = u.primaryRoleId === role.id;
                      const busy = busyKey === `${u.id}:${role.id}`;
                      return (
                        <button
                          key={role.id}
                          type="button"
                          className={`role-chip ${active ? 'active' : ''}`}
                          onClick={() => toggleRole(u, role)}
                          disabled={busy}
                          title={isPrimary ? 'Rol principal' : ''}
                        >
                          {active && (
                            <Icon name={isPrimary ? 'star' : 'check'} />
                          )}
                          {role.label}
                        </button>
                      );
                    })}
                  </div>
                </td>
                <td>
                  <div className="row-actions">
                    <button
                      type="button"
                      className="icon-btn"
                      onClick={() => setEditUser(u)}
                      aria-label="Editează"
                    >
                      <Icon name="edit" />
                    </button>
                  </div>
                </td>
              </tr>
            ))}
            {filteredUsers.length === 0 && (
              <tr>
                <td colSpan={5} className="muted center">
                  Niciun utilizator pentru filtrele selectate.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
      <Toast
        visible={!!toast}
        variant={toast?.variant || 'success'}
        message={toast?.message || ''}
      />

      <CreateUserModal
        open={createOpen}
        onClose={() => setCreateOpen(false)}
        onCreated={(u) => {
          flashToast('success', `Cont creat pentru ${u.full_name}.`);
          load();
        }}
      />

      <EditUserModal
        open={!!editUser}
        user={editUser}
        onClose={() => setEditUser(null)}
        onSaved={() => {
          flashToast('success', 'Utilizator actualizat.');
          load();
        }}
      />
    </section>
  );
}
