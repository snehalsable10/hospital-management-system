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

  logout: () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
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