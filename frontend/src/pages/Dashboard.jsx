import React, { useState, useEffect } from 'react';
import patientService from '../services/patientService';
import doctorService from '../services/doctorService';
import appointmentService from '../services/appointmentService';
import billService from '../services/billService';
import roomService from '../services/roomService';

const Dashboard = () => {
  const [stats, setStats] = useState({
    totalPatients: 0,
    totalDoctors: 0,
    totalAppointments: 0,
    totalRooms: 0,
    occupiedRooms: 0,
    availableRooms: 0,
    totalRevenue: 0,
    paidBills: 0,
    unpaidBills: 0
  });
  const [recentAppointments, setRecentAppointments] = useState([]);
  const [occupancyData, setOccupancyData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    loadDashboardData();
  }, []);

  const loadDashboardData = async () => {
    try {
      setLoading(true);
      
      const patientsRes = await patientService.getAllPatients();
      const doctorsRes = await doctorService.getAllDoctors();
      const appointmentsRes = await appointmentService.getAllAppointments();
      const billsRes = await billService.getAllBills();
      const roomsRes = await roomService.getAllRooms();

      const patients = patientsRes.data || [];
      const doctors = doctorsRes.data || [];
      const appointments = appointmentsRes.data || [];
      const bills = billsRes.data || [];
      const rooms = roomsRes.data || [];

      // Calculate stats
      const totalRevenue = bills.reduce((sum, bill) => sum + (bill.totalAmount || 0), 0);
      const paidBills = bills.filter(b => b.status === 'PAID').length;
      const unpaidBills = bills.filter(b => b.status === 'UNPAID').length;
      const occupiedRooms = rooms.filter(r => r.occupiedBeds > 0).length;
      const availableRooms = rooms.filter(r => r.status === 'AVAILABLE').length;

      setStats({
        totalPatients: patients.length,
        totalDoctors: doctors.length,
        totalAppointments: appointments.length,
        totalRooms: rooms.length,
        occupiedRooms,
        availableRooms,
        totalRevenue: totalRevenue.toFixed(2),
        paidBills,
        unpaidBills
      });

      // Get recent appointments (last 5)
      const recent = appointments
        .sort((a, b) => new Date(b.appointmentDate) - new Date(a.appointmentDate))
        .slice(0, 5);
      setRecentAppointments(recent);

      // Calculate occupancy by room type
      const roomTypeOccupancy = {};
      rooms.forEach(room => {
        if (!roomTypeOccupancy[room.roomType]) {
          roomTypeOccupancy[room.roomType] = {
            total: 0,
            occupied: 0
          };
        }
        roomTypeOccupancy[room.roomType].total += room.capacity;
        roomTypeOccupancy[room.roomType].occupied += room.occupiedBeds;
      });

      const occupancyArray = Object.entries(roomTypeOccupancy).map(([type, data]) => ({
        type,
        occupancyPercent: Math.round((data.occupied / data.total) * 100)
      }));
      setOccupancyData(occupancyArray);

      setError('');
    } catch (err) {
      setError('Failed to load dashboard data');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const getPatientName = (patientId) => {
    // Since we don't have full patient data in appointments, show ID
    return `Patient #${patientId}`;
  };

  const getDoctorName = (doctorId) => {
    return `Doctor #${doctorId}`;
  };

  if (loading) {
    return <div className="container mt-4"><p>Loading dashboard...</p></div>;
  }

  return (
    <div className="container-fluid mt-4">
      <h1 className="mb-4">Hospital Management Dashboard</h1>

      {error && <div className="alert alert-danger">{error}</div>}

      {/* Key Statistics */}
      <div className="row mb-4">
        <div className="col-md-3 mb-3">
          <div className="card bg-primary text-white">
            <div className="card-body">
              <h5 className="card-title">Total Patients</h5>
              <p className="card-text display-4">{stats.totalPatients}</p>
            </div>
          </div>
        </div>

        <div className="col-md-3 mb-3">
          <div className="card bg-success text-white">
            <div className="card-body">
              <h5 className="card-title">Total Doctors</h5>
              <p className="card-text display-4">{stats.totalDoctors}</p>
            </div>
          </div>
        </div>

        <div className="col-md-3 mb-3">
          <div className="card bg-info text-white">
            <div className="card-body">
              <h5 className="card-title">Total Appointments</h5>
              <p className="card-text display-4">{stats.totalAppointments}</p>
            </div>
          </div>
        </div>

        <div className="col-md-3 mb-3">
          <div className="card bg-warning text-white">
            <div className="card-body">
              <h5 className="card-title">Total Revenue</h5>
              <p className="card-text display-5">${stats.totalRevenue}</p>
            </div>
          </div>
        </div>
      </div>

      {/* Room & Bed Statistics */}
      <div className="row mb-4">
        <div className="col-md-3 mb-3">
          <div className="card border-primary">
            <div className="card-body">
              <h5 className="card-title">Total Rooms</h5>
              <p className="card-text display-4">{stats.totalRooms}</p>
            </div>
          </div>
        </div>

        <div className="col-md-3 mb-3">
          <div className="card border-success">
            <div className="card-body">
              <h5 className="card-title">Occupied Rooms</h5>
              <p className="card-text display-4">{stats.occupiedRooms}</p>
            </div>
          </div>
        </div>

        <div className="col-md-3 mb-3">
          <div className="card border-info">
            <div className="card-body">
              <h5 className="card-title">Available Rooms</h5>
              <p className="card-text display-4">{stats.availableRooms}</p>
            </div>
          </div>
        </div>

        <div className="col-md-3 mb-3">
          <div className="card border-warning">
            <div className="card-body">
              <h5 className="card-title">Occupancy Rate</h5>
              <p className="card-text display-5">
                {stats.totalRooms > 0 
                  ? Math.round((stats.occupiedRooms / stats.totalRooms) * 100) 
                  : 0}%
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* Billing Statistics */}
      <div className="row mb-4">
        <div className="col-md-4 mb-3">
          <div className="card border-success">
            <div className="card-body">
              <h5 className="card-title">Paid Bills</h5>
              <p className="card-text display-4">{stats.paidBills}</p>
            </div>
          </div>
        </div>

        <div className="col-md-4 mb-3">
          <div className="card border-danger">
            <div className="card-body">
              <h5 className="card-title">Unpaid Bills</h5>
              <p className="card-text display-4">{stats.unpaidBills}</p>
            </div>
          </div>
        </div>

        <div className="col-md-4 mb-3">
          <div className="card border-info">
            <div className="card-body">
              <h5 className="card-title">Payment Rate</h5>
              <p className="card-text display-5">
                {(stats.paidBills + stats.unpaidBills) > 0
                  ? Math.round((stats.paidBills / (stats.paidBills + stats.unpaidBills)) * 100)
                  : 0}%
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* Occupancy by Room Type */}
      {occupancyData.length > 0 && (
        <div className="card mb-4">
          <div className="card-header">
            <h5>Occupancy by Room Type</h5>
          </div>
          <div className="card-body">
            <div className="row">
              {occupancyData.map(item => (
                <div key={item.type} className="col-md-6 mb-3">
                  <h6>{item.type} Rooms</h6>
                  <div className="progress" style={{ height: '30px' }}>
                    <div
                      className={`progress-bar ${item.occupancyPercent === 100 ? 'bg-danger' : item.occupancyPercent >= 75 ? 'bg-warning' : 'bg-success'}`}
                      role="progressbar"
                      style={{ width: `${item.occupancyPercent}%` }}
                    >
                      {item.occupancyPercent}%
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* Recent Appointments */}
      <div className="card">
        <div className="card-header">
          <h5>Recent Appointments (Last 5)</h5>
        </div>
        <div className="card-body">
          {recentAppointments.length === 0 ? (
            <p className="text-muted">No appointments found</p>
          ) : (
            <div className="table-responsive">
              <table className="table table-hover">
                <thead className="table-light">
                  <tr>
                    <th>Date</th>
                    <th>Time</th>
                    <th>Patient</th>
                    <th>Doctor</th>
                    <th>Reason</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {recentAppointments.map(appt => (
                    <tr key={appt.id}>
                      <td>{new Date(appt.appointmentDate).toLocaleDateString()}</td>
                      <td>{appt.appointmentTime}</td>
                      <td>{getPatientName(appt.patient.id)}</td>
                      <td>{getDoctorName(appt.doctor.id)}</td>
                      <td>{appt.reason}</td>
                      <td>
                        <span className={`badge bg-${appt.status === 'SCHEDULED' ? 'primary' : appt.status === 'COMPLETED' ? 'success' : 'secondary'}`}>
                          {appt.status}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default Dashboard;