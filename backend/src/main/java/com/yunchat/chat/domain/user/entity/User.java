package com.yunchat.chat.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String email;

    @Column(unique = true)
    private String nickname;

    private String password;

    // 🔥 프로필 이미지 (Base64 저장)
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String profileImageUrl;

    @Column(length = 100)
    private String statusMessage;

    private LocalDateTime lastActiveAt;
}