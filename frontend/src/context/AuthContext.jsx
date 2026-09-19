import React, { createContext, useState, useEffect } from 'react';
import authService from '../services/authService';

export const AuthContext = createContext(null);

/**
 * authService owns localStorage; this context mirrors it into React state so
 * components re-render when the session changes. Previously the two disagreed:
 * login wrote storage directly and context state stayed null until a reload.
 */
export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(() => authService.getCurrentUser());
  const [token, setToken] = useState(() => localStorage.getItem('token'));
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // Keep other tabs in step: logging out in one tab clears the others.
  useEffect(() => {
    const syncFromStorage = () => {
      setUser(authService.getCurrentUser());
      setToken(localStorage.getItem('token'));
    };
    window.addEventListener('storage', syncFromStorage);
    return () => window.removeEventListener('storage', syncFromStorage);
  }, []);

  const login = async (credentials) => {
    setLoading(true);
    setError(null);
    try {
      const response = await authService.login(credentials);

      if (response.success) {
        setUser(authService.getCurrentUser());
        setToken(localStorage.getItem('token'));
      } else {
        setError(response.message);
      }

      return response;
    } catch (err) {
      const message = err.response?.data?.message || 'Login failed';
      setError(message);
      throw err;
    } finally {
      setLoading(false);
    }
  };

  /**
   * Signup does not return a token - the backend creates the account and the
   * user logs in afterwards. No session state is set here.
   */
  const signup = async (userData) => {
    setLoading(true);
    setError(null);
    try {
      const response = await authService.signup(userData);
      if (!response.success) {
        setError(response.message);
      }
      return response;
    } catch (err) {
      const message = err.response?.data?.message || 'Signup failed';
      setError(message);
      throw err;
    } finally {
      setLoading(false);
    }
  };

  const logout = async () => {
    await authService.logout();
    setUser(null);
    setToken(null);
    setError(null);
  };

  const value = {
    user,
    token,
    loading,
    error,
    role: user?.role || null,
    isAuthenticated: !!token,
    login,
    signup,
    logout,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};