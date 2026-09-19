import { useState, useEffect, useCallback } from 'react';
import { Plus, Building2, ChevronLeft, ChevronRight } from 'lucide-react';
import Table from '../components/common/Table';
import Modal from '../components/common/Modal';
import ConfirmDialog from '../components/common/ConfirmDialog';
import Button from '../components/common/Button';
import Badge from '../components/common/Badge';
import EmptyState from '../components/common/EmptyState';
import InputField from '../components/forms/InputField';
import { useNotification } from '../hooks/useNotification';
import { useAuth } from '../hooks/useAuth';
import departmentService from '../services/departmentService';

const PAGE_SIZE = 10;

const EMPTY_FORM = {
  name: '',
  description: '',
  phone: '',
  isActive: true,
};

function DepartmentManagement() {
  const { role } = useAuth();
  const notify = useNotification();

  const canManage = role === 'ADMIN';

  const [departments, setDepartments] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);

  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [formData, setFormData] = useState(EMPTY_FORM);
  const [fieldErrors, setFieldErrors] = useState({});
  const [saving, setSaving] = useState(false);

  const [confirmTarget, setConfirmTarget] = useState(null);
  const [deleting, setDeleting] = useState(false);

  const loadDepartments = useCallback(async () => {
    setLoading(true);
    try {
      // No search endpoint exists for departments, so there is no filter here.
      const result = await departmentService.getAllDepartmentsPaginated(page, PAGE_SIZE);
      const pageData = result.data || {};
      setDepartments(pageData.content || []);
      setTotalPages(pageData.totalPages || 0);
      setTotalElements(pageData.totalElements || 0);
    } catch (err) {
      notify.error(err.response?.data?.message || 'Could not load departments');
      setDepartments([]);
      setTotalPages(0);
      setTotalElements(0);
    } finally {
      setLoading(false);
    }
  }, [page, notify]);

  useEffect(() => {
    loadDepartments();
  }, [loadDepartments]);

  const openCreate = () => {
    setEditingId(null);
    setFormData(EMPTY_FORM);
    setFieldErrors({});
    setModalOpen(true);
  };

  const openEdit = (department) => {
    setEditingId(department.id);
    setFormData({
      name: department.name || '',
      description: department.description || '',
      phone: department.phone || '',
      isActive: department.isActive ?? true,
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
      description: formData.description.trim() || null,
      phone: formData.phone.trim() || null,
    };

    try {
      const result = editingId
        ? await departmentService.updateDepartment(editingId, payload)
        : await departmentService.createDepartment(payload);

      notify.success(result.message);
      setModalOpen(false);
      loadDepartments();
    } catch (err) {
      const body = err.response?.data;
      if (body?.data && typeof body.data === 'object') {
        setFieldErrors(body.data);
        notify.error(body.message || 'Please correct the highlighted fields');
      } else {
        notify.error(body?.message || 'Could not save the department');
      }
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    setDeleting(true);
    try {
      const result = await departmentService.deleteDepartment(confirmTarget.id);
      notify.success(result.message);
      setConfirmTarget(null);
      if (departments.length === 1 && page > 0) {
        setPage(page - 1);
      } else {
        loadDepartments();
      }
    } catch (err) {
      notify.error(err.response?.data?.message || 'Could not delete the department');
    } finally {
      setDeleting(false);
    }
  };

  const columns = [
    {
      key: 'name',
      label: 'Department',
      render: (value) => (
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-lg bg-primary-100 text-primary-700 flex items-center justify-center flex-shrink-0">
            <Building2 className="w-4 h-4" />
          </div>
          <p className="font-medium text-gray-900">{value}</p>
        </div>
      ),
    },
    {
      key: 'description',
      label: 'Description',
      render: (value) =>
        value ? (
          <span className="text-gray-600">{value}</span>
        ) : (
          <span className="text-gray-400">—</span>
        ),
    },
    {
      key: 'phone',
      label: 'Phone',
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
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Departments</h1>
          <p className="text-sm text-gray-600 mt-1">
            Clinical departments doctors are assigned to.
          </p>
        </div>
        {canManage && (
          <Button onClick={openCreate} icon={Plus}>
            Add Department
          </Button>
        )}
      </div>

      {loading ? (
        <div className="bg-white rounded-lg border border-gray-200 shadow-sm p-6 space-y-3">
          {[0, 1, 2, 3].map((i) => (
            <div key={i} className="skeleton h-12 w-full" />
          ))}
        </div>
      ) : departments.length === 0 ? (
        <div className="bg-white rounded-lg border border-gray-200 shadow-sm">
          <EmptyState
            icon={Building2}
            title="No departments yet"
            description="Create a department before adding doctors to it."
            action={canManage ? openCreate : undefined}
            actionLabel="Add Department"
          />
        </div>
      ) : (
        <>
          <Table
            columns={columns}
            data={departments}
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
        title={editingId ? 'Edit Department' : 'Add Department'}
        onClose={() => !saving && setModalOpen(false)}
        size="lg"
        closeOnBackdrop={!saving}
      >
        <form onSubmit={handleSubmit} className="space-y-5">
          <InputField
            label="Department name"
            name="name"
            required
            placeholder="e.g. Cardiology"
            value={formData.name}
            onChange={handleChange}
            error={fieldErrors.name}
            touched
          />

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Description</label>
            <textarea
              name="description"
              rows={3}
              value={formData.description}
              onChange={handleChange}
              placeholder="What this department handles"
              className={`w-full px-4 py-2 border rounded-lg outline-none transition-colors focus:ring-2 focus:ring-offset-2 ${
                fieldErrors.description
                  ? 'border-danger-500 focus:ring-danger-500'
                  : 'border-gray-300 focus:border-primary-500 focus:ring-primary-500'
              }`}
            />
            {fieldErrors.description && (
              <p className="mt-1 text-sm text-danger-600">{fieldErrors.description}</p>
            )}
          </div>

          <InputField
            label="Phone"
            name="phone"
            value={formData.phone}
            onChange={handleChange}
            error={fieldErrors.phone}
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
              {editingId ? 'Save changes' : 'Add department'}
            </Button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        isOpen={!!confirmTarget}
        title="Delete department"
        message={
          confirmTarget
            ? `Delete ${confirmTarget.name}? The record is deactivated, not permanently removed. Doctors assigned to it keep their assignment.`
            : ''
        }
        loading={deleting}
        onConfirm={handleDelete}
        onCancel={() => !deleting && setConfirmTarget(null)}
      />
    </div>
  );
}

export default DepartmentManagement;
