import axios from 'axios';

export const API_BASE_URL = (import.meta.env.VITE_API_URL || import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/$/, '');

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  // Generating a large dataset or running every workload can take a while.
  timeout: 10 * 60 * 1000,
});

/** Turns any axios error into a short, human-readable sentence. */
export function apiErrorMessage(err) {
  if (err?.response?.data) {
    const d = err.response.data;
    if (typeof d === 'string' && d.trim()) return d.slice(0, 400);
    if (d.message) return d.message;
    if (d.error && typeof d.error === 'string') return `${d.error} (HTTP ${err.response.status})`;
    return `HTTP ${err.response.status}`;
  }
  if (err?.code === 'ECONNABORTED') return 'The request timed out.';
  if (err?.message === 'Network Error') return `Cannot reach the backend at ${API_BASE_URL} (is Spring Boot running on port 8080?).`;
  return err?.message || 'Unknown error';
}

export default api;
