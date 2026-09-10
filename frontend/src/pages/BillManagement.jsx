import React, { useState, useEffect } from 'react';
import billService from '../services/billService';
import patientService from '../services/patientService';
import doctorService from '../services/doctorService';

const BillManagement = () => {
  const [bills, setBills] = useState([]);
  const [patients, setPatients] = useState([]);
  const [doctors, setDoctors] = useState([]);
  const [formData, setFormData] = useState({
    patientId: '',
    doctorId: '',
    billDate: '',
    consultationFee: '',
    testsFee: '',
    medicationsFee: '',
    otherCharges: '',
    totalAmount: '',
    status: 'UNPAID',
    paymentMethod: 'Cash',
    description: ''
  });
  const [editingId, setEditingId] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  useEffect(() => {
    loadBills();
    loadPatients();
    loadDoctors();
  }, []);

  const loadBills = async () => {
    try {
      setLoading(true);
      const response = await billService.getAllBills();
      setBills(response.data || []);
      setError('');
    } catch (err) {
      setError('Failed to load bills');
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

  const loadDoctors = async () => {
    try {
      const response = await doctorService.getAllDoctors();
      setDoctors(response.data || []);
    } catch (err) {
      console.error('Failed to load doctors', err);
    }
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const calculateTotal = () => {
    const consultation = parseFloat(formData.consultationFee) || 0;
    const tests = parseFloat(formData.testsFee) || 0;
    const medications = parseFloat(formData.medicationsFee) || 0;
    const other = parseFloat(formData.otherCharges) || 0;
    return (consultation + tests + medications + other).toFixed(2);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!formData.patientId || !formData.doctorId || !formData.billDate || !formData.consultationFee || !formData.totalAmount) {
      setError('Patient, doctor, bill date, consultation fee, and total amount are required');
      return;
    }

    try {
      setLoading(true);
      const totalAmount = calculateTotal();
      const billData = {
        ...formData,
        totalAmount: parseFloat(totalAmount)
      };

      if (editingId) {
        await billService.updateBill(editingId, billData);
        setSuccess('Bill updated successfully');
      } else {
        await billService.createBill(billData);
        setSuccess('Bill created successfully');
      }
      resetForm();
      loadBills();
      setError('');
    } catch (err) {
      setError('Failed to save bill: ' + err.response?.data?.message || err.message);
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleEdit = (bill) => {
    setFormData({
      patientId: bill.patient.id,
      doctorId: bill.doctor.id,
      billDate: bill.billDate,
      consultationFee: bill.consultationFee,
      testsFee: bill.testsFee || '',
      medicationsFee: bill.medicationsFee || '',
      otherCharges: bill.otherCharges || '',
      totalAmount: bill.totalAmount,
      status: bill.status,
      paymentMethod: bill.paymentMethod,
      description: bill.description || ''
    });
    setEditingId(bill.id);
  };

  const handleDelete = async (id) => {
    if (window.confirm('Are you sure you want to delete this bill?')) {
      try {
        setLoading(true);
        await billService.deleteBill(id);
        setSuccess('Bill deleted successfully');
        loadBills();
        setError('');
      } catch (err) {
        setError('Failed to delete bill: ' + err.response?.data?.message || err.message);
        console.error(err);
      } finally {
        setLoading(false);
      }
    }
  };

  const resetForm = () => {
    setFormData({
      patientId: '',
      doctorId: '',
      billDate: '',
      consultationFee: '',
      testsFee: '',
      medicationsFee: '',
      otherCharges: '',
      totalAmount: '',
      status: 'UNPAID',
      paymentMethod: 'Cash',
      description: ''
    });
    setEditingId(null);
  };

  const getStatusBadge = (status) => {
    const badges = {
      PAID: 'success',
      UNPAID: 'danger',
      PENDING: 'warning'
    };
    return badges[status] || 'secondary';
  };

  const getPatientName = (patientId) => {
    const patient = patients.find(p => p.id === patientId);
    return patient ? `${patient.firstName} ${patient.lastName}` : 'Unknown';
  };

  const getDoctorName = (doctorId) => {
    const doctor = doctors.find(d => d.id === doctorId);
    return doctor ? `${doctor.firstName} ${doctor.lastName}` : 'Unknown';
  };

  return (
    <div className="container mt-4">
      <h1 className="mb-4">Billing Management</h1>

      {error && <div className="alert alert-danger">{error}</div>}
      {success && <div className="alert alert-success">{success}</div>}

      <div className="card mb-4">
        <div className="card-header">
          <h5>{editingId ? 'Edit Bill' : 'Create New Bill'}</h5>
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
                <label className="form-label">Doctor *</label>
                <select
                  name="doctorId"
                  className="form-control"
                  value={formData.doctorId}
                  onChange={handleInputChange}
                  required
                >
                  <option value="">Select Doctor</option>
                  {doctors.map(doctor => (
                    <option key={doctor.id} value={doctor.id}>
                      {doctor.firstName} {doctor.lastName} ({doctor.specialization})
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div className="row">
              <div className="col-md-6 mb-3">
                <label className="form-label">Bill Date *</label>
                <input
                  type="date"
                  name="billDate"
                  className="form-control"
                  value={formData.billDate}
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
                  <option value="UNPAID">Unpaid</option>
                  <option value="PAID">Paid</option>
                  <option value="PENDING">Pending</option>
                </select>
              </div>
            </div>

            <div className="row">
              <div className="col-md-6 mb-3">
                <label className="form-label">Consultation Fee *</label>
                <input
                  type="number"
                  name="consultationFee"
                  className="form-control"
                  value={formData.consultationFee}
                  onChange={handleInputChange}
                  placeholder="0.00"
                  step="0.01"
                  required
                />
              </div>

              <div className="col-md-6 mb-3">
                <label className="form-label">Tests Fee</label>
                <input
                  type="number"
                  name="testsFee"
                  className="form-control"
                  value={formData.testsFee}
                  onChange={handleInputChange}
                  placeholder="0.00"
                  step="0.01"
                />
              </div>
            </div>

            <div className="row">
              <div className="col-md-6 mb-3">
                <label className="form-label">Medications Fee</label>
                <input
                  type="number"
                  name="medicationsFee"
                  className="form-control"
                  value={formData.medicationsFee}
                  onChange={handleInputChange}
                  placeholder="0.00"
                  step="0.01"
                />
              </div>

              <div className="col-md-6 mb-3">
                <label className="form-label">Other Charges</label>
                <input
                  type="number"
                  name="otherCharges"
                  className="form-control"
                  value={formData.otherCharges}
                  onChange={handleInputChange}
                  placeholder="0.00"
                  step="0.01"
                />
              </div>
            </div>

            <div className="row">
              <div className="col-md-6 mb-3">
                <label className="form-label">Total Amount *</label>
                <input
                  type="number"
                  name="totalAmount"
                  className="form-control"
                  value={calculateTotal()}
                  readOnly
                  placeholder="Auto-calculated"
                />
              </div>

              <div className="col-md-6 mb-3">
                <label className="form-label">Payment Method *</label>
                <select
                  name="paymentMethod"
                  className="form-control"
                  value={formData.paymentMethod}
                  onChange={handleInputChange}
                  required
                >
                  <option value="Cash">Cash</option>
                  <option value="Card">Card</option>
                  <option value="Insurance">Insurance</option>
                  <option value="Check">Check</option>
                  <option value="Online">Online</option>
                </select>
              </div>
            </div>

            <div className="mb-3">
              <label className="form-label">Description</label>
              <textarea
                name="description"
                className="form-control"
                value={formData.description}
                onChange={handleInputChange}
                rows="3"
                placeholder="Bill notes or details"
              />
            </div>

            <div className="d-flex gap-2">
              <button type="submit" className="btn btn-primary" disabled={loading}>
                {loading ? 'Saving...' : editingId ? 'Update Bill' : 'Create Bill'}
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
          <h5>Bills List</h5>
        </div>
        <div className="card-body">
          {loading && <p>Loading...</p>}
          {bills.length === 0 ? (
            <p className="text-muted">No bills found</p>
          ) : (
            <div className="table-responsive">
              <table className="table table-hover">
                <thead className="table-light">
                  <tr>
                    <th>Patient</th>
                    <th>Doctor</th>
                    <th>Bill Date</th>
                    <th>Consultation</th>
                    <th>Tests</th>
                    <th>Medications</th>
                    <th>Other</th>
                    <th>Total</th>
                    <th>Status</th>
                    <th>Payment</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {bills.map(bill => (
                    <tr key={bill.id}>
                      <td>{getPatientName(bill.patient.id)}</td>
                      <td>{getDoctorName(bill.doctor.id)}</td>
                      <td>{new Date(bill.billDate).toLocaleDateString()}</td>
                      <td>{bill.consultationFee}</td>
                      <td>{bill.testsFee || '-'}</td>
                      <td>{bill.medicationsFee || '-'}</td>
                      <td>{bill.otherCharges || '-'}</td>
                      <td><strong>{bill.totalAmount}</strong></td>
                      <td>
                        <span className={`badge bg-${getStatusBadge(bill.status)}`}>
                          {bill.status}
                        </span>
                      </td>
                      <td>{bill.paymentMethod}</td>
                      <td>
                        <button
                          className="btn btn-sm btn-warning me-2"
                          onClick={() => handleEdit(bill)}
                          disabled={loading}
                        >
                          Edit
                        </button>
                        <button
                          className="btn btn-sm btn-danger"
                          onClick={() => handleDelete(bill.id)}
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

export default BillManagement;