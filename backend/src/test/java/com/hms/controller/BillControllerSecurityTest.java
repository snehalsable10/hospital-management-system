package com.hms.controller;

import com.hms.entity.Bill;
import com.hms.security.AuthService;
import com.hms.security.JwtAuthenticationFilter;
import com.hms.security.RateLimitFilter;
import com.hms.service.BillService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Billing is the money, and its delete rule is stricter than the rest of the
 * controller - ADMIN only, where everything else is ADMIN or STAFF. That
 * asymmetry is exactly the kind of thing a later refactor flattens by
 * accident.
 */
@WebMvcTest(
        controllers = BillController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {JwtAuthenticationFilter.class, RateLimitFilter.class}))
@Import(MethodSecurityTestConfig.class)
class BillControllerSecurityTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private BillService billService;
    @MockBean(name = "authService") private AuthService authService;

    @ParameterizedTest(name = "a {0} may not read the whole bill ledger")
    @ValueSource(strings = {"DOCTOR", "PATIENT"})
    @DisplayName("only ADMIN and STAFF may list every bill")
    void ledgerIsClosed(String role) throws Exception {
        mockMvc.perform(get("/api/bills").with(user("someone").roles(role)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    @DisplayName("staff may list every bill")
    void staffMayListTheLedger() throws Exception {
        when(billService.getAllBills()).thenReturn(List.of());

        mockMvc.perform(get("/api/bills")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "PATIENT")
    @DisplayName("a patient may read their own bill")
    void patientMayReadOwnBill() throws Exception {
        when(authService.isOwnBill(1L)).thenReturn(true);
        when(billService.getBillById(1L)).thenReturn(Optional.of(new Bill()));

        mockMvc.perform(get("/api/bills/1")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "PATIENT")
    @DisplayName("a patient may not read somebody else's bill")
    void patientMayNotReadAnotherBill() throws Exception {
        when(authService.isOwnBill(2L)).thenReturn(false);

        mockMvc.perform(get("/api/bills/2")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PATIENT")
    @DisplayName("a patient may not read another patient's bill history")
    void patientMayNotReadAnotherHistory() throws Exception {
        when(authService.isOwnPatient(anyLong())).thenReturn(false);

        mockMvc.perform(get("/api/bills/patient/7")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("deleting a bill is allowed for an administrator")
    void adminMayDelete() throws Exception {
        mockMvc.perform(delete("/api/bills/1").with(csrf())).andExpect(status().isOk());
    }

    @ParameterizedTest(name = "a {0} may not delete a bill")
    @ValueSource(strings = {"STAFF", "DOCTOR", "PATIENT"})
    @DisplayName("deleting is ADMIN-only, stricter than the rest of the controller")
    void deleteIsAdminOnly(String role) throws Exception {
        // Even holding the ownership claim does not open the delete route.
        when(authService.isOwnBill(anyLong())).thenReturn(true);

        mockMvc.perform(delete("/api/bills/1").with(csrf()).with(user("someone").roles(role)))
                .andExpect(status().isForbidden());
    }
}
