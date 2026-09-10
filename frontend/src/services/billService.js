import api from './api';

const billService = {
  getAllBills: async () => {
    try {
      const response = await api.get('/bills');
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  getBillById: async (id) => {
    try {
      const response = await api.get(`/bills/${id}`);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  getBillsByPatient: async (patientId) => {
    try {
      const response = await api.get(`/bills/patient/${patientId}`);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  getBillsByDoctor: async (doctorId) => {
    try {
      const response = await api.get(`/bills/doctor/${doctorId}`);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  getBillsByStatus: async (status) => {
    try {
      const response = await api.get(`/bills/status/${status}`);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  getBillsByDateRange: async (startDate, endDate) => {
    try {
      const response = await api.get('/bills/daterange', {
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

  getPatientUnpaidBills: async (patientId) => {
    try {
      const response = await api.get(`/bills/patient/${patientId}/unpaid`);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  getDoctorPaidBills: async (doctorId) => {
    try {
      const response = await api.get(`/bills/doctor/${doctorId}/paid`);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  createBill: async (billData) => {
    try {
      const response = await api.post('/bills', billData);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  updateBill: async (id, billData) => {
    try {
      const response = await api.put(`/bills/${id}`, billData);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  deleteBill: async (id) => {
    try {
      const response = await api.delete(`/bills/${id}`);
      return response.data;
    } catch (error) {
      throw error;
    }
  }
};

export default billService;