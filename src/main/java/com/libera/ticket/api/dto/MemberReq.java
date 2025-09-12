package com.libera.ticket.api.dto;

import jakarta.validation.constraints.*;

public record MemberReq(
        @NotBlank String name,

        @Email(regexp = "(^$)|(^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,63}$)",
                message = "이메일 형식이 올바르지 않습니다.")
        String email,

        @Pattern(regexp = "(^$)|(^010-\\d{4}-\\d{4}$)",
                message = "전화번호는 010-xxxx-xxxx 형식이어야 합니다.")
        String phone
) {}
