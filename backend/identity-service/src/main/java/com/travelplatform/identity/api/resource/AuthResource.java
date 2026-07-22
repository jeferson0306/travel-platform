package com.travelplatform.identity.api.resource;

import com.travelplatform.identity.api.dto.AuthResponse;
import com.travelplatform.identity.api.dto.LoginRequest;
import com.travelplatform.identity.api.dto.RegisterUserRequest;
import com.travelplatform.identity.api.dto.RegisteredResponse;
import com.travelplatform.identity.api.mapper.AuthDtoMapper;
import com.travelplatform.identity.application.port.in.AuthenticateUserUseCase;
import com.travelplatform.identity.application.port.in.RegisterUserUseCase;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/v1/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

    private final RegisterUserUseCase registerUserUseCase;
    private final AuthenticateUserUseCase authenticateUserUseCase;
    private final AuthDtoMapper mapper;

    public AuthResource(
            RegisterUserUseCase registerUserUseCase,
            AuthenticateUserUseCase authenticateUserUseCase,
            AuthDtoMapper mapper) {
        this.registerUserUseCase = registerUserUseCase;
        this.authenticateUserUseCase = authenticateUserUseCase;
        this.mapper = mapper;
    }

    @POST
    @Path("/register")
    public Response register(@Valid RegisterUserRequest request) {
        var userId = registerUserUseCase.register(mapper.toCommand(request));
        return Response.status(Response.Status.CREATED)
                .entity(new RegisteredResponse(userId.value().toString()))
                .build();
    }

    @POST
    @Path("/login")
    public AuthResponse login(@Valid LoginRequest request) {
        var result = authenticateUserUseCase.authenticate(mapper.toCommand(request));
        return new AuthResponse(result.accessToken(), result.expiresInSeconds());
    }
}
