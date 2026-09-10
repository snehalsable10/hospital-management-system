import api from './api';

const medicalHistoryService = {
  // Get all medical histories
  getAllMedicalHistories: async () => {
    try {
      const response = await api.get('/medical-histories');
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  // Get a specific medical history by ID
  getMedicalHistoryById: async (id) => {
    try {
      const response = await api.get(`/medical-histories/${id}`);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  // Get all medical histories for a patient
  getMedicalHistoriesByPatient: async (patientId) => {
    try {
      const response = await api.get(`/medical-histories/patient/${patientId}`);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  // Get medical histories by status
  getMedicalHistoriesByStatus: async (status) => {
    try {
      const response = await api.get(`/medical-histories/status/${status}`);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  // Search medical histories by condition name
  searchByConditionName: async (conditionName) => {
    try {
      const response = await api.get(`/medical-histories/search/${conditionName}`);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  // Create a new medical history entry
  createMedicalHistory: async (medicalHistoryData) => {
    try {
      const response = await api.post('/medical-histories', medicalHistoryData);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  // Update an existing medical history entry
  updateMedicalHistory: async (id, medicalHistoryData) => {
    try {
      const response = await api.put(`/medical-histories/${id}`, medicalHistoryData);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  // Delete a medical history entry
  deleteMedicalHistory: async (id) => {
    try {
      const response = await api.delete(`/medical-histories/${id}`);
      return response.data;
    } catch (error) {
      throw error;
    }
  },
};

export default medicalHistoryService;