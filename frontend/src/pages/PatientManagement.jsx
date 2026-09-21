import { useState, useEffect, useCallback, useRef } from 'react';
import { Plus, Users, ChevronLeft, ChevronRight } from 'lucide-react';
import Table from '../components/common/Table';
import SearchBar from '../components/common/SearchBar';
import Modal from '../components/common/Modal';
import ConfirmDialog from '../components/common/ConfirmDialog';
import Button from '../components/common/Button';
import Badge from '../components/common/Badge';
import EmptyState from '../components/common/EmptyState';
import InputField from '../components/forms/InputField';
import SelectField from '../components/forms/SelectField';
import DateField from '../components/forms/DateField';
import { useNotification } from '../hooks/useNotification';
import { useAuth } from '../hooks/useAuth';
import patientService from '../services/patientService';

const PAGE_SIZE = 10;

const EMPTY_FORM = {
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
  userId: '',
  isActive: true,
};

// The backend exposes one endpoint per searchable field rather than a single
// query, so the field being searched has to be chosen explicitly.
const SEARCH_FIELDS = [
  { value: 'firstName', label: 'First name' },
  { value: 'lastName', label: 'Last name' },
  { value: 'city', label: 'City' },
];

const GENDERS = ['Male', 'Female', 'Other'];

function PatientManagement() {
  const { role } = useAuth();
  const notify = useNotification();

  const canCreate = role === 'ADMIN' || role === 'STAFF';
  const canEdit = role === 'ADMIN' || role === 'STAFF';
  const canDelete = role === 'ADMIN';

  const [patients, setPatients] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);

  const [searchTerm, setSearchTerm] = useState('');
  const [searchField, setSearchField] = useState('firstName');

  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [formData, setFormData] = useState(EMPTY_FORM);
  const [fieldErrors, setFieldErrors] = useState({});
  const [saving, setSaving] = useState(false);

  const [confirmTarget, setConfirmTarget] = useState(null);
  const [deleting, setDeleting] = useState(false);

  // Changing the search term or field in quick succession leaves two requests
  // in flight, and whichever answers last wins - which may be the one for the
  // term the user already moved off. Each load takes a ticket and drops its
  // result if a newer load started meanwhile.
  const requestId = useRef(0);

  const loadPatients = useCallback(async () => {
    const ticket = ++requestId.current;
    setLoading(true);
    try {
      let result;
      if (!searchTerm.trim()) {
        result = await patientService.getAllPatientsPaginated(page, PAGE_SIZE);
      } else if (searchField === 'firstName') {
        result = await patientService.searchByFirstNamePaginated(searchTerm, page, PAGE_SIZE);
      } else if (searchField === 'lastName') {
        result = await patientService.searchByLastNamePaginated(searchTerm, page, PAGE_SIZE);
      } else {
        result = await patientService.searchByCityPaginated(searchTerm, page, PAGE_SIZE);
      }

      if (ticket !== requestId.current) return;
      const pageData = result.data || {};
      setPatients(pageData.content || []);
      setTotalPages(pageData.totalPages || 0);
      setTotalElements(pageData.totalElements || 0);
    } catch (err) {
      if (ticket !== requestId.current) return;
      notify.error(err.response?.data?.message || 'Could not load patients');
      setPatients([]);
      setTotalPages(0);
      setTotalElements(0);
    } finally {
      if (ticket === requestId.current) setLoading(false);
    }
  }, [page, searchTerm, searchField, notify]);

  useEffect(() => {
    loadPatients();
  }, [loadPatients]);

  const handleSearch = (term) => {
    setPage(0); // a new query starts from the first page, not the current one
    setSearchTerm(term);
  };

  const openCreate = () => {
    setEditingId(null);
    setFormData(EMPTY_FORM);
    setFieldErrors({});
    setModalOpen(true);
  };

  const openEdit = (patient) => {
    setEditingId(patient.id);
    setFormData({
      firstName: patient.firstName || '',
      lastName: patient.lastName || '',
      email: patient.email || '',
      phone: patient.phone || '',
      dateOfBirth: patient.dateOfBirth || '',
      gender: patient.gender || 'Male',
      address: patient.address || '',
      city: patient.city || '',
      state: patient.state || '',
      zipCode: patient.zipCode || '',
      bloodGroup: patient.bloodGroup || '',
      emergencyContact: patient.emergencyContact || '',
      emergencyPhone: patient.emergencyPhone || '',
      // Left blank deliberately: the backend treats an absent userId as
      // "leave the existing link alone" rather than "unlink".
      userId: '',
      isActive: patient.isActive ?? true,
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
      email: formData.email.trim() || null,
      bloodGroup: formData.bloodGroup.trim() || null,
      emergencyContact: formData.emergencyContact.trim() || null,
      emergencyPhone: formData.emergencyPhone.trim() || null,
      userId: formData.userId ? Number(formData.userId) : null,
    };

    try {
      const result = editingId
        ? await patientService.updatePatient(editingId, payload)
        : await patientService.createPatient(payload);

      notify.success(result.message);
      setModalOpen(false);
      loadPatients();
    } catch (err) {
      const body = err.response?.data;
      // A validation failure returns a field -> message map in `data`;
      // anything else (404, 409, 400) is a single message.
      if (body?.data && typeof body.data === 'object') {
        setFieldErrors(body.data);
        notify.error(body.message || 'Please correct the highlighted fields');
      } else {
        notify.error(body?.message || 'Could not save the patient');
      }
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    setDeleting(true);
    try {
      const result = await patientService.deletePatient(confirmTarget.id);
      notify.success(result.message);
      setConfirmTarget(null);
      // Deleting the only row on the last page would leave it empty.
      if (patients.length === 1 && page > 0) {
        setPage(page - 1);
      } else {
        loadPatients();
      }
    } catch (err) {
      notify.error(err.response?.data?.message || 'Could not delete the patient');
    } finally {
      setDeleting(false);
    }
  };

  const columns = [
    {
      key: 'firstName',
      label: 'Patient',
      render: (_value, row) => (
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-full bg-primary-100 text-primary-700 flex items-center justify-center text-xs font-semibold flex-shrink-0">
            {`${row.firstName?.[0] || ''}${row.lastName?.[0] || ''}`.toUpperCase()}
          </div>
          <div className="min-w-0">
            <p className="font-medium text-gray-900 truncate">
              {row.firstName} {row.lastName}
            </p>
            <p className="text-xs text-gray-500 truncate">{row.email || 'No email'}</p>
          </div>
        </div>
      ),
    },
    { key: 'gender', label: 'Gender' },
    { key: 'phone', label: 'Phone' },
    { key: 'city', label: 'City' },
    {
      key: 'bloodGroup',
      label: 'Blood',
      render: (value) => value || <span className="text-gray-400">—</span>,
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

  return (
    <div className="p-6 space-y-5">
      {/* Header */}
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Patients</h1>
          <p className="text-sm text-gray-600 mt-1">
            Register, search and manage patient records.
          </p>
        </div>
        {canCreate && (
          <Button onClick={openCreate} icon={Plus}>
            Add Patient
          </Button>
        )}
      </div>

      {/* Toolbar */}
      <div className="bg-white rounded-lg border border-gray-200 shadow-sm p-4">
        <div className="flex flex-col sm:flex-row gap-3">
          <SearchBar
            className="flex-1"
            placeholder={`Search by ${SEARCH_FIELDS.find((f) => f.value === searchField)?.label.toLowerCase()}...`}
            onSearch={handleSearch}
          />
          <select
            value={searchField}
            onChange={(e) => {
              setSearchField(e.target.value);
              setPage(0);
            }}
            className="px-4 py-2 border border-gray-300 rounded-lg outline-none focus:ring-2 focus:ring-primary-500 sm:w-44"
            aria-label="Search field"
          >
            {SEARCH_FIELDS.map((f) => (
              <option key={f.value} value={f.value}>
                {f.label}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Table */}
      {loading ? (
        <div className="bg-white rounded-lg border border-gray-200 shadow-sm p-6 space-y-3">
          {[0, 1, 2, 3, 4].map((i) => (
            <div key={i} className="skeleton h-12 w-full" />
          ))}
        </div>
      ) : patients.length === 0 ? (
        <div className="bg-white rounded-lg border border-gray-200 shadow-sm">
          <EmptyState
            icon={Users}
            title={searchTerm ? 'No matching patients' : 'No patients yet'}
            description={
              searchTerm
                ? `Nothing matched "${searchTerm}". Try a different term or search field.`
                : 'Register your first patient to get started.'
            }
            action={canCreate && !searchTerm ? openCreate : undefined}
            actionLabel="Add Patient"
          />
        </div>
      ) : (
        <>
          <Table
            columns={columns}
            data={patients}
            onEdit={canEdit ? openEdit : undefined}
            onDelete={canDelete ? (row) => setConfirmTarget(row) : undefined}
          />

          {/* Server-side pager. Table paginates whatever array it is given,
              so it is handed exactly one page and its own pager stays hidden. */}
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

      {/* Create / edit */}
      <Modal
        isOpen={modalOpen}
        title={editingId ? 'Edit Patient' : 'Register Patient'}
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
              <DateField
                label="Date of birth"
                name="dateOfBirth"
                required
                value={formData.dateOfBirth}
                onChange={handleChange}
                error={fieldErrors.dateOfBirth}
                touched
              />
              <SelectField
                label="Gender"
                name="gender"
                required
                options={GENDERS}
                placeholder="Select gender"
                value={formData.gender}
                onChange={handleChange}
                error={fieldErrors.gender}
                touched
              />
              <InputField
                label="Blood group"
                name="bloodGroup"
                placeholder="e.g. O+"
                value={formData.bloodGroup}
                onChange={handleChange}
                error={fieldErrors.bloodGroup}
                touched
              />
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
                value={formData.email}
                onChange={handleChange}
                error={fieldErrors.email}
                touched
              />
            </div>
          </section>

          <section className="border-t border-gray-200 pt-5">
            <h4 className="text-xs font-semibold uppercase tracking-wide text-primary-600 mb-3">
              Address
            </h4>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="sm:col-span-2">
                <InputField
                  label="Address"
                  name="address"
                  required
                  value={formData.address}
                  onChange={handleChange}
                  error={fieldErrors.address}
                  touched
                />
              </div>
              <InputField
                label="City"
                name="city"
                required
                value={formData.city}
                onChange={handleChange}
                error={fieldErrors.city}
                touched
              />
              <InputField
                label="State"
                name="state"
                required
                value={formData.state}
                onChange={handleChange}
                error={fieldErrors.state}
                touched
              />
              <InputField
                label="Zip code"
                name="zipCode"
                required
                value={formData.zipCode}
                onChange={handleChange}
                error={fieldErrors.zipCode}
                touched
              />
            </div>
          </section>

          <section className="border-t border-gray-200 pt-5">
            <h4 className="text-xs font-semibold uppercase tracking-wide text-primary-600 mb-3">
              Emergency Contact
            </h4>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <InputField
                label="Contact name"
                name="emergencyContact"
                value={formData.emergencyContact}
                onChange={handleChange}
                error={fieldErrors.emergencyContact}
                touched
              />
              <InputField
                label="Contact phone"
                name="emergencyPhone"
                value={formData.emergencyPhone}
                onChange={handleChange}
                error={fieldErrors.emergencyPhone}
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
              Links this record to a login so the patient can see their own details.
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
              {editingId ? 'Save changes' : 'Register patient'}
            </Button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        isOpen={!!confirmTarget}
        title="Delete patient"
        message={
          confirmTarget
            ? `Delete ${confirmTarget.firstName} ${confirmTarget.lastName}? The record is deactivated, not permanently removed.`
            : ''
        }
        loading={deleting}
        onConfirm={handleDelete}
        onCancel={() => !deleting && setConfirmTarget(null)}
      />
    </div>
  );
}

export default PatientManagement;
