import { AuthProvider, useAuth } from './context/AuthContext';
import ComparisonDashboard from './components/ComparisonDashboard';

const ORACLE_SITE = import.meta.env.VITE_ORACLE_SITE_URL || 'http://localhost:5173';
const INFLUX_SITE = import.meta.env.VITE_INFLUX_SITE_URL || 'http://localhost:5174';

function AppContent() {
  const { oracleUser } = useAuth();

  return (
    <div style={{ minHeight: '100vh', background: 'var(--cream)' }}>
      <nav className="cmp-nav">
        <div className="cmp-nav-brand">
          <img className="cmp-nav-icon" src="/logo.svg" alt="Smart Home" />
          <span className="cmp-nav-title">Smart<br />Home</span>
          <span className="cmp-nav-badge">⚖️ DB Comparison (:5175)</span>
        </div>

        <div className="cmp-nav-links">
          <a href={ORACLE_SITE} className="cmp-nav-link oracle" title="Oracle Smart Home Dashboard">🏛️ Oracle<span className="cmp-hide-sm"> Dashboard (:5173)</span></a>
          <a href={INFLUX_SITE} className="cmp-nav-link influx" title="InfluxDB Smart Home Dashboard">⚡ InfluxDB<span className="cmp-hide-sm"> Dashboard (:5174)</span></a>
          <span className="cmp-user-chip" title="Local development identity (authentication is disabled for DA3)">
            {oracleUser?.name || 'DA3 Teammate'}<b>{oracleUser?.role || 'ADMIN'}</b>
          </span>
        </div>
      </nav>

      <ComparisonDashboard />

      <footer className="cmp-footer">
        <div className="cmp-footer-inner">
          <div className="cmp-footer-brand">
            <img src="/logo.svg" alt="Smart Home" />
            <span>Smart Home</span>
          </div>
          <p style={{ margin: 0 }}>Oracle 21c XE vs InfluxDB 2.x · DA3 performance comparison.</p>
          <span>© 2026 Smart Home</span>
        </div>
      </footer>
    </div>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <AppContent />
    </AuthProvider>
  );
}
