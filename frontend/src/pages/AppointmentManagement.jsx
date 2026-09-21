import { useState, useEffect, useCallback, useMemo } from 'react';
import { Plus, Calendar, Clock, ChevronLeft, ChevronRight, X } from 'lucide-react';
import Table from '../components/common/Table';
import Modal from '../components/common/Modal';
import ConfirmDialog from '../components/common/ConfirmDialog';
import Button from '../components/common/Button';
import Badge from '../components/common/Badge';
import EmptyState from '../components/common/EmptyState';
import InputField from '../components/forms/InputField';
import SelectField from '../components/forms/SelectField';
import DateField from '../components/forms/DateField';
import TextAreaField from '../components/forms/TextAreaField';
import { useNotification } from '../hooks/useNotification';
import { useAuth } from '../hooks/useAuth';
import appointmentService from '../services/appointmentService';
import patientService from '../services/patientService';
import doctorService from '../services/doctorService';

const PAGE_SIZE = 10;

// The backend stores status as a free-text column; these are the three values
// the API documents and the only ones this UI writes.
const STATUSES = ['SCHEDULED', 'COMPLETED', 'CANCELLED'];

const STATUS_VARIANT = {
  SCHEDULED: 'info',
  COMPLETED: 'success',
  CANCELLED: 'danger',
};

const EMPTY_FORM = {
  patientId: '',
  doctorId: '',
  appointmentDate: '',
  appointmentTime: '',
  status: 'SCHEDULED',
  reason: '',
  notes: '',
  isActive: true,
};

const fullName = (person) =>
  person ? `${person.firstName || ''} ${person.lastName || ''}`.trim() : '';

/** "14:30:00" -> "2:30 PM". Times come back from the API as ISO LocalTime. */
const formatTime = (value) => {
  if (!value) return '—';
  const [h, m] = value.split(':');
  const hour = Number(h);
  const suffix = hour >= 12 ? 'PM' : 'AM';
  const display = hour % 12 === 0 ? 12 : hour % 12;
  return `${display}:${m} ${suffix}`;
};

const formatDate = (value) => {
  if (!value) return '—';
  const date = new Date(`${value}T00:00:00`);
  return date.toLocaleDateString(undefined, {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  });
};

function AppointmentManagement() {
  const { role } = useAuth();
  const notify = useNotification();

  // The list endpoint itself is ADMIN/STAFF only, so anyone who can reach this
  // page can also book and edit. Deleting is ADMIN/STAFF too.
  const canManage = role === 'ADMIN' || role === 'STAFF';

  const [appointments, setAppointments] = useState([]);
  const [patients, setPatients] = useState([]);
  const [doctors, setDoctors] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);

  const [statusFilter, setStatusFilter] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');

  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [formData, setFormData] = useState(EMPTY_FORM);
  const [fieldErrors, setFieldErrors] = useState({});
  const [saving, setSaving] = useState(false);

  const [confirmTarget, setConfirmTarget] = useState(null);
  const [deleting, setDeleting] = useState(false);

  // Both dropdowns are needed before the booking form can be used at all, so
  // they load once rather than per-open.
  useEffect(() => {
    patientService
      .getAllPatients()
      .then((result) => setPatients(result.data || []))
      .catch(() => notify.error('Could not load patients'));

    doctorService
      .getAllDoctors()
      .then((result) => setDoctors(result.data || []))
      .catch(() => notify.error('Could not load doctors'));
  }, [notify]);

  const patientOptions = useMemo(
    () => patients.map((p) => ({ value: p.id, label: `${fullName(p)} · ${p.email}` })),
    [patients]
  );

  const doctorOptions = useMemo(
    () =>
      doctors.map((d) => ({
        value: d.id,
        label: `Dr. ${fullName(d)}${d.specialization ? ` · ${d.specialization}` : ''}`,
      })),
    [doctors]
  );

  // A date range needs both ends before the API will accept it.
  const rangeActive = Boolean(startDate && endDate);

  const loadAppointments = useCallback(async () => {
    setLoading(true);
    try {
      let result;
      if (rangeActive) {
        result = await appointmentService.getAppointmentsByDateRangePaginated(
          startDate,
          endDate,
          page,
          PAGE_SIZE
        );
      } else if (statusFilter) {
        result = await appointmentService.getAppointmentsByStatusPaginated(
          statusFilter,
          page,
          PAGE_SIZE
        );
      } else {
        result = await appointmentService.getAllAppointmentsPaginated(page, PAGE_SIZE);
      }

      const pageData = result.data || {};
      setAppointments(pageData.content || []);
      setTotalPages(pageData.totalPages || 0);
      setTotalElements(pageData.totalElements || 0);
    } catch (err) {
      notify.error(err.response?.data?.message || 'Could not load appointments');
      setAppointments([]);
      setTotalPages(0);
      setTotalElements(0);
    } finally {
      setLoading(false);
    }
  }, [page, statusFilter, startDate, endDate, rangeActive, notify]);

  useEffect(() => {
    loadAppointments();
  }, [loadAppointments]);

  const changeStatusFilter = (value) => {
    setStatusFilter(value);
    setPage(0);
  };

  const changeStartDate = (value) => {
    setStartDate(value);
    setPage(0);
  };

  const changeEndDate = (value) => {
    setEndDate(value);
    setPage(0);
  };

  const clearFilters = () => {
    setStatusFilter('');
    setStartDate('');
    setEndDate('');
    setPage(0);
  };

  const openCreate = () => {
    setEditingId(null);
    setFormData(EMPTY_FORM);
    setFieldErrors({});
    setModalOpen(true);
  };

  const openEdit = (appointment) => {
    setEditingId(appointment.id);
    setFormData({
      patientId: appointment.patient?.id || '',
      doctorId: appointment.doctor?.id || '',
      appointmentDate: appointment.appointmentDate || '',
      // <input type="time"> wants HH:mm; the API sends HH:mm:ss.
      appointmentTime: (appointment.appointmentTime || '').slice(0, 5),
      status: appointment.status || 'SCHEDULED',
      reason: appointment.reason || '',
      notes: appointment.notes || '',
      isActive: appointment.isActive ?? true,
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
      patientId: Number(formData.patientId),
      doctorId: Number(formData.doctorId),
      // LocalTime parses HH:mm, but send seconds so the value round-trips
      // identically whatever the browser's time input produces.
      appointmentTime:
        formData.appointmentTime.length === 5
          ? `${formData.appointmentTime}:00`
          : formData.appointmentTime,
      notes: formData.notes.trim() || null,
    };

    try {
      const result = editingId
        ? await appointmentService.updateAppointment(editingId, payload)
        : await appointmentService.createAppointment(payload);

      notify.success(result.message);
      setModalOpen(false);
      loadAppointments();
    } catch (err) {
      const body = err.response?.data;
      if (body?.data && typeof body.data === 'object') {
        setFieldErrors(body.data);
        notify.error(body.message || 'Please correct the highlighted fields');
      } else {
        notify.error(body?.message || 'Could not save the appointment');
      }
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    setDeleting(true);
    try {
      const result = await appointmentService.deleteAppointment(confirmTarget.id);
      notify.success(result.message);
      setConfirmTarget(null);
      if (appointments.length === 1 && page > 0) {
        setPage(page - 1);
      } else {
        loadAppointments();
      }
    } catch (err) {
      notify.error(err.response?.data?.message || 'Could not cancel the appointment');
    } finally {
      setDeleting(false);
    }
  };

  const columns = [
    {
      key: 'patient',
      label: 'Patient',
      render: (patient) => (
        <div>
          <p className="font-medium text-gray-900">{fullName(patient) || '—'}</p>
          <p className="text-xs text-gray-500">{patient?.email}</p>
        </div>
      ),
    },
    {
      key: 'doctor',
      label: 'Doctor',
      render: (doctor) => (
        <div>
          <p className="font-medium text-gray-900">
            {doctor ? `Dr. ${fullName(doctor)}` : '—'}
          </p>
          <p className="text-xs text-gray-500">{doctor?.specialization}</p>
        </div>
      ),
    },
    {
      key: 'appointmentDate',
      label: 'When',
      render: (value, row) => (
        <div>
          <p className="flex items-center gap-1.5 text-gray-900">
            <Calendar className="w-3.5 h-3.5 text-gray-400" />
            {formatDate(value)}
          </p>
          <p className="flex items-center gap-1.5 text-xs text-gray-500 mt-0.5">
            <Clock className="w-3.5 h-3.5 text-gray-400" />
            {formatTime(row.appointmentTime)}
          </p>
        </div>
      ),
    },
    {
      key: 'reason',
      label: 'Reason',
      render: (value) => (
        <span className="text-gray-600 line-clamp-2" title={value}>
          {value || '—'}
        </span>
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
  const filtersApplied = Boolean(statusFilter || startDate || endDate);

  return (
    <div className="p-6 space-y-5">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Appointments</h1>
          <p className="text-sm text-gray-600 mt-1">
            Bookings between patients and doctors.
          </p>
        </div>
        {canManage && (
          <Button onClick={openCreate} icon={Plus}>
            Book Appointment
          </Button>
        )}
      </div>

      <div className="bg-white rounded-lg border border-gray-200 shadow-sm p-4">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <SelectField
            label="Status"
            placeholder="Any status"
            value={statusFilter}
            disabled={rangeActive}
            onChange={(e) => changeStatusFilter(e.target.value)}
            options={STATUSES.map((s) => ({ value: s, label: s }))}
          />
          <DateField
            label="From"
            value={startDate}
            onChange={(e) => changeStartDate(e.target.value)}
          />
          <DateField
            label="To"
            value={endDate}
            onChange={(e) => changeEndDate(e.target.value)}
          />
          <div className="flex items-end">
            {filtersApplied && (
              <Button variant="outline" icon={X} onClick={clearFilters}>
                Clear filters
              </Button>
            )}
          </div>
        </div>
        {rangeActive && (
          <p className="text-xs text-gray-500 mt-3">
            Showing a date range. The status filter is ignored while both dates are set,
            because the API filters on one or the other.
          </p>
        )}
        {Boolean(startDate) !== Boolean(endDate) && (
          <p className="text-xs text-gray-500 mt-3">
            Set both dates to filter by range.
          </p>
        )}
      </div>

      {loading ? (
        <div className="bg-white rounded-lg border border-gray-200 shadow-sm p-6 space-y-3">
          {[0, 1, 2, 3].map((i) => (
            <div key={i} className="skeleton h-12 w-full" />
          ))}
        </div>
      ) : appointments.length === 0 ? (
        <div className="bg-white rounded-lg border border-gray-200 shadow-sm">
          <EmptyState
            icon={Calendar}
            title={filtersApplied ? 'No appointments match these filters' : 'No appointments yet'}
            description={
              filtersApplied
                ? 'Try a different status or date range.'
                : 'Book the first appointment between a patient and a doctor.'
            }
            action={filtersApplied ? clearFilters : canManage ? openCreate : undefined}
            actionLabel={filtersApplied ? 'Clear filters' : 'Book Appointment'}
          />
        </div>
      ) : (
        <>
          <Table
            columns={columns}
            data={appointments}
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
        title={editingId ? 'Edit Appointment' : 'Book Appointment'}
        onClose={() => !saving && setModalOpen(false)}
        size="2xl"
        closeOnBackdrop={!saving}
      >
        <form onSubmit={handleSubmit} className="space-y-5">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
            <SelectField
              label="Patient"
              name="patientId"
              required
              placeholder="Select a patient"
              value={formData.patientId}
              onChange={handleChange}
              options={patientOptions}
              error={fieldErrors.patientId}
              touched
            />
            <SelectField
              label="Doctor"
              name="doctorId"
              required
              placeholder="Select a doctor"
              value={formData.doctorId}
              onChange={handleChange}
              options={doctorOptions}
              error={fieldErrors.doctorId}
              touched
            />
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
            <DateField
              label="Date"
              name="appointmentDate"
              required
              value={formData.appointmentDate}
              onChange={handleChange}
              error={fieldErrors.appointmentDate}
              touched
            />
            <InputField
              label="Time"
              name="appointmentTime"
              type="time"
              required
              value={formData.appointmentTime}
              onChange={handleChange}
              error={fieldErrors.appointmentTime}
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
          </div>

          <InputField
            label="Reason"
            name="reason"
            required
            placeholder="e.g. Follow-up on blood pressure"
            value={formData.reason}
            onChange={handleChange}
            error={fieldErrors.reason}
            touched
          />

          <TextAreaField
            label="Notes"
            name="notes"
            rows={3}
            placeholder="Anything the clinician should know beforehand"
            value={formData.notes}
            onChange={handleChange}
            error={fieldErrors.notes}
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
              {editingId ? 'Save changes' : 'Book appointment'}
            </Button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        isOpen={!!confirmTarget}
        title="Delete appointment"
        message={
          confirmTarget
            ? `Delete the appointment for ${fullName(confirmTarget.patient) || 'this patient'} on ${formatDate(
                confirmTarget.appointmentDate
              )}? The record is deactivated, not permanently removed.`
            : ''
        }
        loading={deleting}
        onConfirm={handleDelete}
        onCancel={() => !deleting && setConfirmTarget(null)}
      />
    </div>
  );
}

export default AppointmentManagement;
