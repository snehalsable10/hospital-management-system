package com.hms.service;

import com.hms.dto.request.BillRequest;
import com.hms.dto.response.ApiResponse;
import com.hms.dto.response.PageResponse;
import com.hms.entity.Bill;
import com.hms.entity.Doctor;
import com.hms.entity.Patient;
import com.hms.exception.ResourceNotFoundException;
import com.hms.repository.BillRepository;
import com.hms.repository.DoctorRepository;
import com.hms.repository.PatientRepository;
import com.hms.util.PaginationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class BillService {

    private final BillRepository billRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    /**
     * Get all bills
     * Cached for 10 minutes
     */
    @Cacheable(value = "bills", key = "'getAllBills'")
    public List<Bill> getAllBills() {
        return billRepository.findByIsActiveTrue();
    }

    /**
     * Get bill by ID
     * Cached for 10 minutes with key = bill ID
     */
    @Cacheable(value = "bill", key = "#id")
    public Optional<Bill> getBillById(Long id) {
        return billRepository.findById(id);
    }

    /**
     * Get all bills for a patient
     * Cached for 10 minutes
     *
     * @throws ResourceNotFoundException if no patient exists with the given id
     */
    @Cacheable(value = "bills", key = "'getBillsByPatient:' + #patientId")
    public List<Bill> getBillsByPatient(Long patientId) {
        if (!patientRepository.existsById(patientId)) {
            throw new ResourceNotFoundException("Patient not found with id: " + patientId);
        }
        return billRepository.findByPatientIdAndIsActiveTrue(patientId);
    }

    /**
     * Get all bills for a doctor
     * Cached for 10 minutes
     *
     * @throws ResourceNotFoundException if no doctor exists with the given id
     */
    @Cacheable(value = "bills", key = "'getBillsByDoctor:' + #doctorId")
    public List<Bill> getBillsByDoctor(Long doctorId) {
        if (!doctorRepository.existsById(doctorId)) {
            throw new ResourceNotFoundException("Doctor not found with id: " + doctorId);
        }
        return billRepository.findByDoctorIdAndIsActiveTrue(doctorId);
    }

    /**
     * Get bills by status
     * Cached for 10 minutes
     */
    @Cacheable(value = "bills", key = "'getBillsByStatus:' + #status")
    public List<Bill> getBillsByStatus(String status) {
        return billRepository.findByStatusAndIsActiveTrue(status);
    }

    /**
     * Get bills in a date range
     * Cached for 10 minutes
     */
    @Cacheable(value = "bills", key = "'getBillsByDateRange:' + #startDate + ':' + #endDate")
    public List<Bill> getBillsByDateRange(LocalDate startDate, LocalDate endDate) {
        return billRepository.findByBillDateBetweenAndIsActiveTrue(startDate, endDate);
    }

    /**
     * Get unpaid bills for a patient
     * Cached for 10 minutes
     *
     * @throws ResourceNotFoundException if no patient exists with the given id
     */
    @Cacheable(value = "bills", key = "'getPatientUnpaidBills:' + #patientId")
    public List<Bill> getPatientUnpaidBills(Long patientId) {
        if (!patientRepository.existsById(patientId)) {
            throw new ResourceNotFoundException("Patient not found with id: " + patientId);
        }
        return billRepository.findByPatientIdAndStatusAndIsActiveTrue(patientId, "UNPAID");
    }

    /**
     * Get paid bills for a doctor
     * Cached for 10 minutes
     *
     * @throws ResourceNotFoundException if no doctor exists with the given id
     */
    @Cacheable(value = "bills", key = "'getDoctorPaidBills:' + #doctorId")
    public List<Bill> getDoctorPaidBills(Long doctorId) {
        if (!doctorRepository.existsById(doctorId)) {
            throw new ResourceNotFoundException("Doctor not found with id: " + doctorId);
        }
        return billRepository.findByDoctorIdAndStatusAndIsActiveTrue(doctorId, "PAID");
    }

    /**
     * Create a new bill
     * Clears all bill caches on create
     *
     * @throws IllegalArgumentException if the referenced patient or doctor does not exist
     */
    @CacheEvict(value = {"bills", "bill"}, allEntries = true)
    public ApiResponse createBill(BillRequest request) {
        verifyTotalMatchesParts(request);

        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Patient not found with id: " + request.getPatientId()));

        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Doctor not found with id: " + request.getDoctorId()));

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
        bill.setIsActive(true);

        billRepository.save(bill);

        return new ApiResponse("Bill created successfully", true);
    }

    /**
     * Update an existing bill
     * Clears all bill caches on update
     *
     * @throws ResourceNotFoundException if no bill exists with the given id
     * @throws IllegalArgumentException  if the referenced patient or doctor does not exist
     */
    @CacheEvict(value = {"bills", "bill"}, allEntries = true)
    public ApiResponse updateBill(Long id, BillRequest request) {
        verifyTotalMatchesParts(request);

        Bill bill = billRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found with id: " + id));

        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Patient not found with id: " + request.getPatientId()));

        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Doctor not found with id: " + request.getDoctorId()));

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

        billRepository.save(bill);

        return new ApiResponse("Bill updated successfully", true);
    }

    /**
     * Delete a bill (soft delete)
     * Clears all bill caches on delete
     *
     * @throws ResourceNotFoundException if no bill exists with the given id
     */
    @CacheEvict(value = {"bills", "bill"}, allEntries = true)
    public ApiResponse deleteBill(Long id) {
        Bill bill = billRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found with id: " + id));

        bill.setIsActive(false);
        billRepository.save(bill);

        return new ApiResponse("Bill deleted successfully", true);
    }

    /**
     * Get all bills with pagination
     */
    public PageResponse<Bill> getAllBillsPaginated(int pageNumber, int pageSize) {
        PaginationUtil.validatePaginationParams(pageNumber, pageSize);
        Pageable pageable = PaginationUtil.pageRequest(pageNumber, pageSize);
        Page<Bill> page = billRepository.findByIsActiveTrue(pageable);
        return PaginationUtil.toPageResponse(page);
    }

    /**
     * Get bills by patient with pagination
     */
    public PageResponse<Bill> getBillsByPatientPaginated(Long patientId, int pageNumber, int pageSize) {
        PaginationUtil.validatePaginationParams(pageNumber, pageSize);
        Pageable pageable = PaginationUtil.pageRequest(pageNumber, pageSize);
        Page<Bill> page = billRepository.findByPatientIdAndIsActiveTrue(patientId, pageable);
        return PaginationUtil.toPageResponse(page);
    }

    /**
     * Get bills by doctor with pagination
     */
    public PageResponse<Bill> getBillsByDoctorPaginated(Long doctorId, int pageNumber, int pageSize) {
        PaginationUtil.validatePaginationParams(pageNumber, pageSize);
        Pageable pageable = PaginationUtil.pageRequest(pageNumber, pageSize);
        Page<Bill> page = billRepository.findByDoctorIdAndIsActiveTrue(doctorId, pageable);
        return PaginationUtil.toPageResponse(page);
    }

    /**
     * Get bills by status with pagination
     */
    public PageResponse<Bill> getBillsByStatusPaginated(String status, int pageNumber, int pageSize) {
        PaginationUtil.validatePaginationParams(pageNumber, pageSize);
        Pageable pageable = PaginationUtil.pageRequest(pageNumber, pageSize);
        Page<Bill> page = billRepository.findByStatusAndIsActiveTrue(status, pageable);
        return PaginationUtil.toPageResponse(page);
    }

    /**
     * Get bills by date range with pagination
     */
    public PageResponse<Bill> getBillsByDateRangePaginated(LocalDate startDate, LocalDate endDate, int pageNumber, int pageSize) {
        PaginationUtil.validatePaginationParams(pageNumber, pageSize);
        Pageable pageable = PaginationUtil.pageRequest(pageNumber, pageSize);
        Page<Bill> page = billRepository.findByBillDateBetweenAndIsActiveTrue(startDate, endDate, pageable);
        return PaginationUtil.toPageResponse(page);
    }

    /**
     * Get unpaid bills for a patient with pagination
     */
    public PageResponse<Bill> getPatientUnpaidBillsPaginated(Long patientId, int pageNumber, int pageSize) {
        PaginationUtil.validatePaginationParams(pageNumber, pageSize);
        Pageable pageable = PaginationUtil.pageRequest(pageNumber, pageSize);
        Page<Bill> page = billRepository.findByPatientIdAndStatusAndIsActiveTrue(patientId, "UNPAID", pageable);
        return PaginationUtil.toPageResponse(page);
    }

    /**
     * A bill's total has to be the sum of its parts.
     *
     * The request carries the four fee columns and the total separately, and
     * nothing tied them together - a caller could send fees of 150 and a total
     * of 99999 and it would be stored and later summed into the revenue
     * figures. Rejecting the mismatch keeps the stored total meaningful.
     */
    private void verifyTotalMatchesParts(BillRequest request) {
        BigDecimal parts = Stream.of(
                        request.getConsultationFee(),
                        request.getTestsFee(),
                        request.getMedicationsFee(),
                        request.getOtherCharges())
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (request.getTotalAmount() == null || parts.compareTo(request.getTotalAmount()) != 0) {
            throw new IllegalArgumentException(
                    "Total amount must equal the sum of the fees (" + parts.toPlainString() + ")");
        }
    }


}