import api from './api';

const laboratoryTestService = {
  getAllLaboratoryTests: async () => {
    try {
      const response = await api.get('/laboratory-tests');
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  getLaboratoryTestById: async (id) => {
    try {
      const response = await api.get(`/laboratory-tests/${id}`);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  getLaboratoryTestsByPatient: async (patientId) => {
    try {
      const response = await api.get(`/laboratory-tests/patient/${patientId}`);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  getLaboratoryTestsByStatus: async (status) => {
    try {
      const response = await api.get(`/laboratory-tests/status/${status}`);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  searchByTestName: async (testName) => {
    try {
      const response = await api.get(`/laboratory-tests/search/${testName}`);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  getLaboratoryTestsByDateRange: async (startDate, endDate) => {
    try {
      const response = await api.get('/laboratory-tests/daterange', {
        params: {
          startDate,
          endDate
        }
      });
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  createLaboratoryTest: async (testData) => {
    try {
      const response = await api.post('/laboratory-tests', testData);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  updateLaboratoryTest: async (id, testData) => {
    try {
      const response = await api.put(`/laboratory-tests/${id}`, testData);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  deleteLaboratoryTest: async (id) => {
    try {
      const response = await api.delete(`/laboratory-tests/${id}`);
      return response.data;
    } catch (error) {
      throw error;
    }
  }
};

export default laboratoryTestService;