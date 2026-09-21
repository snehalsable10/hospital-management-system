import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import {
  LayoutDashboard,
  Building2,
  Users,
  UserCog,
  Calendar,
  FileText,
  ClipboardList,
  FlaskConical,
  DollarSign,
  Stethoscope,
  ChevronRight,
  BedDouble,
  ShieldCheck,
} from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';

/**
 * `roles` mirrors the @PreAuthorize rule on each module's list endpoint.
 * Showing a link the backend will refuse is worse than hiding it: the user
 * clicks it and gets an access-denied page instead of a menu that reflects
 * what they can actually do.
 */
const MENU_ITEMS = [
  { path: '/dashboard', icon: LayoutDashboard, label: 'Dashboard', roles: ['ADMIN', 'STAFF', 'DOCTOR', 'PATIENT'] },
  { path: '/departments', icon: Building2, label: 'Departments', roles: ['ADMIN', 'STAFF', 'DOCTOR', 'PATIENT'] },
  { path: '/doctors', icon: UserCog, label: 'Doctors', roles: ['ADMIN', 'STAFF', 'DOCTOR', 'PATIENT'] },
  { path: '/patients', icon: Users, label: 'Patients', roles: ['ADMIN', 'STAFF', 'DOCTOR'] },
  { path: '/appointments', icon: Calendar, label: 'Appointments', roles: ['ADMIN', 'STAFF'] },
  { path: '/prescriptions', icon: FileText, label: 'Prescriptions', roles: ['ADMIN', 'STAFF'] },
  { path: '/medical-history', icon: ClipboardList, label: 'Medical History', roles: ['ADMIN', 'STAFF'] },
  { path: '/laboratory-tests', icon: FlaskConical, label: 'Lab Tests', roles: ['ADMIN', 'STAFF'] },
  { path: '/bills', icon: DollarSign, label: 'Bills', roles: ['ADMIN', 'STAFF'] },
  { path: '/rooms', icon: BedDouble, label: 'Rooms', roles: ['ADMIN', 'STAFF', 'DOCTOR', 'PATIENT'] },
  { path: '/users', icon: ShieldCheck, label: 'Users', roles: ['ADMIN'] },
];

const Sidebar = ({ open }) => {
  const location = useLocation();
  const { role } = useAuth();

  const visibleItems = MENU_ITEMS.filter((item) => !role || item.roles.includes(role));
  const isActive = (path) => location.pathname === path;

  return (
    <div
      className={`${
        open ? 'w-sidebar' : 'w-0'
      } bg-primary-900 text-white fixed left-0 top-0 h-screen overflow-y-auto transition-all duration-300 z-40 shadow-lg`}
    >
      {open && (
        <div className="p-6 border-b border-primary-700">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-primary-500 rounded-lg flex items-center justify-center">
              <Stethoscope className="w-6 h-6 text-white" />
            </div>
            <div>
              <h1 className="text-lg font-bold">HMS</h1>
              <p className="text-xs text-primary-300">Hospital Management</p>
            </div>
          </div>
        </div>
      )}

      <nav className="p-4 space-y-2">
        {visibleItems.map((item) => {
          const Icon = item.icon;
          const active = isActive(item.path);

          return (
            <Link
              key={item.path}
              to={item.path}
              className={`flex items-center gap-3 px-4 py-3 rounded-lg transition-all duration-200 group ${
                active
                  ? 'bg-primary-700 text-white shadow-md'
                  : 'text-primary-100 hover:bg-primary-800'
              }`}
              title={!open ? item.label : ''}
            >
              <Icon className="w-5 h-5 flex-shrink-0" />
              {open && (
                <>
                  <span className="flex-1 text-sm font-medium">{item.label}</span>
                  {active && <ChevronRight className="w-4 h-4" />}
                </>
              )}
            </Link>
          );
        })}
      </nav>

      {open && (
        <div className="absolute bottom-0 left-0 right-0 p-4 border-t border-primary-700 bg-primary-950">
          <p className="text-xs text-primary-400 text-center">
            © {new Date().getFullYear()} Hospital Management System
          </p>
        </div>
      )}
    </div>
  );
};

export default Sidebar;