import { useState, useEffect } from 'react';
import medicalHistoryService from '../services/medicalHistoryService';
import patientService from '../services/patientService';

function MedicalHistoryManagement() {
  const [medicalHistories, setMedicalHistories] = useState([]);
  const [patients, setPatients] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [formData, setFormData] = useState({
    patientId: '',
    conditionName: '',
    diagnosisDate: '',
    status: 'ACTIVE',
    description: '',
    treatment: '',
    doctorNotes: '',
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
      const [medicalHistoriesResult, patientsResult] = await Promise.all([
        medicalHistoryService.getAllMedicalHistories(),
        patientService.getAllPatients(),
      ]);

      if (medicalHistoriesResult.success) {
        setMedicalHistories(medicalHistoriesResult.data || []);
      }
      if (patientsResult.success) {
        setPatients(patientsResult.data || []);
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
        result = await medicalHistoryService.updateMedicalHistory(editingId, formData);
      } else {
        result = await medicalHistoryService.createMedicalHistory(formData);
      }

      if (result.success) {
        setSuccess(result.message);
        setShowForm(false);
        setEditingId(null);
        setFormData({
          patientId: '',
          conditionName: '',
          diagnosisDate: '',
          status: 'ACTIVE',
          description: '',
          treatment: '',
          doctorNotes: '',
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

  const handleEdit = (medicalHistory) => {
    setEditingId(medicalHistory.id);
    setFormData({
      patientId: medicalHistory.patient?.id || '',
      conditionName: medicalHistory.conditionName,
      diagnosisDate: medicalHistory.diagnosisDate,
      status: medicalHistory.status,
      description: medicalHistory.description || '',
      treatment: medicalHistory.treatment || '',
      doctorNotes: medicalHistory.doctorNotes || '',
      isActive: medicalHistory.isActive,
    });
    setShowForm(true);
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this medical history entry?')) {
      return;
    }

    setError('');
    setSuccess('');

    try {
      const result = await medicalHistoryService.deleteMedicalHistory(id);
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
      conditionName: '',
      diagnosisDate: '',
      status: 'ACTIVE',
      description: '',
      treatment: '',
      doctorNotes: '',
      isActive: true,
    });
    setError('');
  };

  const getPatientName = (patientId) => {
    const patient = patients.find((p) => p.id === patientId);
    return patient ? `${patient.firstName} ${patient.lastName}` : 'Unknown';
  };

  const getStatusBadge = (status) => {
    const statusColors = {
      ACTIVE: 'bg-danger',
      RESOLVED: 'bg-success',
      CHRONIC: 'bg-warning',
    };
    return statusColors[status] || 'bg-secondary';
  };

  return (
    <div className="container mt-4">
      <h1 className="mb-4">Medical History</h1>

      {error && <div className="alert alert-danger">{error}</div>}
      {success && <div className="alert alert-success">{success}</div>}

      {/* Add/Edit Form */}
      {showForm && (
        <div className="card mb-4 bg-light">
          <div className="card-body">
            <h5>{editingId ? 'Edit Medical History' : 'Add New Medical History Entry'}</h5>
            <form onSubmit={handleSubmit}>
              <div className="mb-3">
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

              <div className="row">
                <div className="col-md-6 mb-3">
                  <label className="form-label">Condition Name *</label>
                  <input
                    type="text"
                    name="conditionName"
                    className="form-control"
                    value={formData.conditionName}
                    onChange={handleChange}
                    required
                    minLength={2}
                    maxLength={100}
                    placeholder="e.g., Diabetes Type 2"
                  />
                </div>
                <div className="col-md-6 mb-3">
                  <label className="form-label">Diagnosis Date *</label>
                  <input
                    type="date"
                    name="diagnosisDate"
                    className="form-control"
                    value={formData.diagnosisDate}
                    onChange={handleChange}
                    required
                  />
                </div>
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
                  <option value="RESOLVED">Resolved</option>
                  <option value="CHRONIC">Chronic</option>
                </select>
              </div>

              <div className="mb-3">
                <label className="form-label">Description</label>
                <textarea
                  name="description"
                  className="form-control"
                  value={formData.description}
                  onChange={handleChange}
                  maxLength={500}
                  rows={2}
                  placeholder="Details about the condition"
                />
              </div>

              <div className="mb-3">
                <label className="form-label">Treatment</label>
                <textarea
                  name="treatment"
                  className="form-control"
                  value={formData.treatment}
                  onChange={handleChange}
                  maxLength={500}
                  rows={2}
                  placeholder="How it's being/was treated"
                />
              </div>

              <div className="mb-3">
                <label className="form-label">Doctor Notes</label>
                <textarea
                  name="doctorNotes"
                  className="form-control"
                  value={formData.doctorNotes}
                  onChange={handleChange}
                  maxLength={500}
                  rows={2}
                  placeholder="Doctor's observations"
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
          + Add Entry
        </button>
      )}

      {/* Medical Histories Table */}
      {loading ? (
        <p>Loading medical histories...</p>
      ) : medicalHistories.length === 0 ? (
        <p>No medical history entries found. Create one to get started.</p>
      ) : (
        <table className="table table-striped table-hover table-sm">
          <thead className="table-dark">
            <tr>
              <th>ID</th>
              <th>Patient</th>
              <th>Condition</th>
              <th>Diagnosis Date</th>
              <th>Status</th>
              <th>Treatment</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {medicalHistories.map((mh) => (
              <tr key={mh.id}>
                <td>{mh.id}</td>
                <td>{getPatientName(mh.patient?.id)}</td>
                <td>{mh.conditionName}</td>
                <td>{mh.diagnosisDate}</td>
                <td>
                  <span className={`badge ${getStatusBadge(mh.status)}`}>
                    {mh.status}
                  </span>
                </td>
                <td>{mh.treatment || '—'}</td>
                <td>
                  <button
                    className="btn btn-sm btn-primary me-2"
                    onClick={() => handleEdit(mh)}
                  >
                    Edit
                  </button>
                  <button
                    className="btn btn-sm btn-danger"
                    onClick={() => handleDelete(mh.id)}
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

export default MedicalHistoryManagement;