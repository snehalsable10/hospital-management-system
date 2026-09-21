import api from './api';

/**
 * Laboratory tests hang off a patient and carry a result value, its unit and
 * the reference range it should fall in. They can be narrowed by patient,
 * status, test-name search or date range. Paginated calls put a PageResponse
 * in `data`: { content, pageNumber, pageSize, totalElements, totalPages }
 */
const laboratoryTestService = {
  getAllLaboratoryTests: async () => {
    const response = await api.get('/laboratory-tests');
    return response.data;
  },

  getAllLaboratoryTestsPaginated: async (pageNumber = 0, pageSize = 10) => {
    const response = await api.get('/laboratory-tests/paginated', {
      params: { pageNumber, pageSize },
    });
    return response.data;
  },

  getLaboratoryTestById: async (id) => {
    const response = await api.get(`/laboratory-tests/${id}`);
    return response.data;
  },

  getLaboratoryTestsByPatient: async (patientId) => {
    const response = await api.get(`/laboratory-tests/patient/${patientId}`);
    return response.data;
  },

  getLaboratoryTestsByPatientPaginated: async (
    patientId,
    pageNumber = 0,
    pageSize = 10
  ) => {
    const response = await api.get(`/laboratory-tests/patient/${patientId}/paginated`, {
      params: { pageNumber, pageSize },
    });
    return response.data;
  },

  getLaboratoryTestsByStatus: async (status) => {
    const response = await api.get(
      `/laboratory-tests/status/${encodeURIComponent(status)}`
    );
    return response.data;
  },

  getLaboratoryTestsByStatusPaginated: async (status, pageNumber = 0, pageSize = 10) => {
    const response = await api.get(
      `/laboratory-tests/status/${encodeURIComponent(status)}/paginated`,
      { params: { pageNumber, pageSize } }
    );
    return response.data;
  },

  searchByTestName: async (testName) => {
    const response = await api.get(
      `/laboratory-tests/search/${encodeURIComponent(testName)}`
    );
    return response.data;
  },

  searchByTestNamePaginated: async (testName, pageNumber = 0, pageSize = 10) => {
    const response = await api.get(
      `/laboratory-tests/search/${encodeURIComponent(testName)}/paginated`,
      { params: { pageNumber, pageSize } }
    );
    return response.data;
  },

  getLaboratoryTestsByDateRange: async (startDate, endDate) => {
    const response = await api.get('/laboratory-tests/daterange', {
      params: { startDate, endDate },
    });
    return response.data;
  },

  getLaboratoryTestsByDateRangePaginated: async (
    startDate,
    endDate,
    pageNumber = 0,
    pageSize = 10
  ) => {
    const response = await api.get('/laboratory-tests/daterange/paginated', {
      params: { startDate, endDate, pageNumber, pageSize },
    });
    return response.data;
  },

  createLaboratoryTest: async (laboratoryTestData) => {
    const response = await api.post('/laboratory-tests', laboratoryTestData);
    return response.data;
  },

  updateLaboratoryTest: async (id, laboratoryTestData) => {
    const response = await api.put(`/laboratory-tests/${id}`, laboratoryTestData);
    return response.data;
  },

  deleteLaboratoryTest: async (id) => {
    const response = await api.delete(`/laboratory-tests/${id}`);
    return response.data;
  },
};

export default laboratoryTestService;
