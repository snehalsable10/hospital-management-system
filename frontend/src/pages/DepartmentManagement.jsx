import { useState, useEffect } from 'react';
import departmentService from '../services/departmentService';

function DepartmentManagement() {
  const [departments, setDepartments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    phone: '',
    isActive: true,
  });

  // Fetch all departments on mount
  useEffect(() => {
    loadDepartments();
  }, []);

  const loadDepartments = async () => {
    setLoading(true);
    setError('');
    try {
      const result = await departmentService.getAllDepartments();
      if (result.success) {
        setDepartments(result.data || []);
      } else {
        setError(result.message || 'Failed to load departments');
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Error loading departments');
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
        result = await departmentService.updateDepartment(editingId, formData);
      } else {
        result = await departmentService.createDepartment(formData);
      }

      if (result.success) {
        setSuccess(result.message);
        setShowForm(false);
        setEditingId(null);
        setFormData({ name: '', description: '', phone: '', isActive: true });
        loadDepartments();
      } else {
        setError(result.message);
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Operation failed');
    }
  };

  const handleEdit = (department) => {
    setEditingId(department.id);
    setFormData({
      name: department.name,
      description: department.description || '',
      phone: department.phone || '',
      isActive: department.isActive,
    });
    setShowForm(true);
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this department?')) {
      return;
    }

    setError('');
    setSuccess('');

    try {
      const result = await departmentService.deleteDepartment(id);
      if (result.success) {
        setSuccess(result.message);
        loadDepartments();
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
    setFormData({ name: '', description: '', phone: '', isActive: true });
    setError('');
  };

  return (
    <div className="container mt-4">
      <h1 className="mb-4">Department Management</h1>

      {error && <div className="alert alert-danger">{error}</div>}
      {success && <div className="alert alert-success">{success}</div>}

      {/* Add/Edit Form */}
      {showForm && (
        <div className="card mb-4 bg-light">
          <div className="card-body">
            <h5>{editingId ? 'Edit Department' : 'Add New Department'}</h5>
            <form onSubmit={handleSubmit}>
              <div className="mb-3">
                <label className="form-label">Department Name *</label>
                <input
                  type="text"
                  name="name"
                  className="form-control"
                  value={formData.name}
                  onChange={handleChange}
                  required
                  minLength={2}
                  maxLength={100}
                />
              </div>

              <div className="mb-3">
                <label className="form-label">Description</label>
                <textarea
                  name="description"
                  className="form-control"
                  value={formData.description}
                  onChange={handleChange}
                  maxLength={500}
                  rows={3}
                />
              </div>

              <div className="mb-3">
                <label className="form-label">Phone</label>
                <input
                  type="text"
                  name="phone"
                  className="form-control"
                  value={formData.phone}
                  onChange={handleChange}
                  maxLength={20}
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
          + Add Department
        </button>
      )}

      {/* Departments Table */}
      {loading ? (
        <p>Loading departments...</p>
      ) : departments.length === 0 ? (
        <p>No departments found. Create one to get started.</p>
      ) : (
        <table className="table table-striped">
          <thead className="table-dark">
            <tr>
              <th>ID</th>
              <th>Name</th>
              <th>Description</th>
              <th>Phone</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {departments.map((dept) => (
              <tr key={dept.id}>
                <td>{dept.id}</td>
                <td>{dept.name}</td>
                <td>{dept.description || '—'}</td>
                <td>{dept.phone || '—'}</td>
                <td>
                  <span
                    className={`badge ${dept.isActive ? 'bg-success' : 'bg-danger'}`}
                  >
                    {dept.isActive ? 'Active' : 'Inactive'}
                  </span>
                </td>
                <td>
                  <button
                    className="btn btn-sm btn-primary me-2"
                    onClick={() => handleEdit(dept)}
                  >
                    Edit
                  </button>
                  <button
                    className="btn btn-sm btn-danger"
                    onClick={() => handleDelete(dept.id)}
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

export default DepartmentManagement;