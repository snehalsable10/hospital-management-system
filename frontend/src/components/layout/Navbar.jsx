import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Menu, X, Search, LogOut, ChevronDown } from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';

const ROLE_LABELS = {
  ADMIN: 'Administrator',
  DOCTOR: 'Doctor',
  STAFF: 'Staff',
  PATIENT: 'Patient',
};

const Navbar = ({ onMenuToggle, sidebarOpen }) => {
  const [profileOpen, setProfileOpen] = useState(false);
  const navigate = useNavigate();
  const { user, logout } = useAuth();

  const fullName = user ? `${user.firstName} ${user.lastName}`.trim() : '';
  const initials = user
    ? `${user.firstName?.[0] || ''}${user.lastName?.[0] || ''}`.toUpperCase()
    : '?';
  const roleLabel = ROLE_LABELS[user?.role] || user?.role || '';

  const handleLogout = async () => {
    setProfileOpen(false);
    await logout();
    navigate('/login');
  };

  return (
    <nav className="bg-white border-b border-gray-200 shadow-sm h-navbar flex items-center px-6 z-30">
      <div className="flex-1 flex items-center gap-4">
        <button
          onClick={onMenuToggle}
          className="p-2 hover:bg-gray-100 rounded-lg transition-colors text-gray-600"
          title={sidebarOpen ? 'Close sidebar' : 'Open sidebar'}
        >
          {sidebarOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
        </button>

        {/* Placeholder: global search has no backend endpoint yet */}
        <div className="hidden md:flex items-center gap-2 flex-1 max-w-md text-gray-400">
          <Search className="w-4 h-4" />
          <span className="text-sm">Search (coming soon)</span>
        </div>
      </div>

      <div className="flex items-center gap-4">
        <div className="relative">
          <button
            onClick={() => setProfileOpen(!profileOpen)}
            className="flex items-center gap-3 px-3 py-2 hover:bg-gray-100 rounded-lg transition-colors"
          >
            <div className="w-9 h-9 bg-primary-600 rounded-full flex items-center justify-center text-white text-sm font-semibold">
              {initials}
            </div>
            <div className="hidden md:block text-left">
              <p className="text-sm font-medium text-gray-900">{fullName || 'Signed in'}</p>
              <p className="text-xs text-gray-500">{roleLabel}</p>
            </div>
            <ChevronDown className="w-4 h-4 text-gray-400" />
          </button>

          {profileOpen && (
            <div className="absolute right-0 mt-2 w-56 bg-white border border-gray-200 rounded-lg shadow-lg py-2 z-50">
              <div className="px-4 py-2 border-b border-gray-100">
                <p className="text-sm font-medium text-gray-900">{fullName}</p>
                <p className="text-xs text-gray-500 truncate">{user?.email}</p>
                <p className="text-xs text-gray-400 mt-0.5">{roleLabel}</p>
              </div>

              <button
                onClick={handleLogout}
                className="mt-1 w-full flex items-center gap-3 px-4 py-2 text-sm text-danger-600 hover:bg-danger-50 transition-colors"
              >
                <LogOut className="w-4 h-4" />
                <span>Log out</span>
              </button>
            </div>
          )}
        </div>
      </div>
    </nav>
  );
};

export default Navbar;