import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import authService from '../../services/authService';

function Signup() {
  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    username: '',
    email: '',
    password: '',
    firstName: '',
    lastName: '',
    phone: '',
  });
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
    setError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    setSuccess('');

    try {
      const result = await authService.signup(formData);
      if (result.success) {
        setSuccess('Account created. Redirecting to login...');
        setTimeout(() => navigate('/login'), 1500);
      } else {
        setError(result.message);
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Signup failed. Is the backend running?');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="container" style={{ maxWidth: '520px', marginTop: '50px', marginBottom: '50px' }}>
      <div className="card shadow">
        <div className="card-body p-4">
          <h3 className="text-center mb-4">Create Account</h3>

          {error && <div className="alert alert-danger">{error}</div>}
          {success && <div className="alert alert-success">{success}</div>}

          <form onSubmit={handleSubmit}>
            <div className="row">
              <div className="col-md-6 mb-3">
                <label className="form-label">First Name</label>
                <input type="text" name="firstName" className="form-control"
                  value={formData.firstName} onChange={handleChange} required minLength={2} />
              </div>
              <div className="col-md-6 mb-3">
                <label className="form-label">Last Name</label>
                <input type="text" name="lastName" className="form-control"
                  value={formData.lastName} onChange={handleChange} required minLength={2} />
              </div>
            </div>

            <div className="mb-3">
              <label className="form-label">Username</label>
              <input type="text" name="username" className="form-control"
                value={formData.username} onChange={handleChange} required minLength={3} />
            </div>

            <div className="mb-3">
              <label className="form-label">Email</label>
              <input type="email" name="email" className="form-control"
                value={formData.email} onChange={handleChange} required />
            </div>

            <div className="mb-3">
              <label className="form-label">Password</label>
              <input type="password" name="password" className="form-control"
                value={formData.password} onChange={handleChange} required minLength={6} />
              <small className="text-muted">Minimum 6 characters</small>
            </div>

            <div className="mb-3">
              <label className="form-label">Phone</label>
              <input type="text" name="phone" className="form-control"
                value={formData.phone} onChange={handleChange} maxLength={20} />
            </div>

            <p className="text-muted small mb-3">
              New accounts are registered as patients. Staff and doctor accounts
              are created by an administrator.
            </p>

            <button type="submit" className="btn btn-primary w-100" disabled={loading}>
              {loading ? 'Creating account...' : 'Sign Up'}
            </button>
          </form>

          <p className="text-center mt-3 mb-0">
            Already have an account? <Link to="/login">Login</Link>
          </p>
        </div>
      </div>
    </div>
  );
}

export default Signup;