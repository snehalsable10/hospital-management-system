import axios from 'axios';

// Set VITE_API_URL per environment (see frontend/.env). The fallback keeps
// local development working without any env file present.
const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Attach JWT token to every outgoing request
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

const signOut = () => {
  localStorage.removeItem('token');
  localStorage.removeItem('refreshToken');
  localStorage.removeItem('user');
  window.location.href = '/login';
};

/**
 * The refresh in flight, if any.
 *
 * Several requests usually fail together when a token expires - a page load
 * that fetches a list and a count at once, say. Without this they would each
 * start their own refresh, and because every exchange revokes the token it
 * was given, the first to finish would invalidate the rest and sign the user
 * out. They all wait on the same promise instead.
 */
let refreshInFlight = null;

const refreshTokens = () => {
  if (refreshInFlight) {
    return refreshInFlight;
  }

  const refreshToken = localStorage.getItem('refreshToken');
  if (!refreshToken) {
    return Promise.reject(new Error('no refresh token'));
  }

  // A bare axios call, not `api`: going through this instance would re-enter
  // the interceptor below and recurse if the refresh itself returned 401.
  refreshInFlight = axios
    .post(`${api.defaults.baseURL}/auth/refresh`, { refreshToken })
    .then((response) => {
      const auth = response.data?.data;
      if (!auth?.token) {
        throw new Error('refresh returned no token');
      }
      localStorage.setItem('token', auth.token);
      if (auth.refreshToken) {
        localStorage.setItem('refreshToken', auth.refreshToken);
      }
      return auth.token;
    })
    .finally(() => {
      refreshInFlight = null;
    });

  return refreshInFlight;
};

// Handle expired/invalid tokens globally
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config;
    const url = original?.url || '';

    // Login, signup and refresh answer 401 for their own reasons; retrying
    // them would loop.
    const isAuthRequest = url.includes('/auth/');

    // `_retried` stops a request that fails again after a successful refresh
    // from refreshing a second time.
    if (error.response?.status === 401 && !isAuthRequest && !original?._retried) {
      original._retried = true;
      try {
        const token = await refreshTokens();
        original.headers = { ...original.headers, Authorization: `Bearer ${token}` };
        return api(original);
      } catch {
        // The refresh token is gone, expired or already spent: this session
        // is over.
        signOut();
        return Promise.reject(error);
      }
    }

    if (error.response?.status === 401 && !isAuthRequest) {
      signOut();
    }

    return Promise.reject(error);
  }
);

export default api;
