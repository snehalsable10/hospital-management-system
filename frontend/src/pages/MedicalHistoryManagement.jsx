import { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import { Plus, ClipboardList, Stethoscope, ChevronLeft, ChevronRight, X } from 'lucide-react';
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
import TextAreaField from '../components/forms/TextAreaField';
import { useNotification } from '../hooks/useNotification';
import { useAuth } from '../hooks/useAuth';
import medicalHistoryService from '../services/medicalHistoryService';
import patientService from '../services/patientService';

const PAGE_SIZE = 10;

// Stored as free text; these are the values the API documents.
const STATUSES = ['ACTIVE', 'ONGOING', 'RESOLVED'];

const STATUS_VARIANT = {
  ACTIVE: 'warning',
  ONGOING: 'info',
  RESOLVED: 'success',
};

const EMPTY_FORM = {
  patientId: '',
  conditionName: '',
  diagnosisDate: '',
  status: 'ACTIVE',
  description: '',
  treatment: '',
  doctorNotes: '',
  isActive: true,
};

const fullName = (person) =>
  person ? `${person.firstName || ''} ${person.lastName || ''}`.trim() : '';

const formatDate = (value) => {
  if (!value) return '—';
  return new Date(`${value}T00:00:00`).toLocaleDateString(undefined, {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  });
};

/** Today in the yyyy-mm-dd the date input and the API both want. */
const today = () => new Date().toISOString().slice(0, 10);

function MedicalHistoryManagement() {
  const { role } = useAuth();
  const notify = useNotification();

  // Writing is ADMIN/STAFF/DOCTOR; deleting is ADMIN only, which is stricter
  // than the rest of the page.
  const canManage = role === 'ADMIN' || role === 'STAFF' || role === 'DOCTOR';
  const canDelete = role === 'ADMIN';

  const [histories, setHistories] = useState([]);
  const [patients, setPatients] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);

  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [patientFilter, setPatientFilter] = useState('');

  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [formData, setFormData] = useState(EMPTY_FORM);
  const [fieldErrors, setFieldErrors] = useState({});
  const [saving, setSaving] = useState(false);

  const [confirmTarget, setConfirmTarget] = useState(null);
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    patientService
      .getAllPatients()
      .then((result) => setPatients(result.data || []))
      .catch(() => notify.error('Could not load patients'));
  }, [notify]);

  const patientOptions = useMemo(
    () => patients.map((p) => ({ value: p.id, label: `${fullName(p)} · ${p.email}` })),
    [patients]
  );

  // A search fires on every pause in typing, so several can be in flight at
  // once. Each load takes a ticket and drops its result if a newer load
  // started meanwhile.
  const requestId = useRef(0);

  const loadHistories = useCallback(async () => {
    const ticket = ++requestId.current;
    setLoading(true);
    try {
      let result;
      if (searchTerm.trim()) {
        result = await medicalHistoryService.searchByConditionNamePaginated(
          searchTerm.trim(),
          page,
          PAGE_SIZE
        );
      } else if (patientFilter) {
        result = await medicalHistoryService.getMedicalHistoriesByPatientPaginated(
          patientFilter,
          page,
          PAGE_SIZE
        );
      } else if (statusFilter) {
        result = await medicalHistoryService.getMedicalHistoriesByStatusPaginated(
          statusFilter,
          page,
          PAGE_SIZE
        );
      } else {
        result = await medicalHistoryService.getAllMedicalHistoriesPaginated(
          page,
          PAGE_SIZE
        );
      }

      if (ticket !== requestId.current) return;
      const pageData = result.data || {};
      setHistories(pageData.content || []);
      setTotalPages(pageData.totalPages || 0);
      setTotalElements(pageData.totalElements || 0);
    } catch (err) {
      if (ticket !== requestId.current) return;
      notify.error(err.response?.data?.message || 'Could not load medical histories');
      setHistories([]);
      setTotalPages(0);
      setTotalElements(0);
    } finally {
      if (ticket === requestId.current) setLoading(false);
    }
  }, [page, searchTerm, statusFilter, patientFilter, notify]);

  useEffect(() => {
    loadHistories();
  }, [loadHistories]);

  const changeSearch = (value) => {
    setSearchTerm(value);
    setPage(0);
  };

  const changeStatusFilter = (value) => {
    setStatusFilter(value);
    setPage(0);
  };

  const changePatientFilter = (value) => {
    setPatientFilter(value);
    setPage(0);
  };

  const clearFilters = () => {
    setSearchTerm('');
    setStatusFilter('');
    setPatientFilter('');
    setPage(0);
  };

  const openCreate = () => {
    setEditingId(null);
    setFormData({ ...EMPTY_FORM, diagnosisDate: today() });
    setFieldErrors({});
    setModalOpen(true);
  };

  const openEdit = (history) => {
    setEditingId(history.id);
    setFormData({
      patientId: history.patient?.id || '',
      conditionName: history.conditionName || '',
      diagnosisDate: history.diagnosisDate || '',
      status: history.status || 'ACTIVE',
      description: history.description || '',
      treatment: history.treatment || '',
      doctorNotes: history.doctorNotes || '',
      isActive: history.isActive ?? true,
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
      description: formData.description.trim() || null,
      treatment: formData.treatment.trim() || null,
      doctorNotes: formData.doctorNotes.trim() || null,
    };

    try {
      const result = editingId
        ? await medicalHistoryService.updateMedicalHistory(editingId, payload)
        : await medicalHistoryService.createMedicalHistory(payload);

      notify.success(result.message);
      setModalOpen(false);
      loadHistories();
    } catch (err) {
      const body = err.response?.data;
      if (body?.data && typeof body.data === 'object') {
        setFieldErrors(body.data);
        notify.error(body.message || 'Please correct the highlighted fields');
      } else {
        notify.error(body?.message || 'Could not save the medical history');
      }
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    setDeleting(true);
    try {
      const result = await medicalHistoryService.deleteMedicalHistory(confirmTarget.id);
      notify.success(result.message);
      setConfirmTarget(null);
      if (histories.length === 1 && page > 0) {
        setPage(page - 1);
      } else {
        loadHistories();
      }
    } catch (err) {
      notify.error(err.response?.data?.message || 'Could not delete the medical history');
    } finally {
      setDeleting(false);
    }
  };

  const columns = [
    {
      key: 'conditionName',
      label: 'Condition',
      render: (value, row) => (
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-lg bg-primary-100 text-primary-700 flex items-center justify-center flex-shrink-0">
            <Stethoscope className="w-4 h-4" />
          </div>
          <div>
            <p className="font-medium text-gray-900">{value}</p>
            {row.description && (
              <p className="text-xs text-gray-500 line-clamp-1" title={row.description}>
                {row.description}
              </p>
            )}
          </div>
        </div>
      ),
    },
    {
      key: 'patient',
      label: 'Patient',
      render: (patient) => (
        <div>
          <p className="text-gray-900">{fullName(patient) || '—'}</p>
          <p className="text-xs text-gray-500">{patient?.email}</p>
        </div>
      ),
    },
    {
      key: 'diagnosisDate',
      label: 'Diagnosed',
      render: (value) => <span className="text-gray-600">{formatDate(value)}</span>,
    },
    {
      key: 'treatment',
      label: 'Treatment',
      render: (value) =>
        value ? (
          <span className="text-gray-600 line-clamp-2" title={value}>
            {value}
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
  const filtersApplied = Boolean(searchTerm || statusFilter || patientFilter);

  return (
    <div className="p-6 space-y-5">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Medical History</h1>
          <p className="text-sm text-gray-600 mt-1">
            Conditions diagnosed for a patient, and how they were treated.
          </p>
        </div>
        {canManage && (
          <Button onClick={openCreate} icon={Plus}>
            Add Condition
          </Button>
        )}
      </div>

      <div className="bg-white rounded-lg border border-gray-200 shadow-sm p-4">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <div className="lg:col-span-2">
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Search condition
            </label>
            {/* Remounted when a filter changes so the box never keeps text for
                a search that is no longer running. */}
            <SearchBar
              key={`search-${statusFilter}-${patientFilter}`}
              placeholder="e.g. Hypertension"
              onSearch={changeSearch}
            />
          </div>
          <SelectField
            label="Status"
            placeholder="Any status"
            value={statusFilter}
            disabled={Boolean(searchTerm || patientFilter)}
            onChange={(e) => changeStatusFilter(e.target.value)}
            options={STATUSES.map((s) => ({ value: s, label: s }))}
          />
          <SelectField
            label="Patient"
            placeholder="Any patient"
            value={patientFilter}
            disabled={Boolean(searchTerm)}
            onChange={(e) => changePatientFilter(e.target.value)}
            options={patientOptions}
          />
        </div>
        <div className="flex items-center justify-between gap-3 mt-3">
          <p className="text-xs text-gray-500">
            {searchTerm
              ? 'Searching by condition. The status and patient filters are ignored, because the API filters on one at a time.'
              : patientFilter
              ? 'Showing one patient. The status filter is ignored, because the API filters on one at a time.'
              : 'Filters apply one at a time.'}
          </p>
          {filtersApplied && (
            <Button variant="outline" size="sm" icon={X} onClick={clearFilters}>
              Clear filters
            </Button>
          )}
        </div>
      </div>

      {loading ? (
        <div className="bg-white rounded-lg border border-gray-200 shadow-sm p-6 space-y-3">
          {[0, 1, 2, 3].map((i) => (
            <div key={i} className="skeleton h-12 w-full" />
          ))}
        </div>
      ) : histories.length === 0 ? (
        <div className="bg-white rounded-lg border border-gray-200 shadow-sm">
          <EmptyState
            icon={ClipboardList}
            title={filtersApplied ? 'No conditions match these filters' : 'No medical history yet'}
            description={
              filtersApplied
                ? 'Try a different condition, status or patient.'
                : 'Record the first diagnosed condition for a patient.'
            }
            action={filtersApplied ? clearFilters : canManage ? openCreate : undefined}
            actionLabel={filtersApplied ? 'Clear filters' : 'Add Condition'}
          />
        </div>
      ) : (
        <>
          <Table
            columns={columns}
            data={histories}
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
        title={editingId ? 'Edit Condition' : 'Add Condition'}
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
            <InputField
              label="Condition"
              name="conditionName"
              required
              placeholder="e.g. Type 2 Diabetes"
              value={formData.conditionName}
              onChange={handleChange}
              error={fieldErrors.conditionName}
              touched
            />
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
            <DateField
              label="Diagnosis date"
              name="diagnosisDate"
              required
              max={today()}
              value={formData.diagnosisDate}
              onChange={handleChange}
              error={fieldErrors.diagnosisDate}
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

          <TextAreaField
            label="Description"
            name="description"
            rows={3}
            placeholder="What was found, and how it presented"
            value={formData.description}
            onChange={handleChange}
            error={fieldErrors.description}
            touched
          />

          <TextAreaField
            label="Treatment"
            name="treatment"
            rows={3}
            placeholder="What was prescribed or performed"
            value={formData.treatment}
            onChange={handleChange}
            error={fieldErrors.treatment}
            touched
          />

          <TextAreaField
            label="Doctor notes"
            name="doctorNotes"
            rows={3}
            placeholder="Anything the next clinician should know"
            value={formData.doctorNotes}
            onChange={handleChange}
            error={fieldErrors.doctorNotes}
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
              {editingId ? 'Save changes' : 'Add condition'}
            </Button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        isOpen={!!confirmTarget}
        title="Delete condition"
        message={
          confirmTarget
            ? `Delete ${confirmTarget.conditionName} for ${
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

export default MedicalHistoryManagement;
