import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import {
  Users,
  UserCog,
  CalendarDays,
  IndianRupee,
  BedDouble,
  ArrowRight,
} from 'lucide-react';
import KPICard from '../components/dashboard/KPICard';
import BarChart from '../components/charts/BarChart';
import PieChart from '../components/charts/PieChart';
import authService from '../services/authService';
import dashboardService from '../services/dashboardService';

const STATUS_STYLES = {
  SCHEDULED: 'bg-info-100 text-info-800',
  COMPLETED: 'bg-success-100 text-success-800',
  CANCELLED: 'bg-danger-100 text-danger-800',
  PAID: 'bg-success-100 text-success-800',
  UNPAID: 'bg-warning-100 text-warning-800',
};

/** "14:30:00" -> "2:30 PM", matching how the appointments page reads. */
const clockTime = (value) => {
  if (!value) return '—';
  const [h, m] = value.split(':');
  const hour = Number(h);
  const suffix = hour >= 12 ? 'PM' : 'AM';
  return `${hour % 12 === 0 ? 12 : hour % 12}:${m} ${suffix}`;
};

const money = (amount) =>
  `₹${Number(amount || 0).toLocaleString('en-IN', { maximumFractionDigits: 0 })}`;

const StatusPill = ({ status }) => (
  <span
    className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold ${
      STATUS_STYLES[status] || 'bg-gray-100 text-gray-700'
    }`}
  >
    {status}
  </span>
);

const Dashboard = () => {
  const [stats, setStats] = useState({
    totalPatients: 0,
    totalDoctors: 0,
    totalAppointments: 0,
    totalRooms: 0,
    occupiedBeds: 0,
    totalBeds: 0,
    totalRevenue: 0,
    outstanding: 0,
    paidBills: 0,
    unpaidBills: 0,
  });
  const [recentAppointments, setRecentAppointments] = useState([]);
  const [appointmentsByStatus, setAppointmentsByStatus] = useState([]);
  const [doctorsByDepartment, setDoctorsByDepartment] = useState([]);
  const [occupancy, setOccupancy] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  // Which datasets this role is allowed to see; panels for the rest are hidden
  // rather than shown empty, which would read as "no data" instead of "not yours".
  const [available, setAvailable] = useState({
    patients: true,
    doctors: true,
    appointments: true,
    bills: true,
    rooms: true,
  });

  const user = authService.getCurrentUser();

  useEffect(() => {
    loadDashboardData();
  }, []);

  const loadDashboardData = async () => {
    setLoading(true);
    setError('');

    try {
      // One call. This used to fetch five full lists and count them here,
      // which meant five round trips and every row of every table on the wire
      // to render a handful of numbers.
      const result = await dashboardService.getSummary();
      const data = result.data || {};

      // The server says which sections this role may see. A section it may not
      // see comes back null rather than 0, so the panel is hidden instead of
      // reading as "none yet".
      const sections = data.sections || {};
      setAvailable({
        patients: !!sections.patients,
        doctors: !!sections.doctors,
        appointments: !!sections.appointments,
        bills: !!sections.bills,
        rooms: !!sections.rooms,
      });

      setStats({
        totalPatients: data.totalPatients ?? 0,
        totalDoctors: data.totalDoctors ?? 0,
        totalAppointments: data.totalAppointments ?? 0,
        totalRooms: data.totalRooms ?? 0,
        occupiedBeds: data.occupiedBeds ?? 0,
        totalBeds: data.totalBeds ?? 0,
        totalRevenue: data.totalRevenue ?? 0,
        outstanding: data.outstanding ?? 0,
        paidBills: data.paidBills ?? 0,
        unpaidBills: data.unpaidBills ?? 0,
      });

      setAppointmentsByStatus(data.appointmentsByStatus || []);
      setDoctorsByDepartment(data.doctorsByDepartment || []);
      setOccupancy(data.occupancyByRoomType || []);
      setRecentAppointments(data.recentAppointments || []);
    } catch (err) {
      setError(
        err.response?.data?.message ||
          'Could not load dashboard data. Check that the backend is running.'
      );
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="p-6 space-y-6">
        <div className="skeleton h-8 w-72" />
        <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-5">
          {[0, 1, 2, 3].map((i) => (
            <div key={i} className="skeleton h-32 w-full" />
          ))}
        </div>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
          <div className="skeleton h-80 w-full" />
          <div className="skeleton h-80 w-full" />
        </div>
      </div>
    );
  }

  const occupancyRate =
    stats.totalBeds > 0 ? Math.round((stats.occupiedBeds / stats.totalBeds) * 100) : 0;

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">
            Welcome back{user?.firstName ? `, ${user.firstName}` : ''}
          </h1>
          <p className="text-gray-600 text-sm mt-1">
            Here's what's happening across the hospital today.
          </p>
        </div>
        <p className="text-sm text-gray-500">
          {new Date().toLocaleDateString('en-IN', {
            weekday: 'short',
            day: '2-digit',
            month: 'short',
            year: 'numeric',
          })}
        </p>
      </div>

      {error && (
        <div className="rounded-lg border border-danger-200 bg-danger-50 text-danger-800 px-4 py-3 text-sm">
          {error}
        </div>
      )}

      {/* KPIs */}
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-5">
        {available.patients && (
          <KPICard title="Total Patients" value={stats.totalPatients} icon={Users} color="primary" />
        )}
        {available.appointments && (
          <KPICard
            title="Appointments"
            value={stats.totalAppointments}
            icon={CalendarDays}
            color="info"
          />
        )}
        {available.bills && (
          <KPICard
            title="Revenue Billed"
            value={money(stats.totalRevenue)}
            icon={IndianRupee}
            color="success"
          />
        )}
        {available.doctors && (
          <KPICard title="Doctors" value={stats.totalDoctors} icon={UserCog} color="warning" />
        )}
      </div>

      {/* Charts. A panel the role cannot see is omitted entirely - showing it
          empty would read as "no data" rather than "not available to you". */}
      {(available.appointments || available.doctors) && (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
          {available.appointments &&
            (appointmentsByStatus.length > 0 ? (
              <BarChart
                title="Appointments by Status"
                data={appointmentsByStatus}
                dataKey="value"
                xAxisKey="name"
                color="#3b82f6"
              />
            ) : (
              <div className="card">
                <h3 className="text-lg font-semibold text-gray-900 mb-4">Appointments by Status</h3>
                <p className="text-gray-500 text-sm">No appointments recorded yet.</p>
              </div>
            ))}

          {available.doctors &&
            (doctorsByDepartment.length > 0 ? (
              <PieChart
                title="Doctors by Department"
                data={doctorsByDepartment}
                dataKey="value"
                nameKey="name"
              />
            ) : (
              <div className="card">
                <h3 className="text-lg font-semibold text-gray-900 mb-4">Doctors by Department</h3>
                <p className="text-gray-500 text-sm">No doctors registered yet.</p>
              </div>
            ))}
        </div>
      )}

      {/* Recent appointments + billing/occupancy */}
      <div
        className={`grid grid-cols-1 gap-5 ${
          available.appointments ? 'lg:grid-cols-3' : 'lg:grid-cols-2'
        }`}
      >
        {available.appointments && (
        <div className="lg:col-span-2 bg-white rounded-lg border border-gray-200 shadow-sm overflow-hidden">
          <div className="flex items-center px-5 py-4 border-b border-gray-200">
            <h3 className="text-base font-semibold text-gray-900">Recent Appointments</h3>
            <Link
              to="/appointments"
              className="ml-auto text-sm font-semibold text-primary-600 hover:text-primary-700 inline-flex items-center gap-1"
            >
              View all <ArrowRight className="w-4 h-4" />
            </Link>
          </div>

          {recentAppointments.length === 0 ? (
            <div className="empty-state">
              <CalendarDays className="empty-state-icon" />
              <p className="empty-state-title">No appointments yet</p>
              <p className="empty-state-text">Scheduled appointments will appear here.</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="bg-gray-50 text-left text-xs uppercase tracking-wide text-gray-500">
                    <th className="px-5 py-3 font-semibold">Date</th>
                    <th className="px-5 py-3 font-semibold">Time</th>
                    <th className="px-5 py-3 font-semibold">Patient</th>
                    <th className="px-5 py-3 font-semibold">Doctor</th>
                    <th className="px-5 py-3 font-semibold">Status</th>
                  </tr>
                </thead>
                <tbody>
                  {recentAppointments.map((appt) => (
                    <tr key={appt.id} className="border-t border-gray-100 hover:bg-gray-50">
                      <td className="px-5 py-3 text-gray-900">
                        {new Date(appt.appointmentDate).toLocaleDateString('en-IN', {
                          day: '2-digit',
                          month: 'short',
                          year: 'numeric',
                        })}
                      </td>
                      <td className="px-5 py-3 text-gray-600">{clockTime(appt.appointmentTime)}</td>
                      <td className="px-5 py-3 font-medium text-gray-900">
                        {appt.patientName || '—'}
                      </td>
                      <td className="px-5 py-3 text-gray-600">
                        {appt.doctorName ? `Dr. ${appt.doctorName}` : '—'}
                      </td>
                      <td className="px-5 py-3">
                        <StatusPill status={appt.status} />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
        )}

        {(available.bills || available.rooms) && (
        <div className="space-y-5">
          {/* Billing */}
          {available.bills && (
          <div className="bg-white rounded-lg border border-gray-200 shadow-sm p-5">
            <h3 className="text-base font-semibold text-gray-900 mb-4">Billing</h3>
            <dl className="space-y-3 text-sm">
              <div className="flex justify-between">
                <dt className="text-gray-600">Outstanding</dt>
                <dd className="font-semibold text-danger-600">{money(stats.outstanding)}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-gray-600">Paid bills</dt>
                <dd className="font-semibold text-gray-900">{stats.paidBills}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-gray-600">Unpaid bills</dt>
                <dd className="font-semibold text-gray-900">{stats.unpaidBills}</dd>
              </div>
            </dl>
            <Link
              to="/bills"
              className="mt-4 inline-flex items-center gap-1 text-sm font-semibold text-primary-600 hover:text-primary-700"
            >
              Go to billing <ArrowRight className="w-4 h-4" />
            </Link>
          </div>
          )}

          {/* Bed occupancy */}
          {available.rooms && (
          <div className="bg-white rounded-lg border border-gray-200 shadow-sm p-5">
            <div className="flex items-center mb-4">
              <h3 className="text-base font-semibold text-gray-900">Bed Occupancy</h3>
              {stats.totalBeds > 0 && (
                <span className="ml-auto text-sm font-semibold text-gray-700">
                  {occupancyRate}%
                </span>
              )}
            </div>

            {occupancy.length === 0 ? (
              <div className="text-center py-6">
                <BedDouble className="w-12 h-12 text-gray-300 mx-auto mb-3" />
                <p className="text-sm font-semibold text-gray-900">No rooms yet</p>
                <p className="text-xs text-gray-500 mt-1">
                  Add a room to start tracking bed occupancy.
                </p>
                <Link
                  to="/rooms"
                  className="mt-3 inline-flex items-center gap-1 text-sm font-semibold text-primary-600 hover:text-primary-700"
                >
                  Manage rooms <ArrowRight className="w-4 h-4" />
                </Link>
              </div>
            ) : (
              <div className="space-y-4">
                {occupancy.map((row) => {
                  const pct = row.total > 0 ? Math.round((row.occupied / row.total) * 100) : 0;
                  const barColor =
                    pct >= 90 ? 'bg-danger-500' : pct >= 70 ? 'bg-warning-500' : 'bg-success-500';
                  return (
                    <div key={row.type}>
                      <div className="flex justify-between text-sm mb-1.5">
                        <span className="text-gray-700">{row.type}</span>
                        <span className="text-gray-500">
                          {row.occupied}/{row.total} · {pct}%
                        </span>
                      </div>
                      <div className="h-2 bg-gray-100 rounded-full overflow-hidden">
                        <div className={`h-full ${barColor}`} style={{ width: `${pct}%` }} />
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
          )}
        </div>
        )}
      </div>
    </div>
  );
};

export default Dashboard;