// Admin: manage buildings (name + zone) & their rooms, plus a standalone zone catalog.
import { useEffect, useMemo, useState } from 'react';
import { api } from '../../api';
import Icon from '../../components/Icon';
import Toast from '../../components/Toast';

const BLANK_BUILDING = { name: '', zone: '' };
const BLANK_ROOM = { code: '', location: '', capacity: '' };

export default function BuildingsRooms() {
  const [buildings, setBuildings] = useState([]);
  const [rooms, setRooms] = useState([]);
  const [zones, setZones] = useState([]);
  const [open, setOpen] = useState(null);
  const [newBuilding, setNewBuilding] = useState(BLANK_BUILDING);
  const [showAddBuilding, setShowAddBuilding] = useState(false);
  const [newRoom, setNewRoom] = useState(BLANK_ROOM);
  const [newZone, setNewZone] = useState('');
  const [toast, setToast] = useState(null);

  const flash = (variant, message) => {
    setToast({ variant, message });
    setTimeout(() => setToast(null), 3000);
  };

  const loadAll = async () => {
    try {
      const [b, r, z] = await Promise.all([
        api.get('/api/buildings'),
        api.get('/api/rooms'),
        api.get('/api/zones').catch(() => []),
      ]);
      setBuildings(b || []);
      setRooms(r || []);
      setZones(z || []);
    } catch (err) {
      console.error(err);
      flash('error', 'Eroare la încărcare.');
    }
  };
  useEffect(() => { loadAll(); }, []);

  const zoneNames = useMemo(() => zones.map((z) => z.name).filter(Boolean), [zones]);
  // Zone <select> options — the catalog plus the current value, so an unlisted
  // (legacy) zone never silently drops out.
  const zoneOptions = (current) => {
    const set = new Set(zoneNames);
    if (current) set.add(current);
    return Array.from(set).sort((a, b) => a.localeCompare(b));
  };

  // ----- zones -----
  const addZone = async (e) => {
    e.preventDefault();
    const name = newZone.trim();
    if (!name) return;
    try {
      await api.post('/api/zones', { name });
      setNewZone('');
      flash('success', 'Zonă adăugată.');
      loadAll();
    } catch (err) {
      console.error(err);
      flash('error', 'Eroare la adăugarea zonei.');
    }
  };

  const deleteZone = async (z) => {
    if (!window.confirm(`Ștergi zona „${z.name}"?`)) return;
    try {
      await api.del(`/api/zones/${z.id}`);
      flash('success', 'Zonă ștearsă.');
      loadAll();
    } catch (err) {
      console.error(err);
      flash('error', 'Eroare la ștergerea zonei.');
    }
  };

  // ----- buildings -----
  const setBuildingField = (id, field, value) =>
    setBuildings((prev) => prev.map((b) => (b.id === id ? { ...b, [field]: value } : b)));

  const addBuilding = async (e) => {
    e.preventDefault();
    if (!newBuilding.name.trim()) {
      flash('error', 'Numele clădirii este obligatoriu.');
      return;
    }
    try {
      await api.post('/api/buildings', {
        code: newBuilding.name.trim(), // code kept in DB (unique) but not user-facing
        name: newBuilding.name.trim(),
        zone: newBuilding.zone.trim() || null,
      });
      setNewBuilding(BLANK_BUILDING);
      setShowAddBuilding(false);
      flash('success', 'Clădire adăugată.');
      loadAll();
    } catch (err) {
      console.error(err);
      flash('error', 'Eroare la adăugare.');
    }
  };

  const saveBuilding = async (b) => {
    try {
      await api.put(`/api/buildings/${b.id}`, {
        name: b.name,
        zone: b.zone || null,
      });
      flash('success', `Clădirea ${b.name} salvată.`);
    } catch (err) {
      console.error(err);
      flash('error', 'Eroare la salvare.');
    }
  };

  const deleteBuilding = async (b) => {
    if (!window.confirm(`Ștergi clădirea „${b.name}" și toate sălile ei?`)) return;
    try {
      await api.del(`/api/buildings/${b.id}`);
      flash('success', 'Clădire ștearsă.');
      loadAll();
    } catch (err) {
      console.error(err);
      flash('error', 'Eroare la ștergere.');
    }
  };

  // ----- rooms -----
  const setRoomField = (id, field, value) =>
    setRooms((prev) => prev.map((r) => (r.id === id ? { ...r, [field]: value } : r)));

  const saveRoom = async (r) => {
    try {
      await api.put(`/api/rooms/${r.id}`, {
        code: r.code,
        location: r.location || null,
        capacity: r.capacity === '' || r.capacity == null ? null : Number(r.capacity),
      });
      flash('success', `Sala ${r.code} salvată.`);
    } catch (err) {
      console.error(err);
      flash('error', 'Eroare la salvarea sălii.');
    }
  };

  const deleteRoom = async (r) => {
    if (!window.confirm(`Ștergi sala ${r.code}?`)) return;
    try {
      await api.del(`/api/rooms/${r.id}`);
      flash('success', 'Sală ștearsă.');
      loadAll();
    } catch (err) {
      console.error(err);
      flash('error', 'Eroare la ștergere.');
    }
  };

  const addRoom = async (buildingId) => {
    if (!newRoom.code.trim()) {
      flash('error', 'Codul sălii este obligatoriu.');
      return;
    }
    try {
      await api.post('/api/rooms', {
        building_id: buildingId,
        code: newRoom.code.trim(),
        location: newRoom.location.trim() || null,
        capacity: newRoom.capacity === '' ? null : Number(newRoom.capacity),
      });
      setNewRoom(BLANK_ROOM);
      flash('success', 'Sală adăugată.');
      loadAll();
    } catch (err) {
      console.error(err);
      flash('error', 'Eroare la adăugare.');
    }
  };

  return (
    <section className="card buildings-page">
      <div className="card-header">
        <h2 className="card-title"><Icon name="apartment" /> Clădiri & săli ({buildings.length})</h2>
        <button type="button" className="btn btn-primary btn-sm" onClick={() => setShowAddBuilding((v) => !v)}>
          <Icon name="add" /> Adaugă clădire
        </button>
      </div>

      <div className="card-body">
        <p className="muted" style={{ fontSize: 13, marginBottom: 12 }}>
          <strong>Zona</strong> de proximitate: clădirile cu aceeași zonă sunt „aproape" (fără pauză de
          deplasare la generarea orarului); în zone diferite generatorul lasă minim 2h între ore consecutive.
        </p>

        {/* Zone catalog — add / remove zones independently of buildings. */}
        <div className="zone-manager">
          <div className="zone-manager-title"><Icon name="hub" /> Zone <span className="zone-manager-count">{zones.length}</span></div>
          <div className="zone-chips">
            {zoneNames.length === 0 && <span className="muted" style={{ fontSize: 13 }}>Nicio zonă încă — adaugă prima mai jos.</span>}
            {zones.map((z) => (
              <span key={z.id} className="zone-chip">
                {z.name}
                <button type="button" aria-label={`Șterge zona ${z.name}`} onClick={() => deleteZone(z)}>
                  <Icon name="close" />
                </button>
              </span>
            ))}
          </div>
          <form className="zone-add" onSubmit={addZone}>
            <input value={newZone} placeholder="zonă nouă (ex. Centru)"
              onChange={(e) => setNewZone(e.target.value)} />
            <button type="submit" className="btn btn-outline btn-sm"><Icon name="add" /> Adaugă zonă</button>
          </form>
        </div>

        {showAddBuilding && (
          <form className="card" style={{ padding: 16, marginBottom: 16 }} onSubmit={addBuilding}>
            <div className="form-grid-2">
              <label className="field"><span className="field-label">Nume *</span>
                <div className="input-wrap"><input value={newBuilding.name}
                  onChange={(e) => setNewBuilding({ ...newBuilding, name: e.target.value })} placeholder="ex. FSEGA" /></div></label>
              <label className="field"><span className="field-label">Zonă</span>
                <div className="input-wrap">
                  <select className="select-bare" value={newBuilding.zone}
                    onChange={(e) => setNewBuilding({ ...newBuilding, zone: e.target.value })}>
                    <option value="">— alege —</option>
                    {zoneOptions(newBuilding.zone).map((z) => <option key={z} value={z}>{z}</option>)}
                  </select>
                </div></label>
            </div>
            <div className="form-actions">
              <button type="button" className="btn btn-ghost" onClick={() => setShowAddBuilding(false)}>Anulează</button>
              <button type="submit" className="btn btn-primary">Adaugă</button>
            </div>
          </form>
        )}

        {buildings.map((b) => {
          const buildingRooms = rooms.filter((r) => r.building_id === b.id);
          const isOpen = open === b.id;
          return (
            <div key={b.id} className={`users-accordion ${isOpen ? 'open' : ''}`} style={{ marginBottom: 8 }}>
              <div className="users-accordion-trigger" style={{ cursor: 'pointer' }}
                onClick={() => setOpen(isOpen ? null : b.id)}>
                <div className="users-accordion-left">
                  <Icon name="apartment" />
                  <span className="users-accordion-label">{b.name}</span>
                  <span className="users-accordion-count">{buildingRooms.length} săli</span>
                  {b.zone && <span className="zone-badge"><Icon name="hub" /> {b.zone}</span>}
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                  <button type="button" className="icon-btn" aria-label="Șterge clădirea"
                    onClick={(e) => { e.stopPropagation(); deleteBuilding(b); }}><Icon name="delete" /></button>
                  <Icon name="expand_more" className={`users-accordion-chevron${isOpen ? ' rotated' : ''}`} />
                </div>
              </div>

              {isOpen && (
                <div className="users-accordion-body">
                  {/* edit building basics: name + zone */}
                  <div className="form-grid-2" style={{ marginBottom: 12 }}>
                    <label className="field"><span className="field-label">Nume</span>
                      <div className="input-wrap"><input value={b.name || ''}
                        onChange={(e) => setBuildingField(b.id, 'name', e.target.value)} /></div></label>
                    <label className="field"><span className="field-label">Zonă</span>
                      <div className="input-wrap">
                        <select className="select-bare" value={b.zone || ''}
                          onChange={(e) => setBuildingField(b.id, 'zone', e.target.value)}>
                          <option value="">— alege —</option>
                          {zoneOptions(b.zone).map((z) => <option key={z} value={z}>{z}</option>)}
                        </select>
                      </div></label>
                  </div>
                  <div className="form-actions" style={{ marginBottom: 12 }}>
                    <button type="button" className="btn btn-outline btn-sm" onClick={() => saveBuilding(b)}>Salvează clădirea</button>
                  </div>

                  <div className="table-wrap">
                    <table className="data-table">
                      <thead>
                        <tr><th>Sală</th><th>Locație</th><th>Capacitate</th><th></th></tr>
                      </thead>
                      <tbody>
                        {buildingRooms.map((r) => (
                          <tr key={r.id}>
                            <td><input value={r.code || ''} style={{ width: 90 }}
                              onChange={(e) => setRoomField(r.id, 'code', e.target.value)} /></td>
                            <td><input value={r.location || ''} style={{ width: 120 }}
                              onChange={(e) => setRoomField(r.id, 'location', e.target.value)} /></td>
                            <td><input type="number" min="0" className="grade-input" style={{ width: 70 }}
                              value={r.capacity ?? ''}
                              onChange={(e) => setRoomField(r.id, 'capacity', e.target.value)} /></td>
                            <td>
                              <div className="row-actions">
                                <button className="btn btn-outline btn-sm" onClick={() => saveRoom(r)}>Salvează</button>
                                <button className="icon-btn" aria-label="Șterge" onClick={() => deleteRoom(r)}><Icon name="delete" /></button>
                              </div>
                            </td>
                          </tr>
                        ))}
                        {/* add room row */}
                        <tr>
                          <td><input value={newRoom.code} placeholder="cod *" style={{ width: 90 }}
                            onChange={(e) => setNewRoom({ ...newRoom, code: e.target.value })} /></td>
                          <td><input value={newRoom.location} placeholder="locație" style={{ width: 120 }}
                            onChange={(e) => setNewRoom({ ...newRoom, location: e.target.value })} /></td>
                          <td><input type="number" min="0" className="grade-input" style={{ width: 70 }}
                            value={newRoom.capacity} placeholder="loc."
                            onChange={(e) => setNewRoom({ ...newRoom, capacity: e.target.value })} /></td>
                          <td>
                            <button className="btn btn-primary btn-sm" onClick={() => addRoom(b.id)}>
                              <Icon name="add" /> Adaugă
                            </button>
                          </td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                </div>
              )}
            </div>
          );
        })}
      </div>

      <Toast visible={!!toast} variant={toast?.variant || 'success'} message={toast?.message || ''} />
    </section>
  );
}
