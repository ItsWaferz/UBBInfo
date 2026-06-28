import { useEffect, useMemo, useState } from 'react';
import { api } from '../../api';
import Icon from '../../components/Icon';
import Toast from '../../components/Toast';
import CreateUserModal from '../../components/CreateUserModal';
import EditUserModal from '../../components/EditUserModal';

export default function Users() {
  const [roles, setRoles] = useState([]);
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [toast, setToast] = useState(null);
  const [createOpen, setCreateOpen] = useState(false);
  const [editUser, setEditUser] = useState(null);

  // Accordion state
  const [openCategory, setOpenCategory] = useState(null); // 'student' | 'profesor' | 'administrator'
  const [openSpec, setOpenSpec] = useState(null); // specialization string

  // Table filter state
  const [groupFilter, setGroupFilter] = useState(new Set(['all']));
  const [financingFilter, setFinancingFilter] = useState(new Set(['all']));
  const [openFilterMenu, setOpenFilterMenu] = useState(null); // 'group' | 'financing' | null

  const toggleSetFilter = (currentSet, option) => {
    const next = new Set(currentSet);
    if (option === 'all') {
      return new Set(['all']);
    }
    if (next.has('all')) {
      next.delete('all');
    }
    if (next.has(option)) {
      next.delete(option);
    } else {
      next.add(option);
    }
    if (next.size === 0) {
      return new Set(['all']);
    }
    return next;
  };

  const load = async () => {
    setLoading(true);
    try {
      const [usersData, rolesData] = await Promise.all([
        api.get('/api/users'),
        api.get('/api/roles'),
      ]);

      setRoles(rolesData || []);
      setUsers(
        (usersData || [])
          .map((item) => ({
            ...item.profile,
            roleIds: new Set((item.roles || []).map((r) => r.role_id)),
            primaryRoleId: (item.roles || []).find((r) => r.is_primary)?.role_id || null,
          }))
          .sort((a, b) => (a.full_name || '').localeCompare(b.full_name || ''))
      );
    } catch (err) {
      console.error('Load users failed:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const flashToast = (variant, message) => {
    setToast({ variant, message });
    setTimeout(() => setToast(null), 3000);
  };

  // Derive role ID lookup
  const roleIdByName = useMemo(
    () => Object.fromEntries(roles.map((r) => [r.name, r.id])),
    [roles]
  );

  // Categorize users by role
  const students = useMemo(
    () => (roleIdByName.student ? users.filter((u) => u.roleIds.has(roleIdByName.student)) : []),
    [users, roleIdByName]
  );
  const professors = useMemo(
    () => (roleIdByName.profesor ? users.filter((u) => u.roleIds.has(roleIdByName.profesor)) : []),
    [users, roleIdByName]
  );
  const admins = useMemo(
    () => (roleIdByName.administrator ? users.filter((u) => u.roleIds.has(roleIdByName.administrator)) : []),
    [users, roleIdByName]
  );

  // Student specializations
  const specGroups = useMemo(() => {
    const map = {};
    for (const s of students) {
      const spec = s.specialization || 'Fără specializare';
      if (!map[spec]) map[spec] = [];
      map[spec].push(s);
    }
    return Object.entries(map).sort(([a], [b]) => a.localeCompare(b));
  }, [students]);

  const toggleCategory = (cat) => {
    setOpenCategory((prev) => (prev === cat ? null : cat));
    setOpenSpec(null);
    setGroupFilter(new Set(['all']));
    setFinancingFilter(new Set(['all']));
    setOpenFilterMenu(null);
  };
  const toggleSpec = (spec) => {
    setOpenSpec((prev) => (prev === spec ? null : spec));
    setGroupFilter(new Set(['all']));
    setFinancingFilter(new Set(['all']));
    setOpenFilterMenu(null);
  };

  if (loading) {
    return (
      <section className="card">
        <div className="card-body muted center">Se încarcă utilizatorii…</div>
      </section>
    );
  }

  const categories = [
    {
      key: 'student',
      icon: 'school',
      label: 'Studenți',
      count: students.length,
    },
    {
      key: 'profesor',
      icon: 'person',
      label: 'Profesori',
      count: professors.length,
    },
    {
      key: 'administrator',
      icon: 'admin_panel_settings',
      label: 'Administratori',
      count: admins.length,
    },
  ];

  return (
    <section className="card">
      <div className="card-header">
        <h2 className="card-title">
          <Icon name="manage_accounts" />
          Utilizatori ({users.length})
        </h2>
        <button type="button" className="btn btn-primary btn-sm" onClick={() => setCreateOpen(true)}>
          <Icon name="person_add" />
          Adaugă utilizator
        </button>
      </div>

      <div className="card-body" style={{ padding: 0, gap: 0 }}>
        {categories.map((cat) => (
          <div key={cat.key} className={`users-accordion ${openCategory === cat.key ? 'open' : ''}`}>
            {/* ── Category header ── */}
            <button
              type="button"
              className={`users-accordion-trigger${openCategory === cat.key ? ' open' : ''}`}
              onClick={() => toggleCategory(cat.key)}
            >
              <div className="users-accordion-left">
                <Icon name={cat.icon} />
                <span className="users-accordion-label">{cat.label}</span>
                <span className="users-accordion-count">{cat.count}</span>
              </div>
              <Icon name="expand_more" className={`users-accordion-chevron${openCategory === cat.key ? ' rotated' : ''}`} />
            </button>

            {/* ── Category body ── */}
            {openCategory === cat.key && (
              <div className="users-accordion-body">
                {/* ── STUDENTS: show specialization sub-menus ── */}
                {cat.key === 'student' && (
                  <>
                    {specGroups.map(([spec, specStudents]) => (
                      <div key={spec} className={`users-accordion sub ${openSpec === spec ? 'open' : ''}`}>
                        <button
                          type="button"
                          className={`users-accordion-trigger sub${openSpec === spec ? ' open' : ''}`}
                          onClick={() => toggleSpec(spec)}
                        >
                          <div className="users-accordion-left">
                            {spec === 'Fără specializare' && <Icon name="help" />}
                            <span className="users-accordion-label">{spec}</span>
                            <span className="users-accordion-count">{specStudents.length}</span>
                          </div>
                          <Icon name="expand_more" className={`users-accordion-chevron${openSpec === spec ? ' rotated' : ''}`} />
                        </button>

                        {openSpec === spec && (() => {
                          const allGroups = [...new Set(specStudents.map(u => u.group_name).filter(Boolean))].sort();
                          const allFinancing = [...new Set(specStudents.map(u => u.financing).filter(Boolean))].sort();

                          const filteredStudents = specStudents.filter(u => {
                            if (!groupFilter.has('all') && !groupFilter.has(u.group_name)) return false;
                            if (!financingFilter.has('all') && !financingFilter.has(u.financing)) return false;
                            return true;
                          });

                          return (
                            <div className="users-accordion-body">
                              <div className="table-wrap no-overflow">
                                <table className="data-table">
                                  <thead>
                                    <tr>
                                      <th>Nume</th>
                                      <th className="filter-th">
                                        <button 
                                          className={`filter-header-btn ${openFilterMenu === 'group' ? 'open' : ''}`} 
                                          onClick={() => setOpenFilterMenu(openFilterMenu === 'group' ? null : 'group')}
                                        >
                                          Grupa
                                          <Icon name="filter_list" />
                                        </button>
                                        {openFilterMenu === 'group' && (
                                          <>
                                            <div className="filter-backdrop" onClick={(e) => { e.stopPropagation(); setOpenFilterMenu(null); }} />
                                            <div className="filter-dropdown">
                                              <button className={`filter-opt ${groupFilter.has('all') ? 'active' : ''}`} onClick={() => setGroupFilter(toggleSetFilter(groupFilter, 'all', allGroups))}>Toate</button>
                                              {allGroups.map(g => (
                                                <button key={g} className={`filter-opt ${groupFilter.has(g) ? 'active' : ''}`} onClick={() => setGroupFilter(toggleSetFilter(groupFilter, g, allGroups))}>{g}</button>
                                              ))}
                                            </div>
                                          </>
                                        )}
                                      </th>
                                      <th className="filter-th">
                                        <button 
                                          className={`filter-header-btn ${openFilterMenu === 'financing' ? 'open' : ''}`} 
                                          onClick={() => setOpenFilterMenu(openFilterMenu === 'financing' ? null : 'financing')}
                                        >
                                          Finanțare
                                          <Icon name="filter_list" />
                                        </button>
                                        {openFilterMenu === 'financing' && (
                                          <>
                                            <div className="filter-backdrop" onClick={(e) => { e.stopPropagation(); setOpenFilterMenu(null); }} />
                                            <div className="filter-dropdown">
                                              <button className={`filter-opt ${financingFilter.has('all') ? 'active' : ''}`} onClick={() => setFinancingFilter(toggleSetFilter(financingFilter, 'all', allFinancing))}>Toate</button>
                                              {allFinancing.map(f => (
                                                <button key={f} className={`filter-opt ${financingFilter.has(f) ? 'active' : ''}`} onClick={() => setFinancingFilter(toggleSetFilter(financingFilter, f, allFinancing))}>{f}</button>
                                              ))}
                                            </div>
                                          </>
                                        )}
                                      </th>
                                      <th></th>
                                    </tr>
                                  </thead>
                                  <tbody>
                                    {filteredStudents.map((u) => (
                                      <tr key={u.id}>
                                        <td>{u.full_name}</td>
                                        <td>{u.group_name || '—'}</td>
                                        <td>
                                          {u.financing ? (
                                            <span className={`badge ${u.financing === 'BUGET' ? 'badge-lab' : 'badge-seminar'}`}>
                                              {u.financing}
                                            </span>
                                          ) : '—'}
                                        </td>
                                        <td>
                                          <div className="row-actions">
                                            <button type="button" className="icon-btn" onClick={() => setEditUser(u)} aria-label="Editează">
                                              <Icon name="edit" />
                                            </button>
                                          </div>
                                        </td>
                                      </tr>
                                    ))}
                                    {filteredStudents.length === 0 && (
                                      <tr>
                                        <td colSpan={4} className="muted center">Nu există rezultate pentru filtrele selectate.</td>
                                      </tr>
                                    )}
                                  </tbody>
                                </table>
                              </div>
                            </div>
                          );
                        })()}
                      </div>
                    ))}
                    {specGroups.length === 0 && (
                      <p className="muted center" style={{ padding: 16 }}>Niciun student înregistrat.</p>
                    )}
                  </>
                )}

                {/* ── PROFESSORS ── */}
                {cat.key === 'profesor' && (
                  <div className="table-wrap">
                    <table className="data-table">
                      <thead>
                        <tr>
                          <th>Nume</th>
                          <th>Email</th>
                          <th></th>
                        </tr>
                      </thead>
                      <tbody>
                        {professors.map((u) => (
                          <tr key={u.id}>
                            <td>{u.full_name}</td>
                            <td className="mono">{u.email}</td>
                            <td>
                              <div className="row-actions">
                                <button type="button" className="icon-btn" onClick={() => setEditUser(u)} aria-label="Editează">
                                  <Icon name="edit" />
                                </button>
                              </div>
                            </td>
                          </tr>
                        ))}
                        {professors.length === 0 && (
                          <tr><td colSpan={3} className="muted center">Niciun profesor înregistrat.</td></tr>
                        )}
                      </tbody>
                    </table>
                  </div>
                )}

                {/* ── ADMINISTRATORS ── */}
                {cat.key === 'administrator' && (
                  <div className="table-wrap">
                    <table className="data-table">
                      <thead>
                        <tr>
                          <th>Nume</th>
                          <th>Email</th>
                          <th></th>
                        </tr>
                      </thead>
                      <tbody>
                        {admins.map((u) => (
                          <tr key={u.id}>
                            <td>{u.full_name}</td>
                            <td className="mono">{u.email}</td>
                            <td>
                              <div className="row-actions">
                                <button type="button" className="icon-btn" onClick={() => setEditUser(u)} aria-label="Editează">
                                  <Icon name="edit" />
                                </button>
                              </div>
                            </td>
                          </tr>
                        ))}
                        {admins.length === 0 && (
                          <tr><td colSpan={3} className="muted center">Niciun administrator înregistrat.</td></tr>
                        )}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            )}
          </div>
        ))}
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
        roles={roles}
        onClose={() => setEditUser(null)}
        onSaved={() => {
          flashToast('success', 'Utilizator actualizat.');
          load();
        }}
      />
    </section>
  );
}
