package com.hms.controller;

import com.hms.dto.response.PageResponse;
import com.hms.security.AuthService;
import com.hms.security.JwtAuthenticationFilter;
import com.hms.security.RateLimitFilter;
import com.hms.service.UserService;
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
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Everything on UserController is ADMIN-only.
 *
 * These endpoints hand out roles and reset passwords, so a rule quietly
 * loosening here is the worst case in the application: a STAFF account that
 * could reach the update endpoint could make itself an administrator.
 */
@WebMvcTest(
        controllers = UserController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {JwtAuthenticationFilter.class, RateLimitFilter.class}))
@Import(MethodSecurityTestConfig.class)
class UserControllerSecurityTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private UserService userService;
    @MockBean(name = "authService") private AuthService authService;

    private static final String BODY = """
            {"username":"someone","email":"someone@hms.local","firstName":"Some",
             "lastName":"One","role":"STAFF","isActive":true}
            """;

    /** Every route on the controller, as request builders. */
    private static List<MockHttpServletRequestBuilder> allRoutes() {
        return List.of(
                get("/api/users/paginated"),
                get("/api/users/role/DOCTOR/paginated"),
                get("/api/users/1"),
                put("/api/users/1").with(csrf()).contentType("application/json").content(BODY),
                delete("/api/users/1").with(csrf()));
    }

    @ParameterizedTest(name = "{0} is refused every user-management route")
    @ValueSource(strings = {"STAFF", "DOCTOR", "PATIENT"})
    @DisplayName("no role other than ADMIN may reach any of these")
    void onlyAdminMayReachThem(String role) throws Exception {
        for (MockHttpServletRequestBuilder route : allRoutes()) {
            mockMvc.perform(route.with(user("someone").roles(role)))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    @DisplayName("an anonymous caller is refused every route")
    @WithAnonymousUser
    void anonymousIsRefusedEverything() throws Exception {
        for (MockHttpServletRequestBuilder route : allRoutes()) {
            mockMvc.perform(route).andExpect(status().isUnauthorized());
        }
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("an administrator may list users")
    void adminMayList() throws Exception {
        when(userService.getAllUsersPaginated(anyInt(), anyInt()))
                .thenReturn(new PageResponse<>(List.of(), 0, 10, 0, 0, true, true, false, false));

        mockMvc.perform(get("/api/users/paginated")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    @DisplayName("staff cannot promote themselves - the update route is closed to them")
    void staffCannotPromoteThemselves() throws Exception {
        // Valid on every field: a 400 from validation would prove nothing about
        // who is allowed through, since body binding happens before the
        // authorization check.
        String promoteToAdmin = """
                {"username":"nurse","email":"nurse@hms.local","firstName":"Anita",
                 "lastName":"Nurse","role":"ADMIN","isActive":true}
                """;

        mockMvc.perform(put("/api/users/28").with(csrf())
                        .contentType("application/json").content(promoteToAdmin))
                .andExpect(status().isForbidden());
    }
}
