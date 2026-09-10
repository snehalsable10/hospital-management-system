import React, { useState, useEffect } from 'react';
import roomService from '../services/roomService';

const RoomManagement = () => {
  const [rooms, setRooms] = useState([]);
  const [formData, setFormData] = useState({
    roomNumber: '',
    roomType: 'General',
    ward: '',
    capacity: '',
    costPerDay: '',
    status: 'AVAILABLE',
    description: '',
    amenities: ''
  });
  const [editingId, setEditingId] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [filterType, setFilterType] = useState('');
  const [filterWard, setFilterWard] = useState('');
  const [filterStatus, setFilterStatus] = useState('');

  useEffect(() => {
    loadRooms();
  }, []);

  const loadRooms = async () => {
    try {
      setLoading(true);
      const response = await roomService.getAllRooms();
      setRooms(response.data || []);
      setError('');
    } catch (err) {
      setError('Failed to load rooms');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleFilterChange = (e) => {
    const { name, value } = e.target;
    if (name === 'filterType') setFilterType(value);
    else if (name === 'filterWard') setFilterWard(value);
    else if (name === 'filterStatus') setFilterStatus(value);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!formData.roomNumber || !formData.ward || !formData.capacity || !formData.costPerDay) {
      setError('Room number, ward, capacity, and cost are required');
      return;
    }

    try {
      setLoading(true);
      const roomData = {
        ...formData,
        capacity: parseInt(formData.capacity),
        costPerDay: parseFloat(formData.costPerDay)
      };

      if (editingId) {
        await roomService.updateRoom(editingId, roomData);
        setSuccess('Room updated successfully');
      } else {
        await roomService.createRoom(roomData);
        setSuccess('Room created successfully');
      }
      resetForm();
      loadRooms();
      setError('');
    } catch (err) {
      setError('Failed to save room: ' + err.response?.data?.message || err.message);
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleEdit = (room) => {
    setFormData({
      roomNumber: room.roomNumber,
      roomType: room.roomType,
      ward: room.ward,
      capacity: room.capacity,
      costPerDay: room.costPerDay,
      status: room.status,
      description: room.description || '',
      amenities: room.amenities || ''
    });
    setEditingId(room.id);
  };

  const handleDelete = async (id) => {
    if (window.confirm('Are you sure you want to delete this room?')) {
      try {
        setLoading(true);
        await roomService.deleteRoom(id);
        setSuccess('Room deleted successfully');
        loadRooms();
        setError('');
      } catch (err) {
        setError('Failed to delete room: ' + err.response?.data?.message || err.message);
        console.error(err);
      } finally {
        setLoading(false);
      }
    }
  };

  const handleOccupyBed = async (roomId) => {
    try {
      setLoading(true);
      await roomService.occupyBed(roomId);
      setSuccess('Bed occupied successfully');
      loadRooms();
      setError('');
    } catch (err) {
      setError('Failed to occupy bed: ' + err.response?.data?.message || err.message);
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleVacateBed = async (roomId) => {
    try {
      setLoading(true);
      await roomService.vacateBed(roomId);
      setSuccess('Bed vacated successfully');
      loadRooms();
      setError('');
    } catch (err) {
      setError('Failed to vacate bed: ' + err.response?.data?.message || err.message);
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const resetForm = () => {
    setFormData({
      roomNumber: '',
      roomType: 'General',
      ward: '',
      capacity: '',
      costPerDay: '',
      status: 'AVAILABLE',
      description: '',
      amenities: ''
    });
    setEditingId(null);
  };

  const getStatusBadge = (status) => {
    const badges = {
      AVAILABLE: 'success',
      FULL: 'danger',
      MAINTENANCE: 'warning'
    };
    return badges[status] || 'secondary';
  };

  const getOccupancyPercentage = (occupiedBeds, capacity) => {
    return Math.round((occupiedBeds / capacity) * 100);
  };

  const filteredRooms = rooms.filter(room => {
    if (filterType && room.roomType !== filterType) return false;
    if (filterWard && room.ward !== filterWard) return false;
    if (filterStatus && room.status !== filterStatus) return false;
    return true;
  });

  return (
    <div className="container-fluid mt-4">
      <h1 className="mb-4">Room & Bed Management</h1>

      {error && <div className="alert alert-danger">{error}</div>}
      {success && <div className="alert alert-success">{success}</div>}

      <div className="card mb-4">
        <div className="card-header">
          <h5>{editingId ? 'Edit Room' : 'Add New Room'}</h5>
        </div>
        <div className="card-body">
          <form onSubmit={handleSubmit}>
            <div className="row">
              <div className="col-md-6 mb-3">
                <label className="form-label">Room Number *</label>
                <input
                  type="text"
                  name="roomNumber"
                  className="form-control"
                  value={formData.roomNumber}
                  onChange={handleInputChange}
                  placeholder="e.g., 101, ICU-5"
                  required
                />
              </div>

              <div className="col-md-6 mb-3">
                <label className="form-label">Room Type *</label>
                <select
                  name="roomType"
                  className="form-control"
                  value={formData.roomType}
                  onChange={handleInputChange}
                  required
                >
                  <option value="General">General</option>
                  <option value="ICU">ICU</option>
                  <option value="Private">Private</option>
                  <option value="Semi-Private">Semi-Private</option>
                </select>
              </div>
            </div>

            <div className="row">
              <div className="col-md-6 mb-3">
                <label className="form-label">Ward *</label>
                <input
                  type="text"
                  name="ward"
                  className="form-control"
                  value={formData.ward}
                  onChange={handleInputChange}
                  placeholder="e.g., Cardiology, Orthopedics"
                  required
                />
              </div>

              <div className="col-md-6 mb-3">
                <label className="form-label">Status</label>
                <select
                  name="status"
                  className="form-control"
                  value={formData.status}
                  onChange={handleInputChange}
                >
                  <option value="AVAILABLE">Available</option>
                  <option value="FULL">Full</option>
                  <option value="MAINTENANCE">Maintenance</option>
                </select>
              </div>
            </div>

            <div className="row">
              <div className="col-md-6 mb-3">
                <label className="form-label">Capacity (Beds) *</label>
                <input
                  type="number"
                  name="capacity"
                  className="form-control"
                  value={formData.capacity}
                  onChange={handleInputChange}
                  placeholder="Number of beds"
                  min="1"
                  required
                />
              </div>

              <div className="col-md-6 mb-3">
                <label className="form-label">Cost Per Day ($) *</label>
                <input
                  type="number"
                  name="costPerDay"
                  className="form-control"
                  value={formData.costPerDay}
                  onChange={handleInputChange}
                  placeholder="0.00"
                  step="0.01"
                  min="0"
                  required
                />
              </div>
            </div>

            <div className="row">
              <div className="col-md-12 mb-3">
                <label className="form-label">Description</label>
                <textarea
                  name="description"
                  className="form-control"
                  value={formData.description}
                  onChange={handleInputChange}
                  rows="2"
                  placeholder="Room description and notes"
                />
              </div>
            </div>

            <div className="mb-3">
              <label className="form-label">Amenities</label>
              <input
                type="text"
                name="amenities"
                className="form-control"
                value={formData.amenities}
                onChange={handleInputChange}
                placeholder="e.g., WiFi, AC, TV, Bathroom"
              />
            </div>

            <div className="d-flex gap-2">
              <button type="submit" className="btn btn-primary" disabled={loading}>
                {loading ? 'Saving...' : editingId ? 'Update Room' : 'Create Room'}
              </button>
              {editingId && (
                <button type="button" className="btn btn-secondary" onClick={resetForm}>
                  Cancel
                </button>
              )}
            </div>
          </form>
        </div>
      </div>

      <div className="card mb-4">
        <div className="card-header">
          <h5>Filter Rooms</h5>
        </div>
        <div className="card-body">
          <div className="row">
            <div className="col-md-4 mb-3">
              <label className="form-label">Room Type</label>
              <select
                name="filterType"
                className="form-control"
                value={filterType}
                onChange={handleFilterChange}
              >
                <option value="">All Types</option>
                <option value="General">General</option>
                <option value="ICU">ICU</option>
                <option value="Private">Private</option>
                <option value="Semi-Private">Semi-Private</option>
              </select>
            </div>

            <div className="col-md-4 mb-3">
              <label className="form-label">Ward</label>
              <input
                type="text"
                name="filterWard"
                className="form-control"
                value={filterWard}
                onChange={handleFilterChange}
                placeholder="Filter by ward"
              />
            </div>

            <div className="col-md-4 mb-3">
              <label className="form-label">Status</label>
              <select
                name="filterStatus"
                className="form-control"
                value={filterStatus}
                onChange={handleFilterChange}
              >
                <option value="">All Status</option>
                <option value="AVAILABLE">Available</option>
                <option value="FULL">Full</option>
                <option value="MAINTENANCE">Maintenance</option>
              </select>
            </div>
          </div>
        </div>
      </div>

      <div className="card">
        <div className="card-header">
          <h5>Rooms List ({filteredRooms.length} rooms)</h5>
        </div>
        <div className="card-body">
          {loading && <p>Loading...</p>}
          {filteredRooms.length === 0 ? (
            <p className="text-muted">No rooms found</p>
          ) : (
            <div className="table-responsive">
              <table className="table table-hover">
                <thead className="table-light">
                  <tr>
                    <th>Room #</th>
                    <th>Type</th>
                    <th>Ward</th>
                    <th>Capacity</th>
                    <th>Occupied</th>
                    <th>Occupancy %</th>
                    <th>Cost/Day</th>
                    <th>Status</th>
                    <th>Amenities</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredRooms.map(room => (
                    <tr key={room.id}>
                      <td><strong>{room.roomNumber}</strong></td>
                      <td>{room.roomType}</td>
                      <td>{room.ward}</td>
                      <td>{room.capacity}</td>
                      <td>{room.occupiedBeds}</td>
                      <td>
                        <div className="progress" style={{ height: '20px' }}>
                          <div
                            className="progress-bar"
                            role="progressbar"
                            style={{ width: `${getOccupancyPercentage(room.occupiedBeds, room.capacity)}%` }}
                          >
                            {getOccupancyPercentage(room.occupiedBeds, room.capacity)}%
                          </div>
                        </div>
                      </td>
                      <td>${room.costPerDay}</td>
                      <td>
                        <span className={`badge bg-${getStatusBadge(room.status)}`}>
                          {room.status}
                        </span>
                      </td>
                      <td>{room.amenities || '-'}</td>
                      <td>
                        <button
                          className="btn btn-sm btn-success me-1"
                          onClick={() => handleOccupyBed(room.id)}
                          disabled={loading || room.occupiedBeds >= room.capacity}
                          title="Mark bed as occupied"
                        >
                          Occupy
                        </button>
                        <button
                          className="btn btn-sm btn-info me-1"
                          onClick={() => handleVacateBed(room.id)}
                          disabled={loading || room.occupiedBeds === 0}
                          title="Mark bed as vacant"
                        >
                          Vacate
                        </button>
                        <button
                          className="btn btn-sm btn-warning me-1"
                          onClick={() => handleEdit(room)}
                          disabled={loading}
                        >
                          Edit
                        </button>
                        <button
                          className="btn btn-sm btn-danger"
                          onClick={() => handleDelete(room.id)}
                          disabled={loading}
                        >
                          Delete
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default RoomManagement;