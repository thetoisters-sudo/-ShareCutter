package com.sharecutter.backend.mapper;

import com.sharecutter.backend.domain.entity.UserEntity;
import com.sharecutter.backend.dto.user.UserResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(UserEntity user);
}