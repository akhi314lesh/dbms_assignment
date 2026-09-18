import { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { auth } from '../config/firebase';
import api from '../services/api';
import HomeSelectModal from '../components/HomeSelectModal';
import CreateHomeModal from '../components/CreateHomeModal';

const staticDeviceCategories = [
  {
    name: 'Smart Lights',
    description: 'Adaptive lighting that responds to your routines.',
    icon: '☼',
  },
  {
    name: 'Thermostats',
    description: 'Intelligent climate control for effortless comfort.',
    icon: '◉',
  },
  {
    name: 'Temperature Sensors',
    description: 'Real-time temperature monitoring throughout your home.',
    icon: '♨',
  },
  {
    name: 'Motion Sensors',
    description: 'Quietly monitor movement and activity in every room.',
    icon: '⌁',
  },
  {
    name: 'Cameras',
    description: 'Keep an eye on what matters, wherever you are.',
    icon: '◌',
  },
];

function Landing({ onSelectHome, onSelectCategory }) {
  const [activeDevice, setActiveDevice] = useState(null);
  const { oracleUser, signOut } = useAuth();

  // State for homes and summary
  const [homes, setHomes] = useState([]);
  const [summary, setSummary] = useState(null);
  const [loadingHomes, setLoadingHomes] = useState(true);
  const [loadingSummary, setLoadingSummary] = useState(true);

  // Modals
  const [selectModalOpen, setSelectModalOpen] = useState(false);
  const [createModalOpen, setCreateModalOpen] = useState(false);

  const fetchHomesAndSummary = async () => {
    try {
      setLoadingHomes(true);
      const homesRes = await api.get('/api/homes');
      setHomes(homesRes.data || []);
    } catch (err) {
      console.error('Failed to fetch homes', err);
    } finally {
      setLoadingHomes(false);
    }

    try {
      setLoadingSummary(true);
      const summaryRes = await api.get('/api/dashboard/summary');
      setSummary(summaryRes.data || null);
    } catch (err) {
      console.error('Failed to fetch dashboard summary', err);
    } finally {
      setLoadingSummary(false);
    }
  };

  useEffect(() => {
    fetchHomesAndSummary();
  }, []);

  const handleHomeCreated = (newHome) => {
    setHomes((prev) => [...prev, newHome]);
    fetchHomesAndSummary();
    if (onSelectHome) onSelectHome(newHome);
  };

  return (
    <main className="landing-page">
      {/* Navigation */}
      <nav className="landing-nav">
        <div className="brand">
          <img src="/logo.svg" alt="Smart Home" />
          <span>Smart Home</span>
        </div>

        <div className="nav-links">
          <button
            onClick={() => setSelectModalOpen(true)}
            className="text-xs uppercase tracking-wider text-[#E6D9CD] hover:text-[#C88242] transition-colors font-medium mr-2"
          >
            My Homes ({homes.length})
          </button>
          <a href="#summary">Overview</a>
          <a href="#devices">Devices</a>
          <a href="#about">About</a>

          {oracleUser && (
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                padding: '4px 12px',
                background: '#261F1A',
                borderRadius: '999px',
                fontSize: '12px',
              }}
            >
              <span style={{ color: '#FBF9F5', fontWeight: 500 }}>{oracleUser.name}</span>
              <span
                style={{
                  padding: '2px 6px',
                  borderRadius: '4px',
                  background: 'rgba(200, 130, 66, 0.25)',
                  color: '#C88242',
                  fontSize: '10px',
                  fontWeight: 700,
                  textTransform: 'uppercase',
                }}
              >
                {oracleUser.role}
              </span>
            </div>
          )}

          <button className="nav-button" id="signout-btn" onClick={signOut}>
            Sign Out
          </button>
        </div>
      </nav>

      {/* Hero Section */}
      <section className="hero-section">
        <div className="hero-content">
          <p className="eyebrow">INTELLIGENT LIVING · CONNECTED HOME</p>

          <h1>
            Smarter and
            <br />
            <span>safer living.</span>
          </h1>

          <p className="hero-description">
            Intelligent technology designed around the way you live. Monitor, manage, and
            control your home with simplicity.
          </p>

          <div className="hero-actions">
            <button
              onClick={() => setSelectModalOpen(true)}
              className="primary-button cursor-pointer"
            >
              Explore your home →
            </button>

            <span className="hero-note">
              {homes.length > 0
                ? `${homes.length} Accessible Residence${homes.length > 1 ? 's' : ''}`
                : 'Sanctuary Active'}
            </span>
          </div>
        </div>

        {/* MY HOMES Launcher Card */}
        <div className="hero-decoration">
          <div
            className="home-card"
            onClick={() => homes.length === 0 ? setCreateModalOpen(true) : setSelectModalOpen(true)}
            title={homes.length === 0 ? 'Create your first home' : 'Select a residence'}
            style={{ cursor: 'pointer' }}
          >
            {/* Background image */}
            <img
              src="/home-card.jpg"
              alt="Home sweet home"
              className="home-card-img"
            />

            {/* Bottom overlay with info */}
            <div className="home-card-overlay">
              {homes.length === 0 ? (
                <>
                  <p className="home-card-eyebrow">MY HOMES</p>
                  <p className="home-card-sub">No homes yet.</p>
                  <button
                    className="home-card-cta home-card-cta--create"
                    onClick={(e) => { e.stopPropagation(); setCreateModalOpen(true); }}
                  >
                    + Create Home
                  </button>
                </>
              ) : (
                <>
                  <p className="home-card-eyebrow">MY HOMES</p>
                  <div style={{ display: 'flex', alignItems: 'baseline', gap: '6px' }}>
                    <span className="home-card-count-num">
                      {loadingHomes ? '—' : homes.length}
                    </span>
                    <span className="home-card-sub" style={{ marginBottom: '2px' }}>
                      {homes.length === 1 ? 'Residence' : 'Residences'}
                    </span>
                  </div>
                  <span className="home-card-cta">Explore Home →</span>
                </>
              )}
            </div>
          </div>
        </div>


      </section>

      {/* Real Backend Home Overview / Summary Report */}
      <section className="max-w-7xl mx-auto px-6 py-12" id="summary">
        <div className="section-heading mb-8">
          <div>
            <p className="eyebrow">SYSTEM OVERVIEW</p>
            <h2 className="text-2xl md:text-3xl font-light text-[#FBF9F5]">
              Real-time telemetry,
              <br />
              <span>across your residences.</span>
            </h2>
          </div>
          <p className="section-description text-sm text-[#A8988B] max-w-md">
            {oracleUser?.role === 'ADMIN'
              ? 'Administrator view covering all registered homes and devices in the system.'
              : 'Authorized resident view summarizing only your accessible properties.'}
          </p>
        </div>

        {loadingSummary ? (
          <div className="flex items-center justify-center py-12 gap-3">
            <div className="w-8 h-8 border-2 border-[#C88242] border-t-transparent rounded-full animate-spin"></div>
            <p className="text-xs uppercase tracking-widest text-[#A8988B]">
              Querying Oracle Telemetry...
            </p>
          </div>
        ) : summary ? (
          <div className="space-y-6">
            {/* Primary System Metrics */}
            <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
              <div className="p-4 rounded-xl bg-[#1B1613] border border-[#2E2520]">
                <span className="text-[10px] uppercase tracking-wider text-[#A8988B] block mb-1">
                  Total Homes
                </span>
                <span className="text-2xl font-light text-[#FBF9F5]">
                  {summary.totalHomes != null ? summary.totalHomes : '--'}
                </span>
              </div>

              <div className="p-4 rounded-xl bg-[#1B1613] border border-[#2E2520]">
                <span className="text-[10px] uppercase tracking-wider text-[#A8988B] block mb-1">
                  Total Devices
                </span>
                <span className="text-2xl font-light text-[#FBF9F5]">
                  {summary.totalDevices != null ? summary.totalDevices : '--'}
                </span>
              </div>

              <div className="p-4 rounded-xl bg-[#1B1613] border border-[#2E2520]">
                <span className="text-[10px] uppercase tracking-wider text-[#A8988B] block mb-1">
                  Online Devices
                </span>
                <span className="text-2xl font-light text-emerald-400">
                  {summary.activeDevices != null ? summary.activeDevices : '--'}
                </span>
              </div>

              <div className="p-4 rounded-xl bg-[#1B1613] border border-[#2E2520]">
                <span className="text-[10px] uppercase tracking-wider text-[#A8988B] block mb-1">
                  Offline Devices
                </span>
                <span className="text-2xl font-light text-red-400">
                  {summary.offlineDevices != null ? summary.offlineDevices : '--'}
                </span>
              </div>

              <div className="p-4 rounded-xl bg-[#1B1613] border border-[#2E2520]">
                <span className="text-[10px] uppercase tracking-wider text-[#A8988B] block mb-1">
                  Alerts
                </span>
                <span className="text-2xl font-light text-amber-400">
                  {summary.totalAlerts != null ? summary.totalAlerts : '--'}
                </span>
              </div>

              <div className="p-4 rounded-xl bg-[#1B1613] border border-[#2E2520]">
                <span className="text-[10px] uppercase tracking-wider text-[#A8988B] block mb-1">
                  Automation Rules
                </span>
                <span className="text-2xl font-light text-[#C88242]">
                  {summary.totalAutomationRules != null ? summary.totalAutomationRules : '--'}
                </span>
              </div>
            </div>

            {/* Dynamic Device Breakdown from Oracle */}
            {summary.deviceBreakdown && (
              <div className="p-6 rounded-2xl bg-[#17120F] border border-[#2E2520]">
                <div className="flex items-center justify-between mb-4">
                  <p className="text-xs uppercase tracking-[0.2em] text-[#C88242] font-semibold">
                    DEVICE BREAKDOWN · HARDWARE DISTRIBUTION
                  </p>
                  <span className="text-[10px] text-[#A8988B] font-mono">Live DB Totals</span>
                </div>

                <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-5 gap-4">
                  <div className="p-3 rounded-xl bg-[#1F1915] border border-[#3E322A]/60 flex items-center gap-3">
                    <span className="text-lg text-yellow-400">☼</span>
                    <div>
                      <span className="text-[10px] uppercase text-[#A8988B] block">Smart Lights</span>
                      <span className="text-lg font-light text-[#FBF9F5]">
                        {summary.deviceBreakdown.smartLights || 0}
                      </span>
                    </div>
                  </div>

                  <div className="p-3 rounded-xl bg-[#1F1915] border border-[#3E322A]/60 flex items-center gap-3">
                    <span className="text-lg text-orange-400">◉</span>
                    <div>
                      <span className="text-[10px] uppercase text-[#A8988B] block">Thermostats</span>
                      <span className="text-lg font-light text-[#FBF9F5]">
                        {summary.deviceBreakdown.thermostats || 0}
                      </span>
                    </div>
                  </div>

                  <div className="p-3 rounded-xl bg-[#1F1915] border border-[#3E322A]/60 flex items-center gap-3">
                    <span className="text-lg text-rose-400">♨</span>
                    <div>
                      <span className="text-[10px] uppercase text-[#A8988B] block">Temp Sensors</span>
                      <span className="text-lg font-light text-[#FBF9F5]">
                        {summary.deviceBreakdown.temperatureSensors || 0}
                      </span>
                    </div>
                  </div>

                  <div className="p-3 rounded-xl bg-[#1F1915] border border-[#3E322A]/60 flex items-center gap-3">
                    <span className="text-lg text-blue-400">⌁</span>
                    <div>
                      <span className="text-[10px] uppercase text-[#A8988B] block">Motion Sensors</span>
                      <span className="text-lg font-light text-[#FBF9F5]">
                        {summary.deviceBreakdown.motionSensors || 0}
                      </span>
                    </div>
                  </div>

                  <div className="p-3 rounded-xl bg-[#1F1915] border border-[#3E322A]/60 flex items-center gap-3">
                    <span className="text-lg text-emerald-400">◌</span>
                    <div>
                      <span className="text-[10px] uppercase text-[#A8988B] block">Cameras</span>
                      <span className="text-lg font-light text-[#FBF9F5]">
                        {summary.deviceBreakdown.cameras || 0}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            )}
          </div>
        ) : null}
      </section>

      {/* Devices Section */}
      <section className="devices-section" id="devices">
        <div className="section-heading">
          <div>
            <p className="eyebrow">CONNECTED LIVING</p>
            <h2>
              Your home,
              <br />
              <span>intelligently connected.</span>
            </h2>
          </div>

          <p className="section-description">
            Every device works together to create a home that understands your needs.
          </p>
        </div>

        <div className="device-grid">
          {staticDeviceCategories.map((device, index) => (
            <div
              className={`device-card ${
                activeDevice === index ? 'device-card-active' : ''
              }`}
              key={device.name}
              onMouseEnter={() => setActiveDevice(index)}
              onMouseLeave={() => setActiveDevice(null)}
              onClick={() => onSelectCategory && onSelectCategory(device.name)}
            >
              <div className="device-top">
                <span className="device-number">0{index + 1}</span>
                <span className="device-icon">{device.icon}</span>
              </div>

              <div className="device-info">
                <h3>{device.name}</h3>
                <p>{device.description}</p>
                <span className="device-arrow">Browse category →</span>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* About */}
      <section className="about-section" id="about">
        <div className="about-line"></div>
        <p className="eyebrow">THE SMART HOME PHILOSOPHY</p>
        <h2>
          Technology should feel
          <br />
          <span>effortless.</span>
        </h2>
        <p className="about-text">
          From adaptive lighting to intelligent security, Smart Home brings every part of
          your home together in one calm, considered experience.
        </p>
      </section>

      {/* Footer */}
      <footer className="landing-footer">
        <div className="brand">
          <img src="/logo.svg" alt="Smart Home" />
          <span>Smart Home</span>
        </div>
        <p>Smarter and safer living.</p>
        <span>© 2026 Smart Home</span>
      </footer>

      {/* Home Selection Overlay */}
      <HomeSelectModal
        isOpen={selectModalOpen}
        onClose={() => setSelectModalOpen(false)}
        homes={homes}
        loading={loadingHomes}
        onSelectHome={(home) => {
          if (onSelectHome) onSelectHome(home);
        }}
        onCreateHome={() => setCreateModalOpen(true)}
      />

      {/* Create Home Modal for empty state or new home */}
      <CreateHomeModal
        isOpen={createModalOpen}
        onClose={() => setCreateModalOpen(false)}
        onHomeCreated={handleHomeCreated}
      />
    </main>
  );
}

export default Landing;