import React from 'react';

// Class component: React error boundaries can't be written as hooks.
export default class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError() {
    return { hasError: true };
  }

  componentDidCatch(error, info) {
    console.error('Unhandled UI error:', error?.message ?? error, info?.componentStack);
  }

  handleReload = () => {
    this.setState({ hasError: false });
    window.location.reload();
  };

  render() {
    if (!this.state.hasError) return this.props.children;
    return (
      <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 24 }}>
        <div className="card" style={{ maxWidth: 420, textAlign: 'center' }}>
          <div className="card-body">
            <h2 style={{ marginBottom: 8 }}>A apărut o eroare</h2>
            <p className="muted" style={{ marginBottom: 16 }}>
              Ceva nu a funcționat corect. Reîncarcă pagina — dacă problema persistă, contactează administratorul.
            </p>
            <button type="button" className="btn btn-primary" onClick={this.handleReload}>
              Reîncarcă pagina
            </button>
          </div>
        </div>
      </div>
    );
  }
}
