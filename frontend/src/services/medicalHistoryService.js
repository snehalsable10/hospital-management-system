import api from './api';

/**
 * Medical histories hang off a patient only - there is no doctor reference,
 * just a free-text doctorNotes field. They can be narrowed by patient, status
 * or a condition-name search. Paginated calls put a PageResponse in `data`:
 *   { content, pageNumber, pageSize, totalElements, totalPages }
 */
const medicalHistoryService = {
  getAllMedicalHistories: async () => {
    const response = await api.get('/medical-histories');
    return response.data;
  },

  getAllMedicalHistoriesPaginated: async (pageNumber = 0, pageSize = 10) => {
    const response = await api.get('/medical-histories/paginated', {
      params: { pageNumber, pageSize },
    });
    return response.data;
  },

  getMedicalHistoryById: async (id) => {
    const response = await api.get(`/medical-histories/${id}`);
    return response.data;
  },

  getMedicalHistoriesByPatient: async (patientId) => {
    const response = await api.get(`/medical-histories/patient/${patientId}`);
    return response.data;
  },

  getMedicalHistoriesByPatientPaginated: async (
    patientId,
    pageNumber = 0,
    pageSize = 10
  ) => {
    const response = await api.get(`/medical-histories/patient/${patientId}/paginated`, {
      params: { pageNumber, pageSize },
    });
    return response.data;
  },

  getMedicalHistoriesByStatus: async (status) => {
    const response = await api.get(
      `/medical-histories/status/${encodeURIComponent(status)}`
    );
    return response.data;
  },

  getMedicalHistoriesByStatusPaginated: async (status, pageNumber = 0, pageSize = 10) => {
    const response = await api.get(
      `/medical-histories/status/${encodeURIComponent(status)}/paginated`,
      { params: { pageNumber, pageSize } }
    );
    return response.data;
  },

  searchByConditionName: async (conditionName) => {
    const response = await api.get(
      `/medical-histories/search/${encodeURIComponent(conditionName)}`
    );
    return response.data;
  },

  searchByConditionNamePaginated: async (conditionName, pageNumber = 0, pageSize = 10) => {
    const response = await api.get(
      `/medical-histories/search/${encodeURIComponent(conditionName)}/paginated`,
      { params: { pageNumber, pageSize } }
    );
    return response.data;
  },

  createMedicalHistory: async (medicalHistoryData) => {
    const response = await api.post('/medical-histories', medicalHistoryData);
    return response.data;
  },

  updateMedicalHistory: async (id, medicalHistoryData) => {
    const response = await api.put(`/medical-histories/${id}`, medicalHistoryData);
    return response.data;
  },

  deleteMedicalHistory: async (id) => {
    const response = await api.delete(`/medical-histories/${id}`);
    return response.data;
  },
};

export default medicalHistoryService;
