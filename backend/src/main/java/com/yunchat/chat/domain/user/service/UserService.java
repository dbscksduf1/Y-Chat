package com.yunchat.chat.domain.user.service;

import com.yunchat.chat.domain.user.dto.LoginRequest;
import com.yunchat.chat.domain.user.dto.ProfileUpdateRequest;
import com.yunchat.chat.domain.user.dto.SignupRequest;
import com.yunchat.chat.domain.user.entity.User;
import com.yunchat.chat.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public void signup(SignupRequest request) {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("이미 존재하는 이메일");
        }

        if (userRepository.findByNickname(request.getNickname()).isPresent()) {
            throw new RuntimeException("이미 존재하는 닉네임");
        }

        User user = User.builder()
                .email(request.getEmail())
                .nickname(request.getNickname())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        userRepository.save(user);
    }

    public String login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("존재하지 않는 이메일"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("비밀번호 불일치");
        }

        user.setLastActiveAt(LocalDateTime.now());

        return user.getEmail();
    }

    // 🔥 추가 (접속 종료 시 lastActive 업데이트)
    public void updateLastActive(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setLastActiveAt(LocalDateTime.now());

    }

    public void updateProfile(String email, ProfileUpdateRequest request) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setStatusMessage(request.getStatusMessage());

        userRepository.save(user);
    }

    public boolean isNicknameDuplicate(String nickname) {
        return userRepository.findByNickname(nickname).isPresent();
    }

    public boolean isEmailDuplicate(String email) {
        return userRepository.findByEmail(email).isPresent();
    }
}