import { useState, useEffect } from 'react';
import patientService from '../services/patientService';

function PatientManagement() {
  const [patients, setPatients] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [formData, setFormData] = useState({
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
    dateOfBirth: '',
    gender: 'Male',
    address: '',
    city: '',
    state: '',
    zipCode: '',
    bloodGroup: '',
    emergencyContact: '',
    emergencyPhone: '',
    isActive: true,
  });

  // Fetch all patients on mount
  useEffect(() => {
    loadPatients();
  }, []);

  const loadPatients = async () => {
    setLoading(true);
    setError('');
    try {
      const result = await patientService.getAllPatients();
      if (result.success) {
        setPatients(result.data || []);
      } else {
        setError(result.message || 'Failed to load patients');
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Error loading patients');
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
        result = await patientService.updatePatient(editingId, formData);
      } else {
        result = await patientService.createPatient(formData);
      }

      if (result.success) {
        setSuccess(result.message);
        setShowForm(false);
        setEditingId(null);
        setFormData({
          firstName: '',
          lastName: '',
          email: '',
          phone: '',
          dateOfBirth: '',
          gender: 'Male',
          address: '',
          city: '',
          state: '',
          zipCode: '',
          bloodGroup: '',
          emergencyContact: '',
          emergencyPhone: '',
          isActive: true,
        });
        loadPatients();
      } else {
        setError(result.message);
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Operation failed');
    }
  };

  const handleEdit = (patient) => {
    setEditingId(patient.id);
    setFormData({
      firstName: patient.firstName,
      lastName: patient.lastName,
      email: patient.email || '',
      phone: patient.phone,
      dateOfBirth: patient.dateOfBirth,
      gender: patient.gender,
      address: patient.address,
      city: patient.city,
      state: patient.state,
      zipCode: patient.zipCode,
      bloodGroup: patient.bloodGroup || '',
      emergencyContact: patient.emergencyContact || '',
      emergencyPhone: patient.emergencyPhone || '',
      isActive: patient.isActive,
    });
    setShowForm(true);
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this patient?')) {
      return;
    }

    setError('');
    setSuccess('');

    try {
      const result = await patientService.deletePatient(id);
      if (result.success) {
        setSuccess(result.message);
        loadPatients();
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
      firstName: '',
      lastName: '',
      email: '',
      phone: '',
      dateOfBirth: '',
      gender: 'Male',
      address: '',
      city: '',
      state: '',
      zipCode: '',
      bloodGroup: '',
      emergencyContact: '',
      emergencyPhone: '',
      isActive: true,
    });
    setError('');
  };

  return (
    <div className="container mt-4">
      <h1 className="mb-4">Patient Management</h1>

      {error && <div className="alert alert-danger">{error}</div>}
      {success && <div className="alert alert-success">{success}</div>}

      {/* Add/Edit Form */}
      {showForm && (
        <div className="card mb-4 bg-light">
          <div className="card-body">
            <h5>{editingId ? 'Edit Patient' : 'Add New Patient'}</h5>
            <form onSubmit={handleSubmit}>
              <div className="row">
                <div className="col-md-6 mb-3">
                  <label className="form-label">First Name *</label>
                  <input
                    type="text"
                    name="firstName"
                    className="form-control"
                    value={formData.firstName}
                    onChange={handleChange}
                    required
                    minLength={2}
                    maxLength={50}
                  />
                </div>
                <div className="col-md-6 mb-3">
                  <label className="form-label">Last Name *</label>
                  <input
                    type="text"
                    name="lastName"
                    className="form-control"
                    value={formData.lastName}
                    onChange={handleChange}
                    required
                    minLength={2}
                    maxLength={50}
                  />
                </div>
              </div>

              <div className="row">
                <div className="col-md-6 mb-3">
                  <label className="form-label">Email</label>
                  <input
                    type="email"
                    name="email"
                    className="form-control"
                    value={formData.email}
                    onChange={handleChange}
                    maxLength={100}
                  />
                </div>
                <div className="col-md-6 mb-3">
                  <label className="form-label">Phone *</label>
                  <input
                    type="text"
                    name="phone"
                    className="form-control"
                    value={formData.phone}
                    onChange={handleChange}
                    required
                    minLength={10}
                    maxLength={20}
                  />
                </div>
              </div>

              <div className="row">
                <div className="col-md-6 mb-3">
                  <label className="form-label">Date of Birth *</label>
                  <input
                    type="date"
                    name="dateOfBirth"
                    className="form-control"
                    value={formData.dateOfBirth}
                    onChange={handleChange}
                    required
                  />
                </div>
                <div className="col-md-6 mb-3">
                  <label className="form-label">Gender *</label>
                  <select
                    name="gender"
                    className="form-select"
                    value={formData.gender}
                    onChange={handleChange}
                    required
                  >
                    <option value="Male">Male</option>
                    <option value="Female">Female</option>
                    <option value="Other">Other</option>
                  </select>
                </div>
              </div>

              <div className="mb-3">
                <label className="form-label">Address *</label>
                <input
                  type="text"
                  name="address"
                  className="form-control"
                  value={formData.address}
                  onChange={handleChange}
                  required
                  minLength={5}
                  maxLength={255}
                />
              </div>

              <div className="row">
                <div className="col-md-6 mb-3">
                  <label className="form-label">City *</label>
                  <input
                    type="text"
                    name="city"
                    className="form-control"
                    value={formData.city}
                    onChange={handleChange}
                    required
                    minLength={2}
                    maxLength={50}
                  />
                </div>
                <div className="col-md-3 mb-3">
                  <label className="form-label">State *</label>
                  <input
                    type="text"
                    name="state"
                    className="form-control"
                    value={formData.state}
                    onChange={handleChange}
                    required
                    minLength={2}
                    maxLength={50}
                  />
                </div>
                <div className="col-md-3 mb-3">
                  <label className="form-label">Zip Code *</label>
                  <input
                    type="text"
                    name="zipCode"
                    className="form-control"
                    value={formData.zipCode}
                    onChange={handleChange}
                    required
                    minLength={5}
                    maxLength={10}
                  />
                </div>
              </div>

              <div className="row">
                <div className="col-md-6 mb-3">
                  <label className="form-label">Blood Group</label>
                  <input
                    type="text"
                    name="bloodGroup"
                    className="form-control"
                    value={formData.bloodGroup}
                    onChange={handleChange}
                    placeholder="e.g., O+, AB-"
                    maxLength={5}
                  />
                </div>
              </div>

              <div className="row">
                <div className="col-md-6 mb-3">
                  <label className="form-label">Emergency Contact</label>
                  <input
                    type="text"
                    name="emergencyContact"
                    className="form-control"
                    value={formData.emergencyContact}
                    onChange={handleChange}
                    maxLength={100}
                  />
                </div>
                <div className="col-md-6 mb-3">
                  <label className="form-label">Emergency Phone</label>
                  <input
                    type="text"
                    name="emergencyPhone"
                    className="form-control"
                    value={formData.emergencyPhone}
                    onChange={handleChange}
                    maxLength={20}
                  />
                </div>
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
          + Add Patient
        </button>
      )}

      {/* Patients Table */}
      {loading ? (
        <p>Loading patients...</p>
      ) : patients.length === 0 ? (
        <p>No patients found. Create one to get started.</p>
      ) : (
        <table className="table table-striped table-hover table-sm">
          <thead className="table-dark">
            <tr>
              <th>ID</th>
              <th>Name</th>
              <th>Email</th>
              <th>Phone</th>
              <th>DOB</th>
              <th>City</th>
              <th>Blood Group</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {patients.map((patient) => (
              <tr key={patient.id}>
                <td>{patient.id}</td>
                <td>
                  {patient.firstName} {patient.lastName}
                </td>
                <td>{patient.email || '—'}</td>
                <td>{patient.phone}</td>
                <td>{patient.dateOfBirth}</td>
                <td>{patient.city}</td>
                <td>{patient.bloodGroup || '—'}</td>
                <td>
                  <span
                    className={`badge ${patient.isActive ? 'bg-success' : 'bg-danger'}`}
                  >
                    {patient.isActive ? 'Active' : 'Inactive'}
                  </span>
                </td>
                <td>
                  <button
                    className="btn btn-sm btn-primary me-2"
                    onClick={() => handleEdit(patient)}
                  >
                    Edit
                  </button>
                  <button
                    className="btn btn-sm btn-danger"
                    onClick={() => handleDelete(patient.id)}
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

export default PatientManagement;