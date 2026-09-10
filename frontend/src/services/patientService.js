import api from './api';

const patientService = {
  // Get all patients
  getAllPatients: async () => {
    try {
      const response = await api.get('/patients');
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  // Get a specific patient by ID
  getPatientById: async (id) => {
    try {
      const response = await api.get(`/patients/${id}`);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  // Search patients by first name
  searchByFirstName: async (firstName) => {
    try {
      const response = await api.get(`/patients/search/firstname/${firstName}`);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  // Search patients by last name
  searchByLastName: async (lastName) => {
    try {
      const response = await api.get(`/patients/search/lastname/${lastName}`);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  // Search patients by city
  searchByCity: async (city) => {
    try {
      const response = await api.get(`/patients/search/city/${city}`);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  // Create a new patient
  createPatient: async (patientData) => {
    try {
      const response = await api.post('/patients', patientData);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  // Update an existing patient
  updatePatient: async (id, patientData) => {
    try {
      const response = await api.put(`/patients/${id}`, patientData);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  // Delete a patient
  deletePatient: async (id) => {
    try {
      const response = await api.delete(`/patients/${id}`);
      return response.data;
    } catch (error) {
      throw error;
    }
  },
};

export default patientService;