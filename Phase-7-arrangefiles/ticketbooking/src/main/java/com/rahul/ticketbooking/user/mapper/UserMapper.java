package com.rahul.ticketbooking.user.mapper;

import com.rahul.ticketbooking.user.dto.UserResponse;
import com.rahul.ticketbooking.user.entity.User;

public class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}