import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { NotificationProvider } from './context/NotificationContext';
import ProtectedRoute from './components/ProtectedRoute';
import MainLayout from './components/layout/MainLayout';

// Auth Pages
import Login from './pages/auth/Login';
import Signup from './pages/auth/Signup';

// App Pages
import Dashboard from './pages/Dashboard';
import DepartmentManagement from './pages/DepartmentManagement';
import DoctorManagement from './pages/DoctorManagement';
import PatientManagement from './pages/PatientManagement';
import AppointmentManagement from './pages/AppointmentManagement';
import PrescriptionManagement from './pages/PrescriptionManagement';
import MedicalHistoryManagement from './pages/MedicalHistoryManagement';
import LaboratoryTestManagement from './pages/LaboratoryTestManagement';
import BillManagement from './pages/BillManagement';
import RoomManagement from './pages/RoomManagement';
import UserManagement from './pages/UserManagement';

// Error Pages
import NotFound from './pages/NotFound';
import Unauthorized from './pages/Unauthorized';

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <NotificationProvider>
          <Routes>
            {/* Public Auth Routes */}
            <Route path="/login" element={<Login />} />
            <Route path="/signup" element={<Signup />} />
            <Route path="/unauthorized" element={<Unauthorized />} />

            {/* Protected Routes with MainLayout */}
            <Route
              element={
                <ProtectedRoute>
                  <MainLayout />
                </ProtectedRoute>
              }
            >
              <Route path="/dashboard" element={<Dashboard />} />
              <Route path="/departments" element={<DepartmentManagement />} />
              <Route path="/doctors" element={<DoctorManagement />} />
              <Route path="/patients" element={<PatientManagement />} />
              <Route path="/appointments" element={<AppointmentManagement />} />
              <Route path="/prescriptions" element={<PrescriptionManagement />} />
              <Route path="/medical-history" element={<MedicalHistoryManagement />} />
              <Route path="/laboratory-tests" element={<LaboratoryTestManagement />} />
              <Route path="/bills" element={<BillManagement />} />
              <Route path="/rooms" element={<RoomManagement />} />
            <Route path="/users" element={<UserManagement />} />
            </Route>

            {/* Default & Error Routes */}
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
            <Route path="*" element={<NotFound />} />
          </Routes>
        </NotificationProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;