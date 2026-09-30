package com.rahul.ticketbooking.mapper;

import com.rahul.ticketbooking.dto.UserResponse;
import com.rahul.ticketbooking.entity.User;

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