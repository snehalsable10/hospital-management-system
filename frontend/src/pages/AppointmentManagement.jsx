import { useState, useEffect } from 'react';
import appointmentService from '../services/appointmentService';
import patientService from '../services/patientService';
import doctorService from '../services/doctorService';

function AppointmentManagement() {
  const [appointments, setAppointments] = useState([]);
  const [patients, setPatients] = useState([]);
  const [doctors, setDoctors] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [formData, setFormData] = useState({
    patientId: '',
    doctorId: '',
    appointmentDate: '',
    appointmentTime: '',
    status: 'SCHEDULED',
    reason: '',
    notes: '',
    isActive: true,
  });

  // Fetch all data on mount
  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    setLoading(true);
    setError('');
    try {
      const [appointmentsResult, patientsResult, doctorsResult] = await Promise.all([
        appointmentService.getAllAppointments(),
        patientService.getAllPatients(),
        doctorService.getAllDoctors(),
      ]);

      if (appointmentsResult.success) {
        setAppointments(appointmentsResult.data || []);
      }
      if (patientsResult.success) {
        setPatients(patientsResult.data || []);
      }
      if (doctorsResult.success) {
        setDoctors(doctorsResult.data || []);
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Error loading data');
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData({
      ...formData,
      [name]: type === 'checkbox' ? checked : value,
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');

    try {
      let result;
      if (editingId) {
        result = await appointmentService.updateAppointment(editingId, formData);
      } else {
        result = await appointmentService.createAppointment(formData);
      }

      if (result.success) {
        setSuccess(result.message);
        setShowForm(false);
        setEditingId(null);
        setFormData({
          patientId: '',
          doctorId: '',
          appointmentDate: '',
          appointmentTime: '',
          status: 'SCHEDULED',
          reason: '',
          notes: '',
          isActive: true,
        });
        loadData();
      } else {
        setError(result.message);
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Operation failed');
    }
  };

  const handleEdit = (appointment) => {
    setEditingId(appointment.id);
    setFormData({
      patientId: appointment.patient?.id || '',
      doctorId: appointment.doctor?.id || '',
      appointmentDate: appointment.appointmentDate,
      appointmentTime: appointment.appointmentTime,
      status: appointment.status,
      reason: appointment.reason,
      notes: appointment.notes || '',
      isActive: appointment.isActive,
    });
    setShowForm(true);
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this appointment?')) {
      return;
    }

    setError('');
    setSuccess('');

    try {
      const result = await appointmentService.deleteAppointment(id);
      if (result.success) {
        setSuccess(result.message);
        loadData();
      } else {
        setError(result.message);
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Delete failed');
    }
  };

  const handleCancel = () => {
    setShowForm(false);
    setEditingId(null);
    setFormData({
      patientId: '',
      doctorId: '',
      appointmentDate: '',
      appointmentTime: '',
      status: 'SCHEDULED',
      reason: '',
      notes: '',
      isActive: true,
    });
    setError('');
  };

  const getPatientName = (patientId) => {
    const patient = patients.find((p) => p.id === patientId);
    return patient ? `${patient.firstName} ${patient.lastName}` : 'Unknown';
  };

  const getDoctorName = (doctorId) => {
    const doctor = doctors.find((d) => d.id === doctorId);
    return doctor ? `${doctor.firstName} ${doctor.lastName}` : 'Unknown';
  };

  const getStatusBadge = (status) => {
    const statusColors = {
      SCHEDULED: 'bg-primary',
      COMPLETED: 'bg-success',
      CANCELLED: 'bg-danger',
    };
    return statusColors[status] || 'bg-secondary';
  };

  return (
    <div className="container mt-4">
      <h1 className="mb-4">Appointment Management</h1>

      {error && <div className="alert alert-danger">{error}</div>}
      {success && <div className="alert alert-success">{success}</div>}

      {/* Add/Edit Form */}
      {showForm && (
        <div className="card mb-4 bg-light">
          <div className="card-body">
            <h5>{editingId ? 'Edit Appointment' : 'Book New Appointment'}</h5>
            <form onSubmit={handleSubmit}>
              <div className="row">
                <div className="col-md-6 mb-3">
                  <label className="form-label">Patient *</label>
                  <select
                    name="patientId"
                    className="form-select"
                    value={formData.patientId}
                    onChange={handleChange}
                    required
                  >
                    <option value="">Select a patient</option>
                    {patients.map((patient) => (
                      <option key={patient.id} value={patient.id}>
                        {patient.firstName} {patient.lastName}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="col-md-6 mb-3">
                  <label className="form-label">Doctor *</label>
                  <select
                    name="doctorId"
                    className="form-select"
                    value={formData.doctorId}
                    onChange={handleChange}
                    required
                  >
                    <option value="">Select a doctor</option>
                    {doctors.map((doctor) => (
                      <option key={doctor.id} value={doctor.id}>
                        {doctor.firstName} {doctor.lastName} ({doctor.specialization})
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="row">
                <div className="col-md-6 mb-3">
                  <label className="form-label">Appointment Date *</label>
                  <input
                    type="date"
                    name="appointmentDate"
                    className="form-control"
                    value={formData.appointmentDate}
                    onChange={handleChange}
                    required
                  />
                </div>
                <div className="col-md-6 mb-3">
                  <label className="form-label">Appointment Time *</label>
                  <input
                    type="time"
                    name="appointmentTime"
                    className="form-control"
                    value={formData.appointmentTime}
                    onChange={handleChange}
                    required
                  />
                </div>
              </div>

              <div className="row">
                <div className="col-md-6 mb-3">
                  <label className="form-label">Status *</label>
                  <select
                    name="status"
                    className="form-select"
                    value={formData.status}
                    onChange={handleChange}
                    required
                  >
                    <option value="SCHEDULED">Scheduled</option>
                    <option value="COMPLETED">Completed</option>
                    <option value="CANCELLED">Cancelled</option>
                  </select>
                </div>
              </div>

              <div className="mb-3">
                <label className="form-label">Reason for Appointment *</label>
                <input
                  type="text"
                  name="reason"
                  className="form-control"
                  value={formData.reason}
                  onChange={handleChange}
                  required
                  minLength={5}
                  maxLength={255}
                  placeholder="e.g., Regular checkup, Follow-up visit"
                />
              </div>

              <div className="mb-3">
                <label className="form-label">Notes</label>
                <textarea
                  name="notes"
                  className="form-control"
                  value={formData.notes}
                  onChange={handleChange}
                  maxLength={500}
                  rows={3}
                  placeholder="Doctor's notes (fill after visit)"
                />
              </div>

              <div className="form-check mb-3">
                <input
                  type="checkbox"
                  name="isActive"
                  id="isActive"
                  className="form-check-input"
                  checked={formData.isActive}
                  onChange={handleChange}
                />
                <label className="form-check-label" htmlFor="isActive">
                  Active
                </label>
              </div>

              <div className="gap-2 d-flex">
                <button type="submit" className="btn btn-primary">
                  {editingId ? 'Update' : 'Book'}
                </button>
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={handleCancel}
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Button to show form */}
      {!showForm && (
        <button className="btn btn-success mb-3" onClick={() => setShowForm(true)}>
          + Book Appointment
        </button>
      )}

      {/* Appointments Table */}
      {loading ? (
        <p>Loading appointments...</p>
      ) : appointments.length === 0 ? (
        <p>No appointments found. Book one to get started.</p>
      ) : (
        <table className="table table-striped table-hover table-sm">
          <thead className="table-dark">
            <tr>
              <th>ID</th>
              <th>Patient</th>
              <th>Doctor</th>
              <th>Date</th>
              <th>Time</th>
              <th>Reason</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {appointments.map((appointment) => (
              <tr key={appointment.id}>
                <td>{appointment.id}</td>
                <td>{getPatientName(appointment.patient?.id)}</td>
                <td>{getDoctorName(appointment.doctor?.id)}</td>
                <td>{appointment.appointmentDate}</td>
                <td>{appointment.appointmentTime}</td>
                <td>{appointment.reason}</td>
                <td>
                  <span className={`badge ${getStatusBadge(appointment.status)}`}>
                    {appointment.status}
                  </span>
                </td>
                <td>
                  <button
                    className="btn btn-sm btn-primary me-2"
                    onClick={() => handleEdit(appointment)}
                  >
                    Edit
                  </button>
                  <button
                    className="btn btn-sm btn-danger"
                    onClick={() => handleDelete(appointment.id)}
                  >
                    Delete
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}

export default AppointmentManagement;