import React from 'react';
import ReactDOM from 'react-dom/client';
import { HashRouter } from 'react-router-dom';
import App from './App.jsx';
import { LanguageProvider } from './i18n/LanguageContext.jsx';
import { AuthProvider } from './contexts/AuthContext.jsx';
import './styles/index.css';

// HashRouter (URLs like /UBBInfo/#/note) so refreshing a deep link works on
// GitHub Pages, which has no server-side route rewriting.
ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <HashRouter>
      <LanguageProvider>
        <AuthProvider>
          <App />
        </AuthProvider>
      </LanguageProvider>
    </HashRouter>
  </React.StrictMode>
);
