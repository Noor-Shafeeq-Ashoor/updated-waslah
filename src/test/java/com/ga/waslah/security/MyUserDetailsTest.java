package com.ga.waslah.security;

import com.ga.waslah.model.Role;
import com.ga.waslah.model.User;
import com.ga.waslah.model.UserStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MyUserDetailsTest {

    @Test
    void shouldReturnCorrectRoleAuthority() {

        User user = new User();

        user.setUsername("noor");
        user.setPassword("encodedPassword");
        user.setRole(Role.EMPLOYER);
        user.setStatus(UserStatus.ACTIVE);

        MyUserDetails userDetails =
                new MyUserDetails(user);

        assertEquals(
                "ROLE_EMPLOYER",
                userDetails.getAuthorities()
                        .iterator()
                        .next()
                        .getAuthority()
        );
    }
}