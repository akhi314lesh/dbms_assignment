import { useState, useEffect, useCallback, useRef, useMemo } from 'react';
import api, { apiErrorMessage, API_BASE_URL } from '../services/api';
import {
  IterationChart, DistributionChart, ResultChart, ThroughputChart,
  SuiteLatencyChart, SpeedupChart,
} from './charts';
import { StorageSection, ScalingSection, EvaluationGrid } from './EvaluationSections';
import { fmtMs, fmtNum, fmtBytes } from './format';
import { DATASET_PRESETS, DEFAULT_SCALING_STEPS, RANGE_OPTIONS, MATRIX_ROWS, JUSTIFICATION } from '../data/content';

const SCALING_STORE = 'cmp-scaling-v1';

function loadScaling() {
  try {
    const raw = localStorage.getItem(SCALING_STORE);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

function saveScaling(value) {
  try {
    localStorage.setItem(SCALING_STORE, JSON.stringify(value));
  } catch {
    /* storage is only a convenience */
  }
}

/* ─────────────────────────────────────────────────────────────
   Oracle 21c XE vs InfluxDB 2.x — comparison dashboard (DA3 · T3)
   Backend contract (Spring Boot, /api/compare/**):
     GET  /health    GET /queries   GET /devices   GET /dataset
     POST /seed      POST /run      POST /run-all  GET /history
   All numbers on this page come from real queries executed by the
   backend against both databases. Nothing is simulated.
   ───────────────────────────────────────────────────────────── */

function ts() {
  const d = new Date();
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}:${String(d.getSeconds()).padStart(2, '0')}`;
}

const DB_NAME = { ORACLE: 'Oracle 21c', INFLUX: 'InfluxDB', TIE: 'Tie' };

function fmtMetric(name, v) {
  if (v === null || v === undefined) return '—';
  if (name === 'time') return new Date(v * 1000).toISOString().replace('T', ' ').replace('.000Z', ' UTC');
  if (name === 'count') return Math.round(v).toLocaleString();
  return fmtNum(v, 4);
}

function MatchBadge({ status }) {
  const map = {
    MATCH: ['match', '✓ Results match'],
    PARTIAL: ['partial', '≈ Partial match'],
    MISMATCH: ['mismatch', '✗ Results differ'],
    UNAVAILABLE: ['partial', '— Not comparable'],
  };
  const [cls, text] = map[status] || ['partial', status || '—'];
  return <span className={`cmp-badge ${cls}`}>{text}</span>;
}

function WinnerTag({ winner }) {
  if (!winner) return <span className="cmp-tag muted">n/a</span>;
  return <span className={`cmp-tag ${winner === 'INFLUX' ? 'influx' : winner === 'ORACLE' ? 'oracle' : 'muted'}`}>{DB_NAME[winner]}</span>;
}

function CodeBlock({ title, lang, code, tone }) {
  const [copied, setCopied] = useState(false);
  const copy = () => {
    navigator.clipboard?.writeText(code).then(() => {
      setCopied(true);
      setTimeout(() => setCopied(false), 1200);
    }).catch(() => {});
  };
  return (
    <div className={`cmp-code ${tone}`}>
      <div className="cmp-code-head">
        <span>{title}</span>
        <span className="cmp-code-lang">{lang}</span>
        <button type="button" onClick={copy}>{copied ? 'Copied' : 'Copy'}</button>
      </div>
      <pre>{code}</pre>
    </div>
  );
}

export default function ComparisonDashboard() {
  // Connectivity
  const [health, setHealth] = useState({ loading: true });
  const [backendDown, setBackendDown] = useState(false);

  // Catalogue / dataset
  const [queries, setQueries] = useState([]);
  const [devices, setDevices] = useState([]);
  const [dataset, setDataset] = useState(null);
  const [presetKey, setPresetKey] = useState('standard');
  const [seeding, setSeeding] = useState(false);
  const [seedResult, setSeedResult] = useState(null);

  // Query config
  const [queryType, setQueryType] = useState('WINDOW_AGGREGATE');
  const [deviceId, setDeviceId] = useState('');
  const [rangeHours, setRangeHours] = useState('default');
  const [threshold, setThreshold] = useState(28);
  const [writeRows, setWriteRows] = useState(5000);
  const [iterations, setIterations] = useState(5);

  // Results
  const [running, setRunning] = useState(false);
  const [result, setResult] = useState(null);
  const [suite, setSuite] = useState(null);
  const [suiteRunning, setSuiteRunning] = useState(false);
  const [history, setHistory] = useState([]);
  const [error, setError] = useState(null);

  // Storage + scalability
  const [storage, setStorage] = useState(null);
  const [measuring, setMeasuring] = useState(false);
  const [scaling, setScaling] = useState(loadScaling);
  const [scaleKeys, setScaleKeys] = useState(DEFAULT_SCALING_STEPS);
  const [scaleRunning, setScaleRunning] = useState(false);
  const [scaleProgress, setScaleProgress] = useState('');

  const [logs, setLogs] = useState([{ ts: ts(), msg: 'Comparison dashboard loaded.', type: 'info' }]);
  const logsRef = useRef(null);

  const addLog = useCallback((msg, type = 'info') => {
    setLogs((prev) => [...prev.slice(-120), { ts: ts(), msg, type }]);
  }, []);

  useEffect(() => {
    if (logsRef.current) logsRef.current.scrollTop = logsRef.current.scrollHeight;
  }, [logs]);

  const selected = useMemo(() => queries.find((q) => q.key === queryType), [queries, queryType]);
  const busy = running || seeding || suiteRunning || measuring || scaleRunning;

  // ── Loaders ──────────────────────────────────────────────────
  const loadHealth = useCallback(async () => {
    try {
      const res = await api.get('/api/compare/health');
      setHealth({ ...res.data, loading: false });
      setBackendDown(false);
      const h = res.data;
      addLog(`Health — Oracle: ${h.oracle ? 'UP' : 'DOWN'}${h.oracleVersion ? ` (${h.oracleVersion})` : ''}, InfluxDB: ${h.influx ? 'UP' : 'DOWN'}${h.influxVersion ? ` (${h.influxVersion})` : ''}`, h.oracle && h.influx ? 'ok' : 'warn');
      if (!h.oracle && h.oracleError) addLog(`Oracle: ${h.oracleError}`, 'err');
      if (!h.influx && h.influxError) addLog(`InfluxDB: ${h.influxError}`, 'err');
      return h;
    } catch (e) {
      setHealth({ oracle: false, influx: false, loading: false });
      setBackendDown(true);
      addLog(`Backend not reachable at ${API_BASE_URL}: ${apiErrorMessage(e)}`, 'err');
      return null;
    }
  }, [addLog]);

  const loadDataset = useCallback(async () => {
    try {
      const res = await api.get('/api/compare/dataset');
      setDataset(res.data);
      return res.data;
    } catch (e) {
      addLog(`Could not read dataset info: ${apiErrorMessage(e)}`, 'warn');
      return null;
    }
  }, [addLog]);

  const loadHistory = useCallback(async () => {
    try {
      const res = await api.get('/api/compare/history');
      setHistory(Array.isArray(res.data) ? res.data : []);
    } catch {
      /* history is optional */
    }
  }, []);

  const loadAll = useCallback(async () => {
    const h = await loadHealth();
    if (!h) return;
    try {
      const q = await api.get('/api/compare/queries');
      setQueries(Array.isArray(q.data) ? q.data : []);
    } catch (e) {
      addLog(`Could not load query catalogue: ${apiErrorMessage(e)}`, 'err');
    }
    if (h.oracle) {
      try {
        const d = await api.get('/api/compare/devices');
        const list = Array.isArray(d.data) ? d.data : [];
        setDevices(list);
        const firstTemp = list.find((x) => x.subtype === 'TEMPERATURE_SENSOR') || list[0];
        if (firstTemp) setDeviceId((cur) => cur || String(firstTemp.deviceId));
      } catch (e) {
        addLog(`Could not load devices: ${apiErrorMessage(e)}`, 'warn');
      }
    }
    const ds = await loadDataset();
    if (ds) {
      addLog(ds.empty
        ? 'Benchmark dataset is empty — generate it in section 01.'
        : `Dataset: Oracle ${Number(ds.oracleRows ?? 0).toLocaleString()} rows, InfluxDB ${Number(ds.influxRows ?? 0).toLocaleString()} rows${ds.inSync ? ' (in sync)' : ' (NOT in sync — regenerate)'}`,
      ds.empty || !ds.inSync ? 'warn' : 'ok');
    }
    loadHistory();
  }, [loadHealth, loadDataset, loadHistory, addLog]);

  useEffect(() => {
    // Defer the first load to a microtask so state updates happen outside the effect body.
    let cancelled = false;
    Promise.resolve().then(() => { if (!cancelled) loadAll(); });
    return () => { cancelled = true; };
  }, [loadAll]);

  // ── Actions ──────────────────────────────────────────────────
  const seed = async () => {
    const preset = DATASET_PRESETS.find((p) => p.key === presetKey);
    setSeeding(true);
    setError(null);
    addLog(`▶ Generating dataset: ${preset.label} — writing identical readings to Oracle and InfluxDB…`, 'info');
    try {
      const res = await api.post('/api/compare/seed', { days: preset.days, intervalMinutes: preset.intervalMinutes });
      const s = res.data;
      setSeedResult(s);
      setDataset(s.dataset);
      addLog(`  Oracle  wrote ${s.rowsGenerated.toLocaleString()} rows in ${fmtMs(s.oracleWriteMs)} (${s.oracleRowsPerSec.toLocaleString()} rows/s)`, 'oracle');
      addLog(`  InfluxDB wrote ${s.rowsGenerated.toLocaleString()} rows in ${fmtMs(s.influxWriteMs)} (${s.influxRowsPerSec.toLocaleString()} rows/s)`, 'influx');
      addLog(`✔ Dataset ready — ${s.dataset?.inSync ? 'both databases hold the same row count' : 'row counts differ!'}`, s.dataset?.inSync ? 'ok' : 'warn');
    } catch (e) {
      const msg = apiErrorMessage(e);
      setError(msg);
      addLog(`✖ Dataset generation failed: ${msg}`, 'err');
    } finally {
      setSeeding(false);
    }
  };

  const runOne = async () => {
    setRunning(true);
    setError(null);
    const body = { queryType, iterations };
    if (selected?.needsDevice && deviceId) body.deviceId = Number(deviceId);
    if (!selected?.write && rangeHours !== 'default') body.rangeHours = Number(rangeHours);
    if (queryType === 'THRESHOLD_ALERTS') body.threshold = Number(threshold);
    if (queryType === 'BULK_INSERT') body.writeRows = Number(writeRows);
    addLog(`▶ ${selected?.label ?? queryType} — ${iterations} timed runs per database (+1 warm-up)…`, 'info');
    try {
      const res = await api.post('/api/compare/run', body);
      const d = res.data;
      setResult(d);
      if (d.oracleError) addLog(`  Oracle error: ${d.oracleError}`, 'err');
      if (d.influxError) addLog(`  InfluxDB error: ${d.influxError}`, 'err');
      if (d.oracleMs != null) addLog(`  Oracle   avg ${fmtMs(d.oracleMs)}  (${d.oracleRowCount ?? '—'} rows)`, 'oracle');
      if (d.influxMs != null) addLog(`  InfluxDB avg ${fmtMs(d.influxMs)}  (${d.influxRowCount ?? '—'} rows)`, 'influx');
      if (d.winner) addLog(`✔ ${DB_NAME[d.winner]} faster by ${fmtMs(d.timeDifferenceMs)} (${d.speedupFactor}×) — results: ${d.resultComparison?.status}`, d.winner === 'INFLUX' ? 'influx' : 'oracle');
      loadHistory();
    } catch (e) {
      const msg = apiErrorMessage(e);
      setError(msg);
      addLog(`✖ ${msg}`, 'err');
    } finally {
      setRunning(false);
    }
  };

  const runSuite = async () => {
    setSuiteRunning(true);
    setError(null);
    const its = Math.min(iterations, 10);
    addLog(`▶ Running all ${queries.length || 8} workloads × ${its} timed runs each…`, 'info');
    try {
      const res = await api.post('/api/compare/run-all', { iterations: its, writeRows: Number(writeRows) });
      setSuite(res.data);
      (res.data.results || []).forEach((r) => {
        if (r.error) addLog(`  ${r.label}: ${r.error}`, 'err');
        else addLog(`  ${r.label}: Oracle ${fmtMs(r.oracleMs)} · InfluxDB ${fmtMs(r.influxMs)} → ${DB_NAME[r.winner] ?? 'n/a'} (${r.matchStatus})`, r.winner === 'INFLUX' ? 'influx' : 'oracle');
      });
      addLog(`✔ ${res.data.analysis}`, 'ok');
      loadHistory();
    } catch (e) {
      const msg = apiErrorMessage(e);
      setError(msg);
      addLog(`✖ ${msg}`, 'err');
    } finally {
      setSuiteRunning(false);
    }
  };

  const measureStorage = async () => {
    setMeasuring(true);
    setError(null);
    addLog('▶ Measuring storage (waiting for InfluxDB to compact its write-ahead log)…', 'info');
    try {
      const res = await api.get('/api/compare/storage', { params: { settle: true } });
      const s = res.data;
      setStorage(s);
      if (s.oracle) addLog(`  Oracle   ${fmtBytes(s.oracle.totalBytes)} (${s.oracle.bytesPerReading} B per reading)`, 'oracle');
      if (s.influx) addLog(`  InfluxDB ${fmtBytes(s.influx.totalBytes)} (${s.influx.bytesPerReading} B per reading)`, 'influx');
      if (s.oracleError) addLog(`  Oracle: ${s.oracleError}`, 'err');
      if (s.influxError) addLog(`  InfluxDB: ${s.influxError}`, 'err');
    } catch (e) {
      const msg = apiErrorMessage(e);
      setError(msg);
      addLog(`✖ ${msg}`, 'err');
    } finally {
      setMeasuring(false);
    }
  };

  const toggleScaleKey = (key) => {
    setScaleKeys((cur) => (cur.includes(key) ? cur.filter((k) => k !== key) : [...cur, key]));
  };

  const runScaling = async () => {
    const plan = DATASET_PRESETS.filter((p) => scaleKeys.includes(p.key));
    setScaleRunning(true);
    setError(null);
    const steps = [];
    addLog(`▶ Scalability test: ${plan.length} dataset sizes, 8 workloads × 3 runs each…`, 'info');
    try {
      for (let i = 0; i < plan.length; i++) {
        const p = plan.at(i);
        const tag = `${i + 1}/${plan.length} · ${p.label}`;
        setScaleProgress(`${tag}: loading both databases…`);
        const seedRes = (await api.post('/api/compare/seed', { days: p.days, intervalMinutes: p.intervalMinutes })).data;
        addLog(`  ${tag}: loaded ${seedRes.rowsGenerated.toLocaleString()} rows — Oracle ${fmtMs(seedRes.oracleWriteMs)}, InfluxDB ${fmtMs(seedRes.influxWriteMs)}`, 'info');
        setScaleProgress(`${tag}: measuring storage…`);
        const st = (await api.get('/api/compare/storage', { params: { settle: true } })).data;
        setScaleProgress(`${tag}: running 8 workloads…`);
        const ra = (await api.post('/api/compare/run-all', { iterations: 3, writeRows: Number(writeRows) })).data;
        const workloads = {};
        (ra.results || []).forEach((r) => {
          workloads[r.queryType] = { oracleMs: r.oracleMs, influxMs: r.influxMs, winner: r.winner, matchStatus: r.matchStatus };
        });
        const readResults = (ra.results || []).filter((r) => r.queryType !== 'BULK_INSERT');
        steps.push({
          key: p.key,
          label: p.label,
          rows: seedRes.rowsGenerated,
          oracleWriteMs: seedRes.oracleWriteMs,
          influxWriteMs: seedRes.influxWriteMs,
          oracleRowsPerSec: seedRes.oracleRowsPerSec,
          influxRowsPerSec: seedRes.influxRowsPerSec,
          oracleBytes: st.oracle?.totalBytes ?? null,
          influxBytes: st.influx?.totalBytes ?? null,
          workloads,
          oracleReadWins: readResults.filter((r) => r.winner === 'ORACLE').length,
          influxReadWins: readResults.filter((r) => r.winner === 'INFLUX').length,
          matched: ra.resultsMatched,
          workloadCount: ra.queries,
        });
        const value = { steps: [...steps], timestamp: new Date().toISOString() };
        setScaling(value);
        saveScaling(value);
        setSeedResult(seedRes);
        setDataset(seedRes.dataset);
        setStorage(st);
        setSuite(ra);
        addLog(`  ${tag}: ${ra.analysis}`, 'ok');
      }
      addLog('✔ Scalability test finished.', 'ok');
      loadHistory();
    } catch (e) {
      const msg = apiErrorMessage(e);
      setError(msg);
      addLog(`✖ Scalability test stopped: ${msg}`, 'err');
    } finally {
      setScaleRunning(false);
      setScaleProgress('');
    }
  };

  const clearHistory = async () => {
    try {
      await api.delete('/api/compare/history');
      setHistory([]);
    } catch (e) {
      addLog(`Could not clear history: ${apiErrorMessage(e)}`, 'warn');
    }
  };

  // ── Derived ──────────────────────────────────────────────────
  const bothUp = health.oracle && health.influx;
  const datasetEmpty = !dataset || dataset.empty;
  const canRunRead = bothUp && !datasetEmpty && !busy;
  const canRun = selected?.write ? bothUp && !busy : canRunRead;
  const winnerIsInflux = result?.winner === 'INFLUX';
  const maxMs = Math.max(result?.oracleMs || 0, result?.influxMs || 0, 0.0001);
  const lastSeed = seedResult || dataset?.lastSeed;

  const rangeLabel = (h) => {
    const opt = RANGE_OPTIONS.find((o) => o.value === h);
    return opt ? opt.label : `${h} h`;
  };

  return (
    <div className="cmp-page">
      {/* ── Hero ── */}
      <div className="cmp-hero">
        <p className="cmp-hero-eyebrow">DA3 · Oracle 21c XE · InfluxDB 2.x · Performance analysis</p>
        <h1>
          Oracle vs InfluxDB,
          <br />
          <em>measured honestly.</em>
        </h1>
        <p className="cmp-hero-sub">
          The same smart-home sensor telemetry is stored in both databases. Each workload runs the equivalent SQL and Flux
          query, times it end-to-end from the Spring Boot backend, and checks that both databases return the same answer.
        </p>

        <div className="cmp-status-strip">
          <div className="cmp-status-pill oracle" title={health.oracleError || health.oracleVersion || ''}>
            <span className={`cmp-status-dot ${health.oracle ? 'pulse' : 'down'}`} />
            Oracle {health.loading ? '…' : health.oracle ? 'Connected' : 'Offline'}
          </div>
          <div className="cmp-status-pill neutral" title={`Backend: ${API_BASE_URL}`}>
            <span className={`cmp-status-dot ${backendDown ? 'down' : 'pulse ok'}`} />
            Backend {backendDown ? 'Offline' : 'API'} · {API_BASE_URL.replace(/^https?:\/\//, '')}
          </div>
          <div className="cmp-status-pill influx" title={health.influxError || health.influxVersion || ''}>
            <span className={`cmp-status-dot ${health.influx ? 'pulse' : 'down'}`} />
            InfluxDB {health.loading ? '…' : health.influx ? 'Connected' : 'Offline'}
          </div>
          <button type="button" className="cmp-ghost-btn" onClick={loadAll} disabled={busy}>↻ Refresh</button>
        </div>
        {(health.oracleVersion || health.influxVersion) && (
          <p className="cmp-versions">
            {health.oracleVersion && <span className="o">{health.oracleVersion}</span>}
            {health.influxVersion && <span className="i">{health.influxVersion} · bucket “{health.influxBucket}”</span>}
          </p>
        )}
      </div>

      {/* ── Problems ── */}
      {backendDown && (
        <div className="cmp-alert err">
          <strong>The Spring Boot backend is not reachable at {API_BASE_URL}.</strong>
          <span>Start it with <code>cd backend</code> → <code>.\mvnw.cmd spring-boot:run</code> (Windows) or <code>./mvnw spring-boot:run</code>, then press Refresh.</span>
        </div>
      )}
      {!backendDown && !health.loading && !health.oracle && (
        <div className="cmp-alert err">
          <strong>Oracle is offline.</strong> <span>{health.oracleError}</span>
          <span>Check that Oracle XE is running on port 1521 and that DB_PASSWORD is set for the backend.</span>
        </div>
      )}
      {!backendDown && !health.loading && !health.influx && (
        <div className="cmp-alert err">
          <strong>InfluxDB is offline.</strong> <span>{health.influxError}</span>
          <span>Start it with <code>cd da3-influxdb-migration</code> → <code>docker compose up -d</code>.</span>
        </div>
      )}
      {error && (
        <div className="cmp-alert warn" role="alert">
          <strong>Request failed.</strong> <span>{error}</span>
          <button type="button" className="cmp-alert-close" onClick={() => setError(null)} aria-label="Dismiss">✕</button>
        </div>
      )}

      {/* ── 01 Dataset ── */}
      <p className="cmp-section-label">01 · Benchmark Dataset</p>
      <h2 className="cmp-section-title">Same readings, <em>both databases.</em></h2>
      <div className="cmp-config-card">
        <div className="cmp-dataset-grid">
          <div className="cmp-ds-stat oracle">
            <p className="cmp-metric-label">Oracle · {dataset?.oracleTable || 'SENSOR_BENCHMARK'}</p>
            <p className="cmp-ds-value">{dataset?.oracleRows != null ? Number(dataset.oracleRows).toLocaleString() : '—'}</p>
            <p className="cmp-metric-sub">rows</p>
          </div>
          <div className="cmp-ds-stat influx">
            <p className="cmp-metric-label">InfluxDB · {dataset?.influxMeasurement || 'sensor_benchmark'}</p>
            <p className="cmp-ds-value">{dataset?.influxRows != null ? Number(dataset.influxRows).toLocaleString() : '—'}</p>
            <p className="cmp-metric-sub">points</p>
          </div>
          <div className="cmp-ds-stat">
            <p className="cmp-metric-label">Consistency</p>
            <p className="cmp-ds-value small">
              {datasetEmpty ? <span className="cmp-badge partial">Empty</span>
                : dataset.inSync ? <span className="cmp-badge match">✓ In sync</span>
                  : <span className="cmp-badge mismatch">✗ Out of sync</span>}
            </p>
            <p className="cmp-metric-sub">{dataset?.devices ? `${dataset.devices} sensor devices` : 'same rows in both stores'}</p>
          </div>
          <div className="cmp-ds-stat">
            <p className="cmp-metric-label">Time span (UTC)</p>
            <p className="cmp-ds-value small mono">{dataset?.start ? dataset.start.replace('T', ' ').replace(':00Z', '') : '—'}</p>
            <p className="cmp-metric-sub mono">→ {dataset?.end ? dataset.end.replace('T', ' ').replace(':00Z', '') : '—'}</p>
          </div>
        </div>

        <div className="cmp-row">
          <div className="cmp-config-field" style={{ flex: '1 1 260px' }}>
            <label htmlFor="preset">Dataset size</label>
            <select id="preset" value={presetKey} onChange={(e) => setPresetKey(e.target.value)} disabled={busy}>
              {DATASET_PRESETS.map((p) => <option key={p.key} value={p.key}>{p.label}</option>)}
            </select>
          </div>
          <button type="button" className="cmp-secondary-btn" onClick={seed} disabled={!bothUp || busy}>
            {seeding ? 'Writing to both databases…' : datasetEmpty ? '⬇ Generate benchmark dataset' : '↺ Regenerate dataset'}
          </button>
        </div>
        <p className="cmp-help">
          Generates realistic readings for every temperature and motion sensor in Oracle’s <code>DEVICES</code> table
          (daily temperature cycle, noise, occasional spikes; motion on/off events) and writes the <em>identical</em> rows to
          Oracle (JDBC batch insert) and InfluxDB (Line Protocol). Approximate size:{' '}
          {(() => {
            const p = DATASET_PRESETS.find((x) => x.key === presetKey);
            const n = (devices.length || 18) * p.days * 24 * 60 / p.intervalMinutes;
            return `${Math.round(n).toLocaleString()} readings (${devices.length || 18} devices × ${p.days} day${p.days > 1 ? 's' : ''} every ${p.intervalMinutes} min)`;
          })()}.
        </p>

        {lastSeed && (
          <div className="cmp-ingest">
            <div>
              <p className="cmp-subhead">Ingestion of {Number(lastSeed.rowsGenerated).toLocaleString()} readings</p>
              <p className="cmp-help" style={{ marginTop: 4 }}>
                Oracle took <strong>{fmtMs(lastSeed.oracleWriteMs)}</strong>, InfluxDB <strong>{fmtMs(lastSeed.influxWriteMs)}</strong> —{' '}
                {DB_NAME[lastSeed.writeWinner]} loaded the data <strong>{lastSeed.writeSpeedup}×</strong> faster
                ({fmtMs(Math.abs(lastSeed.oracleWriteMs - lastSeed.influxWriteMs))} difference).
              </p>
            </div>
            <ThroughputChart oracle={lastSeed.oracleRowsPerSec} influx={lastSeed.influxRowsPerSec} />
          </div>
        )}
      </div>

      {/* ── 02 Configure ── */}
      <p className="cmp-section-label">02 · Choose a Workload</p>
      <h2 className="cmp-section-title">Choose a <em>workload.</em></h2>
      <div className="cmp-config-card">
        <div className="cmp-query-grid" role="radiogroup" aria-label="Workload">
          {queries.map((q) => (
            <button
              type="button"
              role="radio"
              aria-checked={q.key === queryType}
              key={q.key}
              className={`cmp-query-card ${q.key === queryType ? 'active' : ''}`}
              onClick={() => { setQueryType(q.key); setRangeHours('default'); }}
              disabled={busy}
            >
              <span className="cmp-query-cat">{q.category} · {q.scope}</span>
              <span className="cmp-query-label">{q.label}</span>
              <span className="cmp-query-desc">{q.description}</span>
            </button>
          ))}
          {queries.length === 0 && <p className="cmp-help">Workloads load from the backend once it is reachable.</p>}
        </div>

        <div className="cmp-config-grid">
          {selected?.needsDevice && (
            <div className="cmp-config-field">
              <label htmlFor="device">Device</label>
              <select id="device" value={deviceId} onChange={(e) => setDeviceId(e.target.value)} disabled={busy}>
                {devices.map((d) => (
                  <option key={d.deviceId} value={d.deviceId}>#{d.deviceId} · {d.deviceName}</option>
                ))}
              </select>
            </div>
          )}
          {selected && !selected.write && (
            <div className="cmp-config-field">
              <label htmlFor="range">Time window</label>
              <select id="range" value={rangeHours} onChange={(e) => setRangeHours(e.target.value)} disabled={busy}>
                <option value="default">Default — {rangeLabel(selected.defaultRangeHours)}</option>
                {RANGE_OPTIONS.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
              </select>
            </div>
          )}
          {queryType === 'THRESHOLD_ALERTS' && (
            <div className="cmp-config-field">
              <label htmlFor="thr">Threshold (value &gt;)</label>
              <input id="thr" type="number" step="0.5" value={threshold} onChange={(e) => setThreshold(e.target.value)} disabled={busy} />
            </div>
          )}
          {selected?.write && (
            <div className="cmp-config-field">
              <label htmlFor="wr">Rows per batch</label>
              <select id="wr" value={writeRows} onChange={(e) => setWriteRows(Number(e.target.value))} disabled={busy}>
                {[1000, 5000, 20000, 50000].map((n) => <option key={n} value={n}>{n.toLocaleString()} rows</option>)}
              </select>
            </div>
          )}
          <div className="cmp-config-field">
            <label htmlFor="its">Timed runs per database</label>
            <input
              id="its"
              type="number"
              min="1"
              max="20"
              value={iterations}
              onChange={(e) => setIterations(Math.max(1, Math.min(20, Number(e.target.value) || 1)))}
              disabled={busy}
            />
          </div>
        </div>

        <div className="cmp-row">
          <button type="button" className={`cmp-run-btn ${running ? 'running' : ''}`} onClick={runOne} disabled={!canRun} id="run-benchmark-btn">
            {running ? <><span className="cmp-spinner" /> Running both databases…</> : <>⚡ Run comparison</>}
          </button>
          <button type="button" className="cmp-secondary-btn" onClick={runSuite} disabled={!canRunRead}>
            {suiteRunning ? 'Running all workloads…' : '▶ Run all workloads'}
          </button>
        </div>
        {!selected?.write && datasetEmpty && bothUp && (
          <p className="cmp-help warn-text">Generate the benchmark dataset (section 01) before running read workloads.</p>
        )}
      </div>

      {/* ── 03 Race ── */}
      <p className="cmp-section-label">03 · Head-to-Head Result</p>
      <h2 className="cmp-section-title">Head-to-<em>head.</em></h2>
      <div className="cmp-race-card">
        <div style={{ display: 'flex', alignItems: 'baseline', gap: 10, flexWrap: 'wrap' }}>
          <h2>{result?.label ?? selected?.label ?? 'Select a workload'}</h2>
          {result?.resultComparison && <MatchBadge status={result.resultComparison.status} />}
        </div>
        <p className="cmp-help" style={{ marginTop: 6 }}>
          {result?.description ?? selected?.description ?? ''} Average time per query — shorter bar is faster.
        </p>

        <div className="cmp-race-track">
          {[
            { key: 'oracle', name: 'Oracle', sub: '21c XE · SQL', ms: result?.oracleMs, err: result?.oracleError },
            { key: 'influx', name: 'InfluxDB', sub: '2.x · Flux', ms: result?.influxMs, err: result?.influxError },
          ].map((s) => (
            <div className="cmp-race-row" key={s.key}>
              <div className={`cmp-race-label ${s.key}`}>{s.name}<span>{s.sub}</span></div>
              <div className="cmp-race-bar-bg">
                <div
                  className={`cmp-race-bar-fill ${s.key} ${running ? 'indeterminate' : ''}`}
                  style={{ width: running ? '100%' : s.ms != null ? `${Math.max(2, (s.ms / maxMs) * 100)}%` : '0%' }}
                />
              </div>
              <div className={`cmp-race-time ${s.ms != null ? s.key : 'pending'}`}>
                {running ? '…' : s.err ? 'error' : fmtMs(s.ms)}
              </div>
            </div>
          ))}
        </div>

        {result?.winner && (
          <div className={`cmp-winner-banner ${winnerIsInflux ? 'influx-wins' : 'oracle-wins'}`}>
            <div className="cmp-winner-left">
              <span className="cmp-winner-title">🏆 Faster for this workload</span>
              <span className={`cmp-winner-name ${winnerIsInflux ? 'influx' : 'oracle'}`}>{DB_NAME[result.winner]}</span>
              <span className="cmp-winner-speedup">
                <strong>{result.speedupFactor}×</strong> faster · <strong>{fmtMs(result.timeDifferenceMs)}</strong> less per {result.queryType === 'BULK_INSERT' ? 'batch' : 'query'} ({result.percentFaster}% less time)
              </span>
            </div>
            <div className="cmp-winner-right">{winnerIsInflux ? '⚡' : '🔷'}</div>
          </div>
        )}
        {result?.summary && <p className="cmp-summary">{result.summary}</p>}
      </div>

      {result && (
        <>
          {/* ── 04 Metrics ── */}
          <p className="cmp-section-label">04 · Key Metrics</p>
          <div className="cmp-metrics-grid">
            <div className="cmp-metric-card oracle">
              <p className="cmp-metric-label">Oracle average</p>
              <p className="cmp-metric-value oracle">{fmtMs(result.oracleMs)}</p>
              <p className="cmp-metric-sub">{result.oracleError ? result.oracleError : result.queryType === 'BULK_INSERT'
                ? `${(result.oracleRowsPerSec ?? 0).toLocaleString()} rows/s`
                : `${(result.oracleRowCount ?? 0).toLocaleString()} rows returned`}</p>
            </div>
            <div className="cmp-metric-card influx">
              <p className="cmp-metric-label">InfluxDB average</p>
              <p className="cmp-metric-value influx">{fmtMs(result.influxMs)}</p>
              <p className="cmp-metric-sub">{result.influxError ? result.influxError : result.queryType === 'BULK_INSERT'
                ? `${(result.influxRowsPerSec ?? 0).toLocaleString()} rows/s`
                : `${(result.influxRowCount ?? 0).toLocaleString()} rows returned`}</p>
            </div>
            <div className="cmp-metric-card neutral">
              <p className="cmp-metric-label">Time difference</p>
              <p className="cmp-metric-value neutral">{fmtMs(result.timeDifferenceMs)}</p>
              <p className="cmp-metric-sub">{result.winner ? `${DB_NAME[result.winner]} faster · ${result.speedupFactor}×` : '—'}</p>
            </div>
            <div className="cmp-metric-card neutral">
              <p className="cmp-metric-label">Dataset</p>
              <p className="cmp-metric-value">{Number(result.datasetSize ?? 0).toLocaleString()}</p>
              <p className="cmp-metric-sub">readings · {result.iterations} timed runs each</p>
            </div>
          </div>

          {/* ── 05 Charts ── */}
          <p className="cmp-section-label">05 · Charts</p>
          <h2 className="cmp-section-title">What the numbers <em>look like.</em></h2>
          <div className="cmp-chart-grid">
            <div className="cmp-chart-card">
              <h3>Latency of every run</h3>
              <p className="cmp-help">Each point is one timed execution (ms, lower is better). A flat line means stable performance; spikes show caching or garbage-collection effects.</p>
              <IterationChart oracle={result.oracle?.samplesMs} influx={result.influx?.samplesMs} />
            </div>
            <div className="cmp-chart-card">
              <h3>Latency distribution</h3>
              <p className="cmp-help">Best, typical (median), average, 95th-percentile and worst time for each database.</p>
              <DistributionChart oracle={result.oracle} influx={result.influx} />
            </div>
            <div className="cmp-chart-card wide">
              <h3>{result.chart?.title || 'Query result'}</h3>
              <p className="cmp-help">
                {result.queryType === 'BULK_INSERT'
                  ? 'Rows written per second (higher is better).'
                  : 'The data each database returned, plotted on top of each other. When the results match, the InfluxDB marks sit exactly on the Oracle marks.'}
              </p>
              <ResultChart chart={result.chart} />
            </div>
          </div>

          {/* ── 06 Explanation ── */}
          <p className="cmp-section-label">06 · Why This Result</p>
          <h2 className="cmp-section-title">Why this <em>result.</em></h2>
          <div className="cmp-justify-card">
            <p className="cmp-explain">{result.explanation}</p>
            <div className="cmp-stats-table-wrap">
              <table className="cmp-matrix-table">
                <thead>
                  <tr><th>Statistic</th><th>Oracle 21c</th><th>InfluxDB</th><th>Difference</th></tr>
                </thead>
                <tbody>
                  {[['avgMs', 'Average'], ['medianMs', 'Median'], ['minMs', 'Fastest run'], ['maxMs', 'Slowest run'], ['p95Ms', '95th percentile'], ['stdDevMs', 'Std. deviation']].map(([k, label]) => {
                    const o = result.oracle?.[k];
                    const f = result.influx?.[k];
                    return (
                      <tr key={k}>
                        <td>{label}</td>
                        <td className="mono">{fmtMs(o)}</td>
                        <td className="mono">{fmtMs(f)}</td>
                        <td className="mono">{o != null && f != null ? `${o > f ? '+' : o < f ? '−' : ''}${fmtMs(Math.abs(o - f))} ${o > f ? '(Oracle slower)' : o < f ? '(Oracle faster)' : ''}` : '—'}</td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>

          {/* ── 07 Verification ── */}
          <p className="cmp-section-label">07 · Result Verification</p>
          <h2 className="cmp-section-title">Same question, <em>same answer?</em></h2>
          <div className="cmp-config-card">
            <div className="cmp-verify-head">
              <MatchBadge status={result.resultComparison?.status} />
              <span>{result.resultComparison?.message}</span>
            </div>
            {result.resultComparison?.totalKeys != null && (
              <p className="cmp-help">
                Compared {result.resultComparison.totalKeys} result rows · {result.resultComparison.matched} identical ·{' '}
                {result.resultComparison.mismatched} different values · {result.resultComparison.oracleOnly} only in Oracle ·{' '}
                {result.resultComparison.influxOnly} only in InfluxDB · largest numeric difference {result.resultComparison.maxAbsDifference}
              </p>
            )}
            {result.resultComparison?.sample?.length > 0 && (
              <div className="cmp-stats-table-wrap">
                <table className="cmp-matrix-table">
                  <thead>
                    <tr><th>Key</th><th>Oracle returned</th><th>InfluxDB returned</th><th>Same?</th></tr>
                  </thead>
                  <tbody>
                    {result.resultComparison.sample.map((row) => (
                      <tr key={row.key}>
                        <td className="mono">{row.key}</td>
                        <td className="mono">{row.oracle ? Object.entries(row.oracle).map(([k, v]) => `${k}=${fmtMetric(k, v)}`).join('  ') : '—'}</td>
                        <td className="mono">{row.influx ? Object.entries(row.influx).map(([k, v]) => `${k}=${fmtMetric(k, v)}`).join('  ') : '—'}</td>
                        <td>{row.match ? <span className="cmp-check">✓</span> : <span className="cmp-cross">✗</span>}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>

          {/* ── 08 Queries ── */}
          <p className="cmp-section-label">08 · Queries Executed</p>
          <h2 className="cmp-section-title">SQL and Flux, <em>side by side.</em></h2>
          {result.params && (
            <p className="cmp-help" style={{ marginTop: -8, marginBottom: 12 }}>
              {result.params.deviceName && <>Scope: <strong>{result.params.deviceName}</strong>. </>}
              {result.params.start && <>Window: <span className="mono">{result.params.start}</span> → <span className="mono">{result.params.stop}</span> ({result.params.rangeHours} h, half-open [start, stop)). </>}
              {result.params.rowsPerBatch && <>Batch: {result.params.rowsPerBatch.toLocaleString()} rows across {result.params.devices} devices. </>}
            </p>
          )}
          <div className="cmp-chart-grid">
            <CodeBlock title="Oracle 21c" lang="SQL" code={result.oracleQuery || ''} tone="oracle" />
            <CodeBlock title="InfluxDB 2.x" lang={result.queryType === 'BULK_INSERT' ? 'Line Protocol' : 'Flux'} code={result.fluxQuery || ''} tone="influx" />
          </div>
        </>
      )}

      {/* ── 09 Suite ── */}
      {suite && (
        <>
          <p className="cmp-section-label">09 · All Workloads</p>
          <h2 className="cmp-section-title">Every workload, <em>one view.</em></h2>
          <div className="cmp-config-card">
            <div className="cmp-suite-head">
              <div className="cmp-suite-stat"><span className="o">{suite.oracleWins}</span><small>Oracle wins</small></div>
              <div className="cmp-suite-stat"><span className="i">{suite.influxWins}</span><small>InfluxDB wins</small></div>
              <div className="cmp-suite-stat"><span>{suite.resultsMatched}/{suite.queries}</span><small>results identical</small></div>
              <p className="cmp-summary" style={{ margin: 0, flex: '1 1 320px' }}>{suite.analysis}</p>
            </div>
            <div className="cmp-chart-grid">
              <div className="cmp-chart-card">
                <h3>Average latency per workload</h3>
                <p className="cmp-help">Milliseconds per query/batch, lower is better.</p>
                <SuiteLatencyChart results={suite.results} />
              </div>
              <div className="cmp-chart-card">
                <h3>Who was faster, and by how much</h3>
                <p className="cmp-help">Bars to the left: Oracle faster. Bars to the right: InfluxDB faster. Length = speed-up factor.</p>
                <SpeedupChart results={suite.results} />
              </div>
            </div>
            <div className="cmp-stats-table-wrap">
              <table className="cmp-matrix-table">
                <thead>
                  <tr><th>Workload</th><th>Oracle</th><th>InfluxDB</th><th>Difference</th><th>Faster</th><th>Results</th></tr>
                </thead>
                <tbody>
                  {suite.results.map((r) => (
                    <tr key={r.queryType}>
                      <td>{r.label}</td>
                      {r.error ? <td colSpan={5} className="err-text">{r.error}</td> : (
                        <>
                          <td className="mono">{fmtMs(r.oracleMs)}</td>
                          <td className="mono">{fmtMs(r.influxMs)}</td>
                          <td className="mono">{fmtMs(r.timeDifferenceMs)} · {r.speedupFactor ?? '—'}×</td>
                          <td><WinnerTag winner={r.winner} /></td>
                          <td><MatchBadge status={r.matchStatus} /></td>
                        </>
                      )}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}

      {/* ── 10 History ── */}
      <p className="cmp-section-label">10 · Run History</p>
      <div className="cmp-stats-table-wrap" style={{ marginBottom: 32 }}>
        {history.length === 0 ? (
          <p className="cmp-help">No runs yet. Results of every comparison appear here.</p>
        ) : (
          <>
            <table className="cmp-matrix-table">
              <thead>
                <tr><th>Time</th><th>Workload</th><th>Oracle</th><th>InfluxDB</th><th>Difference</th><th>Faster</th><th>Results</th></tr>
              </thead>
              <tbody>
                {history.slice(0, 25).map((h) => (
                  <tr key={h.id}>
                    <td className="mono">{h.timestamp ? new Date(h.timestamp).toLocaleTimeString() : ''}</td>
                    <td>{h.label}<span className="cmp-dim"> · {Number(h.datasetSize ?? 0).toLocaleString()} rows · {h.iterations}×</span></td>
                    <td className="mono">{fmtMs(h.oracleMs)}</td>
                    <td className="mono">{fmtMs(h.influxMs)}</td>
                    <td className="mono">{h.speedupFactor != null ? `${fmtMs(h.timeDifferenceMs)} · ${h.speedupFactor}×` : '—'}</td>
                    <td><WinnerTag winner={h.winner} /></td>
                    <td><MatchBadge status={h.matchStatus} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
            <button type="button" className="cmp-ghost-btn" style={{ marginTop: 10 }} onClick={clearHistory} disabled={busy}>Clear history</button>
          </>
        )}
      </div>

      {/* ── 11 Log ── */}
      <p className="cmp-section-label">11 · Execution Log</p>
      <div className="cmp-terminal" ref={logsRef}>
        {logs.map((l, i) => (
          <div key={i} className="cmp-terminal-line">
            <span className="cmp-terminal-ts">[{l.ts}]</span>
            <span className={`cmp-terminal-msg ${l.type}`}>{l.msg}</span>
          </div>
        ))}
        {busy && (
          <div className="cmp-terminal-line">
            <span className="cmp-terminal-ts">[{ts()}]</span>
            <span className="cmp-terminal-msg warn">_ ▌</span>
          </div>
        )}
      </div>

      {/* ── 12 Matrix ── */}
      <p className="cmp-section-label">12 · Capability Matrix</p>
      <h2 className="cmp-section-title">Beyond <em>speed.</em></h2>
      <div className="cmp-stats-table-wrap" style={{ marginBottom: 32 }}>
        <table className="cmp-matrix-table">
          <thead>
            <tr><th>Dimension</th><th>Oracle 21c XE</th><th>InfluxDB</th><th>Notes</th></tr>
          </thead>
          <tbody>
            {MATRIX_ROWS.map((row) => (
              <tr key={row.dimension}>
                <td style={{ fontWeight: 500 }}>{row.dimension}</td>
                <td><span className={row.oracle === '✓' ? 'cmp-check' : row.oracle === '✗' ? 'cmp-cross' : 'cmp-partial'}>{row.oracle}</span></td>
                <td><span className={row.influx === '✓' ? 'cmp-check' : row.influx === '✗' ? 'cmp-cross' : 'cmp-partial'}>{row.influx}</span></td>
                <td style={{ fontSize: 12, color: 'var(--ink-2)' }}>
                  <span className="note-o">Oracle:</span> {row.oracleNote} · <span className="note-i">InfluxDB:</span> {row.influxNote}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        <p className="cmp-help" style={{ marginTop: 8 }}>✓ strong · ~ possible with extra work · ✗ not supported</p>
      </div>

      {/* ── 13 Storage · 14 Scalability · 15 The 12 parameters ── */}
      <StorageSection storage={storage} measuring={measuring} onMeasure={measureStorage} canMeasure={bothUp && !datasetEmpty && !busy} />
      <ScalingSection
        presets={DATASET_PRESETS}
        selectedKeys={scaleKeys}
        onToggle={toggleScaleKey}
        onRun={runScaling}
        running={scaleRunning}
        progress={scaleProgress}
        scaling={scaling}
        canRun={bothUp && !busy}
        devicesCount={devices.length}
      />
      <EvaluationGrid suite={suite} lastSeed={lastSeed} storage={storage} scaling={scaling} />

      {/* ── 16 Conclusion (full-width dark band, like the landing page) ── */}
      <section className="cmp-band">
        <div className="cmp-band-inner">
          <p className="cmp-eyebrow">16 · Conclusion</p>
          <h2>
            Which database,
            <br />
            <em>for which job?</em>
          </h2>
          <p className="cmp-band-lead">
            {suite
              ? `${suite.analysis} `
              : 'Run “Run all workloads” to fill this conclusion with your own measurements. '}
            {storage?.oracle && storage?.influx
              ? `Storing the same ${Number(storage.rows).toLocaleString()} readings took ${fmtBytes(storage.oracle.totalBytes)} in Oracle and ${fmtBytes(storage.influx.totalBytes)} in InfluxDB. `
              : ''}
            Oracle is the relational system of record for the smart home; InfluxDB is purpose-built for the append-only,
            time-ordered sensor stream.
          </p>
          <div className="cmp-justify-grid">
            {JUSTIFICATION.map((j) => (
              <div className="cmp-justify-item" key={j.title}>
                <p className="cmp-justify-item-title">{j.title}</p>
                <p className="cmp-justify-item-body">{j.body}</p>
              </div>
            ))}
          </div>
        </div>
      </section>
    </div>
  );
}
