import { useEffect, useMemo, useState } from 'react';
import { api } from '../api';
import Icon from './Icon';

// Cascading building -> room selector.
// value = room_id (uuid|null); onChange(room_id).
// Shared cache so multiple instances don't refetch.
let _cache = null;
async function loadRooms() {
  if (_cache) return _cache;
  const [buildings, rooms] = await Promise.all([
    api.get('/api/buildings'),
    api.get('/api/rooms'),
  ]);
  _cache = { buildings: buildings || [], rooms: rooms || [] };
  return _cache;
}

export default function RoomPicker({ value, onChange, required = false }) {
  const [buildings, setBuildings] = useState([]);
  const [rooms, setRooms] = useState([]);
  const [buildingId, setBuildingId] = useState('');

  useEffect(() => {
    let active = true;
    loadRooms().then((data) => {
      if (!active) return;
      setBuildings(data.buildings);
      setRooms(data.rooms);
    });
    return () => {
      active = false;
    };
  }, []);

  // Derive the building from the currently-selected room
  useEffect(() => {
    if (value && rooms.length) {
      const r = rooms.find((x) => x.id === value);
      if (r) setBuildingId(r.building_id);
    } else if (!value) {
      // keep building selection so user can pick another room in it
    }
  }, [value, rooms]);

  const roomsInBuilding = useMemo(
    () => rooms.filter((r) => r.building_id === buildingId),
    [rooms, buildingId]
  );

  const roomLabel = (r) => (r.note ? `${r.code} (${r.note})` : r.code);

  return (
    <div className="room-picker">
      <label className="field">
        <span className="field-label">Clădire</span>
        <div className="input-wrap">
          <Icon name="apartment" className="input-icon" />
          <select
            className="select-bare"
            value={buildingId}
            onChange={(e) => {
              setBuildingId(e.target.value);
              onChange(''); // reset room when building changes
            }}
          >
            <option value="">Alege clădirea…</option>
            {buildings.map((b) => (
              <option key={b.id} value={b.id}>
                {b.name}
              </option>
            ))}
          </select>
        </div>
      </label>

      <label className="field">
        <span className="field-label">Sala</span>
        <div className="input-wrap">
          <Icon name="meeting_room" className="input-icon" />
          <select
            className="select-bare"
            value={value || ''}
            onChange={(e) => onChange(e.target.value || '')}
            disabled={!buildingId}
            required={required}
          >
            <option value="">{buildingId ? 'Alege sala…' : 'Întâi clădirea'}</option>
            {roomsInBuilding.map((r) => (
              <option key={r.id} value={r.id}>
                {roomLabel(r)}
                {r.location ? ` · ${r.location}` : ''}
              </option>
            ))}
          </select>
        </div>
      </label>
    </div>
  );
}
