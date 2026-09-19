import { useState, useEffect, useCallback } from 'react';
import { Plus, UserCog, ChevronLeft, ChevronRight } from 'lucide-react';
import Table from '../components/common/Table';
import SearchBar from '../components/common/SearchBar';
import Modal from '../components/common/Modal';
import ConfirmDialog from '../components/common/ConfirmDialog';
import Button from '../components/common/Button';
import Badge from '../components/common/Badge';
import EmptyState from '../components/common/EmptyState';
import InputField from '../components/forms/InputField';
import SelectField from '../components/forms/SelectField';
import { useNotification } from '../hooks/useNotification';
import { useAuth } from '../hooks/useAuth';
import doctorService from '../services/doctorService';
import departmentService from '../services/departmentService';

const PAGE_SIZE = 10;

const EMPTY_FORM = {
  firstName: '',
  lastName: '',
  email: '',
  phone: '',
  specialization: '',
  licenseNumber: '',
  departmentId: '',
  userId: '',
  isActive: true,
};

function DoctorManagement() {
  const { role } = useAuth();
  const notify = useNotification();

  // Mutations are ADMIN-only; a doctor may edit their own record, which the
  // backend allows and the server enforces per row.
  const canManage = role === 'ADMIN';

  const [doctors, setDoctors] = useState([]);
  const [departments, setDepartments] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);

  const [specialization, setSpecialization] = useState('');
  const [departmentFilter, setDepartmentFilter] = useState('');

  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [formData, setFormData] = useState(EMPTY_FORM);
  const [fieldErrors, setFieldErrors] = useState({});
  const [saving, setSaving] = useState(false);

  const [confirmTarget, setConfirmTarget] = useState(null);
  const [deleting, setDeleting] = useState(false);

  // Needed both for the filter dropdown and the form's department selector.
  useEffect(() => {
    departmentService
      .getAllDepartments()
      .then((result) => setDepartments(result.data || []))
      .catch(() => notify.error('Could not load departments'));
  }, [notify]);

  const loadDoctors = useCallback(async () => {
    setLoading(true);
    try {
      let result;
      if (departmentFilter) {
        result = await doctorService.getDoctorsByDepartmentPaginated(
          departmentFilter,
          page,
          PAGE_SIZE
        );
      } else if (specialization.trim()) {
        result = await doctorService.getDoctorsBySpecializationPaginated(
          specialization,
          page,
          PAGE_SIZE
        );
      } else {
        result = await doctorService.getAllDoctorsPaginated(page, PAGE_SIZE);
      }

      const pageData = result.data || {};
      setDoctors(pageData.content || []);
      setTotalPages(pageData.totalPages || 0);
      setTotalElements(pageData.totalElements || 0);
    } catch (err) {
      notify.error(err.response?.data?.message || 'Could not load doctors');
      setDoctors([]);
      setTotalPages(0);
      setTotalElements(0);
    } finally {
      setLoading(false);
    }
  }, [page, specialization, departmentFilter, notify]);

  useEffect(() => {
    loadDoctors();
  }, [loadDoctors]);

  const openCreate = () => {
    setEditingId(null);
    setFormData(EMPTY_FORM);
    setFieldErrors({});
    setModalOpen(true);
  };

  const openEdit = (doctor) => {
    setEditingId(doctor.id);
    setFormData({
      firstName: doctor.firstName || '',
      lastName: doctor.lastName || '',
      email: doctor.email || '',
      phone: doctor.phone || '',
      specialization: doctor.specialization || '',
      licenseNumber: doctor.licenseNumber || '',
      departmentId: doctor.department?.id || '',
      // Blank on purpose: the backend reads an absent userId as "keep the
      // current link" rather than "unlink".
      userId: '',
      isActive: doctor.isActive ?? true,
    });
    setFieldErrors({});
    setModalOpen(true);
  };

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData((prev) => ({ ...prev, [name]: type === 'checkbox' ? checked : value }));
    setFieldErrors((prev) => ({ ...prev, [name]: undefined }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setFieldErrors({});

    const payload = {
      ...formData,
      departmentId: formData.departmentId ? Number(formData.departmentId) : null,
      userId: formData.userId ? Number(formData.userId) : null,
    };

    try {
      const result = editingId
        ? await doctorService.updateDoctor(editingId, payload)
        : await doctorService.createDoctor(payload);

      notify.success(result.message);
      setModalOpen(false);
      loadDoctors();
    } catch (err) {
      const body = err.response?.data;
      if (body?.data && typeof body.data === 'object') {
        setFieldErrors(body.data);
        notify.error(body.message || 'Please correct the highlighted fields');
      } else {
        notify.error(body?.message || 'Could not save the doctor');
      }
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    setDeleting(true);
    try {
      const result = await doctorService.deleteDoctor(confirmTarget.id);
      notify.success(result.message);
      setConfirmTarget(null);
      if (doctors.length === 1 && page > 0) {
        setPage(page - 1);
      } else {
        loadDoctors();
      }
    } catch (err) {
      notify.error(err.response?.data?.message || 'Could not delete the doctor');
    } finally {
      setDeleting(false);
    }
  };

  const columns = [
    {
      key: 'firstName',
      label: 'Doctor',
      render: (_value, row) => (
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-full bg-info-100 text-info-700 flex items-center justify-center text-xs font-semibold flex-shrink-0">
            {`${row.firstName?.[0] || ''}${row.lastName?.[0] || ''}`.toUpperCase()}
          </div>
          <div className="min-w-0">
            <p className="font-medium text-gray-900 truncate">
              Dr. {row.firstName} {row.lastName}
            </p>
            <p className="text-xs text-gray-500 truncate">{row.email}</p>
          </div>
        </div>
      ),
    },
    { key: 'specialization', label: 'Specialization' },
    {
      key: 'department',
      label: 'Department',
      render: (value) => value?.name || <span className="text-gray-400">—</span>,
    },
    { key: 'phone', label: 'Phone' },
    {
      key: 'licenseNumber',
      label: 'License',
      render: (value) => <span className="font-mono text-xs">{value}</span>,
    },
    {
      key: 'isActive',
      label: 'Status',
      render: (value) => (
        <Badge variant={value ? 'success' : 'gray'} size="sm">
          {value ? 'Active' : 'Inactive'}
        </Badge>
      ),
    },
  ];

  const showingFrom = totalElements === 0 ? 0 : page * PAGE_SIZE + 1;
  const showingTo = Math.min((page + 1) * PAGE_SIZE, totalElements);
  const hasFilter = !!departmentFilter || !!specialization.trim();

  return (
    <div className="p-6 space-y-5">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Doctors</h1>
          <p className="text-sm text-gray-600 mt-1">
            Medical staff, their specialisations and departments.
          </p>
        </div>
        {canManage && (
          <Button onClick={openCreate} icon={Plus}>
            Add Doctor
          </Button>
        )}
      </div>

      {/* Filters. Department and specialisation are separate endpoints, so only
          one can apply at a time; choosing a department clears the other. */}
      <div className="bg-white rounded-lg border border-gray-200 shadow-sm p-4">
        <div className="flex flex-col sm:flex-row gap-3">
          <SearchBar
            // SearchBar keeps its text in internal state with no way to reset it
            // from outside; remounting on department change clears the box so it
            // cannot display a filter that is no longer applied.
            key={`spec-${departmentFilter}`}
            className="flex-1"
            placeholder="Filter by specialization..."
            onSearch={(term) => {
              setPage(0);
              setDepartmentFilter('');
              setSpecialization(term);
            }}
          />
          <select
            value={departmentFilter}
            onChange={(e) => {
              setPage(0);
              setSpecialization('');
              setDepartmentFilter(e.target.value);
            }}
            className="px-4 py-2 border border-gray-300 rounded-lg outline-none focus:ring-2 focus:ring-primary-500 sm:w-56"
            aria-label="Filter by department"
          >
            <option value="">All departments</option>
            {departments.map((d) => (
              <option key={d.id} value={d.id}>
                {d.name}
              </option>
            ))}
          </select>
        </div>
      </div>

      {loading ? (
        <div className="bg-white rounded-lg border border-gray-200 shadow-sm p-6 space-y-3">
          {[0, 1, 2, 3, 4].map((i) => (
            <div key={i} className="skeleton h-12 w-full" />
          ))}
        </div>
      ) : doctors.length === 0 ? (
        <div className="bg-white rounded-lg border border-gray-200 shadow-sm">
          <EmptyState
            icon={UserCog}
            title={hasFilter ? 'No matching doctors' : 'No doctors yet'}
            description={
              hasFilter
                ? 'Nothing matched that filter. Try another department or specialization.'
                : 'Add your first doctor to get started.'
            }
            action={canManage && !hasFilter ? openCreate : undefined}
            actionLabel="Add Doctor"
          />
        </div>
      ) : (
        <>
          <Table
            columns={columns}
            data={doctors}
            onEdit={canManage ? openEdit : undefined}
            onDelete={canManage ? (row) => setConfirmTarget(row) : undefined}
          />

          <div className="flex flex-wrap items-center justify-between gap-3 px-1">
            <p className="text-sm text-gray-600">
              Showing {showingFrom}–{showingTo} of {totalElements}
            </p>
            {totalPages > 1 && (
              <div className="flex items-center gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  icon={ChevronLeft}
                  disabled={page === 0}
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                >
                  Previous
                </Button>
                <span className="text-sm text-gray-600 px-2">
                  Page {page + 1} of {totalPages}
                </span>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={page >= totalPages - 1}
                  onClick={() => setPage((p) => p + 1)}
                >
                  Next
                  <ChevronRight className="w-4 h-4" />
                </Button>
              </div>
            )}
          </div>
        </>
      )}

      <Modal
        isOpen={modalOpen}
        title={editingId ? 'Edit Doctor' : 'Add Doctor'}
        onClose={() => !saving && setModalOpen(false)}
        size="2xl"
        closeOnBackdrop={!saving}
      >
        <form onSubmit={handleSubmit} className="space-y-6">
          <section>
            <h4 className="text-xs font-semibold uppercase tracking-wide text-primary-600 mb-3">
              Personal Information
            </h4>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <InputField
                label="First name"
                name="firstName"
                required
                value={formData.firstName}
                onChange={handleChange}
                error={fieldErrors.firstName}
                touched
              />
              <InputField
                label="Last name"
                name="lastName"
                required
                value={formData.lastName}
                onChange={handleChange}
                error={fieldErrors.lastName}
                touched
              />
            </div>
          </section>

          <section className="border-t border-gray-200 pt-5">
            <h4 className="text-xs font-semibold uppercase tracking-wide text-primary-600 mb-3">
              Professional Details
            </h4>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <InputField
                label="Specialization"
                name="specialization"
                required
                placeholder="e.g. Cardiology"
                value={formData.specialization}
                onChange={handleChange}
                error={fieldErrors.specialization}
                touched
              />
              <InputField
                label="License number"
                name="licenseNumber"
                required
                value={formData.licenseNumber}
                onChange={handleChange}
                error={fieldErrors.licenseNumber}
                touched
              />
              <div className="sm:col-span-2">
                <SelectField
                  label="Department"
                  name="departmentId"
                  required
                  placeholder="Select a department"
                  options={departments.map((d) => ({ value: d.id, label: d.name }))}
                  value={formData.departmentId}
                  onChange={handleChange}
                  error={fieldErrors.departmentId}
                  touched
                />
              </div>
            </div>
          </section>

          <section className="border-t border-gray-200 pt-5">
            <h4 className="text-xs font-semibold uppercase tracking-wide text-primary-600 mb-3">
              Contact
            </h4>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <InputField
                label="Phone"
                name="phone"
                required
                value={formData.phone}
                onChange={handleChange}
                error={fieldErrors.phone}
                touched
              />
              <InputField
                label="Email"
                name="email"
                type="email"
                required
                value={formData.email}
                onChange={handleChange}
                error={fieldErrors.email}
                touched
              />
            </div>
          </section>

          <section className="border-t border-gray-200 pt-5">
            <h4 className="text-xs font-semibold uppercase tracking-wide text-primary-600 mb-3">
              Login Account
            </h4>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <InputField
                label="Linked user ID"
                name="userId"
                type="number"
                placeholder="Optional"
                value={formData.userId}
                onChange={handleChange}
                error={fieldErrors.userId}
                touched
              />
              <div className="flex items-end pb-2">
                <label className="flex items-center gap-2 text-sm text-gray-700">
                  <input
                    type="checkbox"
                    name="isActive"
                    checked={formData.isActive}
                    onChange={handleChange}
                    className="rounded border-gray-300"
                  />
                  Active
                </label>
              </div>
            </div>
            <p className="text-xs text-gray-500 mt-2">
              Links this record to a login so the doctor can manage their own profile.
              {editingId && ' Leave blank to keep the current link.'}
            </p>
          </section>

          <div className="flex justify-end gap-3 border-t border-gray-200 pt-5">
            <Button
              type="button"
              variant="outline"
              onClick={() => setModalOpen(false)}
              disabled={saving}
            >
              Cancel
            </Button>
            <Button type="submit" loading={saving}>
              {editingId ? 'Save changes' : 'Add doctor'}
            </Button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        isOpen={!!confirmTarget}
        title="Delete doctor"
        message={
          confirmTarget
            ? `Delete Dr. ${confirmTarget.firstName} ${confirmTarget.lastName}? The record is deactivated, not permanently removed.`
            : ''
        }
        loading={deleting}
        onConfirm={handleDelete}
        onCancel={() => !deleting && setConfirmTarget(null)}
      />
    </div>
  );
}

export default DoctorManagement;
