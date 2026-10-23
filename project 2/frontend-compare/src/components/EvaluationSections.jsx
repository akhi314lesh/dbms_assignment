import { PairBars, SizeLines, StorageStack, CorrectnessBars, SuiteLatencyChart } from './charts';
import { fmtMs, fmtNum, fmtBytes, fmtRows } from './format';

/* ─────────────────────────────────────────────────────────────
   Sections 13–15: storage efficiency, scalability test and the
   12 evaluation parameters. Every verdict below is computed from
   the measurements on this page; nothing is assumed or simulated.
   ───────────────────────────────────────────────────────────── */

const READ_KEYS = ['RAW_RANGE', 'WINDOW_AGGREGATE', 'DEVICE_SUMMARY', 'LATEST_PER_DEVICE', 'THRESHOLD_ALERTS', 'DAILY_DOWNSAMPLE', 'GLOBAL_AGGREGATE'];
const AGG_KEYS = ['WINDOW_AGGREGATE', 'DEVICE_SUMMARY', 'DAILY_DOWNSAMPLE', 'GLOBAL_AGGREGATE'];
const NAME = { ORACLE: 'Oracle', INFLUX: 'InfluxDB', TIE: 'Tie', BOTH: 'Both' };

/** Winner of one measure; lowerIsBetter for times and sizes, higher for throughput. */
function winnerOf(o, f, lowerIsBetter = true) {
  if (o == null || f == null) return null;
  if (o === f) return 'TIE';
  return (lowerIsBetter ? o < f : o > f) ? 'ORACLE' : 'INFLUX';
}

function ratio(o, f) {
  if (o == null || f == null) return null;
  return Math.max(o, f) / Math.max(Math.min(o, f), 1e-9);
}

const byKey = (results, key) => results?.find((r) => r.queryType === key);

/** One measured step of the scalability test → per-workload lookup. */
function stepWorkload(step, key) {
  return step.workloads?.[key];
}

function sumReads(step, side) {
  let total = 0;
  for (const k of READ_KEYS) {
    const w = stepWorkload(step, k);
    const v = w?.[side];
    if (v == null) return null;
    total += v;
  }
  return total;
}

// ── Section 13 · Storage ─────────────────────────────────────────

export function StorageSection({ storage, measuring, onMeasure, canMeasure }) {
  const o = storage?.oracle;
  const f = storage?.influx;
  const w = winnerOf(o?.totalBytes, f?.totalBytes);
  return (
    <>
      <p className="cmp-section-label">13 · Storage Efficiency</p>
      <h2 className="cmp-section-title">Same readings, <em>how much disk?</em></h2>
      <div className="cmp-config-card">
        <div className="cmp-dataset-grid">
          <div className="cmp-ds-stat oracle">
            <p className="cmp-metric-label">Oracle · table + indexes</p>
            <p className="cmp-ds-value">{fmtBytes(o?.totalBytes)}</p>
            <p className="cmp-metric-sub">{o?.bytesPerReading != null ? `${fmtNum(o.bytesPerReading, 1)} bytes per reading` : 'not measured'}</p>
          </div>
          <div className="cmp-ds-stat influx">
            <p className="cmp-metric-label">InfluxDB · data + index + WAL</p>
            <p className="cmp-ds-value">{fmtBytes(f?.totalBytes)}</p>
            <p className="cmp-metric-sub">{f?.bytesPerReading != null ? `${fmtNum(f.bytesPerReading, 1)} bytes per reading` : 'not measured'}</p>
          </div>
          <div className="cmp-ds-stat">
            <p className="cmp-metric-label">Smaller footprint</p>
            <p className="cmp-ds-value small">{w ? `${NAME[w]} · ${fmtNum(ratio(o.totalBytes, f.totalBytes), 1)}×` : '—'}</p>
            <p className="cmp-metric-sub">{storage ? `${Number(storage.rows).toLocaleString()} readings` : 'press Measure'}</p>
          </div>
          <div className="cmp-ds-stat">
            <p className="cmp-metric-label">Compressed data only</p>
            <p className="cmp-ds-value small">{f?.tsmBytes != null && f?.points ? `${fmtNum(f.tsmBytes / f.points, 1)} B` : '—'}</p>
            <p className="cmp-metric-sub">InfluxDB TSM bytes per reading</p>
          </div>
        </div>
        <div className="cmp-row">
          <button type="button" className="cmp-secondary-btn" onClick={onMeasure} disabled={!canMeasure}>
            {measuring ? 'Waiting for InfluxDB compaction…' : '▣ Measure storage'}
          </button>
        </div>
        {storage?.oracleError && <p className="cmp-help err-text">Oracle: {storage.oracleError}</p>}
        {storage?.influxError && <p className="cmp-help err-text">InfluxDB: {storage.influxError}</p>}
        {f && !f.indexMeasured && (
          <p className="cmp-help warn-text">InfluxDB index size could not be read (Docker not reachable from the backend); only TSM data and WAL are counted.</p>
        )}
        {storage && (
          <div className="cmp-chart-card" style={{ marginTop: 20 }}>
            <h3>Where the bytes go</h3>
            <p className="cmp-help">Disk space by storage structure for the current benchmark dataset.</p>
            <StorageStack storage={storage} />
          </div>
        )}
        <p className="cmp-help">
          Oracle: allocated extents of <code>SENSOR_BENCHMARK</code> and its two indexes (<code>USER_SEGMENTS</code>).
          InfluxDB: disk blocks used by the bucket (compressed TSM files, TSI/series index and write-ahead log), measured after
          InfluxDB has flushed its write-ahead log into compressed TSM files. The InfluxDB figure also includes the 120-point
          <code> sensor_reading</code> sample, which is negligible.
        </p>
      </div>
    </>
  );
}

// ── Section 14 · Scalability ─────────────────────────────────────

export function ScalingSection({ presets, selectedKeys, onToggle, onRun, running, progress, scaling, canRun, devicesCount }) {
  const steps = scaling?.steps || [];
  const throughput = steps.map((s) => ({ rows: s.rows, oracle: s.oracleRowsPerSec, influx: s.influxRowsPerSec }));
  const storageBytes = steps.map((s) => ({ rows: s.rows, oracle: s.oracleBytes, influx: s.influxBytes }));
  const readTotal = steps.map((s) => ({ rows: s.rows, oracle: sumReads(s, 'oracleMs'), influx: sumReads(s, 'influxMs') }));
  const totalRows = presets.filter((p) => selectedKeys.includes(p.key))
    .reduce((n, p) => n + (devicesCount || 18) * p.days * 24 * 60 / p.intervalMinutes, 0);
  return (
    <>
      <p className="cmp-section-label">14 · Scalability Test</p>
      <h2 className="cmp-section-title">Grow the data, <em>watch the curves.</em></h2>
      <div className="cmp-config-card">
        <p className="cmp-help" style={{ marginTop: 0 }}>
          For each size the backend regenerates the dataset in both databases (timing the load), measures storage after
          compaction, then runs all 8 workloads 3 times each. Larger steps take minutes; the last size stays loaded.
        </p>
        <div className="cmp-scale-options">
          {presets.map((p) => (
            <label key={p.key} className="cmp-scale-option">
              <input type="checkbox" checked={selectedKeys.includes(p.key)} onChange={() => onToggle(p.key)} disabled={running} />
              <span>{p.label}</span>
              <span className="cmp-dim">{fmtRows((devicesCount || 18) * p.days * 24 * 60 / p.intervalMinutes)} readings</span>
            </label>
          ))}
        </div>
        <div className="cmp-row">
          <button type="button" className="cmp-secondary-btn" onClick={onRun} disabled={!canRun || selectedKeys.length === 0}>
            {running ? 'Scalability test running…' : `▶ Run scalability test (${fmtRows(totalRows)} readings in total)`}
          </button>
          {progress && <span className="cmp-help" style={{ margin: 0 }}>{progress}</span>}
        </div>
        {steps.length > 0 && (
          <>
            <div className="cmp-chart-grid" style={{ marginTop: 20, marginBottom: 0 }}>
              <div className="cmp-chart-card">
                <h3>Ingestion throughput</h3>
                <p className="cmp-help">Rows per second while loading each dataset (higher is better).</p>
                <SizeLines data={throughput} axisLabel="rows / s" valueFormatter={(v) => fmtRows(Math.round(v))} />
              </div>
              <div className="cmp-chart-card">
                <h3>Storage used</h3>
                <p className="cmp-help">Disk space for the same readings (lower is better, log scale).</p>
                <SizeLines data={storageBytes} axisLabel="bytes" valueFormatter={fmtBytes} logY />
              </div>
              <div className="cmp-chart-card wide">
                <h3>Total read time, all 7 read workloads</h3>
                <p className="cmp-help">Sum of the average latency of every read workload (lower is better, log scale).</p>
                <SizeLines data={readTotal} axisLabel="ms" valueFormatter={fmtMs} logY />
              </div>
            </div>
            <div className="cmp-stats-table-wrap" style={{ marginTop: 16 }}>
              <table className="cmp-matrix-table">
                <thead>
                  <tr><th>Readings</th><th>Load Oracle</th><th>Load InfluxDB</th><th>Storage Oracle</th><th>Storage InfluxDB</th><th>Reads Oracle</th><th>Reads InfluxDB</th><th>Read wins O / I</th><th>Results</th></tr>
                </thead>
                <tbody>
                  {steps.map((s) => (
                    <tr key={s.key}>
                      <td className="mono">{Number(s.rows).toLocaleString()}</td>
                      <td className="mono">{fmtMs(s.oracleWriteMs)}</td>
                      <td className="mono">{fmtMs(s.influxWriteMs)}</td>
                      <td className="mono">{fmtBytes(s.oracleBytes)}</td>
                      <td className="mono">{fmtBytes(s.influxBytes)}</td>
                      <td className="mono">{fmtMs(sumReads(s, 'oracleMs'))}</td>
                      <td className="mono">{fmtMs(sumReads(s, 'influxMs'))}</td>
                      <td className="mono">{s.oracleReadWins} / {s.influxReadWins}</td>
                      <td className="mono">{s.matched}/{s.workloadCount} match</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </>
        )}
      </div>
    </>
  );
}

// ── Section 15 · The 12 evaluation parameters ────────────────────

function ParamCard({ n, title, question, winner, verdict, children }) {
  const cls = winner === 'INFLUX' ? 'influx' : winner === 'ORACLE' ? 'oracle' : 'muted';
  return (
    <div className="cmp-chart-card cmp-param-card">
      <p className="cmp-param-num">{String(n).padStart(2, '0')}</p>
      <h3>{title}</h3>
      <p className="cmp-help">{question}</p>
      <p className="cmp-param-verdict">
        <span className={`cmp-tag ${cls}`}>{winner ? NAME[winner] ?? winner : 'Not measured yet'}</span>
        <span>{verdict}</span>
      </p>
      {children}
    </div>
  );
}

const lineCount = (s) => (s ? s.split('\n').filter((l) => l.trim() && !l.trim().startsWith('--')).length : null);
const charCount = (s) => (s ? s.split('\n').filter((l) => !l.trim().startsWith('--')).join('\n').replace(/\s+/g, ' ').trim().length : null);

export function EvaluationGrid({ suite, lastSeed, storage, scaling }) {
  const results = suite?.results || [];
  const rows = results[0]?.datasetSize;
  const at = rows ? ` at ${fmtRows(rows)} readings` : '';
  const steps = scaling?.steps || [];
  const hasScale = steps.length > 1;
  const reads = READ_KEYS.map((k) => byKey(results, k)).filter(Boolean);

  // 1 · query time
  const readWins = { ORACLE: 0, INFLUX: 0 };
  reads.forEach((r) => { if (readWins[r.winner] != null) readWins[r.winner]++; });
  const w1 = reads.length ? (readWins.INFLUX > readWins.ORACLE ? 'INFLUX' : readWins.ORACLE > readWins.INFLUX ? 'ORACLE' : 'TIE') : null;

  // 2 · insertion
  const bulk = byKey(results, 'BULK_INSERT');
  const insertBars = [
    lastSeed ? { label: `Dataset load (${fmtRows(lastSeed.rowsGenerated)})`, oracle: lastSeed.oracleRowsPerSec, influx: lastSeed.influxRowsPerSec } : null,
    bulk?.oracleRowsPerSec != null ? { label: 'Bulk ingestion batch', oracle: bulk.oracleRowsPerSec, influx: bulk.influxRowsPerSec } : null,
  ].filter(Boolean);
  const w2 = lastSeed ? winnerOf(lastSeed.oracleRowsPerSec, lastSeed.influxRowsPerSec, false) : null;

  // 3, 4 · latest reading and time range (over dataset size when the scaling test ran)
  const overSize = (key) => steps.map((s) => ({ rows: s.rows, oracle: stepWorkload(s, key)?.oracleMs, influx: stepWorkload(s, key)?.influxMs }));
  const latest = byKey(results, 'LATEST_PER_DEVICE');
  const range = byKey(results, 'RAW_RANGE');

  // 5 · aggregation
  const aggs = AGG_KEYS.map((k) => byKey(results, k)).filter(Boolean);
  const aggWins = { ORACLE: 0, INFLUX: 0 };
  aggs.forEach((r) => { if (aggWins[r.winner] != null) aggWins[r.winner]++; });
  const w5 = aggs.length ? (aggWins.INFLUX > aggWins.ORACLE ? 'INFLUX' : aggWins.ORACLE > aggWins.INFLUX ? 'ORACLE' : 'TIE') : null;

  // 6 · data volume: read-time growth from the smallest to the largest step
  const first = steps[0];
  const last = steps[steps.length - 1];
  const growth = (side) => (hasScale && sumReads(first, side) && sumReads(last, side) ? sumReads(last, side) / sumReads(first, side) : null);
  const gO = growth('oracleMs');
  const gF = growth('influxMs');
  const w6 = hasScale ? winnerOf(sumReads(last, 'oracleMs'), sumReads(last, 'influxMs')) : null;

  // 7 · correctness
  const correctness = hasScale
    ? steps.map((s) => ({ label: fmtRows(s.rows), matched: s.matched, differ: s.workloadCount - s.matched }))
    : results.length ? [{ label: fmtRows(rows), matched: suite.resultsMatched, differ: suite.queries - suite.resultsMatched }] : [];
  const allMatch = correctness.length > 0 && correctness.every((c) => c.differ === 0);

  // 8 · storage
  const w8 = storage ? winnerOf(storage.oracle?.totalBytes, storage.influx?.totalBytes) : null;
  const storageOverSize = steps.map((s) => ({ rows: s.rows, oracle: s.oracleBytes, influx: s.influxBytes }));

  // 9 · scalability: ingestion throughput across sizes
  const throughput = steps.map((s) => ({ rows: s.rows, oracle: s.oracleRowsPerSec, influx: s.influxRowsPerSec }));
  const w9 = hasScale ? winnerOf(last.oracleRowsPerSec, last.influxRowsPerSec, false) : null;

  // 10 · query complexity: length of the executed SQL vs Flux
  const complexity = reads.map((r) => ({ label: r.label, oracle: charCount(r.oracleQuery), influx: charCount(r.fluxQuery) }))
    .filter((d) => d.oracle != null && d.influx != null);
  const shorter = { ORACLE: 0, INFLUX: 0 };
  complexity.forEach((d) => { const w = winnerOf(d.oracle, d.influx); if (shorter[w] != null) shorter[w]++; });
  const sqlLines = reads.reduce((n, r) => n + (lineCount(r.oracleQuery) || 0), 0);
  const fluxLines = reads.reduce((n, r) => n + (lineCount(r.fluxQuery) || 0), 0);

  // 12 · best use case: wins by kind of work
  const cat = [
    { label: 'Lookups (latest, time range)', keys: ['LATEST_PER_DEVICE', 'RAW_RANGE'] },
    { label: 'Aggregations', keys: AGG_KEYS },
    { label: 'Filtering (threshold)', keys: ['THRESHOLD_ALERTS'] },
    { label: 'Ingestion', keys: ['BULK_INSERT'] },
  ].map((c) => {
    const rs = c.keys.map((k) => byKey(results, k)).filter(Boolean);
    return { label: c.label, oracle: rs.filter((r) => r.winner === 'ORACLE').length, influx: rs.filter((r) => r.winner === 'INFLUX').length };
  });
  if (w2) cat[3] = { ...cat[3], oracle: cat[3].oracle + (w2 === 'ORACLE' ? 1 : 0), influx: cat[3].influx + (w2 === 'INFLUX' ? 1 : 0) };
  if (w8) cat.push({ label: 'Storage', oracle: w8 === 'ORACLE' ? 1 : 0, influx: w8 === 'INFLUX' ? 1 : 0 });
  const strongO = cat.filter((c) => c.oracle > c.influx).map((c) => c.label.split(' (')[0].toLowerCase());
  const strongI = cat.filter((c) => c.influx > c.oracle).map((c) => c.label.split(' (')[0].toLowerCase());

  return (
    <>
      <p className="cmp-section-label">15 · The 12 Evaluation Parameters</p>
      <h2 className="cmp-section-title">Every parameter, <em>measured.</em></h2>
      <p className="cmp-help" style={{ marginTop: -8, marginBottom: 20 }}>
        Each card is filled from this page’s own measurements: “Run all workloads” (02), the dataset load (01),
        Measure storage (13) and the scalability test (14). The tag shows which database came out ahead on that parameter.
      </p>
      <div className="cmp-param-grid">
        <ParamCard n={1} title="Query execution time" question="Time to execute equivalent SQL and Flux queries." winner={w1}
          verdict={reads.length ? `Oracle faster in ${readWins.ORACLE}, InfluxDB in ${readWins.INFLUX} of ${reads.length} read workloads${at}.` : 'Run all workloads.'}>
          {reads.length > 0 && <SuiteLatencyChart results={reads} />}
        </ParamCard>

        <ParamCard n={2} title="Data insertion speed" question="Time taken to insert sensor readings." winner={w2}
          verdict={lastSeed ? `Loading ${Number(lastSeed.rowsGenerated).toLocaleString()} readings: Oracle ${fmtRows(lastSeed.oracleRowsPerSec)}/s, InfluxDB ${fmtRows(lastSeed.influxRowsPerSec)}/s.` : 'Generate a dataset.'}>
          {insertBars.length > 0 && <PairBars data={insertBars} axisLabel="rows / s (higher is better)" valueFormatter={(v) => fmtRows(Math.round(v))} />}
        </ParamCard>

        <ParamCard n={3} title="Data retrieval: latest reading" question="Fetching the latest reading of every sensor." winner={latest?.winner ?? null}
          verdict={latest ? `Oracle ${fmtMs(latest.oracleMs)} vs InfluxDB ${fmtMs(latest.influxMs)}${at}.` : 'Run all workloads.'}>
          {hasScale ? <SizeLines data={overSize('LATEST_PER_DEVICE')} axisLabel="ms" valueFormatter={fmtMs} />
            : latest && <ThroughputBarsMs label="Latest reading per device" r={latest} />}
        </ParamCard>

        <ParamCard n={4} title="Time-range queries" question="Retrieving one sensor's readings from a specific period." winner={range?.winner ?? null}
          verdict={range ? `Oracle ${fmtMs(range.oracleMs)} vs InfluxDB ${fmtMs(range.influxMs)}${at}.` : 'Run all workloads.'}>
          {hasScale ? <SizeLines data={overSize('RAW_RANGE')} axisLabel="ms" valueFormatter={fmtMs} />
            : range && <ThroughputBarsMs label="Raw time-range scan" r={range} />}
        </ParamCard>

        <ParamCard n={5} title="Aggregation performance" question="Averages, counts, min/max, hourly and daily roll-ups." winner={w5}
          verdict={aggs.length ? `Oracle faster in ${aggWins.ORACLE}, InfluxDB in ${aggWins.INFLUX} of ${aggs.length} aggregation workloads${at}.` : 'Run all workloads.'}>
          {aggs.length > 0 && <PairBars data={aggs.map((r) => ({ label: r.label, oracle: r.oracleMs, influx: r.influxMs }))} axisLabel="ms (lower is better)" valueFormatter={fmtMs} />}
        </ParamCard>

        <ParamCard n={6} title="Data volume" question="Performance as the number of stored readings grows." winner={w6}
          verdict={hasScale ? `From ${fmtRows(first.rows)} to ${fmtRows(last.rows)} readings, total read time grew ${fmtNum(gO, 1)}× for Oracle and ${fmtNum(gF, 1)}× for InfluxDB.` : 'Run the scalability test (14).'}>
          <SizeLines data={steps.map((s) => ({ rows: s.rows, oracle: sumReads(s, 'oracleMs'), influx: sumReads(s, 'influxMs') }))} axisLabel="ms, all reads" valueFormatter={fmtMs} logY />
        </ParamCard>

        <ParamCard n={7} title="Result correctness" question="Do both databases return equivalent results?" winner={correctness.length ? (allMatch ? 'BOTH' : 'Differences') : null}
          verdict={correctness.length ? (allMatch ? 'Every workload returned identical answers in both databases.' : 'Some workloads returned different answers — see section 07.') : 'Run all workloads.'}>
          <CorrectnessBars data={correctness} />
        </ParamCard>

        <ParamCard n={8} title="Storage efficiency" question="Space occupied by the same dataset." winner={w8}
          verdict={storage?.oracle && storage?.influx ? `${fmtBytes(storage.oracle.totalBytes)} vs ${fmtBytes(storage.influx.totalBytes)} — ${fmtNum(storage.oracle.bytesPerReading, 1)} vs ${fmtNum(storage.influx.bytesPerReading, 1)} bytes per reading.` : 'Measure storage (13).'}>
          {hasScale ? <SizeLines data={storageOverSize} axisLabel="bytes" valueFormatter={fmtBytes} logY />
            : storage && <PairBars data={[{ label: 'Bytes per reading', oracle: storage.oracle?.bytesPerReading, influx: storage.influx?.bytesPerReading }]} axisLabel="bytes (lower is better)" valueFormatter={(v) => fmtNum(v, 1)} />}
        </ParamCard>

        <ParamCard n={9} title="Scalability" question="Write throughput as volume and frequency of readings increase." winner={w9}
          verdict={hasScale ? `At ${fmtRows(last.rows)} readings: Oracle loads ${fmtRows(last.oracleRowsPerSec)}/s, InfluxDB ${fmtRows(last.influxRowsPerSec)}/s.` : 'Run the scalability test (14).'}>
          <SizeLines data={throughput} axisLabel="rows / s" valueFormatter={(v) => fmtRows(Math.round(v))} />
        </ParamCard>

        <ParamCard n={10} title="Query complexity" question="SQL in Oracle versus Flux in InfluxDB, by length of the executed query." winner={complexity.length ? winnerOf(-shorter.ORACLE, -shorter.INFLUX) : null}
          verdict={complexity.length ? `SQL is shorter in ${shorter.ORACLE} and Flux in ${shorter.INFLUX} of ${complexity.length} reads; ${sqlLines} SQL lines vs ${fluxLines} Flux lines in total.` : 'Run all workloads.'}>
          {complexity.length > 0 && <PairBars data={complexity} axisLabel="characters (shorter is simpler)" valueFormatter={(v) => fmtNum(v, 0)} />}
        </ParamCard>

        <ParamCard n={11} title="Data model" question="Relational tables versus time-series measurements and tags." winner="Different by design"
          verdict="Oracle stores each reading as a row in a 3NF table with a primary-key index, a time index and a foreign key to DEVICES; InfluxDB stores one compressed series per device (tags) with the values as fields. The chart shows what each model costs on disk.">
          <StorageStack storage={storage} />
        </ParamCard>

        <ParamCard n={12} title="Best use case" question="Structured smart-home records versus continuous sensor telemetry." winner={strongI.length > strongO.length ? 'INFLUX' : strongO.length > strongI.length ? 'ORACLE' : (strongI.length ? 'TIE' : null)}
          verdict={results.length ? `Measured here — Oracle ahead on: ${strongO.join(', ') || 'none'}; InfluxDB ahead on: ${strongI.join(', ') || 'none'}. Joins, constraints and transactions (homes, rooms, users, rules) need Oracle.` : 'Run all workloads and measure storage.'}>
          {results.length > 0 && <PairBars data={cat} axisLabel="measurements won" valueFormatter={(v) => fmtNum(v, 0)} labelWidth={190} />}
        </ParamCard>
      </div>
    </>
  );
}

/** Two bars (Oracle, InfluxDB) for one workload's latency. */
function ThroughputBarsMs({ label, r }) {
  return <PairBars data={[{ label, oracle: r.oracleMs, influx: r.influxMs }]} axisLabel="ms (lower is better)" valueFormatter={fmtMs} />;
}
