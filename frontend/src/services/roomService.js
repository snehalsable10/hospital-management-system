import api from './api';

const roomService = {
  getAllRooms: async () => {
    try {
      const response = await api.get('/rooms');
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  getRoomById: async (id) => {
    try {
      const response = await api.get(`/rooms/${id}`);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  getRoomsByType: async (roomType) => {
    try {
      const response = await api.get(`/rooms/type/${roomType}`);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  getRoomsByWard: async (ward) => {
    try {
      const response = await api.get(`/rooms/ward/${ward}`);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  getRoomsByStatus: async (status) => {
    try {
      const response = await api.get(`/rooms/status/${status}`);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  getAvailableRooms: async () => {
    try {
      const response = await api.get('/rooms/available');
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  getAvailableRoomsByType: async (roomType) => {
    try {
      const response = await api.get(`/rooms/available/type/${roomType}`);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  getAvailableRoomsByWard: async (ward) => {
    try {
      const response = await api.get(`/rooms/available/ward/${ward}`);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  createRoom: async (roomData) => {
    try {
      const response = await api.post('/rooms', roomData);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  updateRoom: async (id, roomData) => {
    try {
      const response = await api.put(`/rooms/${id}`, roomData);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  deleteRoom: async (id) => {
    try {
      const response = await api.delete(`/rooms/${id}`);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  occupyBed: async (roomId) => {
    try {
      const response = await api.post(`/rooms/${roomId}/occupy`);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  vacateBed: async (roomId) => {
    try {
      const response = await api.post(`/rooms/${roomId}/vacate`);
      return response.data;
    } catch (error) {
      throw error;
    }
  }
};

export default roomService;