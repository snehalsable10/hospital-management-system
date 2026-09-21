import { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import { Plus, FlaskConical, ChevronLeft, ChevronRight, X } from 'lucide-react';
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
import laboratoryTestService from '../services/laboratoryTestService';
import patientService from '../services/patientService';

const PAGE_SIZE = 10;

// Stored as free text; these are the values the API documents.
const STATUSES = ['NORMAL', 'ABNORMAL', 'PENDING'];

const STATUS_VARIANT = {
  NORMAL: 'success',
  ABNORMAL: 'danger',
  PENDING: 'warning',
};

const EMPTY_FORM = {
  patientId: '',
  testName: '',
  testDate: '',
  resultValue: '',
  resultUnit: '',
  referenceMin: '',
  referenceMax: '',
  status: 'NORMAL',
  notes: '',
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

/**
 * The reference range is two free-text columns, either of which may be blank,
 * so render whichever side exists rather than assuming both.
 */
const referenceRange = (min, max) => {
  if (min && max) return `${min} – ${max}`;
  if (min) return `≥ ${min}`;
  if (max) return `≤ ${max}`;
  return null;
};

function LaboratoryTestManagement() {
  const { role } = useAuth();
  const notify = useNotification();

  // Every write on this controller is ADMIN/STAFF, delete included.
  const canManage = role === 'ADMIN' || role === 'STAFF';

  const [tests, setTests] = useState([]);
  const [patients, setPatients] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);

  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [patientFilter, setPatientFilter] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');

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

  // A date range needs both ends before the API will accept it.
  const rangeActive = Boolean(startDate && endDate);

  // Searching fires on every pause in typing, so several loads can be in
  // flight at once. Each takes a ticket and drops its result if a newer load
  // started meanwhile.
  const requestId = useRef(0);

  const loadTests = useCallback(async () => {
    const ticket = ++requestId.current;
    setLoading(true);
    try {
      let result;
      if (searchTerm.trim()) {
        result = await laboratoryTestService.searchByTestNamePaginated(
          searchTerm.trim(),
          page,
          PAGE_SIZE
        );
      } else if (rangeActive) {
        result = await laboratoryTestService.getLaboratoryTestsByDateRangePaginated(
          startDate,
          endDate,
          page,
          PAGE_SIZE
        );
      } else if (patientFilter) {
        result = await laboratoryTestService.getLaboratoryTestsByPatientPaginated(
          patientFilter,
          page,
          PAGE_SIZE
        );
      } else if (statusFilter) {
        result = await laboratoryTestService.getLaboratoryTestsByStatusPaginated(
          statusFilter,
          page,
          PAGE_SIZE
        );
      } else {
        result = await laboratoryTestService.getAllLaboratoryTestsPaginated(
          page,
          PAGE_SIZE
        );
      }

      if (ticket !== requestId.current) return;
      const pageData = result.data || {};
      setTests(pageData.content || []);
      setTotalPages(pageData.totalPages || 0);
      setTotalElements(pageData.totalElements || 0);
    } catch (err) {
      if (ticket !== requestId.current) return;
      notify.error(err.response?.data?.message || 'Could not load laboratory tests');
      setTests([]);
      setTotalPages(0);
      setTotalElements(0);
    } finally {
      if (ticket === requestId.current) setLoading(false);
    }
  }, [page, searchTerm, statusFilter, patientFilter, startDate, endDate, rangeActive, notify]);

  useEffect(() => {
    loadTests();
  }, [loadTests]);

  const onFilterChange = (setter) => (value) => {
    setter(value);
    setPage(0);
  };

  const clearFilters = () => {
    setSearchTerm('');
    setStatusFilter('');
    setPatientFilter('');
    setStartDate('');
    setEndDate('');
    setPage(0);
  };

  const openCreate = () => {
    setEditingId(null);
    setFormData({ ...EMPTY_FORM, testDate: today() });
    setFieldErrors({});
    setModalOpen(true);
  };

  const openEdit = (test) => {
    setEditingId(test.id);
    setFormData({
      patientId: test.patient?.id || '',
      testName: test.testName || '',
      testDate: test.testDate || '',
      resultValue: test.resultValue || '',
      resultUnit: test.resultUnit || '',
      referenceMin: test.referenceMin || '',
      referenceMax: test.referenceMax || '',
      status: test.status || 'NORMAL',
      notes: test.notes || '',
      isActive: test.isActive ?? true,
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
      referenceMin: formData.referenceMin.trim() || null,
      referenceMax: formData.referenceMax.trim() || null,
      notes: formData.notes.trim() || null,
    };

    try {
      const result = editingId
        ? await laboratoryTestService.updateLaboratoryTest(editingId, payload)
        : await laboratoryTestService.createLaboratoryTest(payload);

      notify.success(result.message);
      setModalOpen(false);
      loadTests();
    } catch (err) {
      const body = err.response?.data;
      if (body?.data && typeof body.data === 'object') {
        setFieldErrors(body.data);
        notify.error(body.message || 'Please correct the highlighted fields');
      } else {
        notify.error(body?.message || 'Could not save the laboratory test');
      }
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    setDeleting(true);
    try {
      const result = await laboratoryTestService.deleteLaboratoryTest(confirmTarget.id);
      notify.success(result.message);
      setConfirmTarget(null);
      if (tests.length === 1 && page > 0) {
        setPage(page - 1);
      } else {
        loadTests();
      }
    } catch (err) {
      notify.error(err.response?.data?.message || 'Could not delete the laboratory test');
    } finally {
      setDeleting(false);
    }
  };

  const columns = [
    {
      key: 'testName',
      label: 'Test',
      render: (value) => (
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-lg bg-primary-100 text-primary-700 flex items-center justify-center flex-shrink-0">
            <FlaskConical className="w-4 h-4" />
          </div>
          <p className="font-medium text-gray-900">{value}</p>
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
      key: 'resultValue',
      label: 'Result',
      render: (value, row) => {
        const range = referenceRange(row.referenceMin, row.referenceMax);
        return (
          <div>
            <p className="font-medium text-gray-900">
              {value} <span className="font-normal text-gray-600">{row.resultUnit}</span>
            </p>
            {range && <p className="text-xs text-gray-500">Reference {range}</p>}
          </div>
        );
      },
    },
    {
      key: 'testDate',
      label: 'Tested',
      render: (value) => <span className="text-gray-600">{formatDate(value)}</span>,
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
  const filtersApplied = Boolean(
    searchTerm || statusFilter || patientFilter || startDate || endDate
  );

  const activeFilterNote = searchTerm
    ? 'Searching by test name. The other filters are ignored, because the API filters on one at a time.'
    : rangeActive
    ? 'Showing a date range. The patient and status filters are ignored.'
    : patientFilter
    ? 'Showing one patient. The status filter is ignored.'
    : Boolean(startDate) !== Boolean(endDate)
    ? 'Set both dates to filter by range.'
    : 'Filters apply one at a time.';

  return (
    <div className="p-6 space-y-5">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Laboratory Tests</h1>
          <p className="text-sm text-gray-600 mt-1">
            Results recorded against a patient, with their reference range.
          </p>
        </div>
        {canManage && (
          <Button onClick={openCreate} icon={Plus}>
            Record Test
          </Button>
        )}
      </div>

      <div className="bg-white rounded-lg border border-gray-200 shadow-sm p-4">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Search test name
            </label>
            {/* Remounted when a filter changes so the box never keeps text for
                a search that is no longer running. */}
            <SearchBar
              key={`search-${statusFilter}-${patientFilter}-${startDate}-${endDate}`}
              placeholder="e.g. Haemoglobin"
              onSearch={onFilterChange(setSearchTerm)}
            />
          </div>
          <SelectField
            label="Patient"
            placeholder="Any patient"
            value={patientFilter}
            disabled={Boolean(searchTerm) || rangeActive}
            onChange={(e) => onFilterChange(setPatientFilter)(e.target.value)}
            options={patientOptions}
          />
          <SelectField
            label="Status"
            placeholder="Any status"
            value={statusFilter}
            disabled={Boolean(searchTerm) || rangeActive || Boolean(patientFilter)}
            onChange={(e) => onFilterChange(setStatusFilter)(e.target.value)}
            options={STATUSES.map((s) => ({ value: s, label: s }))}
          />
          <DateField
            label="Tested from"
            value={startDate}
            disabled={Boolean(searchTerm)}
            onChange={(e) => onFilterChange(setStartDate)(e.target.value)}
          />
          <DateField
            label="Tested to"
            value={endDate}
            disabled={Boolean(searchTerm)}
            onChange={(e) => onFilterChange(setEndDate)(e.target.value)}
          />
          <div className="flex items-end">
            {filtersApplied && (
              <Button variant="outline" icon={X} onClick={clearFilters}>
                Clear filters
              </Button>
            )}
          </div>
        </div>
        <p className="text-xs text-gray-500 mt-3">{activeFilterNote}</p>
      </div>

      {loading ? (
        <div className="bg-white rounded-lg border border-gray-200 shadow-sm p-6 space-y-3">
          {[0, 1, 2, 3].map((i) => (
            <div key={i} className="skeleton h-12 w-full" />
          ))}
        </div>
      ) : tests.length === 0 ? (
        <div className="bg-white rounded-lg border border-gray-200 shadow-sm">
          <EmptyState
            icon={FlaskConical}
            title={filtersApplied ? 'No tests match these filters' : 'No laboratory tests yet'}
            description={
              filtersApplied
                ? 'Try a different test name, patient, status or date range.'
                : 'Record the first test result for a patient.'
            }
            action={filtersApplied ? clearFilters : canManage ? openCreate : undefined}
            actionLabel={filtersApplied ? 'Clear filters' : 'Record Test'}
          />
        </div>
      ) : (
        <>
          <Table
            columns={columns}
            data={tests}
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
        title={editingId ? 'Edit Test Result' : 'Record Test Result'}
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
              label="Test name"
              name="testName"
              required
              placeholder="e.g. Haemoglobin"
              value={formData.testName}
              onChange={handleChange}
              error={fieldErrors.testName}
              touched
            />
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
            <InputField
              label="Result"
              name="resultValue"
              required
              placeholder="e.g. 13.4"
              value={formData.resultValue}
              onChange={handleChange}
              error={fieldErrors.resultValue}
              touched
            />
            <InputField
              label="Unit"
              name="resultUnit"
              required
              placeholder="e.g. g/dL"
              value={formData.resultUnit}
              onChange={handleChange}
              error={fieldErrors.resultUnit}
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

          <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
            <InputField
              label="Reference min"
              name="referenceMin"
              placeholder="e.g. 12.0"
              value={formData.referenceMin}
              onChange={handleChange}
              error={fieldErrors.referenceMin}
              touched
            />
            <InputField
              label="Reference max"
              name="referenceMax"
              placeholder="e.g. 16.0"
              value={formData.referenceMax}
              onChange={handleChange}
              error={fieldErrors.referenceMax}
              touched
            />
            <DateField
              label="Test date"
              name="testDate"
              required
              max={today()}
              value={formData.testDate}
              onChange={handleChange}
              error={fieldErrors.testDate}
              touched
            />
          </div>

          <TextAreaField
            label="Notes"
            name="notes"
            rows={3}
            placeholder="Anything the clinician reading this should know"
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
              {editingId ? 'Save changes' : 'Record test'}
            </Button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        isOpen={!!confirmTarget}
        title="Delete test result"
        message={
          confirmTarget
            ? `Delete the ${confirmTarget.testName} result for ${
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

export default LaboratoryTestManagement;
