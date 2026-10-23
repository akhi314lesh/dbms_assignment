import React, { useState, useEffect } from 'react';
import api from '../services/api';
import DeviceCard from '../components/DeviceCard';
import SqlConsole from '../components/SqlConsole';

export default function HomeDetails({ home, onBack }) {
  const [rooms, setRooms] = useState([]);
  const [detailedDevices, setDetailedDevices] = useState([]);
  const [alerts, setAlerts] = useState([]);
  const [rules, setRules] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Health report state
  const [healthReport, setHealthReport] = useState(null);
  const [generatingReport, setGeneratingReport] = useState(false);
  const [showReportModal, setShowReportModal] = useState(false);

  // Alert acknowledgment tracking
  const [acknowledgingId, setAcknowledgingId] = useState(null);

  useEffect(() => {
    if (!home?.homeId) return;

    let isMounted = true;
    setLoading(true);
    setError(null);

    async function fetchHomeData() {
      try {
        const [roomsRes, devicesRes, alertsRes, rulesRes] = await Promise.all([
          api.get(`/api/rooms/home/${home.homeId}`),
          api.get(`/api/devices/home/${home.homeId}/detailed`),
          api.get('/api/alerts'),
          api.get('/api/rules'),
        ]);

        if (isMounted) {
          setRooms(roomsRes.data || []);
          setDetailedDevices(devicesRes.data || []);

          // Filter alerts to devices belonging to this home
          const homeDeviceIds = new Set((devicesRes.data || []).map((d) => d.deviceId));
          const homeAlerts = (alertsRes.data || []).filter(
            (a) => a.device && homeDeviceIds.has(a.device.deviceId)
          );
          setAlerts(homeAlerts);

          // Filter rules to devices in this home
          const homeRules = (rulesRes.data || []).filter(
            (r) => r.conditionDevice && homeDeviceIds.has(r.conditionDevice.deviceId)
          );
          setRules(homeRules);
        }
      } catch (err) {
        console.error('Failed to load home details', err);
        if (isMounted) {
          setError(
            err.response?.status === 403
              ? 'Access Denied: You do not have permission to view this home.'
              : 'Failed to load home details. Please check your connection.'
          );
        }
      } finally {
        if (isMounted) setLoading(false);
      }
    }

    fetchHomeData();

    return () => {
      isMounted = false;
    };
  }, [home?.homeId]);

  const handleAcknowledgeAlert = async (alertId) => {
    setAcknowledgingId(alertId);
    try {
      await api.post(`/api/alerts/${alertId}/acknowledge`);
      setAlerts((prev) =>
        prev.map((a) =>
          a.alertId === alertId
            ? { ...a, status: 'ACKNOWLEDGED', acknowledgedTimestamp: new Date().toISOString() }
            : a
        )
      );
    } catch (err) {
      console.error('Failed to acknowledge alert', err);
      alert(err.response?.data?.message || 'Failed to acknowledge alert.');
    } finally {
      setAcknowledgingId(null);
    }
  };

  const handleGenerateHealthReport = async () => {
    setGeneratingReport(true);
    try {
      const res = await api.get(`/api/homes/${home.homeId}/health-report`);
      const text = typeof res.data === 'object' && res.data?.reportText ? res.data.reportText : String(res.data);
      setHealthReport(text);
      setShowReportModal(true);
    } catch (err) {
      console.error('Failed to generate health report', err);
      alert(err.response?.data?.message || 'Failed to generate health report from Oracle database.');
    } finally {
      setGeneratingReport(false);
    }
  };

  const handleVisualizeHome = () => {
    window.open(`/visual.html?homeId=${home.homeId}`, '_blank');
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-[#140F0C] text-[#FBF9F5] flex flex-col items-center justify-center gap-4">
        <div className="w-10 h-10 border-4 border-[#C88242] border-t-transparent rounded-full animate-spin"></div>
        <p className="text-xs uppercase tracking-widest text-[#A8988B]">Loading Home Environment...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-[#140F0C] text-[#FBF9F5] p-8 flex flex-col items-center justify-center">
        <div className="max-w-md p-8 rounded-2xl bg-[#1B1613] border border-red-900/50 text-center">
          <span className="text-3xl mb-4 block">⚠</span>
          <h2 className="text-xl font-light mb-2 text-red-400">Access Issue</h2>
          <p className="text-sm text-[#A8988B] mb-6">{error}</p>
          <button
            onClick={onBack}
            className="px-6 py-2.5 rounded-xl bg-[#261F1A] border border-[#3E322A] text-sm hover:border-[#C88242] transition-colors"
          >
            ← Back to Homes
          </button>
        </div>
      </div>
    );
  }

  // Summary Metrics
  const onlineCount = detailedDevices.filter(
    (d) => d.status === 'ONLINE' || d.status === 'ON'
  ).length;
  const offlineCount = detailedDevices.length - onlineCount;

  // Group devices by room
  const devicesByRoom = {};
  rooms.forEach((r) => {
    devicesByRoom[r.roomId] = [];
  });
  detailedDevices.forEach((d) => {
    if (d.roomId && devicesByRoom[d.roomId]) {
      devicesByRoom[d.roomId].push(d);
    } else {
      if (!devicesByRoom['unassigned']) devicesByRoom['unassigned'] = [];
      devicesByRoom['unassigned'].push(d);
    }
  });

  return (
    <div className="min-h-screen bg-[#140F0C] text-[#FBF9F5] pb-24">
      {/* Top Sticky Bar */}
      <header className="sticky top-0 z-30 bg-[#140F0C]/90 backdrop-blur-md border-b border-[#2E2520] px-6 py-4">
        <div className="max-w-7xl mx-auto flex items-center justify-between">
          <button
            onClick={onBack}
            className="flex items-center gap-2 text-xs uppercase tracking-wider text-[#A8988B] hover:text-[#C88242] transition-colors"
          >
            <span>←</span> Back to Homes
          </button>

          <div className="flex items-center gap-4">
            <button
              onClick={handleGenerateHealthReport}
              disabled={generatingReport}
              className="px-4 py-2 rounded-xl bg-[#261F1A] border border-[#3E322A] hover:border-[#C88242] text-xs uppercase tracking-wider text-[#E6D9CD] transition-all flex items-center gap-2 disabled:opacity-50"
            >
              {generatingReport ? (
                <>
                  <div className="w-3 h-3 border-2 border-[#C88242] border-t-transparent rounded-full animate-spin"></div>
                  <span>Generating...</span>
                </>
              ) : (
                <>
                  <span>📋</span>
                  <span>Health Report</span>
                </>
              )}
            </button>

            <button
              onClick={handleVisualizeHome}
              className="px-5 py-2 rounded-xl bg-gradient-to-r from-[#C88242] to-[#A3642E] text-white font-medium text-xs uppercase tracking-wider shadow-lg hover:brightness-110 active:scale-[0.98] transition-all flex items-center gap-2"
            >
              <span>◈</span>
              <span>VISUALIZE HOME</span>
            </button>
          </div>
        </div>
      </header>

      <main className="max-w-7xl mx-auto px-6 pt-8 space-y-12">
        {/* Home Header */}
        <section className="flex flex-col md:flex-row md:items-end justify-between gap-6 pb-6 border-b border-[#2E2520]">
          <div>
            <p className="text-xs uppercase tracking-[0.25em] text-[#C88242] font-semibold mb-2">
              SMART RESIDENCE #{home.homeId}
            </p>
            <h1 className="text-3xl md:text-4xl font-light tracking-wide text-[#FBF9F5] mb-2">
              {home.homeName}
            </h1>
            <p className="text-sm text-[#A8988B] flex items-center gap-2">
              <span>📍</span>
              <span>
                {home.street ? `${home.street}, ` : ''}
                {home.city ? `${home.city} ` : ''}
                {home.pincode || ''}
              </span>
            </p>
          </div>

          <div className="flex items-center gap-3">
            <div className="p-3 rounded-xl bg-[#1B1613] border border-[#2E2520] flex items-center gap-3">
              <span className="w-2.5 h-2.5 rounded-full bg-emerald-400 animate-pulse"></span>
              <div>
                <span className="text-[10px] uppercase text-[#A8988B] block">Digital Twin Seam</span>
                <span className="text-xs font-medium text-[#FBF9F5]">Ready on ID {home.homeId}</span>
              </div>
            </div>
          </div>
        </section>

        {/* Summary Metric Cards */}
        <section className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-3">
          <div className="p-4 rounded-xl bg-[#1B1613] border border-[#2E2520]">
            <span className="text-[10px] uppercase tracking-wider text-[#A8988B] block mb-1">
              Rooms
            </span>
            <span className="text-2xl font-light text-[#FBF9F5]">{rooms.length}</span>
          </div>

          <div className="p-4 rounded-xl bg-[#1B1613] border border-[#2E2520]">
            <span className="text-[10px] uppercase tracking-wider text-[#A8988B] block mb-1">
              Devices
            </span>
            <span className="text-2xl font-light text-[#FBF9F5]">{detailedDevices.length}</span>
          </div>

          <div className="p-4 rounded-xl bg-[#1B1613] border border-[#2E2520]">
            <span className="text-[10px] uppercase tracking-wider text-[#A8988B] block mb-1">
              Online
            </span>
            <span className="text-2xl font-light text-emerald-400">{onlineCount}</span>
          </div>

          <div className="p-4 rounded-xl bg-[#1B1613] border border-[#2E2520]">
            <span className="text-[10px] uppercase tracking-wider text-[#A8988B] block mb-1">
              Offline
            </span>
            <span className="text-2xl font-light text-red-400">{offlineCount}</span>
          </div>

          <div className="p-4 rounded-xl bg-[#1B1613] border border-[#2E2520]">
            <span className="text-[10px] uppercase tracking-wider text-[#A8988B] block mb-1">
              Alerts
            </span>
            <span className="text-2xl font-light text-amber-400">{alerts.length}</span>
          </div>

          <div className="p-4 rounded-xl bg-[#1B1613] border border-[#2E2520]">
            <span className="text-[10px] uppercase tracking-wider text-[#A8988B] block mb-1">
              Automation
            </span>
            <span className="text-2xl font-light text-[#C88242]">{rules.length}</span>
          </div>
        </section>

        {/* Room-Grouped Devices */}
        <section className="space-y-8">
          <div>
            <p className="text-xs uppercase tracking-[0.25em] text-[#C88242] font-semibold mb-1">
              PHYSICAL TOPOLOGY
            </p>
            <h2 className="text-xl font-light tracking-wide text-[#FBF9F5]">
              Rooms & Connected Hardware
            </h2>
          </div>

          {rooms.length === 0 ? (
            <div className="p-8 rounded-2xl bg-[#1B1613] border border-[#2E2520] text-center text-[#A8988B] text-sm">
              No rooms registered in this residence yet.
            </div>
          ) : (
            <div className="space-y-8">
              {rooms.map((room) => {
                const roomDevices = devicesByRoom[room.roomId] || [];
                return (
                  <div
                    key={room.roomId}
                    className="p-6 rounded-2xl bg-[#17120F] border border-[#2E2520] space-y-4"
                  >
                    <div className="flex items-center justify-between pb-3 border-b border-[#2E2520]">
                      <div className="flex items-center gap-3">
                        <span className="w-7 h-7 rounded-lg bg-[#261F1A] border border-[#3E322A] flex items-center justify-center text-xs text-[#C88242]">
                          {room.floorNumber || 1}F
                        </span>
                        <div>
                          <h3 className="text-base font-medium text-[#FBF9F5]">
                            {room.roomName.toUpperCase()}
                          </h3>
                          <span className="text-[11px] text-[#A8988B]">
                            Floor {room.floorNumber || 1} · {roomDevices.length} device(s)
                          </span>
                        </div>
                      </div>
                    </div>

                    {roomDevices.length === 0 ? (
                      <p className="text-xs text-[#A8988B] italic py-2">
                        No devices located in this room.
                      </p>
                    ) : (
                      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                        {roomDevices.map((device) => (
                          <DeviceCard key={device.deviceId} device={device} />
                        ))}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          )}
        </section>

        {/* Alerts Section */}
        <section className="space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs uppercase tracking-[0.25em] text-[#C88242] font-semibold mb-1">
                SYSTEM TELEMETRY
              </p>
              <h2 className="text-xl font-light tracking-wide text-[#FBF9F5]">
                Home Alerts & Warnings
              </h2>
            </div>
          </div>

          {alerts.length === 0 ? (
            <div className="p-6 rounded-2xl bg-[#1B1613] border border-[#2E2520] text-center text-xs text-[#A8988B]">
              All systems normal. No active or pending alerts for this residence.
            </div>
          ) : (
            <div className="rounded-2xl bg-[#1B1613] border border-[#2E2520] overflow-hidden">
              <div className="divide-y divide-[#2E2520]">
                {alerts.map((alert) => {
                  const isAck = alert.status === 'ACKNOWLEDGED';
                  return (
                    <div
                      key={alert.alertId}
                      className="p-4 flex flex-col md:flex-row md:items-center justify-between gap-4 hover:bg-[#261F1A]/30 transition-colors"
                    >
                      <div className="flex items-start gap-3">
                        <span
                          className={`mt-0.5 w-2 h-2 rounded-full ${
                            isAck ? 'bg-stone-500' : 'bg-amber-400 animate-ping'
                          }`}
                        ></span>
                        <div>
                          <p className="text-sm text-[#FBF9F5] font-medium">{alert.message}</p>
                          <div className="flex items-center gap-3 text-[11px] text-[#A8988B] mt-1">
                            <span>Alert #{alert.alertId}</span>
                            {alert.device && <span>Device: {alert.device.deviceName}</span>}
                            <span>{new Date(alert.alertTime).toLocaleString()}</span>
                          </div>
                        </div>
                      </div>

                      <div className="flex items-center gap-3 self-end md:self-center">
                        <span
                          className={`text-[10px] font-semibold px-2.5 py-1 rounded border uppercase tracking-wider ${
                            isAck
                              ? 'bg-stone-900 text-stone-400 border-stone-800'
                              : 'bg-amber-950/60 text-amber-300 border-amber-800'
                          }`}
                        >
                          {alert.status}
                        </span>

                        {!isAck && (
                          <button
                            onClick={() => handleAcknowledgeAlert(alert.alertId)}
                            disabled={acknowledgingId === alert.alertId}
                            className="px-3.5 py-1.5 rounded-lg bg-[#261F1A] border border-[#3E322A] hover:border-[#C88242] text-[#FBF9F5] text-xs transition-colors active:scale-95 disabled:opacity-50"
                          >
                            {acknowledgingId === alert.alertId ? 'Acknowledging...' : 'Acknowledge'}
                          </button>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          )}
        </section>

        {/* Automation & Database Integrity Section */}
        <section className="space-y-4">
          <div>
            <p className="text-xs uppercase tracking-[0.25em] text-[#C88242] font-semibold mb-1">
              AUTOMATION ENGINE
            </p>
            <h2 className="text-xl font-light tracking-wide text-[#FBF9F5]">
              Active Rules & Schema Integrity
            </h2>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Rules List */}
            <div className="lg:col-span-2 space-y-3">
              {rules.length === 0 ? (
                <div className="p-6 rounded-2xl bg-[#1B1613] border border-[#2E2520] text-center text-xs text-[#A8988B]">
                  No automation rules assigned to devices in this residence.
                </div>
              ) : (
                rules.map((rule) => (
                  <div
                    key={rule.ruleId}
                    className="p-4 rounded-xl bg-[#1B1613] border border-[#2E2520] flex items-center justify-between"
                  >
                    <div>
                      <h4 className="text-sm font-medium text-[#FBF9F5]">{rule.ruleName}</h4>
                      <p className="text-xs text-[#A8988B] mt-0.5">
                        When {rule.conditionDevice?.deviceName || 'Device'} {rule.conditionOperator} {rule.conditionValue}
                      </p>
                    </div>
                    <span className="text-[10px] uppercase font-mono px-2 py-0.5 rounded bg-[#261F1A] text-[#C88242] border border-[#3E322A]">
                      Rule #{rule.ruleId}
                    </span>
                  </div>
                ))
              )}
            </div>

            {/* Oracle Trigger Constraint Info */}
            <div className="p-5 rounded-2xl bg-[#17120F] border border-[#3E322A] space-y-3">
              <div className="flex items-center gap-2 text-xs font-semibold text-[#C88242] uppercase tracking-wider">
                <span>🛡</span>
                <span>Oracle Constraint</span>
              </div>
              <h4 className="text-sm font-medium text-[#FBF9F5]">TRG_PREVENT_DEVICE_CYCLE</h4>
              <p className="text-xs text-[#A8988B] leading-relaxed">
                Database-level trigger strictly prevents cyclical parent-device relationships. Any attempt to associate a device as an indirect or direct ancestor of itself is aborted at the transaction boundary.
              </p>
              <div className="pt-2 border-t border-[#2E2520] text-[10px] text-emerald-400 font-mono flex items-center gap-1.5">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-400"></span>
                <span>Trigger Active in Schema SMART_HOME</span>
              </div>
            </div>
          </div>
        </section>

        {/* Embedded SQL Console */}
        <section className="space-y-4">
          <div>
            <p className="text-xs uppercase tracking-[0.25em] text-[#C88242] font-semibold mb-1">
              DATABASE CONSOLE
            </p>
            <h2 className="text-xl font-light tracking-wide text-[#FBF9F5]">
              Real Backend Database Querying
            </h2>
          </div>

          <SqlConsole homeId={home.homeId} />
        </section>
      </main>

      {/* Health Report Modal */}
      {showReportModal && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-md animate-fadeIn"
          onClick={() => setShowReportModal(false)}
        >
          <div
            className="relative w-full max-w-2xl bg-[#1B1613] border border-[#3E322A] rounded-2xl shadow-2xl p-6 text-[#FBF9F5]"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center justify-between pb-4 mb-4 border-b border-[#2E2520]">
              <div className="flex items-center gap-2.5">
                <span className="text-lg">📋</span>
                <h3 className="text-sm font-medium uppercase tracking-wider text-[#FBF9F5]">
                  Oracle PL/SQL Device Health Report
                </h3>
              </div>
              <button
                onClick={() => setShowReportModal(false)}
                className="text-[#A8988B] hover:text-[#FBF9F5] text-lg font-light"
              >
                ✕
              </button>
            </div>

            <pre className="p-4 rounded-xl bg-[#0D0A08] border border-[#2E2520] font-mono text-xs text-emerald-400 overflow-x-auto max-h-[60vh] custom-scrollbar whitespace-pre-wrap leading-relaxed">
              {healthReport || 'No report output returned.'}
            </pre>

            <div className="mt-4 flex justify-end">
              <button
                onClick={() => setShowReportModal(false)}
                className="px-5 py-2 rounded-xl bg-[#261F1A] border border-[#3E322A] text-xs text-[#E6D9CD] hover:border-[#C88242] transition-colors"
              >
                Close Report
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
