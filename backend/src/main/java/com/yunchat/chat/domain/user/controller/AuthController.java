package com.yunchat.chat.domain.user.controller;

import com.yunchat.chat.domain.user.dto.LoginRequest;
import com.yunchat.chat.domain.user.dto.SignupRequest;
import com.yunchat.chat.domain.user.dto.LoginResponse;
import com.yunchat.chat.domain.user.repository.UserRepository;
import com.yunchat.chat.domain.user.service.UserService;
import com.yunchat.chat.global.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/signup")
    public String signup(@RequestBody SignupRequest request) {
        userService.signup(request);
        return "회원가입 완료";
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {

        var user = userRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        String token = jwtProvider.generateToken(user.getEmail());

        return new LoginResponse(
                token,
                user.getEmail(),
                user.getNickname()
        );
    }

    @GetMapping("/check-nickname")
    public boolean checkNickname(@RequestParam String nickname) {
        return userService.isNicknameDuplicate(nickname);
    }

    @GetMapping("/check-email")
    public boolean checkEmail(@RequestParam String email) {
        return userService.isEmailDuplicate(email);
    }
}