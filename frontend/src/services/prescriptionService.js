import api from './api';

const prescriptionService = {
  // Get all prescriptions
  getAllPrescriptions: async () => {
    try {
      const response = await api.get('/prescriptions');
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  // Get a specific prescription by ID
  getPrescriptionById: async (id) => {
    try {
      const response = await api.get(`/prescriptions/${id}`);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  // Get all prescriptions for an appointment
  getPrescriptionsByAppointment: async (appointmentId) => {
    try {
      const response = await api.get(`/prescriptions/appointment/${appointmentId}`);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  // Get all prescriptions for a patient
  getPrescriptionsByPatient: async (patientId) => {
    try {
      const response = await api.get(`/prescriptions/patient/${patientId}`);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  // Get all prescriptions written by a doctor
  getPrescriptionsByDoctor: async (doctorId) => {
    try {
      const response = await api.get(`/prescriptions/doctor/${doctorId}`);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  // Get prescriptions by status
  getPrescriptionsByStatus: async (status) => {
    try {
      const response = await api.get(`/prescriptions/status/${status}`);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  // Create a new prescription
  createPrescription: async (prescriptionData) => {
    try {
      const response = await api.post('/prescriptions', prescriptionData);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  // Update an existing prescription
  updatePrescription: async (id, prescriptionData) => {
    try {
      const response = await api.put(`/prescriptions/${id}`, prescriptionData);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  // Delete a prescription
  deletePrescription: async (id) => {
    try {
      const response = await api.delete(`/prescriptions/${id}`);
      return response.data;
    } catch (error) {
      throw error;
    }
  },
};

export default prescriptionService;