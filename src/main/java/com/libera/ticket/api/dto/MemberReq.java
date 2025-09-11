package com.libera.ticket.api.dto;

import jakarta.validation.constraints.*;

public record MemberReq(
        @NotBlank String name,
        String email,
        String phone
) {}
