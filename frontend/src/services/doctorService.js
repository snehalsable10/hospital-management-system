import api from './api';

const doctorService = {
  // Get all doctors
  getAllDoctors: async () => {
    try {
      const response = await api.get('/doctors');
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  // Get a specific doctor by ID
  getDoctorById: async (id) => {
    try {
      const response = await api.get(`/doctors/${id}`);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  // Get all doctors in a department
  getDoctorsByDepartment: async (departmentId) => {
    try {
      const response = await api.get(`/doctors/department/${departmentId}`);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  // Get doctors by specialization
  getDoctorsBySpecialization: async (specialization) => {
    try {
      const response = await api.get(`/doctors/specialization/${specialization}`);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  // Create a new doctor
  createDoctor: async (doctorData) => {
    try {
      const response = await api.post('/doctors', doctorData);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  // Update an existing doctor
  updateDoctor: async (id, doctorData) => {
    try {
      const response = await api.put(`/doctors/${id}`, doctorData);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  // Delete a doctor
  deleteDoctor: async (id) => {
    try {
      const response = await api.delete(`/doctors/${id}`);
      return response.data;
    } catch (error) {
      throw error;
    }
  },
};

export default doctorService;