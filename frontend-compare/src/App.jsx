import { useState } from 'react'
import { AuthProvider, useAuth } from './context/AuthContext'
import Authentication from './pages/Authentication'
import ComparisonDashboard from './components/ComparisonDashboard'

function AppContent() {
  const { firebaseUser, signOut } = useAuth()
  const [showAuthModal, setShowAuthModal] = useState(false)

  return (
    <div style={{ minHeight: '100vh', background: '#03060f' }}>
      {/* Navigation */}
      <nav className="cmp-nav">
        <div className="cmp-nav-brand">
          <div className="cmp-nav-icon">⚖️</div>
          <span className="cmp-nav-title">Smart Home</span>
          <span className="cmp-nav-badge">DB Comparison</span>
        </div>

        <div className="cmp-nav-links">
          <a href="http://localhost:5173" className="cmp-nav-link oracle" title="Oracle Smart Home Dashboard">
            ← Oracle Dashboard (:5173)
          </a>
          <a href="http://localhost:5174" className="cmp-nav-link influx" title="InfluxDB Smart Home Dashboard">
            InfluxDB Dashboard (:5174) →
          </a>
          {firebaseUser ? (
            <button className="cmp-signout-btn" onClick={signOut}>
              Sign Out ({firebaseUser.email?.split('@')[0]})
            </button>
          ) : (
            <button
              className="cmp-signout-btn"
              style={{ background: 'rgba(34,211,238,0.1)', borderColor: 'rgba(34,211,238,0.3)', color: '#38bdf8' }}
              onClick={() => setShowAuthModal(true)}
            >
              Sign In
            </button>
          )}
        </div>
      </nav>

      {/* Auth Modal if clicked */}
      {showAuthModal && !firebaseUser && (
        <div style={{
          position: 'fixed', inset: 0, zIndex: 1000,
          background: 'rgba(0,0,0,0.85)', backdropFilter: 'blur(8px)',
          overflowY: 'auto'
        }}>
          <div style={{ position: 'relative' }}>
            <button
              onClick={() => setShowAuthModal(false)}
              style={{
                position: 'fixed', top: 20, right: 28, zIndex: 1010,
                background: '#1e293b', color: '#fff', border: '1px solid #475569',
                borderRadius: '50%', width: 36, height: 36, cursor: 'pointer',
                fontSize: 18, display: 'flex', alignItems: 'center', justifyContent: 'center'
              }}
            >
              ✕
            </button>
            <Authentication />
          </div>
        </div>
      )}

      <ComparisonDashboard />
    </div>
  )
}

export default function App() {
  return (
    <AuthProvider>
      <AppContent />
    </AuthProvider>
  )
}
