package com.travelplatform.assistant.api.dto;

import jakarta.validation.constraints.NotBlank;

public record AskRequest(@NotBlank String question) {}
