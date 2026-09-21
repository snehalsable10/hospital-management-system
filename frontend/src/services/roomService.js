import api from './api';

/**
 * Rooms carry a bed count: `capacity` total and `occupiedBeds` taken. Those
 * are not editable through the room form - occupy and vacate move them one bed
 * at a time, and the service derives the status from them.
 *
 * "Available" can be combined with a type or a ward, which is why those three
 * endpoints exist as well. Paginated calls put a PageResponse in `data`:
 *   { content, pageNumber, pageSize, totalElements, totalPages }
 */
const roomService = {
  getAllRooms: async () => {
    const response = await api.get('/rooms');
    return response.data;
  },

  getAllRoomsPaginated: async (pageNumber = 0, pageSize = 10) => {
    const response = await api.get('/rooms/paginated', {
      params: { pageNumber, pageSize },
    });
    return response.data;
  },

  getRoomById: async (id) => {
    const response = await api.get(`/rooms/${id}`);
    return response.data;
  },

  getRoomsByType: async (roomType) => {
    const response = await api.get(`/rooms/type/${encodeURIComponent(roomType)}`);
    return response.data;
  },

  getRoomsByTypePaginated: async (roomType, pageNumber = 0, pageSize = 10) => {
    const response = await api.get(
      `/rooms/type/${encodeURIComponent(roomType)}/paginated`,
      { params: { pageNumber, pageSize } }
    );
    return response.data;
  },

  getRoomsByWard: async (ward) => {
    const response = await api.get(`/rooms/ward/${encodeURIComponent(ward)}`);
    return response.data;
  },

  getRoomsByWardPaginated: async (ward, pageNumber = 0, pageSize = 10) => {
    const response = await api.get(`/rooms/ward/${encodeURIComponent(ward)}/paginated`, {
      params: { pageNumber, pageSize },
    });
    return response.data;
  },

  getRoomsByStatus: async (status) => {
    const response = await api.get(`/rooms/status/${encodeURIComponent(status)}`);
    return response.data;
  },

  getRoomsByStatusPaginated: async (status, pageNumber = 0, pageSize = 10) => {
    const response = await api.get(
      `/rooms/status/${encodeURIComponent(status)}/paginated`,
      { params: { pageNumber, pageSize } }
    );
    return response.data;
  },

  getAvailableRooms: async () => {
    const response = await api.get('/rooms/available');
    return response.data;
  },

  getAvailableRoomsPaginated: async (pageNumber = 0, pageSize = 10) => {
    const response = await api.get('/rooms/available/paginated', {
      params: { pageNumber, pageSize },
    });
    return response.data;
  },

  getAvailableRoomsByType: async (roomType) => {
    const response = await api.get(
      `/rooms/available/type/${encodeURIComponent(roomType)}`
    );
    return response.data;
  },

  getAvailableRoomsByTypePaginated: async (roomType, pageNumber = 0, pageSize = 10) => {
    const response = await api.get(
      `/rooms/available/type/${encodeURIComponent(roomType)}/paginated`,
      { params: { pageNumber, pageSize } }
    );
    return response.data;
  },

  getAvailableRoomsByWard: async (ward) => {
    const response = await api.get(`/rooms/available/ward/${encodeURIComponent(ward)}`);
    return response.data;
  },

  getAvailableRoomsByWardPaginated: async (ward, pageNumber = 0, pageSize = 10) => {
    const response = await api.get(
      `/rooms/available/ward/${encodeURIComponent(ward)}/paginated`,
      { params: { pageNumber, pageSize } }
    );
    return response.data;
  },

  createRoom: async (roomData) => {
    const response = await api.post('/rooms', roomData);
    return response.data;
  },

  updateRoom: async (id, roomData) => {
    const response = await api.put(`/rooms/${id}`, roomData);
    return response.data;
  },

  deleteRoom: async (id) => {
    const response = await api.delete(`/rooms/${id}`);
    return response.data;
  },

  // Bed moves are POST, not PUT - they are actions, not edits.
  occupyBed: async (id) => {
    const response = await api.post(`/rooms/${id}/occupy`);
    return response.data;
  },

  vacateBed: async (id) => {
    const response = await api.post(`/rooms/${id}/vacate`);
    return response.data;
  },
};

export default roomService;
