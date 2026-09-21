import { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import { Plus, Receipt, ChevronLeft, ChevronRight, X } from 'lucide-react';
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
import billService from '../services/billService';
import patientService from '../services/patientService';
import doctorService from '../services/doctorService';

const PAGE_SIZE = 10;

// Stored as free text. UNPAID and PAID are the two the backend queries by
// name, in getUnpaidBillsByPatient and getPaidBillsByDoctor.
const STATUSES = ['UNPAID', 'PAID', 'PARTIAL', 'CANCELLED'];

const STATUS_VARIANT = {
  PAID: 'success',
  UNPAID: 'danger',
  PARTIAL: 'warning',
  CANCELLED: 'gray',
};

const PAYMENT_METHODS = ['CASH', 'CARD', 'UPI', 'NET_BANKING', 'INSURANCE'];

// The four fee columns that make up the total, in the order they are billed.
const FEE_FIELDS = [
  { name: 'consultationFee', label: 'Consultation', required: true },
  { name: 'testsFee', label: 'Tests', required: false },
  { name: 'medicationsFee', label: 'Medications', required: false },
  { name: 'otherCharges', label: 'Other charges', required: false },
];

const EMPTY_FORM = {
  patientId: '',
  doctorId: '',
  billDate: '',
  consultationFee: '',
  testsFee: '',
  medicationsFee: '',
  otherCharges: '',
  status: 'UNPAID',
  paymentMethod: 'CASH',
  description: '',
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

const formatMoney = (value) => {
  const number = Number(value);
  if (!Number.isFinite(number)) return '—';
  return number.toLocaleString(undefined, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
};

/** Today in the yyyy-mm-dd the date input and the API both want. */
const today = () => new Date().toISOString().slice(0, 10);

/**
 * The total is the sum of the fee columns - never typed. The API takes it as
 * its own field and the service rejects a total that does not match its parts,
 * so deriving it here is what keeps the form from ever being rejected.
 * Blank optional fees count as zero.
 */
const sumFees = (form) =>
  FEE_FIELDS.reduce((total, field) => {
    const value = parseFloat(form[field.name]);
    return total + (Number.isFinite(value) ? value : 0);
  }, 0);

function BillManagement() {
  const { role } = useAuth();
  const notify = useNotification();

  // Writing is ADMIN/STAFF; deleting is ADMIN only.
  const canManage = role === 'ADMIN' || role === 'STAFF';
  const canDelete = role === 'ADMIN';

  const [bills, setBills] = useState([]);
  const [patients, setPatients] = useState([]);
  const [doctors, setDoctors] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);

  const [statusFilter, setStatusFilter] = useState('');
  const [patientFilter, setPatientFilter] = useState('');
  const [unpaidOnly, setUnpaidOnly] = useState(false);
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

  const rangeActive = Boolean(startDate && endDate);

  const requestId = useRef(0);

  const loadBills = useCallback(async () => {
    const ticket = ++requestId.current;
    setLoading(true);
    try {
      let result;
      if (patientFilter && unpaidOnly) {
        result = await billService.getUnpaidBillsByPatientPaginated(
          patientFilter,
          page,
          PAGE_SIZE
        );
      } else if (patientFilter) {
        result = await billService.getBillsByPatientPaginated(patientFilter, page, PAGE_SIZE);
      } else if (rangeActive) {
        result = await billService.getBillsByDateRangePaginated(
          startDate,
          endDate,
          page,
          PAGE_SIZE
        );
      } else if (statusFilter) {
        result = await billService.getBillsByStatusPaginated(statusFilter, page, PAGE_SIZE);
      } else {
        result = await billService.getAllBillsPaginated(page, PAGE_SIZE);
      }

      if (ticket !== requestId.current) return;
      const pageData = result.data || {};
      setBills(pageData.content || []);
      setTotalPages(pageData.totalPages || 0);
      setTotalElements(pageData.totalElements || 0);
    } catch (err) {
      if (ticket !== requestId.current) return;
      notify.error(err.response?.data?.message || 'Could not load bills');
      setBills([]);
      setTotalPages(0);
      setTotalElements(0);
    } finally {
      if (ticket === requestId.current) setLoading(false);
    }
  }, [page, statusFilter, patientFilter, unpaidOnly, startDate, endDate, rangeActive, notify]);

  useEffect(() => {
    loadBills();
  }, [loadBills]);

  const onFilterChange = (setter) => (value) => {
    setter(value);
    setPage(0);
  };

  const changePatientFilter = (value) => {
    setPatientFilter(value);
    // "Unpaid only" has no endpoint without a patient, so it cannot outlive one.
    if (!value) setUnpaidOnly(false);
    setPage(0);
  };

  const clearFilters = () => {
    setStatusFilter('');
    setPatientFilter('');
    setUnpaidOnly(false);
    setStartDate('');
    setEndDate('');
    setPage(0);
  };

  const openCreate = () => {
    setEditingId(null);
    setFormData({ ...EMPTY_FORM, billDate: today() });
    setFieldErrors({});
    setModalOpen(true);
  };

  const openEdit = (bill) => {
    setEditingId(bill.id);
    setFormData({
      patientId: bill.patient?.id || '',
      doctorId: bill.doctor?.id || '',
      billDate: bill.billDate || '',
      consultationFee: bill.consultationFee ?? '',
      testsFee: bill.testsFee ?? '',
      medicationsFee: bill.medicationsFee ?? '',
      otherCharges: bill.otherCharges ?? '',
      status: bill.status || 'UNPAID',
      paymentMethod: bill.paymentMethod || 'CASH',
      description: bill.description || '',
    });
    setFieldErrors({});
    setModalOpen(true);
  };

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData((prev) => ({ ...prev, [name]: type === 'checkbox' ? checked : value }));
    setFieldErrors((prev) => ({ ...prev, [name]: undefined }));
  };

  const formTotal = sumFees(formData);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setFieldErrors({});

    // Each optional fee is @Positive, so a blank or zero has to go as null
    // rather than 0 - the constraint rejects zero.
    const fee = (value) => {
      const number = parseFloat(value);
      return Number.isFinite(number) && number > 0 ? number : null;
    };

    const payload = {
      patientId: Number(formData.patientId),
      doctorId: Number(formData.doctorId),
      billDate: formData.billDate,
      consultationFee: fee(formData.consultationFee),
      testsFee: fee(formData.testsFee),
      medicationsFee: fee(formData.medicationsFee),
      otherCharges: fee(formData.otherCharges),
      totalAmount: formTotal,
      status: formData.status,
      paymentMethod: formData.paymentMethod,
      description: formData.description.trim() || null,
    };

    try {
      const result = editingId
        ? await billService.updateBill(editingId, payload)
        : await billService.createBill(payload);

      notify.success(result.message);
      setModalOpen(false);
      loadBills();
    } catch (err) {
      const body = err.response?.data;
      if (body?.data && typeof body.data === 'object') {
        setFieldErrors(body.data);
        notify.error(body.message || 'Please correct the highlighted fields');
      } else {
        notify.error(body?.message || 'Could not save the bill');
      }
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    setDeleting(true);
    try {
      const result = await billService.deleteBill(confirmTarget.id);
      notify.success(result.message);
      setConfirmTarget(null);
      if (bills.length === 1 && page > 0) {
        setPage(page - 1);
      } else {
        loadBills();
      }
    } catch (err) {
      notify.error(err.response?.data?.message || 'Could not delete the bill');
    } finally {
      setDeleting(false);
    }
  };

  const columns = [
    {
      key: 'id',
      label: 'Bill',
      render: (value, row) => (
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-lg bg-primary-100 text-primary-700 flex items-center justify-center flex-shrink-0">
            <Receipt className="w-4 h-4" />
          </div>
          <div>
            <p className="font-medium text-gray-900">#{value}</p>
            <p className="text-xs text-gray-500">{formatDate(row.billDate)}</p>
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
      key: 'doctor',
      label: 'Doctor',
      render: (doctor) => (
        <span className="text-gray-900">{doctor ? `Dr. ${fullName(doctor)}` : '—'}</span>
      ),
    },
    {
      key: 'totalAmount',
      label: 'Amount',
      render: (value, row) => (
        <div>
          <p className="font-semibold text-gray-900 tabular-nums">₹{formatMoney(value)}</p>
          <p className="text-xs text-gray-500">
            Consultation ₹{formatMoney(row.consultationFee)}
            {Number(row.testsFee) > 0 && ` · Tests ₹${formatMoney(row.testsFee)}`}
            {Number(row.medicationsFee) > 0 &&
              ` · Meds ₹${formatMoney(row.medicationsFee)}`}
            {Number(row.otherCharges) > 0 && ` · Other ₹${formatMoney(row.otherCharges)}`}
          </p>
        </div>
      ),
    },
    {
      key: 'paymentMethod',
      label: 'Method',
      render: (value) => (
        <span className="text-gray-600">{(value || '—').replace('_', ' ')}</span>
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

  // The page's own rows only - the API has no aggregate endpoint yet, so this
  // deliberately says "on this page" rather than implying a grand total.
  const pageTotal = bills.reduce((sum, bill) => sum + Number(bill.totalAmount || 0), 0);

  const showingFrom = totalElements === 0 ? 0 : page * PAGE_SIZE + 1;
  const showingTo = Math.min((page + 1) * PAGE_SIZE, totalElements);
  const filtersApplied = Boolean(
    statusFilter || patientFilter || unpaidOnly || startDate || endDate
  );

  const activeFilterNote = patientFilter
    ? unpaidOnly
      ? 'Showing unpaid bills for one patient. The status and date filters are ignored.'
      : 'Showing one patient. The status and date filters are ignored.'
    : rangeActive
    ? 'Showing a date range. The status filter is ignored.'
    : Boolean(startDate) !== Boolean(endDate)
    ? 'Set both dates to filter by range.'
    : 'Filters apply one at a time.';

  return (
    <div className="p-6 space-y-5">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Bills</h1>
          <p className="text-sm text-gray-600 mt-1">
            Charges raised against a patient for a doctor's care.
          </p>
        </div>
        {canManage && (
          <Button onClick={openCreate} icon={Plus}>
            Raise Bill
          </Button>
        )}
      </div>

      <div className="bg-white rounded-lg border border-gray-200 shadow-sm p-4">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <SelectField
            label="Patient"
            placeholder="Any patient"
            value={patientFilter}
            onChange={(e) => changePatientFilter(e.target.value)}
            options={patientOptions}
          />
          <SelectField
            label="Status"
            placeholder="Any status"
            value={statusFilter}
            disabled={Boolean(patientFilter) || rangeActive}
            onChange={(e) => onFilterChange(setStatusFilter)(e.target.value)}
            options={STATUSES.map((s) => ({ value: s, label: s }))}
          />
          <DateField
            label="Billed from"
            value={startDate}
            disabled={Boolean(patientFilter)}
            onChange={(e) => onFilterChange(setStartDate)(e.target.value)}
          />
          <DateField
            label="Billed to"
            value={endDate}
            disabled={Boolean(patientFilter)}
            onChange={(e) => onFilterChange(setEndDate)(e.target.value)}
          />
        </div>
        <div className="flex flex-wrap items-center justify-between gap-3 mt-3">
          <div className="flex items-center gap-4">
            <label
              className={`flex items-center gap-2 text-sm ${
                patientFilter ? 'text-gray-700' : 'text-gray-400 cursor-not-allowed'
              }`}
              title={
                patientFilter ? undefined : 'Pick a patient first - the API scopes this to one'
              }
            >
              <input
                type="checkbox"
                checked={unpaidOnly}
                disabled={!patientFilter}
                onChange={(e) => onFilterChange(setUnpaidOnly)(e.target.checked)}
                className="rounded border-gray-300"
              />
              Unpaid only
            </label>
            <p className="text-xs text-gray-500">{activeFilterNote}</p>
          </div>
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
      ) : bills.length === 0 ? (
        <div className="bg-white rounded-lg border border-gray-200 shadow-sm">
          <EmptyState
            icon={Receipt}
            title={filtersApplied ? 'No bills match these filters' : 'No bills yet'}
            description={
              filtersApplied
                ? 'Try a different patient, status or date range.'
                : 'Raise the first bill for a patient.'
            }
            action={filtersApplied ? clearFilters : canManage ? openCreate : undefined}
            actionLabel={filtersApplied ? 'Clear filters' : 'Raise Bill'}
          />
        </div>
      ) : (
        <>
          <Table
            columns={columns}
            data={bills}
            onEdit={canManage ? openEdit : undefined}
            onDelete={canDelete ? (row) => setConfirmTarget(row) : undefined}
          />

          <div className="flex flex-wrap items-center justify-between gap-3 px-1">
            <div>
              <p className="text-sm text-gray-600">
                Showing {showingFrom}–{showingTo} of {totalElements}
              </p>
              <p className="text-sm text-gray-900 mt-0.5">
                Total on this page:{' '}
                <span className="font-semibold tabular-nums">₹{formatMoney(pageTotal)}</span>
              </p>
            </div>
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
        title={editingId ? `Edit Bill #${editingId}` : 'Raise Bill'}
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

          <div className="rounded-lg border border-gray-200 p-4 space-y-4">
            <p className="text-xs font-medium text-gray-500 uppercase tracking-wide">
              Charges
            </p>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {FEE_FIELDS.map((field) => (
                <InputField
                  key={field.name}
                  label={field.label}
                  name={field.name}
                  type="number"
                  step="0.01"
                  min="0"
                  required={field.required}
                  placeholder="0.00"
                  value={formData[field.name]}
                  onChange={handleChange}
                  error={fieldErrors[field.name]}
                  touched
                />
              ))}
            </div>
            <div className="flex items-center justify-between border-t border-gray-200 pt-4">
              <span className="text-sm font-medium text-gray-700">Total</span>
              <span className="text-lg font-semibold text-gray-900 tabular-nums">
                ₹{formatMoney(formTotal)}
              </span>
            </div>
            {fieldErrors.totalAmount && (
              <p className="text-sm text-danger-600">{fieldErrors.totalAmount}</p>
            )}
            <p className="text-xs text-gray-500">
              The total is the sum of the charges above and is not typed in.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
            <DateField
              label="Bill date"
              name="billDate"
              required
              max={today()}
              value={formData.billDate}
              onChange={handleChange}
              error={fieldErrors.billDate}
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
            <SelectField
              label="Payment method"
              name="paymentMethod"
              required
              placeholder="Select a method"
              value={formData.paymentMethod}
              onChange={handleChange}
              options={PAYMENT_METHODS.map((m) => ({
                value: m,
                label: m.replace('_', ' '),
              }))}
              error={fieldErrors.paymentMethod}
              touched
            />
          </div>

          <TextAreaField
            label="Description"
            name="description"
            rows={3}
            placeholder="What this bill covers"
            value={formData.description}
            onChange={handleChange}
            error={fieldErrors.description}
            touched
          />

          <div className="flex justify-end gap-3 border-t border-gray-200 pt-5">
            <Button
              type="button"
              variant="outline"
              onClick={() => setModalOpen(false)}
              disabled={saving}
            >
              Cancel
            </Button>
            <Button type="submit" loading={saving} disabled={formTotal <= 0}>
              {editingId ? 'Save changes' : 'Raise bill'}
            </Button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        isOpen={!!confirmTarget}
        title="Delete bill"
        message={
          confirmTarget
            ? `Delete bill #${confirmTarget.id} for ${
                fullName(confirmTarget.patient) || 'this patient'
              }, ₹${formatMoney(
                confirmTarget.totalAmount
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

export default BillManagement;
