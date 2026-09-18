import axios from 'axios';
import { auth } from '../config/firebase';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Centralized request interceptor for Firebase Bearer token injection
api.interceptors.request.use(
  async (config) => {
    const currentUser = auth.currentUser;
    if (currentUser) {
      try {
        const idToken = await currentUser.getIdToken();
        config.headers.Authorization = `Bearer ${idToken}`;
      } catch (err) {
        console.error('Failed to retrieve Firebase ID token for API request');
      }
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Centralized response interceptor for security errors
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

