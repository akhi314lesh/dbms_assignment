import {
  ResponsiveContainer, LineChart, Line, BarChart, Bar, XAxis, YAxis, CartesianGrid,
  Tooltip, ReferenceLine, Cell,
} from 'recharts';

import { ORACLE, INFLUX, fmtMs, fmtNum, fmtBytes, fmtRows } from './format';

const SURFACE = '#FFFDF9';
const GRID = 'rgba(33,26,21,0.09)';
const AXIS_TEXT = '#6C5B51';
const TEXT = '#211A15';
const HOVER = 'rgba(155,128,101,0.10)';

const axisProps = {
  stroke: GRID,
  tick: { fill: AXIS_TEXT, fontSize: 11, fontFamily: 'var(--font-mono)' },
  tickLine: false,
};

function DarkTooltip({ active, payload, label, valueFormatter = fmtNum, labelFormatter }) {
  if (!active || !payload || payload.length === 0) return null;
  return (
    <div className="chart-tooltip">
      <div className="chart-tooltip-label">{labelFormatter ? labelFormatter(label) : label}</div>
      {payload.map((p) => (
        <div key={p.dataKey} className="chart-tooltip-row">
          <span className="chart-tooltip-key" style={{ background: p.color || p.stroke || p.fill }} />
          <strong>{valueFormatter(p.value)}</strong>
          <span className="chart-tooltip-name">{p.name}</span>
        </div>
      ))}
    </div>
  );
}

function LegendRow({ items }) {
  return (
    <div className="chart-legend">
      {items.map((it) => (
        <span key={it.label} className="chart-legend-item">
          <span className={`chart-legend-swatch ${it.shape || 'rect'}`} style={{ background: it.color }} />
          {it.label}
        </span>
      ))}
    </div>
  );
}

const BOTH_BARS = [
  { label: 'Oracle 21c', color: ORACLE },
  { label: 'InfluxDB', color: INFLUX },
];
const BOTH_LINES = BOTH_BARS.map((b) => ({ ...b, shape: 'line' }));

/** Latency of every timed iteration (lower is better). */
export function IterationChart({ oracle = [], influx = [] }) {
  const n = Math.max(oracle.length, influx.length);
  const data = Array.from({ length: n }, (_, i) => ({ run: i + 1, oracle: oracle[i] ?? null, influx: influx[i] ?? null }));
  return (
    <div>
      <LegendRow items={BOTH_LINES} />
      <ResponsiveContainer width="100%" height={240}>
        <LineChart data={data} margin={{ top: 8, right: 16, bottom: 4, left: 0 }}>
          <CartesianGrid stroke={GRID} vertical={false} />
          <XAxis dataKey="run" {...axisProps} label={{ value: 'Timed run #', position: 'insideBottom', offset: -2, fill: AXIS_TEXT, fontSize: 11 }} height={36} />
          <YAxis {...axisProps} width={56} tickFormatter={(v) => fmtNum(v, 1)} label={{ value: 'ms', angle: -90, position: 'insideLeft', fill: AXIS_TEXT, fontSize: 11 }} />
          <Tooltip content={<DarkTooltip valueFormatter={fmtMs} labelFormatter={(l) => `Run ${l}`} />} cursor={{ stroke: AXIS_TEXT, strokeWidth: 1 }} />
          <Line type="linear" dataKey="oracle" name="Oracle 21c" stroke={ORACLE} strokeWidth={2} dot={{ r: 4, fill: ORACLE, stroke: SURFACE, strokeWidth: 2 }} activeDot={{ r: 6 }} isAnimationActive={false} />
          <Line type="linear" dataKey="influx" name="InfluxDB" stroke={INFLUX} strokeWidth={2} dot={{ r: 4, fill: INFLUX, stroke: SURFACE, strokeWidth: 2 }} activeDot={{ r: 6 }} isAnimationActive={false} />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}

/** min / median / avg / p95 / max side by side. */
export function DistributionChart({ oracle, influx }) {
  const keys = [['minMs', 'Min'], ['medianMs', 'Median'], ['avgMs', 'Average'], ['p95Ms', 'p95'], ['maxMs', 'Max']];
  const data = keys.map(([k, label]) => ({ label, oracle: oracle?.[k] ?? null, influx: influx?.[k] ?? null }));
  return (
    <div>
      <LegendRow items={BOTH_BARS} />
      <ResponsiveContainer width="100%" height={240}>
        <BarChart data={data} margin={{ top: 8, right: 16, bottom: 4, left: 0 }} barGap={2} barCategoryGap="28%">
          <CartesianGrid stroke={GRID} vertical={false} />
          <XAxis dataKey="label" {...axisProps} />
          <YAxis {...axisProps} width={56} tickFormatter={(v) => fmtNum(v, 1)} label={{ value: 'ms', angle: -90, position: 'insideLeft', fill: AXIS_TEXT, fontSize: 11 }} />
          <Tooltip content={<DarkTooltip valueFormatter={fmtMs} />} cursor={{ fill: HOVER }} />
          <Bar dataKey="oracle" name="Oracle 21c" fill={ORACLE} radius={[4, 4, 0, 0]} maxBarSize={24} isAnimationActive={false} />
          <Bar dataKey="influx" name="InfluxDB" fill={INFLUX} radius={[4, 4, 0, 0]} maxBarSize={24} isAnimationActive={false} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}

/** What each database actually returned — the two series should overlap when results match. */
export function ResultChart({ chart }) {
  if (!chart || !chart.points || chart.points.length === 0) {
    return <p className="chart-empty">No result rows to plot.</p>;
  }
  const { kind, points, unit } = chart;
  if (kind === 'time') {
    const short = (l) => (typeof l === 'string' ? l.replace('T', ' ').replace(':00Z', '').slice(5) : l);
    return (
      <div>
        <LegendRow items={[{ label: 'Oracle 21c', color: ORACLE, shape: 'line' }, { label: 'InfluxDB (dots)', color: INFLUX, shape: 'dot' }]} />
        <ResponsiveContainer width="100%" height={260}>
          <LineChart data={points} margin={{ top: 8, right: 34, bottom: 4, left: 0 }}>
            <CartesianGrid stroke={GRID} vertical={false} />
            <XAxis dataKey="label" {...axisProps} tickFormatter={short} minTickGap={40} />
            <YAxis {...axisProps} width={56} domain={['auto', 'auto']} tickFormatter={(v) => fmtNum(v, 1)} label={{ value: unit, angle: -90, position: 'insideLeft', fill: AXIS_TEXT, fontSize: 11 }} />
            <Tooltip content={<DarkTooltip labelFormatter={(l) => String(l).replace('T', ' ').replace('Z', ' UTC')} />} cursor={{ stroke: AXIS_TEXT, strokeWidth: 1 }} />
            <Line type="linear" dataKey="oracle" name="Oracle 21c" stroke={ORACLE} strokeWidth={2} dot={false} isAnimationActive={false} connectNulls />
            <Line type="linear" dataKey="influx" name="InfluxDB" stroke="transparent" dot={{ r: 2.5, fill: INFLUX, stroke: 'none' }} activeDot={{ r: 5, fill: INFLUX }} isAnimationActive={false} />
          </LineChart>
        </ResponsiveContainer>
      </div>
    );
  }
  if (kind === 'throughput') {
    return <ThroughputChart oracle={points[0]?.oracle} influx={points[0]?.influx} />;
  }
  return (
    <div>
      <LegendRow items={BOTH_BARS} />
      <ResponsiveContainer width="100%" height={Math.max(220, kind === 'device' ? points.length * 26 + 40 : 260)}>
        {kind === 'device' ? (
          <BarChart data={points} layout="vertical" margin={{ top: 4, right: 24, bottom: 4, left: 8 }} barGap={2} barCategoryGap="22%">
            <CartesianGrid stroke={GRID} horizontal={false} />
            <XAxis type="number" {...axisProps} tickFormatter={(v) => fmtNum(v, 1)} />
            <YAxis type="category" dataKey="label" {...axisProps} width={190} tick={{ fill: AXIS_TEXT, fontSize: 11 }} interval={0} />
            <Tooltip content={<DarkTooltip />} cursor={{ fill: HOVER }} />
            <Bar dataKey="oracle" name="Oracle 21c" fill={ORACLE} radius={[0, 4, 4, 0]} maxBarSize={10} isAnimationActive={false} />
            <Bar dataKey="influx" name="InfluxDB" fill={INFLUX} radius={[0, 4, 4, 0]} maxBarSize={10} isAnimationActive={false} />
          </BarChart>
        ) : (
          <BarChart data={points} margin={{ top: 8, right: 16, bottom: 4, left: 0 }} barGap={2} barCategoryGap="28%">
            <CartesianGrid stroke={GRID} vertical={false} />
            <XAxis dataKey="label" {...axisProps} tickFormatter={(l) => (typeof l === 'string' ? l.slice(5, 10) || l : l)} />
            <YAxis {...axisProps} width={56} tickFormatter={(v) => fmtNum(v, 1)} label={{ value: unit, angle: -90, position: 'insideLeft', fill: AXIS_TEXT, fontSize: 11 }} />
            <Tooltip content={<DarkTooltip />} cursor={{ fill: HOVER }} />
            <Bar dataKey="oracle" name="Oracle 21c" fill={ORACLE} radius={[4, 4, 0, 0]} maxBarSize={24} isAnimationActive={false} />
            <Bar dataKey="influx" name="InfluxDB" fill={INFLUX} radius={[4, 4, 0, 0]} maxBarSize={24} isAnimationActive={false} />
          </BarChart>
        )}
      </ResponsiveContainer>
    </div>
  );
}

/** Rows per second written (higher is better). */
export function ThroughputChart({ oracle, influx }) {
  const data = [
    { label: 'Oracle 21c', value: oracle ?? 0, color: ORACLE },
    { label: 'InfluxDB', value: influx ?? 0, color: INFLUX },
  ];
  return (
    <ResponsiveContainer width="100%" height={130}>
      <BarChart data={data} layout="vertical" margin={{ top: 4, right: 90, bottom: 4, left: 8 }}>
        <CartesianGrid stroke={GRID} horizontal={false} />
        <XAxis type="number" {...axisProps} tickFormatter={(v) => (v >= 1e6 ? `${+(v / 1e6).toFixed(1)}M` : v >= 1000 ? `${Math.round(v / 1000)}K` : v)} />
        <YAxis type="category" dataKey="label" {...axisProps} width={90} tick={{ fill: TEXT, fontSize: 12 }} />
        <Tooltip content={<DarkTooltip valueFormatter={(v) => `${Math.round(v).toLocaleString()} rows/s`} />} cursor={{ fill: HOVER }} />
        <Bar dataKey="value" name="Rows / second" radius={[0, 4, 4, 0]} maxBarSize={24} isAnimationActive={false}
          label={{ position: 'right', fill: TEXT, fontSize: 12, formatter: (v) => `${Math.round(v).toLocaleString()} /s` }}>
          {data.map((d) => <Cell key={d.label} fill={d.color} />)}
        </Bar>
      </BarChart>
    </ResponsiveContainer>
  );
}

/** Average latency per workload for the full suite. */
export function SuiteLatencyChart({ results }) {
  const data = results.filter((r) => r.oracleMs != null && r.influxMs != null)
    .map((r) => ({ label: r.label, oracle: r.oracleMs, influx: r.influxMs }));
  return (
    <div>
      <LegendRow items={BOTH_BARS} />
      <ResponsiveContainer width="100%" height={data.length * 44 + 40}>
        <BarChart data={data} layout="vertical" margin={{ top: 4, right: 24, bottom: 4, left: 8 }} barGap={2} barCategoryGap="24%">
          <CartesianGrid stroke={GRID} horizontal={false} />
          <XAxis type="number" {...axisProps} tickFormatter={(v) => fmtNum(v, 0)} label={{ value: 'average ms (lower is better)', position: 'insideBottomRight', offset: -2, fill: AXIS_TEXT, fontSize: 11 }} height={34} />
          <YAxis type="category" dataKey="label" {...axisProps} width={200} tick={{ fill: AXIS_TEXT, fontSize: 11 }} interval={0} />
          <Tooltip content={<DarkTooltip valueFormatter={fmtMs} />} cursor={{ fill: HOVER }} />
          <Bar dataKey="oracle" name="Oracle 21c" fill={ORACLE} radius={[0, 4, 4, 0]} maxBarSize={14} isAnimationActive={false} />
          <Bar dataKey="influx" name="InfluxDB" fill={INFLUX} radius={[0, 4, 4, 0]} maxBarSize={14} isAnimationActive={false} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}

/** Diverging chart: how many times faster the winner was (left = Oracle, right = InfluxDB). */
export function SpeedupChart({ results }) {
  const data = results.filter((r) => r.speedupFactor != null && r.winner)
    .map((r) => ({
      label: r.label,
      value: r.winner === 'ORACLE' ? -r.speedupFactor : r.winner === 'INFLUX' ? r.speedupFactor : 0,
      winner: r.winner,
      speedup: r.speedupFactor,
    }));
  const max = Math.max(2, ...data.map((d) => Math.abs(d.value)));
  const lim = Math.ceil(max);
  return (
    <div>
      <LegendRow items={[{ label: 'Oracle faster (left)', color: ORACLE }, { label: 'InfluxDB faster (right)', color: INFLUX }]} />
      <ResponsiveContainer width="100%" height={data.length * 40 + 40}>
        <BarChart data={data} layout="vertical" margin={{ top: 4, right: 40, bottom: 4, left: 8 }}>
          <CartesianGrid stroke={GRID} horizontal={false} />
          <XAxis type="number" domain={[-lim, lim]} {...axisProps} tickFormatter={(v) => `${Math.abs(v)}×`} />
          <YAxis type="category" dataKey="label" {...axisProps} width={200} tick={{ fill: AXIS_TEXT, fontSize: 11 }} interval={0} />
          <ReferenceLine x={0} stroke={AXIS_TEXT} />
          <Tooltip
            cursor={{ fill: HOVER }}
            content={({ active, payload }) => {
              if (!active || !payload?.length) return null;
              const d = payload[0].payload;
              return (
                <div className="chart-tooltip">
                  <div className="chart-tooltip-label">{d.label}</div>
                  <div className="chart-tooltip-row">
                    <span className="chart-tooltip-key" style={{ background: d.winner === 'ORACLE' ? ORACLE : INFLUX }} />
                    <strong>{d.speedup.toFixed(2)}× faster</strong>
                    <span className="chart-tooltip-name">{d.winner === 'ORACLE' ? 'Oracle' : d.winner === 'INFLUX' ? 'InfluxDB' : 'Tie'}</span>
                  </div>
                </div>
              );
            }}
          />
          <Bar dataKey="value" maxBarSize={16} isAnimationActive={false}>
            {data.map((d) => (
              <Cell key={d.label} fill={d.winner === 'ORACLE' ? ORACLE : INFLUX} radius={d.value < 0 ? [4, 0, 0, 4] : [0, 4, 4, 0]} />
            ))}
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}

/** Oracle vs InfluxDB for a few labelled items, as horizontal paired bars. */
export function PairBars({ data, axisLabel, valueFormatter = fmtNum, labelWidth = 170 }) {
  const rows = data.filter((d) => d.oracle != null || d.influx != null);
  if (rows.length === 0) return <p className="chart-empty">No measurements yet.</p>;
  return (
    <div>
      <LegendRow items={BOTH_BARS} />
      <ResponsiveContainer width="100%" height={rows.length * 40 + 50}>
        <BarChart data={rows} layout="vertical" margin={{ top: 4, right: 70, bottom: 4, left: 4 }} barGap={2} barCategoryGap="24%">
          <CartesianGrid stroke={GRID} horizontal={false} />
          <XAxis type="number" {...axisProps} tickFormatter={(v) => valueFormatter(v)} label={axisLabel ? { value: axisLabel, position: 'insideBottomRight', offset: -2, fill: AXIS_TEXT, fontSize: 11 } : undefined} height={34} />
          <YAxis type="category" dataKey="label" {...axisProps} width={labelWidth} tick={{ fill: AXIS_TEXT, fontSize: 11 }} interval={0} />
          <Tooltip content={<DarkTooltip valueFormatter={valueFormatter} />} cursor={{ fill: HOVER }} />
          <Bar dataKey="oracle" name="Oracle 21c" fill={ORACLE} radius={[0, 4, 4, 0]} maxBarSize={14} isAnimationActive={false}
            label={{ position: 'right', fill: AXIS_TEXT, fontSize: 10, formatter: (v) => (v == null ? '' : valueFormatter(v)) }} />
          <Bar dataKey="influx" name="InfluxDB" fill={INFLUX} radius={[0, 4, 4, 0]} maxBarSize={14} isAnimationActive={false}
            label={{ position: 'right', fill: AXIS_TEXT, fontSize: 10, formatter: (v) => (v == null ? '' : valueFormatter(v)) }} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}

/** A measure plotted against dataset size (log scale on rows), one line per database. */
export function SizeLines({ data, axisLabel, valueFormatter = fmtNum, logY = false }) {
  const rows = data.filter((d) => d.rows > 0 && (d.oracle != null || d.influx != null));
  if (rows.length === 0) return <p className="chart-empty">Run the scalability test (section 14) to fill this chart.</p>;
  const positive = (v) => (logY && v != null && v <= 0 ? null : v);
  const plotted = rows.map((d) => ({ rows: d.rows, oracle: positive(d.oracle), influx: positive(d.influx) }));
  return (
    <div>
      <LegendRow items={BOTH_LINES} />
      <ResponsiveContainer width="100%" height={240}>
        <LineChart data={plotted} margin={{ top: 8, right: 20, bottom: 4, left: 4 }}>
          <CartesianGrid stroke={GRID} vertical={false} />
          <XAxis dataKey="rows" type="number" scale="log" domain={['dataMin', 'dataMax']} ticks={plotted.map((d) => d.rows)}
            {...axisProps} tickFormatter={fmtRows} label={{ value: 'readings in dataset (log scale)', position: 'insideBottom', offset: -2, fill: AXIS_TEXT, fontSize: 11 }} height={38} />
          <YAxis {...axisProps} width={64} scale={logY ? 'log' : 'auto'} domain={logY ? ['auto', 'auto'] : [0, 'auto']} allowDataOverflow={false}
            tickFormatter={(v) => valueFormatter(v)} label={axisLabel ? { value: axisLabel, angle: -90, position: 'insideLeft', fill: AXIS_TEXT, fontSize: 11 } : undefined} />
          <Tooltip content={<DarkTooltip valueFormatter={valueFormatter} labelFormatter={(l) => `${Number(l).toLocaleString()} readings`} />} cursor={{ stroke: AXIS_TEXT, strokeWidth: 1 }} />
          <Line type="linear" dataKey="oracle" name="Oracle 21c" stroke={ORACLE} strokeWidth={2} dot={{ r: 4, fill: ORACLE, stroke: SURFACE, strokeWidth: 2 }} isAnimationActive={false} connectNulls />
          <Line type="linear" dataKey="influx" name="InfluxDB" stroke={INFLUX} strokeWidth={2} dot={{ r: 4, fill: INFLUX, stroke: SURFACE, strokeWidth: 2 }} isAnimationActive={false} connectNulls />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}

/** Bytes on disk split by storage structure: Oracle table + indexes vs InfluxDB TSM data + index + WAL. */
export function StorageStack({ storage }) {
  const o = storage?.oracle;
  const f = storage?.influx;
  if (!o && !f) return <p className="chart-empty">Measure storage (section 13) to fill this chart.</p>;
  const seg = (name) => o?.segments?.find((s) => s.name === name)?.bytes ?? 0;
  const pk = seg('PK_SENSOR_BENCH');
  const timeIdx = seg('IDX_SENSOR_BENCH_TIME');
  const data = [
    { label: 'Oracle 21c', oTable: o?.tableBytes ?? 0, oPk: pk, oTime: timeIdx, oOther: Math.max(0, (o?.indexBytes ?? 0) - pk - timeIdx) },
    { label: 'InfluxDB', fTsm: f?.tsmBytes ?? 0, fIndex: f?.indexBytes ?? 0, fWal: f?.walBytes ?? 0 },
  ];
  const parts = [
    ['oTable', 'Oracle table rows', ORACLE, 1], ['oPk', 'Oracle primary-key index', ORACLE, 0.6], ['oTime', 'Oracle time index', ORACLE, 0.35], ['oOther', 'Oracle other indexes', ORACLE, 0.2],
    ['fTsm', 'InfluxDB compressed TSM data', INFLUX, 1], ['fIndex', 'InfluxDB series + TSI index', INFLUX, 0.55], ['fWal', 'InfluxDB write-ahead log', INFLUX, 0.25],
  ];
  return (
    <div>
      <div className="chart-legend">
        {parts.map(([k, label, color, op]) => (
          <span key={k} className="chart-legend-item">
            <span className="chart-legend-swatch rect" style={{ background: color, opacity: op }} />
            {label}
          </span>
        ))}
      </div>
      <ResponsiveContainer width="100%" height={150}>
        <BarChart data={data} layout="vertical" margin={{ top: 4, right: 24, bottom: 4, left: 4 }}>
          <CartesianGrid stroke={GRID} horizontal={false} />
          <XAxis type="number" {...axisProps} tickFormatter={fmtBytes} />
          <YAxis type="category" dataKey="label" {...axisProps} width={90} tick={{ fill: TEXT, fontSize: 12 }} />
          <Tooltip content={<DarkTooltip valueFormatter={fmtBytes} />} cursor={{ fill: HOVER }} />
          {parts.map(([k, label, color, op]) => (
            <Bar key={k} dataKey={k} name={label} stackId="s" fill={color} fillOpacity={op} maxBarSize={30} isAnimationActive={false} />
          ))}
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}

/** Stacked count of workloads whose answers matched / differed. */
export function CorrectnessBars({ data }) {
  if (!data.length) return <p className="chart-empty">Run all workloads to fill this chart.</p>;
  return (
    <div>
      <LegendRow items={[{ label: 'Identical results', color: '#4f8f62' }, { label: 'Results differ', color: '#b54848' }]} />
      <ResponsiveContainer width="100%" height={data.length * 34 + 50}>
        <BarChart data={data} layout="vertical" margin={{ top: 4, right: 24, bottom: 4, left: 4 }}>
          <CartesianGrid stroke={GRID} horizontal={false} />
          <XAxis type="number" {...axisProps} allowDecimals={false} label={{ value: 'workloads', position: 'insideBottomRight', offset: -2, fill: AXIS_TEXT, fontSize: 11 }} height={34} />
          <YAxis type="category" dataKey="label" {...axisProps} width={90} tick={{ fill: AXIS_TEXT, fontSize: 11 }} interval={0} />
          <Tooltip content={<DarkTooltip valueFormatter={(v) => `${v}`} />} cursor={{ fill: HOVER }} />
          <Bar dataKey="matched" name="Identical results" stackId="c" fill="#4f8f62" maxBarSize={16} isAnimationActive={false} />
          <Bar dataKey="differ" name="Results differ" stackId="c" fill="#b54848" maxBarSize={16} isAnimationActive={false} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
