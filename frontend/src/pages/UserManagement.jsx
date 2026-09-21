import { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import { Plus, ShieldCheck, Link2, Link2Off, ChevronLeft, ChevronRight, X } from 'lucide-react';
import Table from '../components/common/Table';
import Modal from '../components/common/Modal';
import ConfirmDialog from '../components/common/ConfirmDialog';
import Button from '../components/common/Button';
import Badge from '../components/common/Badge';
import EmptyState from '../components/common/EmptyState';
import InputField from '../components/forms/InputField';
import SelectField from '../components/forms/SelectField';
import { useNotification } from '../hooks/useNotification';
import { useAuth } from '../hooks/useAuth';
import userService from '../services/userService';
import doctorService from '../services/doctorService';
import patientService from '../services/patientService';

const PAGE_SIZE = 10;

const ROLES = ['ADMIN', 'DOCTOR', 'STAFF', 'PATIENT'];

const ROLE_VARIANT = {
  ADMIN: 'primary',
  DOCTOR: 'info',
  STAFF: 'warning',
  PATIENT: 'gray',
};

// Only these two roles have a clinical record a login can be attached to.
const LINKABLE_ROLES = ['DOCTOR', 'PATIENT'];

const EMPTY_FORM = {
  username: '',
  email: '',
  password: '',
  firstName: '',
  lastName: '',
  phone: '',
  role: 'STAFF',
  isActive: true,
};

const fullName = (person) =>
  person ? `${person.firstName || ''} ${person.lastName || ''}`.trim() : '';

function UserManagement() {
  const { user: currentUser } = useAuth();
  const notify = useNotification();

  const [users, setUsers] = useState([]);
  const [doctors, setDoctors] = useState([]);
  const [patients, setPatients] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);

  const [roleFilter, setRoleFilter] = useState('');

  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [formData, setFormData] = useState(EMPTY_FORM);
  const [fieldErrors, setFieldErrors] = useState({});
  const [saving, setSaving] = useState(false);

  const [linkTarget, setLinkTarget] = useState(null);
  const [linkRecordId, setLinkRecordId] = useState('');
  const [linking, setLinking] = useState(false);

  const [confirmTarget, setConfirmTarget] = useState(null);
  const [deleting, setDeleting] = useState(false);

  // Doctor and patient records carry a read-only userId, which is how this
  // page knows which login already owns which record.
  const loadRecords = useCallback(() => {
    doctorService
      .getAllDoctors()
      .then((result) => setDoctors(result.data || []))
      .catch(() => notify.error('Could not load doctor records'));

    patientService
      .getAllPatients()
      .then((result) => setPatients(result.data || []))
      .catch(() => notify.error('Could not load patient records'));
  }, [notify]);

  useEffect(() => {
    loadRecords();
  }, [loadRecords]);

  const linkedRecordFor = useCallback(
    (user) => {
      if (user.role === 'DOCTOR') {
        const doctor = doctors.find((d) => d.userId === user.id);
        return doctor ? { kind: 'Doctor', label: `Dr. ${fullName(doctor)}` } : null;
      }
      if (user.role === 'PATIENT') {
        const patient = patients.find((p) => p.userId === user.id);
        return patient ? { kind: 'Patient', label: fullName(patient) } : null;
      }
      return null;
    },
    [doctors, patients]
  );

  const requestId = useRef(0);

  const loadUsers = useCallback(async () => {
    const ticket = ++requestId.current;
    setLoading(true);
    try {
      const result = roleFilter
        ? await userService.getUsersByRolePaginated(roleFilter, page, PAGE_SIZE)
        : await userService.getAllUsersPaginated(page, PAGE_SIZE);

      if (ticket !== requestId.current) return;
      const pageData = result.data || {};
      setUsers(pageData.content || []);
      setTotalPages(pageData.totalPages || 0);
      setTotalElements(pageData.totalElements || 0);
    } catch (err) {
      if (ticket !== requestId.current) return;
      notify.error(err.response?.data?.message || 'Could not load users');
      setUsers([]);
      setTotalPages(0);
      setTotalElements(0);
    } finally {
      if (ticket === requestId.current) setLoading(false);
    }
  }, [page, roleFilter, notify]);

  useEffect(() => {
    loadUsers();
  }, [loadUsers]);

  const refresh = () => {
    loadUsers();
    loadRecords();
  };

  const openCreate = () => {
    setEditingId(null);
    setFormData(EMPTY_FORM);
    setFieldErrors({});
    setModalOpen(true);
  };

  const openEdit = (user) => {
    setEditingId(user.id);
    setFormData({
      username: user.username || '',
      email: user.email || '',
      password: '',
      firstName: user.firstName || '',
      lastName: user.lastName || '',
      phone: user.phone || '',
      role: user.role || 'STAFF',
      isActive: user.isActive ?? true,
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

    const base = {
      username: formData.username.trim(),
      email: formData.email.trim(),
      firstName: formData.firstName.trim(),
      lastName: formData.lastName.trim(),
      phone: formData.phone.trim() || null,
      role: formData.role,
    };

    try {
      let result;
      if (editingId) {
        // A blank password means "leave it alone", so it is omitted rather
        // than sent empty - the field has a minimum length.
        const payload = { ...base, isActive: formData.isActive };
        if (formData.password) payload.password = formData.password;
        result = await userService.updateUser(editingId, payload);
      } else {
        result = await userService.createUser({ ...base, password: formData.password });
      }

      notify.success(result.message);
      setModalOpen(false);
      refresh();
    } catch (err) {
      const body = err.response?.data;
      if (body?.data && typeof body.data === 'object') {
        setFieldErrors(body.data);
        notify.error(body.message || 'Please correct the highlighted fields');
      } else {
        notify.error(body?.message || 'Could not save the user');
      }
    } finally {
      setSaving(false);
    }
  };

  const handleDeactivate = async () => {
    setDeleting(true);
    try {
      const result = await userService.deactivateUser(confirmTarget.id);
      notify.success(result.message);
      setConfirmTarget(null);
      if (users.length === 1 && page > 0) {
        setPage(page - 1);
      } else {
        refresh();
      }
    } catch (err) {
      notify.error(err.response?.data?.message || 'Could not deactivate the user');
    } finally {
      setDeleting(false);
    }
  };

  const openLink = (user) => {
    const existing = linkedRecordFor(user);
    setLinkTarget(user);
    setLinkRecordId('');
    // Re-opening on a linked user starts blank, so choosing nothing and
    // cancelling cannot silently move the link.
    if (existing) setLinkRecordId('');
  };

  /**
   * Linking is a write on the clinical record, not on the login: the record's
   * own update endpoint takes a userId. So this sends the record back with
   * every field it already had plus the new link.
   */
  const handleLink = async () => {
    if (!linkRecordId) return;
    setLinking(true);
    const isDoctor = linkTarget.role === 'DOCTOR';

    try {
      if (isDoctor) {
        const doctor = doctors.find((d) => String(d.id) === String(linkRecordId));
        await doctorService.updateDoctor(doctor.id, {
          firstName: doctor.firstName,
          lastName: doctor.lastName,
          email: doctor.email,
          phone: doctor.phone,
          specialization: doctor.specialization,
          licenseNumber: doctor.licenseNumber,
          departmentId: doctor.department?.id ?? null,
          userId: linkTarget.id,
          isActive: doctor.isActive,
        });
      } else {
        const patient = patients.find((p) => String(p.id) === String(linkRecordId));
        await patientService.updatePatient(patient.id, {
          firstName: patient.firstName,
          lastName: patient.lastName,
          email: patient.email,
          phone: patient.phone,
          dateOfBirth: patient.dateOfBirth,
          gender: patient.gender,
          address: patient.address,
          city: patient.city,
          state: patient.state,
          zipCode: patient.zipCode,
          bloodGroup: patient.bloodGroup,
          emergencyContact: patient.emergencyContact,
          emergencyPhone: patient.emergencyPhone,
          roomId: patient.room?.id ?? null,
          userId: linkTarget.id,
          isActive: patient.isActive,
        });
      }

      notify.success('Login linked to the record');
      setLinkTarget(null);
      refresh();
    } catch (err) {
      notify.error(err.response?.data?.message || 'Could not link the record');
    } finally {
      setLinking(false);
    }
  };

  const linkOptions = useMemo(() => {
    if (!linkTarget) return [];
    const isDoctor = linkTarget.role === 'DOCTOR';
    const records = isDoctor ? doctors : patients;
    return records
      // A record already claimed by a different login is not offered.
      .filter((r) => !r.userId || r.userId === linkTarget.id)
      .map((r) => ({
        value: r.id,
        label: `${isDoctor ? 'Dr. ' : ''}${fullName(r)} · ${r.email}${
          r.userId === linkTarget.id ? ' (currently linked)' : ''
        }`,
      }));
  }, [linkTarget, doctors, patients]);

  const columns = [
    {
      key: 'username',
      label: 'User',
      render: (value, row) => (
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-full bg-primary-100 text-primary-700 flex items-center justify-center flex-shrink-0 text-xs font-semibold">
            {(row.firstName?.[0] || '') + (row.lastName?.[0] || '') || '?'}
          </div>
          <div>
            <p className="font-medium text-gray-900">
              {fullName(row) || value}
              {row.id === currentUser?.userId && (
                <span className="ml-2 text-xs font-normal text-gray-500">(you)</span>
              )}
            </p>
            <p className="text-xs text-gray-500">@{value}</p>
          </div>
        </div>
      ),
    },
    {
      key: 'email',
      label: 'Email',
      render: (value) => <span className="text-gray-600">{value}</span>,
    },
    {
      key: 'phone',
      label: 'Phone',
      render: (value) => value || <span className="text-gray-400">—</span>,
    },
    {
      key: 'role',
      label: 'Role',
      render: (value) => (
        <Badge variant={ROLE_VARIANT[value] || 'gray'} size="sm">
          {value}
        </Badge>
      ),
    },
    {
      key: 'id',
      label: 'Linked record',
      render: (_value, row) => {
        if (!LINKABLE_ROLES.includes(row.role)) {
          return <span className="text-gray-400">—</span>;
        }
        const linked = linkedRecordFor(row);
        return (
          <div className="flex items-center gap-2">
            {linked ? (
              <span className="inline-flex items-center gap-1.5 text-gray-700">
                <Link2 className="w-3.5 h-3.5 text-success-600" />
                {linked.label}
              </span>
            ) : (
              <span className="inline-flex items-center gap-1.5 text-gray-400">
                <Link2Off className="w-3.5 h-3.5" />
                Not linked
              </span>
            )}
            <button
              onClick={() => openLink(row)}
              className="text-xs font-medium text-primary-700 hover:text-primary-800 hover:underline"
            >
              {linked ? 'Change' : 'Link'}
            </button>
          </div>
        );
      },
    },
  ];

  const showingFrom = totalElements === 0 ? 0 : page * PAGE_SIZE + 1;
  const showingTo = Math.min((page + 1) * PAGE_SIZE, totalElements);

  return (
    <div className="p-6 space-y-5">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Users</h1>
          <p className="text-sm text-gray-600 mt-1">
            Login accounts, their roles, and the records they belong to.
          </p>
        </div>
        <Button onClick={openCreate} icon={Plus}>
          Add User
        </Button>
      </div>

      <div className="bg-white rounded-lg border border-gray-200 shadow-sm p-4">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <SelectField
            label="Role"
            placeholder="Any role"
            value={roleFilter}
            onChange={(e) => {
              setRoleFilter(e.target.value);
              setPage(0);
            }}
            options={ROLES.map((r) => ({ value: r, label: r }))}
          />
          <div className="flex items-end">
            {roleFilter && (
              <Button
                variant="outline"
                icon={X}
                onClick={() => {
                  setRoleFilter('');
                  setPage(0);
                }}
              >
                Clear filter
              </Button>
            )}
          </div>
        </div>
        <p className="text-xs text-gray-500 mt-3">
          Only DOCTOR and PATIENT logins link to a record. A doctor login must be linked
          before it can edit its own profile or see its own appointments.
        </p>
      </div>

      {loading ? (
        <div className="bg-white rounded-lg border border-gray-200 shadow-sm p-6 space-y-3">
          {[0, 1, 2, 3].map((i) => (
            <div key={i} className="skeleton h-12 w-full" />
          ))}
        </div>
      ) : users.length === 0 ? (
        <div className="bg-white rounded-lg border border-gray-200 shadow-sm">
          <EmptyState
            icon={ShieldCheck}
            title={roleFilter ? `No ${roleFilter} accounts` : 'No users yet'}
            description={
              roleFilter
                ? 'Try a different role.'
                : 'Create the first login account.'
            }
            action={roleFilter ? () => setRoleFilter('') : openCreate}
            actionLabel={roleFilter ? 'Clear filter' : 'Add User'}
          />
        </div>
      ) : (
        <>
          <Table
            columns={columns}
            data={users}
            onEdit={openEdit}
            onDelete={(row) => setConfirmTarget(row)}
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
        title={editingId ? 'Edit User' : 'Add User'}
        onClose={() => !saving && setModalOpen(false)}
        size="2xl"
        closeOnBackdrop={!saving}
      >
        <form onSubmit={handleSubmit} className="space-y-5">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
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

          <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
            <InputField
              label="Username"
              name="username"
              required
              placeholder="Used to sign in alongside the email"
              value={formData.username}
              onChange={handleChange}
              error={fieldErrors.username}
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

          <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
            <InputField
              label="Phone"
              name="phone"
              value={formData.phone}
              onChange={handleChange}
              error={fieldErrors.phone}
              touched
            />
            <SelectField
              label="Role"
              name="role"
              required
              placeholder="Select a role"
              value={formData.role}
              onChange={handleChange}
              options={ROLES.map((r) => ({ value: r, label: r }))}
              error={fieldErrors.role}
              touched
            />
            <InputField
              label={editingId ? 'New password' : 'Password'}
              name="password"
              type="password"
              required={!editingId}
              placeholder={editingId ? 'Leave blank to keep it' : 'At least 6 characters'}
              value={formData.password}
              onChange={handleChange}
              error={fieldErrors.password}
              touched
            />
          </div>

          {editingId && (
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
          )}

          {editingId === currentUser?.userId && (
            <p className="text-xs text-gray-500">
              This is your own account. The server will refuse a change to your role or a
              deactivation, so you cannot lock yourself out.
            </p>
          )}

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
              {editingId ? 'Save changes' : 'Add user'}
            </Button>
          </div>
        </form>
      </Modal>

      <Modal
        isOpen={!!linkTarget}
        title={linkTarget ? `Link ${fullName(linkTarget)}` : 'Link record'}
        onClose={() => !linking && setLinkTarget(null)}
        size="lg"
        closeOnBackdrop={!linking}
      >
        {linkTarget && (
          <div className="space-y-5">
            <p className="text-sm text-gray-600">
              Attach this {linkTarget.role.toLowerCase()} login to its{' '}
              {linkTarget.role === 'DOCTOR' ? 'doctor' : 'patient'} record. Ownership
              checks on the API use this link, so without it the account can sign in but
              cannot reach its own data.
            </p>

            {linkOptions.length === 0 ? (
              <p className="text-sm text-gray-500">
                Every {linkTarget.role === 'DOCTOR' ? 'doctor' : 'patient'} record is
                already claimed by another login. Create the record first, or unlink the
                other account.
              </p>
            ) : (
              <SelectField
                label={linkTarget.role === 'DOCTOR' ? 'Doctor record' : 'Patient record'}
                placeholder="Select a record"
                value={linkRecordId}
                onChange={(e) => setLinkRecordId(e.target.value)}
                options={linkOptions}
              />
            )}

            <div className="flex justify-end gap-3 border-t border-gray-200 pt-5">
              <Button
                variant="outline"
                onClick={() => setLinkTarget(null)}
                disabled={linking}
              >
                Cancel
              </Button>
              <Button onClick={handleLink} loading={linking} disabled={!linkRecordId}>
                Link record
              </Button>
            </div>
          </div>
        )}
      </Modal>

      <ConfirmDialog
        isOpen={!!confirmTarget}
        title="Deactivate user"
        message={
          confirmTarget
            ? `Deactivate ${fullName(confirmTarget) || confirmTarget.username}? They will no longer be able to sign in. The account is kept so their audit trail still resolves.`
            : ''
        }
        loading={deleting}
        onConfirm={handleDeactivate}
        onCancel={() => !deleting && setConfirmTarget(null)}
      />
    </div>
  );
}

export default UserManagement;
