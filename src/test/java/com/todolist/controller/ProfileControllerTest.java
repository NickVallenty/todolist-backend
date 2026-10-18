package com.todolist.controller;

import com.todolist.dto.request.DeleteAccountRequest;
import com.todolist.dto.request.UpdateProfileRequest;
import com.todolist.dto.response.ProfileResponse;
import com.todolist.security.CustomUserDetails;
import com.todolist.service.DeleteAccountOtpService;
import com.todolist.service.ProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileControllerTest {

    private static final long USER_ID = 7L;

    @Mock
    private ProfileService profileService;
    @Mock
    private DeleteAccountOtpService deleteAccountOtpService;
    @Mock
    private CustomUserDetails principal;

    private ProfileController controller;

    @BeforeEach
    void setUp() {
        controller = new ProfileController(profileService, deleteAccountOtpService);
        when(principal.getUserId()).thenReturn(USER_ID);
    }

    @Test
    void detail_usesUserIdFromPrincipal() {
        ProfileResponse expected = ProfileResponse.builder().userId(USER_ID).build();
        when(profileService.getProfile(USER_ID)).thenReturn(expected);

        ResponseEntity<ProfileResponse> res = controller.detail(principal);

        assertThat(res.getStatusCode().value()).isEqualTo(200);
        assertThat(res.getBody()).isSameAs(expected);
    }

    @Test
    void updateInfo_usesUserIdFromPrincipal() {
        UpdateProfileRequest req = new UpdateProfileRequest();
        ProfileResponse expected = ProfileResponse.builder().userId(USER_ID).build();
        when(profileService.updateProfile(USER_ID, req)).thenReturn(expected);

        ResponseEntity<ProfileResponse> res = controller.updateInfo(principal, req);

        assertThat(res.getStatusCode().value()).isEqualTo(200);
        assertThat(res.getBody()).isSameAs(expected);
    }

    @Test
    void deleteAccount_usesPrincipalIdAsBothCurrentAndTarget_returns204() {
        DeleteAccountRequest req = new DeleteAccountRequest();
        req.setOtp("123456");

        ResponseEntity<Void> res = controller.deleteAccount(principal, req);

        assertThat(res.getStatusCode().value()).isEqualTo(204);
        verify(deleteAccountOtpService).confirmDelete(USER_ID, USER_ID, req);
    }
}