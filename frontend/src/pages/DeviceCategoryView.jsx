import { useState, useEffect, useCallback } from 'react';
import api from '../services/api';
import DeviceCard from '../components/DeviceCard';

const SUBTYPES = {
  'Smart Lights': 'SMART_LIGHT',
  'Thermostats': 'THERMOSTAT',
  'Temperature Sensors': 'TEMPERATURE_SENSOR',
  'Motion Sensors': 'MOTION_SENSOR',
  'Cameras': 'CAMERA',
};

const CATEGORY_META = {
  SMART_LIGHT: { icon: 'sun', color: '#F59E0B', bg: 'rgba(245,158,11,0.08)' },
  THERMOSTAT: { icon: 'circle', color: '#FB923C', bg: 'rgba(251,146,60,0.08)' },
  TEMPERATURE_SENSOR: { icon: 'flame', color: '#F87171', bg: 'rgba(248,113,113,0.08)' },
  MOTION_SENSOR: { icon: 'zap', color: '#60A5FA', bg: 'rgba(96,165,250,0.08)' },
  CAMERA: { icon: 'camera', color: '#34D399', bg: 'rgba(52,211,153,0.08)' },
};

const STATUS_OPTIONS = ['ONLINE', 'OFFLINE', 'ERROR'];

function StatusBadge({ status }) {
  const color =
    status === 'ONLINE' ? '#34D399' : status === 'OFFLINE' ? '#F87171' : '#FBBF24';
  return (
    <span
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: '5px',
        fontSize: '11px',
        fontWeight: 600,
        letterSpacing: '0.08em',
        textTransform: 'uppercase',
        color,
        padding: '2px 8px',
        borderRadius: '999px',
        border: '1px solid ' + color + '40',
        background: color + '14',
      }}
    >
      <span
        style={{
          width: '6px',
          height: '6px',
          borderRadius: '50%',
          background: color,
          display: 'inline-block',
          boxShadow: status === 'ONLINE' ? '0 0 6px ' + color : 'none',
          animation: status === 'ONLINE' ? 'pulse 2s infinite' : 'none',
        }}
      />
      {status}
    </span>
  );
}

export default function DeviceCategoryView({ categoryName, onBack }) {
  const subtype = SUBTYPES[categoryName];
  const meta = CATEGORY_META[subtype] || { icon: 'box', color: '#C88242', bg: 'rgba(200,130,66,0.08)' };

  const [devices, setDevices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [patchingId, setPatchingId] = useState(null);
  const [patchError, setPatchError] = useState(null);
  const [patchSuccess, setPatchSuccess] = useState(null);
  const [filterHome, setFilterHome] = useState('ALL');

  const fetchDevices = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await api.get('/api/devices/subtype/' + subtype + '/detailed');
      setDevices(res.data || []);
    } catch (err) {
      setError('Failed to load devices: ' + (err.response?.data?.error || err.message));
    } finally {
      setLoading(false);
    }
  }, [subtype]);

  useEffect(() => {
    fetchDevices();
  }, [fetchDevices]);

  const handleStatusChange = async (deviceId, newStatus) => {
    setPatchingId(deviceId);
    setPatchError(null);
    setPatchSuccess(null);
    try {
      const res = await api.patch('/api/devices/' + deviceId + '/status', { status: newStatus });
      const updated = res.data;
      setDevices((prev) =>
        prev.map((d) =>
          d.deviceId === deviceId ? { ...d, status: updated.status } : d
        )
      );
      setPatchSuccess('Device ' + deviceId + ' → ' + updated.status);
      setTimeout(() => setPatchSuccess(null), 3000);
    } catch (err) {
      setPatchError('Failed: ' + (err.response?.data?.error || err.message));
      setTimeout(() => setPatchError(null), 4000);
    } finally {
      setPatchingId(null);
    }
  };

  // Unique home names for filter
  const homes = ['ALL', ...Array.from(new Set(devices.map((d) => d.homeName).filter(Boolean)))];
  const filtered = filterHome === 'ALL' ? devices : devices.filter((d) => d.homeName === filterHome);

  return (
    <main style={{ minHeight: '100vh', background: '#0D0907', color: '#FBF9F5', fontFamily: 'Inter, sans-serif' }}>
      {/* Top Bar */}
      <nav
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '16px 28px',
          borderBottom: '1px solid #1E1A17',
          background: '#110C09',
          position: 'sticky',
          top: 0,
          zIndex: 10,
        }}
      >
        <button
          onClick={onBack}
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            background: 'transparent',
            border: 'none',
            color: '#C88242',
            cursor: 'pointer',
            fontSize: '13px',
            fontWeight: 500,
            letterSpacing: '0.04em',
          }}
        >
          ← Back to Home
        </button>

        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <span
            style={{
              fontSize: '11px',
              fontWeight: 700,
              letterSpacing: '0.12em',
              textTransform: 'uppercase',
              color: meta.color,
              padding: '3px 10px',
              borderRadius: '999px',
              border: '1px solid ' + meta.color + '44',
              background: meta.bg,
            }}
          >
            {categoryName}
          </span>
          <span style={{ fontSize: '12px', color: '#A8988B' }}>
            {loading ? '...' : devices.length + ' devices'}
          </span>
        </div>
      </nav>

      {/* Header */}
      <header
        style={{
          padding: '48px 28px 32px',
          borderBottom: '1px solid #1E1A17',
          background: 'linear-gradient(135deg, #110C09 0%, #0D0907 100%)',
        }}
      >
        <p
          style={{
            fontSize: '10px',
            fontWeight: 700,
            letterSpacing: '0.2em',
            textTransform: 'uppercase',
            color: meta.color,
            marginBottom: '12px',
          }}
        >
          CONNECTED LIVING · {subtype.replace(/_/g, ' ')}
        </p>
        <h1
          style={{
            fontSize: 'clamp(28px, 5vw, 44px)',
            fontWeight: 300,
            lineHeight: 1.15,
            color: '#FBF9F5',
            margin: '0 0 12px',
          }}
        >
          {categoryName}
          <br />
          <span style={{ color: meta.color }}>{filtered.length} devices</span>
        </h1>

        {/* Status feedback */}
        {patchSuccess && (
          <div
            style={{
              marginTop: '12px',
              padding: '8px 16px',
              borderRadius: '8px',
              background: 'rgba(52,211,153,0.12)',
              border: '1px solid rgba(52,211,153,0.3)',
              color: '#34D399',
              fontSize: '13px',
              fontWeight: 500,
            }}
          >
            ✓ {patchSuccess}
          </div>
        )}
        {patchError && (
          <div
            style={{
              marginTop: '12px',
              padding: '8px 16px',
              borderRadius: '8px',
              background: 'rgba(248,113,113,0.12)',
              border: '1px solid rgba(248,113,113,0.3)',
              color: '#F87171',
              fontSize: '13px',
              fontWeight: 500,
            }}
          >
            ✗ {patchError}
          </div>
        )}
      </header>

      {/* Filter Bar */}
      {!loading && devices.length > 0 && (
        <div
          style={{
            display: 'flex',
            gap: '8px',
            padding: '16px 28px',
            overflowX: 'auto',
            borderBottom: '1px solid #1E1A17',
            background: '#0D0907',
          }}
        >
          {homes.map((h) => (
            <button
              key={h}
              onClick={() => setFilterHome(h)}
              style={{
                flexShrink: 0,
                padding: '5px 14px',
                borderRadius: '999px',
                border: '1px solid ' + (filterHome === h ? meta.color : '#2E2520'),
                background: filterHome === h ? meta.bg : 'transparent',
                color: filterHome === h ? meta.color : '#A8988B',
                fontSize: '11px',
                fontWeight: 600,
                letterSpacing: '0.06em',
                textTransform: 'uppercase',
                cursor: 'pointer',
                transition: 'all 0.2s',
                whiteSpace: 'nowrap',
              }}
            >
              {h === 'ALL' ? 'All Homes' : h}
            </button>
          ))}
        </div>
      )}

      {/* Content */}
      <section style={{ padding: '32px 28px', maxWidth: '1400px', margin: '0 auto' }}>
        {loading ? (
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', padding: '48px 0', justifyContent: 'center' }}>
            <div
              style={{
                width: '32px',
                height: '32px',
                border: '2px solid ' + meta.color,
                borderTopColor: 'transparent',
                borderRadius: '50%',
                animation: 'spin 1s linear infinite',
              }}
            />
            <span style={{ fontSize: '12px', letterSpacing: '0.12em', textTransform: 'uppercase', color: '#A8988B' }}>
              Querying Oracle…
            </span>
          </div>
        ) : error ? (
          <div
            style={{
              padding: '24px',
              borderRadius: '12px',
              background: 'rgba(248,113,113,0.08)',
              border: '1px solid rgba(248,113,113,0.2)',
              color: '#F87171',
              textAlign: 'center',
            }}
          >
            {error}
          </div>
        ) : filtered.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '64px 0', color: '#A8988B' }}>
            <div style={{ fontSize: '32px', marginBottom: '12px', opacity: 0.5 }}>◌</div>
            <p style={{ fontSize: '14px' }}>No devices found.</p>
          </div>
        ) : (
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))',
              gap: '20px',
            }}
          >
            {filtered.map((device) => (
              <div key={device.deviceId} style={{ position: 'relative' }}>
                {/* Home label above card */}
                {device.homeName && (
                  <div
                    style={{
                      fontSize: '10px',
                      fontWeight: 600,
                      letterSpacing: '0.1em',
                      textTransform: 'uppercase',
                      color: '#A8988B',
                      marginBottom: '4px',
                      paddingLeft: '4px',
                    }}
                  >
                    {device.homeName}
                  </div>
                )}

                <div
                  style={{
                    background: '#1B1613',
                    border: '1px solid #2E2520',
                    borderRadius: '16px',
                    overflow: 'hidden',
                    transition: 'border-color 0.2s, box-shadow 0.2s',
                  }}
                  onMouseEnter={(e) => {
                    e.currentTarget.style.borderColor = meta.color + '60';
                    e.currentTarget.style.boxShadow = '0 4px 32px rgba(0,0,0,0.4)';
                  }}
                  onMouseLeave={(e) => {
                    e.currentTarget.style.borderColor = '#2E2520';
                    e.currentTarget.style.boxShadow = 'none';
                  }}
                >
                  <DeviceCard device={device} />

                  {/* Status Control */}
                  <div
                    style={{
                      padding: '12px 16px',
                      borderTop: '1px solid #2E2520',
                      background: '#17120F',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      gap: '8px',
                    }}
                  >
                    <span style={{ fontSize: '10px', textTransform: 'uppercase', letterSpacing: '0.1em', color: '#A8988B' }}>
                      Set Status
                    </span>
                    <div style={{ display: 'flex', gap: '4px' }}>
                      {STATUS_OPTIONS.map((s) => {
                        const isActive = device.status === s;
                        const isPatchingThis = patchingId === device.deviceId;
                        const color = s === 'ONLINE' ? '#34D399' : s === 'OFFLINE' ? '#F87171' : '#FBBF24';
                        return (
                          <button
                            key={s}
                            disabled={isActive || isPatchingThis}
                            onClick={() => handleStatusChange(device.deviceId, s)}
                            style={{
                              padding: '3px 9px',
                              borderRadius: '999px',
                              border: '1px solid ' + color + (isActive ? 'cc' : '40'),
                              background: isActive ? color + '22' : 'transparent',
                              color: isActive ? color : color + '88',
                              fontSize: '10px',
                              fontWeight: 600,
                              letterSpacing: '0.08em',
                              textTransform: 'uppercase',
                              cursor: isActive || isPatchingThis ? 'default' : 'pointer',
                              opacity: isPatchingThis && !isActive ? 0.5 : 1,
                              transition: 'all 0.15s',
                            }}
                          >
                            {isPatchingThis && !isActive ? '…' : s}
                          </button>
                        );
                      })}
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>

      <style>{`
        @keyframes spin { to { transform: rotate(360deg); } }
        @keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.4; } }
      `}</style>
    </main>
  );
}
