import { useState } from 'react'
import { AuthProvider, useAuth } from './context/AuthContext'
import Authentication from './pages/Authentication'
import Landing from './pages/Landing'
import HomeDetails from './pages/HomeDetails'
import DeviceCategoryView from './pages/DeviceCategoryView'

function AppContent() {
  const { firebaseUser, loading } = useAuth()
  const [guestMode, setGuestMode] = useState(false)
  const [selectedHome, setSelectedHome] = useState(null)
  const [selectedCategory, setSelectedCategory] = useState(null)

  if (loading) {
    return (
      <div className="min-h-screen w-full flex items-center justify-center bg-[#140F0C] text-[#FBF9F5]">
        <div className="flex flex-col items-center gap-4">
          <div className="w-10 h-10 border-4 border-[#22d3ee] border-t-transparent rounded-full animate-spin"></div>
          <p className="text-sm tracking-widest text-[#E6D9CD] uppercase font-mono">
            Connecting InfluxDB Time-Series Sanctuary (:8086)...
          </p>
        </div>
      </div>
    )
  }

  // Allow exploration if signed in OR if guest mode is activated
  if (!firebaseUser && !guestMode) {
    return (
      <div className="relative">
        {/* Quick Demo Bypass Bar */}
        <div className="bg-[#0f172a] border-b border-cyan-500/30 px-4 py-2.5 text-center flex items-center justify-center gap-4 text-xs text-slate-300">
          <span className="flex items-center gap-1.5 text-cyan-400 font-medium">
            ⚡ <strong>Website 2: InfluxDB Smart Home Dashboard</strong> (:5174)
          </span>
          <button
            onClick={() => setGuestMode(true)}
            className="px-3 py-1 rounded bg-cyan-500 hover:bg-cyan-400 text-slate-950 font-bold uppercase tracking-wider text-[11px] transition-all"
          >
            Enter Sanctuary as Guest (InfluxDB Mode) →
          </button>
          <a
            href="http://localhost:5175"
            className="text-purple-400 hover:text-purple-300 underline text-xs"
          >
            Or Open Benchmark Dashboard (:5175)
          </a>
        </div>
        <Authentication />
      </div>
    )
  }

  return (
    <>
      {/* Top Edition Banner */}
      <div className="bg-[#0c1322] border-b border-cyan-500/20 px-4 py-1.5 flex items-center justify-between text-xs text-slate-300">
        <div className="flex items-center gap-2">
          <span className="w-2 h-2 rounded-full bg-cyan-400 animate-pulse" />
          <span className="font-semibold text-cyan-400 tracking-wider uppercase text-[10px]">
            InfluxDB Time-Series Engine Active
          </span>
          <span className="hidden md:inline text-slate-500 text-[10px]">
            Bucket: sensor_telemetry (:8086) • Sub-10ms Aggregations
          </span>
        </div>
        <div className="flex items-center gap-3 text-[11px]">
          <a
            href="http://localhost:5173"
            className="text-orange-400 hover:text-orange-300 transition-colors"
          >
            ← Oracle Dashboard (:5173)
          </a>
          <span className="text-slate-600">|</span>
          <a
            href="http://localhost:5175"
            className="text-purple-400 hover:text-purple-300 transition-colors"
          >
            ⚖️ Benchmark (:5175) →
          </a>
        </div>
      </div>

      {selectedHome ? (
        <HomeDetails
          home={selectedHome}
          onBack={() => setSelectedHome(null)}
        />
      ) : selectedCategory ? (
        <DeviceCategoryView
          categoryName={selectedCategory}
          onBack={() => setSelectedCategory(null)}
        />
      ) : (
        <Landing
          onSelectHome={(home) => setSelectedHome(home)}
          onSelectCategory={(cat) => setSelectedCategory(cat)}
        />
      )}
    </>
  )
}

export default function App() {
  return (
    <AuthProvider>
      <AppContent />
    </AuthProvider>
  )
}
