// Admin: manage buildings & rooms (CRUD) + proximity zones + room capacity/type.
import { useEffect, useState } from 'react';
import { api } from '../../api';
import Icon from '../../components/Icon';
import Toast from '../../components/Toast';

const ROOM_TYPES = ['', 'CURS', 'SEMINAR', 'LABORATOR', 'ORICE'];
const BLANK_BUILDING = { code: '', name: '', address: '', zone: '', sort_order: '' };
const BLANK_ROOM = { code: '', note: '', location: '', capacity: '', room_type: '' };

export default function BuildingsRooms() {
  const [buildings, setBuildings] = useState([]);
  const [rooms, setRooms] = useState([]);
  const [open, setOpen] = useState(null);
  const [newBuilding, setNewBuilding] = useState(BLANK_BUILDING);
  const [showAddBuilding, setShowAddBuilding] = useState(false);
  const [newRoom, setNewRoom] = useState(BLANK_ROOM);
  const [toast, setToast] = useState(null);

  const flash = (variant, message) => {
    setToast({ variant, message });
    setTimeout(() => setToast(null), 3000);
  };

  const loadAll = async () => {
    try {
      const [b, r] = await Promise.all([api.get('/api/buildings'), api.get('/api/rooms')]);
      setBuildings(b || []);
      setRooms(r || []);
    } catch (err) {
      console.error(err);
      flash('error', 'Eroare la încărcare.');
    }
  };
  useEffect(() => { loadAll(); }, []);

  // ----- buildings -----
  const setBuildingField = (id, field, value) =>
    setBuildings((prev) => prev.map((b) => (b.id === id ? { ...b, [field]: value } : b)));

  const addBuilding = async (e) => {
    e.preventDefault();
    if (!newBuilding.code.trim() || !newBuilding.name.trim()) {
      flash('error', 'Codul și numele clădirii sunt obligatorii.');
      return;
    }
    try {
      await api.post('/api/buildings', {
        code: newBuilding.code.trim(),
        name: newBuilding.name.trim(),
        address: newBuilding.address.trim() || null,
        zone: newBuilding.zone.trim() || null,
        sort_order: newBuilding.sort_order === '' ? null : Number(newBuilding.sort_order),
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
        code: b.code, name: b.name, address: b.address || null,
        zone: b.zone || null, sort_order: b.sort_order ?? null,
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
        code: r.code, note: r.note || null, location: r.location || null,
        capacity: r.capacity === '' || r.capacity == null ? null : Number(r.capacity),
        room_type: r.room_type || null,
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
        note: newRoom.note.trim() || null,
        location: newRoom.location.trim() || null,
        capacity: newRoom.capacity === '' ? null : Number(newRoom.capacity),
        room_type: newRoom.room_type || null,
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
    <section className="card">
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

        {showAddBuilding && (
          <form className="card" style={{ padding: 16, marginBottom: 16 }} onSubmit={addBuilding}>
            <div className="form-grid-2">
              <label className="field"><span className="field-label">Cod *</span>
                <div className="input-wrap"><input value={newBuilding.code}
                  onChange={(e) => setNewBuilding({ ...newBuilding, code: e.target.value })} placeholder="ex. FSEGA" /></div></label>
              <label className="field"><span className="field-label">Nume *</span>
                <div className="input-wrap"><input value={newBuilding.name}
                  onChange={(e) => setNewBuilding({ ...newBuilding, name: e.target.value })} placeholder="ex. FSEGA" /></div></label>
            </div>
            <div className="form-grid-2">
              <label className="field"><span className="field-label">Adresă</span>
                <div className="input-wrap"><input value={newBuilding.address}
                  onChange={(e) => setNewBuilding({ ...newBuilding, address: e.target.value })} /></div></label>
              <label className="field"><span className="field-label">Zonă</span>
                <div className="input-wrap"><input value={newBuilding.zone}
                  onChange={(e) => setNewBuilding({ ...newBuilding, zone: e.target.value })} placeholder="ex. Centru" /></div></label>
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
              <div className="users-accordion-trigger" style={{ gap: 10, flexWrap: 'wrap' }}>
                <button type="button"
                  style={{ background: 'none', border: 'none', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 8 }}
                  onClick={() => setOpen(isOpen ? null : b.id)}>
                  <Icon name="apartment" />
                  <span className="users-accordion-label">{b.name}</span>
                  <span className="users-accordion-count">{buildingRooms.length} săli</span>
                </button>
                <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginLeft: 'auto' }}>
                  <Icon name="hub" style={{ opacity: 0.6 }} title="zonă" />
                  <input placeholder="zonă" value={b.zone || ''} style={{ width: 100 }}
                    onClick={(e) => e.stopPropagation()}
                    onChange={(e) => setBuildingField(b.id, 'zone', e.target.value)} />
                  <button type="button" className="btn btn-outline btn-sm"
                    onClick={(e) => { e.stopPropagation(); saveBuilding(b); }}>Salvează</button>
                  <button type="button" className="icon-btn" aria-label="Șterge clădirea"
                    onClick={(e) => { e.stopPropagation(); deleteBuilding(b); }}><Icon name="delete" /></button>
                </div>
              </div>

              {isOpen && (
                <div className="users-accordion-body">
                  {/* edit building basics */}
                  <div className="form-grid-2" style={{ marginBottom: 12 }}>
                    <label className="field"><span className="field-label">Cod</span>
                      <div className="input-wrap"><input value={b.code || ''}
                        onChange={(e) => setBuildingField(b.id, 'code', e.target.value)} /></div></label>
                    <label className="field"><span className="field-label">Nume</span>
                      <div className="input-wrap"><input value={b.name || ''}
                        onChange={(e) => setBuildingField(b.id, 'name', e.target.value)} /></div></label>
                  </div>

                  <div className="table-wrap">
                    <table className="data-table">
                      <thead>
                        <tr><th>Sală</th><th>Notă</th><th>Locație</th><th>Capacitate</th><th>Tip</th><th></th></tr>
                      </thead>
                      <tbody>
                        {buildingRooms.map((r) => (
                          <tr key={r.id}>
                            <td><input value={r.code || ''} style={{ width: 90 }}
                              onChange={(e) => setRoomField(r.id, 'code', e.target.value)} /></td>
                            <td><input value={r.note || ''} style={{ width: 120 }}
                              onChange={(e) => setRoomField(r.id, 'note', e.target.value)} /></td>
                            <td><input value={r.location || ''} style={{ width: 100 }}
                              onChange={(e) => setRoomField(r.id, 'location', e.target.value)} /></td>
                            <td><input type="number" min="0" className="grade-input" style={{ width: 70 }}
                              value={r.capacity ?? ''}
                              onChange={(e) => setRoomField(r.id, 'capacity', e.target.value)} /></td>
                            <td>
                              <select className="select-bare" value={r.room_type || ''}
                                onChange={(e) => setRoomField(r.id, 'room_type', e.target.value)}>
                                {ROOM_TYPES.map((t) => <option key={t} value={t}>{t || '—'}</option>)}
                              </select>
                            </td>
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
                          <td><input value={newRoom.note} placeholder="notă" style={{ width: 120 }}
                            onChange={(e) => setNewRoom({ ...newRoom, note: e.target.value })} /></td>
                          <td><input value={newRoom.location} placeholder="locație" style={{ width: 100 }}
                            onChange={(e) => setNewRoom({ ...newRoom, location: e.target.value })} /></td>
                          <td><input type="number" min="0" className="grade-input" style={{ width: 70 }}
                            value={newRoom.capacity} placeholder="loc."
                            onChange={(e) => setNewRoom({ ...newRoom, capacity: e.target.value })} /></td>
                          <td>
                            <select className="select-bare" value={newRoom.room_type}
                              onChange={(e) => setNewRoom({ ...newRoom, room_type: e.target.value })}>
                              {ROOM_TYPES.map((t) => <option key={t} value={t}>{t || '—'}</option>)}
                            </select>
                          </td>
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
