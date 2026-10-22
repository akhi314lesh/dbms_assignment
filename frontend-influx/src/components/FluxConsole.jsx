import React, { useState } from 'react';
import api from '../services/api';

const FLUX_TEMPLATES = [
  {
    name: 'Recent 20 Readings',
    desc: 'Fetch the latest 20 telemetry records',
    flux: `from(bucket: "sensor_telemetry")
  |> range(start: -24h)
  |> filter(fn: (r) => r._measurement == "sensor_reading")
  |> limit(n: 20)`,
  },
  {
    name: '5-Minute Average Window',
    desc: 'Downsample raw time-series into 5m mean aggregates',
    flux: `from(bucket: "sensor_telemetry")
  |> range(start: -6h)
  |> filter(fn: (r) => r._measurement == "sensor_reading" and r._field == "temperature")
  |> aggregateWindow(every: 5m, fn: mean, createEmpty: false)
  |> yield(name: "mean_temp")`,
  },
  {
    name: 'Max Spikes (> 30°C)',
    desc: 'Filter high temperature threshold breaches',
    flux: `from(bucket: "sensor_telemetry")
  |> range(start: -7d)
  |> filter(fn: (r) => r._measurement == "sensor_reading" and r._field == "temperature")
  |> filter(fn: (r) => r._value > 30.0)
  |> limit(n: 50)`,
  },
  {
    name: 'Device Min/Max Summary',
    desc: 'Group by device_id and calculate extremes',
    flux: `from(bucket: "sensor_telemetry")
  |> range(start: -24h)
  |> filter(fn: (r) => r._measurement == "sensor_reading")
  |> group(columns: ["device_id", "_field"])
  |> mean()`,
  },
  {
    name: 'Total Data Points Count',
    desc: 'Count total telemetry records in time range',
    flux: `from(bucket: "sensor_telemetry")
  |> range(start: -30d)
  |> filter(fn: (r) => r._measurement == "sensor_reading")
  |> count()`,
  },
];

export default function FluxConsole() {
  const [activeTab, setActiveTab] = useState('QUERY'); // 'QUERY' | 'WRITE'
  const [fluxQuery, setFluxQuery] = useState(FLUX_TEMPLATES[0].flux);
  const [lineProtocol, setLineProtocol] = useState(`sensor_reading,device_id=4,room=LivingRoom,type=TEMPERATURE_SENSOR temperature=23.4,humidity=46.2`);
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);
  const [latency, setLatency] = useState(null);

  const executeFlux = async () => {
    setLoading(true);
    setError(null);
    setResult(null);
    const start = performance.now();
    try {
      const res = await api.post('/api/influx/query', { flux: fluxQuery });
      setLatency(Math.round(performance.now() - start));
      setResult(res.data);
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Flux execution failed');
      setLatency(Math.round(performance.now() - start));
    } finally {
      setLoading(false);
    }
  };

  const executeWrite = async () => {
    setLoading(true);
    setError(null);
    setResult(null);
    const start = performance.now();
    try {
      const res = await api.post('/api/influx/write', lineProtocol, {
        headers: { 'Content-Type': 'text/plain' },
      });
      setLatency(Math.round(performance.now() - start));
      setResult(res.data || { status: 'success', message: 'Point ingested into InfluxDB bucket sensor_telemetry' });
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Write failed');
      setLatency(Math.round(performance.now() - start));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="rounded-2xl border border-cyan-500/30 bg-[#09101d] p-6 shadow-2xl text-slate-200 my-8">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between pb-5 border-b border-cyan-500/20 gap-4">
        <div>
          <div className="flex items-center gap-2.5">
            <span className="px-2.5 py-0.5 rounded text-[10px] font-bold tracking-wider uppercase bg-cyan-500/20 text-cyan-300 border border-cyan-500/40">
              ⚡ InfluxDB 2.x Flux Engine
            </span>
            <span className="text-xs text-slate-400 font-mono">Bucket: sensor_telemetry (:8086)</span>
          </div>
          <h3 className="text-xl font-light text-slate-100 mt-1 font-serif">
            Time-Series Query & Line Protocol Terminal
          </h3>
        </div>

        {/* Tab switcher */}
        <div className="flex items-center gap-1 p-1 bg-slate-900 rounded-lg border border-slate-800">
          <button
            onClick={() => setActiveTab('QUERY')}
            className={`px-4 py-1.5 rounded-md text-xs font-medium transition-all ${
              activeTab === 'QUERY'
                ? 'bg-cyan-500 text-slate-950 font-bold shadow-lg shadow-cyan-500/20'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            Flux Query
          </button>
          <button
            onClick={() => setActiveTab('WRITE')}
            className={`px-4 py-1.5 rounded-md text-xs font-medium transition-all ${
              activeTab === 'WRITE'
                ? 'bg-cyan-500 text-slate-950 font-bold shadow-lg shadow-cyan-500/20'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            Line Protocol Ingest
          </button>
        </div>
      </div>

      {/* Query Tab */}
      {activeTab === 'QUERY' && (
        <div className="mt-5 space-y-4">
          {/* Preset Buttons */}
          <div className="flex flex-wrap gap-2">
            {FLUX_TEMPLATES.map((tmpl) => (
              <button
                key={tmpl.name}
                onClick={() => setFluxQuery(tmpl.flux)}
                className="px-3 py-1.5 text-xs rounded-lg border border-slate-800 bg-slate-900/80 hover:border-cyan-500/50 hover:bg-slate-800 text-slate-300 transition-all text-left"
                title={tmpl.desc}
              >
                {tmpl.name}
              </button>
            ))}
          </div>

          {/* Editor */}
          <div>
            <textarea
              value={fluxQuery}
              onChange={(e) => setFluxQuery(e.target.value)}
              rows={6}
              className="w-full rounded-xl bg-slate-950 border border-slate-800 p-4 font-mono text-xs text-cyan-300 focus:outline-none focus:border-cyan-400 leading-relaxed shadow-inner"
              placeholder="Enter Flux query..."
            />
          </div>

          {/* Action Row */}
          <div className="flex items-center justify-between">
            <span className="text-[11px] text-slate-500 font-mono">
              Flux v2.0 • Zero table locks • Vectorized columnar processing
            </span>
            <button
              onClick={executeFlux}
              disabled={loading}
              className="px-6 py-2.5 rounded-xl bg-gradient-to-r from-cyan-500 to-blue-600 text-slate-950 font-bold text-xs uppercase tracking-wider hover:opacity-90 active:scale-98 transition-all flex items-center gap-2 shadow-lg shadow-cyan-500/25 disabled:opacity-50"
            >
              {loading ? (
                <>
                  <span className="w-3.5 h-3.5 border-2 border-slate-950 border-t-transparent rounded-full animate-spin" />
                  Executing Flux...
                </>
              ) : (
                '▶ Run Flux Query'
              )}
            </button>
          </div>
        </div>
      )}

      {/* Write Tab */}
      {activeTab === 'WRITE' && (
        <div className="mt-5 space-y-4">
          <p className="text-xs text-slate-400">
            Push real-time time-series measurement points directly to InfluxDB using Influx Line Protocol syntax:
            <code className="text-cyan-300 ml-1 font-mono">measurement,tag1=val,tag2=val field1=val,field2=val</code>
          </p>

          <textarea
            value={lineProtocol}
            onChange={(e) => setLineProtocol(e.target.value)}
            rows={4}
            className="w-full rounded-xl bg-slate-950 border border-slate-800 p-4 font-mono text-xs text-emerald-400 focus:outline-none focus:border-emerald-400 leading-relaxed shadow-inner"
            placeholder="sensor_reading,device_id=1,room=Kitchen temperature=22.5"
          />

          <div className="flex items-center justify-end">
            <button
              onClick={executeWrite}
              disabled={loading}
              className="px-6 py-2.5 rounded-xl bg-gradient-to-r from-emerald-500 to-teal-600 text-slate-950 font-bold text-xs uppercase tracking-wider hover:opacity-90 active:scale-98 transition-all flex items-center gap-2 shadow-lg shadow-emerald-500/25 disabled:opacity-50"
            >
              {loading ? 'Ingesting Point...' : '⚡ Ingest Line Protocol'}
            </button>
          </div>
        </div>
      )}

      {/* Latency & Status Pill */}
      {latency !== null && (
        <div className="mt-4 flex items-center gap-3 text-xs">
          <span className="px-2.5 py-1 rounded bg-slate-900 border border-slate-800 text-slate-400">
            Execution Latency: <strong className="text-cyan-400 font-mono">{latency} ms</strong>
          </span>
          {result && (
            <span className="text-emerald-400 flex items-center gap-1">
              ✓ InfluxDB Query Processed Successfully
            </span>
          )}
        </div>
      )}

      {/* Error display */}
      {error && (
        <div className="mt-4 p-3.5 rounded-xl bg-rose-950/60 border border-rose-800 text-rose-300 text-xs font-mono">
          ✕ Error: {error}
        </div>
      )}

      {/* Result Display */}
      {result && (
        <div className="mt-4">
          <div className="flex items-center justify-between text-[11px] text-slate-400 uppercase tracking-wider mb-2 font-mono">
            <span>Result Output</span>
            <span>
              {result.records ? `${result.records.length} records` : result.points ? `${result.points.length} points` : 'Raw Response'}
            </span>
          </div>
          <pre className="max-h-72 overflow-auto p-4 rounded-xl bg-slate-950 border border-slate-800/80 text-[11px] font-mono text-slate-300 leading-relaxed shadow-inner">
            {JSON.stringify(result, null, 2)}
          </pre>
        </div>
      )}
    </div>
  );
}
