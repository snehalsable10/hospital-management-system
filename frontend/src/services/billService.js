import api from './api';

/**
 * A bill belongs to a patient and the doctor who saw them, and breaks down
 * into four fee columns whose sum is the total. It can be narrowed by patient,
 * doctor, status or date range, and there is a dedicated unpaid list per
 * patient. Paginated calls put a PageResponse in `data`:
 *   { content, pageNumber, pageSize, totalElements, totalPages }
 */
const billService = {
  getAllBills: async () => {
    const response = await api.get('/bills');
    return response.data;
  },

  getAllBillsPaginated: async (pageNumber = 0, pageSize = 10) => {
    const response = await api.get('/bills/paginated', {
      params: { pageNumber, pageSize },
    });
    return response.data;
  },

  getBillById: async (id) => {
    const response = await api.get(`/bills/${id}`);
    return response.data;
  },

  getBillsByPatient: async (patientId) => {
    const response = await api.get(`/bills/patient/${patientId}`);
    return response.data;
  },

  getBillsByPatientPaginated: async (patientId, pageNumber = 0, pageSize = 10) => {
    const response = await api.get(`/bills/patient/${patientId}/paginated`, {
      params: { pageNumber, pageSize },
    });
    return response.data;
  },

  getBillsByDoctor: async (doctorId) => {
    const response = await api.get(`/bills/doctor/${doctorId}`);
    return response.data;
  },

  getBillsByDoctorPaginated: async (doctorId, pageNumber = 0, pageSize = 10) => {
    const response = await api.get(`/bills/doctor/${doctorId}/paginated`, {
      params: { pageNumber, pageSize },
    });
    return response.data;
  },

  getBillsByStatus: async (status) => {
    const response = await api.get(`/bills/status/${encodeURIComponent(status)}`);
    return response.data;
  },

  getBillsByStatusPaginated: async (status, pageNumber = 0, pageSize = 10) => {
    const response = await api.get(
      `/bills/status/${encodeURIComponent(status)}/paginated`,
      { params: { pageNumber, pageSize } }
    );
    return response.data;
  },

  getBillsByDateRange: async (startDate, endDate) => {
    const response = await api.get('/bills/daterange', {
      params: { startDate, endDate },
    });
    return response.data;
  },

  getBillsByDateRangePaginated: async (
    startDate,
    endDate,
    pageNumber = 0,
    pageSize = 10
  ) => {
    const response = await api.get('/bills/daterange/paginated', {
      params: { startDate, endDate, pageNumber, pageSize },
    });
    return response.data;
  },

  getUnpaidBillsByPatient: async (patientId) => {
    const response = await api.get(`/bills/patient/${patientId}/unpaid`);
    return response.data;
  },

  getUnpaidBillsByPatientPaginated: async (patientId, pageNumber = 0, pageSize = 10) => {
    const response = await api.get(`/bills/patient/${patientId}/unpaid/paginated`, {
      params: { pageNumber, pageSize },
    });
    return response.data;
  },

  getPaidBillsByDoctor: async (doctorId) => {
    const response = await api.get(`/bills/doctor/${doctorId}/paid`);
    return response.data;
  },

  createBill: async (billData) => {
    const response = await api.post('/bills', billData);
    return response.data;
  },

  updateBill: async (id, billData) => {
    const response = await api.put(`/bills/${id}`, billData);
    return response.data;
  },

  deleteBill: async (id) => {
    const response = await api.delete(`/bills/${id}`);
    return response.data;
  },
};

export default billService;
