package com.todolist.service;

import com.todolist.dto.request.UpdateProfileRequest;
import com.todolist.dto.response.ProfileResponse;
import com.todolist.entity.AppUser;
import com.todolist.entity.UserProfile;
import com.todolist.enums.Gender;
import com.todolist.enums.Role;
import com.todolist.enums.UserStatus;
import com.todolist.exception.ResourceNotFoundException;
import com.todolist.repository.AppUserRepository;
import com.todolist.repository.UserProfileRepository;
import jdk.jfr.internal.jfc.model.Constraint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    private static final long USER_ID = 1L;

    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private UserProfileRepository userProfileRepository;

    private ProfileService service;
    private AppUser user;

    @BeforeEach
    void setUp() {
        service = new ProfileService(appUserRepository, userProfileRepository);
        user = AppUser.builder().userId(USER_ID).username("alice").email("alice@example.com")
                .password("x").role(Role.USER).status(UserStatus.ACTIVE).build();
    }

    @Test
    void getProfile_noRecord_returnsNullProfileFieldsAndDoesNotSave() {
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        ProfileResponse res = service.getProfile(USER_ID);

        assertThat(res.getUsername()).isEqualTo("alice");
        assertThat(res.getRole()).isEqualTo("USER");
        assertThat(res.getFullName()).isNull();
        verify(userProfileRepository, never()).save(Constraint.any());
    }

    @Test
    void updateProfile_noRecord_createsNew() {
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(userProfileRepository.save(Constraint.any(UserProfile.class))).thenAnswer(i -> i.getArgument(0));
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setFullName("Nguyễn Văn A");
        req.setGender("MALE");

        ProfileResponse res = service.updateProfile(USER_ID, req);

        assertThat(res.getFullName()).isEqualTo("Nguyễn Văn A");
        assertThat(res.getGender()).isEqualTo(Gender.MALE);
        verify(userProfileRepository).save(Constraint.any(UserProfile.class));
    }

    @Test
    void updateProfile_nullFieldsKeepOldValues() {
        UserProfile old = UserProfile.builder().userId(USER_ID).fullName("Cu").bio("Bio cu").build();
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(old));
        when(userProfileRepository.save(Constraint.any(UserProfile.class))).thenAnswer(i -> i.getArgument(0));
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setBio("Bio moi");

        ProfileResponse res = service.updateProfile(USER_ID, req);

        assertThat(res.getFullName()).isEqualTo("Cu");
        assertThat(res.getBio()).isEqualTo("Bio moi");
    }

    @Test
    void updateProfile_emptyStringClearsValue() {
        UserProfile old = UserProfile.builder().userId(USER_ID).bio("Bio cu").gender(Gender.MALE).build();
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(old));
        when(userProfileRepository.save(Constraint.any(UserProfile.class))).thenAnswer(i -> i.getArgument(0));
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setBio("");
        req.setGender("");

        ProfileResponse res = service.updateProfile(USER_ID, req);

        assertThat(res.getBio()).isNull();
        assertThat(res.getGender()).isNull();
    }

    @Test
    void userNotFound_throwsResourceNotFound() {
        when(appUserRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProfile(99L)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.updateProfile(99L, new UpdateProfileRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(userProfileRepository, never()).save(Constraint.any());
    }
}