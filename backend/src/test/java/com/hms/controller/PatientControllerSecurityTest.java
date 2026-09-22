package com.hms.controller;

import com.hms.dto.response.PageResponse;
import com.hms.entity.Patient;
import com.hms.security.AuthService;
import com.hms.security.JwtAuthenticationFilter;
import com.hms.security.RateLimitFilter;
import com.hms.service.PatientService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The @PreAuthorize rules on PatientController, exercised through the web
 * layer.
 *
 * Every one of these had only ever been checked by hand with a real token.
 * A rule that silently stops applying - an annotation dropped in a refactor,
 * a role name typo - would not have failed anything.
 *
 * The service is mocked: what is under test is who is allowed to reach it,
 * not what it does.
 */
@WebMvcTest(
        controllers = PatientController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {JwtAuthenticationFilter.class, RateLimitFilter.class}))
@Import(MethodSecurityTestConfig.class)
class PatientControllerSecurityTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private PatientService patientService;
    // Named explicitly: the @PreAuthorize expressions reference this bean as
    // @authService, and @MockBean would otherwise register it under a
    // generated name that the expression cannot resolve.
    @MockBean(name = "authService") private AuthService authService;

    private static final String BODY = """
            {"firstName":"Asha","lastName":"Verma","email":"asha@example.com",
             "phone":"9876543210","dateOfBirth":"1990-05-12","gender":"Female",
             "address":"221B Baker Street","city":"Mumbai","state":"Maharashtra",
             "zipCode":"400001"}
            """;

    @Nested
    @DisplayName("listing patients")
    class Listing {

        @Test
        @WithAnonymousUser
        @DisplayName("is refused without a login")
        void anonymousIsRefused() throws Exception {
            mockMvc.perform(get("/api/patients")).andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("is allowed for an administrator")
        void adminMayList() throws Exception {
            when(patientService.getAllPatients()).thenReturn(List.of());
            mockMvc.perform(get("/api/patients")).andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "STAFF")
        @DisplayName("is allowed for staff")
        void staffMayList() throws Exception {
            when(patientService.getAllPatients()).thenReturn(List.of());
            mockMvc.perform(get("/api/patients")).andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "DOCTOR")
        @DisplayName("is allowed for a doctor")
        void doctorMayList() throws Exception {
            when(patientService.getAllPatients()).thenReturn(List.of());
            mockMvc.perform(get("/api/patients")).andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "PATIENT")
        @DisplayName("is refused to a patient - one patient must not read the whole roll")
        void patientMayNotList() throws Exception {
            mockMvc.perform(get("/api/patients")).andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "PATIENT")
        @DisplayName("is refused to a patient on the paginated route too")
        void patientMayNotListPaginated() throws Exception {
            mockMvc.perform(get("/api/patients/paginated"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "PATIENT")
        @DisplayName("is refused to a patient on every search route")
        void patientMayNotSearch() throws Exception {
            for (String path : List.of("/api/patients/search/firstname/Asha",
                    "/api/patients/search/lastname/Verma",
                    "/api/patients/search/city/Mumbai")) {
                mockMvc.perform(get(path)).andExpect(status().isForbidden());
            }
        }
    }

    @Nested
    @DisplayName("reading one patient")
    class ReadingOne {

        @Test
        @WithMockUser(roles = "PATIENT")
        @DisplayName("is allowed when the record belongs to the caller")
        void ownRecordIsAllowed() throws Exception {
            when(authService.isOwnPatient(1L)).thenReturn(true);
            when(patientService.getPatientById(1L)).thenReturn(Optional.of(new Patient()));

            mockMvc.perform(get("/api/patients/1")).andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "PATIENT")
        @DisplayName("is refused when the record belongs to somebody else")
        void otherRecordIsRefused() throws Exception {
            when(authService.isOwnPatient(2L)).thenReturn(false);

            mockMvc.perform(get("/api/patients/2")).andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "STAFF")
        @DisplayName("is allowed for staff without any ownership link")
        void staffNeedsNoOwnership() throws Exception {
            when(authService.isOwnPatient(anyLong())).thenReturn(false);
            when(patientService.getPatientById(anyLong())).thenReturn(Optional.of(new Patient()));

            mockMvc.perform(get("/api/patients/99")).andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("writing")
    class Writing {

        @Test
        @WithMockUser(roles = "DOCTOR")
        @DisplayName("creating is refused to a doctor - registration is a desk job")
        void doctorMayNotCreate() throws Exception {
            mockMvc.perform(post("/api/patients").with(csrf())
                            .contentType("application/json").content(BODY))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "PATIENT")
        @DisplayName("creating is refused to a patient")
        void patientMayNotCreate() throws Exception {
            mockMvc.perform(post("/api/patients").with(csrf())
                            .contentType("application/json").content(BODY))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "PATIENT")
        @DisplayName("updating somebody else's record is refused")
        void patientMayNotUpdateAnother() throws Exception {
            when(authService.isOwnPatient(2L)).thenReturn(false);

            mockMvc.perform(put("/api/patients/2").with(csrf())
                            .contentType("application/json").content(BODY))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "DOCTOR")
        @DisplayName("updating is refused to a doctor with no ownership link")
        void doctorMayNotUpdate() throws Exception {
            when(authService.isOwnPatient(anyLong())).thenReturn(false);

            mockMvc.perform(put("/api/patients/1").with(csrf())
                            .contentType("application/json").content(BODY))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("deleting")
    class Deleting {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("is allowed for an administrator")
        void adminMayDelete() throws Exception {
            mockMvc.perform(delete("/api/patients/1").with(csrf()))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "STAFF")
        @DisplayName("is refused to staff - stricter than the rest of the controller")
        void staffMayNotDelete() throws Exception {
            mockMvc.perform(delete("/api/patients/1").with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "DOCTOR")
        @DisplayName("is refused to a doctor")
        void doctorMayNotDelete() throws Exception {
            mockMvc.perform(delete("/api/patients/1").with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "PATIENT")
        @DisplayName("is refused to a patient, even for their own record")
        void patientMayNotDeleteOwnRecord() throws Exception {
            when(authService.isOwnPatient(1L)).thenReturn(true);

            mockMvc.perform(delete("/api/patients/1").with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }
}
