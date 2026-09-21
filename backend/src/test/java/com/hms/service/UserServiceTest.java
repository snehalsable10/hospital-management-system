package com.hms.service;

import com.hms.dto.request.UpdateUserRequest;
import com.hms.entity.User;
import com.hms.exception.DuplicateResourceException;
import com.hms.exception.ResourceNotFoundException;
import com.hms.repository.UserRepository;
import com.hms.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The guards that stop an administrator locking themselves out, and the
 * optional-password behaviour on edit.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final Long ACTING_ADMIN_ID = 9L;
    private static final Long OTHER_USER_ID = 28L;

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;

    @InjectMocks private UserService userService;

    private User actingAdmin;
    private User otherUser;
    private UpdateUserRequest request;

    @BeforeEach
    void setUp() {
        actingAdmin = user(ACTING_ADMIN_ID, "demoadmin", "demoadmin@hms.local", "ADMIN");
        otherUser = user(OTHER_USER_ID, "nurseanita", "anita@hms.local", "STAFF");

        lenient().when(userRepository.findById(ACTING_ADMIN_ID)).thenReturn(Optional.of(actingAdmin));
        lenient().when(userRepository.findById(OTHER_USER_ID)).thenReturn(Optional.of(otherUser));
        lenient().when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
        lenient().when(userRepository.findByUsername(any())).thenReturn(Optional.empty());
        lenient().when(userRepository.countByRoleAndIsActiveTrue("ADMIN")).thenReturn(2L);

        request = new UpdateUserRequest();
        request.setUsername("nurseanita");
        request.setEmail("anita@hms.local");
        request.setFirstName("Anita");
        request.setLastName("Rao");
        request.setRole("STAFF");
        request.setIsActive(true);
    }

    private User user(Long id, String username, String email, String role) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setEmail(email);
        u.setRole(role);
        u.setIsActive(true);
        u.setPassword("$2a$10$originalhash");
        return u;
    }

    // --- self-lockout guards ---

    @Test
    @DisplayName("an administrator cannot change their own role")
    void refusesSelfDemotion() {
        request.setUsername("demoadmin");
        request.setEmail("demoadmin@hms.local");
        request.setRole("STAFF");

        assertThatThrownBy(() -> userService.updateUser(ACTING_ADMIN_ID, request, ACTING_ADMIN_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot change your own role");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("an administrator cannot deactivate themselves through update")
    void refusesSelfDeactivationOnUpdate() {
        request.setUsername("demoadmin");
        request.setEmail("demoadmin@hms.local");
        request.setRole("ADMIN");
        request.setIsActive(false);

        assertThatThrownBy(() -> userService.updateUser(ACTING_ADMIN_ID, request, ACTING_ADMIN_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot deactivate your own account");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("an administrator cannot deactivate themselves through delete")
    void refusesSelfDeactivation() {
        assertThatThrownBy(() -> userService.deactivateUser(ACTING_ADMIN_ID, ACTING_ADMIN_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot deactivate your own account");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("keeping your own role as ADMIN is allowed - the guard is not a blanket self-edit ban")
    void allowsEditingYourOwnDetails() {
        request.setUsername("demoadmin");
        request.setEmail("demoadmin@hms.local");
        request.setFirstName("Demo");
        request.setLastName("Administrator");
        request.setRole("ADMIN");

        assertThat(userService.updateUser(ACTING_ADMIN_ID, request, ACTING_ADMIN_ID).getSuccess())
                .isTrue();
        assertThat(actingAdmin.getLastName()).isEqualTo("Administrator");
    }

    @Test
    @DisplayName("deactivating somebody else is allowed")
    void allowsDeactivatingSomeoneElse() {
        assertThat(userService.deactivateUser(OTHER_USER_ID, ACTING_ADMIN_ID).getSuccess()).isTrue();
        assertThat(otherUser.getIsActive()).isFalse();
        verify(userRepository).save(otherUser);
    }

    // --- password handling on edit ---

    @Test
    @DisplayName("a blank password leaves the stored hash alone")
    void blankPasswordKeepsTheExistingHash() {
        request.setPassword("   ");

        userService.updateUser(OTHER_USER_ID, request, ACTING_ADMIN_ID);

        assertThat(otherUser.getPassword()).isEqualTo("$2a$10$originalhash");
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("an absent password leaves the stored hash alone")
    void absentPasswordKeepsTheExistingHash() {
        request.setPassword(null);

        userService.updateUser(OTHER_USER_ID, request, ACTING_ADMIN_ID);

        assertThat(otherUser.getPassword()).isEqualTo("$2a$10$originalhash");
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("a supplied password is encoded, never stored as given")
    void suppliedPasswordIsEncoded() {
        when(passwordEncoder.encode("newpass456")).thenReturn("$2a$10$newhash");
        request.setPassword("newpass456");

        userService.updateUser(OTHER_USER_ID, request, ACTING_ADMIN_ID);

        assertThat(otherUser.getPassword()).isEqualTo("$2a$10$newhash");
        assertThat(otherUser.getPassword()).isNotEqualTo("newpass456");
    }

    // --- role and uniqueness ---

    @Test
    @DisplayName("a role outside the allowed set is rejected")
    void rejectsUnknownRole() {
        request.setRole("SUPERUSER");

        assertThatThrownBy(() -> userService.updateUser(OTHER_USER_ID, request, ACTING_ADMIN_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown role");
    }

    @Test
    @DisplayName("a lowercase role is normalised rather than refused")
    void normalisesRoleCase() {
        request.setRole("doctor");

        userService.updateUser(OTHER_USER_ID, request, ACTING_ADMIN_ID);

        assertThat(otherUser.getRole()).isEqualTo("DOCTOR");
    }

    @Test
    @DisplayName("taking another account's email is a duplicate")
    void rejectsEmailBelongingToSomeoneElse() {
        when(userRepository.findByEmail("anita@hms.local")).thenReturn(Optional.of(actingAdmin));

        assertThatThrownBy(() -> userService.updateUser(OTHER_USER_ID, request, ACTING_ADMIN_ID))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email already registered");
    }

    @Test
    @DisplayName("keeping your own email is not a duplicate")
    void allowsKeepingYourOwnEmail() {
        when(userRepository.findByEmail("anita@hms.local")).thenReturn(Optional.of(otherUser));

        assertThat(userService.updateUser(OTHER_USER_ID, request, ACTING_ADMIN_ID).getSuccess())
                .isTrue();
    }

    @Test
    @DisplayName("taking another account's username is a duplicate")
    void rejectsUsernameBelongingToSomeoneElse() {
        when(userRepository.findByUsername("nurseanita")).thenReturn(Optional.of(actingAdmin));

        assertThatThrownBy(() -> userService.updateUser(OTHER_USER_ID, request, ACTING_ADMIN_ID))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Username already taken");
    }

    @Test
    @DisplayName("editing a user that does not exist is a 404, not a silent create")
    void missingUserIsNotFound() {
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(404L, request, ACTING_ADMIN_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
