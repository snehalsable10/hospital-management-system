import api from './api';

/**
 * Every call returns the ApiResponse envelope: { message, data, success }.
 * Paginated calls put a PageResponse in `data`:
 * { content, pageNumber, pageSize, totalElements, totalPages, ... }
 */
const patientService = {
  getAllPatients: async () => {
    const response = await api.get('/patients');
    return response.data;
  },

  getAllPatientsPaginated: async (pageNumber = 0, pageSize = 10) => {
    const response = await api.get('/patients/paginated', {
      params: { pageNumber, pageSize },
    });
    return response.data;
  },

  getPatientById: async (id) => {
    const response = await api.get(`/patients/${id}`);
    return response.data;
  },

  searchByFirstName: async (firstName) => {
    const response = await api.get(`/patients/search/firstname/${encodeURIComponent(firstName)}`);
    return response.data;
  },

  searchByFirstNamePaginated: async (firstName, pageNumber = 0, pageSize = 10) => {
    const response = await api.get(
      `/patients/search/firstname/${encodeURIComponent(firstName)}/paginated`,
      { params: { pageNumber, pageSize } }
    );
    return response.data;
  },

  searchByLastName: async (lastName) => {
    const response = await api.get(`/patients/search/lastname/${encodeURIComponent(lastName)}`);
    return response.data;
  },

  searchByLastNamePaginated: async (lastName, pageNumber = 0, pageSize = 10) => {
    const response = await api.get(
      `/patients/search/lastname/${encodeURIComponent(lastName)}/paginated`,
      { params: { pageNumber, pageSize } }
    );
    return response.data;
  },

  searchByCity: async (city) => {
    const response = await api.get(`/patients/search/city/${encodeURIComponent(city)}`);
    return response.data;
  },

  searchByCityPaginated: async (city, pageNumber = 0, pageSize = 10) => {
    const response = await api.get(
      `/patients/search/city/${encodeURIComponent(city)}/paginated`,
      { params: { pageNumber, pageSize } }
    );
    return response.data;
  },

  createPatient: async (patientData) => {
    const response = await api.post('/patients', patientData);
    return response.data;
  },

  updatePatient: async (id, patientData) => {
    const response = await api.put(`/patients/${id}`, patientData);
    return response.data;
  },

  deletePatient: async (id) => {
    const response = await api.delete(`/patients/${id}`);
    return response.data;
  },
};

export default patientService;
