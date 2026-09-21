package com.hms.service;

import com.hms.dto.request.BillRequest;
import com.hms.entity.Bill;
import com.hms.entity.Doctor;
import com.hms.entity.Patient;
import com.hms.repository.BillRepository;
import com.hms.repository.DoctorRepository;
import com.hms.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A bill's total has to equal the sum of its parts.
 *
 * Nothing enforced that until this session: fees of 150 with a total of 99999
 * was accepted and stored, and would later be summed into revenue. These tests
 * exist so that cannot come back.
 */
@ExtendWith(MockitoExtension.class)
class BillServiceTest {

    @Mock private BillRepository billRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private DoctorRepository doctorRepository;

    @InjectMocks private BillService billService;

    private BillRequest request;

    @BeforeEach
    void setUp() {
        Patient patient = new Patient();
        patient.setId(1L);
        Doctor doctor = new Doctor();
        doctor.setId(2L);

        // lenient: the mismatch cases throw before either lookup happens.
        lenient().when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        lenient().when(doctorRepository.findById(2L)).thenReturn(Optional.of(doctor));

        request = new BillRequest();
        request.setPatientId(1L);
        request.setDoctorId(2L);
        request.setBillDate(LocalDate.of(2026, 9, 20));
        request.setStatus("UNPAID");
        request.setPaymentMethod("CASH");
    }

    private void fees(String consultation, String tests, String medications, String other) {
        request.setConsultationFee(consultation == null ? null : new BigDecimal(consultation));
        request.setTestsFee(tests == null ? null : new BigDecimal(tests));
        request.setMedicationsFee(medications == null ? null : new BigDecimal(medications));
        request.setOtherCharges(other == null ? null : new BigDecimal(other));
    }

    @Test
    @DisplayName("a total that matches the sum of the fees is accepted")
    void acceptsMatchingTotal() {
        fees("100", "50", null, null);
        request.setTotalAmount(new BigDecimal("150"));

        assertThat(billService.createBill(request).getSuccess()).isTrue();

        ArgumentCaptor<Bill> saved = ArgumentCaptor.forClass(Bill.class);
        verify(billRepository).save(saved.capture());
        assertThat(saved.getValue().getTotalAmount()).isEqualByComparingTo("150");
    }

    @Test
    @DisplayName("a total larger than the fees is rejected and nothing is saved")
    void rejectsInflatedTotal() {
        fees("100", "50", null, null);
        request.setTotalAmount(new BigDecimal("99999"));

        assertThatThrownBy(() -> billService.createBill(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Total amount must equal the sum of the fees")
                .hasMessageContaining("150");

        verify(billRepository, never()).save(any());
    }

    @Test
    @DisplayName("a total smaller than the fees is rejected too")
    void rejectsUnderstatedTotal() {
        fees("600", "1800", "450", null);
        request.setTotalAmount(new BigDecimal("600"));

        assertThatThrownBy(() -> billService.createBill(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2850");

        verify(billRepository, never()).save(any());
    }

    @Test
    @DisplayName("blank optional fees count as zero rather than breaking the sum")
    void treatsAbsentOptionalFeesAsZero() {
        fees("600", null, null, null);
        request.setTotalAmount(new BigDecimal("600"));

        assertThat(billService.createBill(request).getSuccess()).isTrue();
        verify(billRepository).save(any());
    }

    @Test
    @DisplayName("fractional fees add up exactly, without floating point drift")
    void sumsDecimalsExactly() {
        fees("750", "1250.50", "399.50", "100");
        request.setTotalAmount(new BigDecimal("2500.00"));

        assertThat(billService.createBill(request).getSuccess()).isTrue();
        verify(billRepository).save(any());
    }

    @Test
    @DisplayName("a null total is rejected rather than treated as zero")
    void rejectsNullTotal() {
        fees("100", null, null, null);
        request.setTotalAmount(null);

        assertThatThrownBy(() -> billService.createBill(request))
                .isInstanceOf(IllegalArgumentException.class);

        verify(billRepository, never()).save(any());
    }

    @Test
    @DisplayName("the same check applies on update, not only on create")
    void rejectsInflatedTotalOnUpdate() {
        Bill existing = new Bill();
        existing.setId(7L);
        lenient().when(billRepository.findById(7L)).thenReturn(Optional.of(existing));

        fees("100", "50", null, null);
        request.setTotalAmount(new BigDecimal("99999"));

        assertThatThrownBy(() -> billService.updateBill(7L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Total amount must equal the sum of the fees");

        verify(billRepository, never()).save(any());
    }

    @Test
    @DisplayName("a matching total updates the stored bill")
    void acceptsMatchingTotalOnUpdate() {
        Bill existing = new Bill();
        existing.setId(7L);
        when(billRepository.findById(7L)).thenReturn(Optional.of(existing));

        fees("100", "50", null, null);
        request.setTotalAmount(new BigDecimal("150"));

        assertThat(billService.updateBill(7L, request).getSuccess()).isTrue();
        assertThat(existing.getTotalAmount()).isEqualByComparingTo("150");
    }
}
