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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

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

    // 🔥 상태메시지 수정
    @PatchMapping("/profile")
    public String updateProfile(@RequestBody ProfileUpdateRequest request,
                                Authentication authentication) {

        String email = authentication.getName();

        userService.updateProfile(email, request);

        return "Profile updated successfully";
    }

    // 🔥 프로필 이미지 업로드 (파일 저장 방식)
    @PostMapping(value = "/profile-image", consumes = "multipart/form-data")
    public String uploadProfileImage(@RequestParam("file") MultipartFile file,
                                     Authentication authentication) throws Exception {

        if (authentication == null) {
            throw new RuntimeException("Authentication required");
        }

        if (file.getSize() > 1024 * 1024) {
            throw new RuntimeException("Image size must be less than 1MB");
        }

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();

        Path uploadPath = Paths.get("uploads");

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path filePath = uploadPath.resolve(fileName);

        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        user.setProfileImageUrl("/uploads/" + fileName);

        userRepository.save(user);

        return "Profile image updated";
    }
}