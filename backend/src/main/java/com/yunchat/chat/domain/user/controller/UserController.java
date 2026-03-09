package com.yunchat.chat.domain.user.controller;

import com.yunchat.chat.domain.user.dto.ProfileResponse;
import com.yunchat.chat.domain.user.dto.ProfileUpdateRequest;
import com.yunchat.chat.domain.user.entity.User;
import com.yunchat.chat.domain.user.repository.UserRepository;
import com.yunchat.chat.domain.user.service.UserService;
import com.yunchat.chat.global.websocket.OnlineUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final OnlineUserService onlineUserService;
    private final UserService userService;

    // 🔥 프로필 조회
    @GetMapping("/profile")
    public ProfileResponse getProfile(@RequestParam String nickname) {

        User user = userRepository.findByNickname(nickname)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean online = onlineUserService.isOnline(user.getEmail());

        return ProfileResponse.builder()
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .statusMessage(user.getStatusMessage())
                .lastActiveAt(user.getLastActiveAt())
                .online(online)
                .build();
    }

    // 🔥 프로필 상태메시지 수정
    @PatchMapping("/profile")
    public String updateProfile(@RequestBody ProfileUpdateRequest request,
                                Authentication authentication) {

        String email = authentication.getName();

        userService.updateProfile(email, request);

        return "Profile updated successfully";
    }

    // 🔥 프로필 이미지 업로드 (카톡처럼 변경 가능)
    @PostMapping(value = "/profile-image", consumes = "multipart/form-data")
    public String uploadProfileImage(@RequestParam("file") MultipartFile file,
                                     Authentication authentication) throws Exception {

        if (authentication == null) {
            throw new RuntimeException("Authentication required");
        }

        // 🔥 이미지 크기 제한 (1MB)
        if (file.getSize() > 1024 * 1024) {
            throw new RuntimeException("Image size must be less than 1MB");
        }

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String base64 = Base64.getEncoder().encodeToString(file.getBytes());

        // 🔥 contentType null 방어
        String contentType = file.getContentType();
        if (contentType == null) {
            contentType = "image/png";
        }

        String imageData = "data:" + contentType + ";base64," + base64;

        user.setProfileImageUrl(imageData);

        userRepository.save(user);

        return "Profile image updated";
    }
}