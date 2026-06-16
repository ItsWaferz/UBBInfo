// Simple toast. variant: 'success' | 'error'
import Icon from './Icon';

export default function Toast({ message, variant = 'success', visible }) {
  if (!visible) return null;
  const icon = variant === 'success' ? 'check_circle' : 'error';
  return (
    <div className={`toast toast-${variant}`} role="status">
      <Icon name={icon} />
      <span>{message}</span>
    </div>
  );
}
