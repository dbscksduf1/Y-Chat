package com.yunchat.chat;

import com.yunchat.chat.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class UserQuerydslTest {

    @Autowired
    UserRepository userRepository;

    @Test
    void querydslTest() {
        var user = userRepository.findByEmail("test@test.com");
        System.out.println(user);
    }
}