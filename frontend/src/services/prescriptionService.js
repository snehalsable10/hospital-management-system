import api from './api';

/**
 * Prescriptions hang off an appointment and carry the patient and doctor of
 * that appointment. They can be listed in full or narrowed by appointment,
 * patient, doctor or status. Paginated calls put a PageResponse in `data`:
 *   { content, pageNumber, pageSize, totalElements, totalPages }
 */
const prescriptionService = {
  getAllPrescriptions: async () => {
    const response = await api.get('/prescriptions');
    return response.data;
  },

  getAllPrescriptionsPaginated: async (pageNumber = 0, pageSize = 10) => {
    const response = await api.get('/prescriptions/paginated', {
      params: { pageNumber, pageSize },
    });
    return response.data;
  },

  getPrescriptionById: async (id) => {
    const response = await api.get(`/prescriptions/${id}`);
    return response.data;
  },

  getPrescriptionsByAppointment: async (appointmentId) => {
    const response = await api.get(`/prescriptions/appointment/${appointmentId}`);
    return response.data;
  },

  getPrescriptionsByAppointmentPaginated: async (
    appointmentId,
    pageNumber = 0,
    pageSize = 10
  ) => {
    const response = await api.get(
      `/prescriptions/appointment/${appointmentId}/paginated`,
      { params: { pageNumber, pageSize } }
    );
    return response.data;
  },

  getPrescriptionsByPatient: async (patientId) => {
    const response = await api.get(`/prescriptions/patient/${patientId}`);
    return response.data;
  },

  getPrescriptionsByPatientPaginated: async (patientId, pageNumber = 0, pageSize = 10) => {
    const response = await api.get(`/prescriptions/patient/${patientId}/paginated`, {
      params: { pageNumber, pageSize },
    });
    return response.data;
  },

  getPrescriptionsByDoctor: async (doctorId) => {
    const response = await api.get(`/prescriptions/doctor/${doctorId}`);
    return response.data;
  },

  getPrescriptionsByDoctorPaginated: async (doctorId, pageNumber = 0, pageSize = 10) => {
    const response = await api.get(`/prescriptions/doctor/${doctorId}/paginated`, {
      params: { pageNumber, pageSize },
    });
    return response.data;
  },

  getPrescriptionsByStatus: async (status) => {
    const response = await api.get(`/prescriptions/status/${encodeURIComponent(status)}`);
    return response.data;
  },

  getPrescriptionsByStatusPaginated: async (status, pageNumber = 0, pageSize = 10) => {
    const response = await api.get(
      `/prescriptions/status/${encodeURIComponent(status)}/paginated`,
      { params: { pageNumber, pageSize } }
    );
    return response.data;
  },

  createPrescription: async (prescriptionData) => {
    const response = await api.post('/prescriptions', prescriptionData);
    return response.data;
  },

  updatePrescription: async (id, prescriptionData) => {
    const response = await api.put(`/prescriptions/${id}`, prescriptionData);
    return response.data;
  },

  deletePrescription: async (id) => {
    const response = await api.delete(`/prescriptions/${id}`);
    return response.data;
  },
};

export default prescriptionService;
