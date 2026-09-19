import api from './api';

/**
 * Doctors can be filtered by department or specialisation. There is no
 * name-search endpoint, so the page filters on those two instead.
 */
const doctorService = {
  getAllDoctors: async () => {
    const response = await api.get('/doctors');
    return response.data;
  },

  getAllDoctorsPaginated: async (pageNumber = 0, pageSize = 10) => {
    const response = await api.get('/doctors/paginated', {
      params: { pageNumber, pageSize },
    });
    return response.data;
  },

  getDoctorById: async (id) => {
    const response = await api.get(`/doctors/${id}`);
    return response.data;
  },

  getDoctorsByDepartment: async (departmentId) => {
    const response = await api.get(`/doctors/department/${departmentId}`);
    return response.data;
  },

  getDoctorsByDepartmentPaginated: async (departmentId, pageNumber = 0, pageSize = 10) => {
    const response = await api.get(`/doctors/department/${departmentId}/paginated`, {
      params: { pageNumber, pageSize },
    });
    return response.data;
  },

  getDoctorsBySpecialization: async (specialization) => {
    const response = await api.get(`/doctors/specialization/${encodeURIComponent(specialization)}`);
    return response.data;
  },

  getDoctorsBySpecializationPaginated: async (specialization, pageNumber = 0, pageSize = 10) => {
    const response = await api.get(
      `/doctors/specialization/${encodeURIComponent(specialization)}/paginated`,
      { params: { pageNumber, pageSize } }
    );
    return response.data;
  },

  createDoctor: async (doctorData) => {
    const response = await api.post('/doctors', doctorData);
    return response.data;
  },

  updateDoctor: async (id, doctorData) => {
    const response = await api.put(`/doctors/${id}`, doctorData);
    return response.data;
  },

  deleteDoctor: async (id) => {
    const response = await api.delete(`/doctors/${id}`);
    return response.data;
  },
};

export default doctorService;
