import Icon from './Icon';

export default function LoadingScreen() {
  return (
    <div className="loading-screen">
      <div className="loading-content">
        <Icon name="school" size={64} className="loading-icon" />
        <div className="loading-title">UBB Info</div>
      </div>
    </div>
  );
}
