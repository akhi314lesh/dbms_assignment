// Series colours validated for the light chart surface #FFFDF9 (lightness band, chroma, CVD separation, contrast).
export const ORACLE = '#c4531a';
export const INFLUX = '#0086a8';

export function fmtMs(v) {
  if (v === null || v === undefined || Number.isNaN(v)) return '—';
  if (v >= 1000) return `${(v / 1000).toFixed(2)} s`;
  if (v >= 100) return `${v.toFixed(0)} ms`;
  if (v >= 10) return `${v.toFixed(1)} ms`;
  return `${v.toFixed(2)} ms`;
}

export function fmtNum(v, digits = 2) {
  if (v === null || v === undefined || Number.isNaN(v)) return '—';
  if (Math.abs(v) >= 1000) return Math.round(v).toLocaleString();
  const str = Number(v).toFixed(digits);
  return str.includes('.') ? str.replace(/0+$/, '').replace(/\.$/, '') : str;
}


export function fmtBytes(b) {
  if (b === null || b === undefined || Number.isNaN(b)) return '—';
  if (b >= 1024 ** 3) return `${(b / 1024 ** 3).toFixed(2)} GB`;
  if (b >= 1024 ** 2) return `${(b / 1024 ** 2).toFixed(1)} MB`;
  if (b >= 1024) return `${(b / 1024).toFixed(0)} KB`;
  return `${Math.round(b)} B`;
}

export function fmtRows(v) {
  if (v === null || v === undefined) return '—';
  if (v >= 1e6) return `${+(v / 1e6).toFixed(2)}M`;
  if (v >= 1000) return `${+(v / 1000).toFixed(1)}K`;
  return String(v);
}
