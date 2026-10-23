// Static content for the comparison dashboard.

export const DATASET_PRESETS = [
  { key: 'small', label: 'Small — 1 day @ 5 min', days: 1, intervalMinutes: 5 },
  { key: 'standard', label: 'Standard — 7 days @ 5 min', days: 7, intervalMinutes: 5 },
  { key: 'large', label: 'Large — 30 days @ 5 min', days: 30, intervalMinutes: 5 },
  { key: 'xl', label: 'Extra large — 30 days @ 1 min', days: 30, intervalMinutes: 1 },
  { key: 'huge', label: 'Huge — 90 days @ 1 min', days: 90, intervalMinutes: 1 },
  { key: 'massive', label: 'Massive — 180 days @ 1 min', days: 180, intervalMinutes: 1 },
];

// Dataset sizes the scalability test steps through by default (smallest first).
export const DEFAULT_SCALING_STEPS = ['small', 'standard', 'large', 'xl', 'huge'];

export const RANGE_OPTIONS = [
  { value: 6, label: 'Last 6 hours' },
  { value: 24, label: 'Last 24 hours' },
  { value: 72, label: 'Last 3 days' },
  { value: 168, label: 'Last 7 days' },
  { value: 720, label: 'Last 30 days' },
  { value: 0, label: 'Entire dataset' },
];

export const MATRIX_ROWS = [
  { dimension: 'Bulk ingestion of telemetry', oracle: '~', influx: '✓', oracleNote: 'JDBC batch INSERT + index & FK maintenance', influxNote: 'Line Protocol appended to WAL' },
  { dimension: 'Time-window aggregation', oracle: '~', influx: '✓', oracleNote: 'TRUNC() + GROUP BY per row', influxNote: 'aggregateWindow() over ordered series' },
  { dimension: 'Latest value per device', oracle: '~', influx: '✓', oracleNote: 'KEEP (DENSE_RANK LAST) scan', influxNote: 'last() storage push-down' },
  { dimension: 'ACID transactions', oracle: '✓', influx: '✗', oracleNote: 'Full ACID, commit/rollback', influxNote: 'Per-write durability only' },
  { dimension: 'Joins & relational model', oracle: '✓', influx: '✗', oracleNote: 'Homes ↔ rooms ↔ devices ↔ users', influxNote: 'Tags only; joins are limited' },
  { dimension: 'Referential integrity', oracle: '✓', influx: '✗', oracleNote: 'FOREIGN KEY → DEVICES', influxNote: 'Any tag value is accepted' },
  { dimension: 'Automatic data retention / TTL', oracle: '✗', influx: '✓', oracleNote: 'Manual partition drop / jobs', influxNote: 'Bucket retention policy' },
  { dimension: 'Stored procedures & triggers', oracle: '✓', influx: '✗', oracleNote: 'PL/SQL, triggers, packages', influxNote: 'Tasks (scheduled Flux) only' },
  { dimension: 'Storage compression of time series', oracle: '~', influx: '✓', oracleNote: 'Row format (compression is an option)', influxNote: 'Delta / Gorilla encoding in TSM' },
  { dimension: 'Lightweight deployment', oracle: '~', influx: '✓', oracleNote: 'Large installation footprint', influxNote: 'Single small binary / container' },
];

export const JUSTIFICATION = [
  {
    title: 'Storage built for time',
    body: 'InfluxDB stores each sensor series contiguously in time order inside compressed TSM blocks, so a time-range query reads only the blocks it needs. Oracle stores rows in heap blocks and relies on B-tree indexes to find a time range.',
  },
  {
    title: 'Ingestion path',
    body: 'A Line Protocol write is appended to a write-ahead log and an in-memory cache. An Oracle INSERT also updates the primary-key index, the time index, checks the foreign key and writes undo/redo — safer, but more work per row.',
  },
  {
    title: 'Time-series functions',
    body: 'Flux has aggregateWindow(), last(), movingAverage() and retention built in. In SQL the same results need TRUNC(), GROUP BY, analytic functions and scheduled purge jobs.',
  },
  {
    title: 'Where Oracle stays essential',
    body: 'Users, homes, rooms, device ownership, automation rules and alerts need joins, constraints and transactions. Oracle remains the system of record; InfluxDB is the telemetry store.',
  },
  {
    title: 'Read the numbers honestly',
    body: 'On small datasets Oracle answers most reads faster: InfluxDB pays a fixed cost of about 10–15 ms per Flux query (HTTP, planning, CSV). As the data grows Oracle’s read time grows with it while InfluxDB’s barely moves, and InfluxDB stores the same readings in a fraction of the space. The scalability test (section 14) shows where the lines cross on this machine.',
  },
  {
    title: 'Polyglot persistence',
    body: 'The recommended architecture is to use both: Oracle for relational smart-home data and InfluxDB for high-frequency sensor readings, linked by device_id.',
  },
];
