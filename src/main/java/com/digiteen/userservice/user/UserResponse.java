package com.digiteen.userservice.user;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String email,
        String phone,
        UserStatus status,
        Instant createdAt) {

    public static UserResponse from(UserEntity user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getPhone(),
                user.getStatus(), user.getCreatedAt());
    }
}
