import api from './api';

/**
 * Appointments can be listed in full, or narrowed by status or date range.
 * There is no free-text search endpoint, so the page filters on those two.
 * Paginated calls put a PageResponse in `data`:
 *   { content, pageNumber, pageSize, totalElements, totalPages }
 */
const appointmentService = {
  getAllAppointments: async () => {
    const response = await api.get('/appointments');
    return response.data;
  },

  getAllAppointmentsPaginated: async (pageNumber = 0, pageSize = 10) => {
    const response = await api.get('/appointments/paginated', {
      params: { pageNumber, pageSize },
    });
    return response.data;
  },

  getAppointmentById: async (id) => {
    const response = await api.get(`/appointments/${id}`);
    return response.data;
  },

  getAppointmentsByPatient: async (patientId) => {
    const response = await api.get(`/appointments/patient/${patientId}`);
    return response.data;
  },

  getAppointmentsByPatientPaginated: async (patientId, pageNumber = 0, pageSize = 10) => {
    const response = await api.get(`/appointments/patient/${patientId}/paginated`, {
      params: { pageNumber, pageSize },
    });
    return response.data;
  },

  getAppointmentsByDoctor: async (doctorId) => {
    const response = await api.get(`/appointments/doctor/${doctorId}`);
    return response.data;
  },

  getAppointmentsByDoctorPaginated: async (doctorId, pageNumber = 0, pageSize = 10) => {
    const response = await api.get(`/appointments/doctor/${doctorId}/paginated`, {
      params: { pageNumber, pageSize },
    });
    return response.data;
  },

  getAppointmentsByStatus: async (status) => {
    const response = await api.get(`/appointments/status/${encodeURIComponent(status)}`);
    return response.data;
  },

  getAppointmentsByStatusPaginated: async (status, pageNumber = 0, pageSize = 10) => {
    const response = await api.get(
      `/appointments/status/${encodeURIComponent(status)}/paginated`,
      { params: { pageNumber, pageSize } }
    );
    return response.data;
  },

  getAppointmentsByDateRange: async (startDate, endDate) => {
    const response = await api.get('/appointments/daterange', {
      params: { startDate, endDate },
    });
    return response.data;
  },

  getAppointmentsByDateRangePaginated: async (
    startDate,
    endDate,
    pageNumber = 0,
    pageSize = 10
  ) => {
    const response = await api.get('/appointments/daterange/paginated', {
      params: { startDate, endDate, pageNumber, pageSize },
    });
    return response.data;
  },

  createAppointment: async (appointmentData) => {
    const response = await api.post('/appointments', appointmentData);
    return response.data;
  },

  updateAppointment: async (id, appointmentData) => {
    const response = await api.put(`/appointments/${id}`, appointmentData);
    return response.data;
  },

  deleteAppointment: async (id) => {
    const response = await api.delete(`/appointments/${id}`);
    return response.data;
  },
};

export default appointmentService;
