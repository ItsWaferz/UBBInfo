import { useEffect, useMemo, useState } from 'react';
import { Navigate } from 'react-router-dom';
import { api } from '../../api';
import { useAuth } from '../../contexts/AuthContext';
import Icon from '../../components/Icon';
import Toast from '../../components/Toast';

export default function Catalog() {
  const { user, currentRole } = useAuth();
  const [courses, setCourses] = useState([]);
  const [selectedCourseId, setSelectedCourseId] = useState('');
  const [rows, setRows] = useState([]); // { enrollmentId, studentName, group, year, semester, isRestanta, grade, draft }
  const [loadingRows, setLoadingRows] = useState(false);
  const [saving, setSaving] = useState(false);
  const [toast, setToast] = useState(null); // { variant, message }

  // Load the professor's courses
  useEffect(() => {
    if (!user) return;
    let active = true;
    (async () => {
      let data;
      try {
        data = await api.get('/api/professor-courses/mine');
      } catch (err) {
        if (active) console.error('Load professor_courses failed:', err);
        return;
      }
      if (!active) return;
      // Group by course_id and merge types (e.g. CURS + LAB)
      const grouped = [];
      const seen = new Map();
      for (const c of data || []) {
        if (seen.has(c.course_id)) {
          const existing = seen.get(c.course_id);
          existing.type = existing.type + ' + ' + c.type;
        } else {
          const entry = { ...c, type: c.type };
          seen.set(c.course_id, entry);
          grouped.push(entry);
        }
      }
      setCourses(grouped);
      if (grouped.length) setSelectedCourseId(grouped[0].course_id);
    })();
    return () => {
      active = false;
    };
  }, [user]);

  // Load enrollments + student profiles for the selected course
  useEffect(() => {
    if (!selectedCourseId) {
      setRows([]);
      return;
    }
    let active = true;
    setLoadingRows(true);
    (async () => {
      let catalog;
      try {
        catalog = await api.get(
          `/api/professor/catalog?courseId=${encodeURIComponent(selectedCourseId)}`
        );
      } catch (err) {
        console.error('Load enrollments failed:', err);
        if (active) {
          setRows([]);
          setLoadingRows(false);
        }
        return;
      }

      if (!active) return;
      setRows(
        (catalog || []).map((e) => ({
          enrollmentId: e.id,
          studentName: e.student_name || '(necunoscut)',
          studentId: '',
          group: e.group_name,
          year: e.academic_year,
          semester: e.semester,
          isRestanta: e.is_restanta,
          grade: e.grade,
          draft: e.grade === null || e.grade === undefined ? '' : String(e.grade),
        }))
      );
      setLoadingRows(false);
    })();
    return () => {
      active = false;
    };
  }, [selectedCourseId]);

  const setDraft = (enrollmentId, value) => {
    // allow empty or 1..10
    if (value !== '' && !/^([1-9]|10)$/.test(value)) return;
    setRows((prev) =>
      prev.map((r) => (r.enrollmentId === enrollmentId ? { ...r, draft: value } : r))
    );
  };

  const dirtyRows = useMemo(
    () =>
      rows.filter((r) => {
        const current = r.grade === null || r.grade === undefined ? '' : String(r.grade);
        return r.draft !== current;
      }),
    [rows]
  );

  const handleSave = async () => {
    if (!dirtyRows.length) return;
    setSaving(true);
    setToast(null);
    try {
      const results = await Promise.allSettled(
        dirtyRows.map((r) =>
          api.patch(`/api/enrollments/${r.enrollmentId}`, {
            grade: r.draft === '' ? null : Number(r.draft),
          })
        )
      );
      const failed = results.filter((res) => res.status === 'rejected');
      if (failed.length) {
        console.error('Some grade updates failed:', failed.map((f) => f.reason));
        setToast({
          variant: 'error',
          message: `Eroare la salvarea a ${failed.length} note.`,
        });
      } else {
        // Commit drafts into the canonical grade values
        setRows((prev) =>
          prev.map((r) => ({
            ...r,
            grade: r.draft === '' ? null : Number(r.draft),
          }))
        );
        setToast({
          variant: 'success',
          message: `${dirtyRows.length} note salvate cu succes!`,
        });
      }
    } catch (err) {
      console.error('Save grades failed:', err);
      setToast({ variant: 'error', message: 'Eroare la salvarea notelor.' });
    } finally {
      setSaving(false);
      setTimeout(() => setToast(null), 3000);
    }
  };

  if (currentRole && currentRole !== 'profesor') {
    return <Navigate to="/" replace />;
  }

  const selectedCourse = courses.find((c) => c.course_id === selectedCourseId);

  return (
    <div className="page">
      <section className="header-card">
        <h1 className="page-title">Catalog Note</h1>
        <p className="page-subtitle">
          Selectează o disciplină și introdu notele studenților înscriși.
        </p>
      </section>

      <section className="card">
        <div className="card-header">
          <h2 className="card-title">
            <Icon name="school" />
            Disciplina
          </h2>
          <button
            type="button"
            className="btn btn-primary btn-sm"
            onClick={handleSave}
            disabled={saving || dirtyRows.length === 0}
          >
            {saving ? (
              <span className="spinner" />
            ) : (
              <>
                <Icon name="save" />
                Salvează notele
                {dirtyRows.length > 0 ? ` (${dirtyRows.length})` : ''}
              </>
            )}
          </button>
        </div>

        <div className="card-body">
          {courses.length === 0 ? (
            <p className="muted">Nu predai nicio disciplină.</p>
          ) : (
            <div className="field" style={{ maxWidth: 420 }}>
              <span className="field-label">Alege disciplina</span>
              <div className="input-wrap">
                <Icon name="menu_book" className="input-icon" />
                <select
                  value={selectedCourseId}
                  onChange={(e) => setSelectedCourseId(e.target.value)}
                  className="select-bare"
                >
                  {courses.map((c) => (
                    <option key={c.id} value={c.course_id}>
                      {c.courses?.name} · {c.type}
                    </option>
                  ))}
                </select>
              </div>
            </div>
          )}
        </div>

        {selectedCourse && (
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Student</th>
                  <th>Nr. matricol</th>
                  <th>Grupa</th>
                  <th className="center" style={{ width: '80px' }}>Nota</th>
                </tr>
              </thead>
              <tbody>
                {loadingRows ? (
                  <tr>
                    <td colSpan={4} className="muted center">
                      Se încarcă studenții…
                    </td>
                  </tr>
                ) : rows.length === 0 ? (
                  <tr>
                    <td colSpan={4} className="muted center">
                      Niciun student înscris la această disciplină.
                    </td>
                  </tr>
                ) : (
                  rows.map((r) => (
                    <tr key={r.enrollmentId}>
                      <td>
                        {r.studentName}
                        {r.isRestanta && <span className="restanta-tag"> (restanță)</span>}
                      </td>
                      <td className="mono">{r.studentId || '—'}</td>
                      <td>{r.group || '—'}</td>
                      <td className="center">
                        <input
                          type="text"
                          inputMode="numeric"
                          className="grade-input"
                          value={r.draft}
                          placeholder="—"
                          onChange={(e) => setDraft(r.enrollmentId, e.target.value.trim())}
                        />
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <Toast
        visible={!!toast}
        variant={toast?.variant || 'success'}
        message={toast?.message || ''}
      />
    </div>
  );
}
