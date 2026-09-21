import { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import {
  Plus,
  BedDouble,
  LogIn,
  LogOut,
  Pencil,
  Trash2,
  ChevronLeft,
  ChevronRight,
  X,
} from 'lucide-react';
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
import roomService from '../services/roomService';

const PAGE_SIZE = 10;

// AVAILABLE and FULL are set by the service as beds move; MAINTENANCE is only
// ever set by hand.
const STATUSES = ['AVAILABLE', 'FULL', 'MAINTENANCE'];

const STATUS_VARIANT = {
  AVAILABLE: 'success',
  FULL: 'danger',
  MAINTENANCE: 'warning',
};

const EMPTY_FORM = {
  roomNumber: '',
  roomType: '',
  ward: '',
  capacity: '',
  costPerDay: '',
  status: 'AVAILABLE',
  description: '',
  amenities: '',
};

const formatMoney = (value) => {
  const number = Number(value);
  if (!Number.isFinite(number)) return '—';
  return number.toLocaleString(undefined, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
};

function RoomManagement() {
  const { role } = useAuth();
  const notify = useNotification();

  // Reading is open to every role. Writing and moving beds is ADMIN/STAFF,
  // and deleting is ADMIN only.
  const canManage = role === 'ADMIN' || role === 'STAFF';
  const canDelete = role === 'ADMIN';

  const [rooms, setRooms] = useState([]);
  const [allRooms, setAllRooms] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);

  const [wardFilter, setWardFilter] = useState('');
  const [typeFilter, setTypeFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [availableOnly, setAvailableOnly] = useState(false);

  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [formData, setFormData] = useState(EMPTY_FORM);
  const [fieldErrors, setFieldErrors] = useState({});
  const [saving, setSaving] = useState(false);

  const [confirmTarget, setConfirmTarget] = useState(null);
  const [deleting, setDeleting] = useState(false);
  const [movingBedFor, setMovingBedFor] = useState(null);

  // Wards and room types are free-text columns with no lookup endpoint, so the
  // filter options come from the rooms that exist.
  const loadAllRooms = useCallback(() => {
    roomService
      .getAllRooms()
      .then((result) => setAllRooms(result.data || []))
      .catch(() => notify.error('Could not load wards and room types'));
  }, [notify]);

  useEffect(() => {
    loadAllRooms();
  }, [loadAllRooms]);

  const wardOptions = useMemo(
    () => [...new Set(allRooms.map((r) => r.ward).filter(Boolean))].sort(),
    [allRooms]
  );

  const typeOptions = useMemo(
    () => [...new Set(allRooms.map((r) => r.roomType).filter(Boolean))].sort(),
    [allRooms]
  );

  const requestId = useRef(0);

  const loadRooms = useCallback(async () => {
    const ticket = ++requestId.current;
    setLoading(true);
    try {
      let result;
      // "Available" combines with a ward or a type - those endpoints exist.
      if (availableOnly && wardFilter) {
        result = await roomService.getAvailableRoomsByWardPaginated(
          wardFilter,
          page,
          PAGE_SIZE
        );
      } else if (availableOnly && typeFilter) {
        result = await roomService.getAvailableRoomsByTypePaginated(
          typeFilter,
          page,
          PAGE_SIZE
        );
      } else if (availableOnly) {
        result = await roomService.getAvailableRoomsPaginated(page, PAGE_SIZE);
      } else if (wardFilter) {
        result = await roomService.getRoomsByWardPaginated(wardFilter, page, PAGE_SIZE);
      } else if (typeFilter) {
        result = await roomService.getRoomsByTypePaginated(typeFilter, page, PAGE_SIZE);
      } else if (statusFilter) {
        result = await roomService.getRoomsByStatusPaginated(statusFilter, page, PAGE_SIZE);
      } else {
        result = await roomService.getAllRoomsPaginated(page, PAGE_SIZE);
      }

      if (ticket !== requestId.current) return;
      const pageData = result.data || {};
      setRooms(pageData.content || []);
      setTotalPages(pageData.totalPages || 0);
      setTotalElements(pageData.totalElements || 0);
    } catch (err) {
      if (ticket !== requestId.current) return;
      notify.error(err.response?.data?.message || 'Could not load rooms');
      setRooms([]);
      setTotalPages(0);
      setTotalElements(0);
    } finally {
      if (ticket === requestId.current) setLoading(false);
    }
  }, [page, wardFilter, typeFilter, statusFilter, availableOnly, notify]);

  useEffect(() => {
    loadRooms();
  }, [loadRooms]);

  const onFilterChange = (setter) => (value) => {
    setter(value);
    setPage(0);
  };

  const changeWardFilter = (value) => {
    setWardFilter(value);
    if (value) setTypeFilter('');
    setPage(0);
  };

  const changeTypeFilter = (value) => {
    setTypeFilter(value);
    if (value) setWardFilter('');
    setPage(0);
  };

  const clearFilters = () => {
    setWardFilter('');
    setTypeFilter('');
    setStatusFilter('');
    setAvailableOnly(false);
    setPage(0);
  };

  const refresh = () => {
    loadRooms();
    loadAllRooms();
  };

  const openCreate = () => {
    setEditingId(null);
    setFormData(EMPTY_FORM);
    setFieldErrors({});
    setModalOpen(true);
  };

  const openEdit = (room) => {
    setEditingId(room.id);
    setFormData({
      roomNumber: room.roomNumber || '',
      roomType: room.roomType || '',
      ward: room.ward || '',
      capacity: room.capacity ?? '',
      costPerDay: room.costPerDay ?? '',
      status: room.status || 'AVAILABLE',
      description: room.description || '',
      amenities: room.amenities || '',
    });
    setFieldErrors({});
    setModalOpen(true);
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    setFieldErrors((prev) => ({ ...prev, [name]: undefined }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setFieldErrors({});

    const payload = {
      ...formData,
      capacity: Number(formData.capacity),
      costPerDay: Number(formData.costPerDay),
      description: formData.description.trim() || null,
      amenities: formData.amenities.trim() || null,
    };

    try {
      const result = editingId
        ? await roomService.updateRoom(editingId, payload)
        : await roomService.createRoom(payload);

      notify.success(result.message);
      setModalOpen(false);
      refresh();
    } catch (err) {
      const body = err.response?.data;
      if (body?.data && typeof body.data === 'object') {
        setFieldErrors(body.data);
        notify.error(body.message || 'Please correct the highlighted fields');
      } else {
        notify.error(body?.message || 'Could not save the room');
      }
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    setDeleting(true);
    try {
      const result = await roomService.deleteRoom(confirmTarget.id);
      notify.success(result.message);
      setConfirmTarget(null);
      if (rooms.length === 1 && page > 0) {
        setPage(page - 1);
      } else {
        refresh();
      }
    } catch (err) {
      notify.error(err.response?.data?.message || 'Could not delete the room');
    } finally {
      setDeleting(false);
    }
  };

  /**
   * Occupying a full room, or vacating an empty one, is a 409 from the server.
   * The buttons are disabled at those boundaries, so the error is a backstop
   * for a stale page rather than something a user should normally hit.
   */
  const moveBed = async (room, direction) => {
    setMovingBedFor(`${room.id}-${direction}`);
    try {
      const result =
        direction === 'occupy'
          ? await roomService.occupyBed(room.id)
          : await roomService.vacateBed(room.id);
      notify.success(result.message);
      refresh();
    } catch (err) {
      notify.error(err.response?.data?.message || 'Could not move the bed');
    } finally {
      setMovingBedFor(null);
    }
  };

  const showingFrom = totalElements === 0 ? 0 : page * PAGE_SIZE + 1;
  const showingTo = Math.min((page + 1) * PAGE_SIZE, totalElements);
  const filtersApplied = Boolean(wardFilter || typeFilter || statusFilter || availableOnly);

  // Occupancy across every room, not just this page - allRooms is already
  // loaded for the filter options.
  const beds = allRooms.reduce(
    (acc, room) => ({
      capacity: acc.capacity + (Number(room.capacity) || 0),
      occupied: acc.occupied + (Number(room.occupiedBeds) || 0),
    }),
    { capacity: 0, occupied: 0 }
  );
  const occupancyPercent = beds.capacity
    ? Math.round((beds.occupied / beds.capacity) * 100)
    : 0;

  const activeFilterNote = availableOnly
    ? wardFilter
      ? 'Available rooms in one ward. The status filter is ignored.'
      : typeFilter
      ? 'Available rooms of one type. The status filter is ignored.'
      : 'Available rooms only. The status filter is ignored.'
    : wardFilter
    ? 'Showing one ward. The type and status filters are ignored.'
    : typeFilter
    ? 'Showing one room type. The status filter is ignored.'
    : 'Ward and room type apply one at a time. "Available only" combines with either.';

  return (
    <div className="p-6 space-y-5">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Rooms</h1>
          <p className="text-sm text-gray-600 mt-1">
            Wards, beds and what each room costs per day.
          </p>
        </div>
        {canManage && (
          <Button onClick={openCreate} icon={Plus}>
            Add Room
          </Button>
        )}
      </div>

      {beds.capacity > 0 && (
        <div className="bg-white rounded-lg border border-gray-200 shadow-sm p-4">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <p className="text-sm font-medium text-gray-700">Bed occupancy</p>
            <p className="text-sm text-gray-600 tabular-nums">
              {beds.occupied} of {beds.capacity} beds ·{' '}
              <span className="font-semibold text-gray-900">{occupancyPercent}%</span>
            </p>
          </div>
          <div className="mt-2 h-2 w-full rounded-full bg-gray-100 overflow-hidden">
            <div
              className={`h-full rounded-full ${
                occupancyPercent >= 90
                  ? 'bg-danger-500'
                  : occupancyPercent >= 70
                  ? 'bg-warning-500'
                  : 'bg-success-500'
              }`}
              style={{ width: `${occupancyPercent}%` }}
            />
          </div>
        </div>
      )}

      <div className="bg-white rounded-lg border border-gray-200 shadow-sm p-4">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          <SelectField
            label="Ward"
            placeholder="Any ward"
            value={wardFilter}
            onChange={(e) => changeWardFilter(e.target.value)}
            options={wardOptions.map((w) => ({ value: w, label: w }))}
          />
          <SelectField
            label="Room type"
            placeholder="Any type"
            value={typeFilter}
            disabled={Boolean(wardFilter)}
            onChange={(e) => changeTypeFilter(e.target.value)}
            options={typeOptions.map((t) => ({ value: t, label: t }))}
          />
          <SelectField
            label="Status"
            placeholder="Any status"
            value={statusFilter}
            disabled={availableOnly || Boolean(wardFilter) || Boolean(typeFilter)}
            onChange={(e) => onFilterChange(setStatusFilter)(e.target.value)}
            options={STATUSES.map((s) => ({ value: s, label: s }))}
          />
        </div>
        <div className="flex flex-wrap items-center justify-between gap-3 mt-3">
          <div className="flex items-center gap-4">
            <label className="flex items-center gap-2 text-sm text-gray-700">
              <input
                type="checkbox"
                checked={availableOnly}
                onChange={(e) => onFilterChange(setAvailableOnly)(e.target.checked)}
                className="rounded border-gray-300"
              />
              Available only
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
      ) : rooms.length === 0 ? (
        <div className="bg-white rounded-lg border border-gray-200 shadow-sm">
          <EmptyState
            icon={BedDouble}
            title={filtersApplied ? 'No rooms match these filters' : 'No rooms yet'}
            description={
              filtersApplied
                ? 'Try a different ward, type or status.'
                : 'Add the first room before admitting anyone.'
            }
            action={filtersApplied ? clearFilters : canManage ? openCreate : undefined}
            actionLabel={filtersApplied ? 'Clear filters' : 'Add Room'}
          />
        </div>
      ) : (
        <>
          {/* A hand-rolled table rather than the shared one: bed moves are two
              extra actions per row, and the shared Table only takes edit and
              delete. */}
          <div className="bg-white rounded-lg border border-gray-200 shadow-sm overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="bg-gray-50 border-b border-gray-200">
                  <tr>
                    {['Room', 'Ward', 'Beds', 'Cost / day', 'Status', 'Actions'].map(
                      (heading) => (
                        <th
                          key={heading}
                          className="px-6 py-3 text-left text-xs font-semibold text-gray-700 uppercase tracking-wider"
                        >
                          {heading}
                        </th>
                      )
                    )}
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-200">
                  {rooms.map((room) => {
                    const occupied = Number(room.occupiedBeds) || 0;
                    const capacity = Number(room.capacity) || 0;
                    const full = occupied >= capacity;
                    const empty = occupied <= 0;
                    return (
                      <tr key={room.id} className="hover:bg-gray-50 transition-colors">
                        <td className="px-6 py-4 text-sm">
                          <div className="flex items-center gap-3">
                            <div className="w-9 h-9 rounded-lg bg-primary-100 text-primary-700 flex items-center justify-center flex-shrink-0">
                              <BedDouble className="w-4 h-4" />
                            </div>
                            <div>
                              <p className="font-medium text-gray-900">
                                {room.roomNumber}
                              </p>
                              <p className="text-xs text-gray-500">{room.roomType}</p>
                            </div>
                          </div>
                        </td>
                        <td className="px-6 py-4 text-sm text-gray-600">{room.ward}</td>
                        <td className="px-6 py-4 text-sm">
                          <p className="text-gray-900 tabular-nums">
                            {occupied} / {capacity}
                          </p>
                          <div className="mt-1 h-1.5 w-24 rounded-full bg-gray-100 overflow-hidden">
                            <div
                              className={`h-full rounded-full ${
                                full ? 'bg-danger-500' : 'bg-success-500'
                              }`}
                              style={{
                                width: capacity
                                  ? `${Math.min(100, (occupied / capacity) * 100)}%`
                                  : '0%',
                              }}
                            />
                          </div>
                        </td>
                        <td className="px-6 py-4 text-sm text-gray-900 tabular-nums">
                          ₹{formatMoney(room.costPerDay)}
                        </td>
                        <td className="px-6 py-4 text-sm">
                          <Badge variant={STATUS_VARIANT[room.status] || 'gray'} size="sm">
                            {room.status || 'UNKNOWN'}
                          </Badge>
                        </td>
                        <td className="px-6 py-4 text-sm">
                          <div className="flex items-center gap-1">
                            {canManage && (
                              <>
                                <button
                                  onClick={() => moveBed(room, 'occupy')}
                                  disabled={full || movingBedFor !== null}
                                  title={
                                    full
                                      ? 'Every bed is taken'
                                      : 'Admit a patient to a bed'
                                  }
                                  className="p-2 text-gray-600 hover:bg-gray-100 rounded transition-colors disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:bg-transparent"
                                >
                                  <LogIn className="w-4 h-4" />
                                </button>
                                <button
                                  onClick={() => moveBed(room, 'vacate')}
                                  disabled={empty || movingBedFor !== null}
                                  title={
                                    empty ? 'No beds are occupied' : 'Free up a bed'
                                  }
                                  className="p-2 text-gray-600 hover:bg-gray-100 rounded transition-colors disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:bg-transparent"
                                >
                                  <LogOut className="w-4 h-4" />
                                </button>
                                <button
                                  onClick={() => openEdit(room)}
                                  title="Edit"
                                  className="p-2 text-gray-600 hover:bg-gray-100 rounded transition-colors"
                                >
                                  <Pencil className="w-4 h-4" />
                                </button>
                              </>
                            )}
                            {canDelete && (
                              <button
                                onClick={() => setConfirmTarget(room)}
                                title="Delete"
                                className="p-2 text-danger-600 hover:bg-danger-50 rounded transition-colors"
                              >
                                <Trash2 className="w-4 h-4" />
                              </button>
                            )}
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>

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
        title={editingId ? 'Edit Room' : 'Add Room'}
        onClose={() => !saving && setModalOpen(false)}
        size="2xl"
        closeOnBackdrop={!saving}
      >
        <form onSubmit={handleSubmit} className="space-y-5">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
            <InputField
              label="Room number"
              name="roomNumber"
              required
              placeholder="e.g. 204-B"
              value={formData.roomNumber}
              onChange={handleChange}
              error={fieldErrors.roomNumber}
              touched
            />
            <InputField
              label="Room type"
              name="roomType"
              required
              placeholder="e.g. General"
              value={formData.roomType}
              onChange={handleChange}
              error={fieldErrors.roomType}
              touched
            />
            <InputField
              label="Ward"
              name="ward"
              required
              placeholder="e.g. Cardiology"
              value={formData.ward}
              onChange={handleChange}
              error={fieldErrors.ward}
              touched
            />
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
            <InputField
              label="Capacity (beds)"
              name="capacity"
              type="number"
              min="1"
              step="1"
              required
              placeholder="e.g. 4"
              value={formData.capacity}
              onChange={handleChange}
              error={fieldErrors.capacity}
              touched
            />
            <InputField
              label="Cost per day"
              name="costPerDay"
              type="number"
              min="0"
              step="0.01"
              required
              placeholder="e.g. 2500.00"
              value={formData.costPerDay}
              onChange={handleChange}
              error={fieldErrors.costPerDay}
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

          <p className="text-xs text-gray-500">
            Occupied beds are not edited here. Admitting and discharging from the list
            moves them, and the server sets AVAILABLE or FULL to match.
          </p>

          <InputField
            label="Amenities"
            name="amenities"
            placeholder="e.g. Oxygen, TV, attached bathroom"
            value={formData.amenities}
            onChange={handleChange}
            error={fieldErrors.amenities}
            touched
          />

          <TextAreaField
            label="Description"
            name="description"
            rows={3}
            placeholder="Anything worth knowing about this room"
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
            <Button type="submit" loading={saving}>
              {editingId ? 'Save changes' : 'Add room'}
            </Button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        isOpen={!!confirmTarget}
        title="Delete room"
        message={
          confirmTarget
            ? `Delete room ${confirmTarget.roomNumber} in ${confirmTarget.ward}${
                Number(confirmTarget.occupiedBeds) > 0
                  ? `? It still has ${confirmTarget.occupiedBeds} occupied bed(s).`
                  : '?'
              } The record is deactivated, not permanently removed.`
            : ''
        }
        loading={deleting}
        onConfirm={handleDelete}
        onCancel={() => !deleting && setConfirmTarget(null)}
      />
    </div>
  );
}

export default RoomManagement;
