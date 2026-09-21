import api from './api';

/**
 * Login accounts, as opposed to the patient and doctor records they may be
 * linked to. Every call here is ADMIN-only.
 *
 * Creating one lives at /auth/users rather than /users, because minting a
 * credential belongs next to signup and login and is audited there. Everything
 * else about an existing account is on /users.
 *
 * Responses carry a UserResponse, never the entity, so no password hash comes
 * back.
 */
const userService = {
  getAllUsersPaginated: async (pageNumber = 0, pageSize = 10) => {
    const response = await api.get('/users/paginated', {
      params: { pageNumber, pageSize },
    });
    return response.data;
  },

  getUsersByRolePaginated: async (role, pageNumber = 0, pageSize = 10) => {
    const response = await api.get(`/users/role/${encodeURIComponent(role)}/paginated`, {
      params: { pageNumber, pageSize },
    });
    return response.data;
  },

  getUserById: async (id) => {
    const response = await api.get(`/users/${id}`);
    return response.data;
  },

  createUser: async (userData) => {
    const response = await api.post('/auth/users', userData);
    return response.data;
  },

  /** A blank password leaves the existing one in place. */
  updateUser: async (id, userData) => {
    const response = await api.put(`/users/${id}`, userData);
    return response.data;
  },

  deactivateUser: async (id) => {
    const response = await api.delete(`/users/${id}`);
    return response.data;
  },
};

export default userService;
