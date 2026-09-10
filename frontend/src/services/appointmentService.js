import api from './api';

const appointmentService = {
  // Get all appointments
  getAllAppointments: async () => {
    try {
      const response = await api.get('/appointments');
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  // Get a specific appointment by ID
  getAppointmentById: async (id) => {
    try {
      const response = await api.get(`/appointments/${id}`);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  // Get all appointments for a patient
  getAppointmentsByPatient: async (patientId) => {
    try {
      const response = await api.get(`/appointments/patient/${patientId}`);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  // Get all appointments for a doctor
  getAppointmentsByDoctor: async (doctorId) => {
    try {
      const response = await api.get(`/appointments/doctor/${doctorId}`);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  // Get appointments by status
  getAppointmentsByStatus: async (status) => {
    try {
      const response = await api.get(`/appointments/status/${status}`);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  // Get appointments in a date range
  getAppointmentsByDateRange: async (startDate, endDate) => {
    try {
      const response = await api.get(`/appointments/daterange?startDate=${startDate}&endDate=${endDate}`);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  // Create a new appointment
  createAppointment: async (appointmentData) => {
    try {
      const response = await api.post('/appointments', appointmentData);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  // Update an existing appointment
  updateAppointment: async (id, appointmentData) => {
    try {
      const response = await api.put(`/appointments/${id}`, appointmentData);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  // Delete an appointment
  deleteAppointment: async (id) => {
    try {
      const response = await api.delete(`/appointments/${id}`);
      return response.data;
    } catch (error) {
      throw error;
    }
  },
};

export default appointmentService;