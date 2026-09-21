import { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import { Plus, FileText, Pill, ChevronLeft, ChevronRight, X } from 'lucide-react';
import Table from '../components/common/Table';
import Modal from '../components/common/Modal';
import ConfirmDialog from '../components/common/ConfirmDialog';
import Button from '../components/common/Button';
import Badge from '../components/common/Badge';
import EmptyState from '../components/common/EmptyState';
import InputField from '../components/forms/InputField';
import SelectField from '../components/forms/SelectField';
import TextAreaField from '../components/forms/TextAreaField';
import { useNotification } from '../hooks/useNotification';
import { useAuth } from '../hooks/useAuth';
import prescriptionService from '../services/prescriptionService';
import appointmentService from '../services/appointmentService';

const PAGE_SIZE = 10;

// Stored as free text; these are the values the API documents.
const STATUSES = ['ACTIVE', 'COMPLETED', 'EXPIRED'];

const STATUS_VARIANT = {
  ACTIVE: 'success',
  COMPLETED: 'info',
  EXPIRED: 'gray',
};

const EMPTY_FORM = {
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
};

const fullName = (person) =>
  person ? `${person.firstName || ''} ${person.lastName || ''}`.trim() : '';

const formatDate = (value) => {
  if (!value) return '';
  return new Date(`${value}T00:00:00`).toLocaleDateString(undefined, {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  });
};

function PrescriptionManagement() {
  const { role } = useAuth();
  const notify = useNotification();

  // The list endpoint is ADMIN/STAFF, and so is writing. Deleting is stricter:
  // the controller requires ADMIN.
  const canManage = role === 'ADMIN' || role === 'STAFF';
  const canDelete = role === 'ADMIN';

  const [prescriptions, setPrescriptions] = useState([]);
  const [appointments, setAppointments] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);

  const [statusFilter, setStatusFilter] = useState('');
  const [appointmentFilter, setAppointmentFilter] = useState('');

  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [formData, setFormData] = useState(EMPTY_FORM);
  const [fieldErrors, setFieldErrors] = useState({});
  const [saving, setSaving] = useState(false);

  const [confirmTarget, setConfirmTarget] = useState(null);
  const [deleting, setDeleting] = useState(false);

  // A prescription is written against an appointment, and its patient and
  // doctor are that appointment's participants - so the appointment list is
  // the only lookup the form needs.
  useEffect(() => {
    appointmentService
      .getAllAppointments()
      .then((result) => setAppointments(result.data || []))
      .catch(() => notify.error('Could not load appointments'));
  }, [notify]);

  const appointmentOptions = useMemo(
    () =>
      appointments.map((a) => ({
        value: a.id,
        label: `#${a.id} · ${fullName(a.patient)} with Dr. ${fullName(a.doctor)} · ${formatDate(
          a.appointmentDate
        )}`,
      })),
    [appointments]
  );

  const selectedAppointment = useMemo(
    () => appointments.find((a) => String(a.id) === String(formData.appointmentId)),
    [appointments, formData.appointmentId]
  );

  // Changing two filters in quick succession leaves two requests in flight,
  // and whichever answers last wins - which may be the one for the filter the
  // user already moved off. Each load takes a ticket and drops its result if
  // a newer load started meanwhile.
  const requestId = useRef(0);

  const loadPrescriptions = useCallback(async () => {
    const ticket = ++requestId.current;
    setLoading(true);
    try {
      let result;
      if (appointmentFilter) {
        result = await prescriptionService.getPrescriptionsByAppointmentPaginated(
          appointmentFilter,
          page,
          PAGE_SIZE
        );
      } else if (statusFilter) {
        result = await prescriptionService.getPrescriptionsByStatusPaginated(
          statusFilter,
          page,
          PAGE_SIZE
        );
      } else {
        result = await prescriptionService.getAllPrescriptionsPaginated(page, PAGE_SIZE);
      }

      if (ticket !== requestId.current) return;
      const pageData = result.data || {};
      setPrescriptions(pageData.content || []);
      setTotalPages(pageData.totalPages || 0);
      setTotalElements(pageData.totalElements || 0);
    } catch (err) {
      if (ticket !== requestId.current) return;
      notify.error(err.response?.data?.message || 'Could not load prescriptions');
      setPrescriptions([]);
      setTotalPages(0);
      setTotalElements(0);
    } finally {
      if (ticket === requestId.current) setLoading(false);
    }
  }, [page, statusFilter, appointmentFilter, notify]);

  useEffect(() => {
    loadPrescriptions();
  }, [loadPrescriptions]);

  const changeStatusFilter = (value) => {
    setStatusFilter(value);
    setPage(0);
  };

  const changeAppointmentFilter = (value) => {
    setAppointmentFilter(value);
    setPage(0);
  };

  const clearFilters = () => {
    setStatusFilter('');
    setAppointmentFilter('');
    setPage(0);
  };

  const openCreate = () => {
    setEditingId(null);
    setFormData(EMPTY_FORM);
    setFieldErrors({});
    setModalOpen(true);
  };

  const openEdit = (prescription) => {
    setEditingId(prescription.id);
    setFormData({
      appointmentId: prescription.appointment?.id || '',
      patientId: prescription.patient?.id || '',
      doctorId: prescription.doctor?.id || '',
      medicineName: prescription.medicineName || '',
      dosage: prescription.dosage || '',
      frequency: prescription.frequency || '',
      duration: prescription.duration || '',
      instructions: prescription.instructions || '',
      status: prescription.status || 'ACTIVE',
      isActive: prescription.isActive ?? true,
    });
    setFieldErrors({});
    setModalOpen(true);
  };

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData((prev) => ({ ...prev, [name]: type === 'checkbox' ? checked : value }));
    setFieldErrors((prev) => ({ ...prev, [name]: undefined }));
  };

  /**
   * Picking the appointment sets the patient and doctor with it. The API takes
   * all three ids separately and does not check that they belong together, so
   * deriving them here is what keeps the triple consistent.
   */
  const handleAppointmentChange = (e) => {
    const appointmentId = e.target.value;
    const appointment = appointments.find((a) => String(a.id) === String(appointmentId));
    setFormData((prev) => ({
      ...prev,
      appointmentId,
      patientId: appointment?.patient?.id || '',
      doctorId: appointment?.doctor?.id || '',
    }));
    setFieldErrors((prev) => ({
      ...prev,
      appointmentId: undefined,
      patientId: undefined,
      doctorId: undefined,
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setFieldErrors({});

    const payload = {
      ...formData,
      appointmentId: Number(formData.appointmentId),
      patientId: Number(formData.patientId),
      doctorId: Number(formData.doctorId),
      instructions: formData.instructions.trim() || null,
    };

    try {
      const result = editingId
        ? await prescriptionService.updatePrescription(editingId, payload)
        : await prescriptionService.createPrescription(payload);

      notify.success(result.message);
      setModalOpen(false);
      loadPrescriptions();
    } catch (err) {
      const body = err.response?.data;
      if (body?.data && typeof body.data === 'object') {
        setFieldErrors(body.data);
        notify.error(body.message || 'Please correct the highlighted fields');
      } else {
        notify.error(body?.message || 'Could not save the prescription');
      }
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    setDeleting(true);
    try {
      const result = await prescriptionService.deletePrescription(confirmTarget.id);
      notify.success(result.message);
      setConfirmTarget(null);
      if (prescriptions.length === 1 && page > 0) {
        setPage(page - 1);
      } else {
        loadPrescriptions();
      }
    } catch (err) {
      notify.error(err.response?.data?.message || 'Could not delete the prescription');
    } finally {
      setDeleting(false);
    }
  };

  const columns = [
    {
      key: 'medicineName',
      label: 'Medicine',
      render: (value, row) => (
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-lg bg-primary-100 text-primary-700 flex items-center justify-center flex-shrink-0">
            <Pill className="w-4 h-4" />
          </div>
          <div>
            <p className="font-medium text-gray-900">{value}</p>
            <p className="text-xs text-gray-500">
              {row.dosage} · {row.frequency}
            </p>
          </div>
        </div>
      ),
    },
    {
      key: 'patient',
      label: 'Patient',
      render: (patient) => (
        <span className="text-gray-900">{fullName(patient) || '—'}</span>
      ),
    },
    {
      key: 'doctor',
      label: 'Prescribed by',
      render: (doctor) => (
        <span className="text-gray-900">{doctor ? `Dr. ${fullName(doctor)}` : '—'}</span>
      ),
    },
    {
      key: 'duration',
      label: 'Duration',
      render: (value) => <span className="text-gray-600">{value || '—'}</span>,
    },
    {
      key: 'appointment',
      label: 'Appointment',
      render: (appointment) =>
        appointment ? (
          <span className="text-gray-600">
            #{appointment.id} · {formatDate(appointment.appointmentDate)}
          </span>
        ) : (
          <span className="text-gray-400">—</span>
        ),
    },
    {
      key: 'status',
      label: 'Status',
      render: (value) => (
        <Badge variant={STATUS_VARIANT[value] || 'gray'} size="sm">
          {value || 'UNKNOWN'}
        </Badge>
      ),
    },
  ];

  const showingFrom = totalElements === 0 ? 0 : page * PAGE_SIZE + 1;
  const showingTo = Math.min((page + 1) * PAGE_SIZE, totalElements);
  const filtersApplied = Boolean(statusFilter || appointmentFilter);

  return (
    <div className="p-6 space-y-5">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Prescriptions</h1>
          <p className="text-sm text-gray-600 mt-1">
            Medicines written against an appointment.
          </p>
        </div>
        {canManage && (
          <Button onClick={openCreate} icon={Plus}>
            Write Prescription
          </Button>
        )}
      </div>

      <div className="bg-white rounded-lg border border-gray-200 shadow-sm p-4">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          <SelectField
            label="Status"
            placeholder="Any status"
            value={statusFilter}
            disabled={Boolean(appointmentFilter)}
            onChange={(e) => changeStatusFilter(e.target.value)}
            options={STATUSES.map((s) => ({ value: s, label: s }))}
          />
          <SelectField
            label="Appointment"
            placeholder="Any appointment"
            value={appointmentFilter}
            onChange={(e) => changeAppointmentFilter(e.target.value)}
            options={appointmentOptions}
          />
          <div className="flex items-end">
            {filtersApplied && (
              <Button variant="outline" icon={X} onClick={clearFilters}>
                Clear filters
              </Button>
            )}
          </div>
        </div>
        {appointmentFilter && (
          <p className="text-xs text-gray-500 mt-3">
            Showing one appointment. The status filter is ignored, because the API filters
            on one or the other.
          </p>
        )}
      </div>

      {loading ? (
        <div className="bg-white rounded-lg border border-gray-200 shadow-sm p-6 space-y-3">
          {[0, 1, 2, 3].map((i) => (
            <div key={i} className="skeleton h-12 w-full" />
          ))}
        </div>
      ) : prescriptions.length === 0 ? (
        <div className="bg-white rounded-lg border border-gray-200 shadow-sm">
          <EmptyState
            icon={FileText}
            title={
              filtersApplied ? 'No prescriptions match these filters' : 'No prescriptions yet'
            }
            description={
              filtersApplied
                ? 'Try a different status or appointment.'
                : 'Write the first prescription against an appointment.'
            }
            action={filtersApplied ? clearFilters : canManage ? openCreate : undefined}
            actionLabel={filtersApplied ? 'Clear filters' : 'Write Prescription'}
          />
        </div>
      ) : (
        <>
          <Table
            columns={columns}
            data={prescriptions}
            onEdit={canManage ? openEdit : undefined}
            onDelete={canDelete ? (row) => setConfirmTarget(row) : undefined}
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
        title={editingId ? 'Edit Prescription' : 'Write Prescription'}
        onClose={() => !saving && setModalOpen(false)}
        size="2xl"
        closeOnBackdrop={!saving}
      >
        <form onSubmit={handleSubmit} className="space-y-5">
          <SelectField
            label="Appointment"
            name="appointmentId"
            required
            placeholder="Select an appointment"
            value={formData.appointmentId}
            onChange={handleAppointmentChange}
            options={appointmentOptions}
            error={fieldErrors.appointmentId}
            touched
          />

          {selectedAppointment && (
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 rounded-lg bg-gray-50 border border-gray-200 p-4">
              <div>
                <p className="text-xs font-medium text-gray-500 uppercase tracking-wide">
                  Patient
                </p>
                <p className="text-sm text-gray-900 mt-1">
                  {fullName(selectedAppointment.patient)}
                </p>
              </div>
              <div>
                <p className="text-xs font-medium text-gray-500 uppercase tracking-wide">
                  Prescribing doctor
                </p>
                <p className="text-sm text-gray-900 mt-1">
                  Dr. {fullName(selectedAppointment.doctor)}
                </p>
              </div>
            </div>
          )}

          {(fieldErrors.patientId || fieldErrors.doctorId) && (
            <p className="text-sm text-danger-600">
              {fieldErrors.patientId || fieldErrors.doctorId}
            </p>
          )}

          <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
            <InputField
              label="Medicine"
              name="medicineName"
              required
              placeholder="e.g. Atorvastatin"
              value={formData.medicineName}
              onChange={handleChange}
              error={fieldErrors.medicineName}
              touched
            />
            <InputField
              label="Dosage"
              name="dosage"
              required
              placeholder="e.g. 10 mg"
              value={formData.dosage}
              onChange={handleChange}
              error={fieldErrors.dosage}
              touched
            />
            <InputField
              label="Frequency"
              name="frequency"
              required
              placeholder="e.g. Once daily"
              value={formData.frequency}
              onChange={handleChange}
              error={fieldErrors.frequency}
              touched
            />
            <InputField
              label="Duration"
              name="duration"
              required
              placeholder="e.g. 30 days"
              value={formData.duration}
              onChange={handleChange}
              error={fieldErrors.duration}
              touched
            />
          </div>

          <TextAreaField
            label="Instructions"
            name="instructions"
            rows={3}
            placeholder="e.g. Take after the evening meal"
            value={formData.instructions}
            onChange={handleChange}
            error={fieldErrors.instructions}
            touched
          />

          <SelectField
            label="Status"
            name="status"
            required
            placeholder="Select a status"
            value={formData.status}
            onChange={handleChange}
            options={STATUSES.map((s) => ({ value: s, label: s }))}
            error={fieldErrors.status}
            touched
          />

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
              {editingId ? 'Save changes' : 'Write prescription'}
            </Button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        isOpen={!!confirmTarget}
        title="Delete prescription"
        message={
          confirmTarget
            ? `Delete ${confirmTarget.medicineName} for ${
                fullName(confirmTarget.patient) || 'this patient'
              }? The record is deactivated, not permanently removed.`
            : ''
        }
        loading={deleting}
        onConfirm={handleDelete}
        onCancel={() => !deleting && setConfirmTarget(null)}
      />
    </div>
  );
}

export default PrescriptionManagement;
