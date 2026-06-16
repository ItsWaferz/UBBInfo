import { useEffect, useState } from 'react';
import { supabase } from '../../supabaseClient';
import { useLanguage } from '../../i18n/LanguageContext';
import Icon from '../../components/Icon';
import IconPicker from '../../components/IconPicker';
import Toast from '../../components/Toast';

const BLANK = { title: '', title_en: '', title_hu: '', title_de: '', url: '#', url_en: '#', url_hu: '#', url_de: '#', icon: 'link', sort_order: '', is_active: true };

export default function Links() {
  const { t } = useLanguage();
  const [links, setLinks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [form, setForm] = useState(BLANK);
  const [editingId, setEditingId] = useState(null);
  const [saving, setSaving] = useState(false);
  const [toast, setToast] = useState(null);

  const load = async () => {
    setLoading(true);
    const { data, error } = await supabase
      .from('useful_links')
      .select('*')
      .order('sort_order');
    if (error) console.error('Load links failed:', error);
    setLinks(data || []);
    setLoading(false);
  };

  useEffect(() => {
    load();
  }, []);

  const flashToast = (variant, message) => {
    setToast({ variant, message });
    setTimeout(() => setToast(null), 3000);
  };

  const resetForm = () => {
    setForm(BLANK);
    setEditingId(null);
  };

  const startEdit = (l) => {
    setEditingId(l.id);
    setForm({
      title: l.title || '',
      title_en: l.title_en || '',
      title_hu: l.title_hu || '',
      title_de: l.title_de || '',
      url: l.url || '#',
      url_en: l.url_en || '#',
      url_hu: l.url_hu || '#',
      url_de: l.url_de || '#',
      icon: l.icon || 'link',
      sort_order: l.sort_order ?? '',
      is_active: !!l.is_active,
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.title.trim()) {
      flashToast('error', t('admin.links.titleRequired'));
      return;
    }
    setSaving(true);
    const payload = {
      title: form.title.trim(),
      title_en: form.title_en.trim() || null,
      title_hu: form.title_hu.trim() || null,
      title_de: form.title_de.trim() || null,
      url: form.url.trim() || '#',
      url_en: form.url_en.trim() || '#',
      url_hu: form.url_hu.trim() || '#',
      url_de: form.url_de.trim() || '#',
      icon: form.icon.trim() || 'link',
      sort_order: form.sort_order === '' ? 0 : Number(form.sort_order),
      is_active: form.is_active,
    };

    const { error } = editingId
      ? await supabase.from('useful_links').update(payload).eq('id', editingId)
      : await supabase.from('useful_links').insert(payload);

    setSaving(false);
    if (error) {
      console.error('Save link failed:', error);
      flashToast('error', t('admin.links.saveError'));
      return;
    }
    flashToast('success', editingId ? t('admin.links.updated') : t('admin.links.added'));
    resetForm();
    load();
  };

  const toggleActive = async (l) => {
    const { error } = await supabase
      .from('useful_links')
      .update({ is_active: !l.is_active })
      .eq('id', l.id);
    if (error) {
      console.error('Toggle link failed:', error);
      flashToast('error', t('admin.links.toggleError'));
      return;
    }
    setLinks((prev) =>
      prev.map((x) => (x.id === l.id ? { ...x, is_active: !x.is_active } : x))
    );
  };

  const handleDelete = async (l) => {
    if (!window.confirm(t('admin.links.deleteConfirm', { title: l.title }))) return;
    const { error } = await supabase.from('useful_links').delete().eq('id', l.id);
    if (error) {
      console.error('Delete link failed:', error);
      flashToast('error', t('admin.links.deleteError'));
      return;
    }
    flashToast('success', t('admin.links.deleted'));
    if (editingId === l.id) resetForm();
    load();
  };

  return (
    <div className="admin-section-grid">
      {/* Form */}
      <section className="card">
        <div className="card-header">
          <h2 className="card-title">
            <Icon name={editingId ? 'edit' : 'add_link'} />
            {editingId ? t('admin.links.editTitle') : t('admin.links.addTitle')}
          </h2>
        </div>
        <form className="card-body" onSubmit={handleSubmit}>
          <label className="field">
            <span className="field-label">🇷🇴 {t('admin.links.title')}</span>
            <div className="input-wrap">
              <input
                type="text"
                value={form.title}
                onChange={(e) => setForm({ ...form, title: e.target.value })}
                placeholder="ex. Bibliotecă online"
              />
            </div>
          </label>
          <label className="field">
            <span className="field-label">🇬🇧 {t('admin.links.titleEn')}</span>
            <div className="input-wrap">
              <input
                type="text"
                value={form.title_en}
                onChange={(e) => setForm({ ...form, title_en: e.target.value })}
                placeholder="ex. Online Library"
              />
            </div>
          </label>
          <label className="field">
            <span className="field-label">🇭🇺 {t('admin.links.titleHu')}</span>
            <div className="input-wrap">
              <input
                type="text"
                value={form.title_hu}
                onChange={(e) => setForm({ ...form, title_hu: e.target.value })}
                placeholder="ex. Online könyvtár"
              />
            </div>
          </label>
          <label className="field">
            <span className="field-label">🇩🇪 {t('admin.links.titleDe')}</span>
            <div className="input-wrap">
              <input
                type="text"
                value={form.title_de}
                onChange={(e) => setForm({ ...form, title_de: e.target.value })}
                placeholder="ex. Online-Bibliothek"
              />
            </div>
          </label>
          <label className="field">
            <span className="field-label">🇷🇴 {t('admin.links.url')}</span>
            <div className="input-wrap">
              <input
                type="text"
                value={form.url}
                onChange={(e) => setForm({ ...form, url: e.target.value })}
                placeholder="https://…"
              />
            </div>
          </label>
          <label className="field">
            <span className="field-label">🇬🇧 {t('admin.links.urlEn')}</span>
            <div className="input-wrap">
              <input
                type="text"
                value={form.url_en}
                onChange={(e) => setForm({ ...form, url_en: e.target.value })}
                placeholder="https://…"
              />
            </div>
          </label>
          <label className="field">
            <span className="field-label">🇭🇺 {t('admin.links.urlHu')}</span>
            <div className="input-wrap">
              <input
                type="text"
                value={form.url_hu}
                onChange={(e) => setForm({ ...form, url_hu: e.target.value })}
                placeholder="https://…"
              />
            </div>
          </label>
          <label className="field">
            <span className="field-label">🇩🇪 {t('admin.links.urlDe')}</span>
            <div className="input-wrap">
              <input
                type="text"
                value={form.url_de}
                onChange={(e) => setForm({ ...form, url_de: e.target.value })}
                placeholder="https://…"
              />
            </div>
          </label>
          <div className="field">
            <span className="field-label">{t('admin.links.icon')}</span>
            <IconPicker value={form.icon} onChange={(name) => setForm({ ...form, icon: name })} />
          </div>
          <label className="field">
            <span className="field-label">{t('admin.links.order')}</span>
            <div className="input-wrap">
              <input
                type="number"
                value={form.sort_order}
                onChange={(e) => setForm({ ...form, sort_order: e.target.value })}
                placeholder="ex. 1"
              />
            </div>
          </label>
          <label className="checkbox-field">
            <input
              type="checkbox"
              checked={form.is_active}
              onChange={(e) => setForm({ ...form, is_active: e.target.checked })}
            />
            <span>{t('admin.links.active')}</span>
          </label>
          <div className="form-actions">
            {editingId && (
              <button type="button" className="btn btn-ghost" onClick={resetForm}>
                {t('admin.links.cancel')}
              </button>
            )}
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? <span className="spinner" /> : editingId ? t('admin.links.save') : t('admin.links.add')}
            </button>
          </div>
        </form>
      </section>

      {/* List */}
      <section className="card">
        <div className="card-header">
          <h2 className="card-title">
            <Icon name="link" />
            {t('admin.links.listTitle')} ({links.length})
          </h2>
        </div>
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>{t('admin.links.order')}</th>
                <th>{t('admin.links.title')}</th>
                <th>URL</th>
                <th>{t('admin.links.activeLabel')}</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={5} className="muted center">
                    {t('admin.links.loading')}
                  </td>
                </tr>
              ) : (
                links.map((l) => (
                  <tr key={l.id}>
                    <td>{l.sort_order}</td>
                    <td>
                      <span className="link-title-cell">
                        <Icon name={l.icon} />
                        {l.title}
                      </span>
                    </td>
                    <td className="mono link-url-cell">{l.url}</td>
                    <td>
                      <button
                        type="button"
                        className={`toggle-pill ${l.is_active ? 'on' : 'off'}`}
                        onClick={() => toggleActive(l)}
                      >
                        {l.is_active ? t('admin.links.activeLabel') : t('admin.links.inactiveLabel')}
                      </button>
                    </td>
                    <td>
                      <div className="row-actions">
                        <button
                          type="button"
                          className="icon-btn"
                          onClick={() => startEdit(l)}
                          aria-label={t('common.edit')}
                        >
                          <Icon name="edit" />
                        </button>
                        <button
                          type="button"
                          className="icon-btn icon-btn-danger"
                          onClick={() => handleDelete(l)}
                          aria-label={t('common.delete')}
                        >
                          <Icon name="delete" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </section>

      <Toast
        visible={!!toast}
        variant={toast?.variant || 'success'}
        message={toast?.message || ''}
      />
    </div>
  );
}
