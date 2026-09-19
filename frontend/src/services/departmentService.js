import api from './api';

/**
 * Departments have no search endpoint - only a full list and a paginated one.
 */
const departmentService = {
  getAllDepartments: async () => {
    const response = await api.get('/departments');
    return response.data;
  },

  getAllDepartmentsPaginated: async (pageNumber = 0, pageSize = 10) => {
    const response = await api.get('/departments/paginated', {
      params: { pageNumber, pageSize },
    });
    return response.data;
  },

  getDepartmentById: async (id) => {
    const response = await api.get(`/departments/${id}`);
    return response.data;
  },

  createDepartment: async (departmentData) => {
    const response = await api.post('/departments', departmentData);
    return response.data;
  },

  updateDepartment: async (id, departmentData) => {
    const response = await api.put(`/departments/${id}`, departmentData);
    return response.data;
  },

  deleteDepartment: async (id) => {
    const response = await api.delete(`/departments/${id}`);
    return response.data;
  },
};

export default departmentService;
