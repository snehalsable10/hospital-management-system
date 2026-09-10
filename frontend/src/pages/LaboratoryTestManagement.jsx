import React, { useState, useEffect } from 'react';
import laboratoryTestService from '../services/laboratoryTestService';
import patientService from '../services/patientService';

const LaboratoryTestManagement = () => {
  const [tests, setTests] = useState([]);
  const [patients, setPatients] = useState([]);
  const [formData, setFormData] = useState({
    patientId: '',
    testName: '',
    testDate: '',
    resultValue: '',
    resultUnit: '',
    referenceMin: '',
    referenceMax: '',
    status: 'NORMAL',
    notes: ''
  });
  const [editingId, setEditingId] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  useEffect(() => {
    loadTests();
    loadPatients();
  }, []);

  const loadTests = async () => {
    try {
      setLoading(true);
      const response = await laboratoryTestService.getAllLaboratoryTests();
      setTests(response.data || []);
      setError('');
    } catch (err) {
      setError('Failed to load laboratory tests');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const loadPatients = async () => {
    try {
      const response = await patientService.getAllPatients();
      setPatients(response.data || []);
    } catch (err) {
      console.error('Failed to load patients', err);
    }
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!formData.patientId || !formData.testName || !formData.testDate) {
      setError('Patient, test name, and test date are required');
      return;
    }

    try {
      setLoading(true);
      if (editingId) {
        await laboratoryTestService.updateLaboratoryTest(editingId, formData);
        setSuccess('Laboratory test updated successfully');
      } else {
        await laboratoryTestService.createLaboratoryTest(formData);
        setSuccess('Laboratory test created successfully');
      }
      resetForm();
      loadTests();
      setError('');
    } catch (err) {
      setError('Failed to save laboratory test: ' + err.response?.data?.message || err.message);
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleEdit = (test) => {
    setFormData({
      patientId: test.patient.id,
      testName: test.testName,
      testDate: test.testDate,
      resultValue: test.resultValue,
      resultUnit: test.resultUnit,
      referenceMin: test.referenceMin || '',
      referenceMax: test.referenceMax || '',
      status: test.status,
      notes: test.notes || ''
    });
    setEditingId(test.id);
  };

  const handleDelete = async (id) => {
    if (window.confirm('Are you sure you want to delete this laboratory test?')) {
      try {
        setLoading(true);
        await laboratoryTestService.deleteLaboratoryTest(id);
        setSuccess('Laboratory test deleted successfully');
        loadTests();
        setError('');
      } catch (err) {
        setError('Failed to delete laboratory test: ' + err.response?.data?.message || err.message);
        console.error(err);
      } finally {
        setLoading(false);
      }
    }
  };

  const resetForm = () => {
    setFormData({
      patientId: '',
      testName: '',
      testDate: '',
      resultValue: '',
      resultUnit: '',
      referenceMin: '',
      referenceMax: '',
      status: 'NORMAL',
      notes: ''
    });
    setEditingId(null);
  };

  const getStatusBadge = (status) => {
    const badges = {
      NORMAL: 'success',
      ABNORMAL: 'warning',
      CRITICAL: 'danger'
    };
    return badges[status] || 'secondary';
  };

  const getPatientName = (patientId) => {
    const patient = patients.find(p => p.id === patientId);
    return patient ? `${patient.firstName} ${patient.lastName}` : 'Unknown';
  };

  return (
    <div className="container mt-4">
      <h1 className="mb-4">Laboratory Test Management</h1>

      {error && <div className="alert alert-danger">{error}</div>}
      {success && <div className="alert alert-success">{success}</div>}

      <div className="card mb-4">
        <div className="card-header">
          <h5>{editingId ? 'Edit Laboratory Test' : 'Add Laboratory Test'}</h5>
        </div>
        <div className="card-body">
          <form onSubmit={handleSubmit}>
            <div className="row">
              <div className="col-md-6 mb-3">
                <label className="form-label">Patient *</label>
                <select
                  name="patientId"
                  className="form-control"
                  value={formData.patientId}
                  onChange={handleInputChange}
                  required
                >
                  <option value="">Select Patient</option>
                  {patients.map(patient => (
                    <option key={patient.id} value={patient.id}>
                      {patient.firstName} {patient.lastName} ({patient.email})
                    </option>
                  ))}
                </select>
              </div>

              <div className="col-md-6 mb-3">
                <label className="form-label">Test Name *</label>
                <input
                  type="text"
                  name="testName"
                  className="form-control"
                  value={formData.testName}
                  onChange={handleInputChange}
                  placeholder="e.g., Blood Test, X-Ray"
                  required
                />
              </div>
            </div>

            <div className="row">
              <div className="col-md-6 mb-3">
                <label className="form-label">Test Date *</label>
                <input
                  type="date"
                  name="testDate"
                  className="form-control"
                  value={formData.testDate}
                  onChange={handleInputChange}
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
                  <option value="NORMAL">Normal</option>
                  <option value="ABNORMAL">Abnormal</option>
                  <option value="CRITICAL">Critical</option>
                </select>
              </div>
            </div>

            <div className="row">
              <div className="col-md-6 mb-3">
                <label className="form-label">Result Value</label>
                <input
                  type="text"
                  name="resultValue"
                  className="form-control"
                  value={formData.resultValue}
                  onChange={handleInputChange}
                  placeholder="e.g., 120"
                />
              </div>

              <div className="col-md-6 mb-3">
                <label className="form-label">Result Unit</label>
                <input
                  type="text"
                  name="resultUnit"
                  className="form-control"
                  value={formData.resultUnit}
                  onChange={handleInputChange}
                  placeholder="e.g., mg/dL"
                />
              </div>
            </div>

            <div className="row">
              <div className="col-md-6 mb-3">
                <label className="form-label">Reference Min</label>
                <input
                  type="text"
                  name="referenceMin"
                  className="form-control"
                  value={formData.referenceMin}
                  onChange={handleInputChange}
                  placeholder="Minimum normal value"
                />
              </div>

              <div className="col-md-6 mb-3">
                <label className="form-label">Reference Max</label>
                <input
                  type="text"
                  name="referenceMax"
                  className="form-control"
                  value={formData.referenceMax}
                  onChange={handleInputChange}
                  placeholder="Maximum normal value"
                />
              </div>
            </div>

            <div className="mb-3">
              <label className="form-label">Notes</label>
              <textarea
                name="notes"
                className="form-control"
                value={formData.notes}
                onChange={handleInputChange}
                rows="3"
                placeholder="Additional notes or observations"
              />
            </div>

            <div className="d-flex gap-2">
              <button type="submit" className="btn btn-primary" disabled={loading}>
                {loading ? 'Saving...' : editingId ? 'Update Test' : 'Create Test'}
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

      <div className="card">
        <div className="card-header">
          <h5>Laboratory Tests List</h5>
        </div>
        <div className="card-body">
          {loading && <p>Loading...</p>}
          {tests.length === 0 ? (
            <p className="text-muted">No laboratory tests found</p>
          ) : (
            <div className="table-responsive">
              <table className="table table-hover">
                <thead className="table-light">
                  <tr>
                    <th>Patient</th>
                    <th>Test Name</th>
                    <th>Test Date</th>
                    <th>Result Value</th>
                    <th>Result Unit</th>
                    <th>Status</th>
                    <th>Notes</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {tests.map(test => (
                    <tr key={test.id}>
                      <td>{getPatientName(test.patient.id)}</td>
                      <td>{test.testName}</td>
                      <td>{new Date(test.testDate).toLocaleDateString()}</td>
                      <td>{test.resultValue}</td>
                      <td>{test.resultUnit}</td>
                      <td>
                        <span className={`badge bg-${getStatusBadge(test.status)}`}>
                          {test.status}
                        </span>
                      </td>
                      <td>{test.notes || '-'}</td>
                      <td>
                        <button
                          className="btn btn-sm btn-warning me-2"
                          onClick={() => handleEdit(test)}
                          disabled={loading}
                        >
                          Edit
                        </button>
                        <button
                          className="btn btn-sm btn-danger"
                          onClick={() => handleDelete(test.id)}
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

export default LaboratoryTestManagement;