package com.hms.service;

import com.hms.dto.request.BillRequest;
import com.hms.entity.Bill;
import com.hms.entity.Doctor;
import com.hms.entity.Patient;
import com.hms.repository.BillRepository;
import com.hms.repository.DoctorRepository;
import com.hms.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class BillService {

    @Autowired
    private BillRepository billRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    public List<Bill> getAllBills() {
        return billRepository.findAll();
    }

    public Optional<Bill> getBillById(Long id) {
        return billRepository.findById(id);
    }

    public List<Bill> getBillsByPatient(Long patientId) {
        if (!patientRepository.existsById(patientId)) {
            throw new RuntimeException("Patient not found");
        }
        return billRepository.findByPatientId(patientId);
    }

    public List<Bill> getBillsByDoctor(Long doctorId) {
        if (!doctorRepository.existsById(doctorId)) {
            throw new RuntimeException("Doctor not found");
        }
        return billRepository.findByDoctorId(doctorId);
    }

    public List<Bill> getBillsByStatus(String status) {
        return billRepository.findByStatus(status);
    }

    public List<Bill> getBillsByDateRange(LocalDate startDate, LocalDate endDate) {
        return billRepository.findByBillDateBetween(startDate, endDate);
    }

    public List<Bill> getPatientUnpaidBills(Long patientId) {
        if (!patientRepository.existsById(patientId)) {
            throw new RuntimeException("Patient not found");
        }
        return billRepository.findByPatientIdAndStatus(patientId, "UNPAID");
    }

    public List<Bill> getDoctorPaidBills(Long doctorId) {
        if (!doctorRepository.existsById(doctorId)) {
            throw new RuntimeException("Doctor not found");
        }
        return billRepository.findByDoctorIdAndStatus(doctorId, "PAID");
    }

    public Bill createBill(BillRequest request) {
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new RuntimeException("Patient not found"));
        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        Bill bill = new Bill();
        bill.setPatient(patient);
        bill.setDoctor(doctor);
        bill.setBillDate(request.getBillDate());
        bill.setConsultationFee(request.getConsultationFee());
        bill.setTestsFee(request.getTestsFee());
        bill.setMedicationsFee(request.getMedicationsFee());
        bill.setOtherCharges(request.getOtherCharges());
        bill.setTotalAmount(request.getTotalAmount());
        bill.setStatus(request.getStatus());
        bill.setPaymentMethod(request.getPaymentMethod());
        bill.setDescription(request.getDescription());

        return billRepository.save(bill);
    }

    public Bill updateBill(Long id, BillRequest request) {
        Bill bill = billRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bill not found"));

        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new RuntimeException("Patient not found"));
        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        bill.setPatient(patient);
        bill.setDoctor(doctor);
        bill.setBillDate(request.getBillDate());
        bill.setConsultationFee(request.getConsultationFee());
        bill.setTestsFee(request.getTestsFee());
        bill.setMedicationsFee(request.getMedicationsFee());
        bill.setOtherCharges(request.getOtherCharges());
        bill.setTotalAmount(request.getTotalAmount());
        bill.setStatus(request.getStatus());
        bill.setPaymentMethod(request.getPaymentMethod());
        bill.setDescription(request.getDescription());

        return billRepository.save(bill);
    }

    public void deleteBill(Long id) {
        Bill bill = billRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bill not found"));
        bill.setIsActive(false);
        billRepository.save(bill);
    }

}