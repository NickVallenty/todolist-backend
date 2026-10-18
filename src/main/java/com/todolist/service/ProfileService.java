package com.todolist.service;

import com.todolist.dto.request.UpdateProfileRequest;
import com.todolist.dto.response.ProfileResponse;
import com.todolist.entity.AppUser;
import com.todolist.entity.UserProfile;
import com.todolist.enums.Gender;
import com.todolist.exception.ResourceNotFoundException;
import com.todolist.repository.AppUserRepository;
import com.todolist.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final AppUserRepository appUserRepository;
    private final UserProfileRepository userProfileRepository;

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(Long userId) {
        AppUser user = findUser(userId);
        return toResponse(user, userProfileRepository.findByUserId(userId).orElse(null));
    }

    @Transactional
    public ProfileResponse updateProfile(Long userId, UpdateProfileRequest r) {
        AppUser user = findUser(userId);
        UserProfile p = userProfileRepository.findByUserId(userId)
                .orElseGet(() -> UserProfile.builder().userId(userId).build());

        if (r.getFullName() != null)    p.setFullName(blankToNull(r.getFullName()));
        if (r.getPhoneNumber() != null) p.setPhoneNumber(blankToNull(r.getPhoneNumber()));
        if (r.getAddress() != null)     p.setAddress(blankToNull(r.getAddress()));
        if (r.getDateOfBirth() != null) p.setDateOfBirth(r.getDateOfBirth());
        if (r.getGender() != null) {
            p.setGender(r.getGender().isBlank() ? null : Gender.valueOf(r.getGender()));
        }
        if (r.getBio() != null)         p.setBio(blankToNull(r.getBio()));
        if (r.getAvatarUrl() != null)   p.setAvatarUrl(blankToNull(r.getAvatarUrl()));

        return toResponse(user, userProfileRepository.save(p));
    }

    private AppUser findUser(Long userId) {
        return appUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
    }

    private String blankToNull(String s) {
        return s.isBlank() ? null : s.trim();
    }

    private ProfileResponse toResponse(AppUser u, UserProfile p) {
        ProfileResponse.ProfileResponseBuilder b = ProfileResponse.builder()
                .userId(u.getUserId()).username(u.getUsername())
                .email(u.getEmail()).role(u.getRole().name());
        if (p != null) {
            b.fullName(p.getFullName()).phoneNumber(p.getPhoneNumber()).address(p.getAddress())
                    .dateOfBirth(p.getDateOfBirth()).gender(p.getGender()).bio(p.getBio())
                    .avatarUrl(p.getAvatarUrl()).updatedDate(p.getUpdatedDate());
        }
        return b.build();
    }
}