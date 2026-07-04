import { useCallback, useEffect, useRef, useState } from 'react';

/**
 * Shared toast state for pages using the <Toast/> component — replaces the
 * per-page copy-pasted `flash()` helpers (which had already drifted on timeout).
 *
 *   const { toast, flash } = useToast();
 *   flash('success', 'Salvat.');
 *   <Toast visible={!!toast} variant={toast?.variant || 'success'} message={toast?.message || ''} />
 */
export function useToast(duration = 3000) {
  const [toast, setToast] = useState(null);
  const timer = useRef(null);

  const flash = useCallback((variant, message) => {
    setToast({ variant, message });
    clearTimeout(timer.current);
    timer.current = setTimeout(() => setToast(null), duration);
  }, [duration]);

  useEffect(() => () => clearTimeout(timer.current), []);

  return { toast, flash };
}
