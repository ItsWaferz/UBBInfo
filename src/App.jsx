import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './contexts/AuthContext';
import LoadingScreen from './components/LoadingScreen';
import LoginPage from './components/LoginPage';
import AppShell from './components/AppShell';
import HomeRouter from './components/HomeRouter';
import Identity from './pages/student/Identity';
import Grades from './pages/student/Grades';
import Orar from './pages/student/Orar';
import Evaluare from './pages/student/Evaluare';
import InscriereExamen from './pages/student/InscriereExamen';
import Catalog from './pages/professor/Catalog';
import Examene from './pages/professor/Examene';
import Availability from './pages/professor/Availability';
import PlataTaxe from './pages/mockups/PlataTaxe';
import Users from './pages/admin/Users';
import Courses from './pages/admin/Courses';
import OrarEditor from './pages/admin/OrarEditor';
import OrarGenerator from './pages/admin/OrarGenerator';
import Calendar from './pages/admin/Calendar';
import Evaluari from './pages/admin/Evaluari';
import Links from './pages/admin/Links';
import ConturiAdmisi from './pages/admin/ConturiAdmisi';

function RequireAuth({ children }) {
  const { isAuthenticated } = useAuth();
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  return children;
}

export default function App() {
  const { loading, isAuthenticated } = useAuth();

  // Loading screen prevents the login page from flashing on refresh
  if (loading) return <LoadingScreen />;

  return (
    <Routes>
      <Route
        path="/login"
        element={isAuthenticated ? <Navigate to="/" replace /> : <LoginPage />}
      />

      <Route
        path="/"
        element={
          <RequireAuth>
            <AppShell />
          </RequireAuth>
        }
      >
        <Route index element={<HomeRouter />} />
        <Route path="identitate" element={<Identity />} />
        <Route path="note" element={<Grades />} />
        <Route path="orar" element={<Orar />} />
        <Route path="catalog" element={<Catalog />} />
        <Route path="examene" element={<Examene />} />
        <Route path="disponibilitate" element={<Availability />} />
        <Route path="evaluare" element={<Evaluare />} />
        <Route path="examen" element={<InscriereExamen />} />
        <Route path="taxe" element={<PlataTaxe />} />

        {/* Admin sections (moved from tabs to nav routes) */}
        <Route path="admin/utilizatori" element={<div className="page"><Users /></div>} />
        <Route path="admin/discipline" element={<div className="page"><Courses /></div>} />
        <Route path="admin/orar" element={<div className="page"><OrarEditor /></div>} />
        <Route path="admin/generare" element={<div className="page"><OrarGenerator /></div>} />
        <Route path="admin/calendar" element={<div className="page"><Calendar /></div>} />
        <Route path="admin/evaluari" element={<div className="page"><Evaluari /></div>} />
        <Route path="admin/linkuri" element={<div className="page"><Links /></div>} />
        <Route path="admin/admisi" element={<div className="page"><ConturiAdmisi /></div>} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
