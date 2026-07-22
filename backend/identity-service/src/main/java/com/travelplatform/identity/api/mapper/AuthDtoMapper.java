package com.travelplatform.identity.api.mapper;

import com.travelplatform.identity.api.dto.LoginRequest;
import com.travelplatform.identity.api.dto.RegisterUserRequest;
import com.travelplatform.identity.application.port.in.AuthenticateUserUseCase.AuthenticateCommand;
import com.travelplatform.identity.application.port.in.RegisterUserUseCase.RegisterUserCommand;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "cdi")
public interface AuthDtoMapper {

    @Mapping(target = "rawPassword", source = "password")
    RegisterUserCommand toCommand(RegisterUserRequest request);

    @Mapping(target = "rawPassword", source = "password")
    AuthenticateCommand toCommand(LoginRequest request);
}
