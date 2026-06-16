import { useEffect, useRef, useState } from 'react';
import Icon, { LINK_ICON_OPTIONS } from './Icon';

/**
 * Visual icon picker dropdown for admin links.
 * @param {{ value: string, onChange: (name: string) => void }} props
 */
export default function IconPicker({ value, onChange }) {
  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState('');
  const ref = useRef(null);

  useEffect(() => {
    function handleClick(e) {
      if (ref.current && !ref.current.contains(e.target)) setOpen(false);
    }
    document.addEventListener('mousedown', handleClick);
    return () => document.removeEventListener('mousedown', handleClick);
  }, []);

  const filtered = search.trim()
    ? LINK_ICON_OPTIONS.filter(
        (o) =>
          o.name.includes(search.toLowerCase()) ||
          o.label.toLowerCase().includes(search.toLowerCase())
      )
    : LINK_ICON_OPTIONS;

  return (
    <div className="icon-picker" ref={ref}>
      <button
        type="button"
        className="icon-picker-trigger"
        onClick={() => setOpen((o) => !o)}
      >
        <Icon name={value || 'link'} size={20} />
        <span className="icon-picker-label">{value || 'link'}</span>
        <Icon name={open ? 'expand_less' : 'expand_more'} size={16} className="icon-picker-chevron" />
      </button>

      {open && (
        <div className="icon-picker-dropdown">
          <div className="icon-picker-search">
            <Icon name="search" size={16} />
            <input
              type="text"
              placeholder="Caută icon..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              autoFocus
            />
          </div>
          <div className="icon-picker-grid">
            {filtered.map((opt) => (
              <button
                key={opt.name}
                type="button"
                className={`icon-picker-item ${opt.name === value ? 'active' : ''}`}
                onClick={() => {
                  onChange(opt.name);
                  setOpen(false);
                  setSearch('');
                }}
                title={opt.label}
              >
                <Icon name={opt.name} size={20} />
                <span className="icon-picker-item-label">{opt.label}</span>
              </button>
            ))}
            {filtered.length === 0 && (
              <div className="icon-picker-empty">Niciun rezultat</div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
