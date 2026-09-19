import api from './api';

const authService = {

  signup: async (userData) => {
    const response = await api.post('/auth/signup', userData);
    return response.data;
  },

  login: async (credentials) => {
    const response = await api.post('/auth/login', credentials);
    const apiResponse = response.data;

    if (apiResponse.success && apiResponse.data) {
      const auth = apiResponse.data;
      localStorage.setItem('token', auth.token);
      localStorage.setItem('user', JSON.stringify({
        userId: auth.userId,
        username: auth.username,
        email: auth.email,
        firstName: auth.firstName,
        lastName: auth.lastName,
        role: auth.role,
      }));
    }

    return apiResponse;
  },

  /**
   * Tell the server to blacklist this token before discarding it locally.
   * A network failure must not trap the user in a logged-in state, so the
   * local session is cleared either way.
   */
  logout: async () => {
    try {
      await api.post('/auth/logout');
    } catch (err) {
      // Already logging out; nothing useful to do with the error.
    } finally {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
    }
  },

  getCurrentUser: () => {
    const user = localStorage.getItem('user');
    return user ? JSON.parse(user) : null;
  },

  isLoggedIn: () => {
    return localStorage.getItem('token') !== null;
  },

};

export default authService;