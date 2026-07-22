package com.travelplatform.identity.api.dto;

public record AuthResponse(String accessToken, long expiresInSeconds) {}
