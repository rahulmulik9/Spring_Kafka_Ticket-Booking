package com.rahul.userservice.mapper;


import com.rahul.userservice.dto.UserResponse;
import com.rahul.userservice.entity.User;


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