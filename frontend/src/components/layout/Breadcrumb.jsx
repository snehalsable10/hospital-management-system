import React from 'react';
import { useLocation, Link } from 'react-router-dom';
import { ChevronRight, Home } from 'lucide-react';

const Breadcrumb = () => {
  const location = useLocation();

  const breadcrumbItems = () => {
    const pathnames = location.pathname.split('/').filter((x) => x);

    if (pathnames.length === 0) {
      return [];
    }

    const labelMap = {
      dashboard: 'Dashboard',
      departments: 'Departments',
      doctors: 'Doctors',
      patients: 'Patients',
      appointments: 'Appointments',
      prescriptions: 'Prescriptions',
      'medical-history': 'Medical History',
      'laboratory-tests': 'Laboratory Tests',
      bills: 'Bills',
      rooms: 'Rooms',
      edit: 'Edit',
      create: 'Create',
      view: 'View',
    };

    return pathnames.map((name, index) => ({
      label: labelMap[name] || name.charAt(0).toUpperCase() + name.slice(1),
      path: '/' + pathnames.slice(0, index + 1).join('/'),
      isLast: index === pathnames.length - 1,
    }));
  };

  const items = breadcrumbItems();

  if (items.length === 0) {
    return null;
  }

  return (
    <nav className="flex items-center gap-2 px-6 py-3 bg-white border-b border-gray-200">
      <Link
        to="/dashboard"
        className="flex items-center gap-2 text-gray-700 hover:text-primary-600 transition-colors"
      >
        <Home className="w-4 h-4" />
        <span className="text-sm font-medium">Home</span>
      </Link>

      {items.map((item) => (
        <div key={item.path} className="flex items-center gap-2">
          <ChevronRight className="w-4 h-4 text-gray-400" />
          {item.isLast ? (
            <span className="text-sm font-medium text-gray-900">{item.label}</span>
          ) : (
            <Link
              to={item.path}
              className="text-sm font-medium text-gray-700 hover:text-primary-600 transition-colors"
            >
              {item.label}
            </Link>
          )}
        </div>
      ))}
    </nav>
  );
};

export default Breadcrumb;
