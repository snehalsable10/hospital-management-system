import { useState, useEffect } from 'react';
import prescriptionService from '../services/prescriptionService';
import appointmentService from '../services/appointmentService';
import patientService from '../services/patientService';
import doctorService from '../services/doctorService';

function PrescriptionManagement() {
  const [prescriptions, setPrescriptions] = useState([]);
  const [appointments, setAppointments] = useState([]);
  const [patients, setPatients] = useState([]);
  const [doctors, setDoctors] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [formData, setFormData] = useState({
    appointmentId: '',
    patientId: '',
    doctorId: '',
    medicineName: '',
    dosage: '',
    frequency: '',
    duration: '',
    instructions: '',
    status: 'ACTIVE',
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
      const [prescriptionsResult, appointmentsResult, patientsResult, doctorsResult] = await Promise.all([
        prescriptionService.getAllPrescriptions(),
        appointmentService.getAllAppointments(),
        patientService.getAllPatients(),
        doctorService.getAllDoctors(),
      ]);

      if (prescriptionsResult.success) {
        setPrescriptions(prescriptionsResult.data || []);
      }
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
        result = await prescriptionService.updatePrescription(editingId, formData);
      } else {
        result = prescriptionService.createPrescription(formData);
      }

      if (result.success) {
        setSuccess(result.message);
        setShowForm(false);
        setEditingId(null);
        setFormData({
          appointmentId: '',
          patientId: '',
          doctorId: '',
          medicineName: '',
          dosage: '',
          frequency: '',
          duration: '',
          instructions: '',
          status: 'ACTIVE',
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

  const handleEdit = (prescription) => {
    setEditingId(prescription.id);
    setFormData({
      appointmentId: prescription.appointment?.id || '',
      patientId: prescription.patient?.id || '',
      doctorId: prescription.doctor?.id || '',
      medicineName: prescription.medicineName,
      dosage: prescription.dosage,
      frequency: prescription.frequency,
      duration: prescription.duration,
      instructions: prescription.instructions || '',
      status: prescription.status,
      isActive: prescription.isActive,
    });
    setShowForm(true);
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this prescription?')) {
      return;
    }

    setError('');
    setSuccess('');

    try {
      const result = await prescriptionService.deletePrescription(id);
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
      appointmentId: '',
      patientId: '',
      doctorId: '',
      medicineName: '',
      dosage: '',
      frequency: '',
      duration: '',
      instructions: '',
      status: 'ACTIVE',
      isActive: true,
    });
    setError('');
  };

  const getAppointmentDisplay = (appointmentId) => {
    const apt = appointments.find((a) => a.id === appointmentId);
    if (apt && apt.patient && apt.doctor) {
      return `${apt.patient.firstName} ${apt.patient.lastName} - ${apt.doctor.firstName} ${apt.doctor.lastName}`;
    }
    return 'Unknown';
  };

  const getStatusBadge = (status) => {
    const statusColors = {
      ACTIVE: 'bg-success',
      COMPLETED: 'bg-info',
      DISCONTINUED: 'bg-warning',
    };
    return statusColors[status] || 'bg-secondary';
  };

  return (
    <div className="container mt-4">
      <h1 className="mb-4">Prescription Management</h1>

      {error && <div className="alert alert-danger">{error}</div>}
      {success && <div className="alert alert-success">{success}</div>}

      {/* Add/Edit Form */}
      {showForm && (
        <div className="card mb-4 bg-light">
          <div className="card-body">
            <h5>{editingId ? 'Edit Prescription' : 'Create New Prescription'}</h5>
            <form onSubmit={handleSubmit}>
              <div className="row">
                <div className="col-md-4 mb-3">
                  <label className="form-label">Appointment *</label>
                  <select
                    name="appointmentId"
                    className="form-select"
                    value={formData.appointmentId}
                    onChange={handleChange}
                    required
                  >
                    <option value="">Select an appointment</option>
                    {appointments.map((apt) => (
                      <option key={apt.id} value={apt.id}>
                        {getAppointmentDisplay(apt.id)}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="col-md-4 mb-3">
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
                <div className="col-md-4 mb-3">
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
                        {doctor.firstName} {doctor.lastName}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="row">
                <div className="col-md-6 mb-3">
                  <label className="form-label">Medicine Name *</label>
                  <input
                    type="text"
                    name="medicineName"
                    className="form-control"
                    value={formData.medicineName}
                    onChange={handleChange}
                    required
                    minLength={2}
                    maxLength={100}
                    placeholder="e.g., Amoxicillin"
                  />
                </div>
                <div className="col-md-6 mb-3">
                  <label className="form-label">Dosage *</label>
                  <input
                    type="text"
                    name="dosage"
                    className="form-control"
                    value={formData.dosage}
                    onChange={handleChange}
                    required
                    minLength={2}
                    maxLength={50}
                    placeholder="e.g., 500mg, 2 tablets"
                  />
                </div>
              </div>

              <div className="row">
                <div className="col-md-6 mb-3">
                  <label className="form-label">Frequency *</label>
                  <input
                    type="text"
                    name="frequency"
                    className="form-control"
                    value={formData.frequency}
                    onChange={handleChange}
                    required
                    minLength={3}
                    maxLength={50}
                    placeholder="e.g., Twice daily"
                  />
                </div>
                <div className="col-md-6 mb-3">
                  <label className="form-label">Duration *</label>
                  <input
                    type="text"
                    name="duration"
                    className="form-control"
                    value={formData.duration}
                    onChange={handleChange}
                    required
                    minLength={2}
                    maxLength={50}
                    placeholder="e.g., 7 days"
                  />
                </div>
              </div>

              <div className="mb-3">
                <label className="form-label">Instructions</label>
                <input
                  type="text"
                  name="instructions"
                  className="form-control"
                  value={formData.instructions}
                  onChange={handleChange}
                  maxLength={255}
                  placeholder="e.g., Take with food"
                />
              </div>

              <div className="mb-3">
                <label className="form-label">Status *</label>
                <select
                  name="status"
                  className="form-select"
                  value={formData.status}
                  onChange={handleChange}
                  required
                >
                  <option value="ACTIVE">Active</option>
                  <option value="COMPLETED">Completed</option>
                  <option value="DISCONTINUED">Discontinued</option>
                </select>
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
                  {editingId ? 'Update' : 'Create'}
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
          + Add Prescription
        </button>
      )}

      {/* Prescriptions Table */}
      {loading ? (
        <p>Loading prescriptions...</p>
      ) : prescriptions.length === 0 ? (
        <p>No prescriptions found. Create one to get started.</p>
      ) : (
        <table className="table table-striped table-hover table-sm">
          <thead className="table-dark">
            <tr>
              <th>ID</th>
              <th>Appointment</th>
              <th>Medicine</th>
              <th>Dosage</th>
              <th>Frequency</th>
              <th>Duration</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {prescriptions.map((prescription) => (
              <tr key={prescription.id}>
                <td>{prescription.id}</td>
                <td>{getAppointmentDisplay(prescription.appointment?.id)}</td>
                <td>{prescription.medicineName}</td>
                <td>{prescription.dosage}</td>
                <td>{prescription.frequency}</td>
                <td>{prescription.duration}</td>
                <td>
                  <span className={`badge ${getStatusBadge(prescription.status)}`}>
                    {prescription.status}
                  </span>
                </td>
                <td>
                  <button
                    className="btn btn-sm btn-primary me-2"
                    onClick={() => handleEdit(prescription)}
                  >
                    Edit
                  </button>
                  <button
                    className="btn btn-sm btn-danger"
                    onClick={() => handleDelete(prescription.id)}
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

export default PrescriptionManagement;