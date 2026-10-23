import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Centralized response interceptor for error logging
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 403) {
      console.warn('Access Denied (403): Insufficient permissions or resource not owned.', error.response.data);
    } else if (error.response && error.response.status === 401) {
      console.warn('Unauthorized (401): Authentication required.');
    }
    return Promise.reject(error);
  }
);

export default api;